package org.broadinstitute.dsde.workbench

import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes
import cats.data.{EitherT, ReaderT}
import cats.Eval._
import cats.effect.IO
import cats.implicits._
import org.broadinstitute.dsde.workbench.leonardo.db.DataAccess
import org.broadinstitute.dsde.workbench.leonardo.model.LeoException
import org.broadinstitute.dsde.workbench.model.ErrorReportSource
import slick.dbio.DBIO

import scala.concurrent.Future

package object leonardo {
  implicit val errorReportSource = ErrorReportSource("leonardo")

  case class InternalError(_message: String, _cause: Throwable) extends LeoException(_message, StatusCodes.InternalServerError, _cause)

  type HttpResult[A] = EitherT[IO, LeoException, A]

  object HttpResult {
    def apply[A](eio: IO[Either[LeoException, A]]): HttpResult[A] = {
      EitherT(eio)
    }

    def pure[A](a: A): HttpResult[A] = {
      EitherT.pure(a)
    }

    def fromFuture[A](f: Future[A]): HttpResult[A] = {
      IO.fromFuture(always(f)).attemptT.leftMap(t => InternalError(t.getMessage, t))
    }

    def fromEither[A](either: Either[LeoException, A]): HttpResult[A] = {
      EitherT.fromEither(either)
    }

    def raiseError[A](e: LeoException): HttpResult[A] = {
      fromEither(e.raiseError)
    }

    def toResponse[A](httpResult: HttpResult[A]): IO[ToResponseMarshallable] = {
      httpResult.fold (
        { leoException => ??? },
        { a => ??? }
      )
    }
  }

  type DbResult[A] = ReaderT[EitherT[IO, Throwable, ?], DataAccess, A]

  object DbResult {
    def apply[A](f: DataAccess => IO[Either[Throwable, A]]): DbResult[A] = {
      ReaderT.apply[EitherT[IO, Throwable, ?], DataAccess, A]((dataAccess: DataAccess) => (f andThen EitherT.apply)(dataAccess))
    }

  }
}