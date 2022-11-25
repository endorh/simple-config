package endorh.simpleconfig.demo;

import com.google.common.collect.ImmutableMap;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.api.AbstractRange.DoubleRange;
import endorh.simpleconfig.api.AbstractRange.FloatRange;
import endorh.simpleconfig.api.ConfigCategoryBuilder;
import endorh.simpleconfig.api.EntryTag;
import endorh.simpleconfig.api.SimpleConfigCategory;
import endorh.simpleconfig.api.annotation.Bind;
import endorh.simpleconfig.api.entry.EnumEntryBuilder.TranslatedEnum;
import endorh.simpleconfig.api.entry.IConfigEntrySerializer;
import endorh.simpleconfig.api.entry.RangedEntryBuilder.InvertibleDouble2DoubleFunction;
import endorh.simpleconfig.api.ui.hotkey.KeyBindMapping;
import endorh.simpleconfig.api.ui.icon.Icon;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons.Entries;
import endorh.simpleconfig.core.SimpleConfigGroupImpl;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.beans.Transient;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static endorh.simpleconfig.SimpleConfigMod.CLIENT_CONFIG;
import static endorh.simpleconfig.api.ConfigBuilderFactoryProxy.*;
import static endorh.simpleconfig.api.ui.hotkey.KeyBindMapping.parse;
import static java.lang.Math.abs;
import static java.util.Arrays.asList;

/**
 * Demo builder API usage
 */
public class DemoConfigCategory {
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
	
	// This category builder is added to the config builder
	//   in SimpleConfigMod. We could as well have put all
	//   the entries there. Using categories is optional.
	// The entries without category are grouped in an implicit
	//   default category for its config (Client / Server)
	// Only server operators can access categories registered
	//   in the server config
	public static ConfigCategoryBuilder getDemoCategory() {
		// This value will be used below
		CompoundTag nbt = new CompoundTag();
		nbt.putString("name", "Steve");
		nbt.putInt("health", 20);
		
		// Used below
		List<String> natoPhoneticAlphabet = asList(
		  "Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf",
		  "Hotel", "India", "Juliet", "Kilo", "Lima", "Mike", "November", "Oscar",
		  "Papa", "Quebec", "Romeo", "Sierra", "Tango", "Uniform", "Victor",
		  "Whiskey", "X-ray", "Yankee", "Zulu");
		
		// We pass this (DemoConfigCategory) class as the backing class
		//   Note that any other class could be passed instead,
		//   for instance, an inner static class, or none at all
		//noinspection ArraysAsListWithZeroOrOneArgument
		return category("demo", DemoConfigCategory.class)
		  // Categories may have an icon and a color, which are used in its tab button
		  //   when more than one category is available.
		  .withIcon(SimpleConfigIcons.Status.INFO)
		  .withColor(0x80607080)
		  // Categories can also have an associated background
		  //   The whole config may also define a default background
		  //   for all categories
		  // Note that the background won't display in-game, unless
		  //   you call .withSolidInGameBackground() in the config builder
		  //   because configs are transparent by default in-game
		  // It will always display when opened from the main menu however.
		  .withBackground("textures/block/warped_planks.png")
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
		  // The tooltip translation key is the label key followed by ':help'
		  //   and its optional, that is, if it's not defined, the entry
		  //   won't display a tooltip.
		  // Tooltip translation keys can contain newline characters to
		  //   produce multiline tooltips, and they automatically wrap as well.
		  // For example, the following entry is mapped under
		  //   {mod-id}.config.demo.some_bool
		  // and its tooltip is mapped to (if the translation exists)
		  //   {mod-id}.config.demo.some_bool:help
		  // There's a translation debug mode which can be enabled in the
		  //   SimpleConfig mod config, which will display translation
		  //   keys, and highlight missing ones in red.
		  .add("bool", bool(true))
		  // Creating config subgroups and categories is done with .n(),
		  //   which is short for 'nest', and the group()/category() builders
		  // Categories can not be nested more than one level, and produce
		  //   separate pages in the GUI. Groups can be nested without limit.
		  // Groups can be automatically expanded in the GUI, by
		  //   passing true in their builder
		  // Groups can contain entries and other groups
		  .n(group("entries", true)
		       // Text entries may also be defined by name, receiving
		       //   automatically mapped translation keys like other entries
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
		            //   fraction(v) is equivalent to number(v, 0, 1).slider();
		            .add("float_value", fraction(0.8F).slider(false))
		            // Float and double values can have a field scale, which is
		            //   multiplied to their value before being assigned to their
		            //   backing field. The scale is inversely applied backwards
		            //   if the field value is committed.
		            // This helps expose users more understandable units, while your
		            //   field uses the most useful for your code.
		            //   For instance, you may expose speed in m/s, but actually use
		            //   it as m/tick in your code, by multiplying the field by 0.05 as here
		            .add("double_value", number(0.0, -10, 10).fieldScale(0.05))
		            // All numeric entry types can be displayed as sliders, including
		            //   floats and doubles
		            // Sliders still let users introduce exact numbers by toggling a text
		            //   input in the GUI, so don't worry about precision when a slider
		            //   would be more intuitive for the general user
		            .add("long_slider", number(5L, 0, 20).slider())
		            // It's also possible to define a different range for the slider than
		            //   the entry itself. In this case, values outside the slider range will
		            //   only be editable as text. This is convenient to differentiate between
		            //   the recommended and the possible range of an entry.
		            //   (The slider range is constrained by the entry range).
		            // Slider entries must either declare min and max, or a slider range,
		            //   since otherwise the slider would have no bounds.
		            .add("float_slider", number(80F).min(0).sliderRange(0, 100))
		            // There's also a shortcut for volume sliders, which display a different
		            //   translation in the slider widget, and take 1F as their default
		            // Sliders may take custom translation keys to show in the slider widget.
		            //   The volume() builder is simply a shortcut for
		            //   fraction(1F).slider("simpleconfig.format.slider.volume")
		            // Sliders can have non-linear mapping over their range
		            .add("sqrt_slider", number(50, 0, 100).sliderMap(Mth::square, Math::sqrt))
		            .add("exp_slider", number(10F, 0, 1000).sliderMap(
		              InvertibleDouble2DoubleFunction.expMap(10)))
		            .add("volume_slider", volume())
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
		            // Enum entries get automatically mapped translation keys
		            //   for their values, however, these mappings do not
		            //   depend on their path, so enums with the same name
		            //   may clash
		            // For instance, the enum RockPaperScissors would be
		            //   mapped to the translation keys
		            //     {mod-id}.config.enum.rock_paper_scissors.rock
		            //     {mod-id}.config.enum.rock_paper_scissors.paper
		            //     {mod-id}.config.enum.rock_paper_scissors.scissors
		            .add("enum_value", option(RockPaperScissors.SCISSORS))
		            // Enums may define their own translations implementing
		            //   ITranslatedEnum (see the Placement enum)
		            .add("enum_value_2", option(Placement.UPSIDE_DOWN))
		            // String entries can suggest possible values and restrict a maximum length
		            .add("str_suggest", string("Alpha")
		              .suggest(natoPhoneticAlphabet).maxLength(20))
		            // The suggestions may also be restrictions
		            .add("str_restrict", string("Alpha")
		              .restrict(natoPhoneticAlphabet))
		            // Any entry can be restricted to a finite set of values
		            //   by using select(), resulting in an enum-like GUI control
		            // A user setting may transform these enum-like controls with
		            //   too many options into combo boxes for a better interface
		            // .add("str_select", select(string("Alpha"), natoPhoneticAlphabet))
		            // Key binding entries also exist, HOWEVER,
		            //   PLEASE REGISTER YOUR KEY BINDINGS THROUGH
		            //   ClientRegistry.registerKeyBinding INSTEAD, to
		            //   benefit from Forge's key conflict resolution and an
		            //   entry in the Controls GUI, where USERS WILL EXPECT it.
		            // Key binding entries are only supported to be used in
		            //   map entries, like shown below in this demo.
		            // Note that the KeyBinding class used by standard
		            //   key bindings and the ModifierKeyCode class used by
		            //   key bind entries are distinct.
		       ).n(group("colors")
		            // Color entries use java.awt.Color as their type
		            .add("no_alpha_color", color(Color.BLUE))
		            // Colors may optionally allow alpha values
		            .add("alpha_color", color(Color.YELLOW).alpha()))
		       .n(group("ranges")
		            // Among number entries, it's common to have two entries to configure a
		            //   valid range of values. This can be neatly packed in just one entry of
		            //   Range type, which is a type of pair.
		            //   Range entries accept min and max arguments, as well as minSize and maxSize
		            .add("range_entry", range(1, 20).min(0).max(100).minSize(2))
		            // Ranges can be exclusive, which is displayed in the interface
		            .add("double_range_entry", range(DoubleRange.exclusive(0.5, 1.5))
			           .min(0).max(10).allowEmpty(true).maxSize(1D))
		            .add("half_exclusive_range", range(DoubleRange.of(0, false, 1, true))
			           .withBounds(-10, 10))
		            // Range exclusiveness may also be left editable.
		            .add("editable_exclusiveness_range", range(DoubleRange.of(0, false, 1, true))
			           .withBounds(-10, 10).allowEmpty(true).canEditExclusiveness(true, true)))
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
		            .add("slider_list_list", list(
						  list(number(0F, 0, 10).slider(), asList(0F)),
						  asList(asList(0F, 1F), asList(2F, 3F))))
		            // Additionally, list types can hold another entry of
		            //   non-expandable types by using caption()
		            // Held entries are displayed on the blank space at the caption
		            //   of the list entry.
		            // A caption() entry produces an org.apache.commons.lang3.tuple.Pair
		            .add("color_list_list", caption(color(Color.GRAY), list(
		              caption(color(Color.GRAY), list(color(Color.BLACK), asList(Color.GRAY))),
		              asList(
							 Pair.of(Color.CYAN, asList(Color.YELLOW, Color.BLUE)),
							 Pair.of(Color.PINK, asList(Color.RED, Color.GREEN))))))
		            // You may also create Java Bean lists, using bean entries.
		            .add("bean_list", list(
						  bean(new BeanDemo())
						    .caption("name", string("<unnamed>"))
						    .add("range", range(FloatRange.inclusive(0F, 1F)))
						    .add("color", color(Color.WHITE))
						    .add("pattern", pattern("(\\d+):(\\w+)"))
						    .withIcon(BeanDemo::getIcon), asList(new BeanDemo()))))
		            // If you find yourself needing multiple nested lists or maps
		            //   in your config, you may instead want a custom
		            //   JsonReloadListener to extend your mod through datapacks
		       // A few string-serializable types come built-in as well
		       .n(group("serializable")
		            // Resource entries contain ResourceLocation s
		            .add("resource", resource("minecraft:elytra"))
		            // Item and block entries provide auto-completion in the GUI as well as icons
		            .add("block", block(Blocks.GRASS_BLOCK))
		            .add("item", item(Items.NETHERITE_HOE))
		            // There are also fluid entries, which show their filled buckets as icons
		            .add("fluid", fluid(Fluids.WATER))
		            // All these entries can be restricted to certain items/blocks/fluids
		            //   Using tags as filter is only possible in server configs,
		            //   as tags are server-dependant
		            // However, items in configs should not be used to customize
		            //   recipes, only other behaviours. Use custom recipe
		            //   serializers for that purpose.
		            .add("apple_item", item(Items.APPLE)
		              .from(Items.APPLE, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE))
		            // All these entries can accept unknown items/blocks/fluids
		            //   by using the itemName()/blockName()/fluidName variant entry types
		            //   which use resource locations as their type instead
		            // This is preferable to simply using a resource() entry, since it
		            //   provides icons and auto-completion for known items
		            // You may also suggest a specific set of items calling .suggest()
		            .add("item_name", itemName(new ResourceLocation("unknown:item")))
		            // NBT compound tag entries contain `CompoundTag`s, and automatically
		            //   check the SNBT syntax in the GUI
		            .add("nbt_compound", compoundTag(nbt))
		            // NBT tag entries also accept `Tag` values as well as `CompoundTag`s
		            .add("nbt_value", tag(StringTag.valueOf("NBT")))
		            // It is also possible to use custom String serializable entries
		            //   You may either pass an IConfigEntrySerializer,
		            //   make the type implement ISerializableConfigEntry, or
		            //   pass directly two de/serializing lambdas
		            // In this case we opt for the first choice, because we
		            //   can't modify the Pair class, and using lambdas directly
		            //   isn't reusable
		            .add("pair", entry(
		              Pair.of("string", 2), new StringIntPairSerializer()))
		            .add("bean", bean(new BeanDemo())
		              .caption("name", string(""))
		              .add("range", range(FloatRange.inclusive(0F, 1F)))
		              .add("color", color(Color.WHITE))
		              .add("pattern", pattern("(\\d+):(\\w+)"))
		              .withIcon(BeanDemo::getIcon)))
		     .n(group("maps")
		          // Map entries are also supported
		          // Map values may be any other entry that serializes
		          //   in the config as any type serializable to NBT
		          //   Currently, this includes every built-in entry type
		          //   except GUI-only entries, including other maps and lists
		          // By default, map keys are strings, but all types that
		          //   can be serialized as strings, and are not expandable
		          //   can be keys. Specifically, other lists and maps
		          //   cannot be used as keys, nor GUI-only entries.
		          // Currently, maps are serialized as NBT compounds in the
		          //   config file, since I haven't found a better alternative.
		          // As lists, maps can hold another entry in their caption
		          //   by passing it to the same .add() method call
		          //   (see below)
		          // Note that, although we use ImmutableMap.of() (for conciseness)
		          //   the values you'll retrieve are only guaranteed to implement
		          //   the Map interface, regardless of the specific type you
		          //   provide as default.
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
		          // In this case, the held entry is used in the validation too
		          .add("divisible_int_map_map", caption(number(2), map(
		            map(number(0)
		                  .error(i -> {
			                  Integer d = CLIENT_CONFIG.<Pair<Integer, Map<String, Map<String, Integer>>>>getGUI(
									  "demo.entries.maps.divisible_int_map_map").getKey();
			                  return d == null || d == 0 || i % d != 0 ? Optional.of(
			                    ttc(prefix("error.not_divisible"), i, d)
			                  ) : Optional.empty();
		                  }), ImmutableMap.of("", 0)
		            ), ImmutableMap.of(
		              "plains", ImmutableMap.of("Cornflower", 2, "Dandelion", 4),
		              "birch forest", ImmutableMap.of("Poppy", 8, "Lily of the Valley", 4)))))
		          // You may pass an entry builder for the key type as well,
		          //   as long as it's serializable as a string.
		          //   Invalid types will result in a compile-time error.
		          // The key entry may have error suppliers, but its tooltip
		          //   is currently ignored
		          // In this example, we use color for both the keys and the values
		          .add("color_color_map", map(
		            color(Color.BLACK).alpha(), color(Color.BLACK).alpha(), ImmutableMap.of(
		              Color.RED, Color.GREEN,
		              Color.YELLOW, Color.BLUE)))
		          // The key and value entries can be different, here we have
		          //   strings with suggestions mapping to items
		          .add("str_suggest_map", caption(bool(true), map(
		            string("").suggest("Have", "Some", "Nice", "Suggestions"),
		            item(Items.APPLE).from(Items.APPLE, Items.CARROT, Items.POTATO, Items.BEETROOT),
		            ImmutableMap.of("lol apple", Items.APPLE))))
		          // Also, map entries may be linked, preserving their order.
		          // In the config file this translates to adding an ordinal prefix
		          //   to the NBT keys
		          .add("int_to_int_map", map(
		            number(0).min(0).max(1024),
		            number(0).min(0).max(2048),
		            ImmutableMap.of(0, 1, 1, 2, 2, 4, 3, 8)
		          ).linked())
		          // If you need duplicates in a map, you may use a pair-list
		          //   Remember to not push the depth too far or the config will stop being simple
		          //   A datapack or a custom json storage may be more suitable for storing
		          //   complex data, since list and map entries must be all the same type.
		          .add("instruction_list", pairList(
						string("move").restrict("move", "rotate", "dig", "tower"),
						number(1), asList(
						  Pair.of("move", 2), Pair.of("tower", 1))))
		          // Keybinds are also supported, but you should consider using regular
		          //   Keybinds instead, as they would appear in the Controls screen,
		          //   where the user can fix conflicts between mods
		          // To actually use keybinds from configs, see ExtendedKeyBind and
		          //   ExtendedKeyBindProvider
		          .add("key_map", map(
						key(), string("/execute ..."), ImmutableMap.of(
			           parse("left.control+h"), "/tp 0 0 0",
						  parse("left.ctrl+i"), "/effect give @s minecraft:invisibility 999999 255 true")))
		          // Of course, maps also support bean entries, which are mostly intended to be
		          //   used this way (or in lists).
		          // In this case, our bean doesn't allow users to edit their name property,
		          //   since it's inferred by the map key (not an encouraged idea, just for demo)
		          // By default, bean entries fail at load time if you don't define entries for
		          //   all their properties, since it's an easy mistake. You may suppress this error
		          //   by calling .allowUneditableProperties() on them.
		          //   Uneditable properties will default to their value within the default value
		          //   of the bean entry.
		          .add("bean_map", map(
						string("<unnamed>"), bean(new BeanDemo())
			           .caption("range", range(FloatRange.inclusive(0F, 1F)))
			           .add("color", color(Color.WHITE))
			           .add("pattern", pattern("(\\d++):(\\w+)"))
			           .withIcon(BeanDemo::getIcon)
			           .allowUneditableProperties(), Util.make(
							 new HashMap<>(), m -> m.put("<unnamed>", new BeanDemo())))))
		     .n(group("pairs_n_triples", false)
		          .add("int_pair", pair(number(0, 0, 10), number(10, 0, 10)))
		          .add("slider_pair", pair(number(0.5F, 0F, 1F).slider(), volume(0.5F)))
		          .add("enum_triple", triple(
		            option(Axis.X), option(Placement.UPSIDE_DOWN), option(Direction.NORTH)))
		          .add("item_fluid_pair_list", caption(pair(
						itemName(new ResourceLocation("apple")), fluidName(new ResourceLocation("water"))),
		               list(pair(
							  itemName(new ResourceLocation("apple")), fluidName(new ResourceLocation("water"))
		               ), asList(
							  Pair.of(new ResourceLocation("apple"), new ResourceLocation("water")),
							  Pair.of(new ResourceLocation("beetroot"), new ResourceLocation("lava"))))))
		          .add("mixed_pair", pair(number(0), string("str")))
		          // Pairs can also be used to create ranges, but remember that there is already a
		          //   range entry for this purpose, which supports configurable exclusive bounds
		          .add("range_pair", pair(number(0), number(10))
		            .guiError(p -> p != null && p.getLeft() != null && p.getRight() != null && p.getLeft() > p.getRight()?
		                           Optional.of(ttc("error.min_greater_than_max")) : Optional.empty())
		            .withMiddleIcon(SimpleConfigIcons.Entries.LESS_EQUAL))
		          .add("pair_pair", pair(
						pair(number(0), number(0L)),
						pair(number(0F), number(0D)))))
		     // As lists and maps, groups can also hold entries in their captions
		     //   The held entries belong to the group and have their own entry
		     //   within it in the config file.
		     // By default, the held entry is named '$root', but a different
		     //   name may be specified
		     .n(group("special", false)
		          // Groups can hold entries in their captions
		          // To set a caption entry, use .caption() instead of .add()
		          // Caption entries have an entry under the group in the config file,
		          //   as any other entry of the group
		          // Only a single caption entry can be set per group
		          .caption("caption", string("Caption entry")
		            .suggest("Groups", "can", "hold", "entries", "in", "their", "captions"))
		          // Text entries can also receive format arguments if they
		          //   are defined by name instead of passing a Component
		          .text("non_persistent_desc", ttc(prefix("text.non_persistent")).withStyle(ChatFormatting.GRAY))
		          // Any entry can be made non-persistent, by calling .temp()
		          //   Non-persistent entries do not appear in the config file
		          //   and are reset on every restart
		          // They are rarely useful for enabling special profiling
		          //   or debugging features
		          .add("non_persistent_bool", bool(false).temp())
		          // Additionally, entries can also be ignored, ignoring any
		          //   modifications in the GUI. These can be useful for
		          //   interactive entries that modify other entries
		          //   but don't have a relevant value to be stored.
		          .add("ignored_bool", bool(false).ignored())
		          // Button entries can execute any arbitrary action
		          .add("action_button", button(
		            () -> Util.getPlatform().openFile(
		              Minecraft.getInstance().gameDirectory.toPath().resolve("options.txt").toFile())
		          ).label("chat.file.open"))
		          // A button may be added to any entry type
		          //   However, this makes the entry not persistent
		          //   This may be useful to modify other entries
		          // It's also useful to create config presets, for which a
		          //   specific builder exists as shown below
		          .add("button_entry", button(
		            number(2F).error(
		              n -> abs(n) > 10? Optional.of(Component.literal("> 10")) :
		                   Optional.empty()
		            ), s -> {
		            	final String path = "demo.entries.special.button_test";
		            	CLIENT_CONFIG.setGUI(path, CLIENT_CONFIG.<List<Integer>>getGUI(path)
			              .stream().map(i -> (int) (s * i)).collect(Collectors.toList()));
		            }))
		          // This is a test entry modified by the above button entry
		          .add("button_test", list(
		            number(0), 0, 1, 2, 3, 4, 5, 6, 7))
		          // You may create config presets, either locally or globally with
		          //   localPresetSwitcher() and globalPresetSwitcher() respectively
		          // Local presets use paths relative to the current scope
		          //   while global presets use paths relative to the whole config
		          //   Global presets cannot affect other configs however, i.e.
		          //   a client config preset cannot modify server config entries
		          // The presets are simply a map of paths to values, and a preset
		          //   entry takes a map of preset names to such maps
		          // The preset names are mapped to translation keys under
		          //   the key of the preset entry
		          // Invalid keys in a preset, or with a wrong type will
		          //   be skipped, logging an error
		          .add("preset_switcher", localPresetSwitcher(
		            presets(
		              preset("even")
		                .add("preset_test.a", 0)
		                .add("preset_test.b", 2)
		                .add("preset_test.c", 4)
		                .add("preset_test.d", "\"6\"")
		                .add("preset_test.e", asList(8, 10, 12)),
						  // We may also extract common path parts using .n()
		              preset("odd")
		                .n(preset("preset_test")
		                     .add("a", 1)
		                     .add("b", 3)
		                     .add("c", 5)
		                     .add("d", "\"7\"")
		                     .add("e", asList(9, 11, 13))),
		              preset("fibonacci")
		                .n(preset("preset_test")
		                     .add("a", 1)
		                     .add("b", 1)
		                     .add("c", 2)
		                     .add("d", "\"3\"")
		                     .add("e", asList(5, 8, 13)))
		              // Or in this case, we could have passed "preset_test" as the path here
		            ), ""))
		          // The following entries are used in the presets above
		          .n(group("preset_test")
		               .add("a", number(0))
		               .add("b", number(0))
		               .add("c", number(0))
		               .add("d", string("\"0\""))
		               .add("e", list(number(0), 0, 0, 0)))))
		  .n(group("errors_tooltips_n_links")
		       // You may add links or contextual tooltips in text entries
		       //   by calling .modifyStyle() on text components
		       // Format arguments may be suppliers, which will be evaluated before
		       //   being filled in.
		       .text("open_file", (Supplier<Component>) () ->
			      ttc(prefix("text.mod_config_file")).withStyle(style -> style
				     .withColor(ChatFormatting.DARK_AQUA)
				     .withClickEvent(new ClickEvent(
				       ClickEvent.Action.OPEN_FILE,
				       CLIENT_CONFIG.getFilePath().map(Path::toString).orElse("")))
				     .withHoverEvent(new HoverEvent(
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
			      .tooltip(s -> asList(
				     ttc(prefix("text.hello"), stc(s).withStyle(ChatFormatting.DARK_AQUA)))))
		       // Any value can be marked as requiring a restart to be effective
		       //   Entire groups and categories can be marked as well
		       .add("restart_bool", bool(false).restart())
		       // Entries may be marked as experimental
		       .add("experimental_bool", bool(false).experimental())
		       // Entries may be marked as advanced using a built-in tag
		       //   You may also create your own tags.
		       //   The states of requiring restart, being experimental or not being persistent are
		       //   displayed using tags as well.
		       // Custom tags may define a click action and a tooltip, besides an icon.
		       .add("advanced_bool", bool(false).withTags(EntryTag.ADVANCED))
		       // The order in which tags are specified is irrelevant, since the tags themselves
		       //   define an ordering. An entry may have an arbitrary amount of tags
		       .add("mixed_bool", bool(false).experimental().restart().withTags(
					EntryTag.ADVANCED, EntryTag.coloredTag(ChatFormatting.RED),
					EntryTag.coloredTag(ChatFormatting.GREEN),
					EntryTag.coloredTag(ChatFormatting.BLUE),
					EntryTag.coloredBookmark(ChatFormatting.YELLOW)))
		       .add("enable_switch", bool(false))
		       .add("enable_test", string("text").editable(g -> g.getGUIBoolean("enable_switch"))))
		  .text("end", Component.translatable("simpleconfig.text.wiki")
		    .withStyle(style -> style.withColor(ChatFormatting.AQUA)
		      .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/endorh/simple-config/wiki"))))
		  // Finally, we may manually set a baker method
		  //   for this config/category/group
		  // The baker method will receive the built config/category/group
		  //   when called, after all backing fields have been baked
		  // This isn't usually necessary, since if a method named 'bake'
		  //   with the correct signature is found in the backing class,
		  //   it will be set as baker automatically
		  .withBaker(DemoConfigCategory::bakeDemoCategory);
		// Since here we're only building a category, we don't need
		//   to finish the call by calling .buildAndRegister()
		// This is done in the SimpleConfigMod class
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
	public enum Placement implements TranslatedEnum {
		UPRIGHT, UPSIDE_DOWN;
		
		// Although in this case it doesn't make much difference
		@Override public Component getDisplayName() {
			return ttc(prefix("enum.placement." + name().toLowerCase()));
		}
	}
	
	public static class BeanDemo {
		private static final Map<Color, Icon> ICON_CACHE = new LinkedHashMap<>(51) {
			@Override protected boolean removeEldestEntry(Entry eldest) {
				return size() > 50;
			}
		};
		
		private String name = "<unnamed>";
		private FloatRange range = FloatRange.inclusive(0F, 1F);
		private Color color = Color.WHITE;
		private Pattern pattern = Pattern.compile("(\\d+):(\\w+)");
		
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		
		public FloatRange getRange() {
			return range;
		}
		public void setRange(FloatRange range) {
			this.range = range;
		}
		
		public Color getColor() {
			return color;
		}
		public void setColor(Color color) {
			this.color = color;
		}
		
		public Pattern getPattern() {
			return pattern;
		}
		public void setPattern(Pattern pattern) {
			this.pattern = pattern;
		}
		
		public @Transient Icon getIcon() {
			return ICON_CACHE.computeIfAbsent(color, c -> Entries.WRENCH.withTint(getColor().getRGB()));
		}
		
		@Override public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			BeanDemo beanDemo = (BeanDemo) o;
			return name.equals(beanDemo.name) && range.equals(beanDemo.range)
			       && color.equals(beanDemo.color)
			       && pattern.pattern().equals(beanDemo.pattern.pattern());
		}
		
		@Override public int hashCode() {
			return Objects.hash(name, range, color, pattern.pattern());
		}
	}
	
	public static class StringIntPairSerializer implements IConfigEntrySerializer<Pair<String, Integer>> {
		@Override public String serializeConfigEntry(Pair<String, Integer> value) {
			return value.getKey() + ", " + value.getValue();
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
		// If we do not explicitly return Pair.class, the reflection
		//   API will match fields with type ImmutablePair instead
		@Override public @NotNull Class<?> getClass(Pair<String, Integer> value) {
			return Pair.class;
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
			static void bake(SimpleConfigGroupImpl g) {
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
			@Bind public static Pair<Color, List<Pair<Color, List<Color>>>> color_list_list;
		}
		
		@Bind public static class serializable {
			@Bind public static ResourceLocation resource;
			@Bind public static Item item;
			@Bind public static CompoundTag nbt_compound;
			@Bind public static Tag nbt_value;
			@Bind public static Pair<String, Integer> pair;
		}
		
		@Bind public static class maps {
			// As with the lists, the generics of the field types must match
			@Bind public static Map<String, String> map;
			@Bind public static Map<String, List<String>> list_map;
			@Bind public static Pair<Integer, Map<String, Map<String, Integer>>> divisible_int_map_map;
			@Bind public static Map<Integer, Integer> int_to_int_map;
			@Bind public static Map<KeyBindMapping, String> key_map;
		}
		
		@Bind public static class special {
			@Bind public static boolean non_persistent_bool;
			@Bind public static List<Integer> button_test;
			@Bind public static class preset_test {
				@Bind public static int a;
				@Bind public static int b;
				@Bind public static int c;
				@Bind public static String d;
				@Bind public static List<Integer> e;
			}
		}
	}
	
	@Bind public static class errors_tooltips_n_links {
		@Bind public static String lowercase_string;
		@Bind public static int min;
		@Bind public static int max;
		@Bind public static List<Integer> even_int_list;
		@Bind public static String dynamic_tooltip;
	}
}
