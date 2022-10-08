package fsm.zio

class FSMContext[State, MessageRequest](
  val self: FSMRef.Self[State, MessageRequest]) {

}
