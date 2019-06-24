package de.musichin.ntpclock

class Event<T>(private val content: T) {
    var consumed = false
        private set

    fun consume(): T? =
        if (!consumed) {
            consumed = true
            content
        } else {
            null
        }

    fun peek(): T = content
}
