package endorh.simple_config.clothconfig2.impl.builders;

import endorh.simple_config.clothconfig2.gui.entries.TextListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public class TextDescriptionBuilder
  extends FieldBuilder<ITextComponent, TextListEntry> {
	private int color = -1;
	@Nullable
	private Supplier<Optional<ITextComponent[]>> tooltipSupplier = null;
	private final ITextComponent value;
	
	public TextDescriptionBuilder(
	  ITextComponent resetButtonKey, ITextComponent fieldNameKey, ITextComponent value
	) {
		super(resetButtonKey, fieldNameKey);
		this.value = value;
	}
	
	@Override
	public void requireRestart(boolean requireRestart) {
		throw new UnsupportedOperationException();
	}
	
	public TextDescriptionBuilder setTooltipSupplier(
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier
	) {
		this.tooltipSupplier = tooltipSupplier;
		return this;
	}
	
	public TextDescriptionBuilder setTooltip(Optional<ITextComponent[]> tooltip) {
		this.tooltipSupplier = () -> tooltip;
		return this;
	}
	
	public TextDescriptionBuilder setTooltip(ITextComponent... tooltip) {
		this.tooltipSupplier = () -> Optional.ofNullable(tooltip);
		return this;
	}
	
	public TextDescriptionBuilder setColor(int color) {
		this.color = color;
		return this;
	}
	
	@Override
	@NotNull
	public TextListEntry build() {
		return new TextListEntry(
		  this.getFieldNameKey(), this.value, this.color, this.tooltipSupplier);
	}
}

