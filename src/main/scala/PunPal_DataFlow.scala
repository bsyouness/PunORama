import scala.io._
import scala.sys.process._
import javax.annotation.Resource
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.BufferedWriter
import java.io.File
import com.google.cloud.dataflow.sdk.Pipeline
import com.google.cloud.dataflow.sdk.io.TextIO
import com.google.cloud.dataflow.sdk.options.PipelineOptions
import com.google.cloud.dataflow.sdk.options.PipelineOptionsFactory
import com.google.cloud.dataflow.sdk.transforms.Count
import com.google.cloud.dataflow.sdk.transforms.DoFn
import com.google.cloud.dataflow.sdk.transforms.ParDo
import com.google.cloud.dataflow.sdk.values.KV

object Main extends App {
  val options = PipelineOptionsFactory.fromArgs(args).as(classOf[PipelineOptions])
  println("Starting Main")
  // Create the Pipeline with default options.
  val p = Pipeline.create(options)

  //  // http://stackoverflow.com/questions/14740199/cross-product-in-scala
  //  implicit class Crossable[X](xs: Traversable[X]) {
  //    def cross[Y](ys: Traversable[Y]) = for { x <- xs; y <- ys } yield (x, y)
  //  }

  val getPronunciation = new DoFn[String, (String, String, String)]() {
    override def processElement(c: DoFn[String, (String, String, String)]#ProcessContext) {
      val len = 3
      "yes | sudo apt-get install espeak".split(" ").toSeq.!!
      val command = Seq("espeak", "-x", "-q", "\"" + c.element + "\"")
      val pronunciation = filterChar(command.!!).trim.drop(4)
      c.output((getPrefix(pronunciation), getSuffix(pronunciation), c.element))
    }
  }

  def filterChar(text: String): String = {
    val forbiddenList = List("'", ",", "-")
    text.toList.filterNot(forbiddenList.contains(_)).mkString("")
  }

  def getPrefix(text: String): String = {
    val len = 3
    if (text.length >= len) { text.take(len) } else { text }
  }

  def getSuffix(text: String): String = {
    val len = 3
    if (text.length >= len) { text.takeRight(len) } else { text }
  }

  //  def pronouncedSame(x: String, y: String, len: Int): Boolean = {
  //    //getPrefix(x, len) == getSuffix(y, len) ||
  //    getSuffix(x, len) == getPrefix(y, len)
  //  }

  val pronouncedSame = new DoFn[((String, String, String), (String, String)), ((String, String), (String, String))]() {
    override def processElement(c: DoFn[((String, String), (String, String)), ((String, String), (String, String))]#ProcessContext) {
      if (getSuffix(c.element._1._2) == getPrefix(c.element._2._2)) { c.output(c.element) }
    }
  }

  val switch0and1 = new DoFn[(String, String, String)]() {
    override def processElement(c: DoFn[(String, String, String)]#ProcessContext) {
      c.output(c.element._2, c.element._1, c.element._3)
    }
  }

  val add1AtTheEnd = new DoFn[((String, String, String), (String, String, String, Int))]() {
    override def processElement(c: DoFn[((String, String, String), (String, String, String, Int))]#ProcessContext) {
      c.output((c.element._1, c.element._2, c.element._3, 1))
    }
  }

  val add1At2heEnd = new DoFn[((String, String, String), (String, String, String, Int))]() {
    override def processElement(c: DoFn[((String, String, String), (String, String, String, Int))]#ProcessContext) {
      c.output((c.element._1, c.element._2, c.element._3, 2))
    }
  }
  
  val extract3And4 = new DoFn[((String, List(String), List(String), List(Int)), (List(Int), List(String)))]() {
    override def processElement(c: DoFn[((String, List(String), List(String), List(Int)), (List(Int), List(String)))]#ProcessContext) {
      c.output(c.element._4, c.element._3)
    }
  }
  
  val extract4 = new DoFn[((String, String, String, Int), (Int))]() {
    override def processElement(c: DoFn[((String, String, String, Int), (Int))]#ProcessContext) {
      c.output(c.element._4)
    }
  }
  
  val transform = new DoFn[((List(Int), List(String), (List(String), List(String))]() {
    override def processElement(c: DoFn[((List(Int), List(String), (List(String), List(String))]#ProcessContext) {
      c.output(c.element._2.filter(c.element._1 == 2),c.element._2.filter(c.element._1 == 1))
    }
  }
  
  //  val firstForString = firstElement[String, String]()

  //  def cross[Y](ys: Traversable[Y]) = for { x <- xs; y <- ys } yield (x, y)
  //  def secondElement[A, B]() = new DoFn[(A, B), B]() {
  //    override def processElement(c: DoFn[(A, B), B]#ProcessContext) {
  //      c.output(c.element(1))
  //    }
  //  }
  //    
  //    val secondForString = firstElement[String, String]()
  //  
  
  // dict1 contains the prefixes
  val dict1 = p.apply(TextIO.Read.from("gs://younessinsight/wordsEn.txt"))
    .apply(ParDo.of(getPronunciation))
  // dict2 contains the suffixes
  val dict2 = dict1.switch0and1
  val dict1Numbered = dict.add1AtTheEnd
  val dict2Numbered = dict.add2AtTheEnd
  
  val dicts: PCollectionList = PCollectionList.of(dict1Numbered).and(dict2Numbered)
    .apply(Flatten.create((String, String, String, Int))
    .apply(GroupByKey.create)
    .apply(ParDo.of(extract3And4))
    .apply(ParDo.of(transform))
    .apply(TextIO.Write.to("gs://younessinsight/puns.txt"))

  p.run()
//  https://github.com/GoogleCloudPlatform/DataflowJavaSDK/blob/master/examples/src/main/java/com/google/cloud/dataflow/examples/cookbook/CombinePerKeyExamples.java
//      p.apply(BigQueryIO.Read.from(options.getInput()))
//     .apply(new PlaysForWord())
//     .apply(BigQueryIO.Write
//        .to(options.getOutput())
//        .withSchema(schema)
//        .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
//        .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_TRUNCATE));
//  https://github.com/GoogleCloudPlatform/DataflowJavaSDK/blob/master/examples/src/main/java/com/google/cloud/dataflow/examples/cookbook/MaxPerKeyExamples.java
//      p.apply(BigQueryIO.Read.from(options.getInput()))
//     .apply(new MaxMeanTemp())
//     .apply(BigQueryIO.Write
//        .to(options.getOutput())
//        .withSchema(schema)
//        .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
//        .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_TRUNCATE));

  println("Main done")

}