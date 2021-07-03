package dnj.simple_config.core.entry;

import dnj.simple_config.core.ISimpleConfigEntryHolder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.ColorFieldBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AlphaColorEntry extends ColorEntry {
	public AlphaColorEntry(Color value) {
		super(value);
	}
	
	@Override
	protected String forConfig(Color value) {
		return String.format("#%08X", value.getRGB());
	}
	
	protected static final Pattern ALPHA_COLOR_PATTERN = Pattern.compile(
	  "\\s*(?:0x|#)(?i)(?<color>[0-9a-f]{3,4}|[0-9a-f]{6}|[0-9a-f]{8})\\s*");
	
	@Override
	protected @Nullable
	Color fromConfig(@Nullable String value) {
		if (value == null)
			return null;
		final Matcher m = ALPHA_COLOR_PATTERN.matcher(value);
		if (m.matches()) {
			String c = m.group("color");
			if (c.length() < 6)
				c = doubleChars(c);
			return new Color((int) Long.parseLong(c.toLowerCase(), 0x10), true);
		}
		return null;
	}
	
	@Override
	protected Integer forGui(Color value) {
		return value.getRGB();
	}
	
	@Override
	protected Color fromGui(@Nullable Integer value) {
		return value != null ? new Color(value, true) : null;
	}
	
	@Override
	protected Optional<ConfigValue<?>> buildConfigEntry(Builder builder) {
		return Optional.of(decorate(builder).define(name, forConfig(value), configValidator()));
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	protected Optional<AbstractConfigListEntry<Integer>> buildGUIEntry(
	  ConfigEntryBuilder builder, ISimpleConfigEntryHolder c
	) {
		final int prev = c.<Color>get(name).getRGB();
		final ColorFieldBuilder valBuilder = builder
		  .startAlphaColorField(getDisplayName(), prev)
		  .setDefaultValue(forGui(value))
		  .setSaveConsumer(saveConsumer(c))
		  .setTooltipSupplier(this::supplyTooltip)
		  .setErrorSupplier(this::supplyError);
		return Optional.of(decorate(valBuilder).build());
	}
}
