package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.gui.entries.DoubleListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(value = Dist.CLIENT)
public class DoubleFieldBuilder
  extends RangedFieldBuilder<Double, DoubleListEntry, DoubleFieldBuilder> {
	
	public DoubleFieldBuilder(ConfigEntryBuilder builder, ITextComponent name, double value) {
		super(DoubleListEntry.class, builder, name, value);
	}
	
	public DoubleFieldBuilder setMin(double min) {
		return setMin((Double) min);
	}
	
	public DoubleFieldBuilder setMax(double max) {
		return setMax((Double) max);
	}
	
	@Override
	@NotNull
	public DoubleListEntry buildEntry() {
		return new DoubleListEntry(
		  fieldNameKey, value
		);
	}
}

