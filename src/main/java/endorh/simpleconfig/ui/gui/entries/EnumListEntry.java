package endorh.simpleconfig.ui.gui.entries;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.api.ui.math.Point;
import endorh.simpleconfig.ui.api.EntryError;
import endorh.simpleconfig.ui.api.Tooltip;
import endorh.simpleconfig.ui.hotkey.HotKeyActionType;
import endorh.simpleconfig.ui.hotkey.HotKeyActionTypes;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public class EnumListEntry<E extends Enum<?>> extends SelectionListEntry<E> {
	public static final Function<Enum<?>, ITextComponent> DEFAULT_NAME_PROVIDER =
	  t -> new TranslationTextComponent(
		 t instanceof SelectionListEntry.Translatable ? ((Translatable) t).getKey() : t.toString());
	protected @Nullable Function<E, List<ITextComponent>> enumTooltipProvider;
	protected @Nullable Set<E> allowedValues;
	
	@Internal public EnumListEntry(
	  ITextComponent fieldName, Class<E> clazz, E value,
	  Function<E, ITextComponent> enumNameProvider,
	  @Nullable Function<E, List<ITextComponent>> enumTooltipProvider,
	  @Nullable Set<E> allowedValues
	) {
		super(fieldName, Arrays.stream(clazz.getEnumConstants())
		        .filter(e -> allowedValues == null || allowedValues.contains(e))
		        .collect(Collectors.toList()), value, enumNameProvider);
		this.enumTooltipProvider = enumTooltipProvider;
		this.allowedValues = allowedValues;
		hotKeyActionTypes.add(HotKeyActionTypes.ENUM_ADD.cast());
	}
	
	@Override protected void renderField(
	  MatrixStack mStack, int fieldX, int fieldY, int fieldWidth, int fieldHeight, int x, int y,
	  int entryWidth, int entryHeight, int index, int mouseX, int mouseY, float delta
	) {
		super.renderField(
		  mStack, fieldX, fieldY, fieldWidth, fieldHeight, x, y,
		  entryWidth, entryHeight, index, mouseX, mouseY, delta);
		if (enumTooltipProvider != null && !getEntryList().isDragging()
		    && fieldArea.contains(mouseX, mouseY)) {
			List<ITextComponent> tooltip = enumTooltipProvider.apply(getDisplayedValue());
			IReorderingProcessor[] tt = postProcessTooltip(tooltip.toArray(new ITextComponent[0]));
			if (!tooltip.isEmpty()) addTooltip(Tooltip.of(
			  fieldArea, Point.of(mouseX, mouseY), tt));
		}
	}
	
	@Override public List<EntryError> getHotKeyActionErrors(HotKeyActionType<E, ?> type) {
		List<EntryError> errors = super.getHotKeyActionErrors(type);
		if (type == HotKeyActionTypes.ENUM_ADD) errors.addAll(intEntry.getErrors());
		errors.addAll(intEntry.getEntryErrors());
		return errors;
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
