package endorh.simple_config.clothconfig2.impl.builders;

import endorh.simple_config.clothconfig2.gui.entries.SelectionListEntry;
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
public class SelectorBuilder<T>
  extends FieldBuilder<T, SelectionListEntry<T>> {
	private Consumer<T> saveConsumer = null;
	private Function<T, Optional<ITextComponent[]>> tooltipSupplier = e -> Optional.empty();
	private final T value;
	private final T[] valuesArray;
	private Function<T, ITextComponent> nameProvider = null;
	
	public SelectorBuilder(
	  ITextComponent resetButtonKey, ITextComponent fieldNameKey, T[] valuesArray, T value
	) {
		super(resetButtonKey, fieldNameKey);
		Objects.requireNonNull(value);
		this.valuesArray = valuesArray;
		this.value = value;
	}
	
	public SelectorBuilder<T> setErrorSupplier(Function<T, Optional<ITextComponent>> errorSupplier) {
		this.errorSupplier = errorSupplier;
		return this;
	}
	
	public SelectorBuilder<T> requireRestart() {
		this.requireRestart(true);
		return this;
	}
	
	public SelectorBuilder<T> setSaveConsumer(Consumer<T> saveConsumer) {
		this.saveConsumer = saveConsumer;
		return this;
	}
	
	public SelectorBuilder<T> setDefaultValue(Supplier<T> defaultValue) {
		this.defaultValue = defaultValue;
		return this;
	}
	
	public SelectorBuilder<T> setDefaultValue(T defaultValue) {
		Objects.requireNonNull(defaultValue);
		this.defaultValue = () -> defaultValue;
		return this;
	}
	
	public SelectorBuilder<T> setTooltipSupplier(
	  Function<T, Optional<ITextComponent[]>> tooltipSupplier
	) {
		this.tooltipSupplier = tooltipSupplier;
		return this;
	}
	
	public SelectorBuilder<T> setTooltipSupplier(
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier
	) {
		this.tooltipSupplier = e -> tooltipSupplier.get();
		return this;
	}
	
	public SelectorBuilder<T> setTooltip(Optional<ITextComponent[]> tooltip) {
		this.tooltipSupplier = e -> tooltip;
		return this;
	}
	
	public SelectorBuilder<T> setTooltip(ITextComponent... tooltip) {
		this.tooltipSupplier = e -> Optional.ofNullable(tooltip);
		return this;
	}
	
	public SelectorBuilder<T> setNameProvider(Function<T, ITextComponent> enumNameProvider) {
		this.nameProvider = enumNameProvider;
		return this;
	}
	
	@Override
	@NotNull
	public SelectionListEntry<T> build() {
		SelectionListEntry<T> entry =
        new SelectionListEntry<>(this.getFieldNameKey(), this.valuesArray, this.value,
                                 this.getResetButtonKey(), this.defaultValue, this.saveConsumer,
                                 this.nameProvider, null, this.isRequireRestart());
		entry.setTooltipSupplier(() -> this.tooltipSupplier.apply(entry.getValue()));
		if (this.errorSupplier != null) {
			entry.setErrorSupplier(() -> this.errorSupplier.apply(entry.getValue()));
		}
		return entry;
	}
}

