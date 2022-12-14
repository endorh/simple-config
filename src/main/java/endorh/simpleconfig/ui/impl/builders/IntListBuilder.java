package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.gui.entries.IntegerListListEntry;
import endorh.simpleconfig.ui.gui.entries.IntegerListListEntry.IntegerListCell;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@OnlyIn(value = Dist.CLIENT)
public class IntListBuilder
  extends RangedListFieldBuilder<Integer, IntegerListCell, IntegerListListEntry, IntListBuilder> {
	
	public IntListBuilder(ConfigFieldBuilder builder, Component name, List<Integer> value) {
		super(IntegerListListEntry.class, builder, name, value);
	}
	
	public IntListBuilder setMin(int min) {
		return super.setMin(min);
	}
	
	public IntListBuilder setMax(int max) {
		return super.setMax(max);
	}
	
	@Override @NotNull public IntegerListListEntry buildEntry() {
		return new IntegerListListEntry(
		  this.fieldNameKey, this.value
		);
	}
}

