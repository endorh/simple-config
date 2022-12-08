package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.ColorEntryBuilder;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.AbstractConfigEntryBuilder;
import endorh.simpleconfig.core.AtomicEntry;
import endorh.simpleconfig.core.EntryType;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.ColorFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.awt.Color;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorEntry extends AbstractConfigEntry<Color, String, Integer>
  implements AtomicEntry<Integer> {
	protected final boolean alpha;
	@Internal public ColorEntry(
	  ConfigEntryHolder parent, String name, Color value, boolean alpha
	) {
		super(parent, name, value);
		this.alpha = alpha;
	}
	
	public static class Builder extends AbstractConfigEntryBuilder<Color, String, Integer, ColorEntry, ColorEntryBuilder, Builder>
	  implements ColorEntryBuilder {
		protected boolean alpha;
		
		public Builder(Color value) {
			super(value, EntryType.of(Color.class));
		}
		
		@Override @Contract(pure=true) public @NotNull Builder alpha() {
			return alpha(true);
		}
		
		@Override @Contract(pure=true) public @NotNull Builder alpha(boolean hasAlpha) {
			Builder copy = copy();
			copy.alpha = hasAlpha;
			return copy;
		}
		
		@Override
		protected ColorEntry buildEntry(ConfigEntryHolder parent, String name) {
			return new ColorEntry(parent, name, value, alpha);
		}
		
		@Override protected Builder createCopy(Color value) {
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
	  "\\s*(?:0x|#)(?i)(?<color>[\\da-f]{3,4}|[\\da-f]{6}|[\\da-f]{8})\\s*");
	
	@Override public @Nullable Color fromConfig(String value) {
		if (value == null) return null;
		final Matcher m = COLOR_PATTERN.matcher(value);
		if (m.matches()) {
			String c = m.group("color");
			if (c.length() < 6) c = doubleChars(c);
			int argb = (int) Long.parseLong(c.toLowerCase(), 0x10);
			if (!alpha) argb = argb & 0xFFFFFF;
			return new Color(argb, alpha);
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
	
	protected String getFormatDescriptor() {
		return alpha? "#AARRGGBB" : "#RRGGBB";
	}
	
	@Override public List<String> getConfigCommentTooltips() {
		List<String> tooltips = super.getConfigCommentTooltips();
		tooltips.add("Color: " + getFormatDescriptor());
		return tooltips;
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override public Optional<FieldBuilder<Integer, ?, ?>> buildGUIEntry(
	  ConfigFieldBuilder builder
	) {
		ColorFieldBuilder valBuilder = builder
		  .startAlphaColorField(getDisplayName(), forGui(get()))
		  .setAlphaMode(alpha);
		return Optional.of(decorate(valBuilder));
	}
	
}
