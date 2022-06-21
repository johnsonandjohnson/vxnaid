[app](../../index.md) / [com.jnj.vaccinetracker.common.helpers](../index.md) / [androidx.databinding.ObservableInt](index.md) / [observe](./observe.md)

# observe

`fun ObservableInt.observe(lifecycle: Lifecycle, callback: (`[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`) -> `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)`): OnPropertyChangedCallback`

Provides a [callback](observe.md#com.jnj.vaccinetracker.common.helpers$observe(androidx.databinding.ObservableInt, androidx.lifecycle.Lifecycle, kotlin.Function1((kotlin.Int, kotlin.Unit)))/callback) that gets called whenever the ObservableInt's value changes.
The [callback](observe.md#com.jnj.vaccinetracker.common.helpers$observe(androidx.databinding.ObservableInt, androidx.lifecycle.Lifecycle, kotlin.Function1((kotlin.Int, kotlin.Unit)))/callback) gets registered immediately, and gets deregistered when [lifecycle](observe.md#com.jnj.vaccinetracker.common.helpers$observe(androidx.databinding.ObservableInt, androidx.lifecycle.Lifecycle, kotlin.Function1((kotlin.Int, kotlin.Unit)))/lifecycle) hits ON_STOP
Upon registration, the [callback](observe.md#com.jnj.vaccinetracker.common.helpers$observe(androidx.databinding.ObservableInt, androidx.lifecycle.Lifecycle, kotlin.Function1((kotlin.Int, kotlin.Unit)))/callback) gets called with the ObservableInt's current value

**Return**
The underlying property change callback, can be used to manually unsubscribe

