package endorh.simple_config.clothconfig2.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class BooleanListEntry extends TooltipListEntry<Boolean> {
   private final AtomicBoolean bool;
   private final boolean original;
   private final Button buttonWidget;
   private final Button resetButton;
   private final Consumer<Boolean> saveConsumer;
   private final Supplier<Boolean> defaultValue;
   private final List<IGuiEventListener> widgets;

   /** @deprecated */
   @Deprecated
   @Internal
   public BooleanListEntry(ITextComponent fieldName, boolean bool, ITextComponent resetButtonKey, Supplier<Boolean> defaultValue, Consumer<Boolean> saveConsumer) {
      this(fieldName, bool, resetButtonKey, defaultValue, saveConsumer, null);
   }

   /** @deprecated */
   @Deprecated
   @Internal
   public BooleanListEntry(ITextComponent fieldName, boolean bool, ITextComponent resetButtonKey, Supplier<Boolean> defaultValue, Consumer<Boolean> saveConsumer, Supplier<Optional<ITextComponent[]>> tooltipSupplier) {
      this(fieldName, bool, resetButtonKey, defaultValue, saveConsumer, tooltipSupplier, false);
   }

   /** @deprecated */
   @Deprecated
   @Internal
   public BooleanListEntry(ITextComponent fieldName, boolean bool, ITextComponent resetButtonKey, Supplier<Boolean> defaultValue, Consumer<Boolean> saveConsumer, Supplier<Optional<ITextComponent[]>> tooltipSupplier, boolean requiresRestart) {
      super(fieldName, tooltipSupplier, requiresRestart);
      this.defaultValue = defaultValue;
      this.original = bool;
      this.bool = new AtomicBoolean(bool);
      this.buttonWidget = new Button(0, 0, 150, 20, NarratorChatListener.EMPTY, (widget) -> {
         this.bool.set(!this.bool.get());
      });
      this.resetButton = new Button(0, 0, Minecraft.getInstance().fontRenderer.getStringPropertyWidth(resetButtonKey) + 6, 20, resetButtonKey, (widget) -> {
         this.bool.set(defaultValue.get());
      });
      this.saveConsumer = saveConsumer;
      this.widgets = Lists.newArrayList(new IGuiEventListener[]{this.buttonWidget, this.resetButton});
   }

   public boolean isEdited() {
      return super.isEdited() || this.original != this.bool.get();
   }

   public void save() {
      if (this.saveConsumer != null) {
         this.saveConsumer.accept(this.getValue());
      }

   }

   public Boolean getValue() {
      return this.bool.get();
   }

   public Optional<Boolean> getDefaultValue() {
      return this.defaultValue == null ? Optional.empty() : Optional.ofNullable(
        this.defaultValue.get());
   }

   public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
      super.render(matrices, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
      MainWindow window = Minecraft.getInstance().getMainWindow();
      this.resetButton.active = this.isEditable() && this.getDefaultValue().isPresent() && this.defaultValue.get() != this.bool.get();
      this.resetButton.y = y;
      this.buttonWidget.active = this.isEditable();
      this.buttonWidget.y = y;
      this.buttonWidget.setMessage(this.getYesNoText(this.bool.get()));
      ITextComponent displayedFieldName = this.getDisplayedFieldName();
      if (Minecraft.getInstance().fontRenderer.getBidiFlag()) {
         Minecraft.getInstance().fontRenderer.func_238407_a_(matrices, displayedFieldName.func_241878_f(), (float)(window.getScaledWidth() - x - Minecraft.getInstance().fontRenderer.getStringPropertyWidth(displayedFieldName)), (float)(y + 6), 16777215);
         this.resetButton.x = x;
         this.buttonWidget.x = x + this.resetButton.getWidth() + 2;
      } else {
         Minecraft.getInstance().fontRenderer.func_238407_a_(matrices, displayedFieldName.func_241878_f(), (float)x, (float)(y + 6), this.getPreferredTextColor());
         this.resetButton.x = x + entryWidth - this.resetButton.getWidth();
         this.buttonWidget.x = x + entryWidth - 150;
      }

      this.buttonWidget.setWidth(150 - this.resetButton.getWidth() - 2);
      this.resetButton.render(matrices, mouseX, mouseY, delta);
      this.buttonWidget.render(matrices, mouseX, mouseY, delta);
   }

   public ITextComponent getYesNoText(boolean bool) {
      return new TranslationTextComponent("text.cloth-config.boolean.value." + bool);
   }

   public List<? extends IGuiEventListener> getEventListeners() {
      return this.widgets;
   }
}
