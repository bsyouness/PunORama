import scala.io._
import scala.sys.process._
import javax.annotation.Resource
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.BufferedWriter
import java.io.File

object PunFinder {
  // http://stackoverflow.com/questions/14740199/cross-product-in-scala
  implicit class Crossable[X](xs: Traversable[X]) {
    def cross[Y](ys: Traversable[Y]) = for { x <- xs; y <- ys } yield (x, y)
  }

  def getPronunciation(text: String): String = {
    val command = Seq("espeak", "-x", "-q", "\"" + text + "\"")

    println("Running: " + command.mkString(" "))
    val pronunciation = command.!!

    pronunciation.trim.drop(4)
  }

  def getPrefix(text: String, len: Int): String = {
    if (text.length >= len) { text.take(len) } else { text }
  }

  def getSuffix(text: String, len: Int): String = {
    if (text.length >= len) { text.takeRight(len) } else { text }
  }

  def pronouncedSame(x: String, y: String, len: Int): Boolean = {
    //getPrefix(x, len) == getSuffix(y, len) ||
    getSuffix(x, len) == getPrefix(y, len)
  }
  def main(args: Array[String]) {
    val prontest = getPronunciation("testing test of tests")

    val dict1 = Source.fromFile("/home/youness/Downloads/wordsEn.txt").getLines.toList
    val dict = dict1.take(1000)
    val pronDict = dict.map(x => (x, getPronunciation(x)))
    val dbl_dict = pronDict cross pronDict
    val len = 3
    val puns = dbl_dict.filter({ case (x, y) => pronouncedSame(x._2, y._2, len) })
      .map({ case (x, y) => (x._1, y._1) })

    val file = new File("/home/youness/Downloads/puns")
    val bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)))
    for (x <- puns) {
      bw.write(x + "\n")
    }
    bw.close()
  }
}