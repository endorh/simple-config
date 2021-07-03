package endorh.simple_config.gui;

import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.FieldBuilder;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class FloatSliderBuilder extends FieldBuilder<Float, FloatSliderEntry> {
	private Consumer<Float> saveConsumer = null;
	private Function<Float, Optional<ITextComponent[]>> tooltipSupplier = l -> Optional.empty();
	private final float value;
	private final float max;
	private final float min;
	private Function<Float, ITextComponent> textGetter = null;
	
	public FloatSliderBuilder(ConfigEntryBuilder builder, ITextComponent fieldNameKey, float value, float min, float max) {
		this(builder.getResetButtonKey(), fieldNameKey, value, min, max);
	}
	
	public FloatSliderBuilder(ITextComponent resetButtonKey, ITextComponent fieldNameKey, float value, float min, float max) {
		super(resetButtonKey, fieldNameKey);
		this.value = value;
		this.max = max;
		this.min = min;
	}
	
	public FloatSliderBuilder setErrorSupplier(Function<Float, Optional<ITextComponent>> errorSupplier) {
		this.errorSupplier = errorSupplier;
		return this;
	}
	
	public FloatSliderBuilder requireRestart() {
		this.requireRestart(true);
		return this;
	}
	
	public FloatSliderBuilder setTextGetter(Function<Float, ITextComponent> textGetter) {
		this.textGetter = textGetter;
		return this;
	}
	
	public FloatSliderBuilder setSaveConsumer(Consumer<Float> saveConsumer) {
		this.saveConsumer = saveConsumer;
		return this;
	}
	
	public FloatSliderBuilder setDefaultValue(
	  Supplier<Float> defaultValue) {
		this.defaultValue = defaultValue;
		return this;
	}
	
	public FloatSliderBuilder setDefaultValue(float defaultValue) {
		this.defaultValue = () -> defaultValue;
		return this;
	}
	
	public FloatSliderBuilder setTooltipSupplier(Function<Float, Optional<ITextComponent[]>> tooltipSupplier) {
		this.tooltipSupplier = tooltipSupplier;
		return this;
	}
	
	public FloatSliderBuilder setTooltipSupplier(Supplier<Optional<ITextComponent[]>> tooltipSupplier) {
		this.tooltipSupplier = i -> tooltipSupplier.get();
		return this;
	}
	
	public FloatSliderBuilder setTooltip(ITextComponent... tooltip) {
		this.tooltipSupplier = i -> Optional.ofNullable(tooltip);
		return this;
	}
	
	@NotNull
	public FloatSliderEntry build() {
		//noinspection deprecation
		FloatSliderEntry entry = new FloatSliderEntry(
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

