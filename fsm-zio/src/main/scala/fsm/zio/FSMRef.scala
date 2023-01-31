package fsm.zio

import zio.{ Promise, Ref, Task, UIO, ZIO }

object FSMRef {
  class Local[MessageRequest](processingLoop: ProcessingLoop[MessageRequest]) extends FSMRef[MessageRequest] {
    import processingLoop.mailbox

    def tell(message: MessageRequest): UIO[Unit] =
      mailbox.tell(message)

    def ask[MessageResponse](createMessage: Promise[Throwable, MessageResponse] => MessageRequest): Task[MessageResponse] =
      mailbox.ask(createMessage)

    def map[B](fn: B => MessageRequest): FSMRef[B] = new Map(this, fn)

    def stop(): UIO[Unit] = processingLoop.stop()

    def isStopped(): UIO[Boolean] = processingLoop.isStopped()
  }

  class Self[MessageRequest](
    mailbox: StatefulMailbox[_, MessageRequest],
    processingLoopRef: Ref[Option[ProcessingLoop[MessageRequest]]]) extends FSMRef[MessageRequest] {
    def tell(message: MessageRequest): UIO[Unit] =
      mailbox.tell(message)

    def map[B](fn: B => MessageRequest): FSMRef[B] = new Map(this, fn)

    def stop(): UIO[Unit] = {
      for {
        processingLoop <- processingLoopRef.get
        _ <- processingLoop match {
          case None => ZIO.succeed(())
          case Some(v) => v.stop()
        }
      } yield ()
    }
  }

  class Map[T, MessageRequest](ref: FSMRef[MessageRequest], transform: T => MessageRequest) extends FSMRef[T] {
    override def tell(message: T): UIO[Unit] = ref.tell(transform(message))

    override def stop(): UIO[Unit] = ref.stop()

    override def map[B](fn: B => T): FSMRef[B] = new Map(ref, fn.andThen(transform))
  }
}

sealed trait FSMRef[MessageRequest] {
  def tell(message: MessageRequest): UIO[Unit]
  def stop(): UIO[Unit]
  def map[B](fn: B => MessageRequest): FSMRef[B]
}
