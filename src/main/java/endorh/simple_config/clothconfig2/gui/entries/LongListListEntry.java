package endorh.simple_config.clothconfig2.gui.entries;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
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
public class LongListListEntry extends AbstractTextFieldListListEntry<Long, LongListListEntry.LongListCell, LongListListEntry> {
   private long minimum;
   private long maximum;

   /** @deprecated */
   @Deprecated
   @Internal
   public LongListListEntry(ITextComponent fieldName, List<Long> value, boolean defaultExpanded, Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<Long>> saveConsumer, Supplier<List<Long>> defaultValue, ITextComponent resetButtonKey) {
      this(fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue, resetButtonKey, false);
   }

   /** @deprecated */
   @Deprecated
   @Internal
   public LongListListEntry(ITextComponent fieldName, List<Long> value, boolean defaultExpanded, Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<Long>> saveConsumer, Supplier<List<Long>> defaultValue, ITextComponent resetButtonKey, boolean requiresRestart) {
      this(fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue, resetButtonKey, requiresRestart, true, true);
   }

   /** @deprecated */
   @Deprecated
   @Internal
   public LongListListEntry(ITextComponent fieldName, List<Long> value, boolean defaultExpanded, Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<Long>> saveConsumer, Supplier<List<Long>> defaultValue, ITextComponent resetButtonKey, boolean requiresRestart, boolean deleteButtonEnabled, boolean insertInFront) {
      super(fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue, resetButtonKey, requiresRestart, deleteButtonEnabled, insertInFront, LongListListEntry.LongListCell::new);
      this.minimum = Long.MIN_VALUE;
      this.maximum = Long.MAX_VALUE;
   }

   public LongListListEntry setMaximum(long maximum) {
      this.maximum = maximum;
      return this;
   }

   public LongListListEntry setMinimum(long minimum) {
      this.minimum = minimum;
      return this;
   }

   public LongListListEntry self() {
      return this;
   }

   public static class LongListCell extends AbstractTextFieldListListEntry.AbstractTextFieldListCell<Long, LongListListEntry.LongListCell, LongListListEntry> {
      public LongListCell(Long value, LongListListEntry listListEntry) {
         super(value, listListEntry);
      }

      @Nullable
      protected Long substituteDefault(@Nullable Long value) {
         return value == null ? 0L : value;
      }

      protected boolean isValidText(@NotNull String text) {
         return text.chars().allMatch((c) -> {
            return Character.isDigit(c) || c == 45;
         });
      }

      public Long getValue() {
         try {
            return Long.valueOf(this.widget.getText());
         } catch (NumberFormatException var2) {
            return 0L;
         }
      }

      public Optional<ITextComponent> getError() {
         try {
            long l = Long.parseLong(this.widget.getText());
            if (l > this.listListEntry.maximum) {
               return Optional.of(new TranslationTextComponent("text.cloth-config.error.too_large", new Object[]{
                 this.listListEntry.maximum}));
            }

            if (l < this.listListEntry.minimum) {
               return Optional.of(new TranslationTextComponent("text.cloth-config.error.too_small", new Object[]{
                 this.listListEntry.minimum}));
            }
         } catch (NumberFormatException var3) {
            return Optional.of(new TranslationTextComponent("text.cloth-config.error.not_valid_number_long"));
         }

         return Optional.empty();
      }
   }
}
