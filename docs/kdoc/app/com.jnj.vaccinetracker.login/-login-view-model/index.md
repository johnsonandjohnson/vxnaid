[app](../../index.md) / [com.jnj.vaccinetracker.login](../index.md) / [LoginViewModel](./index.md)

# LoginViewModel

`class LoginViewModel : ViewModel`

**Author**
maartenvangiel

**Version**
1

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `LoginViewModel(loginManager: `[`LoginManager`](../../com.jnj.vaccinetracker.common.data.managers/-login-manager/index.md)`, userRepository: `[`UserRepository`](../../com.jnj.vaccinetracker.common.data.repositories/-user-repository/index.md)`)` |

### Properties

| Name | Summary |
|---|---|
| [errorMessage](error-message.md) | `val errorMessage: ObservableField<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>` |
| [loading](loading.md) | `val loading: ObservableBoolean` |
| [passwordValidationMessage](password-validation-message.md) | `val passwordValidationMessage: ObservableField<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>` |
| [prefillUsername](prefill-username.md) | `val prefillUsername: ObservableField<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>` |
| [usernameValidationMessage](username-validation-message.md) | `val usernameValidationMessage: ObservableField<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>` |

### Functions

| Name | Summary |
|---|---|
| [init](init.md) | `fun init(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [login](login.md) | `fun login(lifecycle: Lifecycle, resourcesWrapper: `[`ResourcesWrapper`](../../com.jnj.vaccinetracker.common.di/-resources-wrapper/index.md)`, username: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, password: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, loggedInCallback: () -> `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [logout](logout.md) | `fun logout(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |

### Extension Functions

| Name | Summary |
|---|---|
| [logDebug](../../com.jnj.vaccinetracker.common.helpers/kotlin.-any/log-debug.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.logDebug(msg: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, vararg args: `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [logError](../../com.jnj.vaccinetracker.common.helpers/kotlin.-any/log-error.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.logError(msg: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, vararg args: `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [logInfo](../../com.jnj.vaccinetracker.common.helpers/kotlin.-any/log-info.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.logInfo(msg: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, vararg args: `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [logVerbose](../../com.jnj.vaccinetracker.common.helpers/kotlin.-any/log-verbose.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.logVerbose(msg: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, vararg args: `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [logWarn](../../com.jnj.vaccinetracker.common.helpers/kotlin.-any/log-warn.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.logWarn(msg: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, vararg args: `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
