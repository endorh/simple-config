package endorh.simple_config.core.entry;

import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.ConfigEntryBuilder;
import endorh.simple_config.clothconfig2.impl.builders.BooleanToggleBuilder;
import endorh.simple_config.core.AbstractConfigEntry;
import endorh.simple_config.core.AbstractConfigEntryBuilder;
import endorh.simple_config.core.IKeyEntry;
import endorh.simple_config.core.ISimpleConfigEntryHolder;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

public class BooleanEntry
  extends AbstractConfigEntry<Boolean, Boolean, Boolean, BooleanEntry> implements
                                                                       IKeyEntry<Boolean, Boolean> {
	protected Function<Boolean, ITextComponent> yesNoSupplier;
	
	@Internal public BooleanEntry(ISimpleConfigEntryHolder parent, String name, boolean value) {
		super(parent, name, value);
	}
	
	public static class Builder extends AbstractConfigEntryBuilder<Boolean, Boolean, Boolean,
	  BooleanEntry, Builder> {
		protected Function<Boolean, ITextComponent> yesNoSupplier = null;
		
		public Builder(Boolean value) {
			super(value, Boolean.class);
		}
		
		public Builder displayAs(Function<Boolean, ITextComponent> displayAdapter) {
			Builder copy = copy();
			copy.yesNoSupplier = displayAdapter;
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
	
	@Override
	protected Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
		return Optional.of(decorate(builder).define(name, value, configValidator()));
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public Optional<AbstractConfigListEntry<Boolean>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		final BooleanToggleBuilder valBuilder = builder
		  .startBooleanToggle(getDisplayName(), get())
		  .setYesNoTextSupplier(yesNoSupplier);
		return Optional.of(decorate(valBuilder).build());
	}
	
	protected static final Pattern truePattern = Pattern.compile(
	  "\\s*+(?:(?i)true|0*+1(?:\\.0*+)?|0x0*+1)\\s*+");
	protected static final Pattern falsePattern = Pattern.compile(
	  "\\s*+(?:(?i)false|0++(?:\\.0*+)?|0x0++)\\s*+");
	@Override public Optional<Boolean> deserializeStringKey(
	  @NotNull String key
	) {
		if (truePattern.matcher(key).matches())
			return Optional.of(true);
		else if (falsePattern.matcher(key).matches())
			return Optional.of(false);
		return Optional.empty();
	}
}
