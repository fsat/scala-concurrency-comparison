package compare.bgrefresh.akka

import akka.actor.ActorSystem
import akka.actor.typed.{ ActorRef, Behavior }
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
  override implicit def patienceConfig: PatienceConfig = PatienceConfig(timeout = 3.seconds)

  implicit val timeout = Timeout(3.seconds)
  val actorSystem = ActorSystem()
  implicit val scheduler = actorSystem.toTyped.scheduler
  implicit val bgRefreshInterpreter = new BackgroundRefreshInterpreter

  override def afterAll(): Unit = {
    Await.ready(actorSystem.terminate(), 10.seconds)
  }

  it("refreshes the list when the tick is sent") {
    val f = testFixture(
      new BackgroundRefreshActor().running(List.empty))
    import f._

    val initialState = actor.ask(BackgroundRefreshActor.Message.GetStateRequest).futureValue
    initialState.state.isEmpty shouldBe true

    actor.tell(BackgroundRefreshActor.MessageSelf.Tick)

    eventually {
      val nextState = actor.ask(BackgroundRefreshActor.Message.GetStateRequest).futureValue
      nextState.state shouldBe List(0)
    }
  }

  it("auto refreshes the list") {
    val f = testFixture(
      behavior = new BackgroundRefreshActor().initialize(200.millis))
    import f._

    eventually {
      val nextState = actor.ask(BackgroundRefreshActor.Message.GetStateRequest).futureValue
      nextState.state shouldBe List(0, 1)
    }
  }

  def testFixture(behavior: Behavior[BackgroundRefreshActor.MessageRequest]) = new {
    val actor = actorSystem.toTyped.systemActorOf(behavior, s"bg-refresh-${UUID.randomUUID().toString}")
  }
}
