import scala.collection.immutable.TreeSet

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
      // if the pun involves the whole pronunciation of a word, we could assume
      // that the pun isn't interesting
      // if (overlap == first.size || overlap == second.size) { 0 } else { overlap }
    }
  }

  def punScore(pron1: String, pron2: String, word1: String, word2: String): Int = {
    val score = pron2.toList.inits
        .filter(x => pron1.toList.tails.contains(x))
        .map(_.size)
        .max
    if ((word1.toUpperCase() contains word2.toUpperCase()) || (word2.toUpperCase() contains word1.toUpperCase())) //(score == pron2.size || score == pron1.size) 
    { 0 } else { score }
  }

  def nextString(s: String): String = {
    val lastChar: Char = s.last
    val incrementedChar: Char = (lastChar.toInt + 1).toChar
    // Make sure we actually increment the char.
    // This would fail if the last char is the largest char, which
    // I'm hoping won't be true for any words in the dataset.
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