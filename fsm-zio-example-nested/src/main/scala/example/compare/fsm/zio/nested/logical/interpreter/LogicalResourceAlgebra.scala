package example.compare.fsm.zio.nested.logical.interpreter

import example.compare.fsm.zio.nested.physical.interpreter.PhysicalResource

trait LogicalResourceAlgebra[F[_]] {
  def toPhysicalResource(logicalResource: LogicalResource): PhysicalResource
}
