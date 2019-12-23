package litejson

case class JsPath(getOption: Option[JsValue]) {
  def apply(key: String): JsValue                               = getOption.get.apply(key)
  def apply(key: Int): JsValue                                  = getOption.get.apply(key)
  def \(key: String): JsPath                                    = JsPath(getOption.map(_ \ key).flatMap(_.getOption))
  def \(key: Int): JsPath                                       = JsPath(getOption.map(_ \ key).flatMap(_.getOption))
  final def as[T](implicit c: Decoder[T]): Option[T]            = getOption.flatMap(c.decode(_))
  final def asOpt[T](implicit c: Decoder[T]): Option[Option[T]] = Some(getOption.flatMap(c.decodeOpt(_)).flatten)
}
