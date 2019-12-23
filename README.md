# みんな仲良く楽しいJson

この記事は [Scala Advent Calendar 2019](https://qiita.com/advent-calendar/2019/scala) の23日目の記事の代行です。

PlayFrameworkなどのWebフレームワークを使っていると、クライアントからJsonを受け取って処理することが多いですね。
普段なら [Play JSON](https://www.playframework.com/documentation/2.8.x/ScalaJson) や [Circe](https://circe.github.io/circe/) を使って処理をすれば良いのですが、これらのライブラリはどうやって実現されているのでしょう？
この記事では、Jsonライブラリを自作してその雰囲気を味わってみます。

## これから作るもの

Jsonを扱うには

* 文字列のJsonをJsonを表現した型に変換する(Parser)
* Jsonを表現した型を任意のcase classに変換する(Decoder)
* 任意のcase classをJsonを表現した型に変換する(Encoder)
* Jsonを表現した型を文字列に変換する(Formatter)

の四つの作業が必要なので、それぞれ実装します。

また、これらを実装する上で、

* パーサーコンビネーター
* 型クラス
* マクロ

を扱います。

まずはJsonを表現する型を用意します。以下では `JsValue` 型と呼ぶことにします。

## JsValue型を定義する

[JSON](https://www.json.org/json-ja.html) の構造は、ホワイトスペースや数値などの細かい表記を除くとこのような構造で表すことができます。

```scala
trait JsValue
case class JsString(value: String) extends JsValue
case class JsNumber(value: Double) extends JsValue
case class JsBoolean(value: Boolean) extends JsValue
case class JsObject(value: Map[String, JsValue]) extends JsValue
case class JsArray(value: Vector[JsValue]) extends JsValue
case object JsNull extends JsValue
```

このようにcase class定義することで、以下のJsonを上記のJsValue型で表現できます

```json
{
  "name": "Watership Down",
  "location": {
    "lat": 51.235685,
    "lng": -1.309197
  },
  "residents": [
    {
      "name": "Fiver",
      "age": 4,
      "role": null
    },
    {
      "name": "Bigwig",
      "age": 6,
      "role": "Owsla"
    }
  ]
}
```

```scala
JsObject(
  Map(
    "name" -> JsString("Watership Down"),
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
            "age" -> JsNumber(4.0),
            "role" -> JsNull
          )
        ),
        JsObject(
          Map(
            "name" -> JsString("Bigwig"),
            "age" -> JsNumber(6.0),
            "role" -> JsString("Owsla")
          )
        )
      )
    )
  )
)
```

`Map` や `Vector` を書くのが面倒なので、ファクトリメソッドを `Json` オブジェクトに生やします。

```scala
object Json {
  def str(value: String): JsValue    = JsString(value)
  def num(value: Double): JsValue    = JsNumber(value)
  def bool(value: Boolean): JsValue  = JsBoolean(value)
  def obj(value: (String, JsValue)*) = JsObject(value.toMap)
  def arr(value: JsValue*)           = JsArray(value.toVector)
  val nul                            = JsNull
}
```

これで

```scala
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
```

ちょっと簡単に書けるようになりました。

ネストしたJsValueの中にアクセスを出来るように便利メソッドを生やします。

```scala
trait JsValue {
  def \(key: String): JsPath
  def \(key: Int): JsPath
}
case class JsString(value: String) extends JsValue {
  override def \(key: String): JsPath      = JsPath(None)
  override def \(key: Int): JsPath         = JsPath(None)
}
case class JsNumber(value: Double) extends JsValue {
  override def \(key: String): JsPath      = JsPath(None)
  override def \(key: Int): JsPath         = JsPath(None)
}
case class JsBoolean(value: Boolean) extends JsValue {
  override def \(key: String): JsPath      = JsPath(None)
  override def \(key: Int): JsPath         = JsPath(None)
}
case class JsObject(value: Map[String, JsValue]) extends JsValue {
  override def \(key: String): JsPath      = JsPath(value.get(key))
  override def \(key: Int): JsPath         = JsPath(None)
}
case class JsArray(value: Vector[JsValue]) extends JsValue {
  override def \(key: String): JsPath      = JsPath(None)
  override def \(key: Int): JsPath         = JsPath(value.lift(key))
}
case object JsNull extends JsValue {
  override def \(key: String): JsPath      = JsPath(None)
  override def \(key: Int): JsPath         = JsPath(None)
}

case class JsPath(getOption: Option[JsValue]) {
  def \(key: String): JsPath = JsPath(getOption.map(_ \ key).flatMap(_.getOption))
  def \(key: Int): JsPath    = JsPath(getOption.map(_ \ key).flatMap(_.getOption))
}
```

これで次のようにJsValueの中のJsValueを取得できるようになりました。

```scala
val result: Option[JsValue] = (json \ "residents" \ 1 \ "role").getOption
```

## Parserを書く

では次にに文字列からJsValue型に変換するメソッドを書いてみます。

パーサーを1から全て実装するのは骨が折れるので、 Scalaの公式にある [scala-parser-combinators](https://github.com/scala/scala-parser-combinators) を利用します。

scala-parser-combinatorsには便利なメソッドなどがたくさん生えているので活用していきます。

`stringLiteral`, `floatingPointNumber` は `Parser[String]` 型で、正規表現でマッチした文字列を取得できます。
また、 Parser型に生えている `|` で複数のParserのいずれかにマッチ（文字列 `"true"` には暗黙の型変換が働いている）
`~` は複数のParserが連続しているものにマッチすることができます。
`repsep` は指定したものの繰り返しにマッチすることができます。

```scala
import scala.util.parsing.combinator._

object JsonParser extends JavaTokenParsers {
  lazy val value: Parser[Any] = obj | arr | stringLiteral | floatingPointNumber | "null" | "true" | "false"
  lazy val obj: Parser[Any] = "{" ~ repsep(member, ",") ~ "}"
  lazy val arr: Parser[Any] = "[" ~ repsep(value, ",") ~ "]"
  lazy val member: Parser[Any] = stringLiteral ~ ":" ~ value
}
```

この状態で `JsonParser` を叩いてみると、`ParseResult[Any]` というものが手に入ります。
`parseAll` は `JavaTokenParsers` の中で生えているものです。

```
scala> JsonParser.parseAll(JsonParser.value, """{"key": "value"}""")
res1: JsonParser.ParseResult[Any] = [1.17] parsed: (({~List((("key"~:)~"value")))~})
```


`Any` が帰ってくると何にも使えないので、パースの結果を `JsValue` にマッピングしていきます。
それぞれのParserに `^^` というメソッドを使うと、パースの結果をパターンマッチで任意の型に変換することができます。

```scala
import scala.util.parsing.combinator._

object JsonParser extends JavaTokenParsers {
  lazy val value: Parser[JsValue] = (
    obj |
      arr |
      dequotedStringLiteral ^^ {
        case string => JsString(string)
      } |
      floatingPointNumber ^^ {
        case number => JsNumber(number.toDouble)
      } |
      "null" ^^ {
        case _ => JsNull
      } |
      "true" ^^ {
        case _ => JsBoolean(true)
      } |
      "false" ^^ {
        case _ => JsBoolean(false)
      }
  )
  lazy val obj: Parser[JsObject] = "{" ~ repsep(member, ",") ~ "}" ^^ {
    case (_ ~ members ~ _) => JsObject(members.toMap)
  }
  lazy val arr: Parser[JsArray] = "[" ~ repsep(value, ",") ~ "]" ^^ {
    case (_ ~ values ~ _) => JsArray(values.to(Vector))
  }
  lazy val member: Parser[(String, JsValue)] = (dequotedStringLiteral ~ ":" ~ value) ^^ {
    case (string ~ _ ~ value) => (string, value)
  }

  lazy val dequotedStringLiteral: Parser[String] = stringLiteral ^^ { str =>
    str.substring(1, str.length - 1) // stringLiteral が前後の `"` も拾うので取り除く
  }
}
```

`value` の型を `Parser[JsValue]` にすることができました。

`case (_ ~ members ~ _) => ` の部分が見慣れない表記ですが、 `_` は普段のScalaのパターンマッチでの、利用しないという意味のもの
（実際は `"{"` がマッチするが不要）

`~` はパーサーコンビネータが定義している `~` という名前のcase classを中置記法で書いたものです。
（`case  ~(~(_, members), _) => ...` と同じ）

最後に `parseAll` の結果の `ParseResult[JsValue]` をパターンマッチで扱いやすい `Either` 等に変換します。
`parseAll` を叩くメソッドを `apply` に定義して扱いやすくします。

```scala
object JsonParser extends JavaTokenParsers {

  def apply(input: String): Either[String, JsValue] = parseAll(value, input) match {
    case Success(result, next) => Right(result)
    case Failure(msg, next)    => Left(msg)
    case Error(msg, next)      => Left(msg)
  }

  // ...
}
```


ユーティリティクラスの `Json` にも生やしておきます。

```scala
object Json {
  def parse(input: String): Either[String, JsValue] = JsonParser(input)
}
```

これで下記のテストが通るようになりました。

```scala
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
```

## Decoderを書く

`JsValue` 型を任意のcase classに変換したいですね。

```scala
case class Person(name: String, age: Int)

val json: JsValue = Json.obj(
  "name" -> Json.str("Bob"),
  "age"  -> Json.num(42)
)
```

があった時に、

```scala
val maybePerson: Option[Person] = json.as[Person]
```

みたいに出来るとカッコいいですね。やってみましょう

まず、任意の型をデコードできる型 `JsonDecoder[T]` を用意します

```scala
trait JsonDecoder[T] {
  def decode(js: JsValue): Option[T]
}
```

`String` にデコードするには

```scala
object StringDecoder extends JsonDecoder[String] {
  override def decode(js: JsValue): Option[String] = js match {
    case JsString(value) => Some(value)
    case _               => None
  }
}
```

を用意すれば

```scala
StringDecoder.decode(Json.str("Foo")) // Some(Foo)
```

のように変換できます。
同様に `IntDecoder`, `DoubleDecoder`, `BooleanDecoder` も用意します。

これらの基本的なDecoderは、 `object JsonDecoder` の中に置いておきます。

```scala
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

}
```

また、 `JsValue` の方に次のメソッドを生やします

```scala
trait JsValue {
  final def as[T](implicit d: JsonDecoder[T]): Option[T] = d.decode(this)
}
```

`JsonDecoder[T]` を暗黙のパラメータとして指定することで、
`object JsonDecoder` の中の `implicit` が指定されたオブジェクト等の中から `T` が合致するものを自動で渡されるようになります。

こうすることで、

```scala
val json: JsValue = Json.str("Foo")
val maybeStr: Option[String] = json.as[String]
```

が通るようになりました。

### 任意の型へのデコード

利用者がDecoderを定義すれば自由に任意の型へデコードできます

```scala
case class Person(
    name: String,
    age: Int
)

val personDecoder: Decoder[Person] = new Decoder[Person] {
  override def decode(js: JsValue): Option[Person] = js match {
    case JsObject(value) => 
      for {
        name      <- (obj \ "name").as[String]
        age       <- (obj \ "age").as[Int]
      } yield Person(name, age)
    case _ => None
  }
}

val json: JsValue = Json.obj(
  "name" -> Json.str("Bob"),
  "age"  -> Json.num(42)
)

val maybePerson: Option[Person] = json.as[Person](personDecoder)
```

次のようにDecoderに対して `implicit` を付けることで、
同一のスコープ内で `json.as[Person]` だけの記述でデコードを行うこともできます。

```scala
implicit val personDecoder: Decoder[Person] = ???
val maybePerson: Option[Person] = json.as[Person]
```

### マクロで自動でDecoderを作成する

上記の `Decoder[Person]` をわざわざ書くのが面倒なので、マクロで作ります

マクロを使う時のlibraryDependenciesへの追加。
マクロは標準で用意されているものの、別パッケージとなっているので読み込みます

project/Dependencies.scala

```scala
import sbt._

object Dependencies {
  // ...
  lazy val scalaRefrect = "org.scala-lang" % "scala-reflect" % "2.13.1"
}
```

build.sbt

```scala
lazy val root = (project in file("."))
  .settings(
    // ...
    libraryDependencies += scalaRefrect,
  )
```

呼び出す際の記述

src/main/scala/litejson/Json.scala

```scala
import language.experimental.macros

object Json {
  // ...
  def autoDecoder[T]: JsonDecoder[T] = macro JsonAutoDecoderImpl[T]
}
```

上記を用意しておいて、利用者側は

```scala
implicit val personDecoder: JsonDecoder[Person] = Json.autoDecoder[Person]
val maybePerson: Option[Person] = json.as[Person]
```

このような感じで使えるようにします。

マクロの実装

src/main/scala/litejson/JsonAutoDecoderImpl.scala

```scala
import scala.reflect.macros.whitebox.Context

object JsonAutoDecoderImpl {

  def apply[T: c.WeakTypeTag](c: Context): c.Expr[JsonDecoder[T]] = {
    import c.universe._

    val tag = weakTypeTag[T]

    case class Field(name: String, tpe: String) {
      def expression: String = {
        s"""$name <- (obj \\ "$name").as[$tpe]"""
      }
    }
    val fields: List[Field] = tag.tpe.typeSymbol.typeSignature.decls.toList.collect {
      case sym: TermSymbol if sym.isVal && sym.isCaseAccessor => {
        Field(sym.name.toString.trim, tq"${sym.typeSignature}".toString)
      }
    }

    val code: String =
      s"""
      |implicit object AnonAutoDecoder extends JsonDecoder[${tag.tpe.toString}] {
      |  override def decode(js: JsValue): Option[${tag.tpe.toString}] = js match {
      |    case obj: JsObject =>
      |      for {
      |        ${fields.map(_.expression).mkString("\n        ")}
      |      } yield ${tag.tpe.toString}(${fields.map(_.name).mkString(", ")})
      |    case _ => None
      |  }
      |}
      |AnonAutoDecoder
      |""".stripMargin

    c.Expr[JsonDecoder[T]](c.parse(code))

  }

}
```

指定されたT型のcase classのフィールド名と型名をそれぞれStringとして取得し、文字列操作で生成したいコードを作成しています。
(もっと良い方法がありそう)

めでたくこれで下記のコードが動くようになりました

```scala
case class Person(
    name: String,
    age: Int
)

val json: JsValue = Json.obj(
  "name" -> Json.str("Bob"),
  "age"  -> Json.num(42)
)

implicit val personDecoder: JsonDecoder[Person] = Json.autoDecoder[Person]
val maybePerson: Option[Person] = json.as[Person]
```


## エンコーダーを作る

逆に、case classからJsValueに変換します。
Decoderと同じように型クラスとマクロを使います


src/main/scala/litejson/JsonEncoder.scala
```scala
trait JsonEncoder[T] {
  def encode(value: T): JsValue
}

object JsonEncoder {

  implicit object StringEncoder extends JsonEncoder[String] {
    override def encode(value: String): JsValue = JsString(value)
  }

  // ...

}
```

src/main/scala/litejson/Json.scala
```scala
import language.experimental.macros

object Json {
  // ...
  def encode[T](value: T)(implicit encoder: JsonEncoder[T]): JsValue = encoder.encode(value)
  def autoEncoder[T]: JsonEncoder[T] = macro JsonAutoEncoderImpl[T]
}
```

マクロを使わない方法でエンコードをするには

```scala
case class Person(name: String, age: Int)
val value = Person("Bob", 42)

implicit val personEncoder extends JsonEncoder[Person] {
  override def encode(value: Person): JsValue = Json.obj(
    "name" -> Json.str(value.name),
    "age"  -> Json.num(value.age)
  )
}

val json: JsValue = Json.encode(value)
```

マクロを使う場合

src/main/scala/litejson/JsonAutoEncoderImpl.scala
```scala
import scala.reflect.macros.whitebox.Context

object JsonAutoEncoderImpl {

  def apply[T: c.WeakTypeTag](c: Context): c.Expr[JsonEncoder[T]] = {
    import c.universe._

    val tag = weakTypeTag[T]

    case class Field(name: String, tpe: String) {
      def expression: String = {
        s""""$name" -> Json.encode(value.$name)"""
      }
    }
    val fields: List[Field] = tag.tpe.typeSymbol.typeSignature.decls.toList.collect {
      case sym: TermSymbol if sym.isVal && sym.isCaseAccessor => {
        Field(sym.name.toString.trim, tq"${sym.typeSignature}".toString)
      }
    }

    val code: String =
      s"""
      |implicit object AnonAutoEncoder extends JsonEncoder[${tag.tpe.toString}] {
      |  override def encode(value: ${tag.tpe.toString}): JsValue = Json.obj(
      |    ${fields.map(_.expression).mkString(",\n    ")}
      |  )
      |}
      |AnonAutoEncoder
      |""".stripMargin

    c.Expr[JsonEncoder[T]](c.parse(code))
  }

}
```

めでたく下記のコードが動くようになりました

```scala
case class Person(name: String, age: Int)
val value = Person("Bob", 42)

implicit val personEncoder: Encoder[Person] = Json.autoEncoder[Person]
val json: JsValue = Json.encode(value)
```

## フォーマッタを作る

JsValueを文字列に変換します。

パターンマッチしながら再帰でガチャガチャするだけですね。

```scala
object JsonFormatter {

  def spaces2(js: JsValue): String = {
    def proc(js: JsValue, indent: Int): String = js match {
      case JsString(value)  => "\"" + value + "\""
      case JsNumber(value)  => if (value == value.toInt) "%d".format(value.toInt) else "%f".format(value)
      case JsBoolean(value) => if (value) "true" else "false"
      case JsObject(obj) =>
        "{\n" +
          obj
            .map {
              case (key, value) =>
                "  " * (indent + 1) + "\"" + key + "\": " + proc(value, indent + 1)
            }
            .mkString(",\n") + "\n" +
          "  " * indent + "}"
      case JsArray(arr) =>
        "[\n" +
          arr
            .map { value =>
              "  " * (indent + 1) + proc(value, indent + 1)
            }
            .mkString(",\n") + "\n" +
          "  " * indent + "]"
      case JsNull => "null"
    }
    proc(js, 0)
  }

}
```

Jsonオブジェクトにユーティリティを生やしておきます

```scala
object Json {
  // ...
  def format(value: JsValue): String = JsonFormatter.spaces2(value)
}
```

動作確認

```scala
import org.scalatest._

class FormatterSpec extends FunSuite with Matchers {

  test("format") {

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

    val result = Json.format(json)
    val answer = """
      |{
      |  "name": "Watership Down",
      |  "location": {
      |    "lat": 51.235685,
      |    "lng": -1.309197
      |  },
      |  "residents": [
      |    {
      |      "name": "Fiver",
      |      "age": 4,
      |      "role": null
      |    },
      |    {
      |      "name": "Bigwig",
      |      "age": 6,
      |      "role": "Owsla"
      |    }
      |  ]
      |}
      |""".stripMargin.trim

    result shouldEqual answer

  }

}
```

めでたく文字列に変換できました

## おわりに

いかがでしたか？

駆け足で進みましたが、Parser, Decoder, Encoder, Formatterが実装できました。
また、それらの実装の過程で、パーサーコンビネータ・型クラス・マクロを使いました。

なんとなく普段使っているJson系のライブラリの気持ちが分かるようになってきたのではないでしょうか？
Json以外の場所でも活用できそうなので、明日からのプログラミングが楽しくなりそうですね。

それでは、みんなで楽しいJsonライフを！
