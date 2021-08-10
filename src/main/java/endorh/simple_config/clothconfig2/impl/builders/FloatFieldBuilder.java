package endorh.simple_config.clothconfig2.impl.builders;

import endorh.simple_config.clothconfig2.gui.entries.FloatListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public class FloatFieldBuilder
  extends FieldBuilder<Float, FloatListEntry> {
	private Consumer<Float> saveConsumer = null;
	private Function<Float, Optional<ITextComponent[]>> tooltipSupplier = f -> Optional.empty();
	private final float value;
	private Float min = null;
	private Float max = null;
	
	public FloatFieldBuilder(
	  ITextComponent resetButtonKey, ITextComponent fieldNameKey, float value
	) {
		super(resetButtonKey, fieldNameKey);
		this.value = value;
	}
	
	public FloatFieldBuilder setErrorSupplier(
	  Function<Float, Optional<ITextComponent>> errorSupplier
	) {
		this.errorSupplier = errorSupplier;
		return this;
	}
	
	public FloatFieldBuilder requireRestart() {
		this.requireRestart(true);
		return this;
	}
	
	public FloatFieldBuilder setSaveConsumer(Consumer<Float> saveConsumer) {
		this.saveConsumer = saveConsumer;
		return this;
	}
	
	public FloatFieldBuilder setDefaultValue(Supplier<Float> defaultValue) {
		this.defaultValue = defaultValue;
		return this;
	}
	
	public FloatFieldBuilder setDefaultValue(float defaultValue) {
		this.defaultValue = () -> defaultValue;
		return this;
	}
	
	public FloatFieldBuilder setTooltipSupplier(
	  Function<Float, Optional<ITextComponent[]>> tooltipSupplier
	) {
		this.tooltipSupplier = tooltipSupplier;
		return this;
	}
	
	public FloatFieldBuilder setTooltipSupplier(
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier
	) {
		this.tooltipSupplier = f -> tooltipSupplier.get();
		return this;
	}
	
	public FloatFieldBuilder setTooltip(Optional<ITextComponent[]> tooltip) {
		this.tooltipSupplier = f -> tooltip;
		return this;
	}
	
	public FloatFieldBuilder setTooltip(ITextComponent... tooltip) {
		this.tooltipSupplier = f -> Optional.ofNullable(tooltip);
		return this;
	}
	
	public FloatFieldBuilder setMin(float min) {
		this.min = min;
		return this;
	}
	
	public FloatFieldBuilder setMax(float max) {
		this.max = max;
		return this;
	}
	
	public FloatFieldBuilder removeMin() {
		this.min = null;
		return this;
	}
	
	public FloatFieldBuilder removeMax() {
		this.max = null;
		return this;
	}
	
	@Override
	@NotNull
	public FloatListEntry build() {
		FloatListEntry entry = new FloatListEntry(
		  this.getFieldNameKey(), this.value, this.getResetButtonKey(), this.defaultValue,
		  this.saveConsumer, null, this.isRequireRestart());
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

