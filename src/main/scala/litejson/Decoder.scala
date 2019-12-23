package litejson

trait Decoder[T] {
  def decode(js: JsValue): Option[T]
  final def decodeOpt(js: JsValue): Option[Option[T]] =
    Some(js match {
      case JsNull => None
      case j      => decode(j)
    })
}

object Decoder {

  implicit object StringDecoder extends Decoder[String] {
    override def decode(js: JsValue): Option[String] = js match {
      case JsString(value) => Some(value)
      case _               => None
    }
  }

  implicit object DoubleDecoder extends Decoder[Double] {
    override def decode(js: JsValue): Option[Double] = js match {
      case JsNumber(value) => Some(value)
      case _               => None
    }
  }

  implicit object IntDecoder extends Decoder[Int] {
    override def decode(js: JsValue): Option[Int] = js match {
      case JsNumber(value) => Some(value.toInt)
      case _               => None
    }
  }

  implicit object BooleanDecoder extends Decoder[Boolean] {
    override def decode(js: JsValue): Option[Boolean] = js match {
      case JsBoolean(value) => Some(value)
      case _                => None
    }
  }

  implicit def listDecoder[U: Decoder] = new Decoder[List[U]] {
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
