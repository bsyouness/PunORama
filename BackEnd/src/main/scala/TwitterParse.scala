import spray.json._
import DefaultJsonProtocol._

object TwitterParse {
  def getHashtags(jsonString: String): Option[List[String]] = {
    //    val parsed = jsonString.parseJson
    //    
    //    parsed match {
    //      case jso: JsObject => {
    //        parsed.getFields("lang").toList match {
    //          case Nil => None
    //          case lang :: Nil => {
    //            case s: JsString => if (s.value == "en") {
    //              None
    //            } else {
    //              None
    //            }
    //          }
    //          case _ => None
    //        }
    //      }
    //      case _ => None
    //    }

    val parsed: JsObject = jsonString.parseJson.asInstanceOf[JsObject]
    if (parsed.getFields("lang").isEmpty) {
      None
    } else if (parsed.getFields("lang")(0).asInstanceOf[JsString].value == "en") {
      val hashtagsArray = parsed.getFields("entities").head.asInstanceOf[JsObject]
        .getFields("hashtags").head.asInstanceOf[JsArray].elements
      val hashtags = hashtagsArray.map({
        case x: JsObject => x.getFields("text").head.asInstanceOf[JsString].value
        case _           => ???
      })
      Some(hashtags.toList)
    } else {
      None
    }
  }
}