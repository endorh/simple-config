package endorh.simpleconfig.core.entry;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.BooleanEntryBuilder;
import endorh.simpleconfig.api.entry.BooleanEntryBuilder.BooleanDisplayer;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.AbstractConfigEntryBuilder;
import endorh.simpleconfig.core.AtomicEntry;
import endorh.simpleconfig.core.EntryType;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.BooleanToggleBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class BooleanEntry
  extends AbstractConfigEntry<Boolean, Boolean, Boolean> implements AtomicEntry<Boolean> {
	protected BooleanDisplayer yesNoSupplier = BooleanEntryBuilder.BooleanDisplayer.TRUE_FALSE;
	
	@Internal public BooleanEntry(ConfigEntryHolder parent, String name, boolean value) {
		super(parent, name, value);
	}
	
	public static class Builder extends AbstractConfigEntryBuilder<
	  Boolean, Boolean, Boolean, BooleanEntry, BooleanEntryBuilder, Builder
	> implements BooleanEntryBuilder {
		protected BooleanDisplayer yesNoSupplier = BooleanDisplayer.TRUE_FALSE;
		
		public Builder(Boolean value) {
			super(value, EntryType.of(Boolean.class));
		}
		
		@Override @Contract(pure=true) public @NotNull Builder text(BooleanDisplayer displayAdapter) {
			Builder copy = copy();
			String trueS = displayAdapter.getSerializableName(true).trim().toLowerCase();
			String falseS = displayAdapter.getSerializableName(false).trim().toLowerCase();
			if (trueS.equals(falseS)) throw new IllegalArgumentException(
			  "Illegal boolean displayer: Serializable names must differ in lowercase after trimming");
			copy.yesNoSupplier = displayAdapter;
			return copy;
		}
		
		@Override @Contract(pure=true) public @NotNull Builder text(String translation) {
			Builder copy = copy();
			final TranslatableComponent yes =
			  new TranslatableComponent(translation + ".true");
			final TranslatableComponent no =
			  new TranslatableComponent(translation + ".false");
			copy.yesNoSupplier = b -> b? yes : no;
			return copy;
		}
		
		@Override
		protected BooleanEntry buildEntry(ConfigEntryHolder parent, String name) {
			final BooleanEntry e = new BooleanEntry(parent, name, value);
			e.yesNoSupplier = yesNoSupplier;
			return e;
		}
		
		@Override protected Builder createCopy(Boolean value) {
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
	@Override public Optional<FieldBuilder<Boolean, ?, ?>> buildGUIEntry(
	  ConfigFieldBuilder builder
	) {
		final BooleanToggleBuilder valBuilder = builder
		  .startBooleanToggle(getDisplayName(), get())
		  .setYesNoTextSupplier(yesNoSupplier::getDisplayName);
		return Optional.of(decorate(valBuilder));
	}
	
	@Override public @Nullable String forCommand(Boolean value) {
		return value == null? null : value? "true" : "false";
	}
	
	@Override public boolean addCommandSuggestions(SuggestionsBuilder builder) {
		boolean current = get();
		Message msg = null;
		if (defValue) {
			msg = new TranslatableComponent("simpleconfig.command.suggest.default");
		} else if (current) msg = new TranslatableComponent("simpleconfig.command.suggest.current");
		if (msg != null) {
			builder.suggest("true", msg);
		} else builder.suggest("true");
		msg = null;
		if (!defValue) {
			msg = new TranslatableComponent("simpleconfig.command.suggest.default");
		} else if (!current) msg = new TranslatableComponent("simpleconfig.command.suggest.current");
		if (msg != null) {
			builder.suggest("false", msg);
		} else builder.suggest("false");
		return true;
	}
}
