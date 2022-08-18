package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.gui.entries.SubCategoryListEntry;
import endorh.simpleconfig.ui.gui.entries.SubCategoryListEntry.VoidEntry;
import endorh.simpleconfig.ui.gui.entries.SubCategoryListEntry.VoidEntryBuilder;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class SubCategoryBuilder extends CaptionedSubCategoryBuilder<Void, VoidEntry, VoidEntryBuilder> {
	public SubCategoryBuilder(ConfigFieldBuilder builder, ITextComponent name) {
		super(builder, name, null);
	}
	
	@Override protected @NotNull SubCategoryListEntry buildEntry() {
		List<AbstractConfigListEntry<?>> builtEntries = entries.stream()
		  .map(FieldBuilder::build).collect(Collectors.toList());
		return new SubCategoryListEntry(fieldNameKey, builtEntries);
	}
}
