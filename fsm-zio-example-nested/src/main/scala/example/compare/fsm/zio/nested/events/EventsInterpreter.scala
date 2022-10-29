package example.compare.fsm.zio.nested.events

import example.compare.fsm.zio.nested.events.EventsInterpreter._
import zio._
import zio.stream._

object EventsInterpreter {
  type ScopedUIO[+A] = ZIO[Scope, Nothing, A]
}

class EventsInterpreter[Event](hub: Hub[Event]) extends EventsAlgebra[UIO, ScopedUIO, UStream, Event] {
  override def publish[T <: Event](event: T): UIO[Unit] =
    hub.publish(event).map(_ => ())

  override def subscribe(): ScopedUIO[UStream[Event]] =
    for {
      queue <- hub.subscribe
      result = ZStream.fromQueue(queue)
    } yield result
}
