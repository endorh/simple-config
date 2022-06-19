package endorh.simpleconfig.clothconfig2.impl.builders;

import endorh.simpleconfig.clothconfig2.api.ConfigEntryBuilder;
import endorh.simpleconfig.clothconfig2.gui.entries.StringListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@OnlyIn(value = Dist.CLIENT)
public class TextFieldBuilder extends FieldBuilder<String, StringListEntry, TextFieldBuilder> {
	
	protected int maxLength = Integer.MAX_VALUE;
	
	public TextFieldBuilder(
	  ConfigEntryBuilder builder, ITextComponent name, String value
	) {
		super(builder, name, Objects.requireNonNull(value));
	}
	
	public TextFieldBuilder setMaxLength(int maxLength) {
		this.maxLength = maxLength;
		return self();
	}
	
	@Override
	@NotNull
	public StringListEntry buildEntry() {
		final StringListEntry entry = new StringListEntry(fieldNameKey, value);
		entry.setMaxLength(maxLength);
		return entry;
	}
}

