package endorh.simple_config.demo;

import endorh.simple_config.SimpleConfigMod;
import endorh.simple_config.core.SimpleConfig;
import endorh.simple_config.core.annotation.*;
import endorh.simple_config.demo.DemoServerConfig.demo.demo_group;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.fml.config.ModConfig.Type;

import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Arrays.asList;

public class DemoServerConfig {
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
	
	public static void registerServerConfig() {
		// In this example, we generate all config entries
		//   directly in the config class through the use of annotations
		// This is discouraged, since it's more restrictive than the
		//   builder. For instance, you may not define lists of
		//   complex types, such as lists of lists, nor lists of maps
		//   in the config class.
		//   Declaring groups and categories like this may require you
		//   to give their order (relative to its siblings) explicitly
		//   in their annotations since it's not possible to read inner
		//   classes in declaration order.
		// Where possible, you should use the builder to define your
		//   entries, as it's done in DemoConfigCategory, but this
		//   class serves as an example for small/simple configs that
		//   may not require any such features.
		
		// We use this class as the backing class
		SimpleConfig.builder(SimpleConfigMod.MOD_ID, Type.SERVER, DemoServerConfig.class)
		  .buildAndRegister();
	}
	
	// A bake method is automatically recognized in the backing class
	//   Within categories or groups, the bake method must receive
	//   the proper type, either SimpleConfigCategory or SimpleConfigGroup
	protected static void bake(SimpleConfig config) {
		// The baker method may be used to translate units
		//   or precompute frequently used values each time
		//   the config changes
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
	// Categories may be generated with @Category
	@Category public static class demo {
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
		@Text private static String greeting;
		
		// Groups can be created with @Group
		//   We pass the desired order of the group related to its siblings
		//   since Java can't read inner classes in declaration order
		// Also, the default insertion place is 0, so 1 will be after the
		//   greeting above. Negative values would place this group above greeting
		//   Conflicting indexes will resolve entries before groups and then
		//   (hopefully) entries in declaration order and groups randomly
		@Group(value = 1, expand = true)
		public static class group {
			// Most entry types can be created by simply using @Entry
			// The default value of an entry is the field's initial value
			@Entry public static boolean bool = false;
			
			// More exactly, it is the field's actual value when the
			//   config is registered, so the following static
			//   initializer affects the default value for this field
			@Entry public static CompoundNBT nbt = new CompoundNBT();
			static {
				nbt.put("name", StringNBT.valueOf("Steve"));
				nbt.put("health", IntNBT.valueOf(20));
			}
			// Enum fields without an initializer default to the first
			//   enum constant in the class, but you should be explicit
			@Entry public static Direction direction;
		}
		
		@Group(value = 2, expand = true)
		public static abstract class demo_group {
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
			private static Optional<ITextComponent> even_score$error(long v) {
				// This example entry accepts only even values
				return v % 2 != 0 ? Optional.of(ttc(prefix("error.demo.not_even"), v)) : Optional.empty();
			}
			// A '$tooltip' method can also be added to provide dynamic or complex
			//   tooltips to entries, but using the builder for this is recommended
			// Note that static tooltips can be added directly in the translations JSON
			//   under the same key of the field followed by '.help', and they support
			//   newlines automatically
			private static Optional<ITextComponent[]> even_score$tooltip() {
				// Here we have to split the lines manually...
				return Optional.of(new ITextComponent[] {
				  ttc(prefix("tooltip.score.1")),
				  ttc(prefix("tooltip.score.2"))
				});
			}
			
			// Text fields can be final, since they won't be updated
			@Text private static final ITextComponent _1 = ttc(prefix("text.some_other_text"));
			
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
			
			// Text entries may be suppliers of ITextComponent instead
			//   Fon example, the following text entry would change
			//   if the mod modified the summon_command field
			@Text private static final Supplier<ITextComponent> _2 = () ->
			  ttc(prefix("text.some_complex_text"),
			      stc(summon_command).modifyStyle(style -> style
				     .setFormatting(TextFormatting.LIGHT_PURPLE)
				     // If you're planning to have many clickable links,
			        // you may want to create a wrapper for this
				     .setHoverEvent(new HoverEvent(
					    HoverEvent.Action.SHOW_TEXT, ttc("chat.copy.click")))
				     .setClickEvent(new ClickEvent(
					    ClickEvent.Action.COPY_TO_CLIPBOARD, summon_command))));
			
			// Groups will always be placed after entries with the same order
			//   Since neither the above entries nor this group declare
			//   an order, both use 0, and the group is resolved at the end
			@Group
			public static abstract class nested_group {
				@Entry
				public static Color no_alpha_color = Color.BLUE;
				@Entry @HasAlpha
				public static Color alpha_color = Color.YELLOW;
				
				// Some list types are also supported out of the box
				@Entry @Min(0)
				// Leaving uninitialized is the same as defaulting to an empty list
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
				
				@Entry @Min(0) @Max(1)
				public static List<Double> double_list = asList(0.1, 0.2, 0.3, 0.4);
				// The validator method can also return an Optional<ITextComponent>
				//   to supply better error messages
				private static Optional<ITextComponent> double_list$validate(double element) {
					// Here we limit the number of decimals to 1 for no reason
					//   If we *really* needed them to have just one decimal
					//   the correct approach would be using the baker
					//   method to round them, this is just an example
					return Double.compare(element, Math.round(element * 10D) / 10D) != 0
					       ? Optional.of(ttc(prefix("error.too_many_decimals")))
					       : Optional.empty();
				}
			}
		}
	}
}
