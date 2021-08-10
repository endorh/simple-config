package endorh.simple_config.clothconfig2.gui.entries;

import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class LongListEntry extends TextFieldListEntry<Long> {
   private static final Function<String, String> stripCharacters = (s) -> {
      StringBuilder stringBuilder_1 = new StringBuilder();
      char[] var2 = s.toCharArray();
      int var3 = var2.length;
      char[] var4 = var2;
      int var5 = var2.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         char c = var4[var6];
         if (Character.isDigit(c) || c == '-') {
            stringBuilder_1.append(c);
         }
      }

      return stringBuilder_1.toString();
   };
   private long minimum;
   private long maximum;
   private final Consumer<Long> saveConsumer;

   /** @deprecated */
   @Deprecated
   @Internal
   public LongListEntry(ITextComponent fieldName, Long value, ITextComponent resetButtonKey, Supplier<Long> defaultValue, Consumer<Long> saveConsumer) {
      super(fieldName, value, resetButtonKey, defaultValue);
      this.minimum = -9223372036854775807L;
      this.maximum = Long.MAX_VALUE;
      this.saveConsumer = saveConsumer;
   }

   /** @deprecated */
   @Deprecated
   @Internal
   public LongListEntry(ITextComponent fieldName, Long value, ITextComponent resetButtonKey, Supplier<Long> defaultValue, Consumer<Long> saveConsumer, Supplier<Optional<ITextComponent[]>> tooltipSupplier) {
      this(fieldName, value, resetButtonKey, defaultValue, saveConsumer, tooltipSupplier, false);
   }

   /** @deprecated */
   @Deprecated
   @Internal
   public LongListEntry(ITextComponent fieldName, Long value, ITextComponent resetButtonKey, Supplier<Long> defaultValue, Consumer<Long> saveConsumer, Supplier<Optional<ITextComponent[]>> tooltipSupplier, boolean requiresRestart) {
      super(fieldName, value, resetButtonKey, defaultValue, tooltipSupplier, requiresRestart);
      this.minimum = -9223372036854775807L;
      this.maximum = Long.MAX_VALUE;
      this.saveConsumer = saveConsumer;
   }

   protected String stripAddText(String s) {
      return stripCharacters.apply(s);
   }

   protected void textFieldPreRender(TextFieldWidget widget) {
      try {
         double i = (double)Long.parseLong(this.textFieldWidget.getText());
         if (!(i < (double)this.minimum) && !(i > (double)this.maximum)) {
            widget.setTextColor(14737632);
         } else {
            widget.setTextColor(16733525);
         }
      } catch (NumberFormatException var4) {
         widget.setTextColor(16733525);
      }

   }

   public void save() {
      if (this.saveConsumer != null) {
         this.saveConsumer.accept(this.getValue());
      }

   }

   protected boolean isMatchDefault(String text) {
      return this.getDefaultValue().isPresent() && text.equals(this.defaultValue.get().toString());
   }

   public LongListEntry setMinimum(long minimum) {
      this.minimum = minimum;
      return this;
   }

   public LongListEntry setMaximum(long maximum) {
      this.maximum = maximum;
      return this;
   }

   public Long getValue() {
      try {
         return Long.valueOf(this.textFieldWidget.getText());
      } catch (Exception var2) {
         return 0L;
      }
   }

   public Optional<ITextComponent> getError() {
      try {
         long i = Long.parseLong(this.textFieldWidget.getText());
         if (i > this.maximum) {
            return Optional.of(new TranslationTextComponent("text.cloth-config.error.too_large", new Object[]{this.maximum}));
         }

         if (i < this.minimum) {
            return Optional.of(new TranslationTextComponent("text.cloth-config.error.too_small", new Object[]{this.minimum}));
         }
      } catch (NumberFormatException var3) {
         return Optional.of(new TranslationTextComponent("text.cloth-config.error.not_valid_number_long"));
      }

      return super.getError();
   }
}
