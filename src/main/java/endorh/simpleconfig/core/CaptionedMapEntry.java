package endorh.simpleconfig.core;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.api.EntryFlag;
import endorh.simpleconfig.ui.gui.entries.EntryPairListListEntry;
import endorh.simpleconfig.ui.impl.builders.CaptionedListEntryBuilder;
import endorh.simpleconfig.core.NBTUtil.ExpectedType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static endorh.simpleconfig.core.NBTUtil.fromNBT;
import static endorh.simpleconfig.core.NBTUtil.toNBT;

public class CaptionedMapEntry<K, V, KC, C, KG, G,
  E extends AbstractConfigEntry<V, C, G, E>,
  B extends AbstractConfigEntryBuilder<V, C, G, E, B>,
  KE extends AbstractConfigEntry<K, KC, KG, KE> & IKeyEntry<KC, KG>,
  KB extends AbstractConfigEntryBuilder<K, KC, KG, KE, KB>,
  MB extends EntryMapEntry.Builder<K, V, KC, C, KG, G, E, B, KE, KB>,
  CV, CC, CG, CE extends AbstractConfigEntry<CV, CC, CG, CE> & IKeyEntry<CC, CG>>
  extends AbstractConfigEntry<Pair<CV, Map<K, V>>, Pair<CC, Map<String, C>>, Pair<CG, List<Pair<KG, G>>>,
  CaptionedMapEntry<K, V, KC, C, KG, G, E, B, KE, KB, MB, CV, CC, CG, CE>> {
	
	protected final EntryMapEntry<K, V, KC, C, KG, G, E, B, KE, KB> mapEntry;
	protected final CE captionEntry;
	
	protected CaptionedMapEntry(
	  ISimpleConfigEntryHolder parent, String name,
	  Pair<CV, Map<K, V>> value, EntryMapEntry<K, V, KC, C, KG, G, E, B, KE, KB> mapEntry, CE captionEntry
	) {
		super(parent, name, value);
		this.mapEntry = mapEntry.withSaver((v, h) -> {});
		this.captionEntry = captionEntry.withSaver((v, h) -> {});
	}
	
	public static class Builder<K, V, KC, C, KG, G,
	  E extends AbstractConfigEntry<V, C, G, E>,
	  B extends AbstractConfigEntryBuilder<V, C, G, E, B>,
	  KE extends AbstractConfigEntry<K, KC, KG, KE> & IKeyEntry<KC, KG>,
	  KB extends AbstractConfigEntryBuilder<K, KC, KG, KE, KB>,
	  MB extends EntryMapEntry.Builder<K, V, KC, C, KG, G, E, B, KE, KB>,
	  CV, CC, CG, CE extends AbstractConfigEntry<CV, CC, CG, CE> & IKeyEntry<CC, CG>,
	  CB extends AbstractConfigEntryBuilder<CV, CC, CG, CE, CB>
	> extends AbstractConfigEntryBuilder<
	  Pair<CV, Map<K, V>>, Pair<CC, Map<String, C>>, Pair<CG, List<Pair<KG, G>>>,
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
			final EntryMapEntry<K, V, KC, C, KG, G, E, B, KE, KB> me =
			  DummyEntryHolder.build(parent, mapEntryBuilder).withSaver((v, h) -> {});
			// final EntryMapEntry<K, V, KC, C, KG, G, E, B, KE, KB> me =
			//   mapEntryBuilder.buildEntry(parent, name + "$val");
			final CE ce = DummyEntryHolder.build(parent, captionEntryBuilder).withSaver((v, h) -> {});
			// final CE ce = captionEntryBuilder.buildEntry(parent, name + "$key");
			return new CaptionedMapEntry<>(parent, name, value, me, ce);
		}
		
		@Override protected Builder<K, V, KC, C, KG, G, E, B, KE, KB, MB, CV, CC, CG, CE, CB> createCopy() {
			//noinspection unchecked
			return new CaptionedMapEntry.Builder<>(
			  value, (MB) mapEntryBuilder.copy(), captionEntryBuilder.copy());
		}
	}
	
	public String forActualConfig(@Nullable Pair<CC, Map<String, C>> value) {
		if (value == null) return "";
		final CompoundNBT nbt = new CompoundNBT();
		nbt.put("k", toNBT(value.getKey()));
		nbt.put("v", toNBT(value.getValue()));
		return nbt.toString();
	}
	
	public @Nullable Pair<CC, Map<String, C>> fromActualConfig(@Nullable Object value) {
		if (!(value instanceof String)) return null;
		final String str = (String) value;
		try {
			final CompoundNBT nbt = new JsonToNBT(new StringReader(str)).readStruct();
			//noinspection unchecked
			final CC key = (CC) fromNBT(nbt.get("k"), getExpectedType().next.get(0));
			//noinspection unchecked
			final Map<String, C> val = (Map<String, C>) fromNBT(nbt.get("v"), getExpectedType().next.get(1));
			return Pair.of(key, val);
		} catch (CommandSyntaxException | IllegalArgumentException | ClassCastException e) {
			return null;
		}
	}
	
	@Override public List<ITextComponent> getErrors(Pair<CG, List<Pair<KG, G>>> value) {
		List<ITextComponent> errors = super.getErrors(value);
		errors.addAll(captionEntry.getErrors(value.getKey()));
		errors.addAll(mapEntry.getErrors(value.getValue()));
		return errors;
	}
	
	@Override public Pair<CC, Map<String, C>> forConfig(Pair<CV, Map<K, V>> value) {
		return Pair.of(captionEntry.forConfig(value.getKey()),
		               mapEntry.forConfig(value.getValue()));
	}
	
	@Override public @Nullable Pair<CV, Map<K, V>> fromConfig(
	  @Nullable Pair<CC, Map<String, C>> value
	) {
		if (value == null) return null;
		return Pair.of(captionEntry.fromConfigOrDefault(value.getKey()),
		               mapEntry.fromConfigOrDefault(value.getValue()));
	}
	
	@Override public Pair<CG, List<Pair<KG, G>>> forGui(Pair<CV, Map<K, V>> value) {
		return Pair.of(captionEntry.forGui(value.getKey()), mapEntry.forGui(value.getValue()));
	}
	
	@Override public @Nullable Pair<CV, Map<K, V>> fromGui(@Nullable Pair<CG, List<Pair<KG, G>>> value) {
		if (value == null) return null;
		return Pair.of(captionEntry.fromGuiOrDefault(value.getKey()),
		               mapEntry.fromGuiOrDefault(value.getValue()));
	}
	
	@Override protected Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
		return Optional.of(decorate(builder).define(name, forActualConfig(forConfig(value)), createConfigValidator()));
	}
	
	@OnlyIn(Dist.CLIENT) @Override public Optional<AbstractConfigListEntry<Pair<CG, List<Pair<KG, G>>>>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		mapEntry.withDisplayName(getDisplayName());
		final Optional<AbstractConfigListEntry<List<Pair<KG, G>>>> opt =
		  mapEntry.buildGUIEntry(builder);
		if (!opt.isPresent()) throw new IllegalStateException("List entry has no GUI entry");
		final AbstractConfigListEntry<List<Pair<KG, G>>> mapGUIEntry = opt.get();
		mapGUIEntry.removeEntryFlag(EntryFlag.NON_PERSISTENT);
		final Pair<CG, List<Pair<KG, G>>> gv = forGui(get());
		final CaptionedListEntryBuilder<Pair<KG, G>, ?, CG, ?> entryBuilder = builder.startCaptionedList(
		  getDisplayName(), (EntryPairListListEntry<KG, G, ?, ?>) mapGUIEntry,
		  Util.make(captionEntry.buildChildGUIEntry(builder), cge -> cge.setOriginal(gv.getKey())), gv);
		return Optional.of(decorate(entryBuilder).build());
	}
	
	@Override public ExpectedType getExpectedType() {
		return new ExpectedType(typeClass, captionEntry.getExpectedType(), mapEntry.getExpectedType());
	}
}