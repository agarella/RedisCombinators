package com.rediscombinators

import com.redis.RedisClient

import scalaz.Scalaz._

object RedisSyncOps {

  implicit class RedisSync(rc: RedisClient) {

    def mSet[A](kvs: List[(String, A)]): Unit = if (kvs.nonEmpty) rc.mset(kvs: _*)

    def mDel(ks: List[String]): Unit = if (ks.nonEmpty) rc.del(ks.head, ks.tail: _*)

    def getKeys: List[String] = getKeys("*")

    def getKeys(pattern: String): List[String] = mapKey("*")(identity)

    def forEachKey(f: String => Unit): Unit = mapKey(f)

    def forEachKey(pattern: String)(f: String => Unit): Unit = mapKey(pattern)(f)

    def mapKey[B](f: String => B): List[B] = mapKey("*")(f)

    def mapKey[B](pattern: String)(f: String => B): List[B] = {
      implicit val c: RedisClient = rc
      implicit val p: String = pattern
      var (cursor, res) = scan(0, f)
      while (cursor > 0) {
        val (newCursor, newRes) = scan(cursor, f)
        cursor = newCursor
        res = res |+| newRes
      }
      res
    }

    private def scan[B](cursor: Int, f: String => B)(implicit pattern: String): (Int, List[B]) =
      rc.scan(cursor, pattern).map { t =>
        val (cursorMaybe, ksMaybe) = t
        val newCursor = cursorMaybe.orZero
        val bs = ksMaybe.map(vs => vs.flatten.map(k => f(k))).orZero
        newCursor -> bs
      }.orZero

  }

}
