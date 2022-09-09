package compare.bgrefresh.zio.interpreter

import zio.{ Task, ZIO }

class BackgroundRefreshInterpreter extends BackgroundRefreshAlgebra[Task] {
  override def refresh(state: List[Int]): Task[List[Int]] = {
    ZIO.succeed((state :+ state.length).takeRight(5))
  }
}
