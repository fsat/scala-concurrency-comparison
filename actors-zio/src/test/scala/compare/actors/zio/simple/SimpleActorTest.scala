package compare.actors.zio.simple

import org.scalatest.{ BeforeAndAfterAll, Inside }
import org.scalatest.concurrent.{ Eventually, ScalaFutures }
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import zio.{ IO, Runtime, Schedule, Task, Unsafe, ZIO }
import zio.actors.{ ActorSystem, Supervisor }
import zio.logging.backend.SLF4J
import zio.test._
import zio.test.Assertion._

import java.util.UUID
import scala.concurrent.duration.DurationInt

class SimpleActorTest extends ZIOSpecDefault {
  //  override implicit def patienceConfig: PatienceConfig = PatienceConfig(timeout = 3.seconds)
  //
  //  val slf4j = SLF4J.slf4j
  //  val runtimeLayers = Runtime.removeDefaultLoggers ++ slf4j
  //  val runtime = Unsafe.unsafe { implicit unsafe =>
  //    Runtime.unsafe.fromLayer(runtimeLayers)
  //  }
  def spec =
    suite("foo") {
      test("foo") {
        for {
          _ <- ZIO.attempt(println("creating actor system"))
          actorSystem <- ActorSystem("test1")
          _ <- ZIO.attempt(println("make actor"))
          actor <- actorSystem.make(s"simple-actor-${UUID.randomUUID()}", Supervisor.none, 0, new SimpleActor)

          getState1 <- actor ? SimpleActor.Message.GetStateRequest()
          _ <- actor ! SimpleActor.Message.IncrementRequest()
          getState2 <- actor ? SimpleActor.Message.GetStateRequest()
        } yield {
          assertTrue(getState1 == 1)
          assertTrue(getState2 == 2)
        }
      }
    }

  //  it("increments the counter") {
  //    Unsafe.unsafe { implicit unsafe =>
  //      println("Make actor system")
  //      val actorSystem = runtime.unsafe.run(ActorSystem("test1")).getOrThrow()
  //
  //      println("Make actor")
  //      val actor = runtime.unsafe.runToFuture(actorSystem.make(s"simple-actor-${UUID.randomUUID()}", Supervisor.none, 0, new SimpleActor)).futureValue
  //
  //      println("Get state 1")
  //      val getState1 = runtime.unsafe.runToFuture(actor ? SimpleActor.Message.GetStateRequest())
  //      getState1.futureValue shouldBe 0
  //
  //      println("Increment")
  //      runtime.unsafe.runToFuture(actor ! SimpleActor.Message.IncrementRequest()).futureValue
  //
  //      println("Get state 2")
  //      val getState2 = runtime.unsafe.runToFuture(actor ? SimpleActor.Message.GetStateRequest()).futureValue
  //      getState2 shouldBe 1
  //    }
  //  }
}