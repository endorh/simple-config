package endorh.simpleconfig.clothconfig2.impl.builders;

import endorh.simpleconfig.clothconfig2.api.ConfigEntryBuilder;
import endorh.simpleconfig.clothconfig2.gui.entries.SubCategoryListEntry;
import endorh.simpleconfig.clothconfig2.gui.entries.SubCategoryListEntry.VoidEntry;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.NotNull;

public class SubCategoryBuilder extends CaptionedSubCategoryBuilder<Void, VoidEntry> {
	public SubCategoryBuilder(ConfigEntryBuilder builder, ITextComponent name) {
		super(builder, name, null);
	}
	
	@Override protected @NotNull SubCategoryListEntry buildEntry() {
		return new SubCategoryListEntry(fieldNameKey, entries);
	}
}
