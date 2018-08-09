package io.saagie.poc.eventdriven

import arrow.core.*
import arrow.instance
import arrow.typeclasses.Semigroup

data class EventSource<S, A : Either<Any, Any>>(val run: (S) -> A,
                                                val next: Option<EventSource<S, A>> = None) {

    fun toList(): List<(S) -> A> = listOf(run) + next.map({ it.toList() }).getOrElse({ listOf() })

    companion object
}

fun <S, A : Either<Any, Any>> ((S) -> A).toEventSource() = EventSource(this)

@instance(EventSource::class)
interface EventSourceSemigroupInstance<S, A : Either<Any, Any>> : Semigroup<EventSource<S, A>> {
    override fun EventSource<S, A>.combine(b: EventSource<S, A>): EventSource<S, A> = b.copy(next = Some(this))
}

inline fun <S, A : Either<Any, Any>> chainEventSources(block: EventSourceSemigroupInstance<S, A>.() -> EventSource<S, A>): EventSource<S, A> = EventSource.semigroup<S, A>().run(block)