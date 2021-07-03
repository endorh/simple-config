package dnj.simple_config.demo;

import com.mojang.datafixers.util.Pair;
import dnj.simple_config.SimpleConfigMod;
import dnj.simple_config.core.SimpleConfigBuilder.CategoryBuilder;
import dnj.simple_config.core.SimpleConfigCategory;
import dnj.simple_config.core.entry.EnumEntry.ITranslatedEnum;
import me.shedaniel.clothconfig2.api.ModifierKeyCode;
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
import net.minecraftforge.fml.loading.FMLPaths;

import java.awt.Color;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static dnj.simple_config.SimpleConfigMod.CLIENT_CONFIG;
import static dnj.simple_config.core.SimpleConfig.category;
import static dnj.simple_config.core.SimpleConfig.group;
import static dnj.simple_config.core.entry.Builders.*;
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
		  .add("bool", bool(true).restart())
		  // Creating config subgroups is done with .n(), which is
		  //   short for 'nest', and the group() builder
		  // Groups can be automatically expanded in the GUI, by
		  //   passing true in their builder
		  // Groups can contain entries and other groups
		  .n(group("entry_types", true)
		       // Text entries may also be defined by name, receiving
		       //   automatically mapped translation keys as other entries
		       .text("intro")
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
		       // Any value can be marked as requiring a restart to be effective
		       //   Entire groups and categories can be marked as well
		       .add("double_value", number(0.0, -10, 10).restart())
		       // All numeric entry types can be displayed as sliders, including
		       //   floats and doubles
		       .add("long_slider", number(5L, 0, 10).slider())
		       // For obvious reasons, entries displayed as sliders must
		       //   have finite defined bounds
		       .add("float_slider", number(80F, 0, 100).slider())
		       // String values are also common
		       .add("string_value", string("Hello World!"))
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
		       // By intentional design, there's no support for keybinding
		       //   entries, please register your keybindings through
		       //   the ClientRegistry.registerKeyBinding to benefit
		       //   from Forge's key conflict resolution and an entry
		       //   in the Controls GUI
		       // Groups can be nested
		       .n(group("colors")
		            // Color entries use java.awt.Color as their type
		            .add("no_alpha_color", color(Color.BLUE))
		            // Colors may optionally allow alpha values
		            .add("alpha_color", color(Color.YELLOW, true)))
		       .n(group("lists")
		            // Lists of basic types are supported
		            .add("string_list", list(asList("Lorem ipsum", "dolor sit amet")))
		            // For numeric lists, null bounds mean unbound ends
		            .add("int_list", list(asList(1, 2, 4, 8), 0, null))
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
		            .add("nbt_compound", nbt(nbt))
		            // If you pass a INBT default value instead of a compound,
		            //   the entry will also accept literal NBT values
		            .add("nbt_value", nbt(StringNBT.valueOf("NBT")))
		            // It is also possible to use custom String serializable entries
		            //   A class may implement ISerializableConfigEntry to be
		            //   automatically serializable as well
		            .add("pair", entry(
		              Pair.of("string", 2),
		              p -> p.getFirst() + ", " + p.getSecond(),
		              s -> {
			              if (s == null || s.isEmpty())
			              	  return Optional.empty();
			              final String[] split = s.split(",");
			              if (split.length != 2)
			              	  return Optional.empty();
			              try {
				              return Optional.of(Pair.of(split[0].trim(), Integer.parseInt(split[1].trim())));
			              } catch (NumberFormatException ignored) {
				              return Optional.empty();
			              }
		              }))))
		  // Text entries can also receive format arguments if they
		  //   are defined by name
		  .text("non_persistent_desc", ttc(prefix("text.non_persistent")).mergeStyle(TextFormatting.GRAY))
		  // A special type of entry is non persistent boolean flags
		  //   This entries do not have an entry in the config file,
		  //   and revert to their default whenever restarting the game
		  // They are rarely useful for enabling special profiling
		  //   or debugging features
		  .add("non_persistent_bool", nonPersistentBool(false))
		  // You may add links or contextual tooltips in text entries
		  //   by calling .modifyStyle() on text components
		  .text("open_file", ttc(prefix("text.mod_file")).modifyStyle(style -> style
		    .setFormatting(TextFormatting.DARK_AQUA)
		    .setClickEvent(new ClickEvent(
		      ClickEvent.Action.OPEN_FILE, FMLPaths.GAMEDIR.get().resolve("options.txt").toString()))
		    .setHoverEvent(new HoverEvent(
		      HoverEvent.Action.SHOW_TEXT, ttc(prefix("text.hover.open_file"))))))
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
		         ? Optional.of(ttc(prefix("error.not_lowercase")))
		         : Optional.empty()))
		  // To check other values in the error supplier, you may use
		  //   the 'getGUI' method (and its primitive variants), which
		  //   retrieves the current candidate value for an entry, or
		  //   the current value if no GUI is active
		  .add("min", number(0).error(
		    n -> CLIENT_CONFIG.getGUIInt("demo.max") < n
		         ? Optional.of(ttc(prefix("error.min_greater_than_max")))
		         : Optional.empty()))
		  // In cases like this, it's better to have the check on both
		  //   keys so that when the error occurs in the config file,
		  //   both keys get reset to a valid default state
		  // However, if you need this kind of range often,
		  //   you could just create a serializable range class
		  .add("max", number(10).error(
		    n -> CLIENT_CONFIG.getGUIInt("demo.min") > n
		         ? Optional.of(ttc(prefix("error.min_greater_than_max")))
		         : Optional.empty()))
		  // For lists, you may also set an element validator, which
		  //   takes single elements, instead of the whole entry
		  //   to validate
		  // It is also possible to call .setValidator, which receives
		  //   a simple Predicate, instead of an error supplier,
		  //   and produces a generic error message, but this is
		  //   discouraged
		  .add("even_int_list", list(asList(2, 4), -20, 20)
		    .elemError(i -> i % 2 != 0 ? Optional.of(ttc(prefix("error.not_even"))) : Optional.empty()))
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
		    .tooltipArgs((Function<String, ITextComponent>)
		                   s -> stc(s).mergeStyle(TextFormatting.DARK_AQUA)))
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
	//    keys under the key 'config.{mod-id}.enum.rock_paper_scissors.*'
	public enum RockPaperScissors {
		ROCK, PAPER, SCISSORS
	}
	
	
	// Alternatively, enums may implement ITranslatedEnum instead,
	//   providing their own translations
	public enum Placement implements ITranslatedEnum {
		UPRIGHT, UPSIDE_DOWN;
		
		// Although in this case it doesn't make much difference
		@Override public ITextComponent getDisplayName() {
			return ttc(prefix("enum.demo.placement." + name().toLowerCase()));
		}
	}
	
	// Since this is the config class for the category, we may
	//   directly define here backing fields for all entries
	// Backing fields must be static, and shouldn't be final
	// If a backing field does not match its expected type,
	//   the game will crash early
	public static boolean bool;
	// Groups are mapped into static inner classes
	public static class entry_types {
		public static int int_value;
		public static long long_value;
		public static float float_value;
		public static double double_value;
		
		public static long long_slider;
		public static float float_slider;
		
		public static String string_value;
		public static RockPaperScissors enum_value;
		
		// As said before, register your keybindings with
		//   ClientRegistry.registerKeyBinding to benefit
		//   from Forge's key conflict resolution
		@Deprecated public static ModifierKeyCode key_bind;
		@Deprecated public static ModifierKeyCode mouse_key;
		
		public static class colors {
			public static Color no_alpha_color;
			public static Color alpha_color;
		}
		
		public static class lists {
			public static List<String> string_list;
			public static List<Integer> int_list;
			public static List<Color> color_list;
			public static List<List<Color>> color_list_list;
		}
		
		public static class serializable {
			public static ResourceLocation resource;
			public static Item item;
			public static CompoundNBT nbt_compound;
			public static INBT nbt_value;
			public static Pair<String, Integer> pair;
		}
	}
	
	public static boolean non_persistent_bool;
	public static String lowercase_string;
	public static int min;
	public static int max;
	public static List<Integer> even_int_list;
	public static String dynamic_tooltip;
	public static String tooltip_args;
}
