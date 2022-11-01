package example.compare.fsm.zio.nested.events

import example.compare.fsm.zio.nested.physical.PhysicalResourceFSM

object ExampleEvent {
  object PhysicalResourceEvent {
    final case class StateTransition(from: PhysicalResourceFSM.State, to: PhysicalResourceFSM.State) extends PhysicalResourceEvent
  }
  sealed trait PhysicalResourceEvent extends ExampleEvent
  sealed trait LogicalResourceEvent extends ExampleEvent
}

sealed trait ExampleEvent extends Product with Serializable
