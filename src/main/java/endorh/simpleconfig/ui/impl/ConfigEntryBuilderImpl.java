package endorh.simpleconfig.ui.impl;

import endorh.simpleconfig.api.range.Range;
import endorh.simpleconfig.api.ui.hotkey.KeyBindMapping;
import endorh.simpleconfig.core.entry.BeanProxy;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.gui.entries.AbstractListListEntry;
import endorh.simpleconfig.ui.gui.entries.EntryPairListListEntry;
import endorh.simpleconfig.ui.gui.entries.NestedListListEntry;
import endorh.simpleconfig.ui.gui.widget.combobox.wrapper.TypeWrapper;
import endorh.simpleconfig.ui.impl.builders.*;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public class ConfigEntryBuilderImpl implements ConfigFieldBuilder {
	private ConfigEntryBuilderImpl() {}
	
	public static ConfigEntryBuilderImpl create() {
		return new ConfigEntryBuilderImpl();
	}
	
	@Override public IntListBuilder startIntList(Component name, List<Integer> value) {
		return new IntListBuilder(this, name, value);
	}
	
	@Override public LongListBuilder startLongList(Component name, List<Long> value) {
		return new LongListBuilder(this, name, value);
	}
	
	@Override public FloatListBuilder startFloatList(Component name, List<Float> value) {
		return new FloatListBuilder(this, name, value);
	}
	
	@Override public DoubleListBuilder startDoubleList(Component name, List<Double> value) {
		return new DoubleListBuilder(this, name, value);
	}
	
	@Override public StringListBuilder startStrList(Component name, List<String> value) {
		return new StringListBuilder(this, name, value);
	}
	
	@Override public SubCategoryBuilder startSubCategory(Component name) {
		return new SubCategoryBuilder(this, name);
	}
	
	@Override public SubCategoryBuilder startSubCategory(
	  Component name, List<FieldBuilder<?, ?, ?>> entries
	) {
		SubCategoryBuilder builder = new SubCategoryBuilder(this, name);
		builder.addAll(entries);
		return builder;
	}
	
	@Override public <
	  T, HE extends AbstractConfigListEntry<T> & IChildListEntry,
	  HEB extends FieldBuilder<T, HE, HEB>
	> CaptionedSubCategoryBuilder<T, HE, HEB> startCaptionedSubCategory(
	  Component name, HEB captionEntry
	) {
		return new CaptionedSubCategoryBuilder<>(this, name, captionEntry);
	}
	
	@Override public <B> BeanFieldBuilder<B> startBeanField(
	  Component name, B value, BeanProxy<B> proxy
	) {
		return new BeanFieldBuilder<>(proxy, this, name, value);
	}
	
	@Override public BooleanToggleBuilder startBooleanToggle(Component name, boolean value) {
		return new BooleanToggleBuilder(this, name, value);
	}
	
	@Override public ColorFieldBuilder startColorField(Component name, int value) {
		return new ColorFieldBuilder(this, name, value);
	}
	
	@Override public <V, E extends AbstractConfigListEntry<V>> EntryListFieldBuilder<V, E> startEntryList(
	  Component name, List<V> value, Function<NestedListListEntry<V, E>, E> cellFactory
	) {
		return new EntryListFieldBuilder<>(this, name, value, cellFactory);
	}
	
	@Override public <K, V, KE extends AbstractConfigListEntry<K> & IChildListEntry,
	  VE extends AbstractConfigListEntry<V>> EntryPairListBuilder<K, V, KE, VE> startEntryPairList(
	  Component name, List<Pair<K, V>> value,
	  Function<EntryPairListListEntry<K, V, KE, VE>, Pair<KE, VE>> cellFactory
	) {
		return new EntryPairListBuilder<>(this, name, value, cellFactory);
	}
	
	@Override public TextFieldBuilder startTextField(Component name, String value) {
		return new TextFieldBuilder(this, name, value);
	}
	
	@Override public TextDescriptionBuilder startTextDescription(Component value) {
		return new TextDescriptionBuilder(
		  this, Component.literal(UUID.randomUUID().toString()), () -> value);
	}
	
	@Override public TextDescriptionBuilder startTextDescription(
	  Supplier<Component> textSupplier
	) {
		return new TextDescriptionBuilder(
		  this, Component.literal(UUID.randomUUID().toString()), textSupplier);
	}
	
	@Override public <T extends Enum<?>> EnumSelectorBuilder<T> startEnumSelector(
	  Component name, T value
	) {
		return new EnumSelectorBuilder<>(this, name, value);
	}
	
	@Override public <T> SelectorBuilder<T> startSelector(
	  Component name, T[] valuesArray, T value
	) {
		return new SelectorBuilder<>(this, name, valuesArray, value);
	}
	
	@Override public IntFieldBuilder startIntField(Component name, int value) {
		return new IntFieldBuilder(this, name, value);
	}
	
	@Override public LongFieldBuilder startLongField(Component name, long value) {
		return new LongFieldBuilder(this, name, value);
	}
	
	@Override public FloatFieldBuilder startFloatField(Component name, float value) {
		return new FloatFieldBuilder(this, name, value);
	}
	
	@Override public DoubleFieldBuilder startDoubleField(Component name, double value) {
		return new DoubleFieldBuilder(this, name, value);
	}
	
	@Override public IntSliderBuilder startIntSlider(
	  Component name, int value, int min, int max
	) {
		return new IntSliderBuilder(this, name, value, min, max);
	}
	
	@Override public LongSliderBuilder startLongSlider(
	  Component name, long value, long min, long max
	) {
		return new LongSliderBuilder(this, name, value, min, max);
	}
	
	@Override public FloatSliderBuilder startFloatSlider(
	  Component name, float value, float min, float max
	) {
		return new FloatSliderBuilder(this, name, value, min, max);
	}
	
	@Override public DoubleSliderBuilder startDoubleSlider(
	  Component name, double value, double min, double max
	) {
		return new DoubleSliderBuilder(this, name, value, min, max);
	}
	
	@Override public ButtonFieldBuilder startButton(Component name, Runnable action) {
		return new ButtonFieldBuilder(this, name, action);
	}
	
	@Override public <
	  V, E extends AbstractConfigListEntry<V> & IChildListEntry, B extends FieldBuilder<V, E, B>
	> EntryButtonFieldBuilder<V, E, B> startButton(
	  Component name, B entry, Consumer<V> action
	) {
		return new EntryButtonFieldBuilder<>(this, name, entry, action);
	}
	
	@Override public KeyBindFieldBuilder startKeyBindField(
	  Component name, KeyBindMapping value
	) {
		return new KeyBindFieldBuilder(this, name, value);
	}
	
	@Override public <T> ComboBoxFieldBuilder<T> startComboBox(
	  Component name, TypeWrapper<T> typeWrapper, T value
	) {
		return new ComboBoxFieldBuilder<>(this, name, value, typeWrapper);
	}
	
	@Override public <
	  V, E extends AbstractListListEntry<V, ?, E>, EB extends FieldBuilder<List<V>, E, ?>,
	  C, CE extends AbstractConfigListEntry<C> & IChildListEntry, CEB extends FieldBuilder<C, CE, ?>
	> CaptionedListEntryBuilder<V, E, EB, C, CE, CEB> startCaptionedList(
	  Component name, EB listEntry, CEB captionEntry, Pair<C, List<V>> value
	) {
		return new CaptionedListEntryBuilder<>(
		  this, name, value, listEntry, captionEntry);
	}
	
	@Override public <
	  L, R, LE extends AbstractConfigListEntry<L> & IChildListEntry,
	  RE extends AbstractConfigListEntry<R> & IChildListEntry,
	  LEB extends FieldBuilder<L, LE, LEB>, REB extends FieldBuilder<R, RE, REB>
	> PairListEntryBuilder<L, R, LE, RE, LEB, REB> startPair(
	  Component name, LEB leftEntry, REB rightEntry, Pair<L, R> value
	) {
		return new PairListEntryBuilder<>(this, name, value, leftEntry, rightEntry);
	}
	
	@Override public <
	  L, R, M, LE extends AbstractConfigListEntry<L> & IChildListEntry,
	  ME extends AbstractConfigListEntry<M> & IChildListEntry,
	  RE extends AbstractConfigListEntry<R> & IChildListEntry,
	  LEB extends FieldBuilder<L, LE, LEB>,
	  MEB extends FieldBuilder<M, ME, MEB>,
	  REB extends FieldBuilder<R, RE, REB>
	> TripleListEntryBuilder<L, M, R, LE, ME, RE, LEB, MEB, REB> startTriple(
	  Component name, LEB leftEntry, MEB middleEntry, REB rightEntry, Triple<L, M, R> value
	) {
		return new TripleListEntryBuilder<>(this, name, value, leftEntry, middleEntry, rightEntry);
	}
	
	@Override public <
	  V extends Comparable<V>, R extends Range<V, R>,
	  E extends AbstractConfigListEntry<V> & IChildListEntry
	> RangeListEntryBuilder<V, R, E> startRange(
	  Component name, R value, FieldBuilder<V, E, ?> entryBuilder
	) {
		return new RangeListEntryBuilder<>(this, name, value, entryBuilder);
	}
	
	@Override public <
	  V extends Comparable<V>, R extends Range<V, R>,
	  E extends AbstractConfigListEntry<V> & IChildListEntry
	> RangeListEntryBuilder<V, R, E> startRange(
	  Component name, R value, E minEntry, E maxEntry
	) {
		return new RangeListEntryBuilder<>(this, name, value, minEntry, maxEntry);
	}
}

