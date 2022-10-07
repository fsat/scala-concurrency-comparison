package compare.actors.zio.simple

import zio._
import zio.actors.{ ActorSystem, Supervisor }
import zio.test._

import java.util.UUID

object SimpleActorSpec extends ZIOSpecDefault {
  def spec =
    suite("simple actor") {
      test("incrementing state") {
        for {
          _ <- Console.printLine("creating actor system")
          actorSystem <- ActorSystem("test1")
          _ <- Console.printLine("make actor")
          actor <- actorSystem.make(s"simple-actor-${UUID.randomUUID()}", Supervisor.none, 0, new SimpleActor)

          getState1 <- actor ? SimpleActor.Message.GetStateRequest()
          _ <- assertTrue(getState1 == 0)

          _ <- actor ! SimpleActor.Message.IncrementRequest()

          getState2 <- actor ? SimpleActor.Message.GetStateRequest()
          r2 <- assertTrue(getState2 == 1)
        } yield r2
      }
    }
}