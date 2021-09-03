package endorh.simple_config.clothconfig2.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simple_config.clothconfig2.api.IChildListEntry;
import endorh.simple_config.clothconfig2.api.IExpandable;
import endorh.simple_config.clothconfig2.gui.AbstractConfigScreen;
import endorh.simple_config.clothconfig2.gui.widget.DynamicEntryListWidget;
import endorh.simple_config.clothconfig2.gui.widget.ResetButton;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AbstractSlider;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public abstract class SliderListEntry<V extends Comparable<V>>
  extends TooltipListEntry<V> implements IChildListEntry {
	protected AtomicReference<V> value;
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
		this.value = new AtomicReference<>(this.original = value);
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
	}
	
	@Override public V getValue() {
		return value.get();
	}
	
	@Override public void setValue(V value) {
		this.value.set(value);
		this.sliderWidget.setValue(value);
		this.sliderWidget.func_230979_b_();
		if (showText && !Objects.equals(textFieldEntry.getValue(), value))
			textFieldEntry.setValue(value);
	}
	
	@Override public Optional<ITextComponent> getError() {
		if (isTextFieldShown()) {
			Optional<ITextComponent> error = textFieldEntry.getError();
			if (error.isPresent())
				return error;
		}
		return super.getError();
	}
	
	@Override public @NotNull List<? extends IGuiEventListener> getEventListeners() {
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
		if (showText && (!canUseTextField || getListener() != textFieldEntry))
			setTextFieldShown(false, false);
		MainWindow window = Minecraft.getInstance().getMainWindow();
		final FontRenderer font = Minecraft.getInstance().fontRenderer;
		resetButton.y = y;
		int sliderX;
		ITextComponent name = getDisplayedFieldName();
		if (font.getBidiFlag()) {
			font.func_238407_a_(
			  mStack, name.func_241878_f(),
			  (float) (window.getScaledWidth() - x - font.getStringPropertyWidth(name)),
			  (float) (y + 6), getPreferredTextColor());
			resetButton.x = x;
			sliderX = x + resetButton.getWidth() + 3;
		} else {
			font.func_238407_a_(
			  mStack, name.func_241878_f(), (float)x, (float)(y + 6), getPreferredTextColor());
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
			textFieldEntry.updateSelected(isSelected && getListener() == textFieldEntry);
			textFieldEntry.setEditable(isEditable());
			textFieldEntry.renderChild(mStack, x, y, w, h, mouseX, mouseY, delta);
			if (!textFieldEntry.getError().isPresent())
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
			forceUnFocus(resetButton);
		if (show) {
			textFieldEntry.setValue(getValue());
			if (focus) {
				setListener(textFieldEntry);
				forceUnFocus(sliderWidget);
				final TextFieldWidget textFieldWidget = textFieldEntry.textFieldWidget;
				textFieldEntry.setListener(textFieldWidget);
				forceFocus(textFieldWidget);
				textFieldWidget.setCursorPosition(textFieldWidget.getText().length());
				textFieldWidget.setSelectionPos(0);
			}
		} else if (focus) {
			setListener(sliderWidget);
			forceFocus(sliderWidget);
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
			return true;
		}
		return false;
	}
	
	@Override public boolean charTyped(char codePoint, int modifiers) {
		final IGuiEventListener listener = getListener();
		if ((listener == sliderWidget || listener == textFieldEntry)
		    && canUseTextField && codePoint == ' ') {
			// Space to toggle, Ctrl + Space to use text, Shift + Space to use slider
			boolean state = Screen.hasControlDown() || !Screen.hasShiftDown() && !isTextFieldShown();
			setTextFieldShown(state, true);
			return true;
		}
		return super.charTyped(codePoint, modifiers);
	}
	
	@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		final IGuiEventListener listener = getListener();
		if ((listener == sliderWidget || listener == textFieldEntry)
		    && canUseTextField && keyCode == 257) {
			// Space to toggle, Ctrl + Space to use text, Shift + Space to use slider
			boolean state = Screen.hasControlDown() || !Screen.hasShiftDown() && !isTextFieldShown();
			setTextFieldShown(state, true);
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
		if (!isSelected)
			forceUnFocus(sliderWidget, resetButton);
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
		
		@Override protected void func_230979_b_() {
			setMessage(textGetter.apply(SliderListEntry.this.getValue()));
		}
		
		@Override protected void func_230972_a_() {
			value.set(getValue());
		}
		
		@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
			if (isEditable()) {
				boolean left = keyCode == 263;
				if (left || keyCode == 262) {
					float step = left ? -2.0F : 2.0F;
					if (Screen.hasShiftDown())
						step *= 0.25D;
					if (Screen.hasControlDown())
						step *= 4D;
					final double value = MathHelper.clamp(
					  sliderValue + (double) (step / (float) (this.width - 8)), 0D, 1D);
					sliderValue = value;
					SliderListEntry.this.setValue(getValue());
					sliderValue = value;
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
