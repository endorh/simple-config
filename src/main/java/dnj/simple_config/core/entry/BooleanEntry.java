package dnj.simple_config.core.entry;

import dnj.simple_config.core.AbstractConfigEntry;
import dnj.simple_config.core.ISimpleConfigEntryHolder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.BooleanToggleBuilder;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

import java.util.Optional;
import java.util.function.Function;

public class BooleanEntry
  extends AbstractConfigEntry<Boolean, Boolean, Boolean, BooleanEntry> {
	protected Function<Boolean, ITextComponent> yesNoSupplier = null;
	
	public BooleanEntry(boolean value) {
		super(value, Boolean.class);
	}
	
	@Override
	protected Optional<ConfigValue<?>> buildConfigEntry(Builder builder) {
		return Optional.of(decorate(builder).define(name, value, configValidator()));
	}
	
	/**
	 * Set a Yes/No supplier for this entry
	 */
	public BooleanEntry displayAs(
	  Function<Boolean, ITextComponent> displayAdapter) {
		this.yesNoSupplier = displayAdapter;
		return this;
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	protected Optional<AbstractConfigListEntry<Boolean>> buildGUIEntry(
	  ConfigEntryBuilder builder, ISimpleConfigEntryHolder c
	) {
		final BooleanToggleBuilder valBuilder = builder
		  .startBooleanToggle(getDisplayName(), c.get(name))
		  .setDefaultValue(value)
		  .setSaveConsumer(saveConsumer(c))
		  .setYesNoTextSupplier(yesNoSupplier)
		  .setTooltipSupplier(this::supplyTooltip)
		  .setErrorSupplier(this::supplyError);
		return Optional.of(decorate(valBuilder).build());
	}
}
