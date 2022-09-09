package compare.bgrefresh.interpreter

import scala.concurrent.Future

class BackgroundRefreshInterpreter extends BackgroundRefreshAlgebra[Future] {
  override def refresh(state: List[Int]): Future[List[Int]] = {
    Future.successful((state :+ state.length).takeRight(5))
  }
}
