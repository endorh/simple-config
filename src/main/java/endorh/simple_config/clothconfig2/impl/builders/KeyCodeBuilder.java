package endorh.simple_config.clothconfig2.impl.builders;

import endorh.simple_config.clothconfig2.api.Modifier;
import endorh.simple_config.clothconfig2.api.ModifierKeyCode;
import endorh.simple_config.clothconfig2.gui.entries.KeyCodeEntry;
import net.minecraft.client.util.InputMappings.Input;
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
public class KeyCodeBuilder extends FieldBuilder<ModifierKeyCode, KeyCodeEntry> {
   @Nullable
   private Consumer<ModifierKeyCode> saveConsumer = null;
   @NotNull
   private Function<ModifierKeyCode, Optional<ITextComponent[]>> tooltipSupplier = (bool) -> {
      return Optional.empty();
   };
   private final ModifierKeyCode value;
   private boolean allowKey = true;
   private boolean allowMouse = true;
   private boolean allowModifiers = true;

   public KeyCodeBuilder(ITextComponent resetButtonKey, ITextComponent fieldNameKey, ModifierKeyCode value) {
      super(resetButtonKey, fieldNameKey);
      this.value = ModifierKeyCode.copyOf(value);
   }

   public KeyCodeBuilder setAllowModifiers(boolean allowModifiers) {
      this.allowModifiers = allowModifiers;
      if (!allowModifiers) {
         this.value.setModifier(Modifier.none());
      }

      return this;
   }

   public KeyCodeBuilder setAllowKey(boolean allowKey) {
      if (!this.allowMouse && !allowKey) {
         throw new IllegalArgumentException();
      } else {
         this.allowKey = allowKey;
         return this;
      }
   }

   public KeyCodeBuilder setAllowMouse(boolean allowMouse) {
      if (!this.allowKey && !allowMouse) {
         throw new IllegalArgumentException();
      } else {
         this.allowMouse = allowMouse;
         return this;
      }
   }

   public KeyCodeBuilder setErrorSupplier(@Nullable Function<Input, Optional<ITextComponent>> errorSupplier) {
      return this.setModifierErrorSupplier((keyCode) -> {
         return errorSupplier.apply(keyCode.getKeyCode());
      });
   }

   public KeyCodeBuilder setModifierErrorSupplier(@Nullable Function<ModifierKeyCode, Optional<ITextComponent>> errorSupplier) {
      this.errorSupplier = errorSupplier;
      return this;
   }

   public KeyCodeBuilder requireRestart() {
      this.requireRestart(true);
      return this;
   }

   public KeyCodeBuilder setSaveConsumer(Consumer<Input> saveConsumer) {
      return this.setModifierSaveConsumer((keyCode) -> {
         saveConsumer.accept(keyCode.getKeyCode());
      });
   }

   public KeyCodeBuilder setDefaultValue(Supplier<Input> defaultValue) {
      return this.setModifierDefaultValue(() -> {
         return ModifierKeyCode.of(defaultValue.get(), Modifier.none());
      });
   }

   public KeyCodeBuilder setModifierSaveConsumer(Consumer<ModifierKeyCode> saveConsumer) {
      this.saveConsumer = saveConsumer;
      return this;
   }

   public KeyCodeBuilder setModifierDefaultValue(Supplier<ModifierKeyCode> defaultValue) {
      this.defaultValue = defaultValue;
      return this;
   }

   public KeyCodeBuilder setDefaultValue(Input defaultValue) {
      return this.setDefaultValue(ModifierKeyCode.of(defaultValue, Modifier.none()));
   }

   public KeyCodeBuilder setDefaultValue(ModifierKeyCode defaultValue) {
      this.defaultValue = () -> {
         return defaultValue;
      };
      return this;
   }

   public KeyCodeBuilder setTooltipSupplier(@NotNull Function<Input, Optional<ITextComponent[]>> tooltipSupplier) {
      return this.setModifierTooltipSupplier((keyCode) -> {
         return tooltipSupplier.apply(keyCode.getKeyCode());
      });
   }

   public KeyCodeBuilder setModifierTooltipSupplier(@NotNull Function<ModifierKeyCode, Optional<ITextComponent[]>> tooltipSupplier) {
      this.tooltipSupplier = tooltipSupplier;
      return this;
   }

   public KeyCodeBuilder setTooltipSupplier(@NotNull Supplier<Optional<ITextComponent[]>> tooltipSupplier) {
      this.tooltipSupplier = (bool) -> {
         return (Optional)tooltipSupplier.get();
      };
      return this;
   }

   public KeyCodeBuilder setTooltip(Optional<ITextComponent[]> tooltip) {
      this.tooltipSupplier = (bool) -> {
         return tooltip;
      };
      return this;
   }

   public KeyCodeBuilder setTooltip(@Nullable ITextComponent... tooltip) {
      this.tooltipSupplier = (bool) -> {
         return Optional.ofNullable(tooltip);
      };
      return this;
   }

   @NotNull
   public KeyCodeEntry build() {
      KeyCodeEntry entry = new KeyCodeEntry(this.getFieldNameKey(), this.value, this.getResetButtonKey(), this.defaultValue, this.saveConsumer,
                                            null, this.isRequireRestart());
      entry.setTooltipSupplier(() -> {
         return this.tooltipSupplier.apply(entry.getValue());
      });
      if (this.errorSupplier != null) {
         entry.setErrorSupplier(() -> {
            return this.errorSupplier.apply(entry.getValue());
         });
      }

      entry.setAllowKey(this.allowKey);
      entry.setAllowMouse(this.allowMouse);
      entry.setAllowModifiers(this.allowModifiers);
      return entry;
   }
}
