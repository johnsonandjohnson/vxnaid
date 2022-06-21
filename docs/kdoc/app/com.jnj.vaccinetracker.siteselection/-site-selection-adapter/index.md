[app](../../index.md) / [com.jnj.vaccinetracker.siteselection](../index.md) / [SiteSelectionAdapter](./index.md)

# SiteSelectionAdapter

`class SiteSelectionAdapter : ArrayAdapter<`[`Site`](../../com.jnj.vaccinetracker.common.data.models.api.response/-site/index.md)`>`

**Author**
maartenvangiel

**Version**
1

### Types

| Name | Summary |
|---|---|
| [SiteLocationFilter](-site-location-filter/index.md) | `inner class SiteLocationFilter : Filter` |

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `SiteSelectionAdapter(context: Context, layoutResource: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, sites: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`Site`](../../com.jnj.vaccinetracker.common.data.models.api.response/-site/index.md)`>)` |

### Functions

| Name | Summary |
|---|---|
| [getCount](get-count.md) | `fun getCount(): `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [getFilter](get-filter.md) | `fun getFilter(): Filter` |
| [getItem](get-item.md) | `fun getItem(position: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Site`](../../com.jnj.vaccinetracker.common.data.models.api.response/-site/index.md)`?` |
| [getPosition](get-position.md) | `fun getPosition(item: `[`Site`](../../com.jnj.vaccinetracker.common.data.models.api.response/-site/index.md)`?): `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [setReturnAllResults](set-return-all-results.md) | `fun setReturnAllResults(returnAllResults: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Sets whether this adapter will return all results (true) or the results based upon the input query (false) |

### Extension Functions

| Name | Summary |
|---|---|
| [logDebug](../../com.jnj.vaccinetracker.common.helpers/kotlin.-any/log-debug.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.logDebug(msg: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, vararg args: `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [logError](../../com.jnj.vaccinetracker.common.helpers/kotlin.-any/log-error.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.logError(msg: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, vararg args: `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [logInfo](../../com.jnj.vaccinetracker.common.helpers/kotlin.-any/log-info.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.logInfo(msg: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, vararg args: `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [logVerbose](../../com.jnj.vaccinetracker.common.helpers/kotlin.-any/log-verbose.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.logVerbose(msg: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, vararg args: `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [logWarn](../../com.jnj.vaccinetracker.common.helpers/kotlin.-any/log-warn.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.logWarn(msg: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, vararg args: `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
