package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.StringListEntryBuilder;
import endorh.simpleconfig.core.EntryType;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import endorh.simpleconfig.ui.impl.builders.StringListBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StringListEntry extends AbstractListEntry<String, String, String, StringListEntry> {
	public StringListEntry(
	  ConfigEntryHolder parent, String name, List<String> value) {
		super(parent, name, value);
	}
	
	@Override protected @Nullable String getListTypeComment() {
		return "Text";
	}
	
	public static class Builder extends AbstractListEntry.Builder<
	  String, String, String, StringListEntry, StringListEntryBuilder, Builder
	> implements StringListEntryBuilder {
		public Builder(List<String> value) {
			super(value, EntryType.of(String.class));
		}
		
		@Override
		protected StringListEntry buildEntry(ConfigEntryHolder parent, String name) {
			return new StringListEntry(parent, name, value);
		}
		
		@Override protected Builder createCopy(List<String> value) {
			return new Builder(new ArrayList<>(value));
		}
	}
	
	@OnlyIn(Dist.CLIENT) @Override
	public Optional<FieldBuilder<List<String>, ?, ?>> buildGUIEntry(ConfigFieldBuilder builder) {
		final StringListBuilder valBuilder = builder
		  .startStrList(getDisplayName(), get());
		return Optional.of(decorate(valBuilder));
	}
}
