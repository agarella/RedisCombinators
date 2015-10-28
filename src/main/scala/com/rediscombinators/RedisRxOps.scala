package com.rediscombinators

import com.redis.serialization.{Format, Parse}
import com.redis.{E, M, RedisClient}
import com.rediscombinators.RedisAsyncOps._
import rx.lang.scala.{Observable, Subscriber}

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.Scalaz._

object RedisRxOps {

  implicit class RedisRx(rc: RedisClient) {

    def eventStream(key: String): Observable[String] = Observable[String] { subscriber =>
      Future {
        rc.subscribe(s"__keyspace@0__:$key") {
          case M(c, m) => subscriber.onNext(m)
          case E(e)    => subscriber.onError(e)
          case _       =>
        }
      }
    }

    def eventStream: Observable[(String, String)] = Observable[(String, String)] { subscriber =>
      Future {
        rc.pSubscribe("__keyspace@0__:*") {
          case M(c, m) => subscriber.onNext((c.split(":").last, m))
          case E(e)    => subscriber.onError(e)
          case _       =>
        }
      }
    }

    def getStream[A](key: String)(implicit format: Format, parse: Parse[A]): Observable[A] = Observable.from(rc.getAsync(key))

    // TODO test with Int
    def mGetStream[A](implicit format: Format, parse: Parse[A]): Observable[A] = mGetStream("*")

    // TODO test with Int
    def mGetStream[A](pattern: String)(implicit f: Format, p: Parse[A]): Observable[A] =
      getKeyStream(pattern).flatMap(k => getStream(k)(f, p))

    def getKeyStream: Observable[String] = getKeyStream("*")

    def getKeyStream(pattern: String): Observable[String] = mapKeyStream(pattern)(identity)

    def mapKeyStream[B](f: String => B): Observable[B] = mapKeyStream("*")(f)

    def mapKeyStream[B](pattern: String)(f: String => B): Observable[B] = Observable { subscriber =>
      Future {
        implicit val c: RedisClient = rc
        implicit val p: String = pattern
        implicit val s: Subscriber[B] = subscriber

        doScan(scan(0, f), f)
        subscriber.onCompleted()
      }
    }

    @tailrec private def doScan[B](cursor: Int, f: String => B)(implicit pattern: String, subscriber: Subscriber[B]): Unit = cursor match {
      case x if x > 0 => doScan(scan(cursor, f), f)
      case _          =>
    }

    private def scan[B](cursor: Int, f: String => B)(implicit pattern: String, subscriber: Subscriber[B]): Int =
      rc.scan(cursor, pattern).map { t =>
        val (cursorMaybe, ks) = t
        val newCursor = cursorMaybe.orZero
        ks.foreach(ks => ks.flatten.foreach(k => subscriber.onNext(f(k))))
        newCursor
      }.orZero

  }

}
