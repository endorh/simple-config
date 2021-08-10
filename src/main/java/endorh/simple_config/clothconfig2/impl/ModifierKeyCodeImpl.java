package endorh.simple_config.clothconfig2.impl;

import endorh.simple_config.clothconfig2.api.Modifier;
import endorh.simple_config.clothconfig2.api.ModifierKeyCode;
import net.minecraft.client.util.InputMappings;
import net.minecraft.client.util.InputMappings.Input;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ModifierKeyCodeImpl implements ModifierKeyCode {
   private Input keyCode;
   private Modifier modifier;

   public Input getKeyCode() {
      return this.keyCode;
   }

   public Modifier getModifier() {
      return this.modifier;
   }

   public ModifierKeyCode setKeyCode(Input keyCode) {
      this.keyCode = keyCode.getType().getOrMakeInput(keyCode.getKeyCode());
      if (keyCode.equals(InputMappings.INPUT_INVALID)) {
         this.setModifier(Modifier.none());
      }

      return this;
   }

   public ModifierKeyCode setModifier(Modifier modifier) {
      this.modifier = Modifier.of(modifier.getValue());
      return this;
   }

   public String toString() {
      return this.getLocalizedName().getString();
   }

   public ITextComponent getLocalizedName() {
      ITextComponent base = this.keyCode.func_237520_d_();
      if (this.modifier.hasShift()) {
         base = new TranslationTextComponent("modifier.cloth-config.shift", new Object[]{base});
      }

      if (this.modifier.hasControl()) {
         base = new TranslationTextComponent("modifier.cloth-config.ctrl", new Object[]{base});
      }

      if (this.modifier.hasAlt()) {
         base = new TranslationTextComponent("modifier.cloth-config.alt", new Object[]{base});
      }

      return base;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (!(o instanceof ModifierKeyCode)) {
         return false;
      } else {
         ModifierKeyCode that = (ModifierKeyCode)o;
         return this.keyCode.equals(that.getKeyCode()) && this.modifier.equals(that.getModifier());
      }
   }

   public int hashCode() {
      int result = this.keyCode != null ? this.keyCode.hashCode() : 0;
      result = 31 * result + (this.modifier != null ? this.modifier.hashCode() : 0);
      return result;
   }
}
