package fsm.zio

import zio.{ Promise, Task, UIO }

object FSMRef {
  class Local[MessageRequest](engine: Engine[_, MessageRequest]) extends FSMRef[MessageRequest] {
    def tell(message: MessageRequest): UIO[Unit] =
      engine.tell(message)

    def ask[MessageResponse](createMessage: Promise[Throwable, MessageResponse] => MessageRequest): Task[MessageResponse] =
      engine.ask(createMessage)

    def map[B](fn: B => MessageRequest): FSMRef[B] = new Map(this, fn)

    def stop(): UIO[Unit] = engine.stop()
  }

  class Self[MessageRequest](engine: Engine[_, MessageRequest]) extends FSMRef[MessageRequest] {
    def tell(message: MessageRequest): UIO[Unit] =
      engine.tell(message)

    def map[B](fn: B => MessageRequest): FSMRef[B] = new Map(this, fn)

    def stop(): UIO[Unit] = engine.stop()
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
