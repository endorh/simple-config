package endorh.simple_config.clothconfig2.impl.builders;

import endorh.simple_config.clothconfig2.api.ConfigEntryBuilder;
import endorh.simple_config.clothconfig2.gui.entries.StringListListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@OnlyIn(value = Dist.CLIENT)
public class StringListBuilder
  extends ListFieldBuilder<String, StringListListEntry, StringListBuilder> {
	
	public StringListBuilder(
	  ConfigEntryBuilder builder, ITextComponent name, List<String> value
	) {
		super(builder, name, value);
	}
	
	@Override @NotNull public StringListListEntry buildEntry() {
		return new StringListListEntry(
		  fieldNameKey, value
		);
	}
}

