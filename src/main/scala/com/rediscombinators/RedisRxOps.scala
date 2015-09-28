package com.rediscombinators

import com.redis.RedisClient
import com.redis.serialization.{Format, Parse}
import rx.lang.scala.{Observable, Subscriber}

import scala.concurrent.ExecutionContext.Implicits.global
import scalaz.Scalaz._

object RedisRxOps {

  implicit class RedisRx(rc: RedisClient) {

    import RedisAsyncOps._

    def getStream[A](key: String)(implicit format: Format, parse: Parse[A]): Observable[A] = Observable.from(rc.getAsync(key))

    // TODO test with Int
    def mGetStream[A](implicit format: Format, parse: Parse[A]): Observable[A] = mGetStream("*")

    // TODO test with Int
    def mGetStream[A](pattern: String)(implicit f: Format, p: Parse[A]): Observable[A] = getKeysRx.flatMap(k => getStream(k)(f, p))

    def getKeysRx: Observable[String] = getKeysRx("*")

    def getKeysRx(pattern: String): Observable[String] = mapKeyRx(pattern)(identity)

    def mapKeyRx[B](f: String => B): Observable[B] = mapKeyRx("*")(f)

    def mapKeyRx[B](pattern: String)(f: String => B): Observable[B] = Observable { subscriber =>
      implicit val c: RedisClient = rc
      implicit val p: String = pattern
      implicit val s: Subscriber[B] = subscriber

      var cursor = 0
      do {
        cursor = scan(cursor, f)
      } while (cursor > 0)

      subscriber.onCompleted()
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
