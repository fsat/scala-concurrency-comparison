package example.compare.fsm.zio.nested.physical.interpreter

import example.compare.fsm.zio.nested.Done
import example.compare.fsm.zio.nested.physical.interpreter.PhysicalResourceAlgebra.{ Artifact, ArtifactDownloadLocation }

import java.net.URL

object PhysicalResourceAlgebra {
  final case class Artifact(url: URL)
  final case class ArtifactDownloadLocation(url: URL)
}

trait PhysicalResourceAlgebra[F[_], FStream[_, _, _]] {
  def find(name: String): F[Option[(PhysicalResource.Id, PhysicalResource)]]
  def create(name: String, resource: PhysicalResource): F[PhysicalResource.Id]
  def update(name: String, resource: PhysicalResource): F[PhysicalResource.Id]
  def listArtifacts(resource: PhysicalResource): F[FStream[Any, Throwable, Artifact]]
  def downloadArtifact(resource: PhysicalResource, artifact: Artifact): F[ArtifactDownloadLocation]
  def delete(id: PhysicalResource.Id): F[Done]
}
