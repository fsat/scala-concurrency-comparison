package fsm.zio

import zio.{ Promise, Task, UIO }

object FSMRef {
  class Local[MessageRequest](engine: Engine[_, MessageRequest]) extends FSMRef[MessageRequest] {
    def tell(message: MessageRequest): UIO[Unit] =
      engine.tell(message)

    def ask[MessageResponse](createMessage: Promise[Throwable, MessageResponse] => MessageRequest): Task[MessageResponse] =
      engine.ask(createMessage)

    def stop(): UIO[Unit] = engine.stop()
  }

  class Self[MessageRequest](engine: Engine[_, MessageRequest]) extends FSMRef[MessageRequest] {
    def tell(message: MessageRequest): UIO[Unit] =
      engine.tell(message)

    def stop(): UIO[Unit] = engine.stop()
  }
}

sealed trait FSMRef[MessageRequest]
