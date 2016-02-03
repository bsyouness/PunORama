object Pun {
  def punScore(pron1: String, pron2: String, word1: String, word2: String): Int = {
    // val score = 
    if (word1 == word2) { 0 } else {
      pron2.toList.inits
        .filter(x => pron1.toList.tails.contains(x))
        .map(_.size)
        .max
    }
//    if (score == pron2.size || score == pron1.size) { 0 } else { score }
  }
}
