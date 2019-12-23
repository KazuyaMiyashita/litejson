package litejson

import org.scalatest._

class ParserSpec extends FunSuite with Matchers {

  test("parse") {

    val input = """
    |{
    |  "name" : "Watership Down",
    |  "location" : {
    |    "lat" : 51.235685,
    |    "lng" : -1.309197
    |  },
    |  "residents" : [ {
    |    "name" : "Fiver",
    |    "age" : 4,
    |    "role" : null
    |  }, {
    |    "name" : "Bigwig",
    |    "age" : 6,
    |    "role" : "Owsla"
    |  } ]
    |}
    |""".stripMargin

    val result: Either[String, JsValue] = Json.parse(input)
    val answer: Either[String, JsValue] = Right(
      Json.obj(
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
    )

    result shouldEqual answer

  }

}
