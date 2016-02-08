import com.google.cloud.dataflow.sdk.Pipeline
import com.google.cloud.dataflow.sdk.io.TextIO
import com.google.cloud.dataflow.sdk.options.PipelineOptions
import com.google.cloud.dataflow.sdk.options.PipelineOptionsFactory
import com.google.cloud.dataflow.sdk.transforms.Count
import com.google.cloud.dataflow.sdk.transforms.DoFn
import com.google.cloud.dataflow.sdk.transforms.ParDo
import com.google.cloud.dataflow.sdk.values.KV
import com.google.cloud.dataflow.sdk.options.PipelineOptions
import com.google.cloud.dataflow.sdk.transforms.Filter
import com.google.cloud.dataflow.sdk.transforms.Sample
import com.google.cloud.dataflow.sdk.transforms.View
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.sys.process._
import com.google.cloud.dataflow.sdk.values.PCollection
import com.google.cloud.dataflow.sdk.values.PCollectionView
import com.google.cloud.dataflow.sdk.transforms.Top
import com.google.cloud.dataflow.sdk.transforms.SerializableComparator
import com.google.api.services.bigquery.model.TableRow
import com.google.api.services.bigquery.model.TableSchema
import com.google.api.services.bigquery.model.TableFieldSchema
import com.google.cloud.dataflow.sdk.io.BigQueryIO
import com.google.cloud.dataflow.sdk.transforms.Combine

object Main extends App {
  // It appears we must explicitly feed our command-line arguments to the
  // Pipeline constructor.
  val options = PipelineOptionsFactory.fromArgs(args).as(classOf[PipelineOptions])

  println("Starting Main")

  val punComparator = new SerializableComparator[Transforms.ScoredPun]() {
    override def compare(a: Transforms.ScoredPun, b: Transforms.ScoredPun) = {
      java.lang.Long.compare(a.getKey, b.getKey)
    }
  }

  // Create the Pipeline with default options.
  val p = Pipeline.create(options)

  val words: PCollection[String] = p
    .apply(TextIO.Read.from("gs://punorama/datasets/american-english"))
  val filtered: PCollection[String] = words
    .apply(ParDo.named("FilterWords").of(Transforms.filterWords))
  val sampled = filtered
//    .apply(Sample.any[String](100))
  val pronunciations = sampled  
    .apply(ParDo.named("GetPronunciations").of(Pronunciation.getPronunciation))
  

  val trie: PCollection[Transforms.SerializedSet] = pronunciations
    .apply(ParDo.named("TrieSeed").of(Transforms.trieSeed))
    .apply(Combine.globally(Transforms.trieCombine))  
  val scoredPuns = Transforms.emitPuns(trie.apply(View.asSingleton[Transforms.SerializedSet]), pronunciations)

  val bestPuns: PCollection[Transforms.ScoredPun] = scoredPuns
    .apply(ParDo.named("FilterPuns").of(Transforms.filterPuns))
  
//  bestPuns
//    .apply(Top.of(10000, punComparator))
//    //    .apply(ParDo.named("FormatPuns").of(Transforms.formatScoredPun))
//    .apply(ParDo.named("FormatPuns").of(Transforms.formatSortedPuns))
//    .apply(TextIO.Write.to("gs://punorama/output/100puns_trie.txt"))

  val tableSpec = BigQueryIO.parseTableSpec("punoramainsight:bestpuns.puns_testing_pron")

  bestPuns
    .apply(ParDo.named("FormatPuns").of(Transforms.scoredPunToWordConverter))
    .apply(BigQueryIO.Write.to(tableSpec)
      .withSchema(Transforms.tableSchema)
      .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_TRUNCATE)
      .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED))
  
//  trie
//    .apply(ParDo.of(Transforms.visualizeTrie))
//    .apply(TextIO.Write.to("gs://punorama/tmp/100serialized_trie.txt"))
  
  p.run()

  println("Main done")
}
