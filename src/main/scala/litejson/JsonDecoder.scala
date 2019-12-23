package litejson

trait JsonDecoder[T] {
  def decode(js: JsValue): Option[T]
  final def decodeOpt(js: JsValue): Option[Option[T]] =
    Some(js match {
      case JsNull => None
      case j      => decode(j)
    })
}

object JsonDecoder {

  implicit object StringDecoder extends JsonDecoder[String] {
    override def decode(js: JsValue): Option[String] = js match {
      case JsString(value) => Some(value)
      case _               => None
    }
  }

  implicit object DoubleDecoder extends JsonDecoder[Double] {
    override def decode(js: JsValue): Option[Double] = js match {
      case JsNumber(value) => Some(value)
      case _               => None
    }
  }

  implicit object IntDecoder extends JsonDecoder[Int] {
    override def decode(js: JsValue): Option[Int] = js match {
      case JsNumber(value) => Some(value.toInt)
      case _               => None
    }
  }

  implicit object BooleanDecoder extends JsonDecoder[Boolean] {
    override def decode(js: JsValue): Option[Boolean] = js match {
      case JsBoolean(value) => Some(value)
      case _                => None
    }
  }

  implicit def listDecoder[U: JsonDecoder] = new JsonDecoder[List[U]] {
    override def decode(js: JsValue): Option[List[U]] = js match {
      // JsArrayの中の要素がUに揃っていなければLeft
      case JsArray(value) =>
        value
          .foldLeft[Option[List[U]]](Some(Nil)) { (acc, value) =>
            value.as[U].flatMap { u =>
              acc.map(u :: _)
            }
          }
          .map(_.reverse)
      case _ => None
    }
  }

}
