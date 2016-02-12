import com.google.cloud.dataflow.sdk.transforms.Top
import com.google.cloud.dataflow.sdk.transforms.SerializableComparator
import org.joda.time.Duration
import org.joda.time.DateTime
import com.google.api.services.bigquery.model.TableFieldSchema
import com.google.cloud.dataflow.sdk.values.PCollection
import com.google.api.services.bigquery.model.TableSchema
import com.google.cloud.dataflow.sdk.options.PipelineOptions
import com.google.cloud.dataflow.sdk.options.PipelineOptionsFactory
import com.google.api.services.bigquery.model.TableFieldSchema
import com.google.api.services.bigquery.model.TableRow
import com.google.api.services.bigquery.model.TableSchema
import common.DataflowExampleUtils
import common.ExampleBigQueryTableOptions
import common.ExamplePubsubTopicOptions
import com.google.cloud.dataflow.sdk.transforms.windowing._
import com.google.cloud.dataflow.sdk.Pipeline
import com.google.cloud.dataflow.sdk.PipelineResult
import com.google.cloud.dataflow.sdk.io.BigQueryIO
import com.google.cloud.dataflow.sdk.io.PubsubIO
import com.google.cloud.dataflow.sdk.options.Default
import com.google.cloud.dataflow.sdk.options.Description
import com.google.cloud.dataflow.sdk.runners.DataflowPipelineRunner
import com.google.cloud.dataflow.sdk.transforms.DoFn
import com.google.cloud.dataflow.sdk.transforms.ParDo
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import java.io.IOException
import java.util.ArrayList
import com.google.cloud.dataflow.sdk.values.KV
import com.google.cloud.dataflow.sdk.transforms.Count

object TwitterStreamHelper {

  val getHashtags = new DoFn[String, String]() {
    override def processElement(c: DoFn[String, String]#ProcessContext) {
      val hashtags = TwitterParse.getHashtags(c.element())
      for (hashtag <- hashtags) {
        for (h <- hashtag) {
          c.output(h)
        }
      }
    }
  }

  //  type HC = KV[String, java.lang.Long]
  //  type CH = KV[java.lang.Long, String]
  //
  //  val swapHCToCH = new DoFn[HC, CH]() {
  //    override def processElement(c: DoFn[HC, CH]#ProcessContext) {
  //      c.output(KV.of(c.element.getValue, c.element.getKey))
  //    }
  //  }
  //
  //  val countComparator = new SerializableComparator[KV[String, java.lang.Long]]() {
  //    override def compare(a: KV[String, java.lang.Long], b: KV[String, java.lang.Long]) = {
  //      java.lang.Long.compare(a.getValue, b.getValue)
  //    }
  //  }

  val tableSchema = new TableSchema().setFields(List(
    new TableFieldSchema().setName("hashtag").setType("STRING"),
    new TableFieldSchema().setName("count").setType("INTEGER"),
    new TableFieldSchema().setName("timestamp").setType("TIMESTAMP")))

  val tweetToRowConverter = new DoFn[KV[String, java.lang.Long], TableRow]() {
    override def processElement(c: DoFn[KV[String, java.lang.Long], TableRow]#ProcessContext) {
      //      for (c <- cs.element) {
      val hashtag: String = c.element.getKey
      val count: java.lang.Long = c.element.getValue
      val timestamp = new DateTime().getMillis() / 1000
      c.output(new TableRow()
        .set("hashtag", hashtag)
        .set("count", count)
        .set("timestamp", timestamp))
    }
  }
}

//abstract class TwitterStreamOptions extends ExamplePubsubTopicOptions with ExampleBigQueryTableOptions

object TwitterStream extends App {
  val options = PipelineOptionsFactory.fromArgs(args)
    .withValidation()
    .as(classOf[StreamingWordExtract.StreamingWordExtractOptions])

  options.setStreaming(true)
  options.setRunner(classOf[DataflowPipelineRunner])

  val dataflowUtils = new DataflowExampleUtils(options)
  dataflowUtils.setup()
  val pipeline = Pipeline.create(options)

  val tableSpec = BigQueryIO.parseTableSpec("punoramainsight:tweets.tweetset")

  //  println("Clearing BigQuery table tweetset1...")
  //  ClearBQTable.pipeline.run
  //  println("Done clearing BigQuery table tweetset1")

  //  type KVList = java.util.List[KV[String, java.lang.Long]]

  pipeline
    .apply(PubsubIO.Read.topic(options.getPubsubTopic()))
    .apply(ParDo.of(TwitterStreamHelper.getHashtags))
    .apply(Window.into[String](SlidingWindows.of(Duration.standardMinutes(1)).every(Duration.standardSeconds(20))))
    .apply(Count.perElement[String]())
    //    .apply(ParDo.of(TwitterStreamHelper.swapHCToCH))
    //    .apply(Top.largest(10))
    //        .apply(Top.of(10, TwitterStreamHelper.countComparator).asSingletonView())
    .apply(ParDo.of(TwitterStreamHelper.tweetToRowConverter))
    .apply(BigQueryIO.Write.to(tableSpec)
      .withSchema(TwitterStreamHelper.tableSchema)
      .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED))

  val result = pipeline.run()

  if (!options.getInputFile().isEmpty()) {
    // Inject the data into the Pub/Sub topic with a Dataflow batch pipeline.
    dataflowUtils.runInjectorPipeline("gs://punorama/datasets/twitter_concat.subset.bz2",
      options.getPubsubTopic())
  }

  // dataflowUtils will try to cancel the pipeline and the injector before the program exists.
  dataflowUtils.waitToFinish(result)
}