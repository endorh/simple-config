package endorh.simple_config.clothconfig2.gui.entries;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simple_config.clothconfig2.gui.widget.ColorDisplayWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ColorEntry extends TextFieldListEntry<Integer> {
   private final ColorDisplayWidget colorDisplayWidget;
   private final Consumer<Integer> saveConsumer;
   private boolean alpha = true;

   /** @deprecated */
   @Deprecated
   @Internal
   public ColorEntry(ITextComponent fieldName, int value, ITextComponent resetButtonKey, Supplier<Integer> defaultValue, Consumer<Integer> saveConsumer, Supplier<Optional<ITextComponent[]>> tooltipSupplier, boolean requiresRestart) {
      super(fieldName, 0, resetButtonKey, defaultValue, tooltipSupplier, requiresRestart);
      ColorEntry.ColorValue colorValue = this.getColorValue(String.valueOf(value));
      if (colorValue.hasError()) {
         throw new IllegalArgumentException("Invalid Color: " + colorValue.getError().name());
      } else {
         this.alpha = false;
         this.saveConsumer = saveConsumer;
         this.original = value;
         this.textFieldWidget.setText(this.getHexColorString(value));
         this.colorDisplayWidget = new ColorDisplayWidget(this.textFieldWidget, 0, 0, 20, this.getColorValueColor(this.textFieldWidget.getText()));
      }
   }
   
   @Override protected void onReset() {
      this.textFieldWidget.setText(this.getHexColorString(defaultValue.get()));
   }
   
   protected boolean isChanged(Integer original, String s) {
      ColorEntry.ColorValue colorValue = this.getColorValue(s);
      return colorValue.hasError() || this.original != colorValue.color;
   }

   public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
      super.render(matrices, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
      this.colorDisplayWidget.y = y;
      ColorEntry.ColorValue value = this.getColorValue(this.textFieldWidget.getText());
      if (!value.hasError()) {
         this.colorDisplayWidget.setColor(this.alpha ? value.getColor() : -16777216 | value.getColor());
      }

      if (Minecraft.getInstance().fontRenderer.getBidiFlag()) {
         this.colorDisplayWidget.x = x + this.resetButton.getWidth() + this.textFieldWidget.getWidth();
      } else {
         this.colorDisplayWidget.x = this.textFieldWidget.x - 23;
      }

      this.colorDisplayWidget.render(matrices, mouseX, mouseY, delta);
   }

   protected void textFieldPreRender(TextFieldWidget widget) {
      if (!this.getConfigError().isPresent()) {
         widget.setTextColor(14737632);
      } else {
         widget.setTextColor(16733525);
      }

   }

   public void save() {
      if (this.saveConsumer != null) {
         this.saveConsumer.accept(this.getValue());
      }

   }

   protected boolean isMatchDefault(String text) {
      if (!this.getDefaultValue().isPresent()) {
         return false;
      } else {
         ColorEntry.ColorValue colorValue = this.getColorValue(text);
         return !colorValue.hasError() && colorValue.color == this.getDefaultValue().get();
      }
   }

   public boolean isEdited() {
      ColorEntry.ColorValue colorValue = this.getColorValue(this.textFieldWidget.getText());
      return colorValue.hasError() || colorValue.color != this.original;
   }

   public Integer getValue() {
      return this.getColorValueColor(this.textFieldWidget.getText());
   }

   /** @deprecated */
   @Deprecated
   public void setValue(int color) {
      this.textFieldWidget.setText(this.getHexColorString(color));
   }

   public Optional<ITextComponent> getError() {
      ColorEntry.ColorValue colorValue = this.getColorValue(this.textFieldWidget.getText());
      return colorValue.hasError() ? Optional.of(new TranslationTextComponent("text.cloth-config.error.color." + colorValue.getError().name().toLowerCase(Locale.ROOT))) : super.getError();
   }

   public void withAlpha() {
      if (!this.alpha) {
         this.alpha = true;
         this.textFieldWidget.setText(this.getHexColorString(this.original));
      }

   }

   public void withoutAlpha() {
      if (this.alpha) {
         this.alpha = false;
         this.textFieldWidget.setText(this.getHexColorString(this.original));
      }

   }

   protected String stripHexStarter(String hex) {
      return hex.startsWith("#") ? hex.substring(1) : hex;
   }

   protected boolean isValidColorString(String str) {
      return !this.getColorValue(str).hasError();
   }

   protected int getColorValueColor(String str) {
      return this.getColorValue(str).getColor();
   }

   protected ColorEntry.ColorValue getColorValue(String str) {
      try {
         int color;
         if (str.startsWith("#")) {
            String stripHexStarter = this.stripHexStarter(str);
            if (stripHexStarter.length() > 8) {
               return ColorEntry.ColorError.INVALID_COLOR.toValue();
            }

            if (!this.alpha && stripHexStarter.length() > 6) {
               return ColorEntry.ColorError.NO_ALPHA_ALLOWED.toValue();
            }

            color = (int)Long.parseLong(stripHexStarter, 16);
         } else {
            color = (int)Long.parseLong(str);
         }

         int a = color >> 24 & 255;
         if (!this.alpha && a > 0) {
            return ColorEntry.ColorError.NO_ALPHA_ALLOWED.toValue();
         } else if (a >= 0 && a <= 255) {
            int r = color >> 16 & 255;
            if (r >= 0 && r <= 255) {
               int g = color >> 8 & 255;
               if (g >= 0 && g <= 255) {
                  int b = color & 255;
                  return b >= 0 && b <= 255 ? new ColorEntry.ColorValue(color) : ColorEntry.ColorError.INVALID_BLUE.toValue();
               } else {
                  return ColorEntry.ColorError.INVALID_GREEN.toValue();
               }
            } else {
               return ColorEntry.ColorError.INVALID_RED.toValue();
            }
         } else {
            return ColorEntry.ColorError.INVALID_ALPHA.toValue();
         }
      } catch (NumberFormatException var7) {
         return ColorEntry.ColorError.INVALID_COLOR.toValue();
      }
   }

   protected String getHexColorString(int color) {
      return "#" + StringUtils.leftPad(Integer.toHexString(color), this.alpha ? 8 : 6, '0');
   }

   protected static class ColorValue {
      private int color = -1;
      @Nullable
      private ColorEntry.ColorError error = null;

      public ColorValue(int color) {
         this.color = color;
      }

      public ColorValue(ColorEntry.ColorError error) {
         this.error = error;
      }

      public int getColor() {
         return this.color;
      }

      @Nullable
      public ColorEntry.ColorError getError() {
         return this.error;
      }

      public boolean hasError() {
         return this.getError() != null;
      }
   }

   protected static enum ColorError {
      NO_ALPHA_ALLOWED,
      INVALID_ALPHA,
      INVALID_RED,
      INVALID_GREEN,
      INVALID_BLUE,
      INVALID_COLOR;

      private final ColorEntry.ColorValue value = new ColorEntry.ColorValue(this);

      public ColorEntry.ColorValue toValue() {
         return this.value;
      }
   }
}
