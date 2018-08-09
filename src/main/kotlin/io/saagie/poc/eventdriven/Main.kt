package io.saagie.poc.eventdriven

import arrow.core.*

val MAX = 10


sealed class TurtleEvent() {
    data class TurtleCreated(val id: Int, val x: Int, val y: Int) : TurtleEvent()
    data class TurtleMoved(val id: Int, val vector: Tuple2<Int, Int>) : TurtleEvent()
}

sealed class TurtleError(message: String): Error(message) {
    class OutOfBoundaries(message: String): TurtleError(message)
}

data class Turtle(
        val id: Int,
        val pos: Tuple2<Int, Int>) {

    companion object {
        fun move(x: Int, y: Int): (Turtle) -> Either<TurtleError, TurtleEvent> = {
            if (it.pos.a + x > MAX || it.pos.b + y > MAX) {
                TurtleError.OutOfBoundaries("Turtle can't go in ${it.pos.a+x}:${it.pos.b+y}").left()
            } else {
                TurtleEvent.TurtleMoved(it.id, x toT y).right()
            }
        }
    }
}

fun turtleAggregate(state: Turtle, event: TurtleEvent): Turtle = when (event) {
    is TurtleEvent.TurtleCreated -> state
    is TurtleEvent.TurtleMoved -> state.copy(pos = state.pos.a + event.vector.a toT state.pos.b + event.vector.b)
}


fun main(args: Array<String>) {
    val test = chainEventSources<Turtle, Either<TurtleError, TurtleEvent>> {
        Turtle.move(2, 2).toEventSource() +
                Turtle.move(4, 3).toEventSource()
    }
    val repository = Repository<Turtle, TurtleEvent, TurtleError>(::turtleAggregate)
    println(repository.run(Turtle(0, 0 toT 0), test))
}