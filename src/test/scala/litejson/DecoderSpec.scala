package litejson

import org.scalatest._

class DecoderSpec extends FlatSpec with Matchers {

  "Decoder" should "decode string value" in {

    val json: JsValue          = Json.str("string value")
    val reuslt: Option[String] = json.as[String]
    val answer                 = Some("string value")

    reuslt shouldEqual answer

  }

  "Decoder" should "decode double value" in {

    val json: JsValue          = Json.num(42)
    val reuslt: Option[Double] = json.as[Double]
    val answer                 = Some(42)

    reuslt shouldEqual answer

  }

  "Decoder" should "decode boolean value" in {

    val json: JsValue           = Json.bool(false)
    val reuslt: Option[Boolean] = json.as[Boolean]
    val answer                  = Some(false)

    reuslt shouldEqual answer

  }

  "Custom Decoder" should "decode custom format" in {

    import scala.util.Try
    import java.time.LocalDateTime

    implicit object LocalDateTimeConverter extends JsonDecoder[LocalDateTime] {
      def ldtconvert(str: String): Option[LocalDateTime] = {
        Try(LocalDateTime.parse(str)).toOption
      }
      override def decode(js: JsValue): Option[LocalDateTime] = js match {
        case JsString(value) => ldtconvert(value)
        case _               => None
      }
    }

    val json: JsValue                 = JsString("2007-12-03T10:15:30")
    val reuslt: Option[LocalDateTime] = json.as[LocalDateTime]
    val answer                        = Some(LocalDateTime.of(2007, 12, 3, 10, 15, 30))

    reuslt shouldEqual answer

  }

  "Decoder" should "decode List[String] value (1)" in {

    val json: JsValue = Json.arr(
      Json.str("first"),
      Json.str("second"),
      Json.str("third")
    )
    val reuslt: Option[List[String]] = json.as[List[String]]
    val answer                       = Some("first" :: "second" :: "third" :: Nil)

    reuslt shouldEqual answer

  }

  "Decoder" should "decode List[String] value (2)" in {

    val json: JsValue = Json.arr(
      Json.str("first"),
      Json.str("second"),
      Json.bool(true), // こいつが型が合わないのでNoneになる
      Json.str("third")
    )
    val reuslt: Option[List[String]] = json.as[List[String]]
    val answer                       = None

    reuslt shouldEqual answer

  }

  "Decoder" should "decodeOpt (1)" in {

    val json: JsValue = Json.obj(
      "key" -> Json.str("value")
    )
    val result: Option[Option[String]] = (json \ "key").asOpt[String]
    val answer: Option[Option[String]] = Some(Some("value"))

    result shouldEqual answer

  }

  "Decoder" should "decodeOpt (2)" in {

    val json: JsValue = Json.obj(
      "key" -> Json.nul
    )
    val result: Option[Option[String]] = (json \ "key").asOpt[String]
    val answer: Option[Option[String]] = Some(None)

    result shouldEqual answer

  }

  "Decoder" should "decodeOpt (3)" in {

    val json: JsValue                  = Json.obj()
    val result: Option[Option[String]] = (json \ "key").asOpt[String]
    val answer: Option[Option[String]] = Some(None)

    result shouldEqual answer

  }

  "Decoder" should "decodeOpt (4)" in {

    val json: JsValue                  = Json.nul
    val result: Option[Option[String]] = (json \ "key").asOpt[String]
    val answer: Option[Option[String]] = Some(None)

    result shouldEqual answer

  }

  "Decoder" should "decode to case class (1)" in {

    case class Person(
        name: String,
        age: Int,
        favorites: List[String],
        child: Option[Person]
    )

    val json: JsValue = Json.obj(
      "name" -> Json.str("Bob"),
      "age"  -> Json.num(42),
      "favorites" -> Json.arr(
        Json.str("Apple"),
        Json.str("Banana")
      ),
      "child" -> Json.nul
    )

    implicit object PersonDecoder extends JsonDecoder[Person] {
      override def decode(js: JsValue): Option[Person] = js match {
        case obj: JsObject =>
          for {
            name      <- (obj \ "name").as[String]
            age       <- (obj \ "age").as[Int]
            favorites <- (obj \ "favorites").as[List[String]]
            child     <- (obj \ "child").asOpt[Person]
          } yield Person(name, age, favorites, child)
        case _ => None
      }
    }

    val reuslt: Option[Person] = json.as[Person]
    val answer                 = Some(Person("Bob", 42, List("Apple", "Banana"), None))

    reuslt shouldEqual answer

  }

  "Decoder" should "decode to case class (2)" in {

    case class Person(
        name: String,
        age: Int,
        favorites: List[String],
        child: Option[Person]
    )

    val json: JsValue = Json.obj(
      "name" -> Json.str("Bob"),
      "age"  -> Json.num(42),
      "favorites" -> Json.arr(
        JsString("Apple"),
        JsString("Banana")
      ),
      "child" -> Json.obj(
        "name"      -> Json.str("Alice"),
        "age"       -> Json.num(24),
        "favorites" -> Json.arr(),
        "child"     -> Json.nul
      )
    )

    implicit object PersonDecoder extends JsonDecoder[Person] {
      override def decode(js: JsValue): Option[Person] = js match {
        case obj: JsObject =>
          for {
            name      <- (obj \ "name").as[String]
            age       <- (obj \ "age").as[Int]
            favorites <- (obj \ "favorites").as[List[String]]
            child     <- (obj \ "child").asOpt[Person]
          } yield Person(name, age, favorites, child)
        case _ => None
      }
    }

    val reuslt: Option[Person] = json.as[Person]
    val answer = {
      val child = Some(Person("Alice", 24, Nil, None))
      Some(Person("Bob", 42, List("Apple", "Banana"), child))
    }

    reuslt shouldEqual answer

  }

}
