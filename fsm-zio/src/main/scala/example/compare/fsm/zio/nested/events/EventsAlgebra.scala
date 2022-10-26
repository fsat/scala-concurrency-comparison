package example.compare.fsm.zio.nested.events

trait EventsAlgebra[F[_], FScoped[_], FStream[_], Event] {
  def publish[T <: Event](event: T): F[Unit]
  def subscribe(): FScoped[FStream[Event]]
}
