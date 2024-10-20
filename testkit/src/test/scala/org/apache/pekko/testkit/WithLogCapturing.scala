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

package org.apache.pekko.testkit

import java.io.{ OutputStream, PrintStream }

import org.scalatest.{ Outcome, SuiteMixin, TestSuite }

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.event.Logging
import pekko.event.Logging._

/**
 * Mixin this trait to a test to make log lines appear only when the test failed.
 */
trait WithLogCapturing extends SuiteMixin { this: TestSuite =>
  implicit def system: ActorSystem

  abstract override def withFixture(test: NoArgTest): Outcome = {
    // When filtering just collects events into this var (yeah, it's a hack to do that in a filter).
    // We assume that the filter will always ever be used from a single actor, so a regular var should be fine.
    var events: List[LogEvent] = Nil

    object LogEventCollector extends EventFilter(Int.MaxValue) {
      override protected def matches(event: Logging.LogEvent): Boolean = {
        events ::= event
        true
      }
    }

    val myLogger = Logging(system, classOf[WithLogCapturing])
    val res = LogEventCollector.intercept {
      myLogger.debug(s"Logging started for test [${test.name}]")
      val r = test()
      myLogger.debug(s"Logging finished for test [${test.name}]")
      r
    }

    if (!(res.isSucceeded || res.isPending)) {
      println(s"--> [${Console.BLUE}${test.name}${Console.RESET}] Start of log messages of test that [$res]")
      val logger = new StdOutLogger {}
      withPrefixedOut("| ") { events.reverse.foreach(logger.print) }
      println(s"<-- [${Console.BLUE}${test.name}${Console.RESET}] End of log messages of test that [$res]")
    }

    res
  }

  /** Adds a prefix to every line printed out during execution of the thunk. */
  private def withPrefixedOut[T](prefix: String)(thunk: => T): T = {
    val oldOut = Console.out
    val prefixingOut =
      new PrintStream(new OutputStream {
        override def write(b: Int): Unit = oldOut.write(b)
      }) {
        override def println(x: Any): Unit =
          oldOut.println(prefix + String.valueOf(x).replace("\n", s"\n$prefix"))
      }

    Console.withOut(prefixingOut) {
      thunk
    }
  }
}

/**
 * An adaption of TestEventListener that never prints debug logs itself. Use together with [[pekko.testkit.WithLogCapturing]].
 * It allows to enable noisy DEBUG logging for tests and silence pre/post test DEBUG output completely while still showing
 * test-specific DEBUG output selectively
 */
class DebugLogSilencingTestEventListener extends TestEventListener {
  override def print(event: Any): Unit = event match {
    case _: Debug => // ignore
    case _        => super.print(event)
  }
}

/**
 * An adaption of TestEventListener that does not print out any logs. Use together with [[pekko.testkit.WithLogCapturing]].
 * It allows to enable noisy logging for tests and silence pre/post test log output completely while still showing
 * test-specific log output selectively on failures.
 */
class SilenceAllTestEventListener extends TestEventListener {
  override def print(event: Any): Unit = ()
}
