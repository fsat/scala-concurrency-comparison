package example.compare.fsm.zio.nested.events

import zio._
import zio.stream._

class EventsInterpreter[Event](hub: Hub[Event]) extends EventsAlgebra[UIO, ZStream, Event] {
  override def publish[T <: Event](event: T): UIO[Unit] =
    hub.publish(event).map(_ => ())

  override def subscribe(): UIO[Queue[Event]] = ???
}
