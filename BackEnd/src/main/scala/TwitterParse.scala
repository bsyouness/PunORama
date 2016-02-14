import spray.json.JsArray
import spray.json.JsObject
import spray.json.JsString
import spray.json.pimpString

/*
 * Parse a tweet, filter the tweets that are English, and return the hashtags if any.
 */

object TwitterParse {
  def getHashtags(jsonString: String): Option[List[String]] = {
    val parsed: JsObject = jsonString.parseJson.asInstanceOf[JsObject]
    if (parsed.getFields("lang").isEmpty) {
      None
    } else if (parsed.getFields("lang")(0).asInstanceOf[JsString].value == "en") {
      val hashtagsArray = parsed.getFields("entities").head.asInstanceOf[JsObject]
        .getFields("hashtags").head.asInstanceOf[JsArray].elements
      val hashtags = hashtagsArray.map({
        case x: JsObject => x.getFields("text").head.asInstanceOf[JsString].value
        // This should raise an error: all objects should be JsObject
        case _           => ???
      })
      Some(hashtags.toList)
    } else {
      None
    }
  }
}