package fsm.zio

import zio.{ Fiber, Ref, UIO }

class ProcessingLoop[MessageRequest](
  private[zio] val mailbox: StatefulMailbox[_, MessageRequest],
  private[zio] val fsm: FSM[_, MessageRequest],
  private[zio] val loopFiber: Fiber.Runtime[Throwable, Nothing]) {
  def stop(): UIO[Unit] = {
    for {
      _ <- mailbox.stop()
      _ <- loopFiber.interrupt
    } yield ()
  }
}
