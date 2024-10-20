/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) 2009-2022 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.osgi

import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.duration._

import PojoSRTestSupport.bundle
import de.kalpatec.pojosr.framework.launch.BundleDescriptor
import language.postfixOps
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import test.{ PingPongActorSystemActivator, RuntimeNameActorSystemActivator, TestActivators }
import test.PingPong._

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.pattern.ask
import pekko.util.Timeout

/**
 * Test cases for [[pekko.osgi.ActorSystemActivator]] in 2 different scenarios:
 * - no name configured for [[pekko.actor.ActorSystem]]
 * - runtime name configuration
 */
object ActorSystemActivatorTest {

  val TEST_BUNDLE_NAME = "org.apache.pekko.osgi.test.activator"

}

class PingPongActorSystemActivatorTest extends AnyWordSpec with Matchers with PojoSRTestSupport {

  import ActorSystemActivatorTest._

  val testBundles: immutable.Seq[BundleDescriptor] = buildTestBundles(
    List(bundle(TEST_BUNDLE_NAME).withActivator(classOf[PingPongActorSystemActivator])))

  "PingPongActorSystemActivator" must {

    "start and register the ActorSystem when bundle starts" in {
      filterErrors() {
        val system = serviceForType[ActorSystem]
        val actor = system.actorSelection("/user/pong")

        implicit val timeout = Timeout(5 seconds)
        Await.result(actor ? Ping, timeout.duration) should be(Pong)
      }
    }

    "stop the ActorSystem when bundle stops" in {
      filterErrors() {
        val system = serviceForType[ActorSystem]
        system.whenTerminated.isCompleted should be(false)

        bundleForName(TEST_BUNDLE_NAME).stop()
        Await.ready(system.whenTerminated, Duration.Inf)
        system.whenTerminated.isCompleted should be(true)
      }
    }
  }

}

class RuntimeNameActorSystemActivatorTest extends AnyWordSpec with Matchers with PojoSRTestSupport {

  import ActorSystemActivatorTest._

  val testBundles: immutable.Seq[BundleDescriptor] =
    buildTestBundles(List(bundle(TEST_BUNDLE_NAME).withActivator(classOf[RuntimeNameActorSystemActivator])))

  "RuntimeNameActorSystemActivator" must {

    "register an ActorSystem and add the bundle id to the system name" in {
      filterErrors() {
        serviceForType[ActorSystem].name should be(
          TestActivators.ACTOR_SYSTEM_NAME_PATTERN.format(bundleForName(TEST_BUNDLE_NAME).getBundleId))
      }
    }
  }

}
