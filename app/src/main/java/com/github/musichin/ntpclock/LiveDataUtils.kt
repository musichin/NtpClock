package com.github.musichin.ntpclock

import androidx.lifecycle.*

private val NOT_SET = Any()

private val NEVER = object : LiveData<Any?>() {}

@Suppress("UNCHECKED_CAST")
fun <T> LiveData<T>.observe(owner: LifecycleOwner, observer: (T) -> Unit) =
    observe(owner, Observer(observer))

fun <T, R> LiveData<T>.map(func: (T) -> R): LiveData<R> {
    return Transformations.map(this, func)
}

fun <T, R> LiveData<T>.switchMap(func: (T) -> LiveData<R>?): LiveData<R> {
    return Transformations.switchMap(this, func)
}

@Suppress("UNCHECKED_CAST")
fun <T, R> combineLatest(sources: Array<out LiveData<out T>>, combiner: (Array<T>) -> R): LiveData<R> {
    if (sources.isEmpty()) return NEVER as LiveData<R>

    val size = sources.size
    val result = MediatorLiveData<Any>()

    val values = arrayOfNulls<Any?>(size)
    for (index in 0 until size) values[index] = NOT_SET

    var emits = 0
    for (index in 0 until size) {
        val observer = Observer<Any> { t ->
            var combine = emits == size
            if (!combine) {
                if (values[index] === NOT_SET) emits++
                combine = emits == size
            }
            values[index] = t

            if (combine) {
                result.value = combiner(values as Array<T>)
            }
        }
        result.addSource(sources[index] as LiveData<Any>, observer)
    }
    return result as LiveData<R>
}

@Suppress("UNCHECKED_CAST")
fun <T1, T2, R> combineLatest(
    source1: LiveData<T1>,
    source2: LiveData<T2>,
    combiner: (T1, T2) -> R
): LiveData<R> {
    val func: (Array<Any>) -> R = { input -> combiner(input[0] as T1, input[1] as T2) }

    return combineLatest(arrayOf(source1 as LiveData<Any>, source2 as LiveData<Any>), func)
}

fun <T1, T2> combineLatest(
    source1: LiveData<T1>,
    source2: LiveData<T2>
): LiveData<Pair<T1, T2>> {
    return combineLatest(source1, source2) { t1, t2 -> t1 to t2 }
}