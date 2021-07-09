package endorh.simple_config.demo;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import endorh.simple_config.SimpleConfigMod;
import endorh.simple_config.core.SimpleConfigBuilder.CategoryBuilder;
import endorh.simple_config.core.SimpleConfigCategory;
import endorh.simple_config.core.SimpleConfigGroup;
import endorh.simple_config.core.annotation.Bind;
import endorh.simple_config.core.entry.EnumEntry.ITranslatedEnum;
import endorh.simple_config.core.entry.IConfigEntrySerializer;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import java.awt.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static endorh.simple_config.SimpleConfigMod.CLIENT_CONFIG;
import static endorh.simple_config.core.SimpleConfig.category;
import static endorh.simple_config.core.SimpleConfig.group;
import static endorh.simple_config.core.entry.Builders.*;
import static java.util.Arrays.asList;

/**
 * Demo builder API usage
 */
public class DemoConfigCategory {
	// The following utility methods are used in this example to shorten expressions
	private static String prefix(String key) {
		return SimpleConfigMod.MOD_ID + ".config." + key;
	}
	private static StringTextComponent stc(String msg, Object... args) {
		return new StringTextComponent(String.format(msg, args));
	}
	private static TranslationTextComponent ttc(String key, Object... args) {
		return new TranslationTextComponent(key, args);
	}
	
	// This category builder is added to the config builder
	//   in SimpleConfigMod. We could as well have put all
	//   the entries there. Using categories is optional.
	// The entries without category are grouped in an implicit
	//   default category for its config (Client / Server)
	// Only server operators can access categories registered
	//   in the server config
	public static CategoryBuilder getDemoCategory() {
		// This value will be used below
		final CompoundNBT nbt = new CompoundNBT();
		nbt.putString("name", "Steve");
		nbt.putInt("health", 20);
		
		// We pass this (DemoConfigCategory) class as the backing class
		//   Note that any other class could be passed instead,
		//   for instance, an inner static class, or none at all
		//noinspection ArraysAsListWithZeroOrOneArgument
		return category("demo", DemoConfigCategory.class)
		  // You may add text to the config GUI in using .text()
		  .text(ttc(prefix("text.greeting_text")))
		  // Adding entries is done with .add() and the various entry
		  //   builders available
		  // Entry names must not contain spaces, and they should
		  //   be valid Java identifiers in order to have backing fields
		  // All entries must have a default value, which is passed
		  //   in their builder
		  // Entries get automatically mapped translation keys for both their
		  //   label and their tooltip based on their paths within the config
		  // The tooltip translation key is the label key followed by '.help'
		  //   and its optional, that is, if it's not defined, the entry
		  //   won't display a tooltip
		  // Tooltip translation keys can contain newline characters to
		  //   produce multiline tooltips
		  // For example, the following entry is mapped under
		  //   config.{mod-id}.demo.some_bool
		  // and its tooltip is mapped to (if the translation exists)
		  //   config.{mod-id}.demo.some_bool.help
		  .add("bool", bool(true))
		  // Creating config subgroups is done with .n(), which is
		  //   short for 'nest', and the group() builder
		  // Groups can be automatically expanded in the GUI, by
		  //   passing true in their builder
		  // Groups can contain entries and other groups
		  .n(group("entries", true)
		       // Text entries may also be defined by name, receiving
		       //   automatically mapped translation keys as other entries
		       .text("intro")
		       // Groups can be nested
		       .n(group("basic")
		            // Many different types of entries exist
		            // All primitive number types can be used in entries, but
		            //   using byte or short is discouraged
		            // Numeric entries can specify their range in their builders
		            //   These bounds are inclusive
		            .add("int_value", number(0, 0, 10))
		            // Bounds can also be specified with builder-like methods
		            //   If a bound is not specified, it will be unbound
		            .add("long_value", number(0L).min(0))
		            // A shortcut exists for float or double values between 0 and 1
		            //   fractional(v) is equivalent to number(v, 0, 1);
		            .add("float_value", fractional(0.8F))
		            .add("double_value", number(0.0, -10, 10))
		            // All numeric entry types can be displayed as sliders, including
		            //   floats and doubles
		            .add("long_slider", number(5L, 0, 10).slider())
		            // For obvious reasons, entries displayed as sliders must
		            //   have finite defined bounds
		            .add("float_slider", number(80F, 0, 100).slider())
		            // String values are also common
		            .add("string_value", string("Hello World!"))
		            // You may also use regex entries, which automatically
		            //   validate the inputted expressions and compile them
		            //   with the flags you choose
		            // Remember that users may freely change most flags using (?_)
		            //   notation, so don't struggle too much about flexibility
		            //   and set as flags what you think should be the default
		            .add("regex_value", pattern("nice (?<regex>.*+)").flags(Pattern.CASE_INSENSITIVE))
		            // Enums are supported too
		            // Enums entries get automatically mapped translation keys
		            //   for their values, however, this mappings do not
		            //   depend on their path, so enums with the same name
		            //   may clash
		            // For instance, the enum RockPaperScissors would be
		            //   mapped to the translation keys
		            //     config.{mod-id}.enum.rock_paper_scissors.rock
		            //     config.{mod-id}.enum.rock_paper_scissors.paper
		            //     config.{mod-id}.enum.rock_paper_scissors.scissors
		            .add("enum_value", enum_(RockPaperScissors.SCISSORS))
		            // Enums may define their own translations implementing
		            //   ITranslatedEnum (see the Placement enum)
		            .add("enum_value_2", enum_(Placement.UPSIDE_DOWN))
		            // By intentional design, there's no built-in support for
		            //   keybinding entries, please register your keybindings
		            //   through the ClientRegistry.registerKeyBinding to
		            //   benefit from Forge's key conflict resolution and an
		            //   entry in the Controls GUI
		       ).n(group("colors")
		            // Color entries use java.awt.Color as their type
		            .add("no_alpha_color", color(Color.BLUE))
		            // Colors may optionally allow alpha values
		            .add("alpha_color", color(Color.YELLOW).alpha()))
		       .n(group("lists")
		            // Lists of basic types are supported
		            .add("string_list", stringList(asList("Lorem ipsum", "dolor sit amet")))
		            // For numeric lists, null bounds mean unbound ends
		            .add("int_list", intList(asList(1, 2, 4, 8)).min(0))
		            // Lists can also contain other entry types
		            .add("color_list", list(
		              color(Color.BLACK), asList(Color.RED, Color.BLUE, Color.GREEN)))
		            // Of course this includes other lists, but try not to
		            //   make your config too complex to use
		            // Each list builder can specify the default for its level
		            // If you find yourself needing multiple nested lists in
		            //   your config, you probably instead want a custom
		            //   JsonReloadListener to extend your mod through datapacks
		            .add("color_list_list", list(
		              list(color(Color.BLACK), asList(Color.GRAY)),
		              asList(asList(Color.RED, Color.GREEN), asList(Color.BLUE, Color.YELLOW)))))
		       // A few string serializable types come built-in as well
		       .n(group("serializable")
		            // Resource entries contain ResourceLocation s
		            .add("resource", resource("minecraft:elytra"))
		            // Item entries provide auto-completion in the GUI
		            //   Item entries can be restricted to certain items
		            //   Using tags as filter is only possible in server configs
		            .add("item", item(Items.APPLE)
		              .from(Items.APPLE, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE))
		            // CompoundNBT entries contain CompoundNBT s, and automatically
		            //   check the NBT syntax in the GUI
		            .add("nbt_compound", nbtTag(nbt))
		            // If you pass a INBT default value instead of a compound,
		            //   the entry will also accept literal NBT values
		            .add("nbt_value", nbtValue(StringNBT.valueOf("NBT")))
		            // It is also possible to use custom String serializable entries
		            //   You may either pass an IConfigEntrySerializer,
		            //   make the type implement ISerializableConfigEntry, or
		            //   pass directly two de/serializing lambdas
		            // In this case we opt for the first choice, because we
		            //   can't modify the Pair class, and using lambdas directly
		            //   isn't reusable
		            .add("pair", entry(
		              Pair.of("string", 2), new StringIntPairSerializer())))
		     .n(group("maps")
		          // Map entries are also supported
		          // Map values may be any other entry that serializes
		          //   in the config as any type serializable to NBT
		          //   Currently, this includes every built-in entry type
		          //   (except GUI only entries)
		          // By default, map keys are strings, but all types that
		          //   can be serialized as strings, including numbers,
		          //   can be used as keys.
		          // Currently, maps are serialized as NBT compounds in the
		          //   config file.
		          .add("map", map(
			         string("<placeholder>"),
			         ImmutableMap.of(
				        "first", "some_value",
				        "second", "other_value")))
		          // Of course, maps of lists, maps of maps, and lists of maps
		          //   are valid entry types, without nesting limit
		          // However, try to keep your config simple to use
		          .add("list_map", map(
		            resource(""),
			         list(string("<name>"), asList("<name>")),
			         ImmutableMap.of(
				        new ResourceLocation("overworld"), asList("Dev", "Dev2"),
				        new ResourceLocation("the_nether"), asList("Dev", "Dev3"))))
		          // The entries within a map can be decorated as usual
		          .add("even_int_map_map", map(
		            map(number(0)
		                  .error(i -> i % 2 != 0 ? Optional.of(ttc(prefix("error.not_even"), i)) : Optional.empty()),
		                ImmutableMap.of("", 0)
		            ), ImmutableMap.of(
		              "plains", ImmutableMap.of("Cornflower", 2, "Dandelion", 4),
		              "birch forest", ImmutableMap.of("Poppy", 8, "Lily of the Valley", 4))))
		          // You may pass an entry builder for the key type as well,
		          //   as long as its serializable as a string.
		          // The key entry may have error suppliers, but its tooltip
		          //   is currently ignored
		          // In this example, we use int for both the keys and the values
		          .add("int_to_int_map", map(
		            number(0).min(0).max(1024),
		            number(0).min(0).max(2048),
		            ImmutableMap.of(0, 1, 1, 2, 2, 4, 3, 8))))
		     .n(group("special")
		          // Text entries can also receive format arguments if they
		          //   are defined by name
		          .text("non_persistent_desc", ttc(prefix("text.non_persistent")).mergeStyle(TextFormatting.GRAY))
		          // A special type of entry is non persistent boolean flags
		          //   This entries do not have an entry in the config file,
		          //   and revert to their default whenever restarting the game
		          // They are rarely useful for enabling special profiling
		          //   or debugging features
		          .add("non_persistent_bool", nonPersistentBool(false))))
		  .n(group("errors_tooltips_n_links")
		       // You may add links or contextual tooltips in text entries
		       //   by calling .modifyStyle() on text components
		       // Format arguments may be suppliers, which will be evaluated before
		       //   being filled in.
		       .text("open_file", (Supplier<ITextComponent>) () ->
			      ttc(prefix("text.mod_config_file")).modifyStyle(style -> style
				     .setFormatting(TextFormatting.DARK_AQUA)
				     .setClickEvent(new ClickEvent(
				       ClickEvent.Action.OPEN_FILE,
				       CLIENT_CONFIG.getFilePath().map(Path::toString).orElse("")))
				     .setHoverEvent(new HoverEvent(
				       // "chat.file.open" does not actually exist in minecraft (yet),
				       // I add it in my translations because I feel it's a fair common
				       // translation key to have. Feel free to use it or translate it.
				       HoverEvent.Action.SHOW_TEXT, ttc("chat.file.open")))))
		       // All entries may additionally have value restrictions,
		       //   which produce an error message in the GUI
		       // This is done adding error suppliers to the entries,
		       //   which may return an optional text component with
		       //   the error reason
		       // Error suppliers are also evaluated against values
		       //   changed in the config file, even if their
		       //   error message cannot be displayed
		       // Erroneous values in the config file are reset to their
		       //   default value. The GUI does not allow invalid values
		       //   to be saved
		       .add("lowercase_string", string("lowercase text").error(
			      s -> !s.equals(s.toLowerCase())
			           ? Optional.of(ttc(prefix("error.not_lowercase"), s))
			           : Optional.empty()))
		       // To check other values in the error supplier, you may use
		       //   the 'getGUI' method (and its primitive variants), which
		       //   retrieves the current candidate value for an entry, or
		       //   the current value if no GUI is active
		       .add("min", number(0).error(
			      n -> CLIENT_CONFIG.getGUIInt("demo.errors_tooltips_n_links.max") < n
			           ? Optional.of(ttc(prefix("error.min_greater_than_max")))
			           : Optional.empty()))
		       // In cases like this, it's better to have the check on both
		       //   keys so that when the error occurs in the config file,
		       //   both keys get reset to a valid default state
		       // However, if you need this kind of range often,
		       //   you could just create a serializable range class
		       .add("max", number(10).error(
			      n -> CLIENT_CONFIG.getGUIInt("demo.errors_tooltips_n_links.min") > n
			           ? Optional.of(ttc(prefix("error.min_greater_than_max")))
			           : Optional.empty()))
		       // For lists, you may also set an element validator, which
		       //   takes single elements, instead of the whole entry
		       //   to validate
		       // It is also possible to call .setValidator, which receives
		       //   a simple Predicate, instead of an error supplier,
		       //   and produces a generic error message, but this is
		       //   discouraged
		       .add("even_int_list", intList(asList(2, 4)).range(-20, 20)
			      .elemError(i -> i % 2 != 0 ? Optional.of(ttc(prefix("error.not_even"), i)) : Optional.empty()))
		       // It is also possible to define a dynamic tooltip for an entry,
		       //   which will depend on the current value of the entry
		       // However, you'll need to manually separate your tooltip
		       //   into separate lines, so think if it's really necessary
		       .add("dynamic_tooltip", string("Steve")
			      .tooltip(s -> Optional.of(new ITextComponent[] {
				     ttc(prefix("text.hello"), stc(s).mergeStyle(TextFormatting.DARK_AQUA))})))
		       // However, most of the time it's enough to just set the
		       //   format arguments that will be passed to the tooltip,
		       //   which can also be functions receiving the value of
		       //   the entry
		       .add("tooltip_args", string("Alex")
			      .tooltipArgs(s -> stc(s).mergeStyle(TextFormatting.DARK_AQUA)))
		       // Any value can be marked as requiring a restart to be effective
		       //   Entire groups and categories can be marked as well
		       .add("restart_bool", bool(false).restart()))
		  .text("end")
		  // Set a specific background for this category
		  //   The whole config may also define a default background
		  //   for all categories
		  // Note that the background won't display ingame, unless
		  //   you call .solidIngameBackground() in the config builder
		  //   because configs are transparent by default ingame
		  .setBackground("textures/block/warped_planks.png")
		  // Finally, we may manually set a baker method
		  //   for this config/category/group
		  // The baker method will receive the built config/category/group
		  //   when called, after all backing fields have been baked
		  // If a method named 'bake' with the correct signature is
		  //   found in the backing class, it will be set as baker
		  //   automatically
		  .setBaker(DemoConfigCategory::bakeDemoCategory);
		// Since here we're only building a category, we don't need
		//   to finish the call by calling .buildAndRegister()
	}
	
	// Sometimes baking isn't as simple as setting a few fields
	public static void bakeDemoCategory(SimpleConfigCategory demo) {
		// The baker always runs after all backing fields have been baked
		// So we may safely use them to create composite values, or transform them
		
		// Sometimes it's useful to convert the config values to more
		//   suitable units for computation, and possibly a different type
		// Users will understand better a speed in m/s than in m/tick,
		//   but you may need to apply it per tick
		// You could define a backing field for speed_meters_per_second,
		//   but it isn't necessary, since it's possible to access
		//   values by name directly
		// numbers.speed_meters_per_tick =
		//   demo.getFloat("numbers.speed_meters_per_second") / 20F;
	}
	
	// A common pattern is creating enums for certain config settings
	// This enum's entries are automatically mapped to translation
	//    keys under the key '{mod-id}.config.enum.rock_paper_scissors.*'
	public enum RockPaperScissors {
		ROCK, PAPER, SCISSORS
	}
	
	
	// Alternatively, enums may implement ITranslatedEnum instead,
	//   providing their own translations
	public enum Placement implements ITranslatedEnum {
		UPRIGHT, UPSIDE_DOWN;
		
		// Although in this case it doesn't make much difference
		@Override public ITextComponent getDisplayName() {
			return ttc(prefix("enum.placement." + name().toLowerCase()));
		}
	}
	
	public static class StringIntPairSerializer implements IConfigEntrySerializer<Pair<String, Integer>> {
		@Override public String serializeConfigEntry(Pair<String, Integer> value) {
			return value.getFirst() + ", " + value.getSecond();
		}
		@Override public Optional<Pair<String, Integer>> deserializeConfigEntry(String value) {
			if (value == null || value.isEmpty() || value.matches("(?s).*?,.*?,.*+"))
				return Optional.empty();
			final String[] split = value.split(",");
			if (split.length != 2)
				return Optional.empty();
			try {
				return Optional.of(Pair.of(split[0].trim(), Integer.parseInt(split[1].trim())));
			} catch (NumberFormatException ignored) {
				return Optional.empty();
			}
		}
	}
	
	// Since we passed 'this' as the config class for the category,
	//   we may directly define here backing fields for all entries
	// Backing fields must be static, and shouldn't be final
	// If a backing field does not match its expected type,
	//   the game will crash early
	//   There's no guaranty however that this will happen with
	//   mismatched generic types, such as List<String>. Be careful.
	// The @Bind annotation is purely optional.
	//   If present, it will throw an exception at load time if a
	//   field that was supposed to match an entry didn't match any
	// This is because fields that don't match any entry are allowed
	//   in the config class. If you explicitly want to ensure the
	//   field is bound to an entry use @Bind, otherwise just be careful
	// If you want to have a field with the same name as an entry,
	//   yet unbound to that entry, or of a different type, you may
	//   annotate it as @NotEntry. This can be useful to translate
	//   the config units to more useful units in the code within the
	//   baker method, such as from m/s to m/tick.
	@Bind public static boolean bool;
	// Groups are mapped into static inner classes
	@Bind public static class entries {
		
		@Bind public static class basic {
			@Bind public static int int_value;
			@Bind public static long long_value;
			@Bind public static float float_value;
			@Bind public static double double_value;
			
			// Fields may be non-public, if they're used by the
			//   config class itself and there's no use in exposing them
			// However, fields should not be final, since they are to
			//   be modified arbitrarily, and the JVM may perform
			//   unpredicted optimizations for their usages.
			@Bind private static long long_slider;
			@Bind public static float float_slider;
			
			// The @Bind annotation is optional, these fields will still
			//   be bound to their corresponding entries
			public static String string_value;
			public static RockPaperScissors enum_value;
			
			// Fields unrelated to any entry are allowed
			//   These fields may be assigned by a baker method
			public static int not_an_entry = 3;
			
			// A bake method is recognized automatically within all
			//   classes, and is run when the config changes, after
			//   the backing fields have been updated
			// The bake method must receive either a SimpleConfig, a
			//   SimpleConfigCategory or a SimpleConfigGroup depending
			//   on where it is defined
			static void bake(SimpleConfigGroup g) {
				// Since int_value has been already updated, we may
				//   use its value for other computations
				// It's sometimes useful to precompute certain values
				//   in baker methods, if they're going to be accessed
				//   often and only rely on config values
				not_an_entry = (int_value * 2) - 1;
				// The received group can also be used to access
				//   config values, or to modify them, triggering however
				//   another bake (take care for infinite recursion)
				// To set values you can also alter the fields and then
				//   call g.commitFields()
				// Here we only read another value
				//   We could have used the long_value field directly as well
				//   getInt() performs the necessary casts if the entry is numeric
				not_an_entry *= g.getInt("long_value");
			}
		}
		
		@Bind public static class colors {
			@Bind public static Color no_alpha_color;
			@Bind public static Color alpha_color;
		}
		
		@Bind public static class lists {
			// It is important that the generics in the field type match
			@Bind public static List<String> string_list;
			@Bind public static List<Integer> int_list;
			@Bind public static List<Color> color_list;
			@Bind public static List<List<Color>> color_list_list;
		}
		
		@Bind public static class serializable {
			@Bind public static ResourceLocation resource;
			@Bind public static Item item;
			@Bind public static CompoundNBT nbt_compound;
			@Bind public static INBT nbt_value;
			@Bind public static Pair<String, Integer> pair;
		}
		
		@Bind public static class maps {
			// As with the lists, the generics of the field types must match
			@Bind public static Map<String, String> map;
			@Bind public static Map<String, List<String>> list_map;
			@Bind public static Map<String, Map<String, Integer>> even_int_map_map;
			@Bind public static Map<Integer, Integer> int_to_int_map;
		}
		
		@Bind public static class special {
			public static boolean non_persistent_bool;
		}
	}
	
	@Bind public static class errors_tooltips_n_links {
		@Bind public static String lowercase_string;
		@Bind public static int min;
		@Bind public static int max;
		@Bind public static List<Integer> even_int_list;
		@Bind public static String dynamic_tooltip;
		@Bind public static String tooltip_args;
	}
}
