package compare.bgrefresh.akka

import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.adapter._
import akka.util.Timeout
import compare.bgrefresh.interpreter.BackgroundRefreshInterpreter
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{ Eventually, ScalaFutures }
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration._

class BackgroundRefreshActorSpec extends AnyFunSpec with Matchers with Eventually with BeforeAndAfterAll with ScalaFutures {
  val actorSystem = ActorSystem()

  override def afterAll(): Unit = {
    Await.ready(actorSystem.terminate(), 10.seconds)
  }

  it("refreshes the list when the tick is sent") {
    implicit val scheduler = actorSystem.toTyped.scheduler
    import actorSystem.dispatcher

    implicit val timeout = Timeout(3.seconds)
    implicit val bgRefreshInterpreter = new BackgroundRefreshInterpreter
    val actor = actorSystem.toTyped.systemActorOf(new BackgroundRefreshActor().running(List.empty), s"bg-refresh-${UUID.randomUUID().toString}")

    val initialState = actor.ask(BackgroundRefreshActor.Message.GetStateRequest).futureValue
    initialState.state.isEmpty shouldBe true

    actor.tell(BackgroundRefreshActor.MessageSelf.Tick)

    eventually {
      val nextState = actor.ask(BackgroundRefreshActor.Message.GetStateRequest).futureValue
      nextState.state shouldBe List(0)
    }
  }
}
