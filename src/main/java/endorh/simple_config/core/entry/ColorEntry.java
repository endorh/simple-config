package endorh.simple_config.core.entry;

import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.ConfigEntryBuilder;
import endorh.simple_config.clothconfig2.impl.builders.ColorFieldBuilder;
import endorh.simple_config.core.AbstractConfigEntry;
import endorh.simple_config.core.AbstractConfigEntryBuilder;
import endorh.simple_config.core.IKeyEntry;
import endorh.simple_config.core.ISimpleConfigEntryHolder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorEntry extends AbstractConfigEntry<Color, String, Integer, ColorEntry>
  implements IKeyEntry<String, Integer> {
	protected final boolean alpha;
	@Internal public ColorEntry(
	  ISimpleConfigEntryHolder parent, String name, Color value, boolean alpha
	) {
		super(parent, name, value);
		this.alpha = alpha;
	}
	
	public static class Builder extends AbstractConfigEntryBuilder<Color, String, Integer, ColorEntry, Builder> {
		protected boolean alpha;
		
		public Builder(Color value) {
			super(value, Color.class);
		}
		
		public Builder alpha() {
			return alpha(true);
		}
		
		public Builder alpha(boolean hasAlpha) {
			Builder copy = copy();
			copy.alpha = hasAlpha;
			return copy;
		}
		
		@Override
		protected ColorEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			return new ColorEntry(parent, name, value, alpha);
		}
		
		@Override protected Builder createCopy() {
			final Builder copy = new Builder(value);
			copy.alpha = alpha;
			return copy;
		}
	}
	
	@Override public String forConfig(Color value) {
		return alpha? String.format("#%08X", value.getRGB()) :
		       String.format("#%06X", value.getRGB() & 0xFFFFFF);
	}
	
	protected static final Pattern COLOR_PATTERN = Pattern.compile(
	  "\\s*(?:0x|#)(?i)(?<color>[0-9a-f]{3}|[0-9a-f]{6})\\s*");
	protected static final Pattern ALPHA_COLOR_PATTERN = Pattern.compile(
	  "\\s*(?:0x|#)(?i)(?<color>[0-9a-f]{3,4}|[0-9a-f]{6}|[0-9a-f]{8})\\s*");
	
	@Override
	@Nullable public Color fromConfig(String value) {
		if (value == null)
			return null;
		final Matcher m = (alpha? ALPHA_COLOR_PATTERN : COLOR_PATTERN).matcher(value);
		if (m.matches()) {
			String c = m.group("color");
			if (c.length() < 6)
				c = doubleChars(c);
			return new Color((int) Long.parseLong(c.toLowerCase(), 0x10), alpha);
		}
		return null;
	}
	
	@Override
	public Integer forGui(Color value) {
		return alpha? value.getRGB() : value.getRGB() & 0xFFFFFF;
	}
	
	@Override
	@Nullable public Color fromGui(@Nullable Integer value) {
		return value != null ? new Color(value, alpha) : null;
	}
	
	protected static String doubleChars(String s) {
		StringBuilder r = new StringBuilder();
		for (char ch : s.toCharArray())
			r.append(ch);
		return r.toString();
	}
	
	@Override
	protected Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
		return Optional.of(decorate(builder).define(name, forConfig(value), configValidator()));
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public Optional<AbstractConfigListEntry<Integer>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		ColorFieldBuilder valBuilder = builder
		  .startAlphaColorField(getDisplayName(), forGui(get()))
		  .setAlphaMode(alpha);
		return Optional.of(decorate(valBuilder).build());
	}
	
	@Override public Optional<String> deserializeStringKey(
	  @NotNull String key
	) {
		return Optional.of(key);
	}
}
