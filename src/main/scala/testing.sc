import collection.mutable.HashMap
import scala.io.Source
import com.google.cloud.dataflow.sdk.repackaged.com.google.common.collect.Count
import PunPal_DataFlow.Transforms.punScore

object testing {
  println("Welcome to the Scala worksheet")
  val consonants = "bcdfghjklmnpqrstvwxyz".toCharArray
  val vowels = "aeiou".toCharArray
  val hash = new HashMap[String,String]()
  hash += ("ce" -> "s",
  				"ci" -> "si",
  				"ca" -> "ka",
  				"co" -> "ko",
  				"cu" -> "ku")
  val dict = Source.fromFile("/home/youness/Downloads/wordsEn.txt").getLines()
  println(dict.length)
  //def filterChar(text: String): String = {
  val forbiddenList = "',-".toCharArray
  var text = "t'Est w'0n m'o@ k'0mplI2k,eItI2d t'Edib'e@ wID b'e@r-INz"
  val s = ""
  println(text.toList.filterNot(forbiddenList.contains(_)))
  val word1 = "caterpillar"
  val word2 = "pontificat"
  for (l <- word1.toList.tails) (println(l))
  word1.toList.inits.filter(x => word2.toList.tails.contains(x)).map(_.size).max
  word1.toList.tails.contains(List())
   
  
  
}