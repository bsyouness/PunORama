import com.google.cloud.dataflow.sdk.Pipeline
import com.google.cloud.dataflow.sdk.io.BigQueryIO
import com.google.cloud.dataflow.sdk.io.TextIO
import com.google.cloud.dataflow.sdk.options.PipelineOptions
import com.google.cloud.dataflow.sdk.options.PipelineOptionsFactory
import com.google.cloud.dataflow.sdk.transforms.Combine
import com.google.cloud.dataflow.sdk.transforms.ParDo
import com.google.cloud.dataflow.sdk.transforms.SerializableComparator
import com.google.cloud.dataflow.sdk.transforms.View
import com.google.cloud.dataflow.sdk.values.PCollection

/*
 * A DataFlow pipeline that finds puns based on the English dictionary, and saves them to a 
 * BigQuery table.
 */

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
  
  val tableSpec = BigQueryIO.parseTableSpec("punoramainsight:bestpuns.puns_testing_pron")

  bestPuns
    .apply(ParDo.named("FormatPuns").of(Transforms.scoredPunToWordConverter))
    .apply(BigQueryIO.Write.to(tableSpec)
      .withSchema(Transforms.tableSchema)
      .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_TRUNCATE)
      .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED))
  
  p.run()

  println("Main done")
}
