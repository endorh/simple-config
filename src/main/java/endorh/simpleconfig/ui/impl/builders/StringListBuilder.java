package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.gui.entries.StringListListEntry;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@OnlyIn(value = Dist.CLIENT)
public class StringListBuilder
  extends ListFieldBuilder<String, StringListListEntry, StringListBuilder> {
	
	public StringListBuilder(
	  ConfigFieldBuilder builder, Component name, List<String> value
	) {
		super(StringListListEntry.class, builder, name, value);
	}
	
	@Override @NotNull public StringListListEntry buildEntry() {
		return new StringListListEntry(
		  fieldNameKey, value
		);
	}
}

