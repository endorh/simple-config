package endorh.simple_config.clothconfig2.impl.builders;

import endorh.simple_config.clothconfig2.api.ConfigEntryBuilder;
import endorh.simple_config.clothconfig2.gui.entries.StringListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@OnlyIn(value = Dist.CLIENT)
public class StringFieldBuilder
  extends FieldBuilder<String, StringListEntry, StringFieldBuilder> {
	
	public StringFieldBuilder(
	  ConfigEntryBuilder builder, ITextComponent name, String value
	) {
		super(builder, name, Objects.requireNonNull(value));
	}
	
	@Override
	@NotNull
	public StringListEntry buildEntry() {
		return new StringListEntry(
		  this.fieldNameKey, this.value
		);
	}
}

