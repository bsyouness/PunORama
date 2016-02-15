import scala.collection.JavaConversions.seqAsJavaList

import com.google.api.services.bigquery.model.TableRow
import com.google.cloud.dataflow.sdk.Pipeline
import com.google.cloud.dataflow.sdk.io.BigQueryIO
import com.google.cloud.dataflow.sdk.options.PipelineOptions
import com.google.cloud.dataflow.sdk.options.PipelineOptionsFactory
import com.google.cloud.dataflow.sdk.runners.DirectPipelineRunner
import com.google.cloud.dataflow.sdk.transforms.Create
/*
 * A pipeline to clear a BigQuery table. 
 * Doesn't currently work...
 */

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