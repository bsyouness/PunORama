import collection.mutable.HashMap
import scala.io.Source
import com.google.cloud.dataflow.sdk.repackaged.com.google.common.collect.Count

object testing {
  println("Welcome to the Scala worksheet")       //> Welcome to the Scala worksheet
  val consonants = "bcdfghjklmnpqrstvwxyz".toCharArray
                                                  //> consonants  : Array[Char] = Array(b, c, d, f, g, h, j, k, l, m, n, p, q, r, 
                                                  //| s, t, v, w, x, y, z)
  val vowels = "aeiou".toCharArray                //> vowels  : Array[Char] = Array(a, e, i, o, u)
  val hash = new HashMap[String,String]()         //> hash  : scala.collection.mutable.HashMap[String,String] = Map()
  hash += ("ce" -> "s",
  				"ci" -> "si",
  				"ca" -> "ka",
  				"co" -> "ko",
  				"cu" -> "ku")     //> res0: testing.hash.type = Map(ca -> ka, co -> ko, ci -> si, cu -> ku, ce -> 
                                                  //| s)
  val dict = Source.fromFile("/home/youness/Downloads/wordsEn.txt").getLines()
                                                  //> dict  : Iterator[String] = non-empty iterator
  println(dict.length)                            //> 109583
  //def filterChar(text: String): String = {
  val forbiddenList = "',-".toCharArray           //> forbiddenList  : Array[Char] = Array(', ,, -)
  var text = "t'Est w'0n m'o@ k'0mplI2k,eItI2d t'Edib'e@ wID b'e@r-INz"
                                                  //> text  : String = t'Est w'0n m'o@ k'0mplI2k,eItI2d t'Edib'e@ wID b'e@r-INz
  val s = ""                                      //> s  : String = ""
  println(text.toList.filterNot(forbiddenList.contains(_)))
                                                  //> List(t, E, s, t,  , w, 0, n,  , m, o, @,  , k, 0, m, p, l, I, 2, k, e, I, t,
                                                  //|  I, 2, d,  , t, E, d, i, b, e, @,  , w, I, D,  , b, e, @, r, I, N, z)
  val word1 = "caterpillar"                       //> word1  : String = caterpillar
  val word2 = "pontificat"                        //> word2  : String = pontificat
  for (l <- word1.toList.tails) (println(l))      //> List(c, a, t, e, r, p, i, l, l, a, r)
                                                  //| List(a, t, e, r, p, i, l, l, a, r)
                                                  //| List(t, e, r, p, i, l, l, a, r)
                                                  //| List(e, r, p, i, l, l, a, r)
                                                  //| List(r, p, i, l, l, a, r)
                                                  //| List(p, i, l, l, a, r)
                                                  //| List(i, l, l, a, r)
                                                  //| List(l, l, a, r)
                                                  //| List(l, a, r)
                                                  //| List(a, r)
                                                  //| List(r)
                                                  //| List()
  word1.toList.inits.filter(x => word2.toList.tails.contains(x)).map(_.size).max
                                                  //> res1: Int = 3
  word1.toList.tails.contains(List())             //> res2: Boolean = true
   
  
  
}