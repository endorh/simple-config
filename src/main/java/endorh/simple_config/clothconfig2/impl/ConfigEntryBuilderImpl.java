package endorh.simple_config.clothconfig2.impl;

import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.ConfigEntryBuilder;
import endorh.simple_config.clothconfig2.api.ModifierKeyCode;
import endorh.simple_config.clothconfig2.gui.entries.DropdownBoxEntry;
import endorh.simple_config.clothconfig2.impl.builders.*;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class ConfigEntryBuilderImpl implements ConfigEntryBuilder {
   private ITextComponent resetButtonKey;

   private ConfigEntryBuilderImpl() {
      this.resetButtonKey = new TranslationTextComponent("text.cloth-config.reset_value");
   }

   public static ConfigEntryBuilderImpl create() {
      return new ConfigEntryBuilderImpl();
   }

   public static ConfigEntryBuilderImpl createImmutable() {
      return new ConfigEntryBuilderImpl() {
         public ConfigEntryBuilder setResetButtonKey(ITextComponent resetButtonKey) {
            throw new UnsupportedOperationException("This is an immutable entry builder!");
         }
      };
   }

   public ITextComponent getResetButtonKey() {
      return this.resetButtonKey;
   }

   public ConfigEntryBuilder setResetButtonKey(ITextComponent resetButtonKey) {
      this.resetButtonKey = resetButtonKey;
      return this;
   }

   public IntListBuilder startIntList(ITextComponent fieldNameKey, List<Integer> value) {
      return new IntListBuilder(this.resetButtonKey, fieldNameKey, value);
   }

   public LongListBuilder startLongList(ITextComponent fieldNameKey, List<Long> value) {
      return new LongListBuilder(this.resetButtonKey, fieldNameKey, value);
   }

   public FloatListBuilder startFloatList(ITextComponent fieldNameKey, List<Float> value) {
      return new FloatListBuilder(this.resetButtonKey, fieldNameKey, value);
   }

   public DoubleListBuilder startDoubleList(ITextComponent fieldNameKey, List<Double> value) {
      return new DoubleListBuilder(this.resetButtonKey, fieldNameKey, value);
   }

   public StringListBuilder startStrList(ITextComponent fieldNameKey, List<String> value) {
      return new StringListBuilder(this.resetButtonKey, fieldNameKey, value);
   }

   public SubCategoryBuilder startSubCategory(ITextComponent fieldNameKey) {
      return new SubCategoryBuilder(this.resetButtonKey, fieldNameKey);
   }

   public SubCategoryBuilder startSubCategory(ITextComponent fieldNameKey, List<AbstractConfigListEntry<?>> entries) {
      SubCategoryBuilder builder = new SubCategoryBuilder(this.resetButtonKey, fieldNameKey);
      builder.addAll(entries);
      return builder;
   }

   public BooleanToggleBuilder startBooleanToggle(ITextComponent fieldNameKey, boolean value) {
      return new BooleanToggleBuilder(this.resetButtonKey, fieldNameKey, value);
   }

   public StringFieldBuilder startStrField(ITextComponent fieldNameKey, String value) {
      return new StringFieldBuilder(this.resetButtonKey, fieldNameKey, value);
   }

   public ColorFieldBuilder startColorField(ITextComponent fieldNameKey, int value) {
      return new ColorFieldBuilder(this.resetButtonKey, fieldNameKey, value);
   }

   public TextFieldBuilder startTextField(ITextComponent fieldNameKey, String value) {
      return new TextFieldBuilder(this.resetButtonKey, fieldNameKey, value);
   }

   public TextDescriptionBuilder startTextDescription(ITextComponent value) {
      return new TextDescriptionBuilder(this.resetButtonKey, new StringTextComponent(UUID.randomUUID().toString()), value);
   }

   public <T extends Enum<?>> EnumSelectorBuilder<T> startEnumSelector(ITextComponent fieldNameKey, Class<T> clazz, T value) {
      return new EnumSelectorBuilder(this.resetButtonKey, fieldNameKey, clazz, value);
   }

   public <T> SelectorBuilder<T> startSelector(ITextComponent fieldNameKey, T[] valuesArray, T value) {
      return new SelectorBuilder(this.resetButtonKey, fieldNameKey, valuesArray, value);
   }

   public IntFieldBuilder startIntField(ITextComponent fieldNameKey, int value) {
      return new IntFieldBuilder(this.resetButtonKey, fieldNameKey, value);
   }

   public LongFieldBuilder startLongField(ITextComponent fieldNameKey, long value) {
      return new LongFieldBuilder(this.resetButtonKey, fieldNameKey, value);
   }

   public FloatFieldBuilder startFloatField(ITextComponent fieldNameKey, float value) {
      return new FloatFieldBuilder(this.resetButtonKey, fieldNameKey, value);
   }

   public DoubleFieldBuilder startDoubleField(ITextComponent fieldNameKey, double value) {
      return new DoubleFieldBuilder(this.resetButtonKey, fieldNameKey, value);
   }

   public IntSliderBuilder startIntSlider(ITextComponent fieldNameKey, int value, int min, int max) {
      return new IntSliderBuilder(this.resetButtonKey, fieldNameKey, value, min, max);
   }

   public LongSliderBuilder startLongSlider(ITextComponent fieldNameKey, long value, long min, long max) {
      return new LongSliderBuilder(this.resetButtonKey, fieldNameKey, value, min, max);
   }

   public KeyCodeBuilder startModifierKeyCodeField(ITextComponent fieldNameKey, ModifierKeyCode value) {
      return new KeyCodeBuilder(this.resetButtonKey, fieldNameKey, value);
   }

   public <T> DropdownMenuBuilder<T> startDropdownMenu(ITextComponent fieldNameKey, DropdownBoxEntry.SelectionTopCellElement<T> topCellElement, DropdownBoxEntry.SelectionCellCreator<T> cellCreator) {
      return new DropdownMenuBuilder(this.resetButtonKey, fieldNameKey, topCellElement, cellCreator);
   }

   // $FF: synthetic method
   ConfigEntryBuilderImpl(Object x0) {
      this();
   }
}
