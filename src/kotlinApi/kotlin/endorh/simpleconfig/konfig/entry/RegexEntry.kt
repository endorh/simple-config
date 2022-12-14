package endorh.simpleconfig.konfig.entry

import endorh.simpleconfig.api.ConfigEntryHolder
import endorh.simpleconfig.api.entry.SerializableEntryBuilder
import endorh.simpleconfig.api.ui.TextFormatter
import endorh.simpleconfig.core.entry.AbstractSerializableEntry
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import org.jetbrains.annotations.Contract
import java.util.*
import java.util.regex.PatternSyntaxException

interface RegexEntryBuilder : SerializableEntryBuilder<Regex, RegexEntryBuilder> {
    @Contract(pure=true) fun withOptions(vararg options: RegexOption): RegexEntryBuilder
    @Contract(pure=true) fun withoutOptions(vararg options: RegexOption): RegexEntryBuilder
}

internal class RegexEntry(
  parent: ConfigEntryHolder, name: String, value: Regex
): AbstractSerializableEntry<Regex>(
    parent, name, value, Regex::class.java
) {
    var options: Set<RegexOption> = EnumSet.noneOf(RegexOption::class.java)
    
    internal class Builder(value: Regex): AbstractSerializableEntry.Builder<
      Regex, RegexEntry, RegexEntryBuilder, Builder
    >(value, Regex::class.java), RegexEntryBuilder {
        private inline fun <reified E: Enum<E>> copySet(src: Collection<E>) =
          if (src.isEmpty()) EnumSet.noneOf(E::class.java) else EnumSet.copyOf(src)
        
        var options: MutableSet<RegexOption> = copySet(value.options)
    
        override fun withOptions(vararg options: RegexOption) = copy()!!.also {
            it.options.addAll(options)
        }
    
        override fun withoutOptions(vararg options: RegexOption) = copy()!!.also {
            it.options.removeAll(options.toSet())
        }
    
        override fun buildEntry(parent: ConfigEntryHolder, name: String) = RegexEntry(parent, name, value).also {
            it.options = copySet(options)
        }
    
        override fun createCopy(value: Regex): Builder = Builder(value).also {
            it.options = copySet(options)
        }
    }
    
    override fun serialize(value: Regex) = value.pattern
    override fun deserialize(value: String) = try {
        Regex(value, options)
    } catch (e: PatternSyntaxException) {
        null
    }
    
    override fun addExtraTooltip(value: String?): List<Component>? {
        val extra = super.addExtraTooltip(value)
        if (options.isNotEmpty()) extra.add(
            0, Component.translatable("simpleconfig.config.help.pattern_flags", displayFlags(options))
              .withStyle(ChatFormatting.GRAY))
        return extra
    }
    
    companion object {
        private val FLAG_NAMES = mapOf(
            RegexOption.UNIX_LINES to 'd',
            RegexOption.IGNORE_CASE to 'i',
            RegexOption.COMMENTS to 'x',
            RegexOption.MULTILINE to 'm',
            RegexOption.DOT_MATCHES_ALL to 's',
            RegexOption.LITERAL to null,
            RegexOption.CANON_EQ to null,
        )
    }
    
    fun displayFlags(flags: Set<RegexOption>): String {
        if (flags.isEmpty()) return ""
        val f = StringBuilder("(?")
        flags.forEach { opt ->
            FLAG_NAMES[opt]?.let { f.append(it) }
        }
        if (RegexOption.LITERAL in flags) f.append("+LITERAL")
        if (RegexOption.CANON_EQ in flags) f.append("+CANON_EQ")
        f.append(')')
        return f.toString()
    }
    
    override fun getErrorMessage(value: String): Optional<Component>? = try {
        Regex(value, options)
        super.getErrorMessage(value)
    } catch (e: PatternSyntaxException) {
        Optional.of(Component.translatable(
            "simpleconfig.config.error.invalid_pattern",
            e.message!!.trim { it <= ' ' }.replace("\r\n", ": ")))
    }
    
    override fun getConfigCommentTooltips(): List<String>? {
        val tooltips = super.getConfigCommentTooltips()
        if (options.isNotEmpty()) tooltips.add("Flags: " + displayFlags(options))
        return tooltips
    }
    
    override fun getTextFormatter(): TextFormatter? =
      TextFormatter.forLanguageOrDefault("regex", TextFormatter.DEFAULT)
}