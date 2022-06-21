[app](../../index.md) / [com.jnj.vaccinetracker.login](../index.md) / [LoginActivity](./index.md)

# LoginActivity

`class LoginActivity : `[`BaseActivity`](../../com.jnj.vaccinetracker.common.ui/-base-activity/index.md)

**Author**
maartenvangiel

**Version**
1

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `LoginActivity()` |

### Inherited Properties

| Name | Summary |
|---|---|
| [resourcesWrapper](../../com.jnj.vaccinetracker.common.ui/-base-activity/resources-wrapper.md) | `val resourcesWrapper: `[`ResourcesWrapper`](../../com.jnj.vaccinetracker.common.di/-resources-wrapper/index.md) |
| [sessionExpiryHelper](../../com.jnj.vaccinetracker.common.ui/-base-activity/session-expiry-helper.md) | `lateinit var sessionExpiryHelper: `[`SessionExpiryHelper`](../../com.jnj.vaccinetracker.common.helpers/-session-expiry-helper/index.md) |
| [viewModelFactory](../../com.jnj.vaccinetracker.common.ui/-base-activity/view-model-factory.md) | `lateinit var viewModelFactory: Factory` |

### Functions

| Name | Summary |
|---|---|
| [onCreate](on-create.md) | `fun onCreate(savedInstanceState: Bundle?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [onStart](on-start.md) | `fun onStart(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |

### Inherited Functions

| Name | Summary |
|---|---|
| [getInt](../../com.jnj.vaccinetracker.common.ui/-base-activity/get-int.md) | `open fun getInt(resId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [logOut](../../com.jnj.vaccinetracker.common.ui/-base-activity/log-out.md) | `fun logOut(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [onPause](../../com.jnj.vaccinetracker.common.ui/-base-activity/on-pause.md) | `open fun onPause(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [onResume](../../com.jnj.vaccinetracker.common.ui/-base-activity/on-resume.md) | `open fun onResume(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [onSessionExpired](../../com.jnj.vaccinetracker.common.ui/-base-activity/on-session-expired.md) | `open fun onSessionExpired(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [onSessionRefreshed](../../com.jnj.vaccinetracker.common.ui/-base-activity/on-session-refreshed.md) | `open fun onSessionRefreshed(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |

### Companion Object Functions

| Name | Summary |
|---|---|
| [create](create.md) | `fun create(context: Context): Intent` |

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
