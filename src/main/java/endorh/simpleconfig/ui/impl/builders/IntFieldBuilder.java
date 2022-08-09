package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.gui.entries.IntegerListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(value = Dist.CLIENT)
public class IntFieldBuilder
  extends RangedFieldBuilder<Integer, IntegerListEntry, IntFieldBuilder> {
	
	public IntFieldBuilder(ConfigEntryBuilder builder, ITextComponent name, int value) {
		super(IntegerListEntry.class, builder, name, value);
	}
	
	@Override
	@NotNull
	public IntegerListEntry buildEntry() {
		return new IntegerListEntry(
		  fieldNameKey, value
		);
	}
}

