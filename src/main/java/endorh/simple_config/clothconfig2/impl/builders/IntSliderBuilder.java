package endorh.simple_config.clothconfig2.impl.builders;

import endorh.simple_config.clothconfig2.gui.entries.IntegerSliderEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public class IntSliderBuilder
  extends FieldBuilder<Integer, IntegerSliderEntry> {
	private Consumer<Integer> saveConsumer = null;
	private Function<Integer, Optional<ITextComponent[]>> tooltipSupplier = i -> Optional.empty();
	private final int value;
	private int max;
	private int min;
	private Function<Integer, ITextComponent> textGetter = null;
	
	public IntSliderBuilder(
	  ITextComponent resetButtonKey, ITextComponent fieldNameKey, int value, int min, int max
	) {
		super(resetButtonKey, fieldNameKey);
		this.value = value;
		this.max = max;
		this.min = min;
	}
	
	public IntSliderBuilder setErrorSupplier(
	  Function<Integer, Optional<ITextComponent>> errorSupplier
	) {
		this.errorSupplier = errorSupplier;
		return this;
	}
	
	public IntSliderBuilder requireRestart() {
		this.requireRestart(true);
		return this;
	}
	
	public IntSliderBuilder setTextGetter(Function<Integer, ITextComponent> textGetter) {
		this.textGetter = textGetter;
		return this;
	}
	
	public IntSliderBuilder setSaveConsumer(Consumer<Integer> saveConsumer) {
		this.saveConsumer = saveConsumer;
		return this;
	}
	
	public IntSliderBuilder setDefaultValue(Supplier<Integer> defaultValue) {
		this.defaultValue = defaultValue;
		return this;
	}
	
	public IntSliderBuilder setDefaultValue(int defaultValue) {
		this.defaultValue = () -> defaultValue;
		return this;
	}
	
	public IntSliderBuilder setTooltipSupplier(
	  Function<Integer, Optional<ITextComponent[]>> tooltipSupplier
	) {
		this.tooltipSupplier = tooltipSupplier;
		return this;
	}
	
	public IntSliderBuilder setTooltipSupplier(
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier
	) {
		this.tooltipSupplier = i -> tooltipSupplier.get();
		return this;
	}
	
	public IntSliderBuilder setTooltip(Optional<ITextComponent[]> tooltip) {
		this.tooltipSupplier = i -> tooltip;
		return this;
	}
	
	public IntSliderBuilder setTooltip(ITextComponent... tooltip) {
		this.tooltipSupplier = i -> Optional.ofNullable(tooltip);
		return this;
	}
	
	public IntSliderBuilder setMax(int max) {
		this.max = max;
		return this;
	}
	
	public IntSliderBuilder setMin(int min) {
		this.min = min;
		return this;
	}
	
	@Override
	@NotNull
	public IntegerSliderEntry build() {
		IntegerSliderEntry entry =
		  new IntegerSliderEntry(this.getFieldNameKey(), this.min, this.max, this.value,
		                         this.getResetButtonKey(), this.defaultValue, this.saveConsumer, null,
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

