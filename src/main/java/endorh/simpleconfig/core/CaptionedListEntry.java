package endorh.simpleconfig.core;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.api.EntryFlag;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.gui.entries.AbstractListListEntry;
import endorh.simpleconfig.ui.impl.builders.CaptionedListEntryBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import endorh.simpleconfig.core.NBTUtil.ExpectedType;
import endorh.simpleconfig.core.entry.AbstractListEntry;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

import static endorh.simpleconfig.core.NBTUtil.fromNBT;
import static endorh.simpleconfig.core.NBTUtil.toNBT;

public class CaptionedListEntry<V, C, G, E extends AbstractListEntry<V, C, G, E>,
  CV, CC, CG, CE extends AbstractConfigEntry<CV, CC, CG, CE> & IKeyEntry<CC, CG>>
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
	  CV, CC, CG, CE extends AbstractConfigEntry<CV, CC, CG, CE> & IKeyEntry<CC, CG>,
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
	
	public String forActualConfig(@Nullable Pair<CC, List<C>> value) {
		if (value == null) return "";
		final CompoundNBT nbt = new CompoundNBT();
		nbt.put("k", toNBT(value.getKey()));
		nbt.put("v", toNBT(value.getValue()));
		return nbt.toString();
	}
	
	public @Nullable Pair<CC, List<C>> fromActualConfig(@Nullable Object value) {
		if (!(value instanceof String))
			return null;
		final String str = ((String) value);
		try {
			final CompoundNBT nbt = new JsonToNBT(new StringReader(str)).readStruct();
			if (!nbt.contains("k") && !nbt.contains("v")) return null;
			//noinspection unchecked
			final CC key = (CC) fromNBT(nbt.get("k"), getExpectedType().next.get(0));
			//noinspection unchecked
			final List<C> val = (List<C>) fromNBT(nbt.get("v"), getExpectedType().next.get(1));
			return Pair.of(key, val);
		} catch (CommandSyntaxException | IllegalArgumentException | ClassCastException e) {
			return null;
		}
	}
	
	@Override public List<ITextComponent> getErrors(Pair<CG, List<G>> value) {
		List<ITextComponent> errors = super.getErrors(value);
		errors.addAll(captionEntry.getErrors(value.getKey()));
		errors.addAll(listEntry.getErrors(value.getValue()));
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
		return Pair.of(captionEntry.fromConfigOrDefault(value.getKey()),
		               listEntry.fromConfigOrDefault(value.getValue()));
	}
	
	@Override public Pair<CG, List<G>> forGui(Pair<CV, List<V>> value) {
		return Pair.of(captionEntry.forGui(value.getKey()),
		               listEntry.forGui(value.getValue()));
	}
	
	@Nullable @Override public Pair<CV, List<V>> fromGui(@Nullable Pair<CG, List<G>> value) {
		if (value == null) return null;
		return Pair.of(captionEntry.fromGuiOrDefault(value.getKey()),
		               listEntry.fromGuiOrDefault(value.getValue()));
	}
	
	@Override protected Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
		return Optional.of(decorate(builder).define(name, forActualConfig(forConfig(value)), createConfigValidator()));
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
	
	@Override public ExpectedType getExpectedType() {
		return new ExpectedType(typeClass, captionEntry.getExpectedType(), listEntry.getExpectedType());
	}
}