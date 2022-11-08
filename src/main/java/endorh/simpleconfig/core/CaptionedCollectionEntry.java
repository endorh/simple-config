package endorh.simpleconfig.core;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.Config.Entry;
import endorh.simpleconfig.api.AtomicEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.EntryTag;
import endorh.simpleconfig.api.entry.CaptionedCollectionEntryBuilder;
import endorh.simpleconfig.core.BackingField.BackingFieldBinding;
import endorh.simpleconfig.core.BackingField.BackingFieldBuilder;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.gui.entries.AbstractListListEntry;
import endorh.simpleconfig.ui.impl.builders.CaptionedListEntryBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import endorh.simpleconfig.yaml.NonConfigMap;
import net.minecraft.network.chat.Component;
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

public class CaptionedCollectionEntry<
  V, C, G, CV, CC, CG
> extends AbstractConfigEntry<Pair<CV, V>, Pair<CC, C>, Pair<CG, List<G>>> {
	
	protected final AbstractConfigEntry<V, C, List<G>> listEntry;
	protected final AbstractConfigEntry<CV, CC, CG> captionEntry;
	
	protected CaptionedCollectionEntry(
	  ConfigEntryHolder parent, String name, Pair<CV, V> value,
	  AbstractConfigEntry<V, C, List<G>> listEntry,
	  AbstractConfigEntry<CV, CC, CG> captionEntry
	) {
		super(parent, name, value);
		this.listEntry = listEntry;
		this.captionEntry = captionEntry;
	}
	
	@SuppressWarnings("unchecked") protected <E extends AbstractConfigEntry<CV, CC, CG> & AtomicEntry<CG>> E getCaptionEntry() {
		return (E) captionEntry;
	}
	
	public static class Builder<
	  V, C, G,
	  S extends ConfigEntryBuilder<V, C, List<G>, S>,
	  CV, CC, CG,
	  CS extends ConfigEntryBuilder<CV, CC, CG, CS> & AtomicEntryBuilder
	> extends AbstractConfigEntryBuilder<Pair<CV, V>, Pair<CC, C>, Pair<CG, List<G>>,
	  CaptionedCollectionEntry<V, C, G, CV, CC, CG>,
	  CaptionedCollectionEntryBuilder<V, C, G, S, CV, CC, CG, CS>,
	  Builder<V, C, G, S, CV, CC, CG, CS>
	> implements CaptionedCollectionEntryBuilder<V, C, G, S, CV, CC, CG, CS> {
		protected AbstractConfigEntryBuilder<V, C, List<G>, ?, S, ?> collectionBuilder;
		protected AbstractConfigEntryBuilder<CV, CC, CG, ?, CS, ?> captionBuilder;
		
		public Builder(Pair<CV, V> value, S collectionBuilder, CS captionBuilder) {
			super(value, Pair.class);
			if (!(collectionBuilder instanceof AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?>))
				throw new IllegalArgumentException("`collectionBuilder` is not an `AbstractConfigEntryBuilder`!");
			if (!(captionBuilder instanceof AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?>))
				throw new IllegalArgumentException("`captionBuilder` is not an `AbstractConfigEntryBuilder`!");
			//noinspection unchecked
			this.collectionBuilder = (AbstractConfigEntryBuilder<V, C, List<G>, ?, S, ?>) collectionBuilder;
			//noinspection unchecked
			this.captionBuilder = (AbstractConfigEntryBuilder<CV, CC, CG, ?, CS, ?>) captionBuilder;
		}
		
		@Override @Contract(pure=true)
		public @NotNull CaptionedCollectionEntryBuilder<V, C, G, S, CV, CC, CG, CS> captionField(String name) {
			return field(name, Pair::getKey, captionBuilder.typeClass);
		}
		
		@Override @Contract(pure=true)
		public @NotNull CaptionedCollectionEntryBuilder<V, C, G, S, CV, CC, CG, CS> collectionField(String name) {
			return field(name, Pair::getValue, collectionBuilder.typeClass);
		}
		
		@Override @Contract(pure=true)
		public @NotNull CaptionedCollectionEntryBuilder<V, C, G, S, CV, CC, CG, CS> collectionField() {
			return addField(BackingFieldBinding.sameName(BackingFieldBuilder.of(
			  Pair::getValue, collectionBuilder.typeClass)));
		}
		
		@Override @Contract(pure=true)
		public @NotNull CaptionedCollectionEntryBuilder<V, C, G, S, CV, CC, CG, CS> splitFields(String captionSuffix) {
			return addField(captionSuffix, Pair::getKey, captionBuilder.typeClass).collectionField();
		}
		
		@Override @Contract(pure=true)
		public @NotNull CaptionedCollectionEntryBuilder<V, C, G, S, CV, CC, CG, CS> splitFields(
		  String captionField, boolean fullFieldName
		) {
			if (!fullFieldName) return splitFields(captionField);
			return captionField(captionField).collectionField();
		}
		
		@Override @Contract(pure=true)
		public @NotNull CaptionedCollectionEntryBuilder<V, C, G, S, CV, CC, CG, CS> split_fields(String caption_suffix) {
			return add_field(caption_suffix, Pair::getKey, captionBuilder.typeClass).collectionField();
		}
		
		@Override protected CaptionedCollectionEntry<V, C, G, CV, CC, CG> buildEntry(
		  ConfigEntryHolder parent, String name
		) {
			final AbstractConfigEntry<V, C, List<G>> le = DummyEntryHolder.build(parent,
			                                                                     collectionBuilder);
			final AbstractConfigEntry<CV, CC, CG> ce = DummyEntryHolder.build(parent, captionBuilder);
			if (!(ce instanceof AtomicEntry)) throw new IllegalStateException(
			  "KeyEntryBuilder created non-key entry: " + captionBuilder.getClass().getCanonicalName());
			return new CaptionedCollectionEntry<>(parent, name, value, le, ce);
		}
		
		@Override protected Builder<V, C, G, S, CV, CC, CG, CS> createCopy(Pair<CV, V> value) {
			//noinspection unchecked
			return new Builder<>(
			  value, (S) collectionBuilder.copy(), (CS) captionBuilder.copy());
		}
	}
	
	@Override public Map<Object, Object> forActualConfig(@Nullable Pair<CC, C> value) {
		if (value == null) return null;
		return NonConfigMap.singleton(
		  captionEntry.forActualConfig(value.getKey()),
		  listEntry.forActualConfig(value.getValue()));
	}
	
	@Override public @Nullable Pair<CC, C> fromActualConfig(@Nullable Object value) {
		if (value instanceof Map<?, ?> m) {
			if (m.size() != 1) return null;
			Map.Entry<?, ?> entry = m.entrySet().stream().findFirst()
			  .orElseThrow(ConcurrentModificationException::new);
			CC key = captionEntry.fromActualConfig(entry.getKey());
			C col = listEntry.fromActualConfig(entry.getValue());
			return key != null && col != null? Pair.of(key, col) : null;
		} else if (value instanceof Config) {
			Optional<? extends Entry> opt = ((Config) value).entrySet().stream().findFirst();
			if (opt.isEmpty()) return null;
			Entry entry = opt.get();
			CC key = captionEntry.fromActualConfig(entry.getKey());
			C col = listEntry.fromActualConfig(entry.getValue());
			return key != null && col != null? Pair.of(key, col) : null;
		}
		return null;
	}
	
	@Override public List<Component> getErrorsFromGUI(Pair<CG, List<G>> value) {
		List<Component> errors = super.getErrorsFromGUI(value);
		errors.addAll(captionEntry.getErrorsFromGUI(value.getKey()));
		errors.addAll(listEntry.getErrorsFromGUI(value.getValue()));
		return errors;
	}
	
	@Override public Pair<CC, C> forConfig(
	  Pair<CV, V> value
	) {
		return Pair.of(captionEntry.forConfig(value.getKey()),
		               listEntry.forConfig(value.getValue()));
	}
	
	@Nullable @Override public Pair<CV, V> fromConfig(@Nullable Pair<CC, C> value) {
		if (value == null) return null;
		CV caption = captionEntry.fromConfig(value.getKey());
		V col = listEntry.fromConfig(value.getValue());
		return caption != null && col != null? Pair.of(caption, col) : null;
	}
	
	@Override public Pair<CG, List<G>> forGui(Pair<CV, V> value) {
		return Pair.of(captionEntry.forGui(value.getKey()),
		               listEntry.forGui(value.getValue()));
	}
	
	@Nullable @Override public Pair<CV, V> fromGui(@Nullable Pair<CG, List<G>> value) {
		if (value == null) return null;
		CV caption = captionEntry.fromGui(value.getKey());
		V col = listEntry.fromGui(value.getValue());
		return caption != null && col != null? Pair.of(caption, col) : null;
	}
	
	@Override public List<String> getConfigCommentTooltips() {
		List<String> tooltips = super.getConfigCommentTooltips();
		String captionTooltip = captionEntry.getConfigCommentTooltip();
		if (!captionTooltip.isEmpty()) tooltips.add("Caption: " + captionTooltip);
		tooltips.addAll(listEntry.getConfigCommentTooltips());
		return tooltips;
	}
	
	@OnlyIn(Dist.CLIENT) @SuppressWarnings("unchecked") protected <
	  LGE extends AbstractListListEntry<G, ?, LGE>,
	  CGE extends AbstractConfigListEntry<CG> & IChildListEntry>
	CaptionedListEntryBuilder<G, LGE, ?, CG, CGE, ?> makeGUIEntry(
	  ConfigFieldBuilder builder, Component name,
	  FieldBuilder<List<G>, ?, ?> listEntry, Pair<CG, List<G>> value
	) {
		final FieldBuilder<CG, CGE, ?> cge = getCaptionEntry().buildAtomicChildGUIEntry(builder);
		cge.setOriginal(value.getKey());
		return new CaptionedListEntryBuilder<>(
		  builder, name, value, (FieldBuilder<List<G>, LGE, ?>) listEntry, cge);
	}
	
	@OnlyIn(Dist.CLIENT) @Override public Optional<FieldBuilder<Pair<CG, List<G>>, ?, ?>> buildGUIEntry(
	  ConfigFieldBuilder builder
	) {
		listEntry.setDisplayName(getDisplayName());
		final CaptionedListEntryBuilder<G, ?, ?, CG, ?, ?>
		  entryBuilder = makeGUIEntry(
			 builder, getDisplayName(), listEntry.buildGUIEntry(builder).map(
				l -> l.withoutTags(EntryTag.NON_PERSISTENT)
		  ).orElseThrow(() -> new IllegalStateException("List entry has no GUI entry")), forGui(get()));
		return Optional.of(decorate(entryBuilder));
	}
}
