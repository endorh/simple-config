package endorh.simple_config.core.entry;

import endorh.simple_config.core.ISimpleConfigEntryHolder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.IntFieldBuilder;
import me.shedaniel.clothconfig2.impl.builders.IntSliderBuilder;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@Deprecated
public class ShortEntry extends AbstractRangedEntry<Short, Number, Integer, ShortEntry>
  implements IAbstractStringKeyEntry<Short> {
	@Internal public ShortEntry(
	  ISimpleConfigEntryHolder parent, String name, short value
	) {
		super(parent, name, value, Short.class);
	}
	
	public static class Builder extends AbstractRangedEntry.Builder<Short, Number, Integer, ShortEntry, Builder> {
		public Builder(Short value) {
			super(value, Short.class);
		}
		
		public Builder min(short min) {
			return super.min(min);
		}
		public Builder max(short max) {
			return super.max(max);
		}
		public Builder range(short min, short max) {
			return super.range(min, max);
		}
		
		@Override
		protected void checkBounds() {
			min = min == null ? Short.MIN_VALUE : min;
			max = max == null ? Short.MAX_VALUE : max;
			super.checkBounds();
		}
		
		@Override
		public ShortEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			return new ShortEntry(parent, name, value);
		}
	}
	
	@Nullable
	@Override
	protected Short fromConfig(@Nullable Number value) {
		return value != null? value.shortValue() : null;
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	protected Optional<AbstractConfigListEntry<Integer>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		if (!asSlider) {
			final IntFieldBuilder valBuilder = builder
			  .startIntField(getDisplayName(), get())
			  .setDefaultValue(value)
			  .setMin(min).setMax(max)
			  .setSaveConsumer(saveConsumer())
			  .setTooltipSupplier(this::supplyTooltip)
			  .setErrorSupplier(this::supplyError);
			return Optional.of(decorate(valBuilder).build());
		} else {
			final IntSliderBuilder valBuilder = builder
			  .startIntSlider(getDisplayName(), get(), min, max)
			  .setDefaultValue(value)
			  .setSaveConsumer(saveConsumer())
			  .setTooltipSupplier(this::supplyTooltip)
			  .setTooltipSupplier(this::supplyTooltip)
			  .setErrorSupplier(this::supplyError)
			  .setTextGetter(g -> sliderTextSupplier.apply(fromGui(g)));
			return Optional.of(decorate(valBuilder).build());
		}
	}
	
	@Override
	public Optional<Short> deserializeStringKey(String key) {
		try {
			return Optional.of(Short.parseShort(key));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}
	
	@Override
	public Optional<ITextComponent> stringKeyError(String key) {
		try {
			final int i = Integer.parseInt(key);
			if (i > max)
				return Optional.of(new TranslationTextComponent(
				  "text.cloth-config.error.too_large", max));
			else if (i < min)
				return Optional.of(new TranslationTextComponent(
				  "text.cloth-config.error.too_small", min));
			return supplyError(i);
		} catch (NumberFormatException e) {
			return Optional.of(new TranslationTextComponent(
			  "text.cloth-config.error.not_valid_number_int"));
		}
	}
}
