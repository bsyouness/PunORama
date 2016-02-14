import scala.sys.process.stringSeqToProcess

import com.google.cloud.dataflow.sdk.transforms.DoFn
import com.google.cloud.dataflow.sdk.values.KV

object Pronunciation {
  // Using `espeak`, get the pronunciation of a word and return a WAP
  def getPronunciation = new DoFn[String, Transforms.WAP]() {
    // Install `espeak` if it hasn't been
    override def startBundle(c: DoFn[String, Transforms.WAP]#Context) {
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
    override def processElement(c: DoFn[String, Transforms.WAP]#ProcessContext) {
      val command = Seq("espeak", "-x", "-q", "\"" + c.element + "\"")
      val pronunciation = Transforms.filterChar(command.!!.trim.drop(4))
      c.output(KV.of(c.element, pronunciation))
    }
  }    
}