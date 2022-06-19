package endorh.simpleconfig.clothconfig2.gui.entries;

import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;

@OnlyIn(value = Dist.CLIENT)
public class StringListEntry extends TextFieldListEntry<String> {
	@Internal public StringListEntry(ITextComponent fieldName, String value) {
		super(fieldName, value, true);
	}
	
	@Override protected String fromString(String s) {
		return s;
	}
}

