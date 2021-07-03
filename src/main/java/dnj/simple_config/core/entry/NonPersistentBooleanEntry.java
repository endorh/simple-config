package dnj.simple_config.core.entry;

import dnj.simple_config.core.ISimpleConfigEntryHolder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.BooleanToggleBuilder;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Optional;
import java.util.function.Function;

public class NonPersistentBooleanEntry extends GUIOnlyEntry<Boolean, Boolean, NonPersistentBooleanEntry> {
	protected Function<Boolean, ITextComponent> yesNoSupplier = null;
	protected boolean actualValue = value;
	
	public NonPersistentBooleanEntry(Boolean value) {
		super(value, Boolean.class);
	}
	
	@Override
	protected Boolean get() {
		return actualValue;
	}
	
	@Override
	protected void set(Boolean value) {
		actualValue = value;
	}
	
	/**
	 * Set a Yes/No supplier for this entry
	 */
	public NonPersistentBooleanEntry displayAs(
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
		  .startBooleanToggle(getDisplayName(), actualValue)
		  .setDefaultValue(value)
		  .setSaveConsumer(saveConsumer(c))
		  .setYesNoTextSupplier(yesNoSupplier)
		  .setTooltipSupplier(this::supplyTooltip)
		  .setErrorSupplier(this::supplyError);
		return Optional.of(decorate(valBuilder).build());
	}
}
