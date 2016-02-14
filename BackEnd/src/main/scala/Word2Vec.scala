import java.io.File

import scala.sys.process.stringSeqToProcess

import org.apache.commons.io.FileUtils


/*
 * A suite of methods to find all most related words to all the words in the dictionary used for the puns. 
 * Doesn't currently work...
 */

object WordToVec {
  val tmuxSessionName = "word2vecdistance"
  val logFile = "/tmp/word2vec_distance_log.txt"
  val word2VecQueryTimeInMilliseconds = 5000
  val numMatchesPerWord = 40

  val word2vecDistancePath = "gs://punorama/wordtovec/distance"
  val word2vecModelNameGZ = "GoogleNews-vectors-negative300.bin.gz"
  val word2vecModelPath = "gs://punorama/wordtovec/" + word2vecModelNameGZ

  def tmuxSessionExists(): Boolean = {
    val sessions = "tmux list-sessions".split(" ").toSeq.!!
    sessions.contains(tmuxSessionName)
  }

  def checkTmuxSessionExists() {
    assert(tmuxSessionExists)
  }

  def stopTmuxSession() {
    val command = s"tmux kill-session -t ${WordToVec.tmuxSessionName}".split(" ").toSeq
    Util.runCommand(command)
  }

  def parseBlock(block: List[String]): Option[List[(Double, String)]] = {
    if (block(1).contains("Out of dictionary word!")) {
      None
    } else {
      val wordsAndScores = block.drop(4)

      Some(wordsAndScores.map(l => {
        val split = l.split("[\t ]+").toList
        val word = split(1)
        val similarity = split(2).toDouble
        (similarity, word)
      }))
    }
  }

  def getSimilarWords(word: String): Option[List[(Double, String)]] = {
    checkTmuxSessionExists()

    val logBefore = FileUtils.readFileToString(new File(logFile))

    val command = s"""tmux send-keys -t $tmuxSessionName $word C-m""".split(" ").toSeq
    println("Running: " + command.mkString(" "))
    assert(command.! == 0)

    // Allow word2vec to run.
    Thread.sleep(word2VecQueryTimeInMilliseconds)

    // Run some silly commands to force word2vec to flush.
    for (i <- 0 until 2) {
      val command = s"""tmux send-keys -t $tmuxSessionName camel C-m""".split(" ").toSeq
      println("Running: " + command.mkString(" "))
      assert(command.! == 0)

      // Allow word2vec to run.
      Thread.sleep(word2VecQueryTimeInMilliseconds)
    }

    val logAfter = FileUtils.readFileToString(new File(logFile))

    val newLines = logAfter.drop(logBefore.length).split("\n")

    val thisBlock = newLines.dropWhile(!_.contains(s"Word: $word")).take(44).toList

    parseBlock(thisBlock)
  }

  def setUpWord2Vec() {
    Util.installPackage("gunzip", "gzip")
    Util.installPackage("tmux", "tmux")

    println(Seq("echo", "\"$USER\"").!!)

    print("after gsutil")

    // Check if the distance binary is here.
    // If not, assume we have to download and install everything.
    if (s"ls $word2vecDistancePath".split(" ").toSeq.! != 0) {
      val cpDistance = s"gsutil cp $word2vecDistancePath .".split(" ").toSeq
      Util.runCommand(cpDistance)

      val cpModel = s"gsutil cp $word2vecModelPath .".split(" ").toSeq
      Util.runCommand(cpModel)

      val gunzipModel = s"gunzip $word2vecModelNameGZ".split(" ").toSeq
      Util.runCommand(gunzipModel)
    }

    if (!tmuxSessionExists()) {
      val tmuxInnerCommand = s"./distance GoogleNews-vectors-negative300.bin > ${WordToVec.logFile}"
      val startTmux = s"tmux new-session -d -s ${WordToVec.tmuxSessionName}".split(" ").toSeq ++ Seq("\"", tmuxInnerCommand, "\"")
      Util.runCommand(startTmux)

      Thread.sleep(30000)
    }
  }
}