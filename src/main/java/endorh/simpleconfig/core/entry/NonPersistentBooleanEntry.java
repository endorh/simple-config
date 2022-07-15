package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.core.ISimpleConfigEntryHolder;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.impl.builders.BooleanToggleBuilder;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.Optional;
import java.util.function.Function;

public class NonPersistentBooleanEntry extends GUIOnlyEntry<Boolean, Boolean, NonPersistentBooleanEntry> {
	protected Function<Boolean, ITextComponent> yesNoSupplier;
	
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
			Builder copy = copy();
			copy.yesNoSupplier = displayAdapter;
			return copy;
		}
		
		@Override
		protected NonPersistentBooleanEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			final NonPersistentBooleanEntry e = new NonPersistentBooleanEntry(parent, name, value);
			e.yesNoSupplier = yesNoSupplier;
			return e;
		}
		
		@Override protected Builder createCopy() {
			final Builder copy = new Builder(value);
			copy.yesNoSupplier = yesNoSupplier;
			return copy;
		}
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
}
