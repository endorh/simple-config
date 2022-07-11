package endorh.simpleconfig.ui.gui.entries;

import endorh.simpleconfig.ui.gui.entries.StringListListEntry.StringListCell;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

@OnlyIn(value = Dist.CLIENT)
public class StringListListEntry
  extends AbstractTextFieldListListEntry<String, StringListCell, StringListListEntry> {
	
		
	@Internal public StringListListEntry(
	  ITextComponent fieldName, List<String> value
	) {
		super(fieldName, value, StringListCell::new);
	}
	
	public static class StringListCell
	  extends AbstractTextFieldListListEntry.AbstractTextFieldListCell<String, StringListCell, StringListListEntry> {
		public StringListCell(StringListListEntry listListEntry) {
			super(listListEntry);
		}
		
		@Override protected boolean isValidText(@NotNull String text) {
			return true;
		}
		
		@Override public String getValue() {
			return this.widget.getValue();
		}
		
		@Override public void doSetValue(String value) {
			this.widget.setValue(value);
		}
		
		@Override public Optional<ITextComponent> getErrorMessage() {
			return Optional.empty();
		}
	}
}

