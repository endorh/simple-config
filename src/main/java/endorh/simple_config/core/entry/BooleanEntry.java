package endorh.simple_config.core.entry;

import endorh.simple_config.core.AbstractConfigEntry;
import endorh.simple_config.core.AbstractConfigEntryBuilder;
import endorh.simple_config.core.ISimpleConfigEntryHolder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.BooleanToggleBuilder;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.Optional;
import java.util.function.Function;

public class BooleanEntry
  extends AbstractConfigEntry<Boolean, Boolean, Boolean, BooleanEntry> {
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
			this.yesNoSupplier = displayAdapter;
			return this;
		}
		
		@Override
		protected BooleanEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			final BooleanEntry e = new BooleanEntry(parent, name, value);
			e.yesNoSupplier = yesNoSupplier;
			return e;
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
		  .setDefaultValue(value)
		  .setSaveConsumer(saveConsumer())
		  .setYesNoTextSupplier(yesNoSupplier)
		  .setTooltipSupplier(this::supplyTooltip)
		  .setErrorSupplier(this::supplyError);
		return Optional.of(decorate(valBuilder).build());
	}
}
