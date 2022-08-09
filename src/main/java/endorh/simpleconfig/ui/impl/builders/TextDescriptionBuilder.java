package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.gui.entries.TextListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public class TextDescriptionBuilder
  extends FieldBuilder<Void, TextListEntry, TextDescriptionBuilder> {
	protected int color = -1;
	protected Supplier<ITextComponent> textSupplier;
	
	public TextDescriptionBuilder(
	  ConfigEntryBuilder builder, ITextComponent name, Supplier<ITextComponent> value
	) {
		super(TextListEntry.class, builder, name, null);
		this.textSupplier = value;
	}
	
	@Override public TextDescriptionBuilder requireRestart(boolean requireRestart) {
		return self();
	}
	
	public TextDescriptionBuilder setColor(int color) {
		this.color = color;
		return this;
	}
	
	@Override @NotNull public TextListEntry buildEntry() {
		return new TextListEntry(
		  this.fieldNameKey, this.textSupplier, this.color);
	}
}

