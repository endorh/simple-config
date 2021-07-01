package dnj.simple_config.core.entry;

import dnj.simple_config.core.AbstractConfigEntry;
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

public class ColorEntry extends AbstractConfigEntry<Color, String, Integer, ColorEntry> {
	public ColorEntry(Color value) {
		super(value);
	}
	
	@Override
	protected String forConfig(Color value) {
		return String.format("#%06X", value.getRGB() & 0xFFFFFF);
	}
	
	protected static final Pattern COLOR_PATTERN = Pattern.compile(
	  "\\s*(?:0x|#)(?i)(?<color>[0-9a-f]{3}|[0-9a-f]{6})\\s*");
	
	@Override
	protected @Nullable
	Color fromConfig(String value) {
		if (value == null)
			return null;
		final Matcher m = COLOR_PATTERN.matcher(value);
		if (m.matches()) {
			String c = m.group("color");
			if (c.length() == 3)
				c = doubleChars(c);
			return new Color((int) Long.parseLong(c.toLowerCase(), 0x10));
		}
		return null;
	}
	
	@Override
	protected Integer forGui(Color value) {
		return value.getRGB() & 0xFFFFFF;
	}
	
	@Override
	protected @Nullable
	Color fromGui(@Nullable Integer value) {
		return value != null ? new Color(value) : null;
	}
	
	protected static String doubleChars(String s) {
		StringBuilder r = new StringBuilder();
		for (char ch : s.toCharArray())
			r.append(ch);
		return r.toString();
	}
	
	@Override
	protected Optional<ConfigValue<?>> buildConfigEntry(Builder builder) {
		return Optional.of(decorate(builder).define(
		  name, forConfig(value), s -> s instanceof String && fromConfig((String) s) != null));
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	protected Optional<AbstractConfigListEntry<?>> buildGUIEntry(
	  ConfigEntryBuilder builder, ISimpleConfigEntryHolder c
	) {
		final ColorFieldBuilder valBuilder = builder
		  .startColorField(getDisplayName(), forGui(c.get(name)))
		  .setDefaultValue(forGui(value))
		  .setSaveConsumer(saveConsumer(c))
		  .setTooltipSupplier(this::supplyTooltip)
		  .setErrorSupplier(this::supplyError);
		return Optional.of(decorate(valBuilder).build());
	}
}
