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
public class DoubleListListEntry extends AbstractTextFieldListListEntry<Double, DoubleListListEntry.DoubleListCell, DoubleListListEntry> {
   private double minimum;
   private double maximum;

   /** @deprecated */
   @Deprecated
   @Internal
   public DoubleListListEntry(ITextComponent fieldName, List<Double> value, boolean defaultExpanded, Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<Double>> saveConsumer, Supplier<List<Double>> defaultValue, ITextComponent resetButtonKey) {
      this(fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue, resetButtonKey, false);
   }

   /** @deprecated */
   @Deprecated
   @Internal
   public DoubleListListEntry(ITextComponent fieldName, List<Double> value, boolean defaultExpanded, Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<Double>> saveConsumer, Supplier<List<Double>> defaultValue, ITextComponent resetButtonKey, boolean requiresRestart) {
      this(fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue, resetButtonKey, requiresRestart, true, true);
   }

   /** @deprecated */
   @Deprecated
   @Internal
   public DoubleListListEntry(ITextComponent fieldName, List<Double> value, boolean defaultExpanded, Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<Double>> saveConsumer, Supplier<List<Double>> defaultValue, ITextComponent resetButtonKey, boolean requiresRestart, boolean deleteButtonEnabled, boolean insertInFront) {
      super(fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue, resetButtonKey, requiresRestart, deleteButtonEnabled, insertInFront, DoubleListListEntry.DoubleListCell::new);
      this.minimum = Double.NEGATIVE_INFINITY;
      this.maximum = Double.POSITIVE_INFINITY;
   }

   public DoubleListListEntry setMaximum(Double maximum) {
      this.maximum = maximum;
      return this;
   }

   public DoubleListListEntry setMinimum(Double minimum) {
      this.minimum = minimum;
      return this;
   }

   public DoubleListListEntry self() {
      return this;
   }

   public static class DoubleListCell extends AbstractTextFieldListListEntry.AbstractTextFieldListCell<Double, DoubleListListEntry.DoubleListCell, DoubleListListEntry> {
      public DoubleListCell(Double value, DoubleListListEntry listListEntry) {
         super(value, listListEntry);
      }

      @Nullable
      protected Double substituteDefault(@Nullable Double value) {
         return value == null ? 0.0D : value;
      }

      protected boolean isValidText(@NotNull String text) {
         return text.chars().allMatch((c) -> {
            return Character.isDigit(c) || c == 45 || c == 46;
         });
      }

      public Double getValue() {
         try {
            return Double.valueOf(this.widget.getText());
         } catch (NumberFormatException var2) {
            return 0.0D;
         }
      }

      public Optional<ITextComponent> getError() {
         try {
            double i = Double.parseDouble(this.widget.getText());
            if (i > this.listListEntry.maximum) {
               return Optional.of(new TranslationTextComponent("text.cloth-config.error.too_large", new Object[]{
                 this.listListEntry.maximum}));
            }

            if (i < this.listListEntry.minimum) {
               return Optional.of(new TranslationTextComponent("text.cloth-config.error.too_small", new Object[]{
                 this.listListEntry.minimum}));
            }
         } catch (NumberFormatException var3) {
            return Optional.of(new TranslationTextComponent("text.cloth-config.error.not_valid_number_double"));
         }

         return Optional.empty();
      }
   }
}
