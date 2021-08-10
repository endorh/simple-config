package endorh.simple_config.clothconfig2.gui.entries;

import endorh.simple_config.clothconfig2.gui.entries.StringListListEntry.StringListCell;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public class StringListListEntry
  extends AbstractTextFieldListListEntry<String, StringListCell, StringListListEntry> {
	@Deprecated
	@ApiStatus.Internal
	public StringListListEntry(
	  ITextComponent fieldName, List<String> value, boolean defaultExpanded,
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<String>> saveConsumer,
	  Supplier<List<String>> defaultValue, ITextComponent resetButtonKey
	) {
		this(
		  fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue,
		  resetButtonKey, false);
	}
	
	@Deprecated
	@ApiStatus.Internal
	public StringListListEntry(
	  ITextComponent fieldName, List<String> value, boolean defaultExpanded,
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<String>> saveConsumer,
	  Supplier<List<String>> defaultValue, ITextComponent resetButtonKey, boolean requiresRestart
	) {
		this(
		  fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue,
		  resetButtonKey, requiresRestart, true, true);
	}
	
	@Deprecated
	@ApiStatus.Internal
	public StringListListEntry(
	  ITextComponent fieldName, List<String> value, boolean defaultExpanded,
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<String>> saveConsumer,
	  Supplier<List<String>> defaultValue, ITextComponent resetButtonKey, boolean requiresRestart,
	  boolean deleteButtonEnabled, boolean insertInFront
	) {
		super(
		  fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue,
		  resetButtonKey, requiresRestart, deleteButtonEnabled, insertInFront, StringListCell::new);
	}
	
	public static class StringListCell
	  extends
	  AbstractTextFieldListListEntry.AbstractTextFieldListCell<String, StringListCell, StringListListEntry> {
		public StringListCell(String value, StringListListEntry listListEntry) {
			super(value, listListEntry);
		}
		
		@Override
		@Nullable
		protected String substituteDefault(@Nullable String value) {
			if (value == null) {
				return "";
			}
			return value;
		}
		
		@Override
		protected boolean isValidText(@NotNull String text) {
			return true;
		}
		
		@Override
		public String getValue() {
			return this.widget.getText();
		}
		
		@Override public void setValue(String value) {
			this.widget.setText(value);
		}
		
		@Override
		public Optional<ITextComponent> getError() {
			return Optional.empty();
		}
	}
}

