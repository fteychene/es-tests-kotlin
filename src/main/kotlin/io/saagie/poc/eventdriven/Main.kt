package io.saagie.poc.eventdriven

import arrow.core.*
import io.saagie.poc.eventdriven.Turtle.Companion.move
import io.saagie.poc.eventdriven.RootSource.Companion.new
import io.saagie.poc.eventdriven.RootSource.Companion.delegate

val MAX = 10

data class Turtle(
        val id: Int,
        val pos: Tuple2<Int, Int>) {

    companion object {

        fun move(x: Int, y: Int): (Turtle) -> Either<String, TurtleEvent> = {
            if (it.pos.a + x > MAX || it.pos.b + y > MAX) {
                "Out of boundaries".left()
            } else {
                TurtleEvent.TurtleMoved(it.id, x toT y).right()
            }
        }

        fun defautlHandler(state: Turtle, event: TurtleEvent): Turtle =
                when (event) {
                    is TurtleEvent.TurtleCreated -> state
                    is TurtleEvent.TurtleMoved -> state.copy(pos = state.pos.a + event.vector.a toT state.pos.b + event.vector.b)
                }
    }
}

sealed class TurtleEvent() {
    data class TurtleCreated(val id: Int, val x: Int, val y: Int) : TurtleEvent(), GeneratorEvent<Turtle> {
        override fun create(): Turtle = Turtle(id, x toT y)
    }

    data class TurtleMoved(val id: Int, val vector: Tuple2<Int, Int>) : TurtleEvent()
}


class TurtleRepository<E>(
        handler: Handler<Turtle, E>,
        var storage: List<Turtle> = listOf()) : Repository<Turtle, E>(handler) {

    override fun saveState(state: Turtle) {
        storage = storage + state
    }

    override fun get(id: Int): Turtle = storage.filter { id.equals(it.id) }.first()

}

fun main(args: Array<String>) {


    val repository = TurtleRepository(Turtle.Companion::defautlHandler)

    val aggregate = Source<Turtle, TurtleEvent, String>(new(TurtleEvent.TurtleCreated(id = 0, x = 0, y = 0)))
            .andThen(move(2, 2))
            .andThen(move(2, 2))

    println(repository.executeAndSave(aggregate))


    val eventsSource = EventSource<Turtle, TurtleEvent, String>()
            .andThen(move(2, 2))
            .andThen(move(2, 2))
            .andThen(move(2, 2))
            .andThen(move(1, 1))
    println(repository.chain(eventsSource)(delegate(0)))
}