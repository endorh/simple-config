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
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
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
					map.entrySet().stream().findFirst().ifPresent(e -> {
						KC key = keyEntry.fromActualConfig(e.getKey());
						C val = entry.fromActualConfig(e.getValue());
						if (key == null && val == null) return;
						if (key == null) key = keyEntry.forConfig(keyEntry.defValue);
						if (val == null) val = entry.forConfig(entry.defValue);
						pairs.add(Pair.of(key, val));
					});
				} else if (o instanceof Config) {
					Config config = (Config) o;
					config.entrySet().stream().findFirst().ifPresent(e -> {
						KC key = keyEntry.fromActualConfig(e.getKey());
						C val = entry.fromActualConfig(e.getValue());
						if (key == null && val == null) return;
						if (key == null) key = keyEntry.forConfig(keyEntry.defValue);
						if (val == null) val = entry.forConfig(entry.defValue);
						pairs.add(Pair.of(key, val));
					});
				}
			}
			return pairs;
		}
		return null;
	}
	
	@Override public List<Pair<KC, C>> forConfig(List<Pair<K, V>> value) {
		return value.stream().map(p -> Pair.of(
		  keyEntry.forConfig(p.getKey()), entry.forConfig(p.getValue())
		)).collect(Collectors.toList());
	}
	
	@Nullable @Override @Contract("null->null")
	public List<Pair<K, V>> fromConfig(@Nullable List<Pair<KC, C>> value) {
		if (value == null) return null;
		final List<Pair<K, V>> l = new ArrayList<>();
		for (Pair<KC, C> p : value)
			  l.add(Pair.of(
				 keyEntry.fromConfigOrDefault(p.getKey()),
				 entry.fromConfigOrDefault(p.getValue())));
		return l;
	}
	
	@Override
	public List<Pair<KG, G>> forGui(List<Pair<K, V>> value) {
		return value.stream().map(
		  p -> Pair.of(keyEntry.forGui(p.getKey()), entry.forGui(p.getValue()))
		).collect(Collectors.toList());
	}
	
	@Nullable @Override public List<Pair<K, V>> fromGui(@Nullable List<Pair<KG, G>> value) {
		if (value == null) return null;
		return value.stream().map(
		  p -> Pair.of(keyEntry.fromGuiOrDefault(p.getKey()), entry.fromGuiOrDefault(p.getValue()))
		).collect(Collectors.toList());
	}
	
	@Override
	protected Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
		return Optional.of(decorate(builder).define(name, forActualConfig(forConfig(defValue)), createConfigValidator()));
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
	
	@Override public List<ITextComponent> getElementErrors(Pair<KG, G> value) {
		List<ITextComponent> errors = super.getElementErrors(value);
		errors.addAll(keyEntry.getErrors(value.getKey()));
		errors.addAll(entry.getErrors(value.getValue()));
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
		  .setIgnoreOrder(true)
		  .setCellErrorSupplier(this::getElementError)
		  .setExpanded(expand);
		return Optional.of(decorate(entryBuilder).build());
	}
}