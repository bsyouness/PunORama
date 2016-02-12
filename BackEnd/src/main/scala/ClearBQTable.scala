import com.google.api.services.bigquery.model.TableFieldSchema
import com.google.cloud.dataflow.sdk.runners.DataflowPipelineRunner
import com.google.cloud.dataflow.sdk.runners.DirectPipelineRunner
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
import com.google.cloud.dataflow.sdk.Pipeline
import com.google.cloud.dataflow.sdk.PipelineResult
import com.google.cloud.dataflow.sdk.io.BigQueryIO
import com.google.cloud.dataflow.sdk.io.PubsubIO
import com.google.cloud.dataflow.sdk.options.Default
import com.google.cloud.dataflow.sdk.options.Description
import com.google.cloud.dataflow.sdk.runners.DataflowPipelineRunner
import com.google.cloud.dataflow.sdk.transforms.DoFn
import com.google.cloud.dataflow.sdk.transforms.ParDo
import com.google.cloud.dataflow.sdk.transforms.Create
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import java.io.IOException
//import java.util.ArrayList

object ClearBQTable extends App {
  val options = PipelineOptionsFactory.fromArgs(args).as(classOf[PipelineOptions])
  options.setRunner(classOf[DirectPipelineRunner])

  val pipeline = Pipeline.create(options)

  val tableSpec = BigQueryIO.parseTableSpec("punoramainsight:tweets.tweetset1")
  
  val emptyList: java.util.List[TableRow] = Nil
  
  pipeline
    .apply(Create.of(emptyList))
    .apply(BigQueryIO.Write.to(tableSpec)
      .withSchema(TwitterStreamHelper.tableSchema)
      .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_TRUNCATE)
      .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED))

}