package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.gui.entries.FloatListListEntry;
import endorh.simpleconfig.ui.gui.entries.FloatListListEntry.FloatListCell;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@OnlyIn(value = Dist.CLIENT)
public class FloatListBuilder
  extends RangedListFieldBuilder<Float, FloatListCell, FloatListListEntry, FloatListBuilder> {
	
	public FloatListBuilder(ConfigFieldBuilder builder, Component name, List<Float> value) {
		super(FloatListListEntry.class, builder, name, value);
	}
	
	public FloatListBuilder setMin(float min) {
		return super.setMin(min);
	}
	
	public FloatListBuilder setMax(float max) {
		return super.setMax(max);
	}
	
	@Override
	@NotNull
	public FloatListListEntry buildEntry() {
		return new FloatListListEntry(
		  this.fieldNameKey, this.value
		);
	}
}

