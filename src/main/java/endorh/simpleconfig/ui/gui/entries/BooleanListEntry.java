package endorh.simpleconfig.ui.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.gui.WidgetUtils;
import endorh.simpleconfig.ui.hotkey.HotKeyActionTypes;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
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
	protected final List<GuiEventListener> widgets;
	protected final List<GuiEventListener> childWidgets;
	protected @NotNull Function<Boolean, Component> yesNoSupplier = bool ->
	  Component.translatable("simpleconfig.format.bool.yes_no." + bool);
	
	@Deprecated
	@ApiStatus.Internal
	public BooleanListEntry(
	  Component fieldName, boolean value
	) {
		super(fieldName);
		setValue(value);
		setOriginal(value);
		displayedValue = value;
		buttonWidget = new Button(
		  0, 0, 150, 20, GameNarrator.NO_TITLE, widget -> {
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
	  @NotNull Function<Boolean, Component> yesNoSupplier
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
	  PoseStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		buttonWidget.active = shouldRenderEditable();
		buttonWidget.x = x;
		buttonWidget.y = y;
		buttonWidget.setWidth(w);
		buttonWidget.setHeight(h);
		buttonWidget.setMessage(getYesNoText(displayedValue));
		buttonWidget.render(mStack, mouseX, mouseY, delta);
	}
	
	public Component getYesNoText(boolean bool) {
		return yesNoSupplier.apply(bool);
	}
	
	@Override public Optional<Component[]> getTooltip(int mouseX, int mouseY) {
		if (buttonWidget.isMouseOver(mouseX, mouseY))
			return Optional.empty();
		return super.getTooltip(mouseX, mouseY);
	}
	
	@Override protected @NotNull List<? extends GuiEventListener> getEntryListeners() {
		return this.isChildSubEntry() ? childWidgets : this.widgets;
	}
	
	@Override public String seekableValueText() {
		return getUnformattedString(yesNoSupplier.apply(getDisplayedValue()));
	}
}

