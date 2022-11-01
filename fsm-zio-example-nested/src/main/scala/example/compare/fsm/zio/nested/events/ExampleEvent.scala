package example.compare.fsm.zio.nested.events

import example.compare.fsm.zio.nested.logical.LogicalResourceFSM
import example.compare.fsm.zio.nested.physical.PhysicalResourceFSM

object ExampleEvent {
  object PhysicalResourceEvent {
    final case class StateTransition(from: PhysicalResourceFSM.State, to: PhysicalResourceFSM.State) extends PhysicalResourceEvent
  }
  sealed trait PhysicalResourceEvent extends ExampleEvent

  object LogicalResourceEvent {
    final case class StateTransition(from: LogicalResourceFSM.State, to: LogicalResourceFSM.State) extends LogicalResourceEvent
  }
  sealed trait LogicalResourceEvent extends ExampleEvent
}

sealed trait ExampleEvent extends Product with Serializable
