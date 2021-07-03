package dnj.simple_config.core.entry;

import dnj.simple_config.core.AbstractConfigEntry;
import dnj.simple_config.core.ISimpleConfigEntryHolder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.api.Modifier;
import me.shedaniel.clothconfig2.api.ModifierKeyCode;
import me.shedaniel.clothconfig2.impl.builders.KeyCodeBuilder;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.client.util.InputMappings.Input;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @deprecated Register keybindings through
 * {@link net.minecraftforge.fml.client.registry.ClientRegistry#registerKeyBinding(KeyBinding)}
 */
@OnlyIn(Dist.CLIENT)
@Deprecated public class KeyBindEntry
  extends AbstractConfigEntry<ModifierKeyCode, String, ModifierKeyCode, KeyBindEntry> {
	protected boolean allowKey = true;
	protected boolean allowModifiers = true;
	protected boolean allowMouse = true;
	protected ModifierKeyCode defaultModifier = null;
	
	public KeyBindEntry() {
		this(InputMappings.INPUT_INVALID);
	}
	public KeyBindEntry(Input input) {
		this(ModifierKeyCode.of(input, Modifier.of(false, false, false)));
	}
	public KeyBindEntry(ModifierKeyCode value) {
		super(value, ModifierKeyCode.class);
	}
	
	protected static final Pattern KEY_PATTERN = Pattern.compile(
	  "(?i)\\s*(?:(\\w+)\\s*\\+\\s*)?(?:(\\w+)\\s*\\+\\s*)?(?:(\\w+)\\s*\\+\\s*)?(.*)");
	public static @Nullable ModifierKeyCode parseKey(@Nullable String key) {
		if (key == null)
			return null;
		final Matcher m = KEY_PATTERN.matcher(key.toLowerCase());
		if (m.matches()) {
			byte mods = 0;
			mods |= guessModifier(m.group(1));
			mods |= guessModifier(m.group(2));
			mods |= guessModifier(m.group(3));
			final Modifier mod = Modifier.of((mods & 2) != 0, (mods & 1) != 0, (mods & 4) != 0);
			try {
				final Input in = InputMappings.getInputByName(m.group(4));
				return ModifierKeyCode.of(in, mod);
			} catch (IllegalArgumentException ignored) {
				return null;
			}
		} else return null;
	}
	
	protected static byte guessModifier(@Nullable String key) {
		if (key == null || key.isEmpty())
			return 0;
		switch (key.toLowerCase().charAt(0)) {
			case 'c': return 1;
			case 'a': return 2;
			case 'm': // Spanish and the like
			case 's': return 4;
			default: return 0;
		}
	}
	
	public static String keyToString(ModifierKeyCode key) {
		if (key == null)
			return "";
		StringBuilder mod = new StringBuilder();
		if (key.getModifier().hasControl())
			mod.append("ctrl+");
		if (key.getModifier().hasAlt())
			mod.append("alt+");
		if (key.getModifier().hasShift())
			mod.append("shift+");
		return mod.append(key.getKeyCode().getTranslationKey()).toString();
	}
	
	@Override
	protected String forConfig(ModifierKeyCode value) {
		return keyToString(value);
	}
	
	@Nullable
	@Override
	protected ModifierKeyCode fromConfig(@Nullable String value) {
		return parseKey(value);
	}
	
	public KeyBindEntry modifier(boolean allowModifiers) {
		this.allowModifiers = allowModifiers;
		return this;
	}
	public KeyBindEntry noModifiers() {
		return modifier(false);
	}
	public KeyBindEntry keyboard(boolean allowKeys) {
		this.allowKey = allowKeys;
		return this;
	}
	public KeyBindEntry noKeyboard() {
		return keyboard(false);
	}
	public KeyBindEntry mouse(boolean allowMouse) {
		this.allowMouse = allowMouse;
		return this;
	}
	public KeyBindEntry noMouse() {
		return mouse(false);
	}
	
	@Override
	protected Optional<ConfigValue<?>> buildConfigEntry(Builder builder) {
		return Optional.of(decorate(builder).define(name, forConfig(value), o ->
		  o instanceof String && fromConfig((String) o) != null
		  && !this.supplyError(forGui(fromConfig((String) o))).isPresent()));
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	protected Optional<AbstractConfigListEntry<ModifierKeyCode>> buildGUIEntry(
	  ConfigEntryBuilder builder, ISimpleConfigEntryHolder c
	) {
		final KeyCodeBuilder valBuilder = builder
		  .startModifierKeyCodeField(getDisplayName(), forGui(c.get(name)))
		  .setModifierDefaultValue(() -> forGui(defaultModifier))
		  .setAllowKey(allowKey)
		  .setAllowModifiers(allowModifiers)
		  .setAllowMouse(allowMouse)
		  .setModifierSaveConsumer(saveConsumer(c))
		  .setModifierTooltipSupplier(this::supplyTooltip)
		  .setModifierErrorSupplier(this::supplyError);
		return Optional.of(decorate(valBuilder).build());
	}
}
