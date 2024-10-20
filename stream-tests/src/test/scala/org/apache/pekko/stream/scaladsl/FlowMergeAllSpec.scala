/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) 2014-2022 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.stream.scaladsl

import org.apache.pekko
import pekko.stream.testkit._
import pekko.stream.testkit.scaladsl.TestSink

class FlowMergeAllSpec extends StreamSpec("""
    pekko.stream.materializer.initial-input-buffer-size = 2
  """) with ScriptedTest {

  "Flow mergeAll" must {
    "merge all upstream elements to its downstream" in {
      val source1 = Source(1 to 3)
      val source2 = Source(4 to 6)
      val source3 = Source(7 to 10)
      source1
        .mergeAll(List(source2, source3), eagerComplete = false)
        .fold(Set.empty[Int])((set, i) => set + i)
        .runWith(TestSink.probe)
        .request(1)
        .expectNext(Set(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
        .expectComplete();
    }

    "merge all elements of the first completed source to its downstream " in {
      val source1 = Source(1 to 2)
      val source2 = Source.repeat(3)
      val source3 = Source.repeat(4)
      val result =
        source1.mergeAll(List(source2, source3), eagerComplete = true).runFold(Set.empty[Int])((set, i) => set + i)
      result.futureValue should contain allElementsOf (Set(1, 2))
    }

    "merge single upstream elements to its downstream" in {
      Source(1 to 3)
        .mergeAll(Nil, eagerComplete = false)
        .runWith(TestSink.probe)
        .request(3)
        .expectNext(1, 2, 3)
        .expectComplete()
    }

    "works in merge numbers example" in {
      // #merge-all
      val sourceA = Source(1 to 3)
      val sourceB = Source(4 to 6)
      val sourceC = Source(7 to 10)
      sourceA.mergeAll(List(sourceB, sourceC), eagerComplete = false).runForeach(println)
      // merging is not deterministic, can for example print 1, 2, 3, 4, 5, 6, 7, 8, 9, 10
      // #merge-all
    }

  }
}
