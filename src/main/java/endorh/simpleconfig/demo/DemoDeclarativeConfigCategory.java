package endorh.simpleconfig.demo;

import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.api.ConfigCategoryBuilder;
import endorh.simpleconfig.api.ConfigGroupBuilder;
import endorh.simpleconfig.api.SimpleConfigCategory;
import endorh.simpleconfig.api.annotation.*;
import endorh.simpleconfig.api.annotation.Bake.Scale;
import endorh.simpleconfig.api.entry.BooleanEntryBuilder;
import endorh.simpleconfig.api.entry.FloatEntryBuilder;
import endorh.simpleconfig.api.entry.IntegerEntryBuilder;
import endorh.simpleconfig.api.entry.StringEntryBuilder;
import endorh.simpleconfig.api.ui.icon.Icon;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons;
import endorh.simpleconfig.demo.DemoDeclarativeConfigCategory.MyConfigAnnotations.ExtraSlider;
import endorh.simpleconfig.demo.DemoDeclarativeConfigCategory.MyConfigAnnotations.Lowercase;
import endorh.simpleconfig.demo.DemoDeclarativeConfigCategory.MyConfigAnnotations.Mapped;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.Util;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
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

@Category(background="textures/block/cobblestone.png", color=0x80607080)
public class DemoDeclarativeConfigCategory {
	// The following utility methods are used in this example to shorten expressions
	private static String prefix(String key) {
		return SimpleConfigMod.MOD_ID + ".config." + key;
	}
	private static IFormattableTextComponent stc(String msg, Object... args) {
		return new StringTextComponent(String.format(msg, args));
	}
	private static IFormattableTextComponent ttc(String key, Object... args) {
		return new TranslationTextComponent(key, args);
	}
	
	@Internal public static ConfigCategoryBuilder build() {
		// In this example, we generate all config entries
		//   directly in the config class through the use of annotations
		// This API has full parity with the builder API, by means of
		//   the @Configure annotation, which is showcased at the end
		//   of this file
		// For an example of the builder API instead see DemoConfigCategory
		
		// We use this class as the backing class
		return category("declarative_demo", DemoDeclarativeConfigCategory.class);
	}
	
	// To define an icon for a category, you can define a getter method
	//   `getIcon` that returns a SimpleConfig `Icon`.
	// This method will only be called once. To make the icon dynamic,
	//   use an `AnimatedIcon`, a `LayeredIcon`/`SimpleLayeredIcon`, or
	//   subclass `Icon` directly, if you really need to.
	// You should create the icon entirely within this method, not in
	//   a field, because this will allow your config class to be
	//   loadable without SimpleConfig available at runtime. If a field
	//   initializer tries to access the `Icon` class, that won't be possible.
	@Bind private static Icon getIcon() {
		return SimpleConfigIcons.Status.INFO;
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
	//    - An ITextComponent, which is served directly
	//        It may be a TranslationTextComponent, or even have click/hover
	//        events through .modifyStyle()
	//    - A Supplier<ITextComponent>, which can offer different
	//        contents dynamically (but is only refreshed every time
	//        the GUI is opened)
	// Making their fields not public helps keep a cleaner config API
	//   to use from the project
	@Text private static final ITextComponent _greeting = ttc(
	  prefix("text.greeting_declarative"),
	  ttc(prefix("text.github")).modifyStyle(s -> s
		 .applyFormatting(TextFormatting.AQUA)
		 .setClickEvent(new ClickEvent(
			ClickEvent.Action.OPEN_URL,
			"https://github.com/endorh/simple-config/blob/1.16/src/main/java/endorh/simpleconfig/demo/DemoDeclarativeConfigCategory.java"))));
	
	// Entries are created by annotating a static field with a supported type
	//   with the @Entry annotation.
	// While the @Entry annotation takes a parameter, which can be used
	//   to manually control the order of the entries declared in the
	//   class, it's encouraged to rely on the declaration order of
	//   the fields as the order of the entries, and use @Entry
	//   without arguments
	// The default value of the entry will be taken from the field initializer,
	//   or the default for the type if the field is not initialized
	@Entry public static String entry = "value";
	
	// A bake method is automatically recognized in the backing class
	//   Within categories or groups, the bake method must receive
	//   the proper type, either SimpleConfigCategory or SimpleConfigGroup
	// The @Bind annotation is optional, but is useful as it'll throw an error
	//   if it fails to bind
	@Bind protected static void bake(SimpleConfigCategory config) {
		// The baker method may be used to preprocess entries everytime
		//   they're modified
		// There are a few other techniques to achieve this, such as
		//   `$bake` methods, the @Bake/@Bake.Scale annotation or the
		//   @Configure annotation, which will be introduced later
		entry = entry.toLowerCase();
	}
	
	// Groups can be created with @Group
	// While you can specify an order as the argument of @Group, the recommended
	//   way to sort groups is to use `$marker` fields, as shown below in the
	//   `nested_group$marker`, which can be used to intuitively sort groups
	//   relative to their sibling entries.
	@Group(expand = false)
	public abstract static class group {
		// The @Configure annotation can be used to access the full extent
		//   of the builder API to modify a generated entry to your liking
		// However, the recommended use of this annotation is to use it
		//   to annotate your own custom annotation type, which you can
		//   then reuse for several entries, as it's showcased at the
		//   end of this file.
		@Entry @Configure("$configure") public static boolean bool = false;
		// All method references in annotations, such as @Configure can be
		//   relative to their target entry name if they start with `$`,
		//   or absolute if they include a class name followed by `#`
		// If not absolute, they're searched in the enclosing classes, from
		//   the innermost to the outermost
		// If annotating a custom annotation, the method is first searched
		//   in the enclosing class of the annotation, if any, which
		//   can be used to define decorator methods besides their custom
		//   annotations, as showcased in the `MyConfigAnnotations` class
		//   declared later below
		// The method of @Configure must accept and return a builder type
		//   compatible with the entry generated by its target entry
		// In the case where @Configure annotates a custom annotation,
		//   multiple overloads can be declared to make the custom annotation
		//   support multiple entry types
		@Bind private static BooleanEntryBuilder bool$configure(BooleanEntryBuilder b) {
			return b.text("simpleconfig.format.bool.on_off");
		}
		
		// The default value for generated entries isn't exactly their
		//   field initializer, but rather the value they have when
		//   scanned by Simple Config.
		// This means that static initializers can be used to initialize
		//   complex default values for entries, as it's done for this one
		// You may also want to use the `Util.make` method, if that's
		//   more akin to your code style
		@Entry public static CompoundNBT nbt = new CompoundNBT();
		static {
			nbt.put("name", StringNBT.valueOf("Steve"));
			nbt.put("health", IntNBT.valueOf(20));
		}
		
		// Enum fields without an initializer default to the first
		//   enum constant in the class, but it's recommended to
		//   specify it explicitly
		@Entry public static Direction direction;
		
		// It's also possible to create entries by defining a `build` method
		//   that transforms a config/category/group builder of the correct type, that is:
		//     - SimpleConfigBuilder for config files
		//     - ConfigCategoryBuilder for categories
		//     - ConfigGroupBuilder for groups
		// This can also be used to access the builder API without a hard dependency
		@Bind private static ConfigGroupBuilder build(ConfigGroupBuilder builder) {
			return builder.add("built_with_builder", string(""));
		}
	}
	
	@Group(expand=false)
	public abstract static class demo_group {
		// Some entry types support special annotations to configure them common aspects
		// For example, numeric entries support the @Min, @Max and @Slider annotations
		@Entry @Min(0) @Max(10) @Slider public static long even_score = 10L;
		// Color entries can use @HasAlpha to allow alpha the entry to make the alpha channel editable
		@Entry @HasAlpha public static Color alpha_color = Color.GRAY;
		// In addition, float and double entries support @Bake.Scale
		// This entry is editable in m/s, but the value stored is in m/tick,
		//   as it's divided by 20 before being stored
		@Entry @Scale(1./20) public static float speed;
		
		// Every field can declare an error supplier, appending the suffix `$error` to its name
		//   This can also be done in the builder, by using the '.error(...)' method on the entry
		//   Entries generated in the builder cannot define their error supplier in the backing class
		// Like text fields, these methods should be non-public to avoid cluttering the exposed API
		@Bind private static Optional<ITextComponent> even_score$error(long v) {
			// This example entry accepts only even values
			return v % 2 != 0 ? Optional.of(ttc(prefix("error.not_even"), v)) : Optional.empty();
		}
		// A `$tooltip` method can also be added to provide dynamic or complex
		//   tooltips to entries, but using the builder for this is recommended
		// Note that static tooltips can be added directly in the translations JSON
		//   under the same key of the field followed by '.help', and they support
		//   newlines automatically
		@Bind private static List<ITextComponent> even_score$tooltip() {
			return asList(
			  ttc(prefix("tooltip.score.1")),
			  ttc(prefix("tooltip.score.2")));
		}
		
		// A `$bake` method can be added to transform the value before being
		//   saved in the field. It must have the same return type, and a
		//   single argument of the same type.
		// Alternatively, you can use the @Bake or @Configure annotations,
		//   showcased later in this demo, which are more flexible and reusable
		@Bind private static long even_score$bake(long v) {
			return v * 2;
		}
		
		// Static text entries can be declared Void, taking their content
		//   from their automatically mapped translation key
		@Text private static Void text;
		
		// Generated entries/groups/categories may also have the
		//   @RequireRestart annotation to flag them as requiring a
		//   world restart to be effective.
		// Some other marker annotations can be used, such as
		//   @Advanced, @Experimental, @Operator
		@Entry @Min(0) @Max(1) @RequireRestart public static double ore_gen_chance = 1;
		
		// Fields without annotations that are also not defined in
		//   the builder are ignored
		public static String summon_command = "/summon minecraft:sheep ~ ~ ~ {Color:6}";
		
		// This can be useful to have settings in different units
		//   in the config.
		@Entry @Min(0) public static float speed_m_s = 2F;
		public static float speed_m_tick;
		
		// Here we insert the group `nested_group` in between entries,
		//   using the recommended strategy to order groups, which is
		//   declaring a @Group annotated Void field with the same
		//   name as the group, followed by `$marker`
		// Categories can also be sorted like this, by using the
		//   @Category annotation, although this only affects their
		//   relative order, as categories cannot be ordered
		//   relative to entries
		@Group private static Void nested_group$marker;
		
		// The @NonPersistent annotation can be used to create entries
		//   not stored in the config file, which can only be changed for
		//   a single play session
		// This can be useful to enable profiling/debug features you wouldn't
		//   want to leave enabled accidentally
		@Entry @NonPersistent public static boolean temp_bool = false;
		
		// Text entries may be suppliers of ITextComponent instead
		//   Fon example, the following text entry would change
		//   if the mod modified the summon_command field at some point
		// If your text is not dynamic it's recommended you use either
		//   a `Component` field or a Void field mapped to a
		//   translation key
		@Text private static final Supplier<ITextComponent> _2 = () ->
		  ttc(prefix("text.some_complex_text"),
		      stc(summon_command).modifyStyle(s -> s
		        // If you're planning to have many clickable links,
		        //   you may want to create a wrapper method for this
		        .applyFormatting(TextFormatting.LIGHT_PURPLE)
			     .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, ttc("chat.copy.click")))
			     .setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, summon_command))));
		
		// This group will appear in the menu just before the `temp_bool` entry
		//   declared above, in the position marked by the `nested_group$marker` field
		@Group public abstract static class nested_group {
			// Within groups, one (and only one) entry can be annotated with
			//   @Group.Caption instead of @Entry to make it the caption of the group
			// This is only recommended if the value of the caption is
			//   clear and easily understood. For example, a master volume slider
			//   as the caption of a group of volume sliders, or a boolean
			//   toggle to enable/disable a feature as the caption of a group
			//   of settings for said feature.
			// Otherwise, please refrain from using caption entries where they'd
			//   just confuse the player
			// Caption entries must be atomic types (not collections/beans)
			@Group.Caption public static String caption = "Caption entry";
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
			//   the field name with `$v`
			// In the case of maps, `$k` can be used to target the key entries
			// In the case of pairs, `$l` and `$r` target the left and right entries,
			//   respectively, and `$m` targets the middle entry in triples
			// In the case of captioned collections, `$caption` targets the caption,
			//   and `$list`/`$set`/`$map` targets the collection entry
			// This can be done to target inner entries of inner entries by chaining
			//   suffixes (e.g. $v$v to access the inner inner entry of a list of lists)
			@Bind private static Optional<ITextComponent> long_list$v$error(long element) {
				// Here we only accept even values as an example
				// Notice that, separately, in the annotation we've also
				//   restricted the values to be >= 0
				// Setting the ranges in the annotation helps provide a
				//   more precise error message to the player
				return element % 2 == 0? Optional.empty() : Optional.of(
				  ttc(prefix("error.not_even")));
			}
			
			@Entry public static List<@Min(0) @Max(1) Double> double_list = asList(0.1, 0.2, 0.3, 0.4);
			@Bind private static Optional<ITextComponent> double_list$v$error(double element) {
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
			
			// Map entries are also supported
			// The key type must be atomic (not a collection/bean)
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
			
			// Pairs with a collection as the right entry are translated into
			//   captioned collections
			@Entry public static Pair<
			  @Default("discarded") @Length(max=10) String,
			  @Size(max=4) List<@Min(0) @Max(10) Integer>
			> captioned_list = Pair.of("caption", asList(4, 2));
			
			// Alternatively, you can declare two adjacent fields, so that the
			//   first one, annotated with @Entry.Caption is used as the caption
			//   of the second
			// This is similar to using the `captionField` and `collectionField` methods,
			//   and it's useful to access the caption and collection separately
			// The @Entry.Caption entry must always be the field declared immediately
			//   above the collection field, and must be an atomic type (not a collection/bean)
			@Entry.Caption public static String set_caption = "caption";
			@Entry public static Set<@Min(0) @Max(10) @Slider Integer> set_with_caption;
			
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
			
			// This entry will have all the annotations @Slider1020 has. It's equivalent to
			//   `@Entry @Min(0) @Max(20) @Slider(max=10) public static int int10 = 5;`
			@Entry @Slider1020 public static int int10 = 5;
			
			// It's recommended you write your custom annotations in a separate
			//   class, such as the `MyConfigAnnotations` class defined
			//   below in this file
			// The following entries use annotations from this class
			@Entry public static @Lowercase String lower_str = "lowercase";
			@Entry @ExtraSlider public static int extra_slider = 50;
			@Entry @ExtraSlider(extraMax=300) public static int extra_slider_300 = 250;
			@Entry @Min(0) @Max(1) @Mapped public static float mapped_float;
			@Entry @Min(0) @Max(1) @Mapped(outputMin=10, outputMax=5) public static double mapped_double;
			
			// It's also possible to define bean entries with the declarative API
			//   Only field properties are supported, and they have to be annotated
			//   with @Entry or @Caption in the bean class (see the `DemoBean` definition below)
			@Entry public static DemoBean demo_bean;
			
			// As long as a @Bean class satisfies these conditions, it can be used
			//   as an entry type, even as type parameter for other entry types
			// The only exception to this rule is that beans cannot have cyclic references
			//   to their own type, as this would require an infinite GUI to be built,
			//   since the config model doesn't support instance reusing
			@Entry public static Set<
			  // Here we set the default value for new set elements using YAML
			  //   with the @Default annotation
			  @Default("{name: name, number: 5, pair: [k, 5], subBean: {name: db, number: 42}}")
			    DemoBean
			> bean_set = Util.make(new HashSet<>(), s -> s.add(new DemoBean()));
		}
	}
	
	// Group markers for the groups above
	@Group private static Void group$marker;
	@Group private static Void demo_group$marker;
	
	// Demo bean class, used in a few entries above
	//   Classes annotated with @Bean are allowed as config field types,
	//   and they're scanned for @Entry and @Caption annotated fields,
	//   which will be used to map bean properties to config entries
	@Bean public static class DemoBean {
		// Only one entry in the bean can be the caption entry,
		//   and it must be an atomic type (not a collection/bean)
		@Group.Caption public String name = "Name";
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
		// Bean classes can be defined anywhere
		//   This class is only defined here as an inner class for convenience
		@Bean public static class SubBean {
			@Group.Caption public String name = "";
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
		// Declaring the second parameter is always optional, regardless of your
		//   annotation class having parameters or not
		@Target({ElementType.FIELD, ElementType.TYPE_USE, ElementType.ANNOTATION_TYPE})
		@Retention(RetentionPolicy.RUNTIME)
		@Configure("decSlider")
		public @interface ExtraSlider {
			int max() default 100;
			int extraMax() default 200;
		}
		
		// Simple Config will try to find a suitable method for entries
		//   annotated with your annotation with the `decSlider` name.
		// If many are applicable, it'll choose the more specific one,
		//   and prefer one that accepts the annotation as a second parameter
		//   if two are equally specific
		// If none can be found, an error is thrown
		@Bind static IntegerEntryBuilder decSlider(IntegerEntryBuilder value, ExtraSlider a) {
			return value.range(0, a.extraMax()).slider().sliderRange(0, a.max());
		}
		
		// We could've also implemented a single decSlider method accepting a
		//   generic RangedEntryBuilder, which would cover all numeric entries
		// For this example, we simply provide two different implementations
		//   of the same annotation
		// Note that defining a method for a generic builder such as
		//   `RangedEntryBuilder` can actually be harder than defining
		//   different methods for each of its subclasses, as generic
		//   code may be harder to write in some cases, or you'll need
		//   to write adapter methods
		@Bind static FloatEntryBuilder decSlider(FloatEntryBuilder value, ExtraSlider a) {
			return value.range(0, a.extraMax()).slider().sliderRange(0, a.max());
		}
		
		// It's also possible to define baking methods for entries with
		//   the @Bake annotation, although this is equivalent to using
		//   @Configure and decorating the entry builder with `baked`,
		//   which may be more flexible, as it can also apply other
		//   transformations
		// For example, for the following example, if we'd used @Configure,
		//   we'd be able to set the entry's min and max to the
		//   `inputMin` and `inputMax` arguments in addition to calling
		//   `baked`, all in one go
		@Target({ElementType.FIELD, ElementType.TYPE_USE, ElementType.ANNOTATION_TYPE})
		@Retention(RetentionPolicy.RUNTIME)
		@Bake(method="map")
		public @interface Mapped {
			float inputMin() default 0;
			float inputMax() default 1;
			float outputMin() default 0;
			float outputMax() default 10;
		}
		
		// In this case we only support float and double entries
		@Bind static float map(float value, Mapped m) {
			return m.outputMin()
			       + (value - m.inputMin())
			         * (m.outputMax() - m.outputMin())
			         / (m.inputMax() - m.inputMin());
		}
		
		@Bind static double map(double value, Mapped m) {
			return m.outputMin()
			       + (value - m.inputMin())
			         * (m.outputMax() - m.outputMin())
			         / (m.inputMax() - m.inputMin());
		}
	}
}
