object Pun {
  def punScore(firstPronunciation: String, secondPronunciation: String): Int = {
     secondPronunciation.toList.inits
       .filter(x => firstPronunciation.toList.tails.contains(x)).map(_.size).max 
  }   
}