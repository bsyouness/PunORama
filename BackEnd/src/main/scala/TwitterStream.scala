import scala.collection.JavaConversions.seqAsJavaList

import org.joda.time.DateTime
import org.joda.time.Duration

import com.google.api.services.bigquery.model.TableFieldSchema
import com.google.api.services.bigquery.model.TableRow
import com.google.api.services.bigquery.model.TableSchema
import com.google.cloud.dataflow.sdk.Pipeline
import com.google.cloud.dataflow.sdk.io.BigQueryIO
import com.google.cloud.dataflow.sdk.io.PubsubIO
import com.google.cloud.dataflow.sdk.options.PipelineOptionsFactory
import com.google.cloud.dataflow.sdk.runners.DataflowPipelineRunner
import com.google.cloud.dataflow.sdk.transforms.Count
import com.google.cloud.dataflow.sdk.transforms.DoFn
import com.google.cloud.dataflow.sdk.transforms.ParDo
import com.google.cloud.dataflow.sdk.transforms.windowing.SlidingWindows
import com.google.cloud.dataflow.sdk.transforms.windowing.Window
import com.google.cloud.dataflow.sdk.values.KV

import StreamingWordExtract.StreamingWordExtractOptions
import common.DataflowExampleUtils


/*
 * A streaming Dataflow pipeline that reads from a file containing tweets, and writes to BigQuery.
 *
 * This pipeline reads lines of text from a PubSub topic created from a text file that contains
 * tweets, filters by tweets that are in English, isolates the hashtags, and writes the output, along
 * with a timestamp, to a BigQuery table.
 *
 * */

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

  pipeline
    .apply(PubsubIO.Read.topic(options.getPubsubTopic()))
    .apply(ParDo.of(TwitterStreamHelper.getHashtags))
    .apply(Window.into[String](SlidingWindows.of(Duration.standardMinutes(1)).every(Duration.standardSeconds(20))))
    .apply(Count.perElement[String]())
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