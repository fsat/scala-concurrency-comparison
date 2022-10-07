package compare.actors.zio.simple

import zio._
import zio.test._

object HelloZioSpec extends ZIOSpecDefault {
  final case class Coffee(origin: String, roastLevel: Int, taste: Seq[String])
  override def spec = suite("hello")(
    test("bla") {
      for {
        result <- ZIO.attempt(Coffee("Rwandan gesovu", 50, Seq("berry", "chocolate")))
      } yield {
        assertTrue(result == Coffee("Brazilian mantequira", 23, Seq("berry", "caramel", "stone fruit")))
      }
    })
}
