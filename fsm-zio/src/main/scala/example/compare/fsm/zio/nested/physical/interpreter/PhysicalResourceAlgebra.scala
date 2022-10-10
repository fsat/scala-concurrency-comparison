package example.compare.fsm.zio.nested.physical.interpreter

import example.compare.fsm.zio.nested.Done

sealed trait PhysicalResourceAlgebra[F[_]] {
  def find(name: String): F[Option[PhysicalResource]]
  def create(name: String, resource: PhysicalResource): F[PhysicalResource.Id]
  def update(name: String, resource: PhysicalResource): F[PhysicalResource.Id]
  def delete(id: PhysicalResource.Id): F[Done]
}
