package endorh.simple_config.core;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.ConfigEntryBuilder;
import endorh.simple_config.clothconfig2.api.IChildListEntry;
import endorh.simple_config.clothconfig2.gui.entries.AbstractListListEntry;
import endorh.simple_config.clothconfig2.impl.builders.DecoratedListEntryBuilder;
import endorh.simple_config.clothconfig2.impl.builders.FieldBuilder;
import endorh.simple_config.core.NBTUtil.ExpectedType;
import endorh.simple_config.core.entry.AbstractListEntry;
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
import java.util.function.Predicate;

import static endorh.simple_config.core.NBTUtil.fromNBT;
import static endorh.simple_config.core.NBTUtil.toNBT;

public class DecoratedListEntry<V, C, G, E extends AbstractListEntry<V, C, G, E>,
  CV, CC, CG, CE extends AbstractConfigEntry<CV, CC, CG, CE> & IKeyEntry<CC, CG>>
  extends AbstractConfigEntry<Pair<CV, List<V>>, Pair<CC, List<C>>, Pair<CG, List<G>>,
  DecoratedListEntry<V, C, G, E, CV, CC, CG, CE>> {
	
	protected final E listEntry;
	protected final CE captionEntry;
	
	protected DecoratedListEntry(
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
	  DecoratedListEntry<V, C, G, E, CV, CC, CG, CE>, Builder<
	  V, C, G, E, B, CV, CC, CG, CE, CB>>
	{
		protected B listEntryBuilder;
		protected CB captionEntryBuilder;
		
		public Builder(Pair<CV, List<V>> value, B listEntryBuilder, CB captionEntryBuilder) {
			super(value, Pair.class);
			this.listEntryBuilder = listEntryBuilder;
			this.captionEntryBuilder = captionEntryBuilder;
		}
		
		@Override protected DecoratedListEntry<V, C, G, E, CV, CC, CG, CE> buildEntry(
		  ISimpleConfigEntryHolder parent, String name
		) {
			final E le = DummyEntryHolder.build(parent, listEntryBuilder).withSaver((v, h) -> {});
			final CE ce = DummyEntryHolder.build(parent, captionEntryBuilder).withSaver((v, h) -> {});
			return new DecoratedListEntry<>(parent, name, value, le, ce);
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
	
	@Override protected Predicate<Object> configValidator() {
		return o -> {
			if (o instanceof String) {
				final Pair<CC, List<C>> pre = fromActualConfig(o);
				final Pair<CV, List<V>> p = fromConfig(pre);
				if (p == null) return false;
				return !supplyError(forGui(p)).isPresent()
				       && !listEntry.supplyError(listEntry.forGui(p.getValue())).isPresent()
				       && !captionEntry.supplyError(captionEntry.forGui(p.getKey())).isPresent();
			} else return false;
		};
	}
	
	@Override protected Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
		return Optional.of(decorate(builder).define(name, forActualConfig(forConfig(value)), configValidator()));
	}
	
	@OnlyIn(Dist.CLIENT) @SuppressWarnings("unchecked") protected <LGE extends AbstractListListEntry<G, ?, LGE>,
	  CGE extends AbstractConfigListEntry<CG> & IChildListEntry>
	DecoratedListEntryBuilder<G, LGE, CG, CGE> makeGUIEntry(
	  ConfigEntryBuilder builder, ITextComponent name,
	  AbstractConfigListEntry<List<G>> listEntry, Pair<CG, List<G>> value
	) {
		return new DecoratedListEntryBuilder<>(
		  builder, name, value, (LGE) listEntry, captionEntry.buildChildGUIEntry(builder));
	}
	
	@OnlyIn(Dist.CLIENT) @Override public Optional<AbstractConfigListEntry<Pair<CG, List<G>>>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		listEntry.withDisplayName(getDisplayName());
		final Optional<AbstractConfigListEntry<List<G>>> opt = listEntry.buildGUIEntry(builder);
		if (!opt.isPresent()) throw new IllegalStateException("List entry has no GUI entry");
		
		final DecoratedListEntryBuilder<G, ?, CG, ?> entryBuilder =
		  makeGUIEntry(builder, getDisplayName(), opt.get(), forGui(get()));
		return Optional.of(decorate(entryBuilder).build());
	}
	
	@OnlyIn(Dist.CLIENT) @Override protected <F extends FieldBuilder<Pair<CG, List<G>>, ?, F>> F decorate(F builder) {
		return super.decorate(builder);
	}
	
	@Override public ExpectedType getExpectedType() {
		return new ExpectedType(typeClass, captionEntry.getExpectedType(), listEntry.getExpectedType());
	}
}
