package endorh.simple_config.core.entry;

import endorh.simple_config.core.ISimpleConfigEntryHolder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.BooleanToggleBuilder;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.Optional;
import java.util.function.Function;

public class NonPersistentBooleanEntry extends GUIOnlyEntry<Boolean, Boolean, NonPersistentBooleanEntry> {
	protected Function<Boolean, ITextComponent> yesNoSupplier;
	protected boolean actualValue = value;
	
	@Internal public NonPersistentBooleanEntry(
	  ISimpleConfigEntryHolder parent, String name, Boolean value
	) {
		super(parent, name, value, Boolean.class);
	}
	
	public static class Builder extends GUIOnlyEntry.Builder<Boolean, Boolean, NonPersistentBooleanEntry, Builder> {
		protected Function<Boolean, ITextComponent> yesNoSupplier = null;
		
		public Builder(Boolean value) {
			super(value, Boolean.class);
		}
		
		/**
		 * Set a Yes/No supplier for this entry
		 */
		public Builder displayAs(Function<Boolean, ITextComponent> displayAdapter) {
			yesNoSupplier = displayAdapter;
			return this;
		}
		
		@Override
		protected NonPersistentBooleanEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			final NonPersistentBooleanEntry e = new NonPersistentBooleanEntry(parent, name, value);
			e.yesNoSupplier = yesNoSupplier;
			return e;
		}
	}
	
	@Override
	protected Boolean get() {
		return actualValue;
	}
	
	@Override
	protected void set(Boolean value) {
		actualValue = value;
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	protected Optional<AbstractConfigListEntry<Boolean>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		final BooleanToggleBuilder valBuilder = builder
		  .startBooleanToggle(getDisplayName(), actualValue)
		  .setDefaultValue(value)
		  .setSaveConsumer(saveConsumer())
		  .setYesNoTextSupplier(yesNoSupplier)
		  .setTooltipSupplier(this::supplyTooltip)
		  .setErrorSupplier(this::supplyError);
		return Optional.of(decorate(valBuilder).build());
	}
}
