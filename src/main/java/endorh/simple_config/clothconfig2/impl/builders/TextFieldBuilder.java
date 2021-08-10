package endorh.simple_config.clothconfig2.impl.builders;

import endorh.simple_config.clothconfig2.gui.entries.StringListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class TextFieldBuilder extends FieldBuilder<String, StringListEntry> {
   private Consumer<String> saveConsumer = null;
   private Function<String, Optional<ITextComponent[]>> tooltipSupplier = (str) -> {
      return Optional.empty();
   };
   private final String value;

   public TextFieldBuilder(ITextComponent resetButtonKey, ITextComponent fieldNameKey, String value) {
      super(resetButtonKey, fieldNameKey);
      Objects.requireNonNull(value);
      this.value = value;
   }

   public TextFieldBuilder setErrorSupplier(Function<String, Optional<ITextComponent>> errorSupplier) {
      this.errorSupplier = errorSupplier;
      return this;
   }

   public TextFieldBuilder requireRestart() {
      this.requireRestart(true);
      return this;
   }

   public TextFieldBuilder setSaveConsumer(Consumer<String> saveConsumer) {
      this.saveConsumer = saveConsumer;
      return this;
   }

   public TextFieldBuilder setDefaultValue(Supplier<String> defaultValue) {
      this.defaultValue = defaultValue;
      return this;
   }

   public TextFieldBuilder setDefaultValue(String defaultValue) {
      this.defaultValue = () -> {
         return (String)Objects.requireNonNull(defaultValue);
      };
      return this;
   }

   public TextFieldBuilder setTooltipSupplier(Supplier<Optional<ITextComponent[]>> tooltipSupplier) {
      this.tooltipSupplier = (str) -> {
         return (Optional)tooltipSupplier.get();
      };
      return this;
   }

   public TextFieldBuilder setTooltipSupplier(Function<String, Optional<ITextComponent[]>> tooltipSupplier) {
      this.tooltipSupplier = tooltipSupplier;
      return this;
   }

   public TextFieldBuilder setTooltip(Optional<ITextComponent[]> tooltip) {
      this.tooltipSupplier = (str) -> {
         return tooltip;
      };
      return this;
   }

   public TextFieldBuilder setTooltip(ITextComponent... tooltip) {
      this.tooltipSupplier = (str) -> {
         return Optional.ofNullable(tooltip);
      };
      return this;
   }

   @NotNull
   public StringListEntry build() {
      StringListEntry entry = new StringListEntry(this.getFieldNameKey(), this.value, this.getResetButtonKey(), this.defaultValue, this.saveConsumer,
                                                  null, this.isRequireRestart());
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
