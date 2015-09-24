import com.redis.RedisClient

import scalaz.Scalaz._

object RedisOps {

  implicit class RedisOps(c: RedisClient) {

    def forEachKey[B](f: String => B): List[B] = forEachKey(f, "*")

    def forEachKey(pattern: String): List[String] = forEachKey(identity, pattern)

    def forEachKey[B](f: String => B, pattern: String): List[B] = {
      var (cursor, res) = scan(0, f, pattern, c)
      while (cursor > 0) {
        val (newCursor, newRes) = scan(cursor, f, pattern, c)
        cursor = newCursor
        res = res |+| newRes
      }
      res
    }

    private def scan[B](cursor: Int, f: String => B, pattern: String, client: RedisClient): (Int, List[B]) =
      client.scan(cursor, pattern).map { t =>
        val newCursor = t._1.orZero
        val bs = t._2.map { xs => xs.flatten.map { key => f(key) } }.orZero
        (newCursor, bs)
      }.orZero
  }
}
