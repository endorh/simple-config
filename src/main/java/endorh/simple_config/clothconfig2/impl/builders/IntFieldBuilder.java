package endorh.simple_config.clothconfig2.impl.builders;

import endorh.simple_config.clothconfig2.gui.entries.IntegerListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class IntFieldBuilder extends FieldBuilder<Integer, IntegerListEntry> {
   private Consumer<Integer> saveConsumer = null;
   private Function<Integer, Optional<ITextComponent[]>> tooltipSupplier = (i) -> {
      return Optional.empty();
   };
   private final int value;
   private Integer min = null;
   private Integer max = null;

   public IntFieldBuilder(ITextComponent resetButtonKey, ITextComponent fieldNameKey, int value) {
      super(resetButtonKey, fieldNameKey);
      this.value = value;
   }

   public IntFieldBuilder requireRestart() {
      this.requireRestart(true);
      return this;
   }

   public IntFieldBuilder setErrorSupplier(Function<Integer, Optional<ITextComponent>> errorSupplier) {
      this.errorSupplier = errorSupplier;
      return this;
   }

   public IntFieldBuilder setSaveConsumer(Consumer<Integer> saveConsumer) {
      this.saveConsumer = saveConsumer;
      return this;
   }

   public IntFieldBuilder setDefaultValue(Supplier<Integer> defaultValue) {
      this.defaultValue = defaultValue;
      return this;
   }

   public IntFieldBuilder setDefaultValue(int defaultValue) {
      this.defaultValue = () -> {
         return defaultValue;
      };
      return this;
   }

   public IntFieldBuilder setTooltipSupplier(Function<Integer, Optional<ITextComponent[]>> tooltipSupplier) {
      this.tooltipSupplier = tooltipSupplier;
      return this;
   }

   public IntFieldBuilder setTooltipSupplier(Supplier<Optional<ITextComponent[]>> tooltipSupplier) {
      this.tooltipSupplier = (i) -> {
         return (Optional)tooltipSupplier.get();
      };
      return this;
   }

   public IntFieldBuilder setTooltip(Optional<ITextComponent[]> tooltip) {
      this.tooltipSupplier = (i) -> {
         return tooltip;
      };
      return this;
   }

   public IntFieldBuilder setTooltip(ITextComponent... tooltip) {
      this.tooltipSupplier = (i) -> {
         return Optional.ofNullable(tooltip);
      };
      return this;
   }

   public IntFieldBuilder setMin(int min) {
      this.min = min;
      return this;
   }

   public IntFieldBuilder setMax(int max) {
      this.max = max;
      return this;
   }

   public IntFieldBuilder removeMin() {
      this.min = null;
      return this;
   }

   public IntFieldBuilder removeMax() {
      this.max = null;
      return this;
   }

   @NotNull
   public IntegerListEntry build() {
      IntegerListEntry entry = new IntegerListEntry(this.getFieldNameKey(), this.value, this.getResetButtonKey(), this.defaultValue, this.saveConsumer,
                                                    null, this.isRequireRestart());
      if (this.min != null) {
         entry.setMinimum(this.min);
      }

      if (this.max != null) {
         entry.setMaximum(this.max);
      }

      entry.setTooltipSupplier(() -> {
         return this.tooltipSupplier.apply(entry.getValue());
      });
      if (this.errorSupplier != null) {
         entry.setErrorSupplier(() -> {
            return this.errorSupplier.apply(entry.getValue());
         });
      }

      return entry;
   }
}
