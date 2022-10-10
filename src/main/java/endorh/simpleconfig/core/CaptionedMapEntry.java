package endorh.simpleconfig.core;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.Config.Entry;
import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.EntryTag;
import endorh.simpleconfig.api.KeyEntryBuilder;
import endorh.simpleconfig.api.entry.CaptionedMapEntryBuilder;
import endorh.simpleconfig.api.entry.EntryMapEntryBuilder;
import endorh.simpleconfig.core.BackingField.BackingFieldBinding;
import endorh.simpleconfig.core.BackingField.BackingFieldBuilder;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.gui.entries.AbstractListListEntry;
import endorh.simpleconfig.ui.impl.builders.CaptionedListEntryBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import endorh.simpleconfig.yaml.NonConfigMap;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CaptionedMapEntry<K, V, KC, C, KG, G, CV, CC, CG>
  extends AbstractConfigEntry<Pair<CV, Map<K, V>>, Pair<CC, Map<KC, C>>, Pair<CG, List<Pair<KG, G>>>> {
	protected final EntryMapEntry<K, V, KC, C, KG, G, ?, ?> mapEntry;
	protected final AbstractConfigEntry<CV, CC, CG> captionEntry;
	
	protected CaptionedMapEntry(
	  ConfigEntryHolder parent, String name,
	  Pair<CV, Map<K, V>> value,
	  EntryMapEntry<K, V, KC, C, KG, G, ?, ?> mapEntry,
	  AbstractConfigEntry<CV, CC, CG> captionEntry
	) {
		super(parent, name, value);
		this.mapEntry = mapEntry;
		this.captionEntry = captionEntry;
	}
	
	@SuppressWarnings("unchecked") protected <E extends AbstractConfigEntry<CV, CC, CG> & IKeyEntry<CG>> E getCaptionEntry() {
		return (E) captionEntry;
	}
	
	public static class Builder<K, V, KC, C, KG, G,
	  CV, CC, CG, CS extends ConfigEntryBuilder<CV, CC, CG, CS> & KeyEntryBuilder<CG>
	> extends AbstractConfigEntryBuilder<
	  Pair<CV, Map<K, V>>, Pair<CC, Map<KC, C>>, Pair<CG, List<Pair<KG, G>>>,
	  CaptionedMapEntry<K, V, KC, C, KG, G, CV, CC, CG>,
	  CaptionedMapEntryBuilder<K, V, KC, C, KG, G,  EntryMapEntry.Builder<
	    K, V, KC, C, KG, G, ?, ?, ?, ?>, CV, CC, CG, CS>,
	  CaptionedMapEntry.Builder<K, V, KC, C, KG, G, CV, CC, CG, CS>
	> implements CaptionedMapEntryBuilder<
	  K, V, KC, C, KG, G, EntryMapEntry.Builder<K, V, KC, C, KG, G, ?, ?, ?, ?>,
	  CV, CC, CG, CS
	> {
		protected EntryMapEntry.Builder<K, V, KC, C, KG, G, ?, ?, ?, ?> mapEntryBuilder;
		protected AbstractConfigEntryBuilder<CV, CC, CG, ?, CS, ?> captionEntryBuilder;
		
		@SuppressWarnings("unchecked") public Builder(
		  Pair<CV, Map<K, V>> value,
		  EntryMapEntryBuilder<K, V, KC, C, KG, G, ?, ?> mapEntryBuilder,
		  CS captionEntryBuilder
		) {
			this(value, (EntryMapEntry.Builder<K, V, KC, C, KG, G, ?, ?, ?, ?>) mapEntryBuilder, captionEntryBuilder);
		}
		
		public Builder(
		  Pair<CV, Map<K, V>> value,
		  EntryMapEntry.Builder<K, V, KC, C, KG, G, ?, ?, ?, ?> mapEntryBuilder,
		  CS captionEntryBuilder
		) {
			super(value, Pair.class);
			this.mapEntryBuilder = mapEntryBuilder;
			if (!(captionEntryBuilder instanceof AbstractConfigEntryBuilder)) throw new IllegalArgumentException(
			  "ConfigEntryBuilder is not subclass of AbstractConfigEntryBuilder");
			//noinspection unchecked
			this.captionEntryBuilder = (AbstractConfigEntryBuilder<CV, CC, CG, ?, CS, ?>) captionEntryBuilder;
		}
		
		public Builder(
		  Pair<CV, Map<K, V>> value,
		  EntryMapEntry.Builder<K, V, KC, C, KG, G, ?, ?, ?, ?> mapBuilder,
		  AbstractConfigEntryBuilder<CV, CC, CG, ?, CS, ?> captionBuilder
		) {
			super(value, Pair.class);
			mapEntryBuilder = mapBuilder;
			captionEntryBuilder = captionBuilder;
		}
		
		@Override @Contract(pure=true)
		public CaptionedMapEntryBuilder<K, V, KC, C, KG, G, EntryMapEntry.Builder<K, V, KC, C, KG, G, ?, ?, ?, ?>, CV, CC, CG, CS> captionField(
		  String name
		) {
			return field(name, Pair::getKey, captionEntryBuilder.typeClass);
		}
		
		@Override @Contract(pure=true)
		public @NotNull CaptionedMapEntryBuilder<K, V, KC, C, KG, G, EntryMapEntry.Builder<K, V, KC, C, KG, G, ?, ?, ?, ?>, CV, CC, CG, CS> mapField(String name) {
			return field(name, Pair::getValue, mapEntryBuilder.typeClass);
		}
		
		@Override public CaptionedMapEntryBuilder<K, V, KC, C, KG, G, EntryMapEntry.Builder<K, V, KC, C, KG, G, ?, ?, ?, ?>, CV, CC, CG, CS> mapField() {
			return addField(BackingFieldBinding.sameName(BackingFieldBuilder.of(
			  Pair::getValue, mapEntryBuilder.typeClass)));
		}
		
		@Override @Contract(pure=true)
		public CaptionedMapEntryBuilder<K, V, KC, C, KG, G, EntryMapEntry.Builder<K, V, KC, C, KG, G, ?, ?, ?, ?>, CV, CC, CG, CS> splitFields(
		  String captionSuffix
		) {
			return addField(captionSuffix, Pair::getKey, captionEntryBuilder.typeClass).mapField();
		}
		
		@Override @Contract(pure=true)
		public CaptionedMapEntryBuilder<K, V, KC, C, KG, G, EntryMapEntry.Builder<K, V, KC, C, KG, G, ?, ?, ?, ?>, CV, CC, CG, CS> splitFields(
		  String captionField, boolean fullFieldName
		) {
			if (!fullFieldName) return splitFields(captionField);
			return captionField(captionField).mapField();
		}
		
		@Override @Contract(pure=true)
		public CaptionedMapEntryBuilder<K, V, KC, C, KG, G, EntryMapEntry.Builder<K, V, KC, C, KG, G, ?, ?, ?, ?>, CV, CC, CG, CS> split_fields(
		  String caption_suffix
		) {
			return add_field(caption_suffix, Pair::getKey, captionEntryBuilder.typeClass).mapField();
		}
		
		@Override protected CaptionedMapEntry<K, V, KC, C, KG, G, CV, CC, CG> buildEntry(
		  ConfigEntryHolder parent, String name
		) {
			final EntryMapEntry<K, V, KC, C, KG, G, ?, ?> me = DummyEntryHolder.build(parent, mapEntryBuilder);
			final AbstractConfigEntry<CV, CC, CG> ce = DummyEntryHolder.build(parent, captionEntryBuilder);
			if (!(ce instanceof IKeyEntry)) throw new IllegalStateException(
			  "KeyEntryBuilder created a non-key entry: " + captionEntryBuilder.getClass().getCanonicalName());
			return new CaptionedMapEntry<>(parent, name, value, me, ce);
		}
		
		@Override protected Builder<K, V, KC, C, KG, G, CV, CC, CG, CS> createCopy() {
			EntryMapEntry.Builder<K, V, KC, C, KG, G, ?, ?, ?, ?> mb = mapEntryBuilder.copy();
			AbstractConfigEntryBuilder<CV, CC, CG, ?, CS, ?> ceb = captionEntryBuilder.copy();
			return new Builder<>(value, mb, ceb);
		}
	}
	
	@Override public Map<Object, Object> forActualConfig(@Nullable Pair<CC, Map<KC, C>> value) {
		if (value == null) return null;
		return NonConfigMap.singleton(
		  captionEntry.forActualConfig(value.getKey()),
		  mapEntry.forActualConfig(value.getValue()));
	}
	
	@Override public @Nullable Pair<CC, Map<KC, C>> fromActualConfig(@Nullable Object value) {
		if (value instanceof Map) {
			Map<?, ?> m = (Map<?, ?>) value;
			if (m.size() != 1) return null;
			Map.Entry<?, ?> entry = m.entrySet().stream().findFirst()
			  .orElseThrow(ConcurrentModificationException::new);
			CC key = captionEntry.fromActualConfig(entry.getKey());
			Map<KC, C> map = mapEntry.fromActualConfig(entry.getValue());
			return key != null && map != null? Pair.of(key, map) : null;
		} else if (value instanceof Config) {
			Optional<? extends Entry> opt = ((Config) value).entrySet().stream().findFirst();
			if (!opt.isPresent()) return null;
			Entry entry = opt.get();
			CC key = captionEntry.fromActualConfig(entry.getKey());
			Map<KC, C> map = mapEntry.fromActualConfig(entry.getValue());
			return key != null && map != null? Pair.of(key, map) : null;
		}
		return null;
	}
	
	@Override public List<ITextComponent> getErrorsFromGUI(Pair<CG, List<Pair<KG, G>>> value) {
		List<ITextComponent> errors = super.getErrorsFromGUI(value);
		errors.addAll(captionEntry.getErrorsFromGUI(value.getKey()));
		errors.addAll(mapEntry.getErrorsFromGUI(value.getValue()));
		return errors;
	}
	
	@Override public Pair<CC, Map<KC, C>> forConfig(Pair<CV, Map<K, V>> value) {
		return Pair.of(captionEntry.forConfig(value.getKey()),
		               mapEntry.forConfig(value.getValue()));
	}
	
	@Override public @Nullable Pair<CV, Map<K, V>> fromConfig(
	  @Nullable Pair<CC, Map<KC, C>> value
	) {
		if (value == null) return null;
		CV caption = captionEntry.fromConfig(value.getKey());
		Map<K, V> map = mapEntry.fromConfig(value.getValue());
		return caption != null && map != null? Pair.of(caption, map) : null;
	}
	
	@Override public Pair<CG, List<Pair<KG, G>>> forGui(Pair<CV, Map<K, V>> value) {
		return Pair.of(captionEntry.forGui(value.getKey()), mapEntry.forGui(value.getValue()));
	}
	
	@Override public @Nullable Pair<CV, Map<K, V>> fromGui(@Nullable Pair<CG, List<Pair<KG, G>>> value) {
		if (value == null) return null;
		CV caption = captionEntry.fromGui(value.getKey());
		Map<K, V> map = mapEntry.fromGui(value.getValue());
		return caption != null && map != null? Pair.of(caption, map) : null;
	}
	
	@Override public List<String> getConfigCommentTooltips() {
		List<String> tooltips = super.getConfigCommentTooltips();
		String captionTooltip = captionEntry.getConfigCommentTooltip();
		if (!captionTooltip.isEmpty()) tooltips.add("Caption: " + captionTooltip);
		tooltips.addAll(mapEntry.getConfigCommentTooltips());
		return tooltips;
	}
	
	@OnlyIn(Dist.CLIENT) @SuppressWarnings("unchecked") protected <
	  MGE extends AbstractListListEntry<Pair<KG, G>, ?, MGE>,
	  CGE extends AbstractConfigListEntry<CG> & IChildListEntry>
	CaptionedListEntryBuilder<Pair<KG, G>, MGE, ?, CG, CGE, ?> makeGUIEntry(
	  ConfigFieldBuilder builder, ITextComponent name,
	  FieldBuilder<List<Pair<KG, G>>, ?, ?> mapEntry, Pair<CG, List<Pair<KG, G>>> value
	) {
		final FieldBuilder<CG, CGE, ?> cge = getCaptionEntry().buildChildGUIEntry(builder);
		cge.setOriginal(value.getKey());
		return new CaptionedListEntryBuilder<>(
		  builder, name, value, (FieldBuilder<List<Pair<KG, G>>, MGE, ?>) mapEntry, cge);
	}
	
	@OnlyIn(Dist.CLIENT) @Override public Optional<FieldBuilder<Pair<CG, List<Pair<KG, G>>>, ?, ?>> buildGUIEntry(
	  ConfigFieldBuilder builder
	) {
		mapEntry.setDisplayName(getDisplayName());
		final Optional<FieldBuilder<List<Pair<KG, G>>, ?, ?>> opt = mapEntry.buildGUIEntry(builder);
		if (!opt.isPresent()) throw new IllegalStateException("List entry has no GUI entry");
		final FieldBuilder<List<Pair<KG, G>>, ?, ?> mapGUIEntry = opt.get();
		mapGUIEntry.withoutTags(EntryTag.NON_PERSISTENT);
		final CaptionedListEntryBuilder<Pair<KG, G>, ?, ?, CG, ?, ?> entryBuilder =
		  makeGUIEntry(builder, getDisplayName(), mapGUIEntry, forGui(get()));
		return Optional.of(decorate(entryBuilder));
	}
}
