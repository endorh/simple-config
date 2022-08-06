package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.AbstractConfigEntryBuilder;
import endorh.simpleconfig.core.IKeyEntry;
import endorh.simpleconfig.core.ISimpleConfigEntryHolder;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.impl.builders.BooleanToggleBuilder;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class BooleanEntry
  extends AbstractConfigEntry<Boolean, Boolean, Boolean, BooleanEntry> implements IKeyEntry<Boolean> {
	protected BooleanDisplayer yesNoSupplier = BooleanDisplayer.TRUE_FALSE;
	
	@Internal public BooleanEntry(ISimpleConfigEntryHolder parent, String name, boolean value) {
		super(parent, name, value);
	}
	
	public static class Builder extends AbstractConfigEntryBuilder<Boolean, Boolean, Boolean,
	  BooleanEntry, Builder> {
		protected BooleanDisplayer yesNoSupplier = BooleanDisplayer.TRUE_FALSE;
		
		public Builder(Boolean value) {
			super(value, Boolean.class);
		}
		
		/**
		 * Change the text displayed in the entry's button<br>
		 * You may also pass a translation key to which '.true' and '.false'
		 * would be appended if you use {@link Builder#text(String)}
		 */
		@Contract(pure=true) public Builder text(BooleanDisplayer displayAdapter) {
			Builder copy = copy();
			String trueS = displayAdapter.getSerializableName(true).trim().toLowerCase();
			String falseS = displayAdapter.getSerializableName(false).trim().toLowerCase();
			if (trueS.equals(falseS)) throw new IllegalArgumentException(
			  "Illegal boolean displayer: Serializable names must differ in lowercase after trimming");
			copy.yesNoSupplier = displayAdapter;
			return copy;
		}
		
		/**
		 * Change the text displayed in the entry's button<br>
		 * This method takes a translation key to which '.true' and '.false'
		 * are appended to retrieve the actual text that will be used.
		 * You may also provide your own logic using {@link Builder#text(BooleanDisplayer)}
		 */
		@Contract(pure=true) public Builder text(String translation) {
			Builder copy = copy();
			final TranslationTextComponent yes =
			  new TranslationTextComponent(translation + ".true");
			final TranslationTextComponent no =
			  new TranslationTextComponent(translation + ".false");
			copy.yesNoSupplier = b -> b? yes : no;
			return copy;
		}
		
		@Override
		protected BooleanEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			final BooleanEntry e = new BooleanEntry(parent, name, value);
			e.yesNoSupplier = yesNoSupplier;
			return e;
		}
		
		@Override protected Builder createCopy() {
			final Builder copy = new Builder(value);
			copy.yesNoSupplier = yesNoSupplier;
			return copy;
		}
	}
	
	protected String[] getLabels() {
		if (yesNoSupplier != null) {
			String trueS = yesNoSupplier.getSerializableName(true);
			String falseS = yesNoSupplier.getSerializableName(false);
			if (!trueS.equals(falseS))
				return new String[] { trueS, falseS };
		}
		return new String[] { "true", "false" };
	}
	
	@Override public Object forActualConfig(@Nullable Boolean value) {
		if (value == null) return null;
		String[] labels = getLabels();
		if ("true".equals(labels[0]) && "false".equals(labels[1])) return value;
		return labels[value? 0 : 1];
	}
	
	private static final Pattern TRUE_PATTERN = Pattern.compile(
	  "^\\s*+(?i:true|on|enabled?|0*+1(?:\\.0*+)?|0x0*+1)\\s*+$");
	private static final Pattern FALSE_PATTERN = Pattern.compile(
	  "^\\s*+(?i:false|off|disabled?|0++(?:\\.0*+)?|0x0++)\\s*+$");
	@Override public @Nullable Boolean fromActualConfig(@Nullable Object value) {
		if (value == null) return null;
		if (value instanceof Boolean) return (Boolean) value;
		if (value instanceof String) {
			final String s = ((String) value).trim().toLowerCase();
			String[] labels = getLabels();
			if (labels[0].equals(s)) return true;
			if (labels[1].equals(s)) return false;
			if (labels[0].startsWith(s) && !labels[1].startsWith(s)) return true;
			if (labels[1].startsWith(s) && !labels[0].startsWith(s)) return false;
			
			// Fallback
			if (TRUE_PATTERN.matcher(s).matches()) return true;
			if (FALSE_PATTERN.matcher(s).matches()) return false;
		}
		// More fallback
		if (value instanceof Number) return ((Number) value).doubleValue() != 0;
		return null;
	}
	
	@Override public List<String> getConfigCommentTooltips() {
		List<String> tooltips = super.getConfigCommentTooltips();
		String[] labels = getLabels();
		tooltips.add(labels[0] + "/" + labels[1]);
		return tooltips;
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override public Optional<AbstractConfigListEntry<Boolean>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		final BooleanToggleBuilder valBuilder = builder
		  .startBooleanToggle(getDisplayName(), get())
		  .setYesNoTextSupplier(yesNoSupplier::getDisplayName);
		return Optional.of(decorate(valBuilder).build());
	}
	
	/**
	 * Determines how a boolean value is displayed, both in the interface and in the config file
	 */
	public interface BooleanDisplayer {
		static BooleanDisplayer forTranslation(String translation) {
			return forTranslation(translation, "true", "false");
		}
		
		static BooleanDisplayer forTranslation(
		  String translation, String serialTrue, String serialFalse
		) {
			return new BooleanDisplayer() {
				@Override public ITextComponent getDisplayName(boolean value) {
					return new TranslationTextComponent(translation + (value? ".true" : ".false"))
					  .mergeStyle(value? TextFormatting.GREEN : TextFormatting.RED);
				}
				@Override public String getSerializableName(boolean value) {
					return value? serialTrue : serialFalse;
				}
			};
		}
		
		BooleanDisplayer TRUE_FALSE = forTranslation("simpleconfig.format.bool.true_false");
		BooleanDisplayer YES_NO = forTranslation(
		  "simpleconfig.format.bool.yes_no", "yes", "no");
		BooleanDisplayer ENABLED_DISABLED = forTranslation(
		  "simpleconfig.format.bool.enabled_disabled", "enabled", "disabled");
		BooleanDisplayer ON_OFF = forTranslation(
		  "simpleconfig.format.bool.on_off", "on", "off");
		
		/**
		 * Get display name.<br>
		 * Can be formatted.
		 */
		ITextComponent getDisplayName(boolean value);
		
		/**
		 * Get serializable name for the config file.<br>
		 * <b>Must be a static, non-translated value.</b><br>
		 * Values for {@code true} and {@code false} must differ in lowercase after trimming.
		 */
		default String getSerializableName(boolean value) {
			return String.valueOf(value);
		}
	}
}
