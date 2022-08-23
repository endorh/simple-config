package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.gui.entries.FloatListEntry;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(value = Dist.CLIENT)
public class FloatFieldBuilder
  extends RangedFieldBuilder<Float, FloatListEntry, FloatFieldBuilder> {
	
	public FloatFieldBuilder(ConfigFieldBuilder builder, Component name, float value) {
		super(FloatListEntry.class, builder, name, value);
	}
	
	@Override
	@NotNull
	public FloatListEntry buildEntry() {
		return new FloatListEntry(
		  this.fieldNameKey, this.value
		);
	}
}

