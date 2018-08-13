package io.saagie.poc.eventdriven

import arrow.core.*
import arrow.instances.extensions
import arrow.typeclasses.binding

typealias Aggregate<S, E> = (S, E) -> S
typealias EventSourceResult<S, R, E> = Either<E, Tuple2<S, List<R>>>


class Repository<S, E : Any, Ex : Any>(val aggregate: Aggregate<S, E>) {

    fun <A : Either<Ex, E>> run(state: S, source: EventSource<S, A, (S) -> A>): Either<Ex, Tuple2<S, List<E>>> =
            ForEventSource<S, A>() extensions {
                source.foldRight<(S) -> A, Either<Ex, Tuple2<S, List<E>>>>(
                        Eval.just((state toT listOf<E>()).right())
                ) { source, eval ->
                    ForEval extensions {
                        binding {
                            val acc = eval.bind()
                            acc.flatMap { (state, pastEvents) ->
                                source.invoke(state)
                                        .map { event -> aggregate.invoke(state, event) toT pastEvents + event }
                            }
                        }.fix()
                    }
                }.extract()
            }
}