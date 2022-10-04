package compare.fsm.zio.simple

import zio.Task

trait FSM[State, MessageRequest, MessageResponse] {
  def apply(state: State, message: MessageRequest): Task[(State, Option[MessageResponse])]
}
