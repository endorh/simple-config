package endorh.simple_config.clothconfig2.impl.builders;

import endorh.simple_config.clothconfig2.gui.entries.BooleanListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class BooleanToggleBuilder extends FieldBuilder<Boolean, BooleanListEntry> {
   @Nullable
   private Consumer<Boolean> saveConsumer = null;
   @NotNull
   private Function<Boolean, Optional<ITextComponent[]>> tooltipSupplier = (bool) -> {
      return Optional.empty();
   };
   private final boolean value;
   @Nullable
   private Function<Boolean, ITextComponent> yesNoTextSupplier = null;

   public BooleanToggleBuilder(ITextComponent resetButtonKey, ITextComponent fieldNameKey, boolean value) {
      super(resetButtonKey, fieldNameKey);
      this.value = value;
   }

   public BooleanToggleBuilder setErrorSupplier(@Nullable Function<Boolean, Optional<ITextComponent>> errorSupplier) {
      this.errorSupplier = errorSupplier;
      return this;
   }

   public BooleanToggleBuilder requireRestart() {
      this.requireRestart(true);
      return this;
   }

   public BooleanToggleBuilder setSaveConsumer(Consumer<Boolean> saveConsumer) {
      this.saveConsumer = saveConsumer;
      return this;
   }

   public BooleanToggleBuilder setDefaultValue(Supplier<Boolean> defaultValue) {
      this.defaultValue = defaultValue;
      return this;
   }

   public BooleanToggleBuilder setDefaultValue(boolean defaultValue) {
      this.defaultValue = () -> {
         return defaultValue;
      };
      return this;
   }

   public BooleanToggleBuilder setTooltipSupplier(@NotNull Function<Boolean, Optional<ITextComponent[]>> tooltipSupplier) {
      this.tooltipSupplier = tooltipSupplier;
      return this;
   }

   public BooleanToggleBuilder setTooltipSupplier(@NotNull Supplier<Optional<ITextComponent[]>> tooltipSupplier) {
      this.tooltipSupplier = (bool) -> {
         return (Optional)tooltipSupplier.get();
      };
      return this;
   }

   public BooleanToggleBuilder setTooltip(Optional<ITextComponent[]> tooltip) {
      this.tooltipSupplier = (bool) -> {
         return tooltip;
      };
      return this;
   }

   public BooleanToggleBuilder setTooltip(@Nullable ITextComponent... tooltip) {
      this.tooltipSupplier = (bool) -> {
         return Optional.ofNullable(tooltip);
      };
      return this;
   }

   @Nullable
   public Function<Boolean, ITextComponent> getYesNoTextSupplier() {
      return this.yesNoTextSupplier;
   }

   public BooleanToggleBuilder setYesNoTextSupplier(@Nullable Function<Boolean, ITextComponent> yesNoTextSupplier) {
      this.yesNoTextSupplier = yesNoTextSupplier;
      return this;
   }

   @NotNull
   public BooleanListEntry build() {
      BooleanListEntry entry = new BooleanListEntry(this.getFieldNameKey(), this.value, this.getResetButtonKey(), this.defaultValue, this.saveConsumer,
                                                    null, this.isRequireRestart()) {
         public ITextComponent getYesNoText(boolean bool) {
            return BooleanToggleBuilder.this.yesNoTextSupplier == null ? super.getYesNoText(bool) : BooleanToggleBuilder.this.yesNoTextSupplier.apply(bool);
         }
      };
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
