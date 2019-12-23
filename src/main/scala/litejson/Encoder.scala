package litejson

trait Encoder[T] {
  def encode(value: T): JsValue
}

object Encoder {

  implicit object StringEncoder extends Encoder[String] {
    override def encode(value: String): JsValue = JsString(value)
  }

  implicit object DoubleEncoder extends Encoder[Double] {
    override def encode(value: Double): JsValue = JsNumber(value)
  }

  implicit object IntEncoder extends Encoder[Int] {
    override def encode(value: Int): JsValue = JsNumber(value)
  }

  implicit object BooleanEncoder extends Encoder[Boolean] {
    override def encode(value: Boolean): JsValue = JsBoolean(value)
  }

  implicit def iterableEncoder[U: Encoder, Iter[U] <: Iterable[U]] = new Encoder[Iter[U]] {
    override def encode(value: Iter[U]): JsValue = JsArray(value.map(Json.encode(_)).toVector)
  }

  implicit object NilEncoder extends Encoder[Nil.type] {
    override def encode(value: Nil.type): JsValue = JsArray(Vector.empty)
  }

  implicit def optionEncoder[U: Encoder, Opt[U] <: Option[U]] = new Encoder[Opt[U]] {
    override def encode(value: Opt[U]): JsValue = value match {
      case Some(v) => Json.encode(v)
      case None    => JsNull
    }
  }

  implicit object NoneEncoder extends Encoder[None.type] {
    override def encode(value: None.type): JsValue = JsNull
  }

}
