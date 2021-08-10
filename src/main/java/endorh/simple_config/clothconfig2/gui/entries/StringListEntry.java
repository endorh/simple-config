package endorh.simple_config.clothconfig2.gui.entries;

import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class StringListEntry extends TextFieldListEntry<String> {
   private final Consumer<String> saveConsumer;

   /** @deprecated */
   @Deprecated
   @Internal
   public StringListEntry(ITextComponent fieldName, String value, ITextComponent resetButtonKey, Supplier<String> defaultValue, Consumer<String> saveConsumer) {
      super(fieldName, value, resetButtonKey, defaultValue);
      this.saveConsumer = saveConsumer;
   }

   /** @deprecated */
   @Deprecated
   @Internal
   public StringListEntry(ITextComponent fieldName, String value, ITextComponent resetButtonKey, Supplier<String> defaultValue, Consumer<String> saveConsumer, Supplier<Optional<ITextComponent[]>> tooltipSupplier) {
      this(fieldName, value, resetButtonKey, defaultValue, saveConsumer, tooltipSupplier, false);
   }

   /** @deprecated */
   @Deprecated
   @Internal
   public StringListEntry(ITextComponent fieldName, String value, ITextComponent resetButtonKey, Supplier<String> defaultValue, Consumer<String> saveConsumer, Supplier<Optional<ITextComponent[]>> tooltipSupplier, boolean requiresRestart) {
      super(fieldName, value, resetButtonKey, defaultValue, tooltipSupplier, requiresRestart);
      this.saveConsumer = saveConsumer;
   }

   public String getValue() {
      return this.textFieldWidget.getText();
   }

   public void save() {
      if (this.saveConsumer != null) {
         this.saveConsumer.accept(this.getValue());
      }

   }

   protected boolean isMatchDefault(String text) {
      return this.getDefaultValue().isPresent() && text.equals(this.getDefaultValue().get());
   }
}
