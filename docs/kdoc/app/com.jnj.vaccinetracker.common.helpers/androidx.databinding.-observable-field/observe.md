[app](../../index.md) / [com.jnj.vaccinetracker.common.helpers](../index.md) / [androidx.databinding.ObservableField](index.md) / [observe](./observe.md)

# observe

`fun <T> ObservableField<`[`T`](observe.md#T)`>.observe(lifecycle: Lifecycle, callback: (`[`T`](observe.md#T)`?) -> `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)`): OnPropertyChangedCallback`

Provides a [callback](observe.md#com.jnj.vaccinetracker.common.helpers$observe(androidx.databinding.ObservableField((com.jnj.vaccinetracker.common.helpers.observe.T)), androidx.lifecycle.Lifecycle, kotlin.Function1((com.jnj.vaccinetracker.common.helpers.observe.T, kotlin.Unit)))/callback) that gets called whenever the ObservableField's value changes.
The [callback](observe.md#com.jnj.vaccinetracker.common.helpers$observe(androidx.databinding.ObservableField((com.jnj.vaccinetracker.common.helpers.observe.T)), androidx.lifecycle.Lifecycle, kotlin.Function1((com.jnj.vaccinetracker.common.helpers.observe.T, kotlin.Unit)))/callback) gets registered immediately, and gets deregistered when [lifecycle](observe.md#com.jnj.vaccinetracker.common.helpers$observe(androidx.databinding.ObservableField((com.jnj.vaccinetracker.common.helpers.observe.T)), androidx.lifecycle.Lifecycle, kotlin.Function1((com.jnj.vaccinetracker.common.helpers.observe.T, kotlin.Unit)))/lifecycle) hits ON_STOP
Upon registration, the [callback](observe.md#com.jnj.vaccinetracker.common.helpers$observe(androidx.databinding.ObservableField((com.jnj.vaccinetracker.common.helpers.observe.T)), androidx.lifecycle.Lifecycle, kotlin.Function1((com.jnj.vaccinetracker.common.helpers.observe.T, kotlin.Unit)))/callback) gets called with the ObservableField's current value

**Return**
The underlying property change callback, can be used to manually unsubscribe

