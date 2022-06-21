[app](../../index.md) / [com.jnj.vaccinetracker.common.helpers](../index.md) / [androidx.lifecycle.Lifecycle](index.md) / [addOnPauseObserver](./add-on-pause-observer.md)

# addOnPauseObserver

`fun Lifecycle.addOnPauseObserver(callback: () -> `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)

Adds an observer to the specified Lifecycle, which will trigger the provided [callback](add-on-pause-observer.md#com.jnj.vaccinetracker.common.helpers$addOnPauseObserver(androidx.lifecycle.Lifecycle, kotlin.Function0((kotlin.Unit)))/callback) when the lifecycle hits ON_PAUSE.
The observer is registered immediately and gets cleaned up after the lifecycle hits ON_PAUSE, so it will only be called once.

