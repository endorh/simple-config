package endorh.simpleconfig.core;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.Config.Entry;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.gui.entries.AbstractListListEntry;
import endorh.simpleconfig.ui.impl.builders.CaptionedListEntryBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import endorh.simpleconfig.yaml.NonConfigMap;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CaptionedMapEntry<K, V, KC, C, KG, G,
  E extends AbstractConfigEntry<V, C, G>,
  B extends AbstractConfigEntryBuilder<V, C, G, E, B>,
  KE extends AbstractConfigEntry<K, KC, KG> & IKeyEntry<KG>,
  KB extends AbstractConfigEntryBuilder<K, KC, KG, KE, KB>,
  MB extends EntryMapEntry.Builder<K, V, KC, C, KG, G, E, B, KE, KB>,
  CV, CC, CG, CE extends AbstractConfigEntry<CV, CC, CG> & IKeyEntry<CG>>
  extends AbstractConfigEntry<Pair<CV, Map<K, V>>, Pair<CC, Map<KC, C>>, Pair<CG, List<Pair<KG, G>>>
  > {
	
	protected final EntryMapEntry<K, V, KC, C, KG, G, E, B, KE, KB> mapEntry;
	protected final CE captionEntry;
	
	protected CaptionedMapEntry(
	  ISimpleConfigEntryHolder parent, String name,
	  Pair<CV, Map<K, V>> value, EntryMapEntry<K, V, KC, C, KG, G, E, B, KE, KB> mapEntry, CE captionEntry
	) {
		super(parent, name, value);
		this.mapEntry = mapEntry;
		this.captionEntry = captionEntry;
	}
	
	public static class Builder<K, V, KC, C, KG, G,
	  E extends AbstractConfigEntry<V, C, G>,
	  B extends AbstractConfigEntryBuilder<V, C, G, E, B>,
	  KE extends AbstractConfigEntry<K, KC, KG> & IKeyEntry<KG>,
	  KB extends AbstractConfigEntryBuilder<K, KC, KG, KE, KB>,
	  MB extends EntryMapEntry.Builder<K, V, KC, C, KG, G, E, B, KE, KB>,
	  CV, CC, CG, CE extends AbstractConfigEntry<CV, CC, CG> & IKeyEntry<CG>,
	  CB extends AbstractConfigEntryBuilder<CV, CC, CG, CE, CB>
	> extends AbstractConfigEntryBuilder<
	  Pair<CV, Map<K, V>>, Pair<CC, Map<KC, C>>, Pair<CG, List<Pair<KG, G>>>,
	  CaptionedMapEntry<K, V, KC, C, KG, G, E, B, KE, KB, MB, CV, CC, CG, CE>,
	  CaptionedMapEntry.Builder<K, V, KC, C, KG, G, E, B, KE, KB, MB, CV, CC, CG, CE, CB>
	> {
		protected MB mapEntryBuilder;
		protected CB captionEntryBuilder;
		
		public Builder(Pair<CV, Map<K, V>> value, MB mapEntryBuilder, CB captionEntryBuilder) {
			super(value, Pair.class);
			this.mapEntryBuilder = mapEntryBuilder;
			this.captionEntryBuilder = captionEntryBuilder;
		}
		
		@Override protected CaptionedMapEntry<K, V, KC, C, KG, G, E, B, KE, KB, MB, CV, CC, CG, CE> buildEntry(
		  ISimpleConfigEntryHolder parent, String name
		) {
			final EntryMapEntry<K, V, KC, C, KG, G, E, B, KE, KB> me = DummyEntryHolder.build(parent, mapEntryBuilder);
			// final EntryMapEntry<K, V, KC, C, KG, G, E, B, KE, KB> me =
			//   mapEntryBuilder.buildEntry(parent, name + "$val");
			final CE ce = DummyEntryHolder.build(parent, captionEntryBuilder);
			// final CE ce = captionEntryBuilder.buildEntry(parent, name + "$key");
			return new CaptionedMapEntry<>(parent, name, value, me, ce);
		}
		
		@Override protected Builder<K, V, KC, C, KG, G, E, B, KE, KB, MB, CV, CC, CG, CE, CB> createCopy() {
			//noinspection unchecked
			return new CaptionedMapEntry.Builder<>(
			  value, (MB) mapEntryBuilder.copy(), captionEntryBuilder.copy());
		}
	}
	
	@Override public Map<Object, Object> forActualConfig(@Nullable Pair<CC, Map<KC, C>> value) {
		if (value == null) return null;
		return Util.make(NonConfigMap.ofHashMap(1), m -> m.put(
		  captionEntry.forActualConfig(value.getKey()),
		  mapEntry.forActualConfig(value.getValue())));
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
	  ConfigEntryBuilder builder, ITextComponent name,
	  FieldBuilder<List<Pair<KG, G>>, ?, ?> mapEntry, Pair<CG, List<Pair<KG, G>>> value
	) {
		final FieldBuilder<CG, CGE, ?> cge = captionEntry.buildChildGUIEntry(builder);
		cge.setOriginal(value.getKey());
		return new CaptionedListEntryBuilder<>(
		  builder, name, value, (FieldBuilder<List<Pair<KG, G>>, MGE, ?>) mapEntry, cge);
	}
	
	@OnlyIn(Dist.CLIENT) @Override public Optional<FieldBuilder<Pair<CG, List<Pair<KG, G>>>, ?, ?>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		mapEntry.setDisplayName(getDisplayName());
		final Optional<FieldBuilder<List<Pair<KG, G>>, ?, ?>> opt = mapEntry.buildGUIEntry(builder);
		if (!opt.isPresent()) throw new IllegalStateException("List entry has no GUI entry");
		final FieldBuilder<List<Pair<KG, G>>, ?, ?> mapGUIEntry = opt.get();
		mapGUIEntry.withoutTags(EntryTag.NON_PERSISTENT);
		final Pair<CG, List<Pair<KG, G>>> gv = forGui(get());
		final CaptionedListEntryBuilder<Pair<KG, G>, ?, ?, CG, ?, ?> entryBuilder =
		  makeGUIEntry(builder, getDisplayName(), mapGUIEntry, forGui(get()));
		return Optional.of(decorate(entryBuilder));
	}
}
