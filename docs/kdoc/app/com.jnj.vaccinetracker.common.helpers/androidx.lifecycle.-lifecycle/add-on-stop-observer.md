[app](../../index.md) / [com.jnj.vaccinetracker.common.helpers](../index.md) / [androidx.lifecycle.Lifecycle](index.md) / [addOnStopObserver](./add-on-stop-observer.md)

# addOnStopObserver

`fun Lifecycle.addOnStopObserver(callback: () -> `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)

Adds an observer to the specified Lifecycle, which will trigger the provided [callback](add-on-stop-observer.md#com.jnj.vaccinetracker.common.helpers$addOnStopObserver(androidx.lifecycle.Lifecycle, kotlin.Function0((kotlin.Unit)))/callback) when the lifecycle hits ON_STOP.
The observer is registered immediately and gets cleaned up after the lifecycle hits ON_STOP, so it will only be called once.

