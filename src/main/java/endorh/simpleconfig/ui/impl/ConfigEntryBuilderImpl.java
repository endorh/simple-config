package endorh.simpleconfig.ui.impl;

import endorh.simpleconfig.core.AbstractRange;
import endorh.simpleconfig.ui.api.*;
import endorh.simpleconfig.ui.gui.entries.AbstractListListEntry;
import endorh.simpleconfig.ui.gui.entries.EntryPairListListEntry;
import endorh.simpleconfig.ui.gui.entries.NestedListListEntry;
import endorh.simpleconfig.ui.gui.widget.combobox.wrapper.ITypeWrapper;
import endorh.simpleconfig.ui.impl.builders.*;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public class ConfigEntryBuilderImpl implements ConfigEntryBuilder {
	private ConfigEntryBuilderImpl() {}
	
	public static ConfigEntryBuilderImpl create() {
		return new ConfigEntryBuilderImpl();
	}
	
	@Override public IntListBuilder startIntList(ITextComponent name, List<Integer> value) {
		return new IntListBuilder(this, name, value);
	}
	
	@Override public LongListBuilder startLongList(ITextComponent name, List<Long> value) {
		return new LongListBuilder(this, name, value);
	}
	
	@Override public FloatListBuilder startFloatList(ITextComponent name, List<Float> value) {
		return new FloatListBuilder(this, name, value);
	}
	
	@Override public DoubleListBuilder startDoubleList(ITextComponent name, List<Double> value) {
		return new DoubleListBuilder(this, name, value);
	}
	
	@Override public StringListBuilder startStrList(ITextComponent name, List<String> value) {
		return new StringListBuilder(this, name, value);
	}
	
	@Override public SubCategoryBuilder startSubCategory(ITextComponent name) {
		return new SubCategoryBuilder(this, name);
	}
	
	@Override public SubCategoryBuilder startSubCategory(
	  ITextComponent name, List<AbstractConfigListEntry<?>> entries
	) {
		SubCategoryBuilder builder = new SubCategoryBuilder(this, name);
		builder.addAll(entries);
		return builder;
	}
	
	@Override public <T, HE extends AbstractConfigEntry<T> & IChildListEntry>
	CaptionedSubCategoryBuilder<T, HE> startCaptionedSubCategory(
	  ITextComponent name, HE captionEntry
	) {
		return new CaptionedSubCategoryBuilder<>(this, name, captionEntry);
	}
	
	@Override public BooleanToggleBuilder startBooleanToggle(ITextComponent name, boolean value) {
		return new BooleanToggleBuilder(this, name, value);
	}
	
	@Override public ColorFieldBuilder startColorField(ITextComponent name, int value) {
		return new ColorFieldBuilder(this, name, value);
	}
	
	@Override public <V, E extends AbstractConfigListEntry<V>> EntryListFieldBuilder<V, E> startEntryList(
	  ITextComponent name, List<V> value, Function<NestedListListEntry<V, E>, E> cellFactory
	) {
		return new EntryListFieldBuilder<>(this, name, value, cellFactory);
	}
	
	@Override public <K, V, KE extends AbstractConfigListEntry<K> & IChildListEntry,
	  VE extends AbstractConfigListEntry<V>> EntryPairListBuilder<K, V, KE, VE> startEntryPairList(
	  ITextComponent name, List<Pair<K, V>> value,
	  Function<EntryPairListListEntry<K, V, KE, VE>, Pair<KE, VE>> cellFactory
	) {
		return new EntryPairListBuilder<>(this, name, value, cellFactory);
	}
	
	@Override public TextFieldBuilder startTextField(ITextComponent name, String value) {
		return new TextFieldBuilder(this, name, value);
	}
	
	@Override public TextDescriptionBuilder startTextDescription(ITextComponent value) {
		return new TextDescriptionBuilder(
		  this, new StringTextComponent(UUID.randomUUID().toString()), () -> value);
	}
	
	@Override public TextDescriptionBuilder startTextDescription(
	  Supplier<ITextComponent> textSupplier
	) {
		return new TextDescriptionBuilder(
		  this, new StringTextComponent(UUID.randomUUID().toString()), textSupplier);
	}
	
	@Override public <T extends Enum<?>> EnumSelectorBuilder<T> startEnumSelector(
	  ITextComponent name, T value
	) {
		return new EnumSelectorBuilder<>(this, name, value);
	}
	
	@Override public <T> SelectorBuilder<T> startSelector(
	  ITextComponent name, T[] valuesArray, T value
	) {
		return new SelectorBuilder<>(this, name, valuesArray, value);
	}
	
	@Override public IntFieldBuilder startIntField(ITextComponent name, int value) {
		return new IntFieldBuilder(this, name, value);
	}
	
	@Override public LongFieldBuilder startLongField(ITextComponent name, long value) {
		return new LongFieldBuilder(this, name, value);
	}
	
	@Override public FloatFieldBuilder startFloatField(ITextComponent name, float value) {
		return new FloatFieldBuilder(this, name, value);
	}
	
	@Override public DoubleFieldBuilder startDoubleField(ITextComponent name, double value) {
		return new DoubleFieldBuilder(this, name, value);
	}
	
	@Override public IntSliderBuilder startIntSlider(
	  ITextComponent name, int value, int min, int max
	) {
		return new IntSliderBuilder(this, name, value, min, max);
	}
	
	@Override public LongSliderBuilder startLongSlider(
	  ITextComponent name, long value, long min, long max
	) {
		return new LongSliderBuilder(this, name, value, min, max);
	}
	
	@Override public FloatSliderBuilder startFloatSlider(
	  ITextComponent name, float value, float min, float max
	) {
		return new FloatSliderBuilder(this, name, value, min, max);
	}
	
	@Override public DoubleSliderBuilder startDoubleSlider(
	  ITextComponent name, double value, double min, double max
	) {
		return new DoubleSliderBuilder(this, name, value, min, max);
	}
	
	@Override public KeyCodeBuilder startModifierKeyCodeField(
	  ITextComponent name, ModifierKeyCode value
	) {
		return new KeyCodeBuilder(this, name, value);
	}
	
	@Override public <T> ComboBoxFieldBuilder<T> startComboBox(
	  ITextComponent name, ITypeWrapper<T> typeWrapper, T value
	) {
		return new ComboBoxFieldBuilder<>(this, name, value, typeWrapper);
	}
	
	@Override public <V, E extends AbstractListListEntry<V, ?, E>,
	  C, CE extends AbstractConfigListEntry<C> & IChildListEntry>
	CaptionedListEntryBuilder<V, E, C, CE> startCaptionedList(
	  ITextComponent name, E listEntry, CE captionEntry, Pair<C, List<V>> value
	) {
		return new CaptionedListEntryBuilder<>(
		  this, name, value, listEntry, captionEntry);
	}
	
	@Override public <
	  L, R, LE extends AbstractConfigListEntry<L> & IChildListEntry,
	  RE extends AbstractConfigListEntry<R> & IChildListEntry
	> PairListEntryBuilder<L, R, LE, RE> startPair(
	  ITextComponent name, LE leftEntry, RE rightEntry, Pair<L, R> value
	) {
		return new PairListEntryBuilder<>(this, name, value, leftEntry, rightEntry);
	}
	
	@Override public <
	  L, R, M, LE extends AbstractConfigListEntry<L> & IChildListEntry,
	  ME extends AbstractConfigListEntry<M> & IChildListEntry,
	  RE extends AbstractConfigListEntry<R> & IChildListEntry
	> TripleListEntryBuilder<L, M, R, LE, ME, RE> startTriple(
	  ITextComponent name, LE leftEntry, ME middleEntry, RE rightEntry, Triple<L, M, R> value
	) {
		return new TripleListEntryBuilder<>(this, name, value, leftEntry, middleEntry, rightEntry);
	}
	
	@Override public <
	  V extends Comparable<V>, R extends AbstractRange<V, R>,
	  E extends AbstractConfigListEntry<V> & IChildListEntry
	> RangeListEntryBuilder<V, R, E> startRange(
	  ITextComponent name, R value, FieldBuilder<V, E, ?> entryBuilder
	) {
		return new RangeListEntryBuilder<>(this, name, value, entryBuilder);
	}
	
	@Override public <
	  V extends Comparable<V>, R extends AbstractRange<V, R>,
	  E extends AbstractConfigListEntry<V> & IChildListEntry
	> RangeListEntryBuilder<V, R, E> startRange(
	  ITextComponent name, R value, E minEntry, E maxEntry
	) {
		return new RangeListEntryBuilder<>(this, name, value, minEntry, maxEntry);
	}
}

