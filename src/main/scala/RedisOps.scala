import com.redis.RedisClient

import scalaz.Scalaz._

object RedisOps {

  implicit class Redis(redis: RedisClient) {

    def set[A](kvs: List[(String, A)]) = if (kvs.nonEmpty) redis.mset(kvs: _*)

    def del(ks: List[String]) = if (ks.nonEmpty) redis.del(ks.head, ks.tail: _*)

    def getKeys: List[String] = getKeys("*")

    def getKeys(pattern: String): List[String] = mapKey("*")(identity)

    def forEachKey(f: String => Unit): Unit = mapKey(f)

    def forEachKey(pattern: String)(f: String => Unit): Unit = mapKey(pattern)(f)

    def mapKey[B](f: String => B): List[B] = mapKey("*")(f)

    def mapKey[B](pattern: String)(f: String => B): List[B] = {
      var (cursor, res) = scan(0, f, pattern, redis)
      while (cursor > 0) {
        val (newCursor, newRes) = scan(cursor, f, pattern, redis)
        cursor = newCursor
        res = res |+| newRes
      }
      res
    }

    private def scan[B](cursor: Int, f: String => B, pattern: String, client: RedisClient): (Int, List[B]) =
      client.scan(cursor, pattern).map { t =>
        val newCursor = t._1.orZero
        val bs = t._2.map { xs => xs.flatten.map { key => f(key) } }.orZero
        newCursor -> bs
      }.orZero
  }
}
