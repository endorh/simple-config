package endorh.simpleconfig.konfig

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import endorh.simpleconfig.api.*
import endorh.simpleconfig.api.AbstractRange.*
import endorh.simpleconfig.api.SimpleConfig.Type
import endorh.simpleconfig.api.entry.*
import endorh.simpleconfig.api.ui.icon.Icon
import endorh.simpleconfig.core.AbstractConfigEntry
import endorh.simpleconfig.core.AbstractConfigEntryBuilder
import endorh.simpleconfig.core.AbstractSimpleConfigEntryHolderBuilder
import endorh.simpleconfig.konfig.builders.*
import endorh.simpleconfig.konfig.commons.toCommonsPair
import endorh.simpleconfig.konfig.commons.toCommonsTriple
import endorh.simpleconfig.konfig.commons.toTriple
import net.minecraft.command.CommandSource
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.ModLoadingContext
import thedarkcolour.kotlinforforge.forge.MOD_BUS
import kotlin.Pair
import kotlin.Triple
import kotlin.also
import kotlin.apply
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import endorh.simpleconfig.api.ConfigBuilderFactoryProxy as F
import endorh.simpleconfig.api.ConfigEntryBuilder as Builder
import org.apache.commons.lang3.tuple.Pair as CPair
import org.apache.commons.lang3.tuple.Triple as CTriple

private fun getLineNumber() = Thread.currentThread().stackTrace.first {
    it.methodName != "getStackTrace" && it.fileName != "SimpleKonfig.kt"
}.lineNumber.takeIf { it >= 0 }

private fun ConfigEntryHolderBuilder<*>.add(order: Int, name: String, builder: Builder<*, *, *, *>) =
  (this as AbstractSimpleConfigEntryHolderBuilder<*>).add(order, name, builder)

/**
 * Base class for [SimpleKonfig], [category]s and [group]s.
 *
 * Exposes many protected methods to inheritors so they can conveniently
 * define config properties using config entry builders from [endorh.simpleconfig.konfig.builders]
 * as delegates.
 */
abstract class AbstractKonfig<B: ConfigEntryHolderBuilder<*>> internal constructor() {
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
     * You may override this method to post-process some configuration values, or to compute
     * dependent values, if [baked] properties aren't enough for your case.
     *
     * You shouldn't need to call this method yourself.
     */
    protected open fun onBake() {}
    
    private fun checkBuilder(builder: Builder<*, *, *, *>) {
        if (builder !is AbstractConfigEntryBuilder<*, *, *, *, *, *>)
            throw IllegalArgumentException("Entry builder not instance of AbstractConfigEntryBuilder")
    }
    
    // Kotlin type adapters
    /**
     * Adapt this entry to use Kotlin [Pair]s rather than Apache Commons [CPair]s
     */
    protected fun <L, R, LC, RC, LG, RG> EntryPairEntryBuilder<L, R, LC, RC, LG, RG>.toKotlin() =
      baked { it.toPair() } writer { it.toCommonsPair() }
    /**
     * Adapt this entry to use Kotlin [Triple]s rather than Apache Commons [CTriple]s
     */
    protected fun <L, M, R, LC, MC, RC, LG, MG, RG> EntryTripleEntryBuilder<L, M, R, LC, MC, RC, LG, MG, RG>.toKotlin() =
      baked { it.toTriple() } writer { it.toCommonsTriple() }
    /**
     * Adapt this entry to use Kotlin [Pair]s rather than Apache Commons [CPair]s
     */
    protected fun <K, V, KC, C, KG, G> EntryPairListEntryBuilder<K, V, KC, C, KG, G, *, *>.toKotlin() =
      baked { l -> l.map { it.toPair() } } writer { l -> l.map { it.toCommonsPair() } }
    
    // Make entry builders delegate providers
    protected operator fun <V, C, G, B: Builder<V, C, G, B>> B.provideDelegate(
      thisRef: Any, property: KProperty<*>
    ): EntryDelegate<V, C, G, B> = createEntryDelegate(property)
    
    private fun <V, C, G, B : Builder<V, C, G, *>> B.createEntryDelegate(
      property: KProperty<*>
    ): EntryDelegate<V, C, G, B> {
        checkBuilder(this)
        return EntryDelegate(this) {
            builder.add(getLineNumber() ?: 0, property.name, it)
        }
    }
    
    // Specific delegate providers for collection types to prevent delegate type inference
    //   from depending on a platform call, which would produce a warning in the config
    //   declaration, suggesting to specify the type explicitly
    protected operator fun <V, C, G> EntryListEntryBuilder<V, C, G, *>.provideDelegate(
      thisRef: Any, property: KProperty<*>
    ): EntryDelegate<List<V>, List<C>, List<G>, *> = createEntryDelegate(property)
    protected operator fun <V, C, G> EntrySetEntryBuilder<V, C, G, *>.provideDelegate(
      thisRef: Any, property: KProperty<*>
    ): EntryDelegate<Set<V>, Set<C>, List<G>, *> = createEntryDelegate(property)
    protected operator fun <K, V, KC, C, KG, G> EntryMapEntryBuilder<K, V, KC, C, KG, G, *, *>.provideDelegate(
      thisRef: Any, property: KProperty<*>
    ): EntryDelegate<Map<K, V>, Map<KC, C>, List<CPair<KG, G>>, *> = createEntryDelegate(property)
    // Pair lists are automatically transformed to Kotlin pairs
    protected operator fun <K, V, KC, C, KG, G> EntryPairListEntryBuilder<K, V, KC, C, KG, G, *, *>.provideDelegate(
      thisRef: Any, property: KProperty<*>
    ): WritableTransformingEntryDelegate<
      List<Pair<K, V>>, List<CPair<K, V>>, List<CPair<KC, C>>, List<CPair<KG, G>>, *
    > = toKotlin().provideDelegate(thisRef, property)
    
    protected operator fun ByteListEntryBuilder.provideDelegate(thisRef: Any, property: KProperty<*>):
      EntryDelegate<List<Byte>, List<Number>, List<Int>, *> = createEntryDelegate(property)
    protected operator fun ShortListEntryBuilder.provideDelegate(thisRef: Any, property: KProperty<*>):
      EntryDelegate<List<Short>, List<Number>, List<Int>, *> = createEntryDelegate(property)
    protected operator fun IntegerListEntryBuilder.provideDelegate(thisRef: Any, property: KProperty<*>):
      EntryDelegate<List<Int>, List<Number>, List<Int>, *> = createEntryDelegate(property)
    protected operator fun LongListEntryBuilder.provideDelegate(thisRef: Any, property: KProperty<*>):
      EntryDelegate<List<Long>, List<Number>, List<Long>, *> = createEntryDelegate(property)
    protected operator fun FloatListEntryBuilder.provideDelegate(thisRef: Any, property: KProperty<*>):
      EntryDelegate<List<Float>, List<Number>, List<Float>, *> = createEntryDelegate(property)
    protected operator fun DoubleListEntryBuilder.provideDelegate(thisRef: Any, property: KProperty<*>):
      EntryDelegate<List<Double>, List<Number>, List<Double>, *> = createEntryDelegate(property)
    protected operator fun StringListEntryBuilder.provideDelegate(thisRef: Any, property: KProperty<*>):
      EntryDelegate<List<String>, List<String>, List<String>, *> = createEntryDelegate(property)
    
    // Automatic transformation to Kotlin types
    protected operator fun <L, R, LC, RC, LG, RG> EntryPairEntryBuilder<L, R, LC, RC, LG, RG>.provideDelegate(
      thisRef: Any, property: KProperty<*>
    ): WritableTransformingEntryDelegate<Pair<L, R>, CPair<L, R>, CPair<LC, RC>, CPair<LG, RG>, *> =
      toKotlin().provideDelegate(thisRef, property)
    protected operator fun <L, M, R, LC, MC, RC, LG, MG, RG> EntryTripleEntryBuilder<L, M, R, LC, MC, RC, LG, MG, RG>.provideDelegate(
      thisRef: Any, property: KProperty<*>
    ): WritableTransformingEntryDelegate<Triple<L, M, R>, CTriple<L, M, R>, CTriple<LC, MC, RC>, CTriple<LG, MG, RG>, *> =
      toKotlin().provideDelegate(thisRef, property)
    
    /**
     * Create a baked property, which can depend on the values of bound properties and prior
     * baked properties.
     *
     * It is recomputed everytime configuration values are updated, before the [onBake] method is called.
     */
    protected fun <T: Any> baked(value: () -> T) =
      BakedPropertyDelegate(value).also { bakedProperties.add(it) }
    
    /**
     * Create a baked config entry, which adapts the value saved in its property
     * every time it's changed, possibly into a different type.
     *
     * This is mostly useful to transform a config value into a different type, or
     * to apply unit conversion to numeric values, so the mod code has access to
     * a more convenient unit than the one exposed to users.
     */
    protected infix fun <T, V, C, G, B: Builder<V, C, G, B>> B.baked(transform: (V) -> T) =
      TransformingEntryDelegate.Provider(builder, this, transform)
    
    /**
     * Common interface for entry delegates, so they can all be notified when
     * configuration has changed.
     */
    interface IEntryDelegate {
        fun bake()
    }
    
    /**
     * Basic entry delegate.
     *
     * Wraps an [AbstractConfigEntry], and caches its value on every bake.
     * Writing to this property writes through to the entry, and updates
     * the cache.
     */
    @Suppress("UNCHECKED_CAST")
    class EntryDelegate<V, Config, Gui, B : Builder<V, Config, Gui, *>>(
      entryBuilder: B, registrar: (B) -> Unit
    ) : ReadWriteProperty<Any, V>, IEntryDelegate {
        lateinit var entry: AbstractConfigEntry<V, Config, Gui>
        var cachedValue: V = entryBuilder.value
        
        init {
            val abstractBuilder = (
              entryBuilder as? AbstractConfigEntryBuilder<V, Config, Gui, *, *, *>
              ?: throw IllegalArgumentException(
                  "EntryBuilder must be an instance of AbstractConfigEntryBuilder"
              )).withBuildListener { entry = it }
            registrar(abstractBuilder as B)
        }
        
        override fun getValue(thisRef: Any, property: KProperty<*>): V = cachedValue!!
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
          internal val builder: ConfigEntryHolderBuilder<*>,
          internal val entryBuilder: B, internal val transform: (V) -> T
        ): PropertyDelegateProvider<Any, TransformingEntryDelegate<T, V, C, G, B>> {
            override fun provideDelegate(thisRef: Any, property: KProperty<*>) =
              TransformingEntryDelegate(entryBuilder, transform) {
                  builder.add(getLineNumber() ?: 0, property.name, it)
              }
            
            fun configure(configure: B.() -> B) =
              Provider(builder, entryBuilder.configure(), transform)
            operator fun invoke(configure: B.() -> B) = configure(configure)
            
            infix fun writer(writer: (T) -> V) =
              WritableTransformingEntryDelegate.Provider(builder, entryBuilder, transform, writer)
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    class WritableTransformingEntryDelegate<T, V, C, G, B: Builder<V, C, G, B>>(
      entryBuilder: B, val transform: (V) -> T, val inverse: (T) -> V, registrar: (B) -> Unit
    ) : ReadWriteProperty<Any, T>, IEntryDelegate {
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
    
        override fun getValue(thisRef: Any, property: KProperty<*>) = cachedValue
        override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
            entry.set(inverse(value))
            bake()
        }
    
        override fun bake() {
            cachedValue = transform(entry.get())
        }
        
        class Provider<T, V, C, G, B: Builder<V, C, G, B>>(
          internal val builder: ConfigEntryHolderBuilder<*>, internal val entryBuilder: B,
          internal val transform: (V) -> T, internal val inverse: (T) -> V
        ): PropertyDelegateProvider<Any, WritableTransformingEntryDelegate<T, V, C, G, B>> {
            override fun provideDelegate(thisRef: Any, property: KProperty<*>) =
              WritableTransformingEntryDelegate(entryBuilder, transform, inverse) {
                  builder.add(getLineNumber() ?: 0, property.name, it)
              }
            
            fun configure(configure: B.() -> B) =
              Provider(builder, entryBuilder.configure(), transform, inverse)
            operator fun invoke(configure: B.() -> B) = configure(configure)
        }
    }
    
    class BakedPropertyDelegate<V: Any>(val value: () -> V) : ReadOnlyProperty<Any, V> {
        lateinit var bakedValue: V
        override fun getValue(thisRef: Any, property: KProperty<*>) = bakedValue
        fun bake() {
            bakedValue = value()
        }
    }
}

/**
 * Base class for Simple Konfig objects.
 *
 * Should only be subclassed by `object`s, one per Konfig [Type] per mod.
 *
 * Register a [SimpleKonfig] object by passing it to [SimpleKonfig.registerConfig]
 * from your mod's constructor. (see the sample below)
 *
 * Define subcategories by declaring nested objects that extend [category], or
 * config groups extending [group].
 * Categories must be top level, but groups can contain other groups without limit.
 * It's up to you to judge which way of organizing the config entries is *simple*r
 * for the players.
 *
 * Within your config object, categories and groups, define config entries using
 * [delegated properties](https://kotlinlang.org/docs/delegated-properties.html)
 * and the entry builders from [endorh.simpleconfig.konfig.builders].
 *
 * You may also use [baked] to define baked properties, computed from others (and
 * updated on any config changes).
 *
 * You may also call [baked] on an entry to adapt its value
 * (for example, converting units, or wrapping it with a different type).
 * The user will be able to edit the original entry, but your code will access
 * the adapted value, which is updated after any config changes.
 *
 * All [SimpleConfig] entries are baked on config changes, so you don't need
 * to perform your own caching of them, unless you need to use a specific value
 * for an operation spanning multiple ticks without the risk of the config being
 * changed halfway through.
 *
 * ## Sample Usage
 * ### Declaring the config file
 * ```
 * object ClientKonfig : SimpleKonfig(Type.CLIENT) {
 *     // Simple entries
 *     val number by int(10).range(0, 20)
 *     val string by string("Hello, konfig!").maxLength(30)
 *     val direction by option(Direction.UP)
 *
 *     // Complex entries (passing entry builders as arguments to others)
 *     val nameList by list(string(), listOf("Steve", "Alex"))
 *     val colorMap by linkedMap(string(), color(Color.GRAY), mapOf(
 *         "Steve" to Color.BLUE,
 *         "Alex" to Color.GREEN
 *     ))
 *     val itemBlockPair by pair(item(Items.OAK_PLANKS), block(Blocks.OAK_PLANKS))
 *
 *     // Subgroup, displayed as a collapsible list of entries
 *     object Times : group(expand=true) {
 *         // Simple entry
 *         val time by double(1.0) // In seconds
 *         // Computed entry (updated on any config change)
 *         val timeInTicks by baked { (time * 20).toInt() }
 *         // Transformed entry (stored/displayed as seconds, directly available as ticks)
 *         val timeInTicks2 by double(1.0) baked { (it * 20).toInt() }
 *     }
 *
 *     // Subcategory, displayed as a tab in the menu (must be top level)
 *     object Items : category(color=0x8080A0A0) {
 *         val apple by item(Items.APPLE)
 *
 *         // Categories may contain groups
 *         object SubGroup : group(expand=true) {
 *             // Groups may contain other groups, without any limitation
 *             object NestedSubGroup : group() {
 *                 val fluid by fluid(Fluids.WATER)
 *             }
 *         }
 *     }
 * }
 * ```
 * ### Registering the config file
 * ```
 * @Mod(MyMod.MOD_ID)
 * object MyMod {
 *     const val MOD_ID = "mymod"
 *     init {
 *         SimpleKonfig.buildAndRegister(ClientKonfig)
 *     }
 * }
 * ```
 * ### Tips
 * You may use a shorter name instead of `ClientKonfig`, since config entries may
 * be used from all over your code. You may also use a typealias to achieve this.
 * ```
 * typealias C = ClientKonfig
 * ```
 */
open class SimpleKonfig protected constructor(
  type: Type, modId: String = THREAD_MOD_ID,
  configure: (SimpleConfigBuilder.() -> Unit)? = null
): AbstractKonfig<SimpleConfigBuilder>() {
    companion object {
        private val THREAD_MOD_ID = ModLoadingContext.get().activeContainer.modId
    
        /**
         * Register the [SimpleKonfig]s for your mod.
         *
         * Call this method from your mod constructor. It ensures that your
         * [SimpleKonfig] objects, and all their nested categories and groups are
         * loaded and registered to the Simple Config API.
         */
        fun registerConfig(
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
        onBake(it)
        onBake()
    }
    
    /**
     * Define a [SimpleKonfig] object, which declares a config file for your mod.
     *
     * See [SimpleKonfig] for an example usage.
     *
     * @param type The [Type] of the config file declared by this object.
     * @param modId The mod ID of your mod. If omitted, it is inferred from the thread this object
     * is created, which for most cases will be your mod's thread.
     * @param background The background used in the config menu, as a texture location.
     * @param color The tint color of the main category button in the menu, in ARGB format.
     * @param icon The [Icon] used in the main category button of the menu.
     * @param commandRoot An alternate command root to register the config command for your mod.
     * The command will still be available under the `/config` command using your mod ID.
     * This is only useful in case you want a direct command to configure your mod.
     * @param solidInGameBackground Pass true to use the background menu texture even when in-game.
     * @param backgroundLocation Background texture location, as a [ResourceLocation] instead.
     */
    protected constructor(
      type: Type, modId: String = THREAD_MOD_ID,
      background: String? = null, color: Long? = null, icon: Icon? = null,
      commandRoot: LiteralArgumentBuilder<CommandSource>? = null,
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
    
    /**
     * Called when the config is baked.
     *
     * You will rarely ever need the [config] parameter, from the Java
     * Simple Config API, so you should instead override [AbstractKonfig.onBake].
     */
    protected open fun onBake(config: SimpleConfig) {}
}

/**
 * Define a [KonfigCategory] object, which declares a config category under a [SimpleKonfig]
 * object.
 *
 * Config categories have their own tab, with a tint color, icon and own background.
 * Otherwise, the background defaults to the config's one.
 *
 * You may define config entries within categories, just as within a [SimpleKonfig] object.
 *
 * See [SimpleKonfig] for an usage sample.
 *
 * @see SimpleKonfig
 * @see group
 */
typealias category = KonfigCategory
open class KonfigCategory protected constructor(
  configure: (ConfigCategoryBuilder.() -> Unit)? = null
): AbstractKonfig<ConfigCategoryBuilder>() {
    
    final override val builder: ConfigCategoryBuilder = F.category(this::class.simpleName!!).apply {
        configure?.invoke(this)
    }.withBaker {
        bakeProperties()
        onBake(it)
        onBake()
    }
    
    init {
        val lineNumber = Thread.currentThread().stackTrace.first {
            it.methodName != "getStackTrace" && it.className != KonfigCategory::class.qualifiedName
        }.lineNumber.takeIf { it >= 0 } ?: Int.MAX_VALUE
        val konfig = this::class.java.declaringClass.kotlin.objectInstance as? SimpleKonfig
                     ?: throw IllegalStateException("KonfigCategory must be a nested object of a SimpleKonfig object")
        konfig.builder.n(builder, lineNumber)
    }
    
    /**
     * Define a [KonfigCategory] object, which declares a config category under a [SimpleKonfig]
     * object.
     *
     * Config categories have their own tab, with a tint color, icon and own background.
     * Otherwise, the background defaults to the config's one.
     *
     * @param background The background used in the config menu, as a texture location.
     * @param color The tint color of the category tab button in the menu, in ARGB format.
     * @param icon The [Icon] used in the category tab button of the menu.
     * @param backgroundLocation Background texture location, as a [ResourceLocation] instead.
     */
    protected constructor(
      background: String? = null, color: Long? = null, icon: Icon? = null,
      backgroundLocation: ResourceLocation? = null
    ) : this({
        backgroundLocation?.let { withBackground(it) }
        ?: background?.let { withBackground(it) }
        color?.let { withColor(it.toInt()) }
        icon?.let { withIcon(it) }
    })
    
    /**
     * Called when the config is baked.
     *
     * You will rarely ever need the [category] parameter, from the Java
     * Simple Config API, so you should instead override [AbstractKonfig.onBake].
     */
    protected open fun onBake(category: SimpleConfigCategory) {}
}

/**
 * Define a [KonfigGroup] object, which declares a config group under a
 * [SimpleKonfig], [category] or another [KonfigGroup] object.
 *
 * Config groups do not have their own tab, like categories. They appear as
 * collapsible lists of entries in the menu. Groups can be made expanded by default.
 *
 * You may define config entries within groups, just as within a [SimpleKonfig] object.
 *
 * Config groups may also define a special entry (and only one), which is displayed besides
 * the group's title in the menu, visible even when the group is collapsed. This entry is
 * called the group's caption entry, and you may mark it using [KonfigGroup.caption] to
 * wrap an entry builder.
 *
 * See [SimpleKonfig] for an usage sample.
 *
 * @see SimpleKonfig
 * @see category
 */
typealias group = KonfigGroup
/**
 * @see group
 */
open class KonfigGroup protected constructor(
  expand: Boolean = false, configure: (ConfigGroupBuilder.() -> Unit)? = null
): AbstractKonfig<ConfigGroupBuilder>() {
    final override val builder: ConfigGroupBuilder = F.group(this::class.simpleName!!, expand).apply {
        configure?.invoke(this)
    }.withBaker {
        bakeProperties()
        onBake(it)
        onBake()
    }
    
    init {
        val lineNumber = Thread.currentThread().stackTrace.first {
            it.methodName != "getStackTrace" && it.className != KonfigGroup::class.qualifiedName
        }.lineNumber.takeIf { it >= 0 } ?: Int.MAX_VALUE
        val konfig = this::class.java.declaringClass.kotlin.objectInstance as? AbstractKonfig<*>
                     ?: throw IllegalStateException("KonfigGroup must be a nested object of a SimpleKonfig/KonfigCategory/KonfigGroup object")
        (konfig.builder as AbstractSimpleConfigEntryHolderBuilder<*>).n(builder, lineNumber)
    }
    
    /**
     * Called when the config is baked.
     *
     * You will rarely ever need the [group] parameter, from the Java
     * Simple Config API, so you should instead override [AbstractKonfig.onBake].
     */
    protected open fun onBake(group: SimpleConfigGroup) {}
    
    // Caption entries
    
    /**
     * Wrap an entry as the caption of a [KonfigGroup]
     *
     * The entry will be displayed besides the group's title in the menu,
     * visible even when the group is collapsed.
     *
     * Only one entry within a group may be marked as a caption entry.
     * Only entry builders marked as [AtomicEntryBuilder] can be used.
     */
    protected fun <V, C, G, B> caption(entryBuilder: B)
    where B : Builder<V, C, G, B>, B: AtomicEntryBuilder =
      CaptionWrapperDelegateProvider(builder, entryBuilder)
    
    /**
     * Wrap an entry as the caption of a [KonfigGroup]
     *
     * The entry will be displayed besides the group's title in the menu,
     * visible even when the group is collapsed.
     *
     * Only one entry within a group may be marked as a caption entry.
     * Only entry builders marked as [AtomicEntryBuilder] can be used.
     */
    protected fun <T, V, C, G, B> caption(
      provider: TransformingEntryDelegate.Provider<T, V, C, G, B>
    ) where B : Builder<V, C, G, B>, B: AtomicEntryBuilder =
      TransformingCaptionWrapperDelegateProvider(
          builder, provider.entryBuilder, provider.transform)
    
    /**
     * Wrap an entry as the caption of a [KonfigGroup]
     *
     * The entry will be displayed besides the group's title in the menu,
     * visible even when the group is collapsed.
     *
     * Only one entry within a group may be marked as a caption entry.
     * Only entry builders marked as [AtomicEntryBuilder] can be used.
     */
    protected fun <T, V, C, G, B> caption(
      provider: WritableTransformingEntryDelegate.Provider<T, V, C, G, B>
    ) where B : Builder<V, C, G, B>, B: AtomicEntryBuilder =
      WritableTransformingCaptionWrapperDelegateProvider(
          builder, provider.entryBuilder, provider.transform, provider.inverse)
    
    // Perform automatic transformation to Kotlin types also on caption entries
    
    /**
     * Wrap an entry as the caption of a [KonfigGroup]
     *
     * The entry will be displayed besides the group's title in the menu,
     * visible even when the group is collapsed.
     *
     * Only one entry within a group may be marked as a caption entry.
     * Only entry builders marked as [AtomicEntryBuilder] can be used.
     */
    protected fun <L, R, LC, RC, LG, RG> caption(
      entryBuilder: EntryPairEntryBuilder<L, R, LC, RC, LG, RG>
    ) = caption(entryBuilder.toKotlin())
    
    /**
     * Wrap an entry as the caption of a [KonfigGroup]
     *
     * The entry will be displayed besides the group's title in the menu,
     * visible even when the group is collapsed.
     *
     * Only one entry within a group may be marked as a caption entry.
     * Only entry builders marked as [AtomicEntryBuilder] can be used.
     */
    protected fun <L, M, R, LC, MC, RC, LG, MG, RG> caption(
      entryBuilder: EntryTripleEntryBuilder<L, M, R, LC, MC, RC, LG, MG, RG>
    ) = caption(entryBuilder.toKotlin())
    
    // Caption delegate providers
    
    class CaptionWrapperDelegateProvider<V, C, G, B>(
      internal val builder: ConfigGroupBuilder, internal val entryBuilder: B,
    ): PropertyDelegateProvider<Any, EntryDelegate<V, C, G, B>>
      where B : Builder<V, C, G, B>, B: AtomicEntryBuilder {
        override fun provideDelegate(thisRef: Any, property: KProperty<*>) =
          EntryDelegate(entryBuilder) { builder.caption(property.name, it) }
    
        fun configure(configure: B.() -> B) =
          CaptionWrapperDelegateProvider(builder, entryBuilder.configure())
        operator fun invoke(configure: B.() -> B) = configure(configure)
        
        infix fun <T> baked(transform: (V) -> T) =
          TransformingCaptionWrapperDelegateProvider(builder, entryBuilder, transform)
    }
    
    class TransformingCaptionWrapperDelegateProvider<T, V, C, G, B>(
      internal val builder: ConfigGroupBuilder, internal val entryBuilder: B,
      internal val transform: (V) -> T
    ): PropertyDelegateProvider<Any, TransformingEntryDelegate<T, V, C, G, B>>
      where B : Builder<V, C, G, B>, B: AtomicEntryBuilder {
        override fun provideDelegate(thisRef: Any, property: KProperty<*>) =
          TransformingEntryDelegate(entryBuilder, transform) { builder.caption(property.name, it) }
    
        fun configure(configure: B.() -> B) = TransformingCaptionWrapperDelegateProvider(
            builder, entryBuilder.configure(), transform)
        operator fun invoke(configure: B.() -> B) = configure(configure)
        
        infix fun writer(writer: (T) -> V) =
          WritableTransformingCaptionWrapperDelegateProvider(builder, entryBuilder, transform, writer)
    }
    
    class WritableTransformingCaptionWrapperDelegateProvider<T, V, C, G, B>(
      internal val builder: ConfigGroupBuilder, internal val entryBuilder: B,
      internal val transform: (V) -> T, internal val inverse: (T) -> V
    ): PropertyDelegateProvider<Any, WritableTransformingEntryDelegate<T, V, C, G, B>>
      where B : Builder<V, C, G, B>, B: AtomicEntryBuilder {
        override fun provideDelegate(thisRef: Any, property: KProperty<*>) =
          WritableTransformingEntryDelegate(entryBuilder, transform, inverse) { builder.caption(property.name, it) }
    
        fun configure(configure: B.() -> B) =
          WritableTransformingCaptionWrapperDelegateProvider(builder, entryBuilder.configure(), transform, inverse)
        operator fun invoke(configure: B.() -> B) = configure(configure)
    }
}