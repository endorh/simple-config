package endorh.simple_config.clothconfig2.api;

import endorh.simple_config.clothconfig2.gui.entries.AbstractListListEntry;
import endorh.simple_config.clothconfig2.gui.entries.EntryPairListListEntry;
import endorh.simple_config.clothconfig2.gui.entries.NestedListListEntry;
import endorh.simple_config.clothconfig2.gui.widget.ComboBoxWidget.ITypeWrapper;
import endorh.simple_config.clothconfig2.impl.ConfigEntryBuilderImpl;
import endorh.simple_config.clothconfig2.impl.builders.*;
import endorh.simple_config.clothconfig2.math.Color;
import net.minecraft.client.util.InputMappings;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public interface ConfigEntryBuilder {
	static ConfigEntryBuilder create() {
		return ConfigEntryBuilderImpl.create();
	}
	
	IntListBuilder startIntList(ITextComponent name, List<Integer> value);
	
	LongListBuilder startLongList(ITextComponent name, List<Long> value);
	
	FloatListBuilder startFloatList(ITextComponent name, List<Float> value);
	
	DoubleListBuilder startDoubleList(ITextComponent name, List<Double> value);
	
	StringListBuilder startStrList(ITextComponent name, List<String> value);
	
	SubCategoryBuilder startSubCategory(ITextComponent name);
	
	SubCategoryBuilder startSubCategory(ITextComponent name, List<AbstractConfigListEntry<?>> value);
	
	BooleanToggleBuilder startBooleanToggle(ITextComponent name, boolean value);
	
	StringFieldBuilder startStrField(ITextComponent name, String value);
	
	ColorFieldBuilder startColorField(ITextComponent name, int value);
	
	<V, E extends AbstractConfigListEntry<V>> EntryListFieldBuilder<V, E> startEntryList(
	  ITextComponent name, List<V> value, Function<NestedListListEntry<V, E>, E> cellFactory
	);
	
	<K, V, KE extends AbstractConfigListEntry<K> & IChildListEntry,
	  VE extends AbstractConfigListEntry<V>> EntryPairListBuilder<K, V, KE, VE>
	startEntryPairList(
	  ITextComponent name, List<Pair<K, V>> value,
	  Function<EntryPairListListEntry<K, V, KE, VE>, Pair<KE, VE>> cellFactory
	);
	
	default ColorFieldBuilder startColorField(
	  ITextComponent fieldNameKey, net.minecraft.util.text.Color color
	) {
		return this.startColorField(fieldNameKey, color.getColor());
	}
	
	default ColorFieldBuilder startColorField(ITextComponent fieldNameKey, Color color) {
		return this.startColorField(fieldNameKey, color.getColor() & 0xFFFFFF);
	}
	
	default ColorFieldBuilder startAlphaColorField(ITextComponent fieldNameKey, int value) {
		return this.startColorField(fieldNameKey, value).setAlphaMode(true);
	}
	
	default ColorFieldBuilder startAlphaColorField(ITextComponent fieldNameKey, Color color) {
		return this.startColorField(fieldNameKey, color.getColor());
	}
	
	TextFieldBuilder startTextField(ITextComponent name, String value);
	
	TextDescriptionBuilder startTextDescription(ITextComponent text);
	
	TextDescriptionBuilder startTextDescription(Supplier<ITextComponent> textSupplier);
	
	<T extends Enum<?>> EnumSelectorBuilder<T> startEnumSelector(ITextComponent name, T value);
	
	<T> SelectorBuilder<T> startSelector(ITextComponent name, T[] value, T var3);
	
	IntFieldBuilder startIntField(ITextComponent name, int value);
	
	LongFieldBuilder startLongField(ITextComponent name, long value);
	
	FloatFieldBuilder startFloatField(ITextComponent name, float value);
	
	DoubleFieldBuilder startDoubleField(ITextComponent name, double value);
	
	IntSliderBuilder startIntSlider(ITextComponent name, int value, int min, int max);
	
	LongSliderBuilder startLongSlider(ITextComponent name, long value, long min, long max);
	
	FloatSliderBuilder startFloatSlider(ITextComponent name, float value, float min, float max);
	
	DoubleSliderBuilder startDoubleSlider(ITextComponent name, double value, double min, double max);
	
	KeyCodeBuilder startModifierKeyCodeField(ITextComponent name, ModifierKeyCode value);
	
	default KeyCodeBuilder startKeyCodeField(
	  ITextComponent name, InputMappings.Input value
	) {
		return this.startModifierKeyCodeField(
		  name, ModifierKeyCode.of(value, Modifier.none())).setAllowModifiers(false);
	}
	
	<T> ComboBoxFieldBuilder<T> startComboBox(
	  ITextComponent name, ITypeWrapper<T> typeWrapper, T value
	);
	
	<V, E extends AbstractListListEntry<V, ?, E>, C,
	  CE extends AbstractConfigListEntry<C> & IChildListEntry>
	  DecoratedListEntryBuilder<V, E, C, CE> makeDecoratedList(
		 ITextComponent name, E listEntry, CE captionEntry, Pair<C, List<V>> value);
}

