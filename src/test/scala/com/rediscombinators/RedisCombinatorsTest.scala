package com.rediscombinators

import com.redis.RedisClientPool
import com.rediscombinators.RedisAsyncOps._
import com.rediscombinators.RedisSyncOps._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FeatureSpec, GivenWhenThen}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class RedisCombinatorsTest extends FeatureSpec with GivenWhenThen with BeforeAndAfter with MockitoSugar {

  val rcs = new RedisClientPool("127.0.0.1", 6379)

  feature ("getKeysAsync") {
    @volatile var done = false
    Given("a Redis client")
    When("storing a list of key-value pairs")
    val kvs = (1 to 100).map(x => s"$x" -> x).toList
    rcs.withClient(rc => rc.mSet(kvs))

    When("retrieving the keys asynchronously")
    rcs.withClient { rc =>
      rc.getKeysAsync.onComplete {
        case Success(x) =>

          Then("the result should contain the original keys")
          assertResult(kvs.map(_._1))(x.map(Integer.parseInt).sorted.map(_.toString))
          done = true
        case Failure(e) => assertResult(true)(false)
      }
    }

    while (!done) { /** Block until done */ }
  }

  after {
    rcs.withClient(_.flushall)
  }

}
