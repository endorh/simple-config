package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.api.ITextFormatter;
import endorh.simpleconfig.ui.gui.entries.StringListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@OnlyIn(value = Dist.CLIENT)
public class TextFieldBuilder extends FieldBuilder<String, StringListEntry, TextFieldBuilder> {
	protected int maxLength = Integer.MAX_VALUE;
	protected ITextFormatter textFormatter = ITextFormatter.DEFAULT;
	
	public TextFieldBuilder(
	  ConfigFieldBuilder builder, ITextComponent name, String value
	) {
		super(StringListEntry.class, builder, name, Objects.requireNonNull(value));
	}
	
	public TextFieldBuilder setMaxLength(int maxLength) {
		this.maxLength = maxLength;
		return self();
	}
	
	public TextFieldBuilder setTextFormatter(ITextFormatter textFormatter) {
		this.textFormatter = textFormatter;
		return self();
	}
	
	@Override
	@NotNull
	public StringListEntry buildEntry() {
		final StringListEntry entry = new StringListEntry(fieldNameKey, value);
		entry.setMaxLength(maxLength);
		entry.setTextFormatter(textFormatter);
		return entry;
	}
}

