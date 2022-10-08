package fsm.zio

import zio.Task

trait FSM[State, MessageRequest] {
  def apply(state: State, message: MessageRequest): Task[State]
}
