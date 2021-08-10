package endorh.simple_config.clothconfig2.impl.builders;

import endorh.simple_config.clothconfig2.gui.entries.DoubleListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public class DoubleFieldBuilder
  extends FieldBuilder<Double, DoubleListEntry> {
	private Consumer<Double> saveConsumer = null;
	private Function<Double, Optional<ITextComponent[]>> tooltipSupplier = d -> Optional.empty();
	private final double value;
	private Double min = null;
	private Double max = null;
	
	public DoubleFieldBuilder(
	  ITextComponent resetButtonKey, ITextComponent fieldNameKey, double value
	) {
		super(resetButtonKey, fieldNameKey);
		this.value = value;
	}
	
	public DoubleFieldBuilder setErrorSupplier(
	  Function<Double, Optional<ITextComponent>> errorSupplier
	) {
		this.errorSupplier = errorSupplier;
		return this;
	}
	
	public DoubleFieldBuilder requireRestart() {
		this.requireRestart(true);
		return this;
	}
	
	public DoubleFieldBuilder setSaveConsumer(Consumer<Double> saveConsumer) {
		this.saveConsumer = saveConsumer;
		return this;
	}
	
	public DoubleFieldBuilder setDefaultValue(Supplier<Double> defaultValue) {
		this.defaultValue = defaultValue;
		return this;
	}
	
	public DoubleFieldBuilder setDefaultValue(double defaultValue) {
		this.defaultValue = () -> defaultValue;
		return this;
	}
	
	public DoubleFieldBuilder setMin(double min) {
		this.min = min;
		return this;
	}
	
	public DoubleFieldBuilder setMax(double max) {
		this.max = max;
		return this;
	}
	
	public DoubleFieldBuilder removeMin() {
		this.min = null;
		return this;
	}
	
	public DoubleFieldBuilder removeMax() {
		this.max = null;
		return this;
	}
	
	public DoubleFieldBuilder setTooltipSupplier(
	  Function<Double, Optional<ITextComponent[]>> tooltipSupplier
	) {
		this.tooltipSupplier = tooltipSupplier;
		return this;
	}
	
	public DoubleFieldBuilder setTooltipSupplier(
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier
	) {
		this.tooltipSupplier = d -> tooltipSupplier.get();
		return this;
	}
	
	public DoubleFieldBuilder setTooltip(Optional<ITextComponent[]> tooltip) {
		this.tooltipSupplier = d -> tooltip;
		return this;
	}
	
	public DoubleFieldBuilder setTooltip(ITextComponent... tooltip) {
		this.tooltipSupplier = d -> Optional.ofNullable(tooltip);
		return this;
	}
	
	@Override
	@NotNull
	public DoubleListEntry build() {
		DoubleListEntry entry =
		  new DoubleListEntry(this.getFieldNameKey(), this.value, this.getResetButtonKey(),
		                      this.defaultValue, this.saveConsumer, null, this.isRequireRestart());
		if (this.min != null) {
			entry.setMinimum(this.min);
		}
		if (this.max != null) {
			entry.setMaximum(this.max);
		}
		entry.setTooltipSupplier(() -> this.tooltipSupplier.apply(entry.getValue()));
		if (this.errorSupplier != null) {
			entry.setErrorSupplier(() -> this.errorSupplier.apply(entry.getValue()));
		}
		return entry;
	}
}

