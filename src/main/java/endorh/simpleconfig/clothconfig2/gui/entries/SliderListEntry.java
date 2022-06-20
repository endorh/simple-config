package endorh.simpleconfig.clothconfig2.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.clothconfig2.api.IChildListEntry;
import endorh.simpleconfig.clothconfig2.api.IExpandable;
import endorh.simpleconfig.clothconfig2.gui.AbstractConfigScreen;
import endorh.simpleconfig.clothconfig2.gui.WidgetUtils;
import endorh.simpleconfig.clothconfig2.gui.widget.DynamicEntryListWidget;
import endorh.simpleconfig.clothconfig2.gui.widget.ResetButton;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AbstractSlider;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static net.minecraft.util.math.MathHelper.clamp;

@OnlyIn(Dist.CLIENT)
public abstract class SliderListEntry<V extends Comparable<V>>
  extends TooltipListEntry<V> implements IChildListEntry {
	protected AtomicReference<V> sliderValue;
	protected SliderWidget sliderWidget;
	protected ResetButton resetButton;
	protected boolean canUseTextField = true;
	protected boolean showText = false;
	protected TextFieldListEntry<V> textFieldEntry = null;
	protected V min;
	protected V max;
	
	protected Function<V, ITextComponent> textGetter;
	protected List<IGuiEventListener> widgets;
	protected List<IGuiEventListener> textWidgets;
	protected List<IGuiEventListener> childWidgets;
	protected List<IGuiEventListener> textChildWidgets;
	protected boolean child;
	
	public SliderListEntry(
	  ITextComponent fieldName, V min, V max, V value,
	  Function<V, ITextComponent> textGetter
	) {
		super(fieldName);
		this.min = min;
		this.max = max;
		this.sliderValue = new AtomicReference<>(this.original = value);
		this.textGetter = textGetter;
		this.resetButton = new ResetButton(this);
		this.widgets = Lists.newArrayList(resetButton);
		this.textWidgets = Lists.newArrayList(resetButton);
		this.childWidgets = Lists.newArrayList();
		this.textChildWidgets = Lists.newArrayList();
	}
	
	/**
	 * Subclasses must call this method with the slider widget to be used
	 */
	protected void initWidgets(SliderWidget widget, TextFieldListEntry<V> textFieldEntry) {
		if (sliderWidget != null)
			throw new IllegalStateException();
		sliderWidget = widget;
		sliderWidget.setHeight(20);
		sliderWidget.setValue(getValue());
		sliderWidget.setMessage(textGetter.apply(getValue()));
		widgets.add(0, sliderWidget);
		childWidgets.add(0, sliderWidget);
		this.textFieldEntry = textFieldEntry;
		textFieldEntry.setChild(true);
		textFieldEntry.setParent(getParentOrNull());
		textFieldEntry.setScreen(getConfigScreenOrNull());
		textFieldEntry.setExpandableParent(getExpandableParent());
		textWidgets.add(0, textFieldEntry);
		textChildWidgets.add(0, textFieldEntry);
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
	
	@Override public V getValue() {
		return sliderValue.get();
	}
	
	@Override public void setValue(V value) {
		this.sliderValue.set(value);
		this.sliderWidget.setValue(value);
		this.sliderWidget.updateMessage();
		if (showText && !Objects.equals(textFieldEntry.getValue(), value))
			textFieldEntry.setValue(value);
	}
	
	@Override public Optional<ITextComponent> getErrorMessage() {
		if (isTextFieldShown()) {
			Optional<ITextComponent> error = textFieldEntry.getErrorMessage();
			if (error.isPresent())
				return error;
		}
		return super.getErrorMessage();
	}
	
	@Override public @NotNull List<? extends IGuiEventListener> children() {
		return isTextFieldShown()? isChild()? textChildWidgets : textWidgets
		                         : isChild()? childWidgets : widgets;
	}
	
	@Override public void setExpandableParent(IExpandable parent) {
		super.setExpandableParent(parent);
		if (textFieldEntry != null) textFieldEntry.setExpandableParent(parent);
	}
	
	@Override public void setParent(DynamicEntryListWidget<?> parent) {
		super.setParent(parent);
		if (textFieldEntry != null) textFieldEntry.setParent(parent);
	}
	
	@Override public void setScreen(AbstractConfigScreen screen) {
		super.setScreen(screen);
		if (textFieldEntry != null) textFieldEntry.setScreen(screen);
	}
	
	@Override public void renderEntry(
	  MatrixStack mStack, int index, int y, int x, int entryWidth, int entryHeight,
	  int mouseX, int mouseY, boolean isHovered, float delta
	) {
		super.renderEntry(mStack, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		if (showText && (!canUseTextField || getFocused() != textFieldEntry))
			setTextFieldShown(false, false);
		MainWindow window = Minecraft.getInstance().getWindow();
		final FontRenderer font = Minecraft.getInstance().font;
		resetButton.y = y;
		int sliderX;
		ITextComponent name = getDisplayedFieldName();
		if (font.isBidirectional()) {
			font.drawShadow(
			  mStack, name.getVisualOrderText(),
			  (float) (window.getGuiScaledWidth() - x - font.width(name)),
			  (float) (y + 6), getPreferredTextColor());
			resetButton.x = x;
			sliderX = x + resetButton.getWidth() + 3;
		} else {
			font.drawShadow(
			  mStack, name.getVisualOrderText(), (float)x, (float)(y + 6), getPreferredTextColor());
			resetButton.x = x + entryWidth - resetButton.getWidth();
			sliderX = x + entryWidth - 148;
		}
		
		if (canUseTextField) {
			int v = 0;
			if (isTextFieldShown())
				v += 9;
			if (entryArea.contains(mouseX, mouseY)
			    && !sliderWidget.isMouseOver(mouseX, mouseY)
			    && !resetButton.isMouseOver(mouseX, mouseY))
				v += 18;
			bindTexture();
			blit(mStack, x - 15, y + 5, 111, v, 9, 9);
		}
		
		renderChild(mStack, sliderX, y, 144 - resetButton.getWidth(), 20, mouseX, mouseY, delta);
		resetButton.render(mStack, mouseX, mouseY, delta);
	}
	
	@Override public void renderChildEntry(
	  MatrixStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		if (isTextFieldShown()) {
			textFieldEntry.updateSelected(isSelected && getFocused() == textFieldEntry);
			textFieldEntry.setEditable(isEditable());
			textFieldEntry.renderChild(mStack, x, y, w, h, mouseX, mouseY, delta);
			if (!textFieldEntry.getErrorMessage().isPresent())
				setValue(textFieldEntry.getValue());
		} else {
			sliderWidget.active = isEditable();
			sliderWidget.x = x - 2;
			sliderWidget.y = y;
			sliderWidget.setWidth(w + 4);
			sliderWidget.setHeight(h);
			sliderWidget.render(mStack, mouseX, mouseY, delta);
		}
	}
	
	public boolean isTextFieldShown() {
		return showText;
	}
	
	public void setTextFieldShown(boolean show, boolean focus) {
		if (!canUseTextField)
			show = false;
		showText = show;
		if (focus)
			WidgetUtils.forceUnFocus(resetButton);
		if (show) {
			textFieldEntry.setValue(getValue());
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
	
	@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!showText && sliderWidget.isMouseOver(mouseX, mouseY) && !isSelected) {
			preserveState();
			isSelected = true;
		}
		if (super.mouseClicked(mouseX, mouseY, button))
			return true;
		if (canUseTextField && entryArea.grow(32, 0, 0, 0).contains(mouseX, mouseY)) {
			setTextFieldShown(!isTextFieldShown(), true);
			Minecraft.getInstance().getSoundManager().play(
			  SimpleSound.forUI(canUseTextField? SimpleConfigMod.UI_TAP : SimpleConfigMod.UI_DOUBLE_TAP, 1F));
			return true;
		}
		return false;
	}
	
	@Override public boolean charTyped(char codePoint, int modifiers) {
		final IGuiEventListener listener = getFocused();
		if ((listener == sliderWidget || listener == textFieldEntry)
		    && canUseTextField && codePoint == ' ') {
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
	
	@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		final IGuiEventListener listener = getFocused();
		if ((listener == sliderWidget || listener == textFieldEntry)
		    && canUseTextField && keyCode == 257) { // Enter
			// Space to toggle, Ctrl + Space to use text, Shift + Space to use slider
			boolean state = Screen.hasControlDown() || !Screen.hasShiftDown() && !isTextFieldShown();
			boolean change = state != isTextFieldShown();
			setTextFieldShown(state, true);
			Minecraft.getInstance().getSoundManager().play(
			  SimpleSound.forUI(change? SimpleConfigMod.UI_TAP : SimpleConfigMod.UI_DOUBLE_TAP, 1F));
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}
	
	@Override public Optional<ITextComponent[]> getTooltip(int mouseX, int mouseY) {
		if (resetButton.isMouseOver(mouseX, mouseY))
			return resetButton.getTooltip(mouseX, mouseY);
		if (sliderWidget.isMouseOver(mouseX, mouseY))
			return Optional.empty();
		return super.getTooltip(mouseX, mouseY);
	}
	
	@Override public void updateSelected(boolean isSelected) {
		super.updateSelected(isSelected);
		if (!isSelected) WidgetUtils.forceUnFocus(sliderWidget, resetButton);
	}
	
	@Override public boolean isChild() {
		return child;
	}
	
	@Override public void setChild(boolean child) {
		this.child = child;
	}
	
	public abstract class SliderWidget extends AbstractSlider {
		public SliderWidget(
		  int x, int y, int width, int height
		) {
			super(x, y, width, height, StringTextComponent.EMPTY, 0D);
		}
		
		@Override protected void updateMessage() {
			setMessage(textGetter.apply(SliderListEntry.this.getValue()));
		}
		
		@Override protected void applyValue() {
			sliderValue.set(getValue());
		}
		
		@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
			if (isEditable()) {
				boolean left = keyCode == 263; // Left
				if (left || keyCode == 262) { // Right
					float step = left ? -2.0F : 2.0F;
					if (Screen.hasShiftDown())
						step *= 0.25D;
					if (Screen.hasControlDown())
						step *= 4D;
					final double v = clamp(value + (double) (step / (float) (this.width - 8)), 0D, 1D);
					value = v;
					SliderListEntry.this.setValue(getValue());
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
		public abstract void setValue(V value);
	}
}
