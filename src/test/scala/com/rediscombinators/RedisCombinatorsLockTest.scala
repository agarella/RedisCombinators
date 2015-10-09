package com.rediscombinators

import java.util.UUID

import com.redis.RedisClientPool
import com.rediscombinators.RedisLockOps._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FeatureSpec, GivenWhenThen}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RedisCombinatorsLockTest extends FeatureSpec with GivenWhenThen with BeforeAndAfter with MockitoSugar {

  val rcs = new RedisClientPool("127.0.0.1", 6379)
  @volatile var counter = 0

  before {
    println(s"Counter: $counter")
  }

  def acquireLockAndPrintSomething(lock: Lock): Future[Unit] =
    Future {
      rcs.withClient { client =>
        client.withLock(lock) { _ =>
          println("Acquired lock!")
          Thread.sleep(1000)
          counter += 1
          println(s"Increment counter to: $counter")

        }
      }
    }

  def acquireLockAndThrowException(lock: Lock): Future[Unit] =
    Future {
      rcs.withClient { client =>
        client.withLock(lock) { _ =>
          println("Acquired lock")
          Thread.sleep(1000)
          println("GOING TO DIE!")
          counter += 1
          println(s"Increment counter to: $counter")
          throw new Exception("DIE!")
        }
      }
    }

  feature("RedisLock") {
    Given("a Redis client")

    val uuid = UUID.randomUUID().toString

    acquireLockAndPrintSomething(uuid)
    acquireLockAndThrowException(uuid)
    acquireLockAndPrintSomething(uuid)

    while (counter < 3)(/** Block until done */)
  }

  after {
    counter = 0
    rcs.withClient(_.flushall)
  }

}
