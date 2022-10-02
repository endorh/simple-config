package endorh.simpleconfig.konfig.entry

import org.intellij.lang.annotations.Language

object KotlinEntryBuilders {
    /**
     * Regex entry
     *
     * Will use the options from the default value to compile user input
     */
    fun regex(value: Regex): RegexEntryBuilder = RegexEntry.Builder(value)
    /**
     * Regex entry
     *
     * Will use the passed options to compile user input
     */
    fun regex(@Language("RegExp") value: String, vararg flags: RegexOption): RegexEntryBuilder = regex(Regex(value, setOf(*flags)))
    
    /**
     * Data class entry
     *
     * You can bind each property to an entry inside the [configure] block by using
     * [DataClassEntryBuilder.bind] and mapping property references to entry builders
     * using [DataClassEntryBuilder.by] or [DataClassEntryBuilder.caption]
     *
     * The [configure] block should only contain a call to [DataClassEntryBuilder.bind],
     * which should contain the property bindings.
     * Once [multiple context receivers](https://github.com/Kotlin/KEEP/blob/master/proposals/context-receivers.md)
     * become stable, the call to [DataClassEntryBuilder.bind] will be unnecessary and deprecated.
     *
     * Example usage:
     * ```
     * object ClientKonfig(Type.CLIENT) {
     *     // ...
     *     data class Data(val name: String, val age: Int)
     *     val data = data(Data("Steve", 20)) { bind {
     *         ::name caption string()
     *         ::age by number()
     *     }}
     * }
     * ```
     * The default values from the entry builders bound to the properties are ignored,
     * as the passed data class instance is used as the default value. As such, you should
     * avoid passing them where possible.
     *
     * This API ensures type safety of the property bindings, and doesn't use string literals
     * to map property names
     */
    fun <B : Any> data(value: B, configure: DataClassEntryBuilder<B>.() -> Unit): DataClassEntryBuilder<B> =
      DataClassEntry.Builder(value).apply(configure)
}