[app](../../index.md) / [com.jnj.vaccinetracker.common.helpers](../index.md) / [androidx.databinding.ObservableBoolean](index.md) / [observe](./observe.md)

# observe

`fun ObservableBoolean.observe(lifecycle: Lifecycle, callback: (`[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`) -> `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)`): OnPropertyChangedCallback`

Provides a [callback](observe.md#com.jnj.vaccinetracker.common.helpers$observe(androidx.databinding.ObservableBoolean, androidx.lifecycle.Lifecycle, kotlin.Function1((kotlin.Boolean, kotlin.Unit)))/callback) that gets called whenever the ObservableBoolean's value changes.
The [callback](observe.md#com.jnj.vaccinetracker.common.helpers$observe(androidx.databinding.ObservableBoolean, androidx.lifecycle.Lifecycle, kotlin.Function1((kotlin.Boolean, kotlin.Unit)))/callback) gets registered immediately, and gets deregistered when [lifecycle](observe.md#com.jnj.vaccinetracker.common.helpers$observe(androidx.databinding.ObservableBoolean, androidx.lifecycle.Lifecycle, kotlin.Function1((kotlin.Boolean, kotlin.Unit)))/lifecycle) hits ON_STOP
Upon registration, the [callback](observe.md#com.jnj.vaccinetracker.common.helpers$observe(androidx.databinding.ObservableBoolean, androidx.lifecycle.Lifecycle, kotlin.Function1((kotlin.Boolean, kotlin.Unit)))/callback) gets called with the ObservableBoolean's current value

**Return**
The underlying property change callback, can be used to manually unsubscribe

