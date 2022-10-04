package compare.fsm.zio.simple

import compare.fsm.zio.simple.Engine.PendingMessage
import zio.{ IO, Promise, Queue, Ref, Schedule, Task, UIO, Unsafe, ZIO }

import java.util.concurrent.TimeUnit

object Engine {
  object PendingMessage {
    final case class Tell[MessageRequest](request: MessageRequest) extends PendingMessage
    final case class Ask[MessageRequest, MessageResponse](
      request: MessageRequest,
      replyTo: Promise[Throwable, Option[MessageResponse]]) extends PendingMessage
  }
  sealed trait PendingMessage extends Product with Serializable

  def create[State, MessageRequest, MessageResponse](
    state: State,
    fsm: FSM[State, MessageRequest, MessageResponse]): Task[Engine[State, MessageRequest, MessageResponse]] = {
    for {
      mailbox <- Queue.unbounded[PendingMessage]
      s <- Ref.make(state)
      engine = new Engine(mailbox, s, fsm)
      _ <- processMessage(engine)
        .repeat(Schedule.spaced(zio.Duration(10, TimeUnit.MILLISECONDS)))
        // Must we fork on the global supervisor?
        .forkDaemon
    } yield engine
  }

  private def processMessage[State, MessageRequest, MessageResponse](engine: Engine[State, MessageRequest, MessageResponse]): Task[Unit] = {
    import engine._
    val t = for {
      pendingMessage <- mailbox.take
      s <- state.get
      _ <- pendingMessage match {
        case m: PendingMessage.Tell[MessageRequest @unchecked] =>
          for {
            r <- fsm.apply(s, m.request)
            (stateNext, _) = r
            _ <- state.set(stateNext)
          } yield ()

        case m: PendingMessage.Ask[MessageRequest @unchecked, MessageResponse @unchecked] =>
          for {
            r <- fsm.apply(s, m.request)
            (stateNext, response) = r
            _ <- m.replyTo.succeed(response)
            _ <- state.set(stateNext)
          } yield ()
      }
    } yield ()

    t
  }

}

class Engine[State, MessageRequest, MessageResponse](
  private[simple] val mailbox: Queue[PendingMessage],
  private[simple] val state: Ref[State],
  private[simple] val fsm: FSM[State, MessageRequest, MessageResponse]) {
  def tell(message: MessageRequest): UIO[Unit] = {
    for {
      _ <- mailbox.offer(PendingMessage.Tell(message))
    } yield ()
  }

  def ask(message: MessageRequest): Task[Option[MessageResponse]] = {
    for {
      p <- Promise.make[Throwable, Option[MessageResponse]]
      _ <- mailbox.offer(PendingMessage.Ask(message, p))
      result <- p.await
    } yield result
  }

}
