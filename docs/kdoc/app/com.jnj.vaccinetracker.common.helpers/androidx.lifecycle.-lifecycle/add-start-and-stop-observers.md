[app](../../index.md) / [com.jnj.vaccinetracker.common.helpers](../index.md) / [androidx.lifecycle.Lifecycle](index.md) / [addStartAndStopObservers](./add-start-and-stop-observers.md)

# addStartAndStopObservers

`fun Lifecycle.addStartAndStopObservers(onStartCallback: () -> `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)`, onStopCallback: () -> `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)

Adds an observer to the specified Lifecycle, which will trigger the provided [callback](#) when the lifecycle hits ON_START and ON_STOP.
The observer is registered immediately and gets cleaned up after the lifecycle hits ON_STOP OR ON_DESTROY,
this means the callbacks can be called only once

