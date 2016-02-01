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

object Transforms {
  // WordAndPronunciation: a key-valye object of a word and its pronunciation.
  type WAP = KV[String, String]
  // A scored pun: a key-value object, where the keys are the scores, and each
  // object is tuple of  the word and its pronunciation
  type ScoredPun = KV[java.lang.Long, KV[String, String]]

  // Filter out all the words that are not alphanumeric (more filters to come)
  val filterWords = new DoFn[String, String]() {
    override def processElement(c: DoFn[String, String]#ProcessContext) {
      val word = c.element()
      // We might invent more filters later.
      val isAlphanumeric = word.forall(_.isLetterOrDigit)
      if (isAlphanumeric) {
        c.output(word)
      }
    }
  }

  // Using `espeak`, get the pronunciation of a word and return a WAP
  def getPronunciation = new DoFn[String, WAP]() {
    // Install `espeak` if it hasn't been
    override def startBundle(c: DoFn[String, WAP]#Context) {
      val existsCommand = "which espeak".split(" ").toSeq
      if (existsCommand.! != 0) {
        val update = "sudo apt-get update".split(" ").toSeq
        assert(update.! == 0)

        val install = "sudo apt-get --assume-yes install espeak".split(" ").toSeq
        assert(install.! == 0)

        assert(existsCommand.! == 0)
      }
    }

    // Get the pronunciation of a word
    override def processElement(c: DoFn[String, WAP]#ProcessContext) {
      val command = Seq("espeak", "-x", "-q", "\"" + c.element + "\"")
      val pronunciation = filterChar(command.!!.trim.drop(4))
      c.output(KV.of(c.element, pronunciation))
    }
  }

  def filterChar(text: String): String = {
    val forbiddenList = List("'", ",", "-")
    text.toList.filterNot(forbiddenList.contains(_)).mkString("")
  }
  
  val buildTrie = new DoFn[WAP, TreeSet[WAP]]() {
    override def processElement(c: DoFn[WAP, TreeSet[WAP]]#ProcessContext) {
      c.output(c.element.foldLeft(new TreeSet[WAP])(_+_))
    }
  }
  
  
  def combineTrie(TreeSet[WAP], TreeSet[WAP]): TreeSet[WAP] = {
    val doCombine = new DoFn[TreeSet[WAP], TreeSet[WAP]]() {
      override def processElement(c: DoFn[TreeSet[WAP], TreeSet[WAP]]#ProcessContext) {
        c.output(c.sideInput.foldLeft(c.element._1)(_+_))
      }
    }
    dict2.apply(ParDo.withSideInputs(dict1).of(doCombine))
  }
  
  def cartesianProduct(pca: PCollection[WAP], pcb: PCollection[WAP]): PCollection[KV[WAP, WAP]] = {
    val view: PCollectionView[java.lang.Iterable[WAP]] = pca.apply(View.asIterable[WAP])
    val doProduct = new DoFn[WAP, KV[WAP, WAP]]() {
      override def processElement(c: DoFn[WAP, KV[WAP, WAP]]#ProcessContext) {
        val collectionA: Iterable[WAP] = c.sideInput(view)
        for (a <- collectionA) {
          c.output(KV.of(a, c.element()))
        }
      }
    }
    pcb.apply(ParDo.withSideInputs(view).of(doProduct))
  }

  def scorePuns = new DoFn[KV[WAP, WAP], ScoredPun]() {
    override def processElement(c: DoFn[KV[WAP, WAP], ScoredPun]#ProcessContext) {
      val word1 = c.element().getKey().getKey()
      val pron1 = c.element().getKey().getValue()
      val word2 = c.element().getValue().getKey()
      val pron2 = c.element().getValue().getValue()
      c.output(KV.of(Pun.punScore(pron1, pron2, word1, word2),
        KV.of(word1, word2)))
    }
  }

  def formatPair = new DoFn[KV[WAP, WAP], String]() {
    override def processElement(c: DoFn[KV[WAP, WAP], String]#ProcessContext) {
      c.output(c.element().toString)
    }
  }

  val formatScoredPun = new DoFn[ScoredPun, String]() {
    override def processElement(c: DoFn[ScoredPun, String]#ProcessContext) {
      val tuple = (c.element.getKey, c.element.getValue.getKey, c.element.getValue.getValue)
      c.output(tuple.toString)
    }
  }

  val emitPuns = new DoFn[java.util.List[ScoredPun], ScoredPun]() {
    override def processElement(c: DoFn[java.util.List[ScoredPun], ScoredPun]#ProcessContext) {
      val puns = c.element
      for (p <- puns) {
        c.output(p)
      }
    }
  }

  val formatSortedPuns = new DoFn[java.util.List[ScoredPun], String]() {
    override def processElement(c: DoFn[java.util.List[ScoredPun], String]#ProcessContext) {
      val puns = c.element
      for (p <- puns) {
        val tuple = (p.getKey, p.getValue.getKey, p.getValue.getValue)
        c.output(tuple.toString)
      }
    }
  }

  val filterPuns = new DoFn[ScoredPun, ScoredPun]() {
    override def processElement(c: DoFn[ScoredPun, ScoredPun]#ProcessContext) {
      val threshold = 2
      val pun = c.element()
      if (pun.getKey >= threshold) {
        c.output(pun)
      }
    }
  }

  val tableSchema = new TableSchema().setFields(List(
    new TableFieldSchema().setName("score").setType("INTEGER"),
    new TableFieldSchema().setName("word_0").setType("STRING"),
    new TableFieldSchema().setName("word_1").setType("STRING")))

  val scoredPunToWordConverter = new DoFn[ScoredPun, TableRow]() {
    override def processElement(c: DoFn[ScoredPun, TableRow]#ProcessContext) {
      val score: Long = c.element().getKey
      val word0: String = c.element().getValue.getKey
      val word1: String = c.element().getValue.getValue
      c.output(new TableRow().set("score", score).set("word_0", word0).set("word_1", word1))
    }
  }
}

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
    .apply(ParDo.named("GetPronunciations").of(Transforms.getPronunciation))
  val dictTrieSingle = pronunciations
    .apply(ParDo.named("BuildSingleDict").of(Transforms.buildTrie))
  val dictTrie = dictTrieSingle
    .apply(ParDoc.named("CombineDict").of(Transforms.combineTreeSet(new TreeSet[String])(_+_)
//  val pairs = Transforms.cartesianProduct(pronunciations, pronunciations)
  val puns = dictTrie.apply(ParDo.named("LookingUp").of(Transforms.LookUp))
  val scoredPuns: PCollection[Transforms.ScoredPun] = puns //pairs
    .apply(ParDo.named("ScorePuns").of(Transforms.scorePuns))
  val bestPuns: PCollection[Transforms.ScoredPun] = scoredPuns
    .apply(ParDo.named("FilterPuns").of(Transforms.filterPuns))

  bestPuns
    .apply(Top.of(10000, punComparator))
    //    .apply(ParDo.named("FormatPuns").of(Transforms.formatScoredPun))
    .apply(ParDo.named("FormatPuns").of(Transforms.formatSortedPuns))
    .apply(TextIO.Write.to("gs://punorama/output/10000puns.txt"))

  val tableSpec = BigQueryIO.parseTableSpec("punoramainsight:bestpuns.puns")

  bestPuns
    .apply(ParDo.named("FormatPuns").of(Transforms.scoredPunToWordConverter))
    .apply(BigQueryIO.Write.to(tableSpec)
      .withSchema(Transforms.tableSchema)
      .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_TRUNCATE)
      .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED))

  p.run()

  println("Main done")
}
