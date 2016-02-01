import scala.collection.immutable.TreeSet

object Pun {
  def punScore(pron1: String, pron2: String, word1: String, word2: String): Int = {
    val score = if (word1 == word2) { 0 } else {
      pron2.toList.inits
        .filter(x => pron1.toList.tails.contains(x))
        .map(_.size)
        .max
    }
    if (score == pron2.size || score == pron1.size) { 0 } else { score }
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

  def punScores(dictTrie: TreeSet[(String,String)], queryWord: (String,String)): List[(Int, (String, String))] = {
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