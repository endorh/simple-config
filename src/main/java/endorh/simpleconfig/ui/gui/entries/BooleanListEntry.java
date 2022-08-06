package endorh.simpleconfig.ui.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.gui.WidgetUtils;
import endorh.simpleconfig.ui.hotkey.HotKeyActionTypes;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@OnlyIn(value = Dist.CLIENT)
public class BooleanListEntry extends TooltipListEntry<Boolean> implements IChildListEntry {
	protected boolean displayedValue;
	protected final Button buttonWidget;
	protected final List<IGuiEventListener> widgets;
	protected final List<IGuiEventListener> childWidgets;
	protected @NotNull Function<Boolean, ITextComponent> yesNoSupplier = bool ->
	  new TranslationTextComponent("text.cloth-config.boolean.value." + bool);
	
	@Deprecated
	@ApiStatus.Internal
	public BooleanListEntry(
	  ITextComponent fieldName, boolean value
	) {
		super(fieldName);
		setValue(value);
		setOriginal(value);
		displayedValue = value;
		buttonWidget = new Button(
		  0, 0, 150, 20, NarratorChatListener.EMPTY, widget -> {
			  if (!isFocused()) {
				  preserveState();
				  setFocused(true);
			  }
			  displayedValue = !displayedValue;
		  });
		widgets = Lists.newArrayList(buttonWidget, sideButtonReference);
		childWidgets = Lists.newArrayList(buttonWidget);
		hotKeyActionTypes.add(HotKeyActionTypes.BOOLEAN_TOGGLE);
	}
	
	public void setYesNoSupplier(
	  @NotNull Function<Boolean, ITextComponent> yesNoSupplier
	) {
		this.yesNoSupplier = yesNoSupplier;
	}
	
	@Override public void updateFocused(boolean isFocused) {
		super.updateFocused(isFocused);
		if (!isFocused)
			WidgetUtils.forceUnFocus(buttonWidget);
	}
	
	@Override
	public Boolean getDisplayedValue() {
		return this.displayedValue;
	}
	
	@Override public void setDisplayedValue(Boolean value) {
		displayedValue = value;
	}
	
	@Override public boolean shouldRenderEditable() {
		if (isEditingHotKeyAction())
			return getHotKeyActionType() == HotKeyActionTypes.ASSIGN.<Boolean>cast();
		return super.shouldRenderEditable();
	}
	
	@Override public void renderChildEntry(
	  MatrixStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		buttonWidget.active = shouldRenderEditable();
		buttonWidget.x = x;
		buttonWidget.y = y;
		buttonWidget.setWidth(w);
		buttonWidget.setHeight(h);
		buttonWidget.setMessage(getYesNoText(displayedValue));
		buttonWidget.render(mStack, mouseX, mouseY, delta);
	}
	
	public ITextComponent getYesNoText(boolean bool) {
		return yesNoSupplier.apply(bool);
	}
	
	@Override public Optional<ITextComponent[]> getTooltip(int mouseX, int mouseY) {
		if (buttonWidget.isMouseOver(mouseX, mouseY))
			return Optional.empty();
		return super.getTooltip(mouseX, mouseY);
	}
	
	@Override protected @NotNull List<? extends IGuiEventListener> getEntryListeners() {
		return this.isChildSubEntry() ? childWidgets : this.widgets;
	}
	
	@Override public String seekableValueText() {
		return getUnformattedString(yesNoSupplier.apply(getDisplayedValue()));
	}
}

