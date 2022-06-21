[app](../../index.md) / [com.jnj.vaccinetracker.common.ui](../index.md) / [BaseActivity](./index.md)

# BaseActivity

`abstract class BaseActivity : DaggerAppCompatActivity, `[`ResourcesWrapper`](../../com.jnj.vaccinetracker.common.di/-resources-wrapper/index.md)`, `[`SessionExpiryHelper.SessionExpiryListener`](../../com.jnj.vaccinetracker.common.helpers/-session-expiry-helper/-session-expiry-listener/index.md)`, `[`RefreshSessionDialog.RefreshSessionListener`](../../com.jnj.vaccinetracker.login/-refresh-session-dialog/-refresh-session-listener/index.md)

**Author**
maartenvangiel

**Version**
1

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `BaseActivity()` |

### Properties

| Name | Summary |
|---|---|
| [resourcesWrapper](resources-wrapper.md) | `val resourcesWrapper: `[`ResourcesWrapper`](../../com.jnj.vaccinetracker.common.di/-resources-wrapper/index.md) |
| [sessionExpiryHelper](session-expiry-helper.md) | `lateinit var sessionExpiryHelper: `[`SessionExpiryHelper`](../../com.jnj.vaccinetracker.common.helpers/-session-expiry-helper/index.md) |
| [viewModelFactory](view-model-factory.md) | `lateinit var viewModelFactory: Factory` |

### Functions

| Name | Summary |
|---|---|
| [getInt](get-int.md) | `open fun getInt(resId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [logOut](log-out.md) | `fun logOut(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [onPause](on-pause.md) | `open fun onPause(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [onResume](on-resume.md) | `open fun onResume(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [onSessionExpired](on-session-expired.md) | `open fun onSessionExpired(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [onSessionRefreshed](on-session-refreshed.md) | `open fun onSessionRefreshed(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |

### Extension Functions

| Name | Summary |
|---|---|
| [findChild](../../com.jnj.vaccinetracker.common.helpers/androidx.appcompat.app.-app-compat-activity/find-child.md) | `fun <T> AppCompatActivity.findChild(parentFirst: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)` = false): `[`T`](../../com.jnj.vaccinetracker.common.helpers/androidx.appcompat.app.-app-compat-activity/find-child.md#T)`?` |
| [hideKeyboard](../../com.jnj.vaccinetracker.common.helpers/android.app.-activity/hide-keyboard.md) | `fun Activity.hideKeyboard(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [logDebug](../../com.jnj.vaccinetracker.common.helpers/kotlin.-any/log-debug.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.logDebug(msg: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, vararg args: `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [logError](../../com.jnj.vaccinetracker.common.helpers/kotlin.-any/log-error.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.logError(msg: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, vararg args: `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [logInfo](../../com.jnj.vaccinetracker.common.helpers/kotlin.-any/log-info.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.logInfo(msg: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, vararg args: `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [logVerbose](../../com.jnj.vaccinetracker.common.helpers/kotlin.-any/log-verbose.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.logVerbose(msg: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, vararg args: `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [logWarn](../../com.jnj.vaccinetracker.common.helpers/kotlin.-any/log-warn.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.logWarn(msg: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, vararg args: `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [showKeyboard](../../com.jnj.vaccinetracker.common.helpers/android.app.-activity/show-keyboard.md) | `fun Activity.showKeyboard(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |

### Inheritors

| Name | Summary |
|---|---|
| [LoginActivity](../../com.jnj.vaccinetracker.login/-login-activity/index.md) | `class LoginActivity : `[`BaseActivity`](./index.md) |
| [ParticipantFlowActivity](../../com.jnj.vaccinetracker.participantflow/-participant-flow-activity/index.md) | `class ParticipantFlowActivity : `[`BaseActivity`](./index.md) |
| [SiteSelectionActivity](../../com.jnj.vaccinetracker.siteselection/-site-selection-activity/index.md) | `class SiteSelectionActivity : `[`BaseActivity`](./index.md) |
| [SplashActivity](../../com.jnj.vaccinetracker.splash/-splash-activity/index.md) | `class SplashActivity : `[`BaseActivity`](./index.md) |
