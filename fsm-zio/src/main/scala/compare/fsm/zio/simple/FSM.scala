package compare.fsm.zio.simple

import zio.Task

trait FSM[State, MessageRequest[+_]] {
  def apply[MessageResponse](state: State, message: MessageRequest[MessageResponse]): Task[(State, MessageResponse)]
}
