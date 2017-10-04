package demo

import scala.concurrent.Future
import cats._
import cats.implicits._
import cats.data._
import cats.effect.IO
import org.broadinstitute.dsde.workbench.leonardo.model.LeoException

/**
  * Created by rtitle on 10/3/17.
  */
object UserRepository {

  case class Cluster(id: String, bucketId: Option[String])
  case class Bucket(id: String)

  def getClusterById(id: String): Option[Cluster] = Some(Cluster("cluster", Some("bucket")))
  def getBucketById(id: String): Option[Bucket] = Some(Bucket("bucket"))
  def deleteBucket(bucket: Bucket): Unit = println(s"Deleting bucket $bucket!")

  // Attempt 1
  def deleteClusterBucket(clusterId: String): Option[Bucket] = {
    for {
      cluster <- getClusterById(clusterId)
      bucketId <- cluster.bucketId
      bucket <- getBucketById(bucketId)
      _ <- Option(deleteBucket(bucket))
    } yield bucket
  }

  def getClusterById2(id: String): Future[Option[Cluster]] = Future.successful(Some(Cluster("cluster", Some("bucket"))))
  def getBucketById2(id: String): Future[Option[Bucket]] = Future.successful(Some(Bucket("bucket")))
  def deleteBucket2(bucket: Bucket): Future[Unit] = Future.successful(println(s"Deleting bucket $bucket!"))

  // Attempt 2
  def deleteClusterBucket2(clusterId: String): Future[Option[Bucket]] = {
    for {
      clusterOpt <- getClusterById(clusterId)
      //bucketId <- clusterOpt.bucketId // uh oh
      bucketIdOpt <- Future.successful(clusterOpt.flatMap(_.bucketId))
      bucketOpt <- bucketIdOpt match {
        case None => Future.successful(None)
        case Some(bucketId) => getBucketById(bucketId)
      }
      _ <- bucketOpt match {
        case None => Future.successful(())
        case Some(bucket) => deleteBucket(bucket)
      }
    } yield bucketOpt
  }

  // Generalize
  case class FutureOption[A](inner: Future[Option[A]]) {
    def pure(a: A): FutureOption[A] = {
      FutureOption(Future.successful(Option(a)))
    }

    def flatMap[B](f: A => FutureOption[B]): FutureOption[B] = {
      FutureOption {
        inner.flatMap {
          case Some(a) => f(a).inner
          case None => Future.successful(None)
        }
      }
    }
  }
  // We can generalize

  case class AnyMonadOption[M[_], A](inner: M[Option[A]])(implicit m: Monad[M]) {
    def pure[A](a: A): AnyMonadOption[M, A] = {
      AnyMonadOption(m.pure(Option(a)))
    }

    def flatMap[B](f: A => AnyMonadOption[M, B]): AnyMonadOption[M, B] = {
      AnyMonadOption {
        m.flatMap(inner) {
          case Some(a) => f(a).inner
          case None => m.pure(none[B])
        }
      }
    }

    def map[B](f: A => B): AnyMonadOption[M, B] = {
      flatMap(a => pure(f(a)))
    }

  }

  // This is OptionT!

  // Attempt 3
  def deleteClusterBucket3(clusterId: String): Future[Option[Bucket]] = {
    val trans: AnyMonadOption[Future, Bucket] = for {
      cluster <- AnyMonadOption(getClusterById2(clusterId))
      bucketId <- AnyMonadOption(Future.successful(cluster.bucketId))
      bucket <- AnyMonadOption(getBucketById2(bucketId))
      _ <- AnyMonadOption(deleteBucket2(bucket).map(Option(_)))
    } yield bucket

    trans.inner
  }

  def deleteClusterBucket(clusterId: String): Future[Option[Bucket]] = {
    val trans: OptionT[Future, Bucket] = for {
      cluster <- OptionT[Future, Cluster](getClusterById(clusterId))
      bucketId <- OptionT.fromOption[Future](cluster.bucketId)
      bucket <- OptionT[Future, Bucket](getBucketById(bucketId))
      _ <- OptionT.liftF(deleteBucket(bucket))
    } yield bucket

    trans.value
  }

  // Can't generalize further
  case class ImpossibleT[F[_], G[_], A](inner: F[G[A]])(implicit f: Monad[F], g: Monad[G]) {
    def pure[A](a: A): ImpossibleT[F, G, A] = {
      ImpossibleT[F, G, A](f.pure(g.pure(a)))
    }

    def flatMap[B](f: A => ImpossibleT[F, G, B]): ImpossibleT[F, G, B] = {
      // can't implement this (you're welcome to try)
      // no matter what you'll end up with something like F[G[F[G[A]]]
      // this can't be flattened
      ???
    }
  }


  // Other combinators
  // EitherT
  // ReaderT
  // StateT
  // WriterT

  type HttpResult[A] = EitherT[IO, LeoException, A]


}
