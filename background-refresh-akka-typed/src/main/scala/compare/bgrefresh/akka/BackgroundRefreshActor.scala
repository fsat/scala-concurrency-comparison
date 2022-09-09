package compare.bgrefresh.akka

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import compare.bgrefresh.akka.BackgroundRefreshActor.{ Message, MessageRequest, MessageSelf }
import compare.bgrefresh.interpreter.BackgroundRefreshAlgebra

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }

object BackgroundRefreshActor {
  object Message {
    final case class GetStateRequest(sender: ActorRef[GetStateResponse]) extends MessageRequest
    final case class GetStateResponse(state: List[Int])
  }

  object MessageSelf {
    case object Tick extends Tick
    sealed trait Tick extends MessageSelf

    final case class RefreshComplete(result: Try[List[Int]]) extends MessageSelf
  }

  sealed trait MessageSelf extends MessageRequest
  sealed trait MessageRequest extends Message
  sealed trait Message

}

class BackgroundRefreshActor()(implicit refreshInterpreter: BackgroundRefreshAlgebra[Future]) {
  def initialize(refreshInterval: FiniteDuration): Behavior[MessageRequest] =
    Behaviors.setup { ctx =>
      import ctx.executionContext
      val self = ctx.self
      val _ = ctx.system.scheduler.scheduleAtFixedRate(0.seconds, refreshInterval) { () =>
        self.tell(MessageSelf.Tick)
      }

      running(List.empty)
    }

  def running(state: List[Int]): Behavior[MessageRequest] =
    Behaviors.setup { ctx =>
      import ctx.log

      Behaviors.receiveMessage {
        case MessageSelf.Tick =>
          log.info("Refreshing")
          refreshing(state)

        case m: Message.GetStateRequest =>
          m.sender.tell(Message.GetStateResponse(state))
          Behaviors.same

        case _: MessageSelf.RefreshComplete =>
          Behaviors.same
      }
    }

  def refreshing(state: List[Int]): Behavior[MessageRequest] = {
    Behaviors.setup { ctx =>
      import ctx.log
      ctx.pipeToSelf(refreshInterpreter.refresh(state))(MessageSelf.RefreshComplete)

      Behaviors.receiveMessage {
        case m: MessageSelf.RefreshComplete =>
          m.result match {
            case Success(value) =>
              log.info("Refresh complete")
              running(value)

            case Failure(e) =>
              log.error(s"Refresh failed: ${e.getMessage}", e)
              running(state)
          }
        case m: Message.GetStateRequest =>
          m.sender.tell(Message.GetStateResponse(state))
          Behaviors.same

        case MessageSelf.Tick =>
          Behaviors.same
      }
    }
  }
}
