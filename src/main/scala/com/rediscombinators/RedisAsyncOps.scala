package com.rediscombinators

import com.redis.RedisClient
import com.redis.serialization.{Parse, Format}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.Scalaz._

object RedisAsyncOps {

  implicit class RedisAsync(rc: RedisClient) {

    def getAsync[A](key: String)(implicit format: Format, parse: Parse[A]): Future[Option[A]] = Future { rc.get[A](key) }

    def delAsync(key: String): Unit = Future { rc.del(key) }

    def mSetAsync[A](kvs: List[(String, A)]): Unit = Future { if (kvs.nonEmpty) rc.mset(kvs: _*) }

    def mDelAsync(ks: List[String]): Unit = Future { if (ks.nonEmpty) rc.del(ks.head, ks.tail: _*) }

    def mDelAsync(pattern: String): Unit = forEachKeyAsync(pattern) { k => delAsync(k) }

    def getKeysAsync: Future[List[String]] = getKeysAsync("*")

    def getKeysAsync(pattern: String): Future[List[String]] = mapKeyAsync("*")(identity)

    def forEachKeyAsync(f: String => Unit): Unit = mapKeyAsync(f)

    def forEachKeyAsync(pattern: String)(f: String => Unit): Unit = mapKeyAsync(pattern)(f)

    def mapKeyAsync[B](f: String => B): Future[List[B]] = mapKeyAsync("*")(f)

    def mapKeyAsync[B](pattern: String)(f: String => B): Future[List[B]] = {
      implicit val p: String = pattern
      scan(0, f).flatMap(combineScan(f))
    }

    private def nextScan[B](cursor: Int, f: String => B)(implicit pattern: String): Future[List[B]] =
      if (cursor > 0)
        scan(cursor, f).flatMap(combineScan(f))
      else
        Future.successful(List.empty[B])

    private def combineScan[B](f: (String) => B)(t: (Int, List[B]))(implicit pattern: String): Future[List[B]] = {
      val (cursor, vs) = t
      nextScan(cursor, f).map(bs => vs |+| bs)
    }

    private def scan[B](cursor: Int, f: String => B)(implicit pattern: String): Future[(Int, List[B])] = Future {
      rc.scan(cursor, pattern).map { t =>
        val (cursorMaybe, vsMaybe) = t
        val cursor: Int = cursorMaybe.orZero
        val bs: List[B] = vsMaybe.map(vs => vs.flatten.map(key => f(key))).orZero
        cursor -> bs
      }.orZero
    }

  }

}
