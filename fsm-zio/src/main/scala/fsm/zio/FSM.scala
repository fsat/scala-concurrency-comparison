package fsm.zio

import zio._

trait FSM[State, MessageRequest] {
  def apply(state: State, message: MessageRequest): UIO[State]
}
