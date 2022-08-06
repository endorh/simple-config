package endorh.simpleconfig.ui.gui.entries;

import endorh.simpleconfig.ui.api.EntryError;
import endorh.simpleconfig.ui.hotkey.HotKeyAction;
import endorh.simpleconfig.ui.hotkey.HotKeyActionType;
import endorh.simpleconfig.ui.hotkey.HotKeyActionTypes;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.List;
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
	
	@Override public List<EntryError> getHotKeyActionErrors(HotKeyActionType<E, ?> type) {
		List<EntryError> errors = super.getHotKeyActionErrors(type);
		if (type == HotKeyActionTypes.ENUM_ADD) errors.addAll(intEntry.getErrors());
		errors.addAll(intEntry.getEntryErrors());
		return errors;
	}
	
	@Override
	public void setHotKeyActionType(HotKeyActionType<E, ?> type, @Nullable HotKeyAction<E> prev) {
		super.setHotKeyActionType(type, prev);
	}
	
	@Override public void setHotKeyActionType(HotKeyActionType<E, ?> type) {
		super.setHotKeyActionType(type);
		widgetReference.setTarget(type == HotKeyActionTypes.ENUM_ADD? intEntry : buttonWidget);
	}
	
	@Override public Object getHotKeyActionValue() {
		if (getHotKeyActionType() == HotKeyActionTypes.ENUM_ADD) {
			return intEntry.getDisplayedValue();
		} else return super.getHotKeyActionValue();
	}
	
	@Override public void setHotKeyActionValue(Object value) {
		if (getHotKeyActionType() == HotKeyActionTypes.ENUM_ADD && value instanceof Number)
			intEntry.setDisplayedValue(((Number) value).intValue());
		super.setHotKeyActionValue(value);
	}
}
