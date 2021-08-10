package endorh.simple_config.clothconfig2.api;

import endorh.simple_config.clothconfig2.gui.entries.DropdownBoxEntry;
import endorh.simple_config.clothconfig2.impl.ConfigEntryBuilderImpl;
import endorh.simple_config.clothconfig2.impl.builders.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings.Input;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
public interface ConfigEntryBuilder {
   static ConfigEntryBuilder create() {
      return ConfigEntryBuilderImpl.create();
   }

   ITextComponent getResetButtonKey();

   ConfigEntryBuilder setResetButtonKey(ITextComponent var1);

   IntListBuilder startIntList(ITextComponent var1, List<Integer> var2);

   LongListBuilder startLongList(ITextComponent var1, List<Long> var2);

   FloatListBuilder startFloatList(ITextComponent var1, List<Float> var2);

   DoubleListBuilder startDoubleList(ITextComponent var1, List<Double> var2);

   StringListBuilder startStrList(ITextComponent var1, List<String> var2);

   SubCategoryBuilder startSubCategory(ITextComponent var1);

   SubCategoryBuilder startSubCategory(ITextComponent var1, List<AbstractConfigListEntry<?>> var2);

   BooleanToggleBuilder startBooleanToggle(ITextComponent var1, boolean var2);

   StringFieldBuilder startStrField(ITextComponent var1, String var2);

   ColorFieldBuilder startColorField(ITextComponent var1, int var2);

   default ColorFieldBuilder startColorField(ITextComponent fieldNameKey, Color color) {
      return this.startColorField(fieldNameKey, color.getColor());
   }

   default ColorFieldBuilder startColorField(ITextComponent fieldNameKey, endorh.simple_config.clothconfig2.math.Color color) {
      return this.startColorField(fieldNameKey, color.getColor() & 16777215);
   }

   default ColorFieldBuilder startAlphaColorField(ITextComponent fieldNameKey, int value) {
      return this.startColorField(fieldNameKey, value).setAlphaMode(true);
   }

   default ColorFieldBuilder startAlphaColorField(ITextComponent fieldNameKey, endorh.simple_config.clothconfig2.math.Color color) {
      return this.startColorField(fieldNameKey, color.getColor());
   }

   TextFieldBuilder startTextField(ITextComponent var1, String var2);

   TextDescriptionBuilder startTextDescription(ITextComponent var1);

   <T extends Enum<?>> EnumSelectorBuilder<T> startEnumSelector(ITextComponent var1, Class<T> var2, T var3);

   <T> SelectorBuilder<T> startSelector(ITextComponent var1, T[] var2, T var3);

   IntFieldBuilder startIntField(ITextComponent var1, int var2);

   LongFieldBuilder startLongField(ITextComponent var1, long var2);

   FloatFieldBuilder startFloatField(ITextComponent var1, float var2);

   DoubleFieldBuilder startDoubleField(ITextComponent var1, double var2);

   IntSliderBuilder startIntSlider(ITextComponent var1, int var2, int var3, int var4);

   LongSliderBuilder startLongSlider(ITextComponent var1, long var2, long var4, long var6);

   KeyCodeBuilder startModifierKeyCodeField(ITextComponent var1, ModifierKeyCode var2);

   default KeyCodeBuilder startKeyCodeField(ITextComponent fieldNameKey, Input value) {
      return this.startModifierKeyCodeField(fieldNameKey, ModifierKeyCode.of(value, Modifier.none())).setAllowModifiers(false);
   }

   default KeyCodeBuilder fillKeybindingField(ITextComponent fieldNameKey, KeyBinding value) {
      return this.startKeyCodeField(fieldNameKey, value.getKey()).setDefaultValue(value.getDefault()).setSaveConsumer((code) -> {
         value.bind(code);
         KeyBinding.resetKeyBindingArrayAndHash();
         Minecraft.getInstance().gameSettings.saveOptions();
      });
   }

   <T> DropdownMenuBuilder<T> startDropdownMenu(ITextComponent var1, DropdownBoxEntry.SelectionTopCellElement<T> var2, DropdownBoxEntry.SelectionCellCreator<T> var3);

   default <T> DropdownMenuBuilder<T> startDropdownMenu(ITextComponent fieldNameKey, DropdownBoxEntry.SelectionTopCellElement<T> topCellElement) {
      return this.startDropdownMenu(fieldNameKey, (DropdownBoxEntry.SelectionTopCellElement)topCellElement,
                                    new DropdownBoxEntry.DefaultSelectionCellCreator());
   }

   default <T> DropdownMenuBuilder<T> startDropdownMenu(ITextComponent fieldNameKey, T value, Function<String, T> toObjectFunction, DropdownBoxEntry.SelectionCellCreator<T> cellCreator) {
      return this.startDropdownMenu(fieldNameKey, DropdownMenuBuilder.TopCellElementBuilder.of(value, toObjectFunction), cellCreator);
   }

   default <T> DropdownMenuBuilder<T> startDropdownMenu(ITextComponent fieldNameKey, T value, Function<String, T> toObjectFunction, Function<T, ITextComponent> toTextFunction, DropdownBoxEntry.SelectionCellCreator<T> cellCreator) {
      return this.startDropdownMenu(fieldNameKey, DropdownMenuBuilder.TopCellElementBuilder.of(value, toObjectFunction, toTextFunction), cellCreator);
   }

   default <T> DropdownMenuBuilder<T> startDropdownMenu(ITextComponent fieldNameKey, T value, Function<String, T> toObjectFunction) {
      return this.startDropdownMenu(fieldNameKey, (DropdownBoxEntry.SelectionTopCellElement)DropdownMenuBuilder.TopCellElementBuilder.of(value, toObjectFunction),
                                    new DropdownBoxEntry.DefaultSelectionCellCreator());
   }

   default <T> DropdownMenuBuilder<T> startDropdownMenu(ITextComponent fieldNameKey, T value, Function<String, T> toObjectFunction, Function<T, ITextComponent> toTextFunction) {
      return this.startDropdownMenu(fieldNameKey, (DropdownBoxEntry.SelectionTopCellElement)DropdownMenuBuilder.TopCellElementBuilder.of(value, toObjectFunction, toTextFunction),
                                    new DropdownBoxEntry.DefaultSelectionCellCreator());
   }

   default DropdownMenuBuilder<String> startStringDropdownMenu(ITextComponent fieldNameKey, String value, DropdownBoxEntry.SelectionCellCreator<String> cellCreator) {
      return this.startDropdownMenu(fieldNameKey, DropdownMenuBuilder.TopCellElementBuilder.of(value, (s) -> {
         return s;
      }, StringTextComponent::new), cellCreator);
   }

   default DropdownMenuBuilder<String> startStringDropdownMenu(ITextComponent fieldNameKey, String value, Function<String, ITextComponent> toTextFunction, DropdownBoxEntry.SelectionCellCreator<String> cellCreator) {
      return this.startDropdownMenu(fieldNameKey, DropdownMenuBuilder.TopCellElementBuilder.of(value, (s) -> {
         return s;
      }, toTextFunction), cellCreator);
   }

   default DropdownMenuBuilder<String> startStringDropdownMenu(ITextComponent fieldNameKey, String value) {
      return this.startDropdownMenu(fieldNameKey, (DropdownBoxEntry.SelectionTopCellElement)DropdownMenuBuilder.TopCellElementBuilder.of(value, (s) -> {
         return s;
      }, StringTextComponent::new), new DropdownBoxEntry.DefaultSelectionCellCreator());
   }

   default DropdownMenuBuilder<String> startStringDropdownMenu(ITextComponent fieldNameKey, String value, Function<String, ITextComponent> toTextFunction) {
      return this.startDropdownMenu(fieldNameKey, (DropdownBoxEntry.SelectionTopCellElement)DropdownMenuBuilder.TopCellElementBuilder.of(value, (s) -> {
         return s;
      }, toTextFunction), new DropdownBoxEntry.DefaultSelectionCellCreator());
   }
}
