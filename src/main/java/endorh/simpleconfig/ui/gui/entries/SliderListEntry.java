package endorh.simpleconfig.ui.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.gui.WidgetUtils;
import endorh.simpleconfig.ui.gui.widget.TextFieldWidgetEx;
import endorh.simpleconfig.ui.hotkey.HotKeyActionType;
import endorh.simpleconfig.ui.hotkey.HotKeyActionTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
public abstract class SliderListEntry<V extends Comparable<V>>
  extends TooltipListEntry<V> implements IChildListEntry {
	protected V sliderValue;
	protected SliderWidget sliderWidget;
	protected boolean showText = false;
	protected TextFieldListEntry<V> textFieldEntry = null;
	protected V min;
	protected V max;
	
	protected Function<V, Component> textGetter;
	protected List<GuiEventListener> widgets;
	protected List<GuiEventListener> textWidgets;
	protected List<GuiEventListener> childWidgets;
	protected List<GuiEventListener> textChildWidgets;
	
	public SliderListEntry(
	  Component fieldName, V min, V max, V value,
	  Function<V, Component> textGetter
	) {
		super(fieldName);
		this.min = min;
		this.max = max;
		setOriginal(value);
		setValue(value);
		sliderValue = value;
		this.textGetter = textGetter;
		widgets = Lists.newArrayList(sideButtonReference);
		textWidgets = Lists.newArrayList(sideButtonReference);
		childWidgets = Lists.newArrayList();
		textChildWidgets = Lists.newArrayList();
	}
	
	/**
	 * Subclasses must call this method with the slider widget to be used
	 */
	protected void initWidgets(
	  SliderWidget widget, @Nullable TextFieldListEntry<V> textFieldEntry
	) {
		if (sliderWidget != null)
			throw new IllegalStateException();
		sliderWidget = widget;
		sliderWidget.setHeight(20);
		sliderWidget.setValue(getValue());
		sliderWidget.setMessage(textGetter.apply(getDisplayedValue()));
		widgets.add(0, sliderWidget);
		childWidgets.add(0, sliderWidget);
		this.textFieldEntry = textFieldEntry;
		if (textFieldEntry != null) {
			textFieldEntry.setChildSubEntry(true);
			textFieldEntry.setParentEntry(this);
		}
		textWidgets.add(0, textFieldEntry);
		textChildWidgets.add(0, textFieldEntry);
	}
	
	public boolean canUseTextField() {
		return isEditable();
	}
	
	@Override public void setHotKeyActionType(HotKeyActionType<V, ?> type) {
		super.setHotKeyActionType(type);
		if (type != null && type != HotKeyActionTypes.ASSIGN.<V>cast())
			setTextFieldShown(true, true);
	}
	
	protected boolean isTextFieldEnforced() {
		if (!isEditingHotKeyAction()) return false;
		HotKeyActionType<?, ?> type = getHotKeyActionType();
		return type != null && type != HotKeyActionTypes.ASSIGN;
	}
	
	public V getMin() {
		return min;
	}
	
	public void setMin(V min) {
		this.min = min;
	}
	
	public V getMax() {
		return max;
	}
	
	public void setMax(V max) {
		this.max = max;
	}
	
	public Function<V, Component> getTextGetter() {
		return textGetter;
	}
	
	public void setTextGetter(
	  Function<V, Component> textGetter
	) {
		this.textGetter = textGetter;
		sliderWidget.setMessage(this.textGetter.apply(getValue()));
	}
	
	@Override public V getDisplayedValue() {
		return sliderValue;
	}
	
	@Override public void setDisplayedValue(V value) {
		this.sliderValue = value;
		this.sliderWidget.setValue(value);
		this.sliderWidget.updateMessage();
		if (showText && !areEqual(textFieldEntry.getDisplayedValue(), value))
			textFieldEntry.setDisplayedValue(value);
	}
	
	@Override public Optional<Component> getErrorMessage() {
		if (isTextFieldShown()) {
			Optional<Component> error = textFieldEntry.getErrorMessage();
			if (error.isPresent())
				return error;
		}
		return super.getErrorMessage();
	}
	
	@Override protected @NotNull List<? extends GuiEventListener> getEntryListeners() {
		return isTextFieldShown()? this.isChildSubEntry() ? textChildWidgets : textWidgets
		                         : this.isChildSubEntry() ? childWidgets : widgets;
	}
	
	@Override public boolean handleNavigationKey(int keyCode, int scanCode, int modifiers) {
		switch (keyCode) {
			case 262: // Right
				if (!isTextFieldShown() && !this.isChildSubEntry()) {
					setTextFieldShown(true, true);
					return true;
				}
				break;
			case 263: // Left
				if (isTextFieldShown()) {
					setTextFieldShown(false, true);
					return true;
				}
				break;
		}
		return super.handleNavigationKey(keyCode, scanCode, modifiers);
	}
	
	@Override public void renderEntry(
	  PoseStack mStack, int index, int x, int y, int entryWidth, int entryHeight,
	  int mouseX, int mouseY, boolean isHovered, float delta
	) {
		super.renderEntry(mStack, index, x, y, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		if (showText && (!canUseTextField() || getFocused() != textFieldEntry)) {
			setTextFieldShown(false, false);
		}
		
		if (canUseTextField() && !isTextFieldEnforced()) {
			SimpleConfigIcons.Entries.SLIDER_EDIT.renderCentered(
			  mStack, x - 15, y + 5, 9, 9,
			  (isTextFieldShown()? 1 : 0) + (
				 entryArea.contains(mouseX, mouseY)
			    && !sliderWidget.isMouseOver(mouseX, mouseY)
			    && !sideButtonReference.isMouseOver(mouseX, mouseY)? 2 : 0));
		}
	}
	
	@Override public void tick() {
		super.tick();
		if (isTextFieldShown()) textFieldEntry.tick();
	}
	
	@Override public void renderChildEntry(
	  PoseStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		if (isTextFieldShown()) {
			textFieldEntry.updateFocused(isFocused() && getFocused() == textFieldEntry);
			textFieldEntry.setEditable(shouldRenderEditable());
			textFieldEntry.renderChild(mStack, x, y, w, h, mouseX, mouseY, delta);
			if (!textFieldEntry.getErrorMessage().isPresent() || isTextFieldEnforced()) {
				V value = textFieldEntry.getDisplayedValue();
				if (value != null) setDisplayedValue(value);
			}
		} else {
			sliderWidget.active = shouldRenderEditable();
			sliderWidget.x = x;
			sliderWidget.y = y;
			sliderWidget.setWidth(w);
			sliderWidget.setHeight(h);
			sliderWidget.render(mStack, mouseX, mouseY, delta);
		}
	}
	
	public boolean isTextFieldShown() {
		return showText;
	}
	
	public void setTextFieldShown(boolean show, boolean focus) {
		if (!canUseTextField()) show = false;
		if (isTextFieldEnforced()) show = true;
		showText = show;
		if (focus)
			WidgetUtils.forceUnFocus(sideButtonReference);
		if (show) {
			textFieldEntry.setDisplayedValue(getDisplayedValue());
			if (focus) {
				setFocused(textFieldEntry);
				WidgetUtils.forceUnFocus(sliderWidget);
				final TextFieldWidgetEx textFieldWidget = textFieldEntry.textFieldWidget;
				textFieldEntry.setFocused(textFieldWidget);
				WidgetUtils.forceFocus(textFieldWidget);
				textFieldWidget.moveCaretToEnd();
				textFieldWidget.setAnchorPos(0);
			}
		} else if (focus) {
			setFocused(sliderWidget);
			WidgetUtils.forceFocus(sliderWidget);
		}
	}
	
	@Override public boolean onMouseClicked(double mouseX, double mouseY, int button) {
		if (!showText && sliderWidget.isMouseOver(mouseX, mouseY) && !isFocused()) {
			preserveState();
			setFocused(true);
		}
		if (super.onMouseClicked(mouseX, mouseY, button))
			return true;
		if (button == 0 && !Screen.hasAltDown() && canUseTextField()
		    && entryArea.grow(32, 0, 0, 0).contains(mouseX, mouseY)) {
			setTextFieldShown(!isTextFieldShown(), true);
			Minecraft.getInstance().getSoundManager().play(
			  SimpleSoundInstance.forUI(canUseTextField()? SimpleConfigMod.UI_TAP : SimpleConfigMod.UI_DOUBLE_TAP, 1F));
			return true;
		}
		return false;
	}
	
	@Override public boolean charTyped(char codePoint, int modifiers) {
		final GuiEventListener listener = getFocused();
		if ((listener == sliderWidget || listener == textFieldEntry)
		    && canUseTextField() && codePoint == ' ') {
			// Space to toggle, Ctrl + Space to use text, Shift + Space to use slider
			boolean state = Screen.hasControlDown() || !Screen.hasShiftDown() && !isTextFieldShown();
			boolean change = state != isTextFieldShown();
			setTextFieldShown(state, true);
			Minecraft.getInstance().getSoundManager().play(
			  SimpleSoundInstance.forUI(change? SimpleConfigMod.UI_TAP : SimpleConfigMod.UI_DOUBLE_TAP, 1F));
			return true;
		}
		return super.charTyped(codePoint, modifiers);
	}
	
	@Override public boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
		final GuiEventListener listener = getFocused();
		if (Screen.hasAltDown()) return true; // Prevent navigation key from triggering slider
		if ((listener == sliderWidget || listener == textFieldEntry)
		    && canUseTextField() && keyCode == GLFW.GLFW_KEY_ENTER) {
			// Space to toggle, Ctrl + Space to use text, Shift + Space to use slider
			boolean state = Screen.hasControlDown() || !Screen.hasShiftDown() && !isTextFieldShown();
			boolean change = state != isTextFieldShown();
			setTextFieldShown(state, true);
			Minecraft.getInstance().getSoundManager().play(
			  SimpleSoundInstance.forUI(change? SimpleConfigMod.UI_TAP : SimpleConfigMod.UI_DOUBLE_TAP, 1F));
			return true;
		}
		return super.onKeyPressed(keyCode, scanCode, modifiers);
	}
	
	@Override public Optional<Component[]> getTooltip(int mouseX, int mouseY) {
		if (sliderWidget.isMouseOver(mouseX, mouseY))
			return Optional.empty();
		return super.getTooltip(mouseX, mouseY);
	}
	
	@Override public void updateFocused(boolean isFocused) {
		super.updateFocused(isFocused);
		if (!isFocused) WidgetUtils.forceUnFocus(sliderWidget);
	}
	
	public abstract class SliderWidget extends AbstractSliderButton {
		public SliderWidget(
		  int x, int y, int width, int height
		) {
			super(x, y, width, height, TextComponent.EMPTY, 0D);
		}
		
		@Override protected void updateMessage() {
			setMessage(textGetter.apply(SliderListEntry.this.getDisplayedValue()));
		}
		
		@Override protected void applyValue() {
			SliderListEntry.this.sliderValue = getValue();
		}
		
		public double getStep(boolean left) {
			float step = left ? -2.0F : 2.0F;
			if (Screen.hasShiftDown())
				step *= 0.25D;
			if (Screen.hasControlDown())
				step *= 4D;
			return step / (float) (this.width - 8);
		}
		
		@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
			if (isEditable()) {
				boolean left = keyCode == GLFW.GLFW_KEY_LEFT;
				if (left || keyCode == GLFW.GLFW_KEY_RIGHT) {
					final double step = getStep(left);
					final double v = Mth.clamp(value + step, 0D, 1D);
					value = v;
					SliderListEntry.this.setDisplayedValue(getValue());
					value = v;
					return true;
				}
			}
			return false;
		}
		
		@Override public boolean mouseDragged(
		  double mouseX, double mouseY, int button, double dragX, double dragY
		) {
			return isEditable() && super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
		}
		
		public abstract V getValue();
		public abstract void setValue(V v);
	}
}
