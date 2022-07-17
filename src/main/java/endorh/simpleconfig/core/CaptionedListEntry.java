package endorh.simpleconfig.core;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.Config.Entry;
import endorh.simpleconfig.core.entry.AbstractListEntry;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.api.EntryFlag;
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

public class CaptionedListEntry<V, C, G, E extends AbstractListEntry<V, C, G, E>,
  CV, CC, CG, CE extends AbstractConfigEntry<CV, CC, CG, CE> & IKeyEntry<CG>>
  extends AbstractConfigEntry<Pair<CV, List<V>>, Pair<CC, List<C>>, Pair<CG, List<G>>,
  CaptionedListEntry<V, C, G, E, CV, CC, CG, CE>> {
	
	protected final E listEntry;
	protected final CE captionEntry;
	
	protected CaptionedListEntry(
	  ISimpleConfigEntryHolder parent, String name,
	  Pair<CV, List<V>> value, E listEntry, CE captionEntry
	) {
		super(parent, name, value);
		this.listEntry = listEntry.withSaver((v, h) -> {});
		this.captionEntry = captionEntry.withSaver((v, h) -> {});
	}
	
	public static class Builder<V, C, G, E extends AbstractListEntry<V, C, G, E>,
	  B extends AbstractListEntry.Builder<V, C, G, E, B>,
	  CV, CC, CG, CE extends AbstractConfigEntry<CV, CC, CG, CE> & IKeyEntry<CG>,
	  CB extends AbstractConfigEntryBuilder<CV, CC, CG, CE, CB>>
	  extends AbstractConfigEntryBuilder<Pair<CV, List<V>>, Pair<CC, List<C>>, Pair<CG, List<G>>,
	  CaptionedListEntry<V, C, G, E, CV, CC, CG, CE>, Builder<
	  V, C, G, E, B, CV, CC, CG, CE, CB>>
	{
		protected B listEntryBuilder;
		protected CB captionEntryBuilder;
		
		public Builder(Pair<CV, List<V>> value, B listEntryBuilder, CB captionEntryBuilder) {
			super(value, Pair.class);
			this.listEntryBuilder = listEntryBuilder;
			this.captionEntryBuilder = captionEntryBuilder;
		}
		
		@Override protected CaptionedListEntry<V, C, G, E, CV, CC, CG, CE> buildEntry(
		  ISimpleConfigEntryHolder parent, String name
		) {
			final E le = DummyEntryHolder.build(parent, listEntryBuilder).withSaver((v, h) -> {});
			final CE ce = DummyEntryHolder.build(parent, captionEntryBuilder).withSaver((v, h) -> {});
			return new CaptionedListEntry<>(parent, name, value, le, ce);
		}
		
		@Override protected Builder<V, C, G, E, B, CV, CC, CG, CE, CB> createCopy() {
			return new Builder<>(
			  value, ((AbstractConfigEntryBuilder<List<V>, List<C>, List<G>, E, B>) listEntryBuilder).copy(),
			  captionEntryBuilder.copy());
		}
	}
	
	public Map<Object, Object> forActualConfig(@Nullable Pair<CC, List<C>> value) {
		if (value == null) return null;
		return Util.make(NonConfigMap.ofHashMap(1), m -> m.put(
		  captionEntry.forActualConfig(value.getKey()),
		  listEntry.forActualConfig(value.getValue())));
	}
	
	public @Nullable Pair<CC, List<C>> fromActualConfig(@Nullable Object value) {
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
	
	@OnlyIn(Dist.CLIENT) @SuppressWarnings("unchecked") protected <LGE extends AbstractListListEntry<G, ?, LGE>,
	  CGE extends AbstractConfigListEntry<CG> & IChildListEntry>
	CaptionedListEntryBuilder<G, LGE, CG, CGE> makeGUIEntry(
	  ConfigEntryBuilder builder, ITextComponent name,
	  AbstractConfigListEntry<List<G>> listEntry, Pair<CG, List<G>> value
	) {
		final CGE cge = captionEntry.buildChildGUIEntry(builder);
		cge.setOriginal(value.getKey());
		return new CaptionedListEntryBuilder<>(
		  builder, name, value, (LGE) listEntry, cge);
	}
	
	@OnlyIn(Dist.CLIENT) @Override public Optional<AbstractConfigListEntry<Pair<CG, List<G>>>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		listEntry.withDisplayName(getDisplayName());
		final Optional<AbstractConfigListEntry<List<G>>> opt = listEntry.buildGUIEntry(builder);
		if (!opt.isPresent()) throw new IllegalStateException("List entry has no GUI entry");
		final AbstractConfigListEntry<List<G>> listGUIEntry = opt.get();
		listGUIEntry.removeEntryFlag(EntryFlag.NON_PERSISTENT);
		final CaptionedListEntryBuilder<G, ?, CG, ?> entryBuilder =
		  makeGUIEntry(builder, getDisplayName(), listGUIEntry, forGui(get()));
		return Optional.of(decorate(entryBuilder).build());
	}
	
	@OnlyIn(Dist.CLIENT) @Override protected <F extends FieldBuilder<Pair<CG, List<G>>, ?, F>> F decorate(F builder) {
		return super.decorate(builder);
	}
}
