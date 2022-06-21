[app](../../index.md) / [com.jnj.vaccinetracker.participantflow](../index.md) / [ParticipantFlowViewModel](./index.md)

# ParticipantFlowViewModel

`class ParticipantFlowViewModel : ViewModel`

### Types

| Name | Summary |
|---|---|
| [NavigationDirection](-navigation-direction/index.md) | `enum class NavigationDirection` |
| [Screen](-screen/index.md) | `enum class Screen` |

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `ParticipantFlowViewModel(userRepository: `[`UserRepository`](../../com.jnj.vaccinetracker.common.data.repositories/-user-repository/index.md)`, configurationManager: `[`ConfigurationManager`](../../com.jnj.vaccinetracker.common.data.managers/-configuration-manager/index.md)`)` |

### Properties

| Name | Summary |
|---|---|
| [canGoBack](can-go-back.md) | `val canGoBack: ObservableBoolean` |
| [navigationDirection](navigation-direction.md) | `var navigationDirection: `[`ParticipantFlowViewModel.NavigationDirection`](-navigation-direction/index.md) |
| [operator](operator.md) | `val operator: ObservableField<`[`User`](../../com.jnj.vaccinetracker.common.data.models.api.response/-user/index.md)`>` |
| [screen](screen.md) | `val screen: ObservableField<`[`ParticipantFlowViewModel.Screen`](-screen/index.md)`!>` |
| [site](site.md) | `val site: ObservableField<`[`Site`](../../com.jnj.vaccinetracker.common.data.models.api.response/-site/index.md)`>` |

### Functions

| Name | Summary |
|---|---|
| [confirmIntro](confirm-intro.md) | `fun confirmIntro(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [goBack](go-back.md) | `fun goBack(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [init](init.md) | `fun init(lifecycle: Lifecycle): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |

### Extension Functions

| Name | Summary |
|---|---|
| [logDebug](../../com.jnj.vaccinetracker.common.helpers/kotlin.-any/log-debug.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.logDebug(msg: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, vararg args: `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [logError](../../com.jnj.vaccinetracker.common.helpers/kotlin.-any/log-error.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.logError(msg: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, vararg args: `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [logInfo](../../com.jnj.vaccinetracker.common.helpers/kotlin.-any/log-info.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.logInfo(msg: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, vararg args: `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [logVerbose](../../com.jnj.vaccinetracker.common.helpers/kotlin.-any/log-verbose.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.logVerbose(msg: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, vararg args: `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [logWarn](../../com.jnj.vaccinetracker.common.helpers/kotlin.-any/log-warn.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.logWarn(msg: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, vararg args: `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
