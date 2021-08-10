package endorh.simple_config.clothconfig2.gui.entries;

import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class StringListListEntry extends AbstractTextFieldListListEntry<String, StringListListEntry.StringListCell, StringListListEntry> {
   /** @deprecated */
   @Deprecated
   @Internal
   public StringListListEntry(ITextComponent fieldName, List<String> value, boolean defaultExpanded, Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<String>> saveConsumer, Supplier<List<String>> defaultValue, ITextComponent resetButtonKey) {
      this(fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue, resetButtonKey, false);
   }

   /** @deprecated */
   @Deprecated
   @Internal
   public StringListListEntry(ITextComponent fieldName, List<String> value, boolean defaultExpanded, Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<String>> saveConsumer, Supplier<List<String>> defaultValue, ITextComponent resetButtonKey, boolean requiresRestart) {
      this(fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue, resetButtonKey, requiresRestart, true, true);
   }

   /** @deprecated */
   @Deprecated
   @Internal
   public StringListListEntry(ITextComponent fieldName, List<String> value, boolean defaultExpanded, Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<String>> saveConsumer, Supplier<List<String>> defaultValue, ITextComponent resetButtonKey, boolean requiresRestart, boolean deleteButtonEnabled, boolean insertInFront) {
      super(fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue, resetButtonKey, requiresRestart, deleteButtonEnabled, insertInFront, StringListListEntry.StringListCell::new);
   }

   public StringListListEntry self() {
      return this;
   }

   public static class StringListCell extends AbstractTextFieldListListEntry.AbstractTextFieldListCell<String, StringListListEntry.StringListCell, StringListListEntry> {
      public StringListCell(String value, StringListListEntry listListEntry) {
         super(value, listListEntry);
      }

      @Nullable
      protected String substituteDefault(@Nullable String value) {
         return value == null ? "" : value;
      }

      protected boolean isValidText(@NotNull String text) {
         return true;
      }

      public String getValue() {
         return this.widget.getText();
      }

      public Optional<ITextComponent> getError() {
         return Optional.empty();
      }
   }
}
