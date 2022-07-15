package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.core.ISimpleConfigEntryHolder;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.impl.builders.StringListBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class StringListEntry extends AbstractListEntry<String, String, String, StringListEntry> {
	public StringListEntry(
	  ISimpleConfigEntryHolder parent, String name, List<String> value) {
		super(parent, name, value);
	}
	
	@Override protected @Nullable String getListTypeComment() {
		return "Text";
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
