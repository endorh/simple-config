package endorh.simple_config.gui;

import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.FieldBuilder;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class DoubleSliderBuilder extends FieldBuilder<Double, DoubleSliderEntry> {
	private Consumer<Double> saveConsumer = null;
	private Function<Double, Optional<ITextComponent[]>> tooltipSupplier = l -> Optional.empty();
	private final double value;
	private final double max;
	private final double min;
	private Function<Double, ITextComponent> textGetter = null;
	
	@Internal public DoubleSliderBuilder(ConfigEntryBuilder builder, ITextComponent fieldNameKey, double value, double min, double max) {
		this(builder.getResetButtonKey(), fieldNameKey, value, min, max);
	}
	
	@Internal public DoubleSliderBuilder(ITextComponent resetButtonKey, ITextComponent fieldNameKey, double value, double min, double max) {
		super(resetButtonKey, fieldNameKey);
		this.value = value;
		this.max = max;
		this.min = min;
	}
	
	public DoubleSliderBuilder setErrorSupplier(Function<Double, Optional<ITextComponent>> errorSupplier) {
		this.errorSupplier = errorSupplier;
		return this;
	}
	
	public DoubleSliderBuilder requireRestart() {
		this.requireRestart(true);
		return this;
	}
	
	public DoubleSliderBuilder setTextGetter(Function<Double, ITextComponent> textGetter) {
		this.textGetter = textGetter;
		return this;
	}
	
	public DoubleSliderBuilder setSaveConsumer(Consumer<Double> saveConsumer) {
		this.saveConsumer = saveConsumer;
		return this;
	}
	
	public DoubleSliderBuilder setDefaultValue(
	  Supplier<Double> defaultValue) {
		this.defaultValue = defaultValue;
		return this;
	}
	
	public DoubleSliderBuilder setDefaultValue(double defaultValue) {
		this.defaultValue = () -> defaultValue;
		return this;
	}
	
	public DoubleSliderBuilder setTooltipSupplier(Function<Double, Optional<ITextComponent[]>> tooltipSupplier) {
		this.tooltipSupplier = tooltipSupplier;
		return this;
	}
	
	public DoubleSliderBuilder setTooltipSupplier(Supplier<Optional<ITextComponent[]>> tooltipSupplier) {
		this.tooltipSupplier = i -> tooltipSupplier.get();
		return this;
	}
	
	public DoubleSliderBuilder setTooltip(ITextComponent... tooltip) {
		this.tooltipSupplier = i -> Optional.ofNullable(tooltip);
		return this;
	}
	
	@NotNull
	public DoubleSliderEntry build() {
		//noinspection deprecation
		DoubleSliderEntry entry = new DoubleSliderEntry(
		  getFieldNameKey(), min, max, value, saveConsumer,
		  getResetButtonKey(), defaultValue, null, isRequireRestart());
		
		if (this.textGetter != null) {
			entry.setTextGetter(this.textGetter);
		}
		
		entry.setTooltipSupplier(() -> tooltipSupplier.apply(entry.getValue()));
		
		if (this.errorSupplier != null) {
			entry.setErrorSupplier(() -> errorSupplier.apply(entry.getValue()));
		}
		
		return entry;
	}
}

