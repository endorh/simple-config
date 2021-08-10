package endorh.simple_config.clothconfig2.impl.builders;

import endorh.simple_config.clothconfig2.gui.entries.LongSliderEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public class LongSliderBuilder
  extends FieldBuilder<Long, LongSliderEntry> {
	private Consumer<Long> saveConsumer = null;
	private Function<Long, Optional<ITextComponent[]>> tooltipSupplier = l -> Optional.empty();
	private final long value;
	private final long max;
	private final long min;
	private Function<Long, ITextComponent> textGetter = null;
	
	public LongSliderBuilder(
	  ITextComponent resetButtonKey, ITextComponent fieldNameKey, long value, long min, long max
	) {
		super(resetButtonKey, fieldNameKey);
		this.value = value;
		this.max = max;
		this.min = min;
	}
	
	public LongSliderBuilder setErrorSupplier(
	  Function<Long, Optional<ITextComponent>> errorSupplier
	) {
		this.errorSupplier = errorSupplier;
		return this;
	}
	
	public LongSliderBuilder requireRestart() {
		this.requireRestart(true);
		return this;
	}
	
	public LongSliderBuilder setTextGetter(Function<Long, ITextComponent> textGetter) {
		this.textGetter = textGetter;
		return this;
	}
	
	public LongSliderBuilder setSaveConsumer(Consumer<Long> saveConsumer) {
		this.saveConsumer = saveConsumer;
		return this;
	}
	
	public LongSliderBuilder setDefaultValue(Supplier<Long> defaultValue) {
		this.defaultValue = defaultValue;
		return this;
	}
	
	public LongSliderBuilder setDefaultValue(long defaultValue) {
		this.defaultValue = () -> defaultValue;
		return this;
	}
	
	public LongSliderBuilder setTooltipSupplier(
	  Function<Long, Optional<ITextComponent[]>> tooltipSupplier
	) {
		this.tooltipSupplier = tooltipSupplier;
		return this;
	}
	
	public LongSliderBuilder setTooltipSupplier(
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier
	) {
		this.tooltipSupplier = i -> tooltipSupplier.get();
		return this;
	}
	
	public LongSliderBuilder setTooltip(Optional<ITextComponent[]> tooltip) {
		this.tooltipSupplier = i -> tooltip;
		return this;
	}
	
	public LongSliderBuilder setTooltip(ITextComponent... tooltip) {
		this.tooltipSupplier = i -> Optional.ofNullable(tooltip);
		return this;
	}
	
	@Override
	@NotNull
	public LongSliderEntry build() {
		LongSliderEntry entry =
		  new LongSliderEntry(this.getFieldNameKey(), this.min, this.max, this.value,
		                      this.saveConsumer, this.getResetButtonKey(), this.defaultValue, null,
		                      this.isRequireRestart());
		if (this.textGetter != null) {
			entry.setTextGetter(this.textGetter);
		}
		entry.setTooltipSupplier(() -> this.tooltipSupplier.apply(entry.getValue()));
		if (this.errorSupplier != null) {
			entry.setErrorSupplier(() -> this.errorSupplier.apply(entry.getValue()));
		}
		return entry;
	}
}

