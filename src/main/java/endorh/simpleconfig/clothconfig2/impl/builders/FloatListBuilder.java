package endorh.simpleconfig.clothconfig2.impl.builders;

import endorh.simpleconfig.clothconfig2.api.ConfigEntryBuilder;
import endorh.simpleconfig.clothconfig2.gui.entries.FloatListListEntry;
import endorh.simpleconfig.clothconfig2.gui.entries.FloatListListEntry.FloatListCell;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@OnlyIn(value = Dist.CLIENT)
public class FloatListBuilder
  extends RangedListFieldBuilder<Float, FloatListCell, FloatListListEntry, FloatListBuilder> {
	
	public FloatListBuilder(ConfigEntryBuilder builder, ITextComponent name, List<Float> value) {
		super(builder, name, value);
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

