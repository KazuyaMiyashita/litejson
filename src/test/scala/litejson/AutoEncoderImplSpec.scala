package litejson

import org.scalatest._

class AutoEncoderImplSpec extends FlatSpec with Matchers {

  "AutoEncoder" should "encode case class (1)" in {

    case class Person(name: String, age: Int)
    val value = Person("Bob", 42)

    val result: JsValue = Json.autoEncoder[Person].encode(value)
    val answer: JsValue = Json.obj(
      "name" -> Json.str("Bob"),
      "age"  -> Json.num(42)
    )

    result shouldEqual answer

  }

  "AutoEncoder" should "encode case class (2)" in {

    case class Person(
        name: String,
        age: Int,
        friend: Option[Int]
    )
    val value = Person("Bob", 42, None)

    val result: JsValue = Json.autoEncoder[Person].encode(value)
    val answer: JsValue = Json.obj(
      "name"   -> Json.str("Bob"),
      "age"    -> Json.num(42),
      "friend" -> Json.nul
    )

    result shouldEqual answer

  }

  "AutoEncoder" should "encode case class (3)" in {

    case class Person(
        name: String,
        age: Int,
        friend: Option[Person]
    )
    val value = Person("Bob", 42, None)

    val result: JsValue = Json.autoEncoder[Person].encode(value)
    val answer: JsValue = Json.obj(
      "name"   -> Json.str("Bob"),
      "age"    -> Json.num(42),
      "friend" -> Json.nul
    )

    result shouldEqual answer

  }

  "AutoEncoder" should "encode case class (4)" in {

    case class Person(
        name: String,
        age: Int,
        friends: List[Person]
    )
    val value = Person("Bob", 42, Nil)

    val result: JsValue = Json.autoEncoder[Person].encode(value)
    val answer: JsValue = Json.obj(
      "name"    -> Json.str("Bob"),
      "age"     -> Json.num(42),
      "friends" -> Json.arr()
    )

    result shouldEqual answer

  }

  "AutoEncoder" should "decode complex case class" in {

    case class Foo(
        name: String,
        location: Location,
        residents: List[Resident]
    )
    case class Location(lat: Double, lng: Double)
    case class Resident(name: String, age: Int, role: Option[String])

    val value = Foo(
      "Watership Down",
      Location(51.235685, -1.309197),
      List(
        Resident("Fiver", 4, None),
        Resident("Bigwig", 6, Some("Owsla"))
      )
    )

    implicit val locationEncoder = Json.autoEncoder[Location]
    implicit val residentEncoder = Json.autoEncoder[Resident]
    val result                   = Json.autoEncoder[Foo].encode(value)

    val answer: JsValue = Json.obj(
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

    result shouldEqual answer

  }

  // TODO: これできないのはしゃあないか
  // "AutoDecoder" should "decode recursive classes" in {

  //   case class Foo(bar: Option[Bar])
  //   case class Bar(foo: Option[Foo])

  //   val foo = Some(None)
  //   val bar = Some(None)

  //   implicit val fooEncoder: JsonEncoder[Foo] = Json.autoEncoder[Foo]
  //   implicit val barEncoder: JsonEncoder[Bar] = Json.autoEncoder[Bar]

  //   val fooResult: JsValue = Json.encode(foo)
  //   val barResult: JsValue = Json.encode(bar)

  //   val fooAnswer = Json.obj(
  //     "bar" -> Json.nul
  //   )
  //   fooResult shouldEqual fooAnswer

  //   val barAnswer = Json.obj(
  //     "foo" -> Json.nul
  //   )
  //   barResult shouldEqual barAnswer

  // }

}
