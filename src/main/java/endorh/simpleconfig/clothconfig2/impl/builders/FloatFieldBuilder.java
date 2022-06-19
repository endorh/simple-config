package endorh.simpleconfig.clothconfig2.impl.builders;

import endorh.simpleconfig.clothconfig2.api.ConfigEntryBuilder;
import endorh.simpleconfig.clothconfig2.gui.entries.FloatListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(value = Dist.CLIENT)
public class FloatFieldBuilder
  extends RangedFieldBuilder<Float, FloatListEntry, FloatFieldBuilder> {
	
	public FloatFieldBuilder(ConfigEntryBuilder builder, ITextComponent name, float value) {
		super(builder, name, value);
	}
	
	@Override
	@NotNull
	public FloatListEntry buildEntry() {
		return new FloatListEntry(
		  this.fieldNameKey, this.value
		);
	}
}

