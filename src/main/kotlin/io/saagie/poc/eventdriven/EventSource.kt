package io.saagie.poc.eventdriven

import arrow.Kind
import arrow.core.*
import arrow.higherkind
import arrow.instance
import arrow.typeclasses.Foldable
import arrow.typeclasses.Semigroup

@higherkind
data class EventSource<S, R, out A>(val run: A,
                                    val next: Option<EventSource<S, R, A>> = None) : EventSourceOf<S, R, A>
        where R : Either<Any, Any> {

    companion object
}

fun <S, R : Either<Any, Any>> ((S) -> R).toES(): EventSource<S, R, (S) -> R> = EventSource(this)

@instance(EventSource::class)
interface EventSourceSemigroupInstance<S, R : Either<Any, Any>, A : (S) -> R> : Semigroup<EventSource<S, R, A>> {
    override fun EventSource<S, R, A>.combine(b: EventSource<S, R, A>): EventSource<S, R, A> = b.next.fold(
            { b.copy(next = Some(this)) },
            { b.copy(next= Some(this.combine(it))) })
}

fun <S, R : Either<Any, Any>> chainEventSources(block: EventSourceSemigroupInstance<S, R, (S) -> R>.() -> EventSource<S, R, (S) -> R>): EventSource<S, R, (S) -> R> = EventSource.semigroup<S, R, (S) -> R>().run(block)

@instance(EventSource::class)
interface EventSourceFoldable<S, R : Either<Any, Any>> : Foldable<EventSourcePartialOf<S, R>> {

    override fun <A, B> Kind<EventSourcePartialOf<S, R>, A>.foldLeft(b: B, f: (B, A) -> B): B = fix().run {
        next.fold(
                { f(b, run) },
                { it.foldLeft(f(b, run), f) }
        )
    }

    override fun <A, B> Kind<EventSourcePartialOf<S, R>, A>.foldRight(lb: Eval<B>, f: (A, Eval<B>) -> Eval<B>): Eval<B> = fix().run {
        next.fold(
                { f(run, lb) },
                { f(run, it.foldRight(lb, f)) }
        )
    }
}

class EventSourceContext<S, R : Either<Any, Any>> : EventSourceFoldable<S, R>, EventSourceSemigroupInstance<S, R, (S) -> R>

class EventContext<S, R : Either<Any, Any>> {
    infix fun <A> extensions(f: EventSourceContext<S, R>.() -> A): A =
            f(EventSourceContext())
}

fun <S, R : Either<Any, Any>> ForEventSource(): EventContext<S, R> = EventContext()