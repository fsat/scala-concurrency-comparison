package compare.actors.zio.simple

import compare.actors.zio.simple.SimpleActor.Message
import zio.{ RIO, UIO, ZIO }
import zio.actors.Actor.Stateful
import zio.actors.Context

object SimpleActor {
  object Message {
    final case class GetStateRequest() extends Request[Int]
    final case class IncrementRequest() extends Request[NoReply]
    sealed trait Request[+A] extends Message
  }
  sealed trait Message extends Product with Serializable
}

class SimpleActor extends Stateful[Any, Int, Message.Request] {
  override def receive[A](state: Int, msg: Message.Request[A], context: Context): UIO[(Int, A)] = {
    msg match {
      case _: Message.IncrementRequest => ZIO.succeed((state + 1, NoReply))
      case _: Message.GetStateRequest => ZIO.succeed((state, state))
    }
  }
}
