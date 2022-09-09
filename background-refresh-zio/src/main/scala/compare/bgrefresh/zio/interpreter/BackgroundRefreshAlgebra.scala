package compare.bgrefresh.zio.interpreter

trait BackgroundRefreshAlgebra[F[_]] {
  def refresh(state: List[Int]): F[List[Int]]
}
