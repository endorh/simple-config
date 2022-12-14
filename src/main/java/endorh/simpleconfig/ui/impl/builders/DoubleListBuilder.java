package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.gui.entries.DoubleListListEntry;
import endorh.simpleconfig.ui.gui.entries.DoubleListListEntry.DoubleListCell;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@OnlyIn(value = Dist.CLIENT)
public class DoubleListBuilder extends RangedListFieldBuilder<
  Double, DoubleListCell, DoubleListListEntry, DoubleListBuilder> {
	
	public DoubleListBuilder(
	  ConfigFieldBuilder builder, Component name, List<Double> value
	) {
		super(DoubleListListEntry.class, builder, name, value);
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

