package compare.fsm.zio.simple

import compare.fsm.zio.simple.Engine.PendingMessage
import zio.{ ExecutionStrategy, IO, Promise, Queue, Ref, Schedule, Scope, Task, UIO, Unsafe, ZIO }

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{ DurationInt, FiniteDuration }

object Engine {
  object PendingMessage {
    final case class Tell[MessageRequest[+_], MessageResponse](request: MessageRequest[MessageResponse]) extends PendingMessage
    final case class Ask[MessageRequest[+_], MessageResponse](
      request: MessageRequest[MessageResponse],
      replyTo: Promise[Throwable, MessageResponse]) extends PendingMessage
  }
  sealed trait PendingMessage extends Product with Serializable

  def create[State, MessageRequest[+_]](
    state: State,
    fsm: FSM[State, MessageRequest],
    mailboxSize: Int = 32000,
    mailboxPollDuration: FiniteDuration = 10.millis): Task[Engine[State, MessageRequest]] = {

    def processMessage[MessageResponse](engine: Engine[State, MessageRequest]): Task[Unit] = {
      for {
        pendingMessage <- engine.mailbox.take
        s <- engine.state.get
        _ <- pendingMessage match {
          case m: PendingMessage.Tell[MessageRequest @unchecked, MessageResponse @unchecked] =>
            for {
              r <- fsm.apply(s, m.request)
              (stateNext, _) = r
              _ <- engine.state.set(stateNext)
            } yield ()

          case m: PendingMessage.Ask[MessageRequest @unchecked, MessageResponse @unchecked] =>
            for {
              r <- fsm.apply(s, m.request)
              (stateNext, response) = r
              _ <- m.replyTo.succeed(response)
              _ <- engine.state.set(stateNext)
            } yield ()
        }
      } yield ()
    }

    for {
      mailbox <- Queue.dropping[PendingMessage](mailboxSize)
      s <- Ref.make(state)
      engine = new Engine(mailbox, s, fsm)

      // Run the queue processing loop in parallel in the background
      parallelScope <- Scope.makeWith(ExecutionStrategy.Parallel)
      _ <- processMessage(engine)
        .repeat(Schedule.spaced(zio.Duration(mailboxPollDuration.toMillis, TimeUnit.MILLISECONDS)))
        .forkIn(parallelScope)
    } yield engine
  }

}

class Engine[State, MessageRequest[+_]](
  private[simple] val mailbox: Queue[PendingMessage],
  private[simple] val state: Ref[State],
  private[simple] val fsm: FSM[State, MessageRequest]) {
  def tell[MessageResponse](message: MessageRequest[MessageResponse]): UIO[Unit] = {
    for {
      _ <- mailbox.offer(PendingMessage.Tell(message))
    } yield ()
  }

  def ask[MessageResponse](message: MessageRequest[MessageResponse]): Task[MessageResponse] = {
    for {
      p <- Promise.make[Throwable, MessageResponse]
      _ <- mailbox.offer(PendingMessage.Ask(message, p))
      result <- p.await
    } yield result
  }

}
