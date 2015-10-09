package com.rediscombinators

import com.redis.RedisClientPool
import org.scalatest.mock.MockitoSugar
import org.scalatest.{DoNotDiscover, BeforeAndAfter, GivenWhenThen, FeatureSpec}
import rx.lang.scala.Observer

import scalaz.Scalaz._

import scala.util.Try

@DoNotDiscover
class RedisCombinatorsRxTest extends FeatureSpec with GivenWhenThen with BeforeAndAfter with MockitoSugar {

  val rcs = new RedisClientPool("127.0.0.1", 6379)
  @volatile var done = false

  feature("mGetRx"){
    import RedisRxOps._
    import RedisSyncOps._

    val kvs = (1 to 100).map(x => s"$x" -> s"$x").toList
    rcs.withClient(rc => rc.mSet(kvs))

    val o = createObserver[String]

    rcs.withClient { rc =>
      rc.mGetStream.subscribe(o)
    }

    while (!done)(/** Block until done */)
  }

  def createObserver[A]: Observer[A] = new Observer[A] {
    override def onNext(value: A): Unit = println(value)
    override def onError(error: Throwable): Unit = error.printStackTrace()
    override def onCompleted(): Unit = done = true
  }

  def parseInt(s: String): Int = Try(Integer.parseInt(s)).toOption.orZero

  after {
    done = false
    rcs.withClient(_.flushall)
  }

}
