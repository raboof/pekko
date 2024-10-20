/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) 2015-2022 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.stream

import org.apache.pekko.stream.FlowMonitorState.StreamState

/**
 * Used to monitor the state of a stream
 *
 * @tparam T Type of messages passed by the stream
 */
trait FlowMonitor[+T] {
  def state: StreamState[T]
}

object FlowMonitorState {
  sealed trait StreamState[+U]

  /**
   * Stream was created, but no events have passed through it
   */
  case object Initialized extends StreamState[Nothing]

  /**
   * Java API
   */
  def initialized[U](): StreamState[U] = Initialized

  /**
   * Stream processed a message
   *
   * @param msg The processed message
   */
  final case class Received[+U](msg: U) extends StreamState[U]

  /**
   * Java API
   */
  def received[U](msg: U): StreamState[U] = Received(msg)

  /**
   * Stream failed
   *
   * @param cause The cause of the failure
   */
  final case class Failed(cause: Throwable) extends StreamState[Nothing]

  /**
   * Java API
   */
  def failed[U](cause: Throwable): StreamState[U] = Failed(cause)

  /**
   * Stream completed successfully
   */
  case object Finished extends StreamState[Nothing]

  /**
   * Java API
   */
  def finished[U](): StreamState[U] = Finished
}
