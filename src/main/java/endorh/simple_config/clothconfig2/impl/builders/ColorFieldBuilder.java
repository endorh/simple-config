package endorh.simple_config.clothconfig2.impl.builders;

import endorh.simple_config.clothconfig2.gui.entries.ColorEntry;
import endorh.simple_config.clothconfig2.math.Color;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public class ColorFieldBuilder
  extends FieldBuilder<String, ColorEntry> {
	private Consumer<Integer> saveConsumer = null;
	private Function<Integer, Optional<ITextComponent>> errorSupplier;
	private Function<Integer, Optional<ITextComponent[]>> tooltipSupplier = str -> Optional.empty();
	private final int value;
	private Supplier<Integer> defaultValue;
	private boolean alpha = false;
	
	public ColorFieldBuilder(ITextComponent resetButtonKey, ITextComponent fieldNameKey, int value) {
		super(resetButtonKey, fieldNameKey);
		this.value = value;
	}
	
	public ColorFieldBuilder setErrorSupplier(
	  Function<Integer, Optional<ITextComponent>> errorSupplier
	) {
		this.errorSupplier = errorSupplier;
		return this;
	}
	
	public ColorFieldBuilder requireRestart() {
		this.requireRestart(true);
		return this;
	}
	
	public ColorFieldBuilder setSaveConsumer(Consumer<Integer> saveConsumer) {
		this.saveConsumer = saveConsumer;
		return this;
	}
	
	public ColorFieldBuilder setSaveConsumer2(Consumer<Color> saveConsumer) {
		this.saveConsumer = integer -> saveConsumer.accept(
		  this.alpha ? Color.ofTransparent(integer) : Color.ofOpaque(integer));
		return this;
	}
	
	public ColorFieldBuilder setSaveConsumer3(Consumer<net.minecraft.util.text.Color> saveConsumer) {
		this.saveConsumer =
		  integer -> saveConsumer.accept(net.minecraft.util.text.Color.fromInt(integer));
		return this;
	}
	
	public ColorFieldBuilder setDefaultValue(Supplier<Integer> defaultValue) {
		this.defaultValue = defaultValue;
		return this;
	}
	
	public ColorFieldBuilder setDefaultValue2(Supplier<Color> defaultValue) {
		this.defaultValue = () -> defaultValue.get().getColor();
		return this;
	}
	
	public ColorFieldBuilder setDefaultValue3(Supplier<net.minecraft.util.text.Color> defaultValue) {
		this.defaultValue = () -> defaultValue.get().getColor();
		return this;
	}
	
	public ColorFieldBuilder setAlphaMode(boolean withAlpha) {
		this.alpha = withAlpha;
		return this;
	}
	
	public ColorFieldBuilder setDefaultValue(int defaultValue) {
		this.defaultValue = () -> defaultValue;
		return this;
	}
	
	public ColorFieldBuilder setDefaultValue(net.minecraft.util.text.Color defaultValue) {
		this.defaultValue = () -> Objects.requireNonNull(defaultValue).getColor();
		return this;
	}
	
	public ColorFieldBuilder setTooltipSupplier(
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier
	) {
		this.tooltipSupplier = str -> tooltipSupplier.get();
		return this;
	}
	
	public ColorFieldBuilder setTooltipSupplier(
	  Function<Integer, Optional<ITextComponent[]>> tooltipSupplier
	) {
		this.tooltipSupplier = tooltipSupplier;
		return this;
	}
	
	public ColorFieldBuilder setTooltip(Optional<ITextComponent[]> tooltip) {
		this.tooltipSupplier = str -> tooltip;
		return this;
	}
	
	public ColorFieldBuilder setTooltip(ITextComponent... tooltip) {
		this.tooltipSupplier = str -> Optional.ofNullable(tooltip);
		return this;
	}
	
	@Override
	@NotNull
	public ColorEntry build() {
		ColorEntry entry =
		  new ColorEntry(this.getFieldNameKey(), this.value, this.getResetButtonKey(),
		                 this.defaultValue, this.saveConsumer, null, this.isRequireRestart());
		if (this.alpha) {
			entry.withAlpha();
		} else {
			entry.withoutAlpha();
		}
		entry.setTooltipSupplier(() -> this.tooltipSupplier.apply(entry.getValue()));
		if (this.errorSupplier != null) {
			entry.setErrorSupplier(() -> this.errorSupplier.apply(entry.getValue()));
		}
		return entry;
	}
}

