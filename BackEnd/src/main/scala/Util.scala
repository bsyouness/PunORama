import scala.sys.process.stringSeqToProcess

/*
 * Miscellaneous utilities 
 */

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