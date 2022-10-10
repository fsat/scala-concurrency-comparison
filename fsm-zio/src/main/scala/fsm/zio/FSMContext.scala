package fsm.zio

import zio._

import scala.util.{ Failure, Success, Try }

class FSMContext[MessageRequest](
  val self: FSMRef.Self[MessageRequest]) {

  def pipeToSelfAsync[T](execution: Task[T])(mapResult: Try[T] => MessageRequest): UIO[Unit] = {
    for {
      scope <- Scope.parallel
      _ <- execution
        .flatMap { v =>
          self.tell(mapResult(Success(v)))
        }
        .flatMapError { v =>
          self.tell(mapResult(Failure(v)))
        }
        .forkIn(scope)
    } yield ()
  }
}
