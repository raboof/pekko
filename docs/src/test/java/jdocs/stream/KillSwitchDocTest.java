/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) 2018-2022 Lightbend Inc. <https://www.lightbend.com>
 */

package jdocs.stream;

import org.apache.pekko.NotUsed;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.japi.Pair;
import org.apache.pekko.stream.*;
import org.apache.pekko.stream.javadsl.Keep;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import jdocs.AbstractJavaTest;
import org.apache.pekko.testkit.javadsl.TestKit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

class KillSwitchDocTest extends AbstractJavaTest {

  static ActorSystem system;

  @BeforeClass
  public static void setup() {
    system = ActorSystem.create("GraphDSLDocTest");
  }

  @AfterClass
  public static void tearDown() {
    TestKit.shutdownActorSystem(system);
    system = null;
  }

  @Test
  public void compileOnlyTest() {}

  public void uniqueKillSwitchShutdownExample() throws Exception {
    // #unique-shutdown
    final Source<Integer, NotUsed> countingSrc =
        Source.from(new ArrayList<>(Arrays.asList(1, 2, 3, 4)))
            .delay(Duration.ofSeconds(1), DelayOverflowStrategy.backpressure());
    final Sink<Integer, CompletionStage<Integer>> lastSnk = Sink.last();

    final Pair<UniqueKillSwitch, CompletionStage<Integer>> stream =
        countingSrc
            .viaMat(KillSwitches.single(), Keep.right())
            .toMat(lastSnk, Keep.both())
            .run(system);

    final UniqueKillSwitch killSwitch = stream.first();
    final CompletionStage<Integer> completionStage = stream.second();

    doSomethingElse();
    killSwitch.shutdown();

    final int finalCount = completionStage.toCompletableFuture().get(1, TimeUnit.SECONDS);
    assertEquals(2, finalCount);
    // #unique-shutdown
  }

  public static void uniqueKillSwitchAbortExample() throws Exception {
    // #unique-abort
    final Source<Integer, NotUsed> countingSrc =
        Source.from(new ArrayList<>(Arrays.asList(1, 2, 3, 4)))
            .delay(Duration.ofSeconds(1), DelayOverflowStrategy.backpressure());
    final Sink<Integer, CompletionStage<Integer>> lastSnk = Sink.last();

    final Pair<UniqueKillSwitch, CompletionStage<Integer>> stream =
        countingSrc
            .viaMat(KillSwitches.single(), Keep.right())
            .toMat(lastSnk, Keep.both())
            .run(system);

    final UniqueKillSwitch killSwitch = stream.first();
    final CompletionStage<Integer> completionStage = stream.second();

    final Exception error = new Exception("boom!");
    killSwitch.abort(error);

    final int result =
        completionStage.toCompletableFuture().exceptionally(e -> -1).get(1, TimeUnit.SECONDS);
    assertEquals(-1, result);
    // #unique-abort
  }

  public void sharedKillSwitchShutdownExample() throws Exception {
    // #shared-shutdown
    final Source<Integer, NotUsed> countingSrc =
        Source.from(new ArrayList<>(Arrays.asList(1, 2, 3, 4)))
            .delay(Duration.ofSeconds(1), DelayOverflowStrategy.backpressure());
    final Sink<Integer, CompletionStage<Integer>> lastSnk = Sink.last();
    final SharedKillSwitch killSwitch = KillSwitches.shared("my-kill-switch");

    final CompletionStage<Integer> completionStage =
        countingSrc
            .viaMat(killSwitch.flow(), Keep.right())
            .toMat(lastSnk, Keep.right())
            .run(system);
    final CompletionStage<Integer> completionStageDelayed =
        countingSrc
            .delay(Duration.ofSeconds(1), DelayOverflowStrategy.backpressure())
            .viaMat(killSwitch.flow(), Keep.right())
            .toMat(lastSnk, Keep.right())
            .run(system);

    doSomethingElse();
    killSwitch.shutdown();

    final int finalCount = completionStage.toCompletableFuture().get(1, TimeUnit.SECONDS);
    final int finalCountDelayed =
        completionStageDelayed.toCompletableFuture().get(1, TimeUnit.SECONDS);

    assertEquals(2, finalCount);
    assertEquals(1, finalCountDelayed);
    // #shared-shutdown
  }

  public static void sharedKillSwitchAbortExample() throws Exception {
    // #shared-abort
    final Source<Integer, NotUsed> countingSrc =
        Source.from(new ArrayList<>(Arrays.asList(1, 2, 3, 4)))
            .delay(Duration.ofSeconds(1), DelayOverflowStrategy.backpressure());
    final Sink<Integer, CompletionStage<Integer>> lastSnk = Sink.last();
    final SharedKillSwitch killSwitch = KillSwitches.shared("my-kill-switch");

    final CompletionStage<Integer> completionStage1 =
        countingSrc
            .viaMat(killSwitch.flow(), Keep.right())
            .toMat(lastSnk, Keep.right())
            .run(system);
    final CompletionStage<Integer> completionStage2 =
        countingSrc
            .viaMat(killSwitch.flow(), Keep.right())
            .toMat(lastSnk, Keep.right())
            .run(system);

    final Exception error = new Exception("boom!");
    killSwitch.abort(error);

    final int result1 =
        completionStage1.toCompletableFuture().exceptionally(e -> -1).get(1, TimeUnit.SECONDS);
    final int result2 =
        completionStage2.toCompletableFuture().exceptionally(e -> -1).get(1, TimeUnit.SECONDS);

    assertEquals(-1, result1);
    assertEquals(-1, result2);
    // #shared-abort
  }

  private static void doSomethingElse() {}
}
