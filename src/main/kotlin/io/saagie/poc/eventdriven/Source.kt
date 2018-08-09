package io.saagie.poc.eventdriven

import arrow.core.Either
import arrow.core.Tuple2

sealed class RootSource <S,E> {
    data class PureSource<S, E>(val value: S) : RootSource<S,E>()
    data class NewSource<S, E>(val event: GeneratorEvent<S>) : RootSource<S,E>()
    data class DelegateSource<S, E>(val id: Int): RootSource<S, E>()

    companion object {
        fun <S, E> pure(value: S): PureSource<S, E> = PureSource(value)
        fun <S, E> new(event: GeneratorEvent<S>): NewSource<S, E> = NewSource(event)
        fun <S, E> delegate(id: Int): DelegateSource<S, E> = DelegateSource(id)
    }
}

typealias Event<S, E, Ex> = (S) -> Either<Ex, E>
interface GeneratorEvent<S> {
    fun create():S
}

data class Source<S, E, Ex>(
        val source: RootSource<S,E>,
        val events: EventSource<S, E, Ex> = EventSource()) {

    fun andThen(event: Event<S, E, Ex>) = Source(source, events.andThen(event))

    fun andThen(events: EventSource<S, E, Ex>) = Source(source, events.andThen(events))
}

data class EventSource<S, E, Ex>(
        val events: List<Event<S, E, Ex>> = listOf()
) {
    fun andThen(event: Event<S, E, Ex>) = EventSource(events + event)

    fun andThen(source: EventSource<S, E, Ex>) = EventSource(events + source.events)
}

typealias SourceResult<S, E, Ex> = Either<Ex, Tuple2<S, List<E>>>