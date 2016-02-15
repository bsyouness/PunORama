import scala.collection.immutable.TreeSet

/*
 * Determine whether two words constitute a pun, and score the pun based on the overlap between the two words.
 */

object Pun {
  def overlapSize(first: String, second: String): Int = {
    first.tails.find({ t => second.startsWith(t) }).get.size
  }

  // Given two pronunciations assign a pun score.
  // Higher scores are better.
  def punScoreOld(first: String, second: String, word1: String, word2: String): Int = {
    if (first == second) {
      0
    } else if ((word1 contains word2) || (word2 contains word1)) {
      0
    } else {
      val overlap = overlapSize(first, second)
      overlap
    }
  }

  def punScore(pron1: String, pron2: String, word1: String, word2: String): Int = {
    val score = pron2.toList.inits
      .filter(x => pron1.toList.tails.contains(x))
      .map(_.size)
      .max
    if ((word1.toUpperCase() contains word2.toUpperCase()) || (word2.toUpperCase() contains word1.toUpperCase())) { 0 } else { score }
  }

  def nextString(s: String): String = {
    val lastChar: Char = s.last
    val incrementedChar: Char = (lastChar.toInt + 1).toChar
    assert(incrementedChar > lastChar)
    s.init + incrementedChar
  }

  def findPuns(dictTrie: TreeSet[(String, String)], queryWord: (String, String)): List[(Int, (String, String))] = {
    queryWord._1.tails.toList.init.zipWithIndex.flatMap(
      {
        case (t, i) => {
          val upperBound = nextString(t)
          val matchingWords = dictTrie.range((t, ""), (upperBound, "")).toList
          // Why not just i??
          matchingWords.map(w => (queryWord._1.length - i, w))
        }
      }).toList
  }
}