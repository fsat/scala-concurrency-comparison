package fsm.zio

import zio.{ Promise, Task, UIO }

object FSMRef {
  class Local[State, MessageRequest](engine: Engine[State, MessageRequest]) extends FSMRef[State, MessageRequest] {
    def tell(message: MessageRequest): UIO[Unit] =
      engine.tell(message)

    def ask[MessageResponse](createMessage: Promise[Throwable, MessageResponse] => MessageRequest): Task[MessageResponse] =
      engine.ask(createMessage)

    def stop(): UIO[Unit] = engine.stop()
  }

  class Self[State, MessageRequest](engine: Engine[State, MessageRequest]) extends FSMRef[State, MessageRequest] {
    def tell(message: MessageRequest): UIO[Unit] =
      engine.tell(message)

    def stop(): UIO[Unit] = engine.stop()
  }
}

sealed trait FSMRef[State, MessageRequest]
