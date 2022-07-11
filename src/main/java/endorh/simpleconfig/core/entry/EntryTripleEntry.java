package endorh.simpleconfig.core.entry;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.gui.Icon;
import endorh.simpleconfig.ui.impl.builders.TripleListEntryBuilder;
import endorh.simpleconfig.core.*;
import endorh.simpleconfig.core.NBTUtil.ExpectedType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.List;
import java.util.Optional;

import static endorh.simpleconfig.core.NBTUtil.fromNBT;
import static endorh.simpleconfig.core.NBTUtil.toNBT;

public class EntryTripleEntry<
  L, M, R, LC, MC, RC, LG, MG, RG,
  LE extends AbstractConfigEntry<L, LC, LG, LE> & IKeyEntry<LC, LG>,
  ME extends AbstractConfigEntry<M, MC, MG, ME> & IKeyEntry<MC, MG>,
  RE extends AbstractConfigEntry<R, RC, RG, RE> & IKeyEntry<RC, RG>
> extends AbstractConfigEntry<
  Triple<L, M, R>, Triple<LC, MC, RC>, Triple<LG, MG, RG>,
  EntryTripleEntry<L, M, R, LC, MC, RC, LG, MG, RG, LE, ME, RE>
> implements IKeyEntry<Triple<LC, MC, RC>, Triple<LG, MG, RG>> {
	protected final LE leftEntry;
	protected final ME middleEntry;
	protected final RE rightEntry;
	
	protected @Nullable Icon leftIcon;
	protected @Nullable Icon rightIcon;
	protected float leftWeight = 0.333F;
	protected float rightWeight = 0.333F;
	
	protected EntryTripleEntry(
	  ISimpleConfigEntryHolder parent, String name, Triple<L, M, R> value,
	  LE leftEntry, ME middleEntry, RE rightEntry
	) {
		super(parent, name, value);
		this.leftEntry = leftEntry;
		this.middleEntry = middleEntry;
		this.rightEntry = rightEntry;
	}
	
	public static class Builder<
	  L, M, R, LC, MC, RC, LG, MG, RG,
	  LE extends AbstractConfigEntry<L, LC, LG, LE> & IKeyEntry<LC, LG>,
	  ME extends AbstractConfigEntry<M, MC, MG, ME> & IKeyEntry<MC, MG>,
	  RE extends AbstractConfigEntry<R, RC, RG, RE> & IKeyEntry<RC, RG>,
	  LB extends AbstractConfigEntryBuilder<L, LC, LG, LE, LB>,
	  MB extends AbstractConfigEntryBuilder<M, MC, MG, ME, MB>,
	  RB extends AbstractConfigEntryBuilder<R, RC, RG, RE, RB>
	> extends AbstractConfigEntryBuilder<
	  Triple<L, M, R>, Triple<LC, MC, RC>, Triple<LG, MG, RG>,
	  EntryTripleEntry<L, M, R, LC, MC, RC, LG, MG, RG, LE, ME, RE>,
	  EntryTripleEntry.Builder<L, M, R, LC, MC, RC, LG, MG, RG, LE, ME, RE, LB, MB, RB>
	> {
		protected LB leftBuilder;
		protected MB middleBuilder;
		protected RB rightBuilder;
		
		protected @Nullable Icon leftIcon;
		protected @Nullable Icon rightIcon;
		protected float leftWeight = 0.333F;
		protected float rightWeight = 0.333F;
		
		public Builder(
		  Triple<L, M, R> value, LB leftBuilder, MB middleBuilder, RB rightBuilder
		) {
			super(value, Triple.class);
			this.leftBuilder = leftBuilder;
			this.middleBuilder = middleBuilder;
			this.rightBuilder = rightBuilder;
		}
		
		public Builder<L, M, R, LC, MC, RC, LG, MG, RG, LE, ME, RE, LB, MB, RB> withLeftIcon(
		  @Nullable Icon leftIcon
		) {
			final Builder<L, M, R, LC, MC, RC, LG, MG, RG, LE, ME, RE, LB, MB, RB> copy = copy();
			copy.leftIcon = leftIcon;
			return copy;
		}
		
		public Builder<L, M, R, LC, MC, RC, LG, MG, RG, LE, ME, RE, LB, MB, RB> withRightIcon(
		  @Nullable Icon rightIcon
		) {
			final Builder<L, M, R, LC, MC, RC, LG, MG, RG, LE, ME, RE, LB, MB, RB> copy = copy();
			copy.rightIcon = rightIcon;
			return copy;
		}
		
		public Builder<L, M, R, LC, MC, RC, LG, MG, RG, LE, ME, RE, LB, MB, RB> withIcons(
		  @Nullable Icon leftIcon, @Nullable Icon rightIcon
		) {
			final Builder<L, M, R, LC, MC, RC, LG, MG, RG, LE, ME, RE, LB, MB, RB> copy = copy();
			copy.leftIcon = leftIcon;
			copy.rightIcon = rightIcon;
			return copy;
		}
		
		public Builder<L, M, R, LC, MC, RC, LG, MG, RG, LE, ME, RE, LB, MB, RB> withWeights(
		  @Range(from = 0, to = 1) float leftWeight, @Range(from = 0, to = 1) float rightWeight
		) {
			if (leftWeight < 0 || rightWeight < 0) throw new IllegalArgumentException(
			  "Weights can be negative. Specifically: " + leftWeight + ", " + rightWeight);
			if (leftWeight + rightWeight > 1) throw new IllegalArgumentException(
			  "Weights must add to less than 1. Specifically: " + leftWeight + ", " + rightWeight);
			final Builder<L, M, R, LC, MC, RC, LG, MG, RG, LE, ME, RE, LB, MB, RB> copy = copy();
			copy.leftWeight = leftWeight;
			copy.rightWeight = rightWeight;
			return copy;
		}
		
		@Override protected EntryTripleEntry<L, M, R, LC, MC, RC, LG, MG, RG, LE, ME, RE> buildEntry(
		  ISimpleConfigEntryHolder parent, String name
		) {
			LE leftEntry = DummyEntryHolder.build(parent, leftBuilder).withSaver((v, h) -> {});
			ME middleEntry = DummyEntryHolder.build(parent, middleBuilder).withSaver((v, h) -> {});
			RE rightEntry = DummyEntryHolder.build(parent, rightBuilder).withSaver((v, h) -> {});
			final EntryTripleEntry<L, M, R, LC, MC, RC, LG, MG, RG, LE, ME, RE> entry =
			  new EntryTripleEntry<>(parent, name, value, leftEntry, middleEntry, rightEntry);
			entry.leftIcon = leftIcon;
			entry.rightIcon = rightIcon;
			entry.leftWeight = leftWeight;
			entry.rightWeight = rightWeight;
			return entry;
		}
		
		@Override
		protected Builder<L, M, R, LC, MC, RC, LG, MG, RG, LE, ME, RE, LB, MB, RB> createCopy() {
			final Builder<L, M, R, LC, MC, RC, LG, MG, RG, LE, ME, RE, LB, MB, RB> copy =
			  new Builder<>(value, leftBuilder, middleBuilder, rightBuilder);
			copy.leftIcon = leftIcon;
			copy.rightIcon = rightIcon;
			copy.leftWeight = leftWeight;
			copy.rightWeight = rightWeight;
			return copy;
		}
	}
	
	@Override public String forActualConfig(@Nullable Triple<LC, MC, RC> value) {
		if (value == null) return "";
		final CompoundNBT nbt = new CompoundNBT();
		nbt.put("l", toNBT(value.getLeft()));
		nbt.put("m", toNBT(value.getMiddle()));
		nbt.put("r", toNBT(value.getRight()));
		return nbt.toString();
	}
	
	@Nullable @Override protected Triple<LC, MC, RC> fromActualConfig(@Nullable Object value) {
		if (!(value instanceof String)) return null;
		final String str = (String) value;
		try {
			final CompoundNBT nbt = new JsonToNBT(new StringReader(str)).readStruct();
			//noinspection unchecked
			final LC left = (LC) fromNBT(nbt.get("l"), getExpectedType().next.get(0));
			//noinspection unchecked
			final MC middle = (MC) fromNBT(nbt.get("l"), getExpectedType().next.get(1));
			//noinspection unchecked
			final RC right = (RC) fromNBT(nbt.get("r"), getExpectedType().next.get(2));
			return Triple.of(left, middle, right);
		} catch (CommandSyntaxException | IllegalArgumentException | ClassCastException e) {
			return null;
		}
	}
	
	@Override public List<ITextComponent> getErrors(Triple<LG, MG, RG> value) {
		List<ITextComponent> errors = super.getErrors(value);
		errors.addAll(leftEntry.getErrors(value.getLeft()));
		errors.addAll(middleEntry.getErrors(value.getMiddle()));
		errors.addAll(rightEntry.getErrors(value.getRight()));
		return errors;
	}
	
	@Override public Triple<LC, MC, RC> forConfig(Triple<L, M, R> value) {
		return Triple.of(leftEntry.forConfig(value.getLeft()),
							middleEntry.forConfig(value.getMiddle()),
		               rightEntry.forConfig(value.getRight()));
	}
	
	@Override public @Nullable Triple<L, M, R> fromConfig(@Nullable Triple<LC, MC, RC> value) {
		if (value == null) return null;
		return Triple.of(leftEntry.fromConfigOrDefault(value.getLeft()),
							middleEntry.fromConfigOrDefault(value.getMiddle()),
		               rightEntry.fromConfigOrDefault(value.getRight()));
	}
	
	@Override public Triple<LG, MG, RG> forGui(Triple<L, M, R> value) {
		return Triple.of(leftEntry.forGui(value.getLeft()),
							middleEntry.forGui(value.getMiddle()),
		               rightEntry.forGui(value.getRight()));
	}
	
	@Override public @Nullable Triple<L, M, R> fromGui(@Nullable Triple<LG, MG, RG> value) {
		if (value == null) return null;
		return Triple.of(leftEntry.fromGuiOrDefault(value.getLeft()),
							middleEntry.fromGuiOrDefault(value.getMiddle()),
		               rightEntry.fromGuiOrDefault(value.getRight()));
	}
	
	@Override public Optional<Triple<LC, MC, RC>> deserializeStringKey(@NotNull String key) {
		return Optional.ofNullable(fromActualConfig(key));
	}
	
	@Override public String serializeStringKey(@NotNull Triple<LC, MC, RC> key) {
		return forActualConfig(key);
	}
	
	@Override protected Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
		return Optional.of(decorate(builder).define(name, forActualConfig(forConfig(value)), createConfigValidator()));
	}
	
	@Override public Optional<AbstractConfigListEntry<Triple<LG, MG, RG>>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		TripleListEntryBuilder<LG, MG, RG, ?, ?, ?> entryBuilder = builder.startTriple(
		  getDisplayName(),
		  leftEntry.buildChildGUIEntry(builder),
		  middleEntry.buildChildGUIEntry(builder),
		  rightEntry.buildChildGUIEntry(builder),
		  forGui(get()))
		  .withIcons(leftIcon, rightIcon)
		  .withWeights(leftWeight, rightWeight);
		return Optional.of(decorate(entryBuilder).build());
	}
	
	@Override public ExpectedType getExpectedType() {
		return new ExpectedType(typeClass,
		                        leftEntry.getExpectedType(),
		                        middleEntry.getExpectedType(),
		                        rightEntry.getExpectedType());
	}
}
