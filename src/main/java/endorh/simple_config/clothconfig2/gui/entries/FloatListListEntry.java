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
public class FloatListListEntry extends AbstractTextFieldListListEntry<Float, FloatListListEntry.FloatListCell, FloatListListEntry> {
   private float minimum;
   private float maximum;

   /** @deprecated */
   @Deprecated
   @Internal
   public FloatListListEntry(ITextComponent fieldName, List<Float> value, boolean defaultExpanded, Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<Float>> saveConsumer, Supplier<List<Float>> defaultValue, ITextComponent resetButtonKey) {
      this(fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue, resetButtonKey, false);
   }

   /** @deprecated */
   @Deprecated
   @Internal
   public FloatListListEntry(ITextComponent fieldName, List<Float> value, boolean defaultExpanded, Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<Float>> saveConsumer, Supplier<List<Float>> defaultValue, ITextComponent resetButtonKey, boolean requiresRestart) {
      this(fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue, resetButtonKey, requiresRestart, true, true);
   }

   /** @deprecated */
   @Deprecated
   @Internal
   public FloatListListEntry(ITextComponent fieldName, List<Float> value, boolean defaultExpanded, Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<Float>> saveConsumer, Supplier<List<Float>> defaultValue, ITextComponent resetButtonKey, boolean requiresRestart, boolean deleteButtonEnabled, boolean insertInFront) {
      super(fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue, resetButtonKey, requiresRestart, deleteButtonEnabled, insertInFront, FloatListListEntry.FloatListCell::new);
      this.minimum = Float.NEGATIVE_INFINITY;
      this.maximum = Float.POSITIVE_INFINITY;
   }

   public FloatListListEntry setMaximum(float maximum) {
      this.maximum = maximum;
      return this;
   }

   public FloatListListEntry setMinimum(float minimum) {
      this.minimum = minimum;
      return this;
   }

   public FloatListListEntry self() {
      return this;
   }

   public static class FloatListCell extends AbstractTextFieldListListEntry.AbstractTextFieldListCell<Float, FloatListListEntry.FloatListCell, FloatListListEntry> {
      public FloatListCell(Float value, FloatListListEntry listListEntry) {
         super(value, listListEntry);
      }

      @Nullable
      protected Float substituteDefault(@Nullable Float value) {
         return value == null ? 0.0F : value;
      }

      protected boolean isValidText(@NotNull String text) {
         return text.chars().allMatch((c) -> {
            return Character.isDigit(c) || c == 45 || c == 46;
         });
      }

      public Float getValue() {
         try {
            return Float.valueOf(this.widget.getText());
         } catch (NumberFormatException var2) {
            return 0.0F;
         }
      }

      public Optional<ITextComponent> getError() {
         try {
            float i = Float.parseFloat(this.widget.getText());
            if (i > this.listListEntry.maximum) {
               return Optional.of(new TranslationTextComponent("text.cloth-config.error.too_large", new Object[]{
                 this.listListEntry.maximum}));
            }

            if (i < this.listListEntry.minimum) {
               return Optional.of(new TranslationTextComponent("text.cloth-config.error.too_small", new Object[]{
                 this.listListEntry.minimum}));
            }
         } catch (NumberFormatException var2) {
            return Optional.of(new TranslationTextComponent("text.cloth-config.error.not_valid_number_float"));
         }

         return Optional.empty();
      }
   }
}
