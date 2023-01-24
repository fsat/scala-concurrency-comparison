package fsm.zio

import zio.{ Fiber, UIO }

class ProcessingLoop[State, MessageRequest](
  private[zio] val mailbox: StatefulMailbox[State, MessageRequest],
  private[zio] val fsm: FSM[State, MessageRequest],
  private[zio] val loopFiber: Fiber.Runtime[Throwable, Nothing]) {
  def stop(): UIO[Unit] = {
    ???
  }
}
