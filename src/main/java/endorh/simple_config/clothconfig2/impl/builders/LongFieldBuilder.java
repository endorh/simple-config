package endorh.simple_config.clothconfig2.impl.builders;

import endorh.simple_config.clothconfig2.gui.entries.LongListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public class LongFieldBuilder
  extends FieldBuilder<Long, LongListEntry> {
	private Consumer<Long> saveConsumer = null;
	private Function<Long, Optional<ITextComponent[]>> tooltipSupplier = l -> Optional.empty();
	private final long value;
	private Long min = null;
	private Long max = null;
	
	public LongFieldBuilder(ITextComponent resetButtonKey, ITextComponent fieldNameKey, long value) {
		super(resetButtonKey, fieldNameKey);
		this.value = value;
	}
	
	public LongFieldBuilder setErrorSupplier(
	  Function<Long, Optional<ITextComponent>> errorSupplier
	) {
		this.errorSupplier = errorSupplier;
		return this;
	}
	
	public LongFieldBuilder requireRestart() {
		this.requireRestart(true);
		return this;
	}
	
	public LongFieldBuilder setSaveConsumer(Consumer<Long> saveConsumer) {
		this.saveConsumer = saveConsumer;
		return this;
	}
	
	public LongFieldBuilder setDefaultValue(Supplier<Long> defaultValue) {
		this.defaultValue = defaultValue;
		return this;
	}
	
	public LongFieldBuilder setDefaultValue(long defaultValue) {
		this.defaultValue = () -> defaultValue;
		return this;
	}
	
	public LongFieldBuilder setTooltipSupplier(
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier
	) {
		this.tooltipSupplier = l -> tooltipSupplier.get();
		return this;
	}
	
	public LongFieldBuilder setTooltipSupplier(
	  Function<Long, Optional<ITextComponent[]>> tooltipSupplier
	) {
		this.tooltipSupplier = tooltipSupplier;
		return this;
	}
	
	public LongFieldBuilder setTooltip(Optional<ITextComponent[]> tooltip) {
		this.tooltipSupplier = l -> tooltip;
		return this;
	}
	
	public LongFieldBuilder setTooltip(ITextComponent... tooltip) {
		this.tooltipSupplier = l -> Optional.ofNullable(tooltip);
		return this;
	}
	
	public LongFieldBuilder setMin(long min) {
		this.min = min;
		return this;
	}
	
	public LongFieldBuilder setMax(long max) {
		this.max = max;
		return this;
	}
	
	public LongFieldBuilder removeMin() {
		this.min = null;
		return this;
	}
	
	public LongFieldBuilder removeMax() {
		this.max = null;
		return this;
	}
	
	@Override
	@NotNull
	public LongListEntry build() {
		LongListEntry entry =
		  new LongListEntry(this.getFieldNameKey(), this.value, this.getResetButtonKey(),
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

