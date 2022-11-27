package endorh.simpleconfig.ui.api;

import endorh.simpleconfig.api.AbstractRange;
import endorh.simpleconfig.api.ui.hotkey.KeyBindMapping;
import endorh.simpleconfig.api.ui.math.Color;
import endorh.simpleconfig.core.entry.BeanProxy;
import endorh.simpleconfig.ui.gui.entries.AbstractListListEntry;
import endorh.simpleconfig.ui.gui.entries.EntryPairListListEntry;
import endorh.simpleconfig.ui.gui.entries.NestedListListEntry;
import endorh.simpleconfig.ui.gui.widget.combobox.wrapper.TypeWrapper;
import endorh.simpleconfig.ui.impl.ConfigEntryBuilderImpl;
import endorh.simpleconfig.ui.impl.builders.*;
import net.minecraft.network.chat.Component;
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
	
	IntListBuilder startIntList(Component name, List<Integer> value);
	LongListBuilder startLongList(Component name, List<Long> value);
	FloatListBuilder startFloatList(Component name, List<Float> value);
	DoubleListBuilder startDoubleList(Component name, List<Double> value);
	
	StringListBuilder startStrList(Component name, List<String> value);
	
	SubCategoryBuilder startSubCategory(Component name);
	SubCategoryBuilder startSubCategory(Component name, List<FieldBuilder<?, ?, ?>> value);
	
	<T, HE extends AbstractConfigListEntry<T> & IChildListEntry,
	  HEB extends FieldBuilder<T, HE, HEB>
	> CaptionedSubCategoryBuilder<T, HE, HEB> startCaptionedSubCategory(Component name, HEB captionEntry);
	
	<B> BeanFieldBuilder<B> startBeanField(Component name, B value, BeanProxy<B> proxy);
	
	BooleanToggleBuilder startBooleanToggle(Component name, boolean value);
	ColorFieldBuilder startColorField(Component name, int value);
	
	<V, E extends AbstractConfigListEntry<V>> EntryListFieldBuilder<V, E> startEntryList(
	  Component name, List<V> value, Function<NestedListListEntry<V, E>, E> cellFactory);
	
	<K, V, KE extends AbstractConfigListEntry<K> & IChildListEntry,
	  VE extends AbstractConfigListEntry<V>> EntryPairListBuilder<K, V, KE, VE>
	startEntryPairList(
	  Component name, List<Pair<K, V>> value,
	  Function<EntryPairListListEntry<K, V, KE, VE>, Pair<KE, VE>> cellFactory
	);
	
	default ColorFieldBuilder startColorField(
	  Component fieldNameKey, net.minecraft.network.chat.TextColor color
	) {
		return this.startColorField(fieldNameKey, color.getValue());
	}
	default ColorFieldBuilder startColorField(Component fieldNameKey, Color color) {
		return this.startColorField(fieldNameKey, color.getColor() & 0xFFFFFF);
	}
	default ColorFieldBuilder startAlphaColorField(Component fieldNameKey, int value) {
		return this.startColorField(fieldNameKey, value).setAlphaMode(true);
	}
	default ColorFieldBuilder startAlphaColorField(Component fieldNameKey, Color color) {
		return this.startColorField(fieldNameKey, color.getColor());
	}
	
	TextFieldBuilder startTextField(Component name, String value);
	
	TextDescriptionBuilder startTextDescription(Component text);
	TextDescriptionBuilder startTextDescription(Supplier<Component> textSupplier);
	
	<T extends Enum<?>> EnumSelectorBuilder<T> startEnumSelector(Component name, T value);
	<T> SelectorBuilder<T> startSelector(Component name, T[] value, T var3);
	
	IntFieldBuilder startIntField(Component name, int value);
	LongFieldBuilder startLongField(Component name, long value);
	FloatFieldBuilder startFloatField(Component name, float value);
	DoubleFieldBuilder startDoubleField(Component name, double value);
	
	IntSliderBuilder startIntSlider(Component name, int value, int min, int max);
	LongSliderBuilder startLongSlider(Component name, long value, long min, long max);
	FloatSliderBuilder startFloatSlider(Component name, float value, float min, float max);
	DoubleSliderBuilder startDoubleSlider(Component name, double value, double min, double max);
	
	ButtonFieldBuilder startButton(Component name, Runnable action);
	<V, E extends AbstractConfigListEntry<V> & IChildListEntry,
	  B extends FieldBuilder<V, E, B>
	> EntryButtonFieldBuilder<V, E, B> startButton(Component name, B entry, Consumer<V> action);
	
	KeyBindFieldBuilder startKeyBindField(Component name, KeyBindMapping value);
	
	<T> ComboBoxFieldBuilder<T> startComboBox(
	  Component name, TypeWrapper<T> typeWrapper, T value);
	
	<V, E extends AbstractListListEntry<V, ?, E>, EB extends FieldBuilder<List<V>, E, ?>,
	  C, CE extends AbstractConfigListEntry<C> & IChildListEntry, CEB extends FieldBuilder<C, CE, ?>
	> CaptionedListEntryBuilder<V, E, EB, C, CE, CEB> startCaptionedList(
	  Component name, EB listEntry, CEB captionEntry, Pair<C, List<V>> value);
	
	<L, R, LE extends AbstractConfigListEntry<L> & IChildListEntry,
	  RE extends AbstractConfigListEntry<R> & IChildListEntry,
	  LEB extends FieldBuilder<L, LE, LEB>, REB extends FieldBuilder<R, RE, REB>
	> PairListEntryBuilder<L, R, LE, RE, LEB, REB> startPair(
	  Component name, LEB leftEntry, REB rightEntry, Pair<L, R> value);
	
	<L, R, M, LE extends AbstractConfigListEntry<L> & IChildListEntry,
	  ME extends AbstractConfigListEntry<M> & IChildListEntry,
	  RE extends AbstractConfigListEntry<R> & IChildListEntry,
	  LEB extends FieldBuilder<L, LE, LEB>,
	  MEB extends FieldBuilder<M, ME, MEB>,
	  REB extends FieldBuilder<R, RE, REB>
	> TripleListEntryBuilder<L, M, R, LE, ME, RE, LEB, MEB, REB> startTriple(
	  Component name, LEB leftEntry, MEB middleEntry, REB rightEntry, Triple<L, M, R> value);
	
	<V extends Comparable<V>, R extends AbstractRange<V, R>,
	  E extends AbstractConfigListEntry<V> & IChildListEntry
	> RangeListEntryBuilder<V, R, E> startRange(
	  Component name, R value, FieldBuilder<V, E, ?> entryBuilder
	);
	
	<V extends Comparable<V>, R extends AbstractRange<V, R>,
	  E extends AbstractConfigListEntry<V> & IChildListEntry
	> RangeListEntryBuilder<V, R, E> startRange(
		 Component name, R value, E minEntry, E maxEntry
	);
}

