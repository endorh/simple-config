package endorh.simpleconfig.core.entry;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.gui.Icon;
import endorh.simpleconfig.ui.impl.builders.PairListEntryBuilder;
import endorh.simpleconfig.core.*;
import endorh.simpleconfig.core.NBTUtil.ExpectedType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.List;
import java.util.Optional;

import static endorh.simpleconfig.core.NBTUtil.fromNBT;
import static endorh.simpleconfig.core.NBTUtil.toNBT;

public class EntryPairEntry<
  L, R, LC, RC, LG, RG,
  LE extends AbstractConfigEntry<L, LC, LG, LE> & IKeyEntry<LC, LG>,
  RE extends AbstractConfigEntry<R, RC, RG, RE> & IKeyEntry<RC, RG>
> extends AbstractConfigEntry<
  Pair<L, R>, Pair<LC, RC>, Pair<LG, RG>,
  EntryPairEntry<L, R, LC, RC, LG, RG, LE, RE>
> implements IKeyEntry<Pair<LC, RC>, Pair<LG, RG>> {
	protected final LE leftEntry;
	protected final RE rightEntry;
	protected float splitPos = 0.5F;
	protected @Nullable Icon middleIcon;
	
	protected EntryPairEntry(
	  ISimpleConfigEntryHolder parent, String name, Pair<L, R> value,
	  LE leftEntry, RE rightEntry
	) {
		super(parent, name, value);
		this.leftEntry = leftEntry.withSaver((v, h) -> {});
		this.rightEntry = rightEntry.withSaver((v, h) -> {});
	}
	
	public static class Builder<
	  L, R, LC, RC, LG, RG,
	  LE extends AbstractConfigEntry<L, LC, LG, LE> & IKeyEntry<LC, LG>,
	  RE extends AbstractConfigEntry<R, RC, RG, RE> & IKeyEntry<RC, RG>,
	  LB extends AbstractConfigEntryBuilder<L, LC, LG, LE, LB>,
	  RB extends AbstractConfigEntryBuilder<R, RC, RG, RE, RB>
	> extends AbstractConfigEntryBuilder<
	  Pair<L, R>, Pair<LC, RC>, Pair<LG, RG>,
	  EntryPairEntry<L, R, LC, RC, LG, RG, LE, RE>,
	  EntryPairEntry.Builder<L, R, LC, RC, LG, RG, LE, RE, LB, RB>
	> {
		protected LB leftBuilder;
		protected RB rightBuilder;
		protected float splitPos = 0.5F;
		protected @Nullable Icon middleIcon;
		
		public Builder(Pair<L, R> value, LB leftBuilder, RB rightBuilder) {
			super(value, Pair.class);
			this.leftBuilder = leftBuilder;
			this.rightBuilder = rightBuilder;
		}
		
		public Builder<L, R, LC, RC, LG, RG, LE, RE, LB, RB> withMiddleIcon(@Nullable Icon icon) {
			final Builder<L, R, LC, RC, LG, RG, LE, RE, LB, RB> copy = copy();
			copy.middleIcon = icon;
			return copy;
		}
		
		public Builder<L, R, LC, RC, LG, RG, LE, RE, LB, RB> withSplitPosition(
		  @Range(from = 0, to = 1) float splitPosition
		) {
			if (splitPosition < 0 || splitPosition > 1) throw new IllegalArgumentException(
			  "Split position must be between 0~1. Specifically: " + splitPosition);
			final Builder<L, R, LC, RC, LG, RG, LE, RE, LB, RB> copy = copy();
			copy.splitPos = splitPosition;
			return copy;
		}
		
		@Override protected EntryPairEntry<L, R, LC, RC, LG, RG, LE, RE> buildEntry(
		  ISimpleConfigEntryHolder parent, String name
		) {
			LE leftEntry = DummyEntryHolder.build(parent, leftBuilder).withSaver((v, h) -> {});
			RE rightEntry = DummyEntryHolder.build(parent, rightBuilder).withSaver((v, h) -> {});
			final EntryPairEntry<L, R, LC, RC, LG, RG, LE, RE> entry =
			  new EntryPairEntry<>(parent, name, value, leftEntry, rightEntry);
			entry.middleIcon = middleIcon;
			entry.splitPos = splitPos;
			return entry;
		}
		
		@Override protected Builder<L, R, LC, RC, LG, RG, LE, RE, LB, RB> createCopy() {
			final Builder<L, R, LC, RC, LG, RG, LE, RE, LB, RB> copy =
			  new Builder<>(value, copyBuilder(leftBuilder), copyBuilder(rightBuilder));
			copy.middleIcon = middleIcon;
			copy.splitPos = splitPos;
			return copy;
		}
	}
	
	@Override public String forActualConfig(@Nullable Pair<LC, RC> value) {
		if (value == null) return "";
		final CompoundNBT nbt = new CompoundNBT();
		nbt.put("l", toNBT(value.getLeft()));
		nbt.put("r", toNBT(value.getRight()));
		return nbt.toString();
	}
	
	@Nullable @Override protected Pair<LC, RC> fromActualConfig(@Nullable Object value) {
		if (!(value instanceof String)) return null;
		final String str = (String) value;
		try {
			final CompoundNBT nbt = new JsonToNBT(new StringReader(str)).readStruct();
			//noinspection unchecked
			final LC left = (LC) fromNBT(nbt.get("l"), getExpectedType().next.get(0));
			//noinspection unchecked
			final RC right = (RC) fromNBT(nbt.get("r"), getExpectedType().next.get(1));
			return Pair.of(left, right);
		} catch (CommandSyntaxException | IllegalArgumentException | ClassCastException e) {
			return null;
		}
	}
	
	@Override public List<ITextComponent> getErrors(Pair<LG, RG> value) {
		List<ITextComponent> errors = super.getErrors(value);
		errors.addAll(leftEntry.getErrors(value.getLeft()));
		errors.addAll(rightEntry.getErrors(value.getRight()));
		return errors;
	}
	
	@Override public Pair<LC, RC> forConfig(Pair<L, R> value) {
		return Pair.of(leftEntry.forConfig(value.getLeft()),
		               rightEntry.forConfig(value.getRight()));
	}
	
	@Override public @Nullable Pair<L, R> fromConfig(@Nullable Pair<LC, RC> value) {
		if (value == null) return null;
		return Pair.of(leftEntry.fromConfigOrDefault(value.getLeft()),
		               rightEntry.fromConfigOrDefault(value.getRight()));
	}
	
	@Override public Pair<LG, RG> forGui(Pair<L, R> value) {
		return Pair.of(leftEntry.forGui(value.getLeft()),
		               rightEntry.forGui(value.getRight()));
	}
	
	@Override public @Nullable Pair<L, R> fromGui(@Nullable Pair<LG, RG> value) {
		if (value == null) return null;
		return Pair.of(leftEntry.fromGuiOrDefault(value.getLeft()),
		               rightEntry.fromGuiOrDefault(value.getRight()));
	}
	
	@Override public Optional<Pair<LC, RC>> deserializeStringKey(@NotNull String key) {
		return Optional.ofNullable(fromActualConfig(key));
	}
	
	@Override public String serializeStringKey(@NotNull Pair<LC, RC> key) {
		return forActualConfig(key);
	}
	
	@Override protected Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
		return Optional.of(decorate(builder).define(name, forActualConfig(forConfig(value)), createConfigValidator()));
	}
	
	@Override public Optional<AbstractConfigListEntry<Pair<LG, RG>>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		PairListEntryBuilder<LG, RG, ?, ?> entryBuilder = builder.startPair(
		  getDisplayName(),
		  leftEntry.buildChildGUIEntry(builder),
		  rightEntry.buildChildGUIEntry(builder),
		  forGui(get()))
		  .withMiddleIcon(middleIcon)
		  .withSplitPos(splitPos);
		return Optional.of(decorate(entryBuilder).build());
	}
	
	@Override public ExpectedType getExpectedType() {
		return new ExpectedType(typeClass, leftEntry.getExpectedType(), rightEntry.getExpectedType());
	}
}
