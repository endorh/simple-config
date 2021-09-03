package endorh.simple_config.clothconfig2.impl.builders;

import endorh.simple_config.clothconfig2.api.ConfigEntryBuilder;
import endorh.simple_config.clothconfig2.gui.entries.ColorListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(value = Dist.CLIENT)
public class ColorFieldBuilder
  extends FieldBuilder<Integer, ColorListEntry, ColorFieldBuilder> {
	private boolean alpha = false;
	
	public ColorFieldBuilder(ConfigEntryBuilder builder, ITextComponent name, int value) {
		super(builder, name, value);
	}
	
	public ColorFieldBuilder setAlphaMode(boolean withAlpha) {
		this.alpha = withAlpha;
		return this;
	}
	
	@Override
	@NotNull
	public ColorListEntry buildEntry() {
		ColorListEntry entry = new ColorListEntry(
		  fieldNameKey, value);
		if (this.alpha)
			entry.withAlpha();
		else entry.withoutAlpha();
		return entry;
	}
}

