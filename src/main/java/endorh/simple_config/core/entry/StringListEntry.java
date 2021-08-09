package endorh.simple_config.core.entry;

import endorh.simple_config.core.ISimpleConfigEntryHolder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.StringListBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Optional;

public class StringListEntry extends AbstractListEntry<String, String, String, StringListEntry> {
	public StringListEntry(
	  ISimpleConfigEntryHolder parent, String name, List<String> value) {
		super(parent, name, value);
	}
	
	public static class Builder extends AbstractListEntry.Builder<String, String, String, StringListEntry, Builder> {
		public Builder(List<String> value) {
			super(value);
		}
		
		@Override
		protected StringListEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			return new StringListEntry(parent, name, value);
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public Optional<AbstractConfigListEntry<List<String>>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		final StringListBuilder valBuilder = builder
		  .startStrList(getDisplayName(), get())
		  .setDefaultValue(value)
		  .setExpanded(expand)
		  .setInsertInFront(insertInTop)
		  .setSaveConsumer(saveConsumer())
		  .setTooltipSupplier(this::supplyTooltip)
		  .setErrorSupplier(this::supplyError);
		return Optional.of(decorate(valBuilder).build());
	}
}
