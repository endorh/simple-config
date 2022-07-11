package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.gui.entries.SubCategoryListEntry;
import endorh.simpleconfig.ui.gui.entries.SubCategoryListEntry.VoidEntry;
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
