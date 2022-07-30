package endorh.simpleconfig.ui.gui.entries;

import endorh.simpleconfig.ui.hotkey.HotKeyAction;
import endorh.simpleconfig.ui.hotkey.HotKeyActionType;
import endorh.simpleconfig.ui.hotkey.HotKeyActionTypes;
import endorh.simpleconfig.ui.hotkey.SimpleHotKeyActionType.SimpleHotKeyAction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@OnlyIn(value = Dist.CLIENT)
public class EnumListEntry<E extends Enum<?>> extends SelectionListEntry<E> {
	public static final Function<Enum<?>, ITextComponent> DEFAULT_NAME_PROVIDER =
	  t -> new TranslationTextComponent(
		 t instanceof SelectionListEntry.Translatable ? ((Translatable) t).getKey() : t.toString());
	
	@Internal public EnumListEntry(
	  ITextComponent fieldName, Class<E> clazz, E value,
	  Function<Enum<?>, ITextComponent> enumNameProvider
	) {
		super(fieldName, clazz.getEnumConstants(), value, enumNameProvider::apply);
		hotKeyActionTypes.add(HotKeyActionTypes.ENUM_ADD.cast());
	}
	
	@Override
	public void setHotKeyActionType(HotKeyActionType<E, ?> type, @Nullable HotKeyAction<E> prev) {
		super.setHotKeyActionType(type, prev);
		if (type == HotKeyActionTypes.ENUM_ADD && prev instanceof SimpleHotKeyAction) {
			int step = ((SimpleHotKeyAction<E, Integer>) prev).getStorage();
			intEntry.setValue(step);
		}
	}
}
