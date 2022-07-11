package endorh.simpleconfig.ui.gui.entries;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.function.Function;

@OnlyIn(value = Dist.CLIENT)
public class EnumListEntry<T extends Enum<?>> extends SelectionListEntry<T> {
	public static final Function<Enum<?>, ITextComponent> DEFAULT_NAME_PROVIDER =
	  t -> new TranslationTextComponent(
		 t instanceof SelectionListEntry.Translatable ? ((Translatable) t).getKey() : t.toString());
	
	@Internal public EnumListEntry(
	  ITextComponent fieldName, Class<T> clazz, T value,
	  Function<Enum<?>, ITextComponent> enumNameProvider
	) {
		super(fieldName, clazz.getEnumConstants(), value, enumNameProvider::apply);
	}
}

