package compare.actors.zio.simple

import zio._
import zio.test._

object HelloZioSpec extends ZIOSpecDefault {
  override def spec = suite("hello") {
    test("bla") {
      for {
        result <- ZIO.attempt("bla")
      } yield {
        assertTrue(result == "bla")
      }
    }
  }
}
