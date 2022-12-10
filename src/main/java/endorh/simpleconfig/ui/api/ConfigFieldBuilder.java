package endorh.simpleconfig.ui.api;

import endorh.simpleconfig.api.range.Range;
import endorh.simpleconfig.api.ui.hotkey.KeyBindMapping;
import endorh.simpleconfig.api.ui.math.Color;
import endorh.simpleconfig.core.entry.BeanProxy;
import endorh.simpleconfig.ui.gui.entries.AbstractListListEntry;
import endorh.simpleconfig.ui.gui.entries.EntryPairListListEntry;
import endorh.simpleconfig.ui.gui.entries.NestedListListEntry;
import endorh.simpleconfig.ui.gui.widget.combobox.wrapper.TypeWrapper;
import endorh.simpleconfig.ui.impl.ConfigEntryBuilderImpl;
import endorh.simpleconfig.ui.impl.builders.*;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public interface ConfigFieldBuilder {
	static ConfigFieldBuilder create() {
		return ConfigEntryBuilderImpl.create();
	}
	
	IntListBuilder startIntList(ITextComponent name, List<Integer> value);
	LongListBuilder startLongList(ITextComponent name, List<Long> value);
	FloatListBuilder startFloatList(ITextComponent name, List<Float> value);
	DoubleListBuilder startDoubleList(ITextComponent name, List<Double> value);
	
	StringListBuilder startStrList(ITextComponent name, List<String> value);
	
	SubCategoryBuilder startSubCategory(ITextComponent name);
	SubCategoryBuilder startSubCategory(ITextComponent name, List<FieldBuilder<?, ?, ?>> value);
	
	<T, HE extends AbstractConfigListEntry<T> & IChildListEntry,
	  HEB extends FieldBuilder<T, HE, HEB>
	> CaptionedSubCategoryBuilder<T, HE, HEB> startCaptionedSubCategory(ITextComponent name, HEB captionEntry);
	
	<B> BeanFieldBuilder<B> startBeanField(ITextComponent name, B value, BeanProxy<B> proxy);
	
	BooleanToggleBuilder startBooleanToggle(ITextComponent name, boolean value);
	ColorFieldBuilder startColorField(ITextComponent name, int value);
	
	<V, E extends AbstractConfigListEntry<V>> EntryListFieldBuilder<V, E> startEntryList(
	  ITextComponent name, List<V> value, Function<NestedListListEntry<V, E>, E> cellFactory);
	
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
	
	ButtonFieldBuilder startButton(ITextComponent name, Runnable action);
	<V, E extends AbstractConfigListEntry<V> & IChildListEntry,
	  B extends FieldBuilder<V, E, B>
	> EntryButtonFieldBuilder<V, E, B> startButton(ITextComponent name, B entry, Consumer<V> action);
	
	KeyBindFieldBuilder startKeyBindField(ITextComponent name, KeyBindMapping value);
	
	<T> ComboBoxFieldBuilder<T> startComboBox(
	  ITextComponent name, TypeWrapper<T> typeWrapper, T value);
	
	<V, E extends AbstractListListEntry<V, ?, E>, EB extends FieldBuilder<List<V>, E, ?>,
	  C, CE extends AbstractConfigListEntry<C> & IChildListEntry, CEB extends FieldBuilder<C, CE, ?>
	> CaptionedListEntryBuilder<V, E, EB, C, CE, CEB> startCaptionedList(
	  ITextComponent name, EB listEntry, CEB captionEntry, Pair<C, List<V>> value);
	
	<L, R, LE extends AbstractConfigListEntry<L> & IChildListEntry,
	  RE extends AbstractConfigListEntry<R> & IChildListEntry,
	  LEB extends FieldBuilder<L, LE, LEB>, REB extends FieldBuilder<R, RE, REB>
	> PairListEntryBuilder<L, R, LE, RE, LEB, REB> startPair(
	  ITextComponent name, LEB leftEntry, REB rightEntry, Pair<L, R> value);
	
	<L, R, M, LE extends AbstractConfigListEntry<L> & IChildListEntry,
	  ME extends AbstractConfigListEntry<M> & IChildListEntry,
	  RE extends AbstractConfigListEntry<R> & IChildListEntry,
	  LEB extends FieldBuilder<L, LE, LEB>,
	  MEB extends FieldBuilder<M, ME, MEB>,
	  REB extends FieldBuilder<R, RE, REB>
	> TripleListEntryBuilder<L, M, R, LE, ME, RE, LEB, MEB, REB> startTriple(
	  ITextComponent name, LEB leftEntry, MEB middleEntry, REB rightEntry, Triple<L, M, R> value);
	
	<V extends Comparable<V>, R extends Range<V, R>,
	  E extends AbstractConfigListEntry<V> & IChildListEntry
	> RangeListEntryBuilder<V, R, E> startRange(
	  ITextComponent name, R value, FieldBuilder<V, E, ?> entryBuilder
	);
	
	<V extends Comparable<V>, R extends Range<V, R>,
	  E extends AbstractConfigListEntry<V> & IChildListEntry
	> RangeListEntryBuilder<V, R, E> startRange(
		 ITextComponent name, R value, E minEntry, E maxEntry
	);
}

