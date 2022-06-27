package endorh.simpleconfig.clothconfig2.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.clothconfig2.api.IChildListEntry;
import endorh.simpleconfig.clothconfig2.gui.SimpleConfigIcons;
import endorh.simpleconfig.clothconfig2.gui.WidgetUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AbstractSlider;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static net.minecraft.util.math.MathHelper.clamp;

@OnlyIn(Dist.CLIENT)
public abstract class SliderListEntry<V extends Comparable<V>>
  extends TooltipListEntry<V> implements IChildListEntry {
	protected V sliderValue;
	protected SliderWidget sliderWidget;
	private boolean canUseTextField = true;
	protected boolean showText = false;
	protected TextFieldListEntry<V> textFieldEntry = null;
	protected V min;
	protected V max;
	
	protected Function<V, ITextComponent> textGetter;
	protected List<IGuiEventListener> widgets;
	protected List<IGuiEventListener> textWidgets;
	protected List<IGuiEventListener> childWidgets;
	protected List<IGuiEventListener> textChildWidgets;
	
	public SliderListEntry(
	  ITextComponent fieldName, V min, V max, V value,
	  Function<V, ITextComponent> textGetter
	) {
		super(fieldName);
		this.min = min;
		this.max = max;
		setOriginal(value);
		setValue(value);
		sliderValue = value;
		this.textGetter = textGetter;
		widgets = Lists.newArrayList(resetButton);
		textWidgets = Lists.newArrayList(resetButton);
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
	
	public boolean getCanUseTextField() {
		return canUseTextField && isEditable();
	}
	
	public void setCanUseTextField(boolean canUseTextField) {
		this.canUseTextField = canUseTextField;
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
	
	public Function<V, ITextComponent> getTextGetter() {
		return textGetter;
	}
	
	public void setTextGetter(
	  Function<V, ITextComponent> textGetter
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
		if (showText && !areEqual(textFieldEntry.getValue(), value))
			textFieldEntry.setDisplayedValue(value);
	}
	
	@Override public Optional<ITextComponent> getErrorMessage() {
		if (isTextFieldShown()) {
			Optional<ITextComponent> error = textFieldEntry.getErrorMessage();
			if (error.isPresent())
				return error;
		}
		return super.getErrorMessage();
	}
	
	@Override protected @NotNull List<? extends IGuiEventListener> getEntryListeners() {
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
				if (Screen.hasAltDown()) return true; // Prevent navigation key from triggering slider
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
	  MatrixStack mStack, int index, int x, int y, int entryWidth, int entryHeight,
	  int mouseX, int mouseY, boolean isHovered, float delta
	) {
		super.renderEntry(mStack, index, x, y, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		if (showText && (!getCanUseTextField() || getFocused() != textFieldEntry))
			setTextFieldShown(false, false);
		
		if (getCanUseTextField()) {
			SimpleConfigIcons.Entries.SLIDER_EDIT.renderCentered(
			  mStack, x - 15, y + 5, 9, 9,
			  (isTextFieldShown()? 1 : 0) + (
				 entryArea.contains(mouseX, mouseY)
			    && !sliderWidget.isMouseOver(mouseX, mouseY)
			    && !resetButton.isMouseOver(mouseX, mouseY)? 2 : 0));
		}
	}
	
	@Override public void renderChildEntry(
	  MatrixStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		if (isTextFieldShown()) {
			textFieldEntry.updateFocused(isFocused() && getFocused() == textFieldEntry);
			textFieldEntry.setEditable(shouldRenderEditable());
			textFieldEntry.renderChild(mStack, x, y, w, h, mouseX, mouseY, delta);
			if (!textFieldEntry.getErrorMessage().isPresent())
				setDisplayedValue(textFieldEntry.getValue());
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
		if (!getCanUseTextField())
			show = false;
		showText = show;
		if (focus)
			WidgetUtils.forceUnFocus(resetButton);
		if (show) {
			textFieldEntry.setDisplayedValue(getDisplayedValue());
			if (focus) {
				setFocused(textFieldEntry);
				WidgetUtils.forceUnFocus(sliderWidget);
				final TextFieldWidget textFieldWidget = textFieldEntry.textFieldWidget;
				textFieldEntry.setFocused(textFieldWidget);
				WidgetUtils.forceFocus(textFieldWidget);
				textFieldWidget.moveCursorTo(textFieldWidget.getValue().length());
				textFieldWidget.setHighlightPos(0);
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
		if (getCanUseTextField() && entryArea.grow(32, 0, 0, 0).contains(mouseX, mouseY)) {
			setTextFieldShown(!isTextFieldShown(), true);
			Minecraft.getInstance().getSoundManager().play(
			  SimpleSound.forUI(getCanUseTextField()? SimpleConfigMod.UI_TAP : SimpleConfigMod.UI_DOUBLE_TAP, 1F));
			return true;
		}
		return false;
	}
	
	@Override public boolean charTyped(char codePoint, int modifiers) {
		final IGuiEventListener listener = getFocused();
		if ((listener == sliderWidget || listener == textFieldEntry)
		    && getCanUseTextField() && codePoint == ' ') {
			// Space to toggle, Ctrl + Space to use text, Shift + Space to use slider
			boolean state = Screen.hasControlDown() || !Screen.hasShiftDown() && !isTextFieldShown();
			boolean change = state != isTextFieldShown();
			setTextFieldShown(state, true);
			Minecraft.getInstance().getSoundManager().play(
			  SimpleSound.forUI(change? SimpleConfigMod.UI_TAP : SimpleConfigMod.UI_DOUBLE_TAP, 1F));
			return true;
		}
		return super.charTyped(codePoint, modifiers);
	}
	
	@Override public boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
		final IGuiEventListener listener = getFocused();
		if ((listener == sliderWidget || listener == textFieldEntry)
		    && getCanUseTextField() && keyCode == 257) { // Enter
			// Space to toggle, Ctrl + Space to use text, Shift + Space to use slider
			boolean state = Screen.hasControlDown() || !Screen.hasShiftDown() && !isTextFieldShown();
			boolean change = state != isTextFieldShown();
			setTextFieldShown(state, true);
			Minecraft.getInstance().getSoundManager().play(
			  SimpleSound.forUI(change? SimpleConfigMod.UI_TAP : SimpleConfigMod.UI_DOUBLE_TAP, 1F));
			return true;
		}
		return super.onKeyPressed(keyCode, scanCode, modifiers);
	}
	
	@Override public Optional<ITextComponent[]> getTooltip(int mouseX, int mouseY) {
		if (sliderWidget.isMouseOver(mouseX, mouseY))
			return Optional.empty();
		return super.getTooltip(mouseX, mouseY);
	}
	
	@Override public void updateFocused(boolean isFocused) {
		super.updateFocused(isFocused);
		if (!isFocused) WidgetUtils.forceUnFocus(sliderWidget);
	}
	
	public abstract class SliderWidget extends AbstractSlider {
		public SliderWidget(
		  int x, int y, int width, int height
		) {
			super(x, y, width, height, StringTextComponent.EMPTY, 0D);
		}
		
		@Override protected void updateMessage() {
			setMessage(textGetter.apply(SliderListEntry.this.getDisplayedValue()));
		}
		
		@Override protected void applyValue() {
			sliderValue = getValue();
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
				boolean left = keyCode == 263; // Left
				if (left || keyCode == 262) { // Right
					final double step = getStep(left);
					final double v = clamp(value + step, 0D, 1D);
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
