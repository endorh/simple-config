package endorh.simpleconfig.core;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.ConfigSpec;
import endorh.simpleconfig.core.entry.AbstractListEntry;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.api.EntryFlag;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.impl.builders.EntryPairListBuilder;
import endorh.simpleconfig.yaml.NonConfigMap;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class EntryPairListEntry<K, V, KC, C, KG, G,
  E extends AbstractConfigEntry<V, C, G, E>,
  B extends AbstractConfigEntryBuilder<V, C, G, E, B>,
  KE extends AbstractConfigEntry<K, KC, KG, KE> & IKeyEntry<KG>,
  KB extends AbstractConfigEntryBuilder<K, KC, KG, KE, KB>>
  extends AbstractListEntry<
    Pair<K, V>, Pair<KC, C>, Pair<KG, G>, EntryPairListEntry<K, V, KC, C, KG, G, E, B, KE, KB>> {
	protected final KB keyEntryBuilder;
	protected final KE keyEntry;
	protected final B entryBuilder;
	protected final E entry;
	protected final Class<?> keyEntryTypeClass;
	protected final Class<?> entryTypeClass;
	protected final FakeEntryHolder holder;
	protected boolean expand;
	
	@Internal public EntryPairListEntry(
	  ISimpleConfigEntryHolder parent, String name,
	  List<Pair<K, V>> value, @NotNull B entryBuilder, KB keyEntryBuilder
	) {
		super(parent, name, value);
		holder = new FakeEntryHolder(parent.getRoot());
		this.entryBuilder = entryBuilder;
		entryTypeClass = entryBuilder.typeClass;
		this.keyEntryBuilder = keyEntryBuilder;
		keyEntryTypeClass = keyEntryBuilder.typeClass;
		entry = entryBuilder.build(holder, name + "$ v");
		keyEntry = keyEntryBuilder.build(holder, name + "$ k");
		if (!entry.canBeNested())
			throw new IllegalArgumentException(
			  "Entry of type " + entry.getClass().getSimpleName() + " can not be " +
			  "nested in a map entry");
		if (entry.defValue == null)
			throw new IllegalArgumentException(
			  "Unsupported value type for map config entry. The values " +
			  "cannot be null");
	}
	
	public static class Builder<K, V, KC, C, KG, G,
	  E extends AbstractConfigEntry<V, C, G, E>,
	  B extends AbstractConfigEntryBuilder<V, C, G, E, B>,
	  KE extends AbstractConfigEntry<K, KC, KG, KE> & IKeyEntry<KG>,
	  KB extends AbstractConfigEntryBuilder<K, KC, KG, KE, KB>>
	  extends AbstractListEntry.Builder<
	  Pair<K, V>, Pair<KC, C>, Pair<KG, G>,
	  EntryPairListEntry<K, V, KC, C, KG, G, E, B, KE, KB>,
	  Builder<K, V, KC, C, KG, G, E, B, KE, KB>> {
		protected final KB keyEntryBuilder;
		protected B entryBuilder;
		protected boolean expand;
		
		public Builder(List<Pair<K, V>> value, KB keyEntryBuilder, B entryBuilder) {
			super(value, Map.class);
			this.entryBuilder = entryBuilder;
			this.keyEntryBuilder = keyEntryBuilder;
		}
		
		public Builder<K, V, KC, C, KG, G, E, B, KE, KB> expand() {
			return expand(true);
		}
		
		public Builder<K, V, KC, C, KG, G, E, B, KE, KB> expand(boolean expand) {
			Builder<K, V, KC, C, KG, G, E, B, KE, KB> copy = copy();
			copy.expand = expand;
			return copy;
		}
		
		@Override protected EntryPairListEntry<K, V, KC, C, KG, G, E, B, KE, KB> buildEntry(
		  ISimpleConfigEntryHolder parent, String name
		) {
			final EntryPairListEntry<K, V, KC, C, KG, G, E, B, KE, KB> e = new EntryPairListEntry<>(
			  parent, name, value, entryBuilder, keyEntryBuilder);
			e.expand = expand;
			return e;
		}
		
		@Override protected Builder<K, V, KC, C, KG, G, E, B, KE, KB> createCopy() {
			final Builder<K, V, KC, C, KG, G, E, B, KE, KB> copy =
			  new Builder<>(value, keyEntryBuilder.copy(), entryBuilder.copy());
			copy.expand = expand;
			return copy;
		}
	}
	
	@Override public List<Map<Object, Object>> forActualConfig(@Nullable List<Pair<KC, C>> value) {
		if (value == null) return null;
		return value.stream()
		  .map(p -> Util.make(
		    NonConfigMap.ofHashMap(1),
			 m -> m.put(keyEntry.forActualConfig(p.getKey()), entry.forActualConfig(p.getValue())))
		  ).collect(Collectors.toList());
	}
	
	@Override @Nullable public List<Pair<KC, C>> fromActualConfig(@Nullable Object value) {
		if (value instanceof List) {
			//noinspection unchecked
			List<Object> seq = (List<Object>) value;
			List<Pair<KC, C>> pairs = new ArrayList<>();
			for (Object o : seq) {
				if (o instanceof Map) {
					Map<?, ?> map = (Map<?, ?>) o;
					if (map.entrySet().size() != 1) return null;
					Map.Entry<?, ?> e = map.entrySet().stream().findFirst()
					  .orElseThrow(IllegalStateException::new);
					KC key = keyEntry.fromActualConfig(e.getKey());
					C val = entry.fromActualConfig(e.getValue());
					if (key == null || val == null) return null;
					pairs.add(Pair.of(key, val));
				} else if (o instanceof Config) {
					Config config = (Config) o;
					if (config.entrySet().size() != 1) return null;
					Config.Entry e = config.entrySet().stream().findFirst()
					  .orElseThrow(IllegalStateException::new);
					KC key = keyEntry.fromActualConfig(e.getKey());
					C val = entry.fromActualConfig(e.getValue());
					if (key == null || val == null) return null;
					pairs.add(Pair.of(key, val));
				}
			}
			return pairs;
		}
		return null;
	}
	
	@Override protected Pair<KG, G> elemForGui(Pair<K, V> value) {
		return Pair.of(keyEntry.forGui(value.getKey()), entry.forGui(value.getValue()));
	}
	@Override protected Pair<K, V> elemFromGui(Pair<KG, G> value) {
		K key = keyEntry.fromGui(value.getKey());
		V val = entry.fromGui(value.getValue());
		return key != null && val != null ? Pair.of(key, val) : null;
	}
	
	@Override protected Pair<KC, C> elemForConfig(Pair<K, V> value) {
		return Pair.of(keyEntry.forConfig(value.getKey()), entry.forConfig(value.getValue()));
	}
	@Override protected Pair<K, V> elemFromConfig(Pair<KC, C> value) {
		K key = keyEntry.fromConfig(value.getKey());
		V val = entry.fromConfig(value.getValue());
		return key != null && val != null? Pair.of(key, val) : null;
	}
	
	@Override protected void buildSpec(ConfigSpec spec, String parentPath) {
		spec.define(parentPath + name, forActualConfig(forConfig(defValue)), createConfigValidator());
	}
	
	@OnlyIn(Dist.CLIENT) protected <KGE extends AbstractConfigListEntry<KG> & IChildListEntry>
	Pair<KGE, AbstractConfigListEntry<G>> buildCell(ConfigEntryBuilder builder) {
		final KE ke = keyEntryBuilder.build(holder, holder.nextName())
		  .withSaver((g, h) -> {})
		  .withDisplayName(new StringTextComponent(""));
		ke.nonPersistent = true;
		final E e = entryBuilder.build(holder, holder.nextName()).withSaver((g, h) -> {})
		  .withDisplayName(new StringTextComponent(""));
		e.nonPersistent = true;
		ke.actualValue = ke.defValue;
		e.actualValue = e.defValue;
		KGE kg = ke.buildChildGUIEntry(builder);
		final AbstractConfigListEntry<G> g = e.buildGUIEntry(builder)
		  .orElseThrow(() -> new IllegalStateException(
			 "Map config entry's sub-entry did not produce a GUI entry"));
		g.removeEntryFlag(EntryFlag.NON_PERSISTENT);
		ke.guiEntry = kg;
		e.guiEntry = g;
		return Pair.of(kg, g);
	}
	
	@Override public List<ITextComponent> getElementErrors(int index, Pair<KG, G> value) {
		List<ITextComponent> errors = super.getElementErrors(index, value);
		keyEntry.getErrorsFromGUI(value.getKey()).stream()
		  .map(e -> addIndex(e, index))
		  .forEach(errors::add);
		entry.getErrorsFromGUI(value.getValue()).stream()
		  .map(e -> addIndex(e, index))
		  .forEach(errors::add);
		return errors;
	}
	
	protected @Nullable String getListTypeComment() {
		String keyComment = keyEntry.getConfigCommentTooltip();
		String valueComment = entry.getConfigCommentTooltip();
		return (keyComment.isEmpty()? "?" : keyComment) + " >> " +
		       (valueComment.isEmpty()? "?" : valueComment);
	}
	
	@Override public List<String> getConfigCommentTooltips() {
		List<String> tooltips = super.getConfigCommentTooltips();
		tooltips.remove(tooltips.size() - 1);
		String typeComment = getListTypeComment();
		if (typeComment != null) tooltips.add("Sorted Map: " + typeComment);
		return tooltips;
	}
	
	@OnlyIn(Dist.CLIENT) @Override
	public Optional<AbstractConfigListEntry<List<Pair<KG, G>>>> buildGUIEntry(ConfigEntryBuilder builder) {
		final EntryPairListBuilder<KG, G, ? extends AbstractConfigListEntry<KG>, AbstractConfigListEntry<G>>
		  entryBuilder = builder
		  .startEntryPairList(getDisplayName(), forGui(get()), en -> buildCell(builder))
		  .setIgnoreOrder(false)
		  .setCellErrorSupplier(this::getElementError)
		  .setExpanded(expand);
		return Optional.of(decorate(entryBuilder).build());
	}
}