package io.saagie.poc.eventdriven

import arrow.core.*


typealias Handler<S, E> = (S, E) -> S

abstract class Repository<S, E>(
        val handler: Handler<S, E>
) {

    abstract fun get(id: Int): S

    abstract fun saveState(state: S)

    private fun <Ex> apply(state: S, source: EventSource<S, E, Ex>, initialEvents: Option<E> = None): SourceResult<S, E, Ex> =
            source.events.foldRight<Event<S, E, Ex>, SourceResult<S, E, Ex>>(
                    Either.right(state toT initialEvents.map { listOf(it) }.getOrElse { listOf() })
            ) { event, acc ->
                acc.flatMap { (state, pastEvents) ->
                    event.invoke(state)
                            .map { handler.invoke(state, it) toT pastEvents + it }
                }
            }

    fun <Ex> execute(source: Source<S, E, Ex>): SourceResult<S, E, Ex> = chain(source.events)(source.source)

    fun <Ex> executeAndSave(source: Source<S, E, Ex>): SourceResult<S, E, Ex> = execute(source).flatMap { saveState(it.a); it.right() }

    fun <Ex> chain(events: EventSource<S, E, Ex>): (RootSource<S, E>) -> SourceResult<S, E, Ex> = {
        when (it) {
            is RootSource.Pure<S, E> -> apply(it.value, events)
            is RootSource.CreationEvent<S, E> -> apply(it.event.create(), events, (it.event as E).some())
            is RootSource.Repository<S, E> -> apply(get(it.id), events)
        }
    }

}