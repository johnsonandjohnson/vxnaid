[app](../../index.md) / [com.jnj.vaccinetracker.common.ui](../index.md) / [BaseFragment](./index.md)

# BaseFragment

`abstract class BaseFragment : DaggerFragment, `[`ResourcesWrapper`](../../com.jnj.vaccinetracker.common.di/-resources-wrapper/index.md)

**Author**
maartenvangiel

**Version**
1

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `BaseFragment()` |

### Properties

| Name | Summary |
|---|---|
| [resourcesWrapper](resources-wrapper.md) | `val resourcesWrapper: `[`ResourcesWrapper`](../../com.jnj.vaccinetracker.common.di/-resources-wrapper/index.md) |
| [viewModelFactory](view-model-factory.md) | `lateinit var viewModelFactory: Factory` |

### Functions

| Name | Summary |
|---|---|
| [getColor](get-color.md) | `open fun getColor(resId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [getInt](get-int.md) | `open fun getInt(resId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |

### Extension Functions

| Name | Summary |
|---|---|
| [findChild](../../com.jnj.vaccinetracker.common.helpers/androidx.fragment.app.-fragment/find-child.md) | `fun <T> Fragment.findChild(): `[`T`](../../com.jnj.vaccinetracker.common.helpers/androidx.fragment.app.-fragment/find-child.md#T)`?` |
| [findParent](../../com.jnj.vaccinetracker.common.helpers/androidx.fragment.app.-fragment/find-parent.md) | `fun <T> Fragment.findParent(includeSelf: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)` = true): `[`T`](../../com.jnj.vaccinetracker.common.helpers/androidx.fragment.app.-fragment/find-parent.md#T)`?` |
| [logDebug](../../com.jnj.vaccinetracker.common.helpers/kotlin.-any/log-debug.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.logDebug(msg: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, vararg args: `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [logError](../../com.jnj.vaccinetracker.common.helpers/kotlin.-any/log-error.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.logError(msg: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, vararg args: `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [logInfo](../../com.jnj.vaccinetracker.common.helpers/kotlin.-any/log-info.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.logInfo(msg: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, vararg args: `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [logVerbose](../../com.jnj.vaccinetracker.common.helpers/kotlin.-any/log-verbose.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.logVerbose(msg: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, vararg args: `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [logWarn](../../com.jnj.vaccinetracker.common.helpers/kotlin.-any/log-warn.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.logWarn(msg: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, vararg args: `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |

### Inheritors

| Name | Summary |
|---|---|
| [ParticipantFlowIntroFragment](../../com.jnj.vaccinetracker.participantflow.screens/-participant-flow-intro-fragment/index.md) | `class ParticipantFlowIntroFragment : `[`BaseFragment`](./index.md) |
