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
public class IntegerListListEntry extends AbstractTextFieldListListEntry<Integer, IntegerListListEntry.IntegerListCell, IntegerListListEntry> {
   private int minimum;
   private int maximum;

   /** @deprecated */
   @Deprecated
   @Internal
   public IntegerListListEntry(ITextComponent fieldName, List<Integer> value, boolean defaultExpanded, Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<Integer>> saveConsumer, Supplier<List<Integer>> defaultValue, ITextComponent resetButtonKey) {
      this(fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue, resetButtonKey, false);
   }

   /** @deprecated */
   @Deprecated
   @Internal
   public IntegerListListEntry(ITextComponent fieldName, List<Integer> value, boolean defaultExpanded, Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<Integer>> saveConsumer, Supplier<List<Integer>> defaultValue, ITextComponent resetButtonKey, boolean requiresRestart) {
      this(fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue, resetButtonKey, requiresRestart, true, true);
   }

   /** @deprecated */
   @Deprecated
   @Internal
   public IntegerListListEntry(ITextComponent fieldName, List<Integer> value, boolean defaultExpanded, Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<Integer>> saveConsumer, Supplier<List<Integer>> defaultValue, ITextComponent resetButtonKey, boolean requiresRestart, boolean deleteButtonEnabled, boolean insertInFront) {
      super(fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue, resetButtonKey, requiresRestart, deleteButtonEnabled, insertInFront, IntegerListListEntry.IntegerListCell::new);
      this.minimum = Integer.MIN_VALUE;
      this.maximum = Integer.MAX_VALUE;
   }

   public IntegerListListEntry setMaximum(int maximum) {
      this.maximum = maximum;
      return this;
   }

   public IntegerListListEntry setMinimum(int minimum) {
      this.minimum = minimum;
      return this;
   }

   public IntegerListListEntry self() {
      return this;
   }

   public static class IntegerListCell extends AbstractTextFieldListListEntry.AbstractTextFieldListCell<Integer, IntegerListListEntry.IntegerListCell, IntegerListListEntry> {
      public IntegerListCell(Integer value, IntegerListListEntry listListEntry) {
         super(value, listListEntry);
      }

      @Nullable
      protected Integer substituteDefault(@Nullable Integer value) {
         return value == null ? 0 : value;
      }

      protected boolean isValidText(@NotNull String text) {
         return text.chars().allMatch((c) -> {
            return Character.isDigit(c) || c == 45;
         });
      }

      public Integer getValue() {
         try {
            return Integer.valueOf(this.widget.getText());
         } catch (NumberFormatException var2) {
            return 0;
         }
      }

      public Optional<ITextComponent> getError() {
         try {
            int i = Integer.parseInt(this.widget.getText());
            if (i > this.listListEntry.maximum) {
               return Optional.of(new TranslationTextComponent("text.cloth-config.error.too_large", new Object[]{
                 this.listListEntry.maximum}));
            }

            if (i < this.listListEntry.minimum) {
               return Optional.of(new TranslationTextComponent("text.cloth-config.error.too_small", new Object[]{
                 this.listListEntry.minimum}));
            }
         } catch (NumberFormatException var2) {
            return Optional.of(new TranslationTextComponent("text.cloth-config.error.not_valid_number_int"));
         }

         return Optional.empty();
      }
   }
}
