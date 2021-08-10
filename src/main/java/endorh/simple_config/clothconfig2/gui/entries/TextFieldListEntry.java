package endorh.simple_config.clothconfig2.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public abstract class TextFieldListEntry<T> extends TooltipListEntry<T> {
   protected TextFieldWidget textFieldWidget;
   protected Button resetButton;
   protected Supplier<T> defaultValue;
   protected T original;
   protected List<IGuiEventListener> widgets;
   private boolean isSelected;

   /** @deprecated */
   @Deprecated
   @Internal
   protected TextFieldListEntry(ITextComponent fieldName, T original, ITextComponent resetButtonKey, Supplier<T> defaultValue) {
      this(fieldName, original, resetButtonKey, defaultValue, null);
   }

   /** @deprecated */
   @Deprecated
   @Internal
   protected TextFieldListEntry(ITextComponent fieldName, T original, ITextComponent resetButtonKey, Supplier<T> defaultValue, Supplier<Optional<ITextComponent[]>> tooltipSupplier) {
      this(fieldName, original, resetButtonKey, defaultValue, tooltipSupplier, false);
   }

   /** @deprecated */
   @Deprecated
   @Internal
   protected TextFieldListEntry(ITextComponent fieldName, T original, ITextComponent resetButtonKey, Supplier<T> defaultValue, Supplier<Optional<ITextComponent[]>> tooltipSupplier, boolean requiresRestart) {
      super(fieldName, tooltipSupplier, requiresRestart);
      this.isSelected = false;
      this.defaultValue = defaultValue;
      this.original = original;
      this.textFieldWidget = new TextFieldWidget(Minecraft.getInstance().fontRenderer, 0, 0, 148, 18, NarratorChatListener.EMPTY) {
         public void render(MatrixStack matrices, int int_1, int int_2, float float_1) {
            this.setFocused(TextFieldListEntry.this.isSelected && TextFieldListEntry.this.getListener() == this);
            TextFieldListEntry.this.textFieldPreRender(this);
            super.render(matrices, int_1, int_2, float_1);
         }

         public void writeText(String string_1) {
            super.writeText(TextFieldListEntry.this.stripAddText(string_1));
         }
      };
      this.textFieldWidget.setMaxStringLength(999999);
      this.textFieldWidget.setText(String.valueOf(original));
      this.resetButton = new Button(0, 0, Minecraft.getInstance().fontRenderer.getStringPropertyWidth(resetButtonKey) + 6, 20, resetButtonKey, (widget) -> onReset());
      this.widgets = Lists.newArrayList(new IGuiEventListener[]{this.textFieldWidget, this.resetButton});
   }
   
   protected void onReset() {
      this.textFieldWidget.setText(String.valueOf(defaultValue.get()));
   }

   public boolean isEdited() {
      return this.isChanged(this.original, this.textFieldWidget.getText());
   }

   protected boolean isChanged(T original, String s) {
      return !String.valueOf(original).equals(s);
   }

   protected static void setTextFieldWidth(TextFieldWidget widget, int width) {
      widget.setWidth(width);
   }

   /** @deprecated */
   @Deprecated
   public void setValue(String s) {
      this.textFieldWidget.setText(String.valueOf(s));
   }

   protected String stripAddText(String s) {
      return s;
   }

   protected void textFieldPreRender(TextFieldWidget widget) {
   }

   public void updateSelected(boolean isSelected) {
      this.isSelected = isSelected;
   }

   public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
      super.render(matrices, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
      MainWindow window = Minecraft.getInstance().getMainWindow();
      this.resetButton.active = this.isEditable() && this.getDefaultValue().isPresent() && !this.isMatchDefault(this.textFieldWidget.getText());
      this.resetButton.y = y;
      this.textFieldWidget.setEnabled(this.isEditable());
      this.textFieldWidget.y = y + 1;
      ITextComponent displayedFieldName = this.getDisplayedFieldName();
      if (Minecraft.getInstance().fontRenderer.getBidiFlag()) {
         Minecraft.getInstance().fontRenderer.func_238407_a_(matrices, displayedFieldName.func_241878_f(), (float)(window.getScaledWidth() - x - Minecraft.getInstance().fontRenderer.getStringPropertyWidth(displayedFieldName)), (float)(y + 6), this.getPreferredTextColor());
         this.resetButton.x = x;
         this.textFieldWidget.x = x + this.resetButton.getWidth();
      } else {
         Minecraft.getInstance().fontRenderer.func_238407_a_(matrices, displayedFieldName.func_241878_f(), (float)x, (float)(y + 6), this.getPreferredTextColor());
         this.resetButton.x = x + entryWidth - this.resetButton.getWidth();
         this.textFieldWidget.x = x + entryWidth - 148;
      }

      setTextFieldWidth(this.textFieldWidget, 148 - this.resetButton.getWidth() - 4);
      this.resetButton.render(matrices, mouseX, mouseY, delta);
      this.textFieldWidget.render(matrices, mouseX, mouseY, delta);
   }

   protected abstract boolean isMatchDefault(String var1);

   public Optional<T> getDefaultValue() {
      return this.defaultValue == null ? Optional.empty() : Optional.ofNullable(this.defaultValue.get());
   }

   public List<? extends IGuiEventListener> getEventListeners() {
      return this.widgets;
   }
}
