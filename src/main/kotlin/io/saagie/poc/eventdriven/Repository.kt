package io.saagie.poc.eventdriven

import arrow.core.*

typealias Aggregate<S, E> = (S, E) -> S


class Repository<S, E : Any, Ex : Any>(val aggregate: Aggregate<S, E>) {

    fun <A : Either<Ex, E>> run(state: S, source: EventSource<S, A>): Either<Ex, Tuple2<S, List<E>>> =
            source.toList().foldRight<(S) -> A, Either<Ex, Tuple2<S, List<E>>>>(
                    (state toT listOf<E>()).right()
            ) { source, acc ->
                acc.flatMap { (state, pastEvents) ->
                    source.invoke(state)
                            .map { event -> aggregate.invoke(state, event) toT pastEvents + event }
                }
            }
}