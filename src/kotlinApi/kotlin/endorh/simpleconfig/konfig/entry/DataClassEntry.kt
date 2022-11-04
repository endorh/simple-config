package endorh.simpleconfig.konfig.entry

import com.electronwill.nightconfig.core.Config
import com.google.common.base.CaseFormat
import com.google.common.collect.Maps
import endorh.simpleconfig.api.AtomicEntryBuilder
import endorh.simpleconfig.api.ConfigEntryBuilder
import endorh.simpleconfig.api.ConfigEntryHolder
import endorh.simpleconfig.api.EntryTag
import endorh.simpleconfig.api.ui.icon.Icon
import endorh.simpleconfig.core.AbstractConfigEntry
import endorh.simpleconfig.core.AbstractConfigEntryBuilder
import endorh.simpleconfig.core.AtomicEntry
import endorh.simpleconfig.core.DummyEntryHolder
import endorh.simpleconfig.core.entry.BeanProxy
import endorh.simpleconfig.ui.api.AbstractConfigListEntry
import endorh.simpleconfig.ui.api.ConfigFieldBuilder
import endorh.simpleconfig.ui.api.IChildListEntry
import endorh.simpleconfig.ui.impl.builders.BeanFieldBuilder
import endorh.simpleconfig.ui.impl.builders.FieldBuilder
import org.apache.logging.log4j.LogManager
import org.jetbrains.annotations.Contract
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

interface DataClassEntryBuilder<D> : ConfigEntryBuilder<D, Map<String, Any>, D, DataClassEntryBuilder<D>> {
    @Contract(pure=true) fun allowUneditableProperties(allowUneditable: Boolean): DataClassEntryBuilder<D>
    @Contract(pure=true) fun withIcon(icon: (D) -> Icon): DataClassEntryBuilder<D>
    
    // TODO: Deprecate when context receivers become a stable Kotlin feature
    @Contract(pure=false) fun bind(bindings: D.() -> Unit)
    
    infix fun <T> KProperty<T>.by(builder: ConfigEntryBuilder<T, *, *, *>)
    infix fun <T> KProperty<T>.by(builder: (T) -> ConfigEntryBuilder<T, *, *, *>)
    infix fun <T, G, CB> KProperty<T>.caption(builder: CB) where
      CB : ConfigEntryBuilder<T, *, G, *>, CB : AtomicEntryBuilder
    fun <T> D.baked(baker: D.() -> T) = BakedDataClassBinding<D, T> { baker() }
    infix fun <T> KProperty<T>.by(binding: BakedDataClassBinding<D, T>)
}

fun interface BakedDataClassBinding<D, T> {
    fun D.bake(): T
}

internal class DataClassEntry<D : Any>(
  parent: ConfigEntryHolder, name: String, defValue: D,
  val proxy: DataClassProxy<D>,
  val entries: Map<String, AbstractConfigEntry<*, *, *>>
) : AbstractConfigEntry<D, Map<String, Any>, D>(parent, name, defValue) {
    companion object {
        private val LOGGER = LogManager.getLogger()
        private val LINE_BREAK = Regex("\\R")
    }
    
    private var caption: String? = null
    private var iconProvider: ((D) -> Icon)? = null
    
    internal class Builder<B : Any>(value: B) : AbstractConfigEntryBuilder<
      B, Map<String, Any>, B, DataClassEntry<B>, DataClassEntryBuilder<B>, Builder<B>
    >(value, value::class.java), DataClassEntryBuilder<B> {
        private val entries = mutableMapOf<String, AbstractConfigEntryBuilder<*, *, *, *, *, *>>()
        private val bakedEntries = mutableMapOf<String, BakedDataClassBinding<B, *>>()
        private var caption: String? = null
        private var iconProvider: ((B) -> Icon)? = null
        private var allowUneditableProperties = false
        
        override fun allowUneditableProperties(allowUneditable: Boolean) = copy()!!.also {
            it.allowUneditableProperties = allowUneditable
        }
    
        override fun bind(bindings: B.() -> Unit) {
            value.apply(bindings)
        }
    
        override infix fun <T> KProperty<T>.by(builder: ConfigEntryBuilder<T, *, *, *>) {
            entries[name] = builder as? AbstractConfigEntryBuilder<*, *, *, *, *, *>
              ?: throw IllegalArgumentException("Builder must be an AbstractConfigEntryBuilder")
        }
        override infix fun <T> KProperty<T>.by(builder: (T) -> ConfigEntryBuilder<T, *, *, *>) =
          this by builder(getter.call())
    
        override infix fun <T, G, CB> KProperty<T>.caption(builder: CB)
        where CB : ConfigEntryBuilder<T, *, G, *>, CB : AtomicEntryBuilder {
            if (caption != null) throw IllegalStateException("caption already set for this data class: $caption")
            by(builder)
            caption = name
        }
    
        override infix fun <T> KProperty<T>.by(binding: BakedDataClassBinding<B, T>) {
            bakedEntries[name] = binding
        }
    
        override fun withIcon(icon: (B) -> Icon) = copy()!!.also { it.iconProvider = icon }
        
        override fun buildEntry(parent: ConfigEntryHolder, name: String): DataClassEntry<B> {
            val entries = linkedMapOf<String, AbstractConfigEntry<*, *, *>>()
            this.entries.forEach { (n, e) -> entries[n] = DummyEntryHolder.build(parent, e) }
            val proxy = DataClassProxy(value, entries.mapValues { createAdapter(it.value) }, bakedEntries.mapValues {
                it.value.run {{ bake() }}
            })
            val prefix = "${entries.values.map { it.root.modId }.firstOrNull() ?: ""}.config.bean.${proxy.typeTranslation}."
            entries.entries.forEach { (n, e) ->
                val key = prefix + proxy.getTranslation(n)
                e.translation = key
                e.tooltipKey = "$key:help"
                e.setName(proxy.getTranslation(n))
            }
            val names = value::class.memberProperties.map { it.name }.toMutableSet()
            names.removeAll(entries.keys)
            names.removeAll(bakedEntries.keys)
            val entry = DataClassEntry(parent, name, value, proxy, entries)
            if (!allowUneditableProperties && names.isNotEmpty()) throw DataClassIntrospectionException(
                "Found uneditable properties in data class " + proxy.typeName + ": ["
                + names.joinToString(", ") + "]\n" +
                "Call allowUneditableProperties() to allow them, or define config entries for them." +
                "\n  at " + entry.globalPath)
            entries.keys.forEach { n ->
                if (proxy.get(value, n) == null)
                    throw DataClassNullPropertyException(proxy.getPropertyName(n))
            }
            entry.caption = caption
            entry.iconProvider = iconProvider
            return entry
        }
        
        private fun <V, G> createAdapter(entry: AbstractConfigEntry<V, *, G>) = BeanProxy.IBeanGuiAdapter.of({ v ->
            try {
                @Suppress("UNCHECKED_CAST")
                entry.forGui(v as V)
            } catch (e: ClassCastException) { null }
        }, entry::fromGui)
        
        override fun createCopy(value: B) = Builder(value).also {
            it.entries.putAll(entries)
            it.bakedEntries.putAll(bakedEntries)
            it.caption = caption
            it.iconProvider = iconProvider
            it.allowUneditableProperties = allowUneditableProperties
        }
    }
    
    override fun forConfig(value: D): Map<String, Any> {
        val map = LinkedHashMap<String, Any>(proxy.propertyMap.size)
        for (name in proxy.propertyNames) {
            try {
                @Suppress("UNCHECKED_CAST")
                (entries[name] as? AbstractConfigEntry<Any, *, *>)?.let { entry ->
                    val v = proxy.get(value, name) ?: throw DataClassNullPropertyException(proxy.getPropertyName(name))
                    map[name] = entry.forConfig(v)
                }
            } catch (e: ClassCastException) {
                throw DataClassAccessException(
                    "Error reading data class for config entry $globalPath", e)
            }
        }
        return map
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun fromConfig(value: Map<String, Any>?): D? {
        if (value == null) return null
        return proxy.createFrom(defValue, Maps.transformEntries(entries) { name, v ->
            (v as AbstractConfigEntry<*, Any, *>).fromConfigOrDefault(value[name]!!)
        })
    }
    
    override fun forActualConfig(value: Map<String, Any>?): Any? {
        if (value == null) return null
        val map = LinkedHashMap<String, Any>(value.size)
        entries.forEach { (name, entry) ->
            val gui = value[name]
            try {
                @Suppress("UNCHECKED_CAST")
                (entry as? AbstractConfigEntry<*, Any, *>)?.let {
                    map[name] = it.forActualConfig(gui)
                }
            } catch (e: ClassCastException) {
                LOGGER.error("Error serializing bean entry property \"$name\": $globalPath", e)
            }
        }
        return if (map.isEmpty()) null else map
    }
    
    @Suppress("LABEL_NAME_CLASH")
    override fun fromActualConfig(value: Any?): Map<String, Any>? {
        @Suppress("UNCHECKED_CAST")
        (value as? List<Any>)?.let { seq ->
            val map = linkedMapOf<String, Any>()
            for (o in seq) {
                (o as? Map<*, *>)?.let { mm ->
                    if (mm.size != 1) return null
                    val e = mm.entries.first()
                    val key = (e.key as? String)
                    val entry = entries[key] ?: return@let
                    val v = entry.fromActualConfig(e.value)
                    if (key == null || v == null) return null
                    map[key] = v
                } ?: (o as? Config)?.let { config ->
                    if (config.entrySet().size != 1) return null
                    val e = config.entrySet().first()
                    val key = e.key
                    val entry = entries[key] ?: return@let
                    val v = entry.fromActualConfig(e.getValue())
                    if (key == null || v == null) return null
                    map[key] = v
                }
            }
            return map
        } ?: (value as? Config)?.let { config ->
            val map = linkedMapOf<String, Any>()
            for (e in config.entrySet()) {
                val key = e.key
                val entry = entries[key] ?: continue
                val v = entry.fromActualConfig(e.getValue())
                if (key == null || v == null) return null
                map[key] = v
            }
            return map
        } ?: (value as? Map<*, *>)?.let { mm ->
            val map = linkedMapOf<String, Any>()
            for (e in mm.entries) {
                val key = e.key as? String
                val entry = entries[key] ?: continue
                val v = entry.fromActualConfig(e.value)
                if (key == null || v == null) return null
                map[key] = v
            }
            return map
        }
        return null
    }
    
    override fun getConfigCommentTooltips() = super.getConfigCommentTooltips()!!.apply {
        add("Object: \n  " + entries.entries.joinToString("\n  ") {
            it.key + ": " + it.value.configCommentTooltip.replace(LINE_BREAK, "\n  ").trim()
        })
    }
    
    override fun buildGUIEntry(builder: ConfigFieldBuilder): Optional<FieldBuilder<D, *, *>> {
        val fieldBuilder = builder
          .startBeanField(getDisplayName(), forGui(get()), proxy)
          .withIcon(iconProvider)
        entries.forEach { (name, entry) ->
            if (name == caption) {
                (entry as? AtomicEntry<*>)?.let { keyEntry ->
                    addCaption(
                        builder, fieldBuilder.withoutTags(EntryTag.NON_PERSISTENT, entry.copyTag), name, keyEntry)
                    return@forEach
                } ?: LOGGER.debug("Caption for Bean entry is not a key entry: $globalPath")
            }
            entry.buildGUIEntry(builder).ifPresent {
                fieldBuilder.add(name, it.withoutTags(EntryTag.NON_PERSISTENT, entry.copyTag))
            }
        }
        return Optional.of(decorate(fieldBuilder))
    }
    
    private fun <B, KG, E> addCaption(
      builder: ConfigFieldBuilder, fieldBuilder: BeanFieldBuilder<B>, name: String, keyEntry: AtomicEntry<KG>
    ) where E : AbstractConfigListEntry<KG>, E : IChildListEntry {
        fieldBuilder.caption<KG, E, Nothing>(name, keyEntry.buildAtomicChildGUIEntry<E, Nothing>(builder))
    }
    
    class DataClassIntrospectionException : RuntimeException {
        constructor(message: String) : super(message)
        constructor(message: String, cause: Throwable) : super(message, cause)
    }
    
    class DataClassAccessException : RuntimeException {
        constructor(message: String) : super(message)
        constructor(message: String, cause: Throwable) : super(message, cause)
    }
    
    class DataClassNullPropertyException : RuntimeException {
        companion object {
            private fun getMessage(path: String) =
              "Null bean property value: $path\nConfigurable beans cannot have nullable properties."
        }
        
        constructor(path: String) : super(getMessage(path))
        constructor(path: String, cause: Throwable) : super(getMessage(path), cause)
    }
}

class DataClassProxy<B: Any>(
  val defValue: B, val adapters: Map<String, BeanProxy.IBeanGuiAdapter>,
  val bakedProperties: Map<String, B.() -> Any?> = emptyMap()
): BeanProxy<B> {
    companion object {
        private val propertyMapCache = mutableMapOf<KClass<*>, Map<String, KProperty<*>>>()
    }
    
    @Suppress("UNCHECKED_CAST")
    private val klass = defValue::class as KClass<B>
    val propertyMap = propertyMapCache.getOrPut(klass) {
        klass.memberProperties.associateBy { it.name }
    }
    val propertyNames get() = propertyMap.keys
    
    override fun create(properties: MutableMap<String, Any?>?) = createFrom(defValue, properties)
    override fun createFrom(def: B, properties: MutableMap<String, Any?>?) = createFrom(def, properties) { _, v -> v }
    override fun createFromGUI(def: B, properties: MutableMap<String, Any?>?) = createFrom(def, properties) { name, v ->
        adapters[name]?.fromGui(v) ?: v
    }
    
    private fun createFrom(def: B, properties: Map<String, Any?>?, skipBake: Boolean = false, transform: (String, Any?) -> Any?) =
      klass.primaryConstructor!!.run {
          call(*parameters.map { p ->
              properties?.get(p.name!!)?.let { transform(p.name!!, it) } ?: get(def, p.name!!)
          }.toTypedArray())
      }.also {
          properties?.forEach { (key, value) ->
              if (key !in propertyNames)
                  (propertyMap[key] as? KMutableProperty<*>)?.setter?.call(it, transform(key, value))
          }
      }.let { if (skipBake) it else it.bakeProperties() }
    
    private fun B.bakeProperties(): B = if (bakedProperties.keys.any { propertyMap[it] !is KMutableProperty<*> }) {
        createFrom(this, Maps.transformValues(bakedProperties) { it() }, skipBake = true) { _, v -> v }
    } else apply {
        bakedProperties.forEach { (k, v) ->
            (propertyMap[k] as KMutableProperty<*>).setter.call(this, v())
        }
    }
    
    override fun get(bean: B, name: String) = propertyMap[name]?.getter?.call(bean)
    override fun getGUI(bean: B, name: String) = adapters[name]?.forGui(get(bean, name))
    
    override fun getTypeName() = klass.simpleName!!
    override fun getPropertyName(name: String) = "$typeName$$name"
    override fun getTypeTranslation(): String = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, typeName)
    override fun getTranslation(property: String): String = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, property)
}