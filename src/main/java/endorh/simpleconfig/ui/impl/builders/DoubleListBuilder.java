package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.gui.entries.DoubleListListEntry;
import endorh.simpleconfig.ui.gui.entries.DoubleListListEntry.DoubleListCell;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@OnlyIn(value = Dist.CLIENT)
public class DoubleListBuilder extends RangedListFieldBuilder<
  Double, DoubleListCell, DoubleListListEntry, DoubleListBuilder> {
	
	public DoubleListBuilder(
	  ConfigEntryBuilder builder, ITextComponent name, List<Double> value
	) {
		super(builder, name, value);
	}
	
	public DoubleListBuilder setMin(double min) {
		return super.setMin(min);
	}
	
	public DoubleListBuilder setMax(double max) {
		return super.setMax(max);
	}
	
	@Override
	@NotNull
	public DoubleListListEntry buildEntry() {
		return new DoubleListListEntry(fieldNameKey, value);
	}
}

