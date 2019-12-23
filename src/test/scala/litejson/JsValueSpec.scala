package litejson

import org.scalatest._

class JsValueSpec extends FlatSpec with Matchers {

  val template: JsValue = JsObject(
    Map(
      "name" -> JsString("WaterShip Down"),
      "location" -> JsObject(
        Map(
          "lat" -> JsNumber(51.235685),
          "lng" -> JsNumber(-1.309197)
        )
      ),
      "residents" -> JsArray(
        Vector(
          JsObject(
            Map(
              "name" -> JsString("Fiver"),
              "age"  -> JsNumber(4),
              "role" -> JsNull
            )
          ),
          JsObject(
            Map(
              "name" -> JsString("Bigwig"),
              "age"  -> JsNumber(6),
              "role" -> JsString("Owsla")
            )
          )
        )
      )
    )
  )

  "JsValue" should "build" in {

    template.isInstanceOf[JsValue] shouldEqual true

  }

  "JsValue" should "traverse (1)" in {

    val result: Option[JsValue] = (template \ "location" \ "lng").getOption
    val answer: Option[JsValue] = Some(JsNumber(-1.309197))

    result shouldEqual answer

  }

  "JsValue" should "traverse (2)" in {

    val result: Option[JsValue] = (template \ "residents" \ 1 \ "role").getOption
    val answer: Option[JsValue] = Some(JsString("Owsla"))

    result shouldEqual answer

  }

  "JsValue" should "traverse (3)" in {

    val result: Option[JsValue] = (template \ "residents" \ 0 \ "role").getOption
    val answer: Option[JsValue] = Some(JsNull)

    result shouldEqual answer

  }

  "JsValue" should "traverse (4)" in {

    val result: Option[JsValue] = (template \ "name" \ "wrongkey!").getOption
    val answer: Option[JsValue] = None

    result shouldEqual answer

  }

  "JsPath" should "convert value" in {

    val reuslt: Option[String] = (template \ "name").as[String]
    val answer                 = Some("WaterShip Down")

    reuslt shouldEqual answer

  }

  "JsValue" should "get value throws exception (1)" in {

    val result: JsValue = template("residents")(0)("name")
    val answer          = JsString("Fiver")
    result shouldEqual answer

  }

  "JsValue" should "get value throws exception (2)" in {

    val json: JsValue = JsString("foo")
    intercept[NoSuchElementException] {
      json("bar")
    }

  }

  "JsValue" should "get value throws exception (3)" in {

    intercept[NoSuchElementException] {
      (template \ "location")("latitude")
    }

  }

}
