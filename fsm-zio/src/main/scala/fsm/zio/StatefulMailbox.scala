package fsm.zio

import fsm.zio.StatefulMailbox.PendingMessage
import zio._

object StatefulMailbox {
  object PendingMessage {
    final case class Tell[MessageRequest](request: MessageRequest) extends PendingMessage
    final case class Ask[MessageRequest](request: MessageRequest) extends PendingMessage
  }
  sealed trait PendingMessage extends Product with Serializable

  def create[State, MessageRequest](
    state: State,
    fsm: FSM[State, MessageRequest],
    mailboxSize: Int = 32000): UIO[FSMRef.Local[MessageRequest]] = {
    for {
      messageQueue <- Queue.dropping[PendingMessage](mailboxSize)
      s <- Ref.make(state)
      statefulMailbox = new StatefulMailbox[State, MessageRequest](messageQueue, s)

      // Run the queue processing loop in parallel in the background
      processingLoop <- processMessage(statefulMailbox, fsm)
    } yield new FSMRef.Local(processingLoop)
  }

  private def processMessage[State, MessageRequest](
    mailbox: StatefulMailbox[State, MessageRequest],
    fsm: FSM[State, MessageRequest]): UIO[ProcessingLoop[MessageRequest]] = {
    import mailbox._
    def processSingleMessage(processingLoopRef: Ref[Option[ProcessingLoop[MessageRequest]]]): UIO[Unit] =
      for {
        ctx <- ZIO.succeed(new FSMContext(new FSMRef.Self(mailbox, processingLoopRef)))
        pendingMessage <- messageQueue.take
        s <- state.get
        stateNext <- pendingMessage match {
          case m: PendingMessage.Tell[MessageRequest @unchecked] => fsm.apply(s, m.request, ctx)
          case m: PendingMessage.Ask[MessageRequest @unchecked] => fsm.apply(s, m.request, ctx)
        }
        _ <- state.set(stateNext)
      } yield ()

    for {
      processingLoopRef <- Ref.make(Option.empty[ProcessingLoop[MessageRequest]])

      parallelScope <- Scope.makeWith(ExecutionStrategy.Parallel)
      processingLoopFiber <- processSingleMessage(processingLoopRef).forever.forkIn(parallelScope)

      processingLoop = new ProcessingLoop(mailbox, fsm, processingLoopFiber)
      _ <- processingLoopRef.set(Some(processingLoop))
    } yield processingLoop
  }

}

class StatefulMailbox[State, MessageRequest](
  private[zio] val messageQueue: Queue[PendingMessage],
  private[zio] val state: Ref[State]) {

  private[zio] def tell(message: MessageRequest): UIO[Unit] = {
    for {
      _ <- messageQueue.offer(PendingMessage.Tell(message))
    } yield ()
  }

  private[zio] def ask[MessageResponse](createMessage: Promise[Throwable, MessageResponse] => MessageRequest): Task[MessageResponse] = {
    for {
      p <- Promise.make[Throwable, MessageResponse]
      m <- ZIO.attempt(createMessage(p))
      _ <- messageQueue.offer(PendingMessage.Ask(m))
      result <- p.await
    } yield result
  }

  def stop(): UIO[Unit] = {
    for {
      _ <- messageQueue.takeAll
      _ <- messageQueue.shutdown
    } yield ()
  }
}
