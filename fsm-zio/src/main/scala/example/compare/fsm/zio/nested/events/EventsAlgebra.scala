package example.compare.fsm.zio.nested.events

import zio.Queue

trait EventsAlgebra[F[_], FStream[_, _, _], Event] {
  def publish[T <: Event](event: T): F[Unit]
  def subscribe(): F[FStream[_, Nothing, Event]]
}
