package dnj.simple_config.demo;

import com.mojang.datafixers.util.Pair;
import dnj.simple_config.SimpleConfigMod;
import dnj.simple_config.core.Entry.ITranslatedEnum;
import dnj.simple_config.core.Entry.SerializableEntry;
import dnj.simple_config.core.SimpleConfig.Category;
import dnj.simple_config.core.SimpleConfigBuilder.CategoryBuilder;
import dnj.simple_config.core.annotation.ConfigEntry;
import dnj.simple_config.core.annotation.ConfigGroup;
import dnj.simple_config.core.annotation.RequireRestart;
import dnj.simple_config.core.annotation.Text;
import net.minecraft.item.Items;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import java.awt.*;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

import static dnj.simple_config.core.Entry.Builders.*;
import static dnj.simple_config.core.SimpleConfig.category;
import static dnj.simple_config.core.SimpleConfig.group;
import static dnj.simple_config.demo.DemoConfigCategory.RockPaperScissors.ROCK;
import static java.util.Arrays.asList;

// This whole file acts as a commented tour showcasing the
//   API possibilities. However, keep in mind that on its own
//   it's a pretty bad example of a simple config due to
//   its disorder.
// When implementing your config please stick to defining
//   it entirely on either the builder or the backing class.
//   Defining it in the builder doesn't prevent you from
//   having a backing class, and it's the recommended option.
/**
 * Demo API usage
 */
public abstract class DemoConfigCategory {
	// This category builder is added to the config builder
	//   in SimpleConfigMod. We could as well have put all
	//   the entries there, using categories is optional.
	// The entries without category are grouped in an implicit
	//   default category for its config (Client / Server)
	// Only server operators can modify categories registered
	//   in the server config
	public static CategoryBuilder getDemoCategory() {
		// We pass this class as the backing class
		// Note that any other class could be passed instead (or none at all)
		return category("demo", DemoConfigCategory.class)
		  // You may add text to the config GUI in using .text()
		  .text(ttc(pref("demo.greeting_text")))
		  // Adding entries is as easy with .add() and the various entry builders
		  // Entries get automatically mapped translation keys for both their
		  // label and their tooltip based on their paths
		  // The following entry is mapped under
		  //   config.⟨mod-id⟩.demo.some_bool
		  // and its tooltip is mapped to (if the translation exists)
		  //   config.⟨mod-id⟩.demo.some_bool.help
		  .add("some_bool", bool(true).restart())
		  .text("second_text")
		  // You can create list of other entries
		  .add("list", list(
		    color(Color.BLACK),
			 asList(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW)
		  ).expand())
		  // Of course, this can be nested, but try to keep your
		  //   config... simple for your users
		  // If you find yourself needing multiple nested lists in
		  //   your config, you probably instead want a custom
		  //   JsonReloadListener to extend your mod through datapacks
		  // Of course, every sub-entry may be configured with
		  //   error/tooltip suppliers as a regular entry
		  .add("list_list", list(
		    list(
		      color(Color.BLACK).tooltip1(c -> stc("color %s", c))
		    ).tooltip1(c -> stc("sublist %s", c)),
		    asList(asList(Color.RED, Color.GREEN), asList(Color.BLUE, Color.YELLOW))
		  ).tooltip1(c -> stc("list %s", c)))
		  // Entries may be decorated before being added to the config,
		  // adding an error supplier, or flagging them to require a restart
		  // It is also possible to provide a dynamic tooltip supplier which
		  // depends on the value, when a static tooltip is not sufficient
		  .add("lower_case_string", string("lowercase")
		    .guiError(s -> !s.equals(s.toLowerCase(Locale.ROOT))
		                ? Optional.of(ttc(pref("error.non_lowercase")))
		                : Optional.empty())
		    .restart(true))
		  .add("second_inventory_menu_icon", item(Items.APPLE))
		  // When using item entries, filter tags can only be used on the server config
		  //   However, keep in mind that behaviour customization related to
		  //   items should in general be deferred to datapacks whenever
		  //   possible through the use of recipe/custom json serializers.
		  //   Using items in configs for behaviour settings is a recipe for headaches.
		  // Also, the following entry is just an example, please don't restrict
		  //   the usage of possible items to your users unnecessarily
		  .add("terrain_inventory_menu_icon", item(Items.GRASS_BLOCK).from(
		    Items.GRASS_BLOCK, Items.DIRT, Items.STONE,
		    Items.NETHERRACK, Items.BLACKSTONE, Items.END_STONE
		  ))
		  // Nesting groups is done using the .n() method and the group() builder,
		  // which can be nested without limit
		  .n(group("overlay_colors", true)
		       .add("bg", color(Color.GRAY, true))
		       .add("fg", color(Color.LIGHT_GRAY, true))
		       .n(group("error")
		            .add("bg", color(new Color(0x540000), true))
		            .add("fg", color(new Color(0xEE5050), true)))
		       .n(group("info")
		            .add("bg", color(new Color(0x002040), true))
		            .add("fg", color(new Color(0x50A0C0), true))))
		  .n(group("numbers")
		       // Number values can define valid ranges in their builders
		       // Only long and double numeric types are supported, since they cover the others
		       .add("magic_number", number(0, 0L, 100L).slider())
		       // Currently, double sliders are not supported
		       // A null bound is equivalent to unbound
		       .add("speed_meters_per_second", number(2.4, 0D, null))
		       // A fractional entry is an alias for number(value, 0.0, 1.0)
		       .add("loot_rate", fractional(0.2)))
		  .add("serializable", new SerializableEntry<>(
		    Pair.of(0.0, 1.0), p -> p.getFirst().toString() + "," + p.getSecond().toString(),
		    s -> Optional.of(Pair.of(
		      Double.parseDouble(s.split(",")[0]),
		      Double.parseDouble(s.split(",")[1])))))
		  // A baker method can be added (optionally) to each category/config
		  // The baker method runs whenever the config changes externally or through the GUI
		  // The config baker runs before the category ones
		  .setBaker(DemoConfigCategory::bakeDemoCategory);
	}
	
	// Sometimes baking isn't as simple as setting a few fields
	public static void bakeDemoCategory(Category demo) {
		// The baker always runs after all backing fields have been baked
		// So we may safely use them to create composite values, or transform them
		complex_string = string + "\n" + lower_case_string;
		
		// Sometimes it's useful to convert the config values to more
		//   suitable units for computation, and possibly a different type
		// Users will understand better a speed in m/s than in m/tick,
		//   but you may need to apply it per tick
		// You could define a backing field for speed_meters_per_second,
		//   but it isn't necessary, since it's possible to access
		//   values by name in the baker method
		numbers.speed_meters_per_tick =
		  demo.getFloat("numbers.speed_meters_per_second") / 20F;
	}
	
	// Since we passed this class as the backing class, it will be parsed
	//   for backing fields and extra config entries
	// Backing fields should not be final, and must be static
	// Inner static classes may be used to represent either groups
	//   or categories (only in the top level) with the proper annotations
	// Also, categories may have their own separate backing class, as
	//   this one has, specified in their builder
	
	// Entries added from the backing class always come after those in the
	//   builder. As said before, you should generate your entries either
	//   in the builder, or either in the config class, instead of mixing them
	//   like in this showcase
	
	// Within the config class you may define new config values with annotations
	@ConfigEntry public static String string = "Hello, World!";
	// Or you may simply declare backing fields for values already defined in the builder
	// Fields already defined in the builder shouldn't have annotations
	public static String lower_case_string;
	
	// Fields not defined in the builder nor annotated do not produce an entry,
	//   but may be used by the baker method
	public static String complex_string;
	
	// A common pattern is creating enums for settings with a few choices
	@ConfigEntry public static RockPaperScissors your_choice = ROCK;
	public enum RockPaperScissors {
		ROCK, PAPER, SCISSORS
		
		// Enums are automatically mapped to translation keys, however,
		// you can't differentiate multiple enums with the same name
		// This enum could define its translations under the keys
		//    config.⟨mod-id⟩.enum.demo.rock_paper_scissors.rock
		//    config.⟨mod-id⟩.enum.demo.rock_paper_scissors.paper
		//    config.⟨mod-id⟩.enum.demo.rock_paper_scissors.scissors
		// But if this keys do not exist, their names would be used instead
	}
	
	// Enum values missing their initializer will default to the first
	//   enum constant, in this case UPRIGHT
	@ConfigEntry public static Placement preferred_placement;
	
	// Additionally, enums may implement ITranslatedEnum instead, providing their own translations
	public enum Placement implements ITranslatedEnum {
		UPRIGHT, UPSIDE_DOWN;
		
		// Although in this case it doesn't make much difference
		@Override public ITextComponent getDisplayName() {
			return ttc(pref("enum.demo.placement." + name().toLowerCase()));
		}
	}
	
	// You may declare groups as static subclasses
	// The classes don't need to be abstract, but it helps emphasize that they won't be instantiated
	// Classes without annotations are ignored, like the enums above, unless the
	//   builder defines a name or category with the same name
	// Groups and categories are mapped translations keys automatically,
	//   under the config.⟨mod-id⟩.group and config.⟨mod-id⟩.category namespaces
	// For instance, this group's label and tooltip would be mapped to the keys
	//   config.⟨mod-id⟩.group.client.demo.simple_group
	//   config.⟨mod-id⟩.group.client.demo.simple_group.help (Tooltip, if defined)
	@ConfigGroup(expand = true)
	public static abstract class simple_group {
		// Types that don't require extra parameters to declare their entries
		// use the @ConfigEntry annotation directly
		@ConfigEntry public static boolean some_bool = false;
		
		// You may also add text entries from the backing class
		//   using the @Text annotation. The field may be either:
		//    - A String, which value is ignored
		//        It is mapped to a translation according to its name/path
		//        For instance, the following field is mapped to the translations
		//          config.⟨mod-id⟩.demo.simple_group.some_text_key
		//          config.⟨mod-id⟩.demo.simple_group.some_text_key.help (Tooltip, optional)
		//    - An ITextComponent, which is served right-away
		//        It may be a TranslationTextComponent, or even have click events
		//        through .modifyStyle()
		//    - A Supplier<ITextComponent>, which can offer different
		//        contents dynamically
		// Making their fields not public helps keep a cleaner config API to use from the project
		@Text private static String some_text_key;
		
		// Fields whose entries require extra parameters have their
		//   own annotations under @ConfigEntry, such as @ConfigEntry.Long
		@ConfigEntry.Long(min = 0, max = 10L, slider = true)
		public static long score = 10L;
		// Every field can declare an error supplier, appending the suffix '$error' to its name
		//   This can also be done in the builder, by using the '.error(...)' method on the entry
		//   Entries generated in the builder cannot define their error supplier in the backing class
		// Like text fields, these methods should be private to avoid cluttering the exposed API
		private static Optional<ITextComponent> score$error(long v) {
			// This example entry accepts only even values
			return v % 2 != 0 ? Optional.of(ttc(pref("error.demo.not_even"))) : Optional.empty();
		}
		// A '$tooltip' method can also be added to provide dynamic or complex
		//   tooltips to entries, but using the builder for this is recommended
		// Note that static tooltips can be added directly in the translations JSON
		//   under the same key of the field followed by '.help', and they support
		//   newlines automatically
		private static Optional<ITextComponent[]> score$tooltip() {
			// Here we have to split the lines manually...
			return Optional.of(new ITextComponent[] {
			  ttc(pref("tooltip.score.1")),
			  ttc(pref("tooltip.score.2"))
			});
		}
		
		// Text fields can be final, since they won't be updated
		@Text private static final ITextComponent _1 = ttc(pref("some_other_text"));
		// Generated entries may also have the @RequireRestart annotation
		//   to flag them as requiring a world restart to be effective
		//   Likewise, this can also be done in the builder
		@ConfigEntry.Double(min = 0, max = 1) @RequireRestart
		public static double ore_gen_chance = 1;
		
		// Fields without annotations that are also not defined in the builder are ignored
		public static String ignored_field = "/summon minecraft:sheep ~ ~ ~ {Color:6}";
		
		// This text entry could have been an ITextComponent directly, since
		//   its generation does not depend on the state of the mod
		@Text private static final Supplier<ITextComponent> _2 = () ->
		  ttc(pref("some_complex_text"),
		      stc(ignored_field).modifyStyle(style -> style
		        .setFormatting(TextFormatting.LIGHT_PURPLE)
		        // If you're planning to have many 'links', you may want to create a wrapper for this
		        .setHoverEvent(new HoverEvent(
		          HoverEvent.Action.SHOW_TEXT, ttc("chat.copy.click")))
		        .setClickEvent(new ClickEvent(
		          ClickEvent.Action.COPY_TO_CLIPBOARD, ignored_field))));
		
		@ConfigGroup public static abstract class nested_group {
			@ConfigEntry.Color
			public static Color no_alpha_color = Color.BLUE;
			@ConfigEntry.Color(alpha = true)
			public static Color alpha_color = Color.YELLOW;
			
			// Some list types are also supported out of the box
			@ConfigEntry.List.Long(min = 0L)
			// Leaving uninitialized is the same as an empty list
			// The default value can also be set in a static initializer
			public static List<Long> long_list = asList(0L, 2L);
			// List entries may define an element validator instead/in addition to
			//   an error supplier. This is also possible in the builder
			// The method may accept the primitive type as well, instead of Long
			private static boolean long_list$validate(long element) {
				// Again here we only accept even values
				// Notice that, separately, in the annotation we've also
				//   restricted the values to be >= 0
				// Setting the ranges in the annotation helps provide a
				//   more precise error message to the user
				return element % 2 == 0;
			}
			
			@ConfigEntry.List.Double(min = 0, max = 1)
			public static List<Double> double_list = asList(0.1, 0.2, 0.3, 0.4);
			// The validator method can also return an Optional<ITextComponent>
			//   to supply better error messages
			private static Optional<ITextComponent> double_list$validate(double element) {
				// Here we limit the number of decimals to 1 for no reason
				//   If we *really* needed them to have just one decimal
				//   the correct approach would probably be using the baker
				//   method to round them, this is just an example
				return Double.compare(element, Math.round(element * 10D) / 10D) != 0
				       ? Optional.of(ttc(pref("error.too_many_decimals")))
				       : Optional.empty();
			}
		}
	}
	
	public static class numbers {
		// This field does not belong to any entry, it is set by the baker above
		public static float speed_meters_per_tick;
	}
	
	// Convenience
	public static String pref(String key) {
		return SimpleConfigMod.MOD_ID + ".config." + key;
	}
	public static StringTextComponent stc(String msg, Object... args) {
		return new StringTextComponent(String.format(msg, args));
	}
	public static TranslationTextComponent ttc(String key, Object... args) {
		return new TranslationTextComponent(key, args);
	}
}
