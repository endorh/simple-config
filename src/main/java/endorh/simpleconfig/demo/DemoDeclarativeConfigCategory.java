package endorh.simpleconfig.demo;

import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.api.ConfigCategoryBuilder;
import endorh.simpleconfig.api.ConfigGroupBuilder;
import endorh.simpleconfig.api.SimpleConfigCategory;
import endorh.simpleconfig.api.annotation.*;
import endorh.simpleconfig.api.entry.BooleanEntryBuilder;
import endorh.simpleconfig.api.entry.FloatEntryBuilder;
import endorh.simpleconfig.api.entry.IntegerEntryBuilder;
import endorh.simpleconfig.api.entry.StringEntryBuilder;
import endorh.simpleconfig.api.ui.icon.Icon;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons.Status;
import endorh.simpleconfig.demo.DemoDeclarativeConfigCategory.MyConfigAnnotations.ExtraSlider;
import endorh.simpleconfig.demo.DemoDeclarativeConfigCategory.MyConfigAnnotations.Lowercase;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.awt.Color;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.*;
import java.util.function.Supplier;

import static endorh.simpleconfig.api.ConfigBuilderFactoryProxy.category;
import static endorh.simpleconfig.api.ConfigBuilderFactoryProxy.string;
import static java.util.Arrays.asList;

@Category(background="textures/block/cobblestone.png", color=0x808080E0)
public class DemoDeclarativeConfigCategory {
	// The following utility methods are used in this example to shorten expressions
	private static String prefix(String key) {
		return SimpleConfigMod.MOD_ID + ".config." + key;
	}
	private static MutableComponent stc(String msg, Object... args) {
		return Component.literal(String.format(msg, args));
	}
	private static MutableComponent ttc(String key, Object... args) {
		return Component.translatable(key, args);
	}
	
	@Internal public static ConfigCategoryBuilder build() {
		// In this example, we generate all config entries
		//   directly in the config class through the use of annotations
		// This API has full parity with the builder API, by means of
		//   the @Configure annotation
		// For an example of the builder API instead see DemoConfigCategory
		
		// We use this class as the backing class
		return category("declarative_demo", DemoDeclarativeConfigCategory.class)
		  .withIcon(Status.INFO)
		  .withColor(0x80607080);
	}
	
	// To define an icon for this category, you can define a getter method
	//   `getIcon` that returns a SimpleConfig `Icon`.
	// This method will only be called once. To make the icon dynamic,
	//   use an `AnimatedIcon`, a `LayeredIcon`/`SimpleLayeredIcon`, or
	//   subclass `Icon` directly.
	// You should create the icon entirely within this method, not in
	//   a field, because this will allow your mod to be loaded without
	//   SimpleConfig available at runtime. If a field initializer
	//   tries to access the `Icon` class, that won't be possible.
	@Bind private static Icon getIcon() {
		return SimpleConfigIcons.Status.INFO;
	}
	
	// A bake method is automatically recognized in the backing class
	//   Within categories or groups, the bake method must receive
	//   the proper type, either SimpleConfigCategory or SimpleConfigGroup
	@Bind protected static void bake(SimpleConfigCategory config) {
		// The baker method may be used to translate units
		//   or precompute frequently used values each time
		//   the config changes
		// For simple changes like this, you may want to create your own
		demo_group.speed_m_tick = demo_group.speed_m_s / 20F;
	}
	
	// You may declare categories and groups as static inner classes
	// Classes without annotations are ignored, unless the builder
	//   defines a category or group with the same name
	// Categories and groups are mapped to translations keys automatically,
	//   under the `config.{mod-id}.category` and `config.{mod-id}.category` namespaces
	// For instance, this category's label and tooltip would be mapped to the keys
	//   config.{mod-id}.category.server.demo
	//   config.{mod-id}.category.server.demo.help (Tooltip, if defined)
	// Categories may be generated with @Category, though in this case, the category
	//   is actually generated in the builder, so we use @Bind
	// You may also add text entries using the @Text annotation.
	// The field may be either:
	//    - A String, which value is ignored
	//        It is mapped to a translation according to its name/path
	//        For instance, the following field is mapped to the translations
	//          config.{mod-id}.server.demo.greeting
	//          config.{mod-id}.server.demo.greeting.help (Tooltip, optional)
	//    - An Component, which is served directly
	//        It may be a TranslationTextComponent, or even have click/hover
	//        events through .modifyStyle()
	//    - A Supplier<Component>, which can offer different
	//        contents dynamically (but is only refreshed every time
	//        the GUI is opened)
	// Making their fields not public helps keep a cleaner config API
	//   to use from the project
	@Text private static Void greeting;
	
	// Groups can be created with @Group
	//   We pass the desired order of the group related to its siblings
	//   since Java can't read inner classes in declaration order
	// Also, the default insertion place is 0, so 1 will be after the
	//   greeting above. Negative values would place this group above greeting
	//   Conflicting indexes will resolve entries before groups and then
	//   (hopefully) entries in declaration order and groups randomly
	@Group(expand = true)
	public abstract static class group {
		// Most entry types can be created by simply using @Entry
		// The default value of an entry is the field's initial value
		@Entry @Configure("$configure") public static boolean bool = false;
		@Bind private static BooleanEntryBuilder bool$configure(BooleanEntryBuilder b) {
			return b.text("simpleconfig.format.bool.on_off");
		}
		
		// More exactly, it is the field's actual value when the
		//   config is registered, so the following static
		//   initializer affects the default value for this field
		@Entry public static CompoundTag nbt = new CompoundTag();
		static {
			nbt.put("name", StringTag.valueOf("Steve"));
			nbt.put("health", IntTag.valueOf(20));
		}
		// Enum fields without an initializer default to the first
		//   enum constant in the class, but you should be explicit
		@Entry public static Direction direction;
		
		// It's also possible to create entries by defining a `build` method
		//   that transforms a builder of the correct type, that is:
		//     - SimpleConfigBuilder for config files
		//     - ConfigCategoryBuilder for categories
		//     - ConfigGroupBuilder for groups
		// This can also be used to access the builder API without a hard dependency
		@Bind private static ConfigGroupBuilder build(ConfigGroupBuilder builder) {
			return builder.add("built_with_builder", string(""));
		}
	}
	
	@Group(expand=true)
	public abstract static class demo_group {
		// For the types that support them, you may use the
		//   @Min, @Max, @Slider or @HasAlpha annotations
		// A number without @Min or @Max is unbound for that limit
		@Entry @Min(0) @Max(10) @Slider
		public static long even_score = 10L;
		@Entry @HasAlpha
		public static Color alpha_color = Color.RED;
		
		// Every field can declare an error supplier, appending the suffix '$error' to its name
		//   This can also be done in the builder, by using the '.error(...)' method on the entry
		//   Entries generated in the builder cannot define their error supplier in the backing class
		// Like text fields, these methods should be non-public to avoid cluttering the exposed API
		@Bind private static Optional<Component> even_score$error(long v) {
			// This example entry accepts only even values
			return v % 2 != 0 ? Optional.of(ttc(prefix("error.not_even"), v)) : Optional.empty();
		}
		// A '$tooltip' method can also be added to provide dynamic or complex
		//   tooltips to entries, but using the builder for this is recommended
		// Note that static tooltips can be added directly in the translations JSON
		//   under the same key of the field followed by '.help', and they support
		//   newlines automatically
		private static List<Component> even_score$tooltip() {
			return asList(
			  ttc(prefix("tooltip.score.1")),
			  ttc(prefix("tooltip.score.2")));
		}
		
		// Text fields can be final, since they won't be updated
		@Text private static final Component _1 = ttc(prefix("text.some_other_text"));
		
		// Generated entries/groups/categories may also have the
		//   @RequireRestart annotation to flag them as requiring a
		//   world restart to be effective.
		//   Likewise, this can also be done in the builder
		@Entry @Min(0) @Max(1) @RequireRestart
		public static double ore_gen_chance = 1;
		
		// Fields without annotations that are also not defined in
		//   the builder are ignored
		public static String summon_command = "/summon minecraft:sheep ~ ~ ~ {Color:6}";
		
		// This can be useful to have settings in different units
		//   in the config.
		@Entry @Min(0) public static float speed_m_s = 2F;
		public static float speed_m_tick;
		
		// Marks the position where the group `nested_group` defined
		//   below should be inserted
		// This can be used to place groups in a specific order, since
		//   Java doesn't support comparing the declaration order of
		//   fields and inner classes by reflection
		@Group private static Void nested_group$marker;
		
		@Entry @NonPersistent public static boolean temp_bool = false;
		
		// Text entries may be suppliers of Component instead
		//   Fon example, the following text entry would change
		//   if the mod modified the summon_command field
		@Text private static final Supplier<Component> _2 = () ->
		  ttc(prefix("text.some_complex_text"),
		      stc(summon_command).withStyle(style -> style
			     .withColor(ChatFormatting.LIGHT_PURPLE)
			     // If you're planning to have many clickable links,
		        // you may want to create a wrapper for this
			     .withHoverEvent(new HoverEvent(
				    HoverEvent.Action.SHOW_TEXT, ttc("chat.copy.click")))
			     .withClickEvent(new ClickEvent(
				    ClickEvent.Action.COPY_TO_CLIPBOARD, summon_command))));
		
		// Groups will always be placed after entries with the same order
		//   Since neither the above entries nor this group declare
		//   an order, both use 0, and the group is resolved at the end
		@Group public abstract static class nested_group {
			@Caption public static String caption = "Caption entry";
			@Entry public static Color no_alpha_color = Color.BLUE;
			@Entry @HasAlpha public static Color alpha_color = Color.YELLOW;
			
			// Composite entries, namely List, Set and Map are also supported
			//   Leaving uninitialized is the same as defaulting to an empty collection
			//   The default value can also be set in a static initializer
			// Most annotations can also be applied to the type parameters of
			//   the field, to configure the inner entries
			@Entry public static List<@Min(0) Long> long_list = asList(0L, 2L);
			// It's possible to target inner entries of composite entry types with
			//   built-in decorator methods, such as `error` and `tooltip`, by suffixing
			//   the field name with '$v'
			// In the case of maps, '$k' can be used to target the key entries
			// In the case of pairs, '$l' and '$r' target the left and right entries,
			//   respectively, and '$m' targets the middle entry in triples
			// This can be done to target inner entries of inner entries by chaining suffixes
			@Bind private static Optional<Component> long_list$v$error(long element) {
				// Again here we only accept even values
				// Notice that, separately, in the annotation we've also
				//   restricted the values to be >= 0
				// Setting the ranges in the annotation helps provide a
				//   more precise error message to the user
				return element % 2 == 0? Optional.empty() : Optional.of(
				  ttc(prefix("error.not_even")));
			}
			
			@Entry public static List<@Min(0) @Max(1) Double> double_list = asList(0.1, 0.2, 0.3, 0.4);
			@Bind private static Optional<Component> double_list$v$error(double element) {
				// Here we limit the number of decimals to 1 for no reason
				//   If we *really* needed them to have just one decimal
				//   the correct approach would be using the baker
				//   method to round them, this is just an example
				return Double.compare(element, Math.round(element * 10D) / 10D) != 0
				       ? Optional.of(ttc(prefix("error.too_many_decimals")))
				       : Optional.empty();
			}
			
			// It's possible to set the default value for the inner entries, by using
			//   the @Default annotation, which receives the default value in YAML format
			@Entry public static List<
			  @Default("['a', 'b', 'c', 'd']") List<@Default("'e'") String>
			> list_list = asList(asList("a", "b", "c", "d"), asList("e", "f", "g", "h"));
			// The `error` method may also return a boolean indicating that an error is
			//   present, instead of a detailed error message, although this is
			//   not encouraged, as players will only see a generic error message
			@Bind public static boolean list_list$v$v$error(String elem) {
				return !elem.equals(elem.toLowerCase());
			}
			
			@Entry public static Map<
			  String, @Min(0) @Max(20) @Default("10") Integer
			> int_map = Util.make(new HashMap<>(), m -> {
				m.put("key", 0);
			});
			
			// Lists of Pairs are always translated into pairList entries
			@Entry public static List<Pair<
			  @Default("move") @Suggest({"move", "rotate", "tower", "dig"}) String,
			  @Min(0) @Default("10") Integer
			>> string_int_pair_list = asList(
			  Pair.of("move", 4),
			  Pair.of("rotate", 90),
			  Pair.of("move", 2)
			);
			
			// It's also possible to reuse annotations
			//   If you want to use the same annotations for multiple entries,
			//   you may declare your own, and annotate it with them
			// It's possible to do this in multiple levels, by annotating
			//   your own annotations with more of your own
			// Annotations directly in the field, or found earlier when
			//   traversing them recursively will take priority
			// Make sure to annotate your annotations with
			//   @Target({ElementType.FIELD, ElementType.TYPE_USE}) and
			//   @Retention(RetentionPolicy.RUNTIME) or they won't work
			// If you want to be able to reuse your own annotation, add
			//   ElementType.ANNOTATION_TYPE as a @Target too
			// If you want to have parameters in your custom annotation,
			//   or access parts of the builder API not exposed as
			//   annotations by default, you can do so by using decorator
			//   methods with the @Configure annotation, as shown at the
			//   end of this demo
			@Target({ElementType.FIELD, ElementType.TYPE_USE, ElementType.ANNOTATION_TYPE})
			@Retention(RetentionPolicy.RUNTIME)
			@Min(0) @Max(20) @Slider(max=10) @interface Slider1020 {}
			
			@Entry @Slider1020 public static int int10 = 5;
			
			// The following entries use annotations from the
			@Entry public static @Lowercase String lower_str = "lowercase";
			@Entry @ExtraSlider public static int extra_slider = 50;
			@Entry @ExtraSlider(extraMax=300) public static int extra_slider_300 = 250;
			
			// It's also possible to define bean entries with the declarative API
			//   Only field properties are supported, and they have to be annotated
			//   with @Entry or @Caption in the bean class (see `BeanDemo` example below)
			@Entry public static BeanDemo demo;
			// As long as a @Bean class satisfies these conditions, it can be used
			//   as a field type, even as type parameter for collections
			// The only exception to this rule is that beans cannot have cyclic references
			//   to their own type, as this would require an infinite GUI to be built,
			//   since the config model doesn't support instance reuse
			@Entry public static Set<
			  @Default("{name: name, number: 5, pair: [k, 5], subBean: {name: db, number: 42}}")
			  BeanDemo
			> bean_set;
		}
	}
	
	// Demo bean class, used in a few entries above
	//   Classes annotated with @Bean are allowed as config field types,
	//   and they're scanned for @Entry and @Caption annotated fields,
	//   which will be used to map bean properties to config entries
	@Bean public static class BeanDemo {
		// Only one entry in the bean can be the caption entry,
		//   and it must be an atomic type (not a collection/bean)
		@Caption public String name = "Name";
		// Entries within bean classes can be defined as if it was a
		//   config class, except that the methods are not static
		@Entry @Min(0) @Max(10) public int number = 0;
		@Entry public Pair<String, @Min(0) @Max(10) @Slider Integer> pair = Pair.of("a", 0);
		// It's possible to have sub bean properties, but only if
		//   they do not result in a cyclic reference from any of the
		//   beans involved to itself
		// While bean entries in config classes can be left uninitialized,
		//   sub bean properties in bean classes must always
		@Entry public SubBean subBean = new SubBean();
		@Bean public static class SubBean {
			@Caption public String name = "";
			@Entry public int value = 0;
		}
		
		// While it's recommended you define an `equals` (and `hashCode`) method for
		//   your config beans, specially if you're going to use them for anything
		//   practical, it's not required for the config menu to distinguish when
		//   they're edited as it'll compare the properties that users can edit, if
		//   no `equals` method is defined
	}
	
	// The most powerful way to customize config entries is by using the @Configure
	//   annotation in custom annotations
	// When used in a custom annotation, its method path will be local to the context
	//   where the annotation is defined, which is useful to write many decorator methods
	//   in a single utility class, separate from your config
	public static class MyConfigAnnotations {
		@Target({ElementType.FIELD, ElementType.TYPE_USE, ElementType.ANNOTATION_TYPE})
		@Retention(RetentionPolicy.RUNTIME)
		@Configure("decLowercase")
		public @interface Lowercase {}
		
		// The @Bind annotation is not functional outside a config class, however,
		//   you may use it to let your IDE understand this is an entry point
		//   (if you've set the `unused` inspection to recognize @Bind as such)
		@Bind static StringEntryBuilder decLowercase(StringEntryBuilder value) {
			return value.error(
			  s -> !s.equals(s.toLowerCase())
			       ? Optional.of(ttc(prefix("error.not_lowercase"), s))
			       : Optional.empty());
		}
		
		// It's possible to even declare annotation parameters, which you may receive
		//   in the second parameter for decorator methods
		// The second parameter is always optional, regardless of your annotation class
		//   having parameters or not
		@Target({ElementType.FIELD, ElementType.TYPE_USE, ElementType.ANNOTATION_TYPE})
		@Retention(RetentionPolicy.RUNTIME)
		@Configure("decSlider")
		public @interface ExtraSlider {
			int max() default 100;
			int extraMax() default 200;
		}
		
		// Simple Config will try to find a suitable method for entries annotated with your
		//   annotation with the `decSlider` name.
		// If none can be found, an error is thrown
		@Bind static IntegerEntryBuilder decSlider(IntegerEntryBuilder value, ExtraSlider a) {
			return value.range(0, a.extraMax()).slider().sliderRange(0, a.max());
		}
		
		// We could've also implemented a single decSlider method accepting a
		//   generic RangedEntryBuilder, which would cover all numeric entries
		// For this example, we simply provide two different implementations
		//   of the same annotation
		@Bind static FloatEntryBuilder decSlider(FloatEntryBuilder value, ExtraSlider a) {
			return value.range(0, a.extraMax()).slider().sliderRange(0, a.max());
		}
	}
}
