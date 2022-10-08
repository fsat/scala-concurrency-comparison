package fsm.zio

import zio._

trait FSM[State, MessageRequest] {
  def apply(state: State, message: MessageRequest, ctx: FSMContext[State, MessageRequest]): UIO[State]
}
