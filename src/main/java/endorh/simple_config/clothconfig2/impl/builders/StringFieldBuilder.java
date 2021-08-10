package endorh.simple_config.clothconfig2.impl.builders;

import endorh.simple_config.clothconfig2.gui.entries.StringListEntry;
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
public class StringFieldBuilder
  extends FieldBuilder<String, StringListEntry> {
	private Consumer<String> saveConsumer = null;
	private Function<String, Optional<ITextComponent[]>> tooltipSupplier = str -> Optional.empty();
	private final String value;
	
	public StringFieldBuilder(
	  ITextComponent resetButtonKey, ITextComponent fieldNameKey, String value
	) {
		super(resetButtonKey, fieldNameKey);
		Objects.requireNonNull(value);
		this.value = value;
	}
	
	public StringFieldBuilder setErrorSupplier(
	  Function<String, Optional<ITextComponent>> errorSupplier
	) {
		this.errorSupplier = errorSupplier;
		return this;
	}
	
	public StringFieldBuilder requireRestart() {
		this.requireRestart(true);
		return this;
	}
	
	public StringFieldBuilder setSaveConsumer(Consumer<String> saveConsumer) {
		this.saveConsumer = saveConsumer;
		return this;
	}
	
	public StringFieldBuilder setDefaultValue(Supplier<String> defaultValue) {
		this.defaultValue = defaultValue;
		return this;
	}
	
	public StringFieldBuilder setDefaultValue(String defaultValue) {
		this.defaultValue = () -> Objects.requireNonNull(defaultValue);
		return this;
	}
	
	public StringFieldBuilder setTooltipSupplier(
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier
	) {
		this.tooltipSupplier = str -> tooltipSupplier.get();
		return this;
	}
	
	public StringFieldBuilder setTooltipSupplier(
	  Function<String, Optional<ITextComponent[]>> tooltipSupplier
	) {
		this.tooltipSupplier = tooltipSupplier;
		return this;
	}
	
	public StringFieldBuilder setTooltip(Optional<ITextComponent[]> tooltip) {
		this.tooltipSupplier = str -> tooltip;
		return this;
	}
	
	public StringFieldBuilder setTooltip(ITextComponent... tooltip) {
		this.tooltipSupplier = str -> Optional.ofNullable(tooltip);
		return this;
	}
	
	@Override
	@NotNull
	public StringListEntry build() {
		StringListEntry entry =
		  new StringListEntry(this.getFieldNameKey(), this.value, this.getResetButtonKey(),
		                      this.defaultValue, this.saveConsumer, null, this.isRequireRestart());
		entry.setTooltipSupplier(() -> this.tooltipSupplier.apply(entry.getValue()));
		if (this.errorSupplier != null) {
			entry.setErrorSupplier(() -> this.errorSupplier.apply(entry.getValue()));
		}
		return entry;
	}
}

