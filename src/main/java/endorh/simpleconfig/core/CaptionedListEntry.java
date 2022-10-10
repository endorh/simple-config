package endorh.simpleconfig.core;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.Config.Entry;
import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.EntryTag;
import endorh.simpleconfig.api.KeyEntryBuilder;
import endorh.simpleconfig.api.entry.CaptionedListEntryBuilder;
import endorh.simpleconfig.api.entry.ListEntryBuilder;
import endorh.simpleconfig.core.BackingField.BackingFieldBinding;
import endorh.simpleconfig.core.BackingField.BackingFieldBuilder;
import endorh.simpleconfig.core.entry.AbstractListEntry;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.gui.entries.AbstractListListEntry;
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

public class CaptionedListEntry<
  V, C, G, CV, CC, CG
> extends AbstractConfigEntry<Pair<CV, List<V>>, Pair<CC, List<C>>, Pair<CG, List<G>>> {
	
	protected final AbstractListEntry<V, C, G, ?> listEntry;
	protected final AbstractConfigEntry<CV, CC, CG> captionEntry;
	
	protected CaptionedListEntry(
	  ConfigEntryHolder parent, String name,
	  Pair<CV, List<V>> value, AbstractListEntry<V, C, G, ?> listEntry, AbstractConfigEntry<CV, CC, CG> captionEntry
	) {
		super(parent, name, value);
		this.listEntry = listEntry;
		this.captionEntry = captionEntry;
	}
	
	@SuppressWarnings("unchecked") protected <E extends AbstractConfigEntry<CV, CC, CG> & IKeyEntry<CG>> E getCaptionEntry() {
		return (E) captionEntry;
	}
	
	public static class Builder<
	  V, C, G,
	  S extends ListEntryBuilder<V, C, G, S>,
	  B extends AbstractListEntry.Builder<V, C, G, ?, S, B>,
	  CV, CC, CG,
	  CS extends ConfigEntryBuilder<CV, CC, CG, CS> & KeyEntryBuilder<CG>,
	  CB extends AbstractConfigEntryBuilder<CV, CC, CG, ?, CS, CB> & KeyEntryBuilder<CG>
	> extends AbstractConfigEntryBuilder<Pair<CV, List<V>>, Pair<CC, List<C>>, Pair<CG, List<G>>,
	  CaptionedListEntry<V, C, G, CV, CC, CG>,
	  CaptionedListEntryBuilder<V, C, G, S, CV, CC, CG, CS>,
	  Builder<V, C, G, S, B, CV, CC, CG, CS, CB>
	> implements CaptionedListEntryBuilder<V, C, G, S, CV, CC, CG, CS> {
		protected B listEntryBuilder;
		protected CB captionEntryBuilder;
		
		@SuppressWarnings("unchecked") public <CBB extends ConfigEntryBuilder<CV, CC, CG, CBB> & KeyEntryBuilder<CG>> Builder(
		  Pair<CV, List<V>> value, ListEntryBuilder<V, C, G, ?> listEntryBuilder, CBB captionEntryBuilder
		) {
			this(value, (B) listEntryBuilder, (CB) captionEntryBuilder);
		}
		
		public Builder(Pair<CV, List<V>> value, B listEntryBuilder, CB captionEntryBuilder) {
			super(value, Pair.class);
			this.listEntryBuilder = listEntryBuilder;
			this.captionEntryBuilder = captionEntryBuilder;
		}
		
		@Override @Contract(pure=true)
		public @NotNull CaptionedListEntryBuilder<V, C, G, S, CV, CC, CG, CS> captionField(String name) {
			return field(name, Pair::getKey, captionEntryBuilder.typeClass);
		}
		
		@Override @Contract(pure=true)
		public @NotNull CaptionedListEntryBuilder<V, C, G, S, CV, CC, CG, CS> listField(String name) {
			return field(name, Pair::getValue, listEntryBuilder.typeClass);
		}
		
		@Override @Contract(pure=true)
		public @NotNull CaptionedListEntryBuilder<V, C, G, S, CV, CC, CG, CS> listField() {
			return addField(BackingFieldBinding.sameName(BackingFieldBuilder.of(
			  Pair::getValue, listEntryBuilder.typeClass)));
		}
		
		@Override @Contract(pure=true)
		public @NotNull CaptionedListEntryBuilder<V, C, G, S, CV, CC, CG, CS> splitFields(String captionSuffix) {
			return addField(captionSuffix, Pair::getKey, captionEntryBuilder.typeClass).listField();
		}
		
		@Override @Contract(pure=true)
		public @NotNull CaptionedListEntryBuilder<V, C, G, S, CV, CC, CG, CS> splitFields(
		  String captionField, boolean fullFieldName
		) {
			if (!fullFieldName) return splitFields(captionField);
			return captionField(captionField).listField();
		}
		
		@Override @Contract(pure=true)
		public @NotNull CaptionedListEntryBuilder<V, C, G, S, CV, CC, CG, CS> split_fields(String caption_suffix) {
			return add_field(caption_suffix, Pair::getKey, captionEntryBuilder.typeClass).listField();
		}
		
		@Override protected CaptionedListEntry<V, C, G, CV, CC, CG> buildEntry(
		  ConfigEntryHolder parent, String name
		) {
			final AbstractListEntry<V, C, G, ?> le = DummyEntryHolder.build(parent, listEntryBuilder);
			final AbstractConfigEntry<CV, CC, CG> ce = DummyEntryHolder.build(parent, captionEntryBuilder);
			if (!(ce instanceof IKeyEntry)) throw new IllegalStateException(
			  "KeyEntryBuilder created non-key entry: " + captionEntryBuilder.getClass().getCanonicalName());
			return new CaptionedListEntry<>(parent, name, value, le, ce);
		}
		
		@Override protected Builder<V, C, G, S, B, CV, CC, CG, CS, CB> createCopy() {
			return new Builder<>(value, listEntryBuilder.copy(), captionEntryBuilder.copy());
		}
	}
	
	@Override public Map<Object, Object> forActualConfig(@Nullable Pair<CC, List<C>> value) {
		if (value == null) return null;
		return NonConfigMap.singleton(
		  captionEntry.forActualConfig(value.getKey()),
		  listEntry.forActualConfig(value.getValue()));
	}
	
	@Override public @Nullable Pair<CC, List<C>> fromActualConfig(@Nullable Object value) {
		if (value instanceof Map) {
			Map<?, ?> m = (Map<?, ?>) value;
			if (m.size() != 1) return null;
			Map.Entry<?, ?> entry = m.entrySet().stream().findFirst()
			  .orElseThrow(ConcurrentModificationException::new);
			CC key = captionEntry.fromActualConfig(entry.getKey());
			List<C> list = listEntry.fromActualConfig(entry.getValue());
			return key != null && list != null? Pair.of(key, list) : null;
		} else if (value instanceof Config) {
			Optional<? extends Entry> opt = ((Config) value).entrySet().stream().findFirst();
			if (!opt.isPresent()) return null;
			Entry entry = opt.get();
			CC key = captionEntry.fromActualConfig(entry.getKey());
			List<C> list = listEntry.fromActualConfig(entry.getValue());
			return key != null && list != null? Pair.of(key, list) : null;
		}
		return null;
	}
	
	@Override public List<ITextComponent> getErrorsFromGUI(Pair<CG, List<G>> value) {
		List<ITextComponent> errors = super.getErrorsFromGUI(value);
		errors.addAll(captionEntry.getErrorsFromGUI(value.getKey()));
		errors.addAll(listEntry.getErrorsFromGUI(value.getValue()));
		return errors;
	}
	
	@Override public Pair<CC, List<C>> forConfig(
	  Pair<CV, List<V>> value
	) {
		return Pair.of(captionEntry.forConfig(value.getKey()),
		               listEntry.forConfig(value.getValue()));
	}
	
	@Nullable @Override public Pair<CV, List<V>> fromConfig(@Nullable Pair<CC, List<C>> value) {
		if (value == null) return null;
		CV caption = captionEntry.fromConfig(value.getKey());
		List<V> list = listEntry.fromConfig(value.getValue());
		return caption != null && list != null? Pair.of(caption, list) : null;
	}
	
	@Override public Pair<CG, List<G>> forGui(Pair<CV, List<V>> value) {
		return Pair.of(captionEntry.forGui(value.getKey()),
		               listEntry.forGui(value.getValue()));
	}
	
	@Nullable @Override public Pair<CV, List<V>> fromGui(@Nullable Pair<CG, List<G>> value) {
		if (value == null) return null;
		CV caption = captionEntry.fromGui(value.getKey());
		List<V> list = listEntry.fromGui(value.getValue());
		return caption != null && list != null? Pair.of(caption, list) : null;
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
	endorh.simpleconfig.ui.impl.builders.CaptionedListEntryBuilder<G, LGE, ?, CG, CGE, ?> makeGUIEntry(
	  ConfigFieldBuilder builder, ITextComponent name,
	  FieldBuilder<List<G>, ?, ?> listEntry, Pair<CG, List<G>> value
	) {
		final FieldBuilder<CG, CGE, ?> cge = getCaptionEntry().buildChildGUIEntry(builder);
		cge.setOriginal(value.getKey());
		return new endorh.simpleconfig.ui.impl.builders.CaptionedListEntryBuilder<>(
		  builder, name, value, (FieldBuilder<List<G>, LGE, ?>) listEntry, cge);
	}
	
	@OnlyIn(Dist.CLIENT) @Override public Optional<FieldBuilder<Pair<CG, List<G>>, ?, ?>> buildGUIEntry(
	  ConfigFieldBuilder builder
	) {
		listEntry.setDisplayName(getDisplayName());
		final Optional<FieldBuilder<List<G>, ?, ?>> opt = listEntry.buildGUIEntry(builder);
		if (!opt.isPresent()) throw new IllegalStateException("List entry has no GUI entry");
		final FieldBuilder<List<G>, ?, ?> listGUIEntry = opt.get();
		listGUIEntry.withoutTags(EntryTag.NON_PERSISTENT);
		final endorh.simpleconfig.ui.impl.builders.CaptionedListEntryBuilder<G, ?, ?, CG, ?, ?>
		  entryBuilder =
		  makeGUIEntry(builder, getDisplayName(), listEntry.buildGUIEntry(builder).map(
			 l -> l.withoutTags(EntryTag.NON_PERSISTENT)).orElseThrow(() -> new IllegalStateException(
				"List entry has no GUI entry")), forGui(get()));
		return Optional.of(decorate(entryBuilder));
	}
}
