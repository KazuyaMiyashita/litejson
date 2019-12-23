package litejson

trait JsValue {
  def apply(key: String): JsValue
  def apply(key: Int): JsValue
  def \(key: String): JsPath
  def \(key: Int): JsPath
  final def as[T](implicit c: Decoder[T]): Option[T]            = c.decode(this)
  final def asOpt[T](implicit c: Decoder[T]): Option[Option[T]] = c.decodeOpt(this)
}
case class JsString(value: String) extends JsValue {
  override def apply(key: String): JsValue = throw new NoSuchElementException
  override def apply(key: Int): JsValue    = throw new NoSuchElementException
  override def \(key: String): JsPath      = JsPath(None)
  override def \(key: Int): JsPath         = JsPath(None)
}
case class JsNumber(value: Double) extends JsValue {
  override def apply(key: String): JsValue = throw new NoSuchElementException
  override def apply(key: Int): JsValue    = throw new NoSuchElementException
  override def \(key: String): JsPath      = JsPath(None)
  override def \(key: Int): JsPath         = JsPath(None)
}
case class JsBoolean(value: Boolean) extends JsValue {
  override def apply(key: String): JsValue = throw new NoSuchElementException
  override def apply(key: Int): JsValue    = throw new NoSuchElementException
  override def \(key: String): JsPath      = JsPath(None)
  override def \(key: Int): JsPath         = JsPath(None)
}
case class JsObject(value: Map[String, JsValue]) extends JsValue {
  override def apply(key: String): JsValue = value.get(key).get
  override def apply(key: Int): JsValue    = throw new NoSuchElementException
  override def \(key: String): JsPath      = JsPath(value.get(key))
  override def \(key: Int): JsPath         = JsPath(None)
}
case class JsArray(value: Vector[JsValue]) extends JsValue {
  override def apply(key: String): JsValue = throw new NoSuchElementException
  override def apply(key: Int): JsValue    = value(key)
  override def \(key: String): JsPath      = JsPath(None)
  override def \(key: Int): JsPath         = JsPath(value.lift(key))
}
case object JsNull extends JsValue {
  override def apply(key: String): JsValue = throw new NoSuchElementException
  override def apply(key: Int): JsValue    = throw new NoSuchElementException
  override def \(key: String): JsPath      = JsPath(None)
  override def \(key: Int): JsPath         = JsPath(None)
}
