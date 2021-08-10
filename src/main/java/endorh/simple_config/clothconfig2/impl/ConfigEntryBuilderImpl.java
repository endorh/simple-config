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

@OnlyIn(value = Dist.CLIENT)
public class ConfigEntryBuilderImpl
  implements ConfigEntryBuilder {
	private ITextComponent resetButtonKey =
	  new TranslationTextComponent("text.cloth-config.reset_value");
	
	private ConfigEntryBuilderImpl() {
	}
	
	public static ConfigEntryBuilderImpl create() {
		return new ConfigEntryBuilderImpl();
	}
	
	public static ConfigEntryBuilderImpl createImmutable() {
		return new ConfigEntryBuilderImpl() {
			
			@Override
			public ConfigEntryBuilder setResetButtonKey(ITextComponent resetButtonKey) {
				throw new UnsupportedOperationException("This is an immutable entry builder!");
			}
		};
	}
	
	@Override
	public ITextComponent getResetButtonKey() {
		return this.resetButtonKey;
	}
	
	@Override
	public ConfigEntryBuilder setResetButtonKey(ITextComponent resetButtonKey) {
		this.resetButtonKey = resetButtonKey;
		return this;
	}
	
	@Override
	public IntListBuilder startIntList(ITextComponent fieldNameKey, List<Integer> value) {
		return new IntListBuilder(this.resetButtonKey, fieldNameKey, value);
	}
	
	@Override
	public LongListBuilder startLongList(ITextComponent fieldNameKey, List<Long> value) {
		return new LongListBuilder(this.resetButtonKey, fieldNameKey, value);
	}
	
	@Override
	public FloatListBuilder startFloatList(ITextComponent fieldNameKey, List<Float> value) {
		return new FloatListBuilder(this.resetButtonKey, fieldNameKey, value);
	}
	
	@Override
	public DoubleListBuilder startDoubleList(ITextComponent fieldNameKey, List<Double> value) {
		return new DoubleListBuilder(this.resetButtonKey, fieldNameKey, value);
	}
	
	@Override
	public StringListBuilder startStrList(ITextComponent fieldNameKey, List<String> value) {
		return new StringListBuilder(this.resetButtonKey, fieldNameKey, value);
	}
	
	@Override
	public SubCategoryBuilder startSubCategory(ITextComponent fieldNameKey) {
		return new SubCategoryBuilder(this.resetButtonKey, fieldNameKey);
	}
	
	@Override
	public SubCategoryBuilder startSubCategory(
	  ITextComponent fieldNameKey, List<AbstractConfigListEntry<?>> entries
	) {
		SubCategoryBuilder builder = new SubCategoryBuilder(this.resetButtonKey, fieldNameKey);
		builder.addAll(entries);
		return builder;
	}
	
	@Override
	public BooleanToggleBuilder startBooleanToggle(ITextComponent fieldNameKey, boolean value) {
		return new BooleanToggleBuilder(this.resetButtonKey, fieldNameKey, value);
	}
	
	@Override
	public StringFieldBuilder startStrField(ITextComponent fieldNameKey, String value) {
		return new StringFieldBuilder(this.resetButtonKey, fieldNameKey, value);
	}
	
	@Override
	public ColorFieldBuilder startColorField(ITextComponent fieldNameKey, int value) {
		return new ColorFieldBuilder(this.resetButtonKey, fieldNameKey, value);
	}
	
	@Override
	public TextFieldBuilder startTextField(ITextComponent fieldNameKey, String value) {
		return new TextFieldBuilder(this.resetButtonKey, fieldNameKey, value);
	}
	
	@Override
	public TextDescriptionBuilder startTextDescription(ITextComponent value) {
		return new TextDescriptionBuilder(this.resetButtonKey,
		                                  new StringTextComponent(UUID.randomUUID().toString()),
		                                  value);
	}
	
	@Override
	public <T extends Enum<?>> EnumSelectorBuilder<T> startEnumSelector(
	  ITextComponent fieldNameKey, Class<T> clazz, T value
	) {
		return new EnumSelectorBuilder<>(this.resetButtonKey, fieldNameKey, clazz, value);
	}
	
	@Override
	public <T> SelectorBuilder<T> startSelector(
	  ITextComponent fieldNameKey, T[] valuesArray, T value
	) {
		return new SelectorBuilder<>(this.resetButtonKey, fieldNameKey, valuesArray, value);
	}
	
	@Override
	public IntFieldBuilder startIntField(ITextComponent fieldNameKey, int value) {
		return new IntFieldBuilder(this.resetButtonKey, fieldNameKey, value);
	}
	
	@Override
	public LongFieldBuilder startLongField(ITextComponent fieldNameKey, long value) {
		return new LongFieldBuilder(this.resetButtonKey, fieldNameKey, value);
	}
	
	@Override
	public FloatFieldBuilder startFloatField(ITextComponent fieldNameKey, float value) {
		return new FloatFieldBuilder(this.resetButtonKey, fieldNameKey, value);
	}
	
	@Override
	public DoubleFieldBuilder startDoubleField(ITextComponent fieldNameKey, double value) {
		return new DoubleFieldBuilder(this.resetButtonKey, fieldNameKey, value);
	}
	
	@Override
	public IntSliderBuilder startIntSlider(
	  ITextComponent fieldNameKey, int value, int min, int max
	) {
		return new IntSliderBuilder(this.resetButtonKey, fieldNameKey, value, min, max);
	}
	
	@Override
	public LongSliderBuilder startLongSlider(
	  ITextComponent fieldNameKey, long value, long min, long max
	) {
		return new LongSliderBuilder(this.resetButtonKey, fieldNameKey, value, min, max);
	}
	
	@Override
	public KeyCodeBuilder startModifierKeyCodeField(
	  ITextComponent fieldNameKey, ModifierKeyCode value
	) {
		return new KeyCodeBuilder(this.resetButtonKey, fieldNameKey, value);
	}
	
	@Override
	public <T> DropdownMenuBuilder<T> startDropdownMenu(
	  ITextComponent fieldNameKey, DropdownBoxEntry.SelectionTopCellElement<T> topCellElement,
	  DropdownBoxEntry.SelectionCellCreator<T> cellCreator
	) {
		return new DropdownMenuBuilder<>(
		  this.resetButtonKey, fieldNameKey, topCellElement, cellCreator);
	}
}

