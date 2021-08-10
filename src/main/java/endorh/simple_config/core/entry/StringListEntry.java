package endorh.simple_config.core.entry;

import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.ConfigEntryBuilder;
import endorh.simple_config.clothconfig2.impl.builders.StringListBuilder;
import endorh.simple_config.core.ISimpleConfigEntryHolder;
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
			super(value, String.class);
		}
		
		@Override
		protected StringListEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			return new StringListEntry(parent, name, value);
		}
		
		@Override protected Builder createCopy() {
			return new Builder(value);
		}
	}
	
	@OnlyIn(Dist.CLIENT) @Override
	public Optional<AbstractConfigListEntry<List<String>>> buildGUIEntry(ConfigEntryBuilder builder) {
		final StringListBuilder valBuilder = builder
		  .startStrList(getDisplayName(), get());
		return Optional.of(decorate(valBuilder).build());
	}
}
