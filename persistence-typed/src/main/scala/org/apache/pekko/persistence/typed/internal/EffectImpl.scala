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

package org.apache.pekko.persistence.typed.internal

import scala.collection.immutable

import org.apache.pekko
import pekko.actor.typed.ActorRef
import pekko.annotation.InternalApi
import pekko.persistence.typed.javadsl
import pekko.persistence.typed.scaladsl

/** INTERNAL API */
@InternalApi
private[pekko] abstract class EffectImpl[+Event, State]
    extends javadsl.EffectBuilder[Event, State]
    with javadsl.ReplyEffect[Event, State]
    with scaladsl.ReplyEffect[Event, State]
    with scaladsl.EffectBuilder[Event, State] {
  /* All events that will be persisted in this effect */
  override def events: immutable.Seq[Event] = Nil

  override def thenRun(chainedEffect: State => Unit): EffectImpl[Event, State] =
    CompositeEffect(this, new Callback[State](chainedEffect))

  override def thenReply[ReplyMessage](replyTo: ActorRef[ReplyMessage])(
      replyWithMessage: State => ReplyMessage): EffectImpl[Event, State] =
    CompositeEffect(this, new ReplyEffectImpl[ReplyMessage, State](replyTo, replyWithMessage))

  override def thenUnstashAll(): EffectImpl[Event, State] =
    CompositeEffect(this, UnstashAll.asInstanceOf[SideEffect[State]])

  override def thenNoReply(): EffectImpl[Event, State] =
    CompositeEffect(this, new NoReplyEffectImpl[State])

  override def thenStop(): EffectImpl[Event, State] =
    CompositeEffect(this, Stop.asInstanceOf[SideEffect[State]])

}

/** INTERNAL API */
@InternalApi
private[pekko] object CompositeEffect {
  def apply[Event, State](
      effect: scaladsl.EffectBuilder[Event, State],
      sideEffects: SideEffect[State]): CompositeEffect[Event, State] =
    CompositeEffect[Event, State](effect, sideEffects :: Nil)
}

/** INTERNAL API */
@InternalApi
private[pekko] final case class CompositeEffect[Event, State](
    persistingEffect: scaladsl.EffectBuilder[Event, State],
    _sideEffects: immutable.Seq[SideEffect[State]])
    extends EffectImpl[Event, State] {

  override val events: immutable.Seq[Event] = persistingEffect.events

  override def toString: String =
    s"CompositeEffect($persistingEffect, sideEffects: ${_sideEffects.size})"
}

/** INTERNAL API */
@InternalApi
private[pekko] case object PersistNothing extends EffectImpl[Nothing, Nothing]

/** INTERNAL API */
@InternalApi
private[pekko] final case class Persist[Event, State](event: Event) extends EffectImpl[Event, State] {
  override def events = event :: Nil

  override def toString: String = s"Persist(${event.getClass.getName})"
}

/** INTERNAL API */
@InternalApi
private[pekko] final case class PersistAll[Event, State](override val events: immutable.Seq[Event])
    extends EffectImpl[Event, State] {

  override def toString: String = s"PersistAll(${events.map(_.getClass.getName).mkString(",")})"
}

/** INTERNAL API */
@InternalApi
private[pekko] case object Unhandled extends EffectImpl[Nothing, Nothing]

/** INTERNAL API */
@InternalApi
private[pekko] case object Stash extends EffectImpl[Nothing, Nothing]
