package compare.actors.zio.simple

import zio._
import zio.test._

object HelloZioSpec extends ZIOSpecDefault {
  final case class Person(name: String, age: Int, hobbies: Seq[String])
  override def spec = suite("hello") {
    test("bla") {
      for {
        result <- ZIO.attempt(Person("james", 23, Seq("procrastinating", "gaming")))
      } yield {
        assertTrue(result == Person("richard", 45, Seq("gaming", "cooking")))
      }
    }
  }
}
