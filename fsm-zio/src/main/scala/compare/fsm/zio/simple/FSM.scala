package compare.fsm.zio.simple

import zio.Task

trait FSM[State, MessageRequest[+_], MessageResponse] {
  def apply(state: State, message: MessageRequest[_]): Task[(State, Option[MessageResponse])]
}
