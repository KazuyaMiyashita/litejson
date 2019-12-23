package litejson

import org.scalatest._

class AutoDecoderImplSpec extends FlatSpec with Matchers {

  "AutoDecoder" should "decode to case class (1)" in {

    case class Person(
        name: String,
        age: Int
    )

    val json: JsValue = Json.obj(
      "name" -> Json.str("Bob"),
      "age"  -> Json.num(42)
    )

    implicit val personDecoder: Decoder[Person] = Json.autoDecoder[Person]

    val reuslt: Option[Person] = json.as[Person]
    val answer                 = Some(Person("Bob", 42))

    reuslt shouldEqual answer

  }

  "AutoDecoder" should "decode to case class (2)" in {

    case class Person(
        name: String,
        age: Int,
        friend: Option[Int]
    )

    val json: JsValue = Json.obj(
      "name"   -> Json.str("Bob"),
      "age"    -> Json.num(42),
      "friend" -> Json.nul
    )

    implicit val personDecoder: Decoder[Person] = Json.autoDecoder[Person]

    val reuslt: Option[Person] = json.as[Person]
    val answer                 = Some(Person("Bob", 42, None))

    reuslt shouldEqual answer

  }

  "AutoDecoder" should "decode to case class (3)" in {

    case class Person(
        name: String,
        age: Int,
        friend: Option[Person]
    )

    val json: JsValue = Json.obj(
      "name"   -> Json.str("Bob"),
      "age"    -> Json.num(42),
      "friend" -> Json.nul
    )

    implicit val personDecoder: Decoder[Person] = Json.autoDecoder[Person]

    val reuslt: Option[Person] = json.as[Person]
    val answer                 = Some(Person("Bob", 42, None))

    reuslt shouldEqual answer

  }

  "AutoDecoder" should "decode to case class (4)" in {

    case class Person(
        name: String,
        age: Int,
        friends: List[Person]
    )

    val json: JsValue = Json.obj(
      "name"    -> Json.str("Bob"),
      "age"     -> Json.num(42),
      "friends" -> Json.arr()
    )

    implicit val personDecoder: Decoder[Person] = Json.autoDecoder[Person]

    val reuslt: Option[Person] = json.as[Person]
    val answer                 = Some(Person("Bob", 42, Nil))

    reuslt shouldEqual answer

  }

  "AutoDecoder" should "decode complex case class" in {

    case class Foo(
        name: String,
        location: Location,
        residents: List[Resident]
    )
    case class Location(lat: Double, lng: Double)
    case class Resident(name: String, age: Int, role: Option[String])

    val json: JsValue = Json.obj(
      "name" -> Json.str("Watership Down"),
      "location" -> Json.obj(
        "lat" -> Json.num(51.235685),
        "lng" -> Json.num(-1.309197)
      ),
      "residents" -> Json.arr(
        Json.obj(
          "name" -> Json.str("Fiver"),
          "age"  -> Json.num(4),
          "role" -> Json.nul
        ),
        Json.obj(
          "name" -> Json.str("Bigwig"),
          "age"  -> Json.num(6),
          "role" -> Json.str("Owsla")
        )
      )
    )

    implicit val locationDecoder: Decoder[Location] = Json.autoDecoder[Location]
    implicit val residentDecoder: Decoder[Resident] = Json.autoDecoder[Resident]
    implicit val fooDecoder: Decoder[Foo]           = Json.autoDecoder[Foo]
    val result: Option[Foo]                         = json.as[Foo]
    val answer = Some(
      Foo(
        "Watership Down",
        Location(51.235685, -1.309197),
        List(
          Resident("Fiver", 4, None),
          Resident("Bigwig", 6, Some("Owsla"))
        )
      )
    )

    result shouldEqual answer

  }

  // TODO: これできないのはしゃあないか
  // "AutoDecoder" should "decode recursive classes" in {

  //   case class Foo(bar: Option[Bar])
  //   case class Bar(foo: Option[Foo])

  //   val fooJson: JsValue = Json.obj(
  //     "bar" -> Json.nul
  //   )
  //   val barJson: JsValue = Json.obj(
  //     "foo" -> Json.nul
  //   )

  //   implicit val fooDecoder: Decoder[Foo] = Json.autoDecoder[Foo]
  //   implicit val barDecoder: Decoder[Bar] = Json.autoDecoder[Bar]

  //   val fooReuslt: Option[Foo] = fooJson.as[Foo]
  //   val fooAnswer              = Some(None)
  //   fooReuslt shouldEqual fooAnswer

  //   val barReuslt: Option[Bar] = barJson.as[Bar]
  //   val barAnswer              = Some(None)
  //   barReuslt shouldEqual barAnswer

  // }

}
