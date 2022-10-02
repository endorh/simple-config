package endorh.simpleconfig.konfig

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import endorh.simpleconfig.api.*
import endorh.simpleconfig.api.AbstractRange.*
import endorh.simpleconfig.api.SimpleConfig.Type
import endorh.simpleconfig.api.ui.icon.Icon
import endorh.simpleconfig.core.AbstractConfigEntry
import endorh.simpleconfig.core.AbstractConfigEntryBuilder
import endorh.simpleconfig.core.AbstractSimpleConfigEntryHolderBuilder
import net.minecraft.commands.CommandSourceStack
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.fml.ModLoadingContext
import thedarkcolour.kotlinforforge.forge.MOD_BUS
import kotlin.math.max
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import endorh.simpleconfig.api.ConfigBuilderFactoryProxy as F
import endorh.simpleconfig.api.ConfigEntryBuilder as Builder

abstract class AbstractKonfig<B: ConfigEntryHolderBuilder<*>> internal constructor(): SimpleKonfigBuilders() {
    internal abstract val builder: B
    internal val bakedProperties = mutableListOf<BakedPropertyDelegate<*>>()
    
    internal fun bakeProperties() {
        @Suppress("UNCHECKED_CAST")
        (this::class as KClass<AbstractKonfig<*>>).memberProperties.mapNotNull {
            it.isAccessible = true
            it.getDelegate(this) as? IEntryDelegate
        }.forEach { it.bake() }
        bakedProperties.forEach {
            it.bake()
        }
    }
    
    /**
     * Called everytime configuration values are updated, after every bound property
     * has been baked.
     *
     * You may use this method to post-process some configuration values, or to compute
     * dependent values, if [baked] properties aren't enough for your case.
     */
    protected open fun bake() {}
    
    protected operator fun <V, C, G, B: Builder<V, C, G, B>> B.provideDelegate(
      thisRef: Any, property: KProperty<*>
    ): EntryDelegate<V, C, G, B> {
        val lineNumber = max(0, Thread.currentThread().stackTrace[2].lineNumber)
        if (this@provideDelegate !is AbstractConfigEntryBuilder<*, *, *, *, *, *>)
            throw IllegalArgumentException("Entry builder not instance of AbstractConfigEntryBuilder")
        return EntryDelegate(this as B) {
            (builder as AbstractSimpleConfigEntryHolderBuilder<*>).add(lineNumber, property.name, it)
        }
    }
    
    /**
     * Create a baked property, which can depend on the values of bound properties and prior
     * baked properties.
     *
     * It is recomputed everytime onfiguration values are updated, before the [bake] method is called.
     */
    protected fun <T: Any> baked(value: () -> T): BakedPropertyDelegate<T> {
        return BakedPropertyDelegate(value).also { bakedProperties.add(it) }
    }
    
    protected fun <T, V, C, G, B: Builder<V, C, G, B>> B.baked(transform: (V) -> T) =
      TransformingEntryDelegate.Provider(builder, this, transform)
    
    internal interface IEntryDelegate {
        fun bake()
    }
    
    @Suppress("UNCHECKED_CAST")
    class EntryDelegate<V, Config, Gui, B : Builder<V, Config, Gui, B>>(
      entryBuilder: B, registrar: (B) -> Unit
    ) : ReadWriteProperty<Any, V>, IEntryDelegate {
        lateinit var entry: AbstractConfigEntry<V, Config, Gui>
        var cachedValue: V = entryBuilder.value
        
        init {
            val abstractBuilder = (
              entryBuilder as? AbstractConfigEntryBuilder<V, Config, Gui, *, B, *>
              ?: throw IllegalArgumentException(
                  "EntryBuilder must be an instance of AbstractConfigEntryBuilder"
              )).withBuildListener { entry = it }
            registrar(abstractBuilder as B)
        }
        
        override fun getValue(thisRef: Any, property: KProperty<*>): V = cachedValue
        override fun setValue(thisRef: Any, property: KProperty<*>, value: V) {
            entry.set(value)
            bake()
        }
        
        override fun bake() {
            cachedValue = entry.get()
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    class TransformingEntryDelegate<T, V, C, G, B: Builder<V, C, G, B>>(
      entryBuilder: B, val transform: (V) -> T, registrar: (B) -> Unit
    ) : ReadOnlyProperty<Any, T>, IEntryDelegate {
        lateinit var entry: AbstractConfigEntry<V, C, G>
        var cachedValue: T = transform(entryBuilder.value)
        
        init {
            val abstractBuilder = (
              entryBuilder as? AbstractConfigEntryBuilder<V, C, G, *, B, *>
              ?: throw IllegalArgumentException(
                  "EntryBuilder must be an instance of AbstractConfigEntryBuilder"
              )).withBuildListener { entry = it }
            registrar(abstractBuilder as B)
        }
        
        override fun getValue(thisRef: Any, property: KProperty<*>): T = cachedValue
        override fun bake() {
            cachedValue = transform(entry.get())
        }
        
        class Provider<T, V, C, G, B: Builder<V, C, G, B>>(
          val builder: ConfigEntryHolderBuilder<*>, val entryBuilder: B, val transform: (V) -> T
        ) {
            operator fun provideDelegate(
              thisRef: Any, property: KProperty<*>
            ) = TransformingEntryDelegate(entryBuilder, transform) {
                builder.add(property.name, it)
            }
            
            fun configure(configure: B.() -> B) =
              Provider(builder, entryBuilder.configure(), transform)
            operator fun invoke(configure: B.() -> B) = configure(configure)
        }
    }
    
    class BakedPropertyDelegate<V: Any>(
      val value: () -> V
    ) : ReadOnlyProperty<Any, V> {
        lateinit var bakedValue: V
        override fun getValue(thisRef: Any, property: KProperty<*>) = bakedValue
        fun bake() {
            bakedValue = value()
        }
    }
}

open class SimpleKonfig protected constructor(
  type: Type, modId: String = THREAD_MOD_ID,
  configure: (SimpleConfigBuilder.() -> Unit)? = null
): AbstractKonfig<SimpleConfigBuilder>() {
    companion object {
        private val THREAD_MOD_ID = ModLoadingContext.get().activeContainer.modId
        
        fun buildAndRegister(
          client: SimpleKonfig? = null, server: SimpleKonfig? = null, common: SimpleKonfig? = null
        ) = listOfNotNull(client, server, common).forEach { build(it) }
    
        private fun build(config: SimpleKonfig) {
            loadFully(config)
            config.builder.buildAndRegister(MOD_BUS)
        }
    
        private fun loadFully(config: AbstractKonfig<*>) {
            config::class.nestedClasses.forEach { kls ->
                (kls.objectInstance as? AbstractKonfig<*>)?.let { loadFully(it) }
            }
        }
    }
    
    final override val builder: SimpleConfigBuilder = F.config(modId, type).also {
        configure?.invoke(it)
    }.withBaker {
        bakeProperties()
        bake(it)
        bake()
    }
    
    protected constructor(
      type: Type, modId: String = THREAD_MOD_ID,
      background: String? = null, color: Long? = null, icon: Icon? = null,
      commandRoot: LiteralArgumentBuilder<CommandSourceStack>? = null,
      solidInGameBackground: Boolean = false,
      backgroundLocation: ResourceLocation? = null
    ): this(type, modId, {
        backgroundLocation?.let { withBackground(it) }
        ?: background?.let { withBackground(it) }
        color?.let { withColor(it.toInt()) }
        icon?.let { withIcon(it) }
        commandRoot?.let { withCommandRoot(it) }
        if (solidInGameBackground) withSolidInGameBackground()
    })
    
    protected open fun bake(config: SimpleConfig) {}
}

typealias category = KonfigCategory
open class KonfigCategory protected constructor(
  configure: (ConfigCategoryBuilder.() -> Unit)? = null
): AbstractKonfig<ConfigCategoryBuilder>() {
    
    final override val builder: ConfigCategoryBuilder = F.category(this::class.simpleName!!).apply {
        configure?.invoke(this)
    }.withBaker {
        bakeProperties()
        bake(it)
        bake()
    }
    
    init {
        val lineNumber = Thread.currentThread().stackTrace.first {
            it.methodName != "getStackTrace" && it.className != KonfigCategory::class.qualifiedName
        }.lineNumber.takeIf { it >= 0 } ?: Int.MAX_VALUE
        val konfig = this::class.java.declaringClass.kotlin.objectInstance as? SimpleKonfig
                     ?: throw IllegalStateException("KonfigCategory must be a nested object of a SimpleKonfig object")
        konfig.builder.n(builder, lineNumber)
    }
    
    protected constructor(
      background: String? = null, color: Long? = null, icon: Icon? = null,
      backgroundLocation: ResourceLocation? = null
    ) : this({
        backgroundLocation?.let { withBackground(it) }
        ?: background?.let { withBackground(it) }
        color?.let { withColor(it.toInt()) }
        icon?.let { withIcon(it) }
    })
    
    protected open fun bake(category: SimpleConfigCategory) {}
}

typealias group = KonfigGroup
open class KonfigGroup protected constructor(
  expand: Boolean = false, configure: (ConfigGroupBuilder.() -> Unit)? = null
): AbstractKonfig<ConfigGroupBuilder>() {
    final override val builder: ConfigGroupBuilder = F.group(this::class.simpleName!!, expand).apply {
        configure?.invoke(this)
    }.withBaker {
        bakeProperties()
        bake(it)
        bake()
    }
    
    init {
        val lineNumber = Thread.currentThread().stackTrace.first {
            it.methodName != "getStackTrace" && it.className != KonfigGroup::class.qualifiedName
        }.lineNumber.takeIf { it >= 0 } ?: Int.MAX_VALUE
        val konfig = this::class.java.declaringClass.kotlin.objectInstance as? AbstractKonfig<*>
                     ?: throw IllegalStateException("KonfigGroup must be a nested object of a SimpleKonfig/KonfigCategory/KonfigGroup object")
        (konfig.builder as AbstractSimpleConfigEntryHolderBuilder<*>).n(builder, lineNumber)
    }
    
    protected open fun bake(group: SimpleConfigGroup) {}
    
    protected fun <V, C, G, B: Builder<V, C, G, B>> caption(entryBuilder: B) =
      CaptionWrapperDelegateProvider(builder, entryBuilder)
    
    class CaptionWrapperDelegateProvider<V, C, G, B: Builder<V, C, G, B>>(
      val builder: ConfigGroupBuilder, val entryBuilder: B
    ) {
        operator fun provideDelegate(
          thisRef: Any, property: KProperty<*>
        ) = EntryDelegate(entryBuilder) {
            builder.caption(property.name, it)
        }
    }
}