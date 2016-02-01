//do we need these imports here??
import com.google.cloud.dataflow.sdk.values.PCollectionView
import com.google.cloud.dataflow.sdk.transforms.DoFn
import com.google.api.services.bigquery.model.TableFieldSchema
import com.google.cloud.dataflow.sdk.values.PCollection
import com.google.cloud.dataflow.sdk.values.KV
import com.google.api.services.bigquery.model.TableSchema
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

object Util {
  def runCommand(command: Seq[String]) {
    println("Running command: " + command)
    assert(command.! == 0)
  }

  def installPackage(binaryName: String, packageName: String) {
    val existsCommand = s"which $binaryName".split(" ").toSeq
    if (existsCommand.! != 0) {
      val update = "sudo apt-get update".split(" ").toSeq
      assert(update.! == 0)

      val install = s"sudo apt-get --assume-yes install $packageName".split(" ").toSeq
      assert(install.! == 0)

      assert(existsCommand.! == 0)
    }
  }
}