package example.compare.fsm.zio.nested.physical.interpreter

import example.compare.fsm.zio.nested.Done
import zio._
import zio.stream._

import java.net.URL

class PhysicalResourceInterpreter extends PhysicalResourceAlgebra[Task, ZStream] {
  override def find(name: String): Task[Option[(PhysicalResource.Id, PhysicalResource)]] = {
    if (name == "existing") {
      ZIO.attempt {
        println(s"find(${name}): returning existing")
        Some((PhysicalResource.Id("existing"), PhysicalResource()))
      }
    } else if (name == "fail") {
      ZIO.die(new RuntimeException(s"Failing to find [${name}]"))
    } else {
      ZIO.attempt {
        println(s"find(${name}): Returning none")
        None
      }
    }
  }

  override def create(name: String, resource: PhysicalResource): Task[PhysicalResource.Id] = {
    if (name == "fail") {
      ZIO.die(new RuntimeException(s"Failing to create [${name}]"))
    } else {
      ZIO.attempt {
        println(s"create(${name}, ${resource})")
        PhysicalResource.Id(name)
      }
    }
  }

  override def update(name: String, resource: PhysicalResource): Task[PhysicalResource.Id] = {
    if (name == "fail") {
      ZIO.die(new RuntimeException(s"Failing to update [${name}]"))
    } else {
      ZIO.attempt {
        println(s"update(${name}, ${resource})")
        PhysicalResource.Id(name)
      }
    }
  }

  override def listArtifacts(resource: PhysicalResource): Task[ZStream[Any, Throwable, PhysicalResourceAlgebra.Artifact]] =
    ZIO.attempt {
      ZStream.apply(
        PhysicalResourceAlgebra.Artifact(new URL("s3://artifact1")),
        PhysicalResourceAlgebra.Artifact(new URL("s3://artifact2")))
    }

  override def downloadArtifact(resource: PhysicalResource, artifact: PhysicalResourceAlgebra.Artifact): Task[PhysicalResourceAlgebra.ArtifactDownloadLocation] =
    ZIO.attempt {
      println(s"downloadArtifact(${resource}, ${artifact})")
      PhysicalResourceAlgebra.ArtifactDownloadLocation(new URL("s3://dummy"))
    }

  override def delete(id: PhysicalResource.Id): Task[Done] = {
    if (id.value == "fail") {
      ZIO.die(new RuntimeException(s"Failing to delete [${id}]"))
    } else {
      ZIO.attempt {
        println(s"delete(${id})")
        Done
      }
    }
  }
}
