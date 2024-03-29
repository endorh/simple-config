package endorh.simpleconfig.ui.gui.entries;

import com.google.common.collect.Lists;
import endorh.simpleconfig.api.ui.TextFormatter;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons;
import endorh.simpleconfig.api.ui.math.Rectangle;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.gui.widget.TextFieldWidgetEx;
import endorh.simpleconfig.ui.gui.widget.ToggleAnimator;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Optional;

@OnlyIn(value = Dist.CLIENT)
public abstract class TextFieldListEntry<V> extends TooltipListEntry<V> implements IChildListEntry {
	protected TextFieldWidgetEx textFieldWidget;
	protected List<GuiEventListener> widgets;
	protected List<GuiEventListener> childWidgets;
	protected boolean expandable;
	private boolean expanded = false;
	protected Rectangle labelArea = new Rectangle();
	protected ToggleAnimator expandAnimator = new ToggleAnimator(250L);
	protected int maxLength;
	protected int minLength;
	private int frame = 0;
	protected TextFormatter textFormatter = TextFormatter.DEFAULT;
	
	@Internal protected TextFieldListEntry(
	  Component fieldName, V original, boolean canExpand
	) {
		super(fieldName);
		expandable = canExpand;
		textFieldWidget = new TextFieldWidgetEx(
		  Minecraft.getInstance().font, 0, 0, 150, 18, GameNarrator.NO_TITLE);
		textFieldWidget.setMaxLength(999999);
		textFieldWidget.setFormatter(TextFormatter.cached(textFormatter));
		setOriginal(original);
		setValue(original);
		setDisplayedValue(original);
		widgets = Lists.newArrayList(textFieldWidget, sideButtonReference);
		childWidgets = Lists.newArrayList(textFieldWidget);
	}
	
	@Override public void setSubEntry(boolean isSubEntry) {
		super.setSubEntry(isSubEntry);
	}
	
	@Override public V getDisplayedValue() {
		return fromString(textFieldWidget.getValue());
	}
	@Override public void setDisplayedValue(V v) {
		textFieldWidget.setValue(toString(v));
	}
	public String getText() {
		return textFieldWidget.getValue();
	}
	
	protected abstract @Nullable V fromString(String s);
	protected String toString(@Nullable V v) {
		return v != null? String.valueOf(v) : "";
	}
	
	@Override public void updateFocused(boolean isFocused) {
		boolean prev = isFocused();
		super.updateFocused(isFocused);
		if (!isFocused && prev) {
			textFieldWidget.setAnchorPos(textFieldWidget.getCaret());
			textFieldWidget.setFocused(false);
		}
	}
	
	@Override protected void acquireFocus() {
		super.acquireFocus();
		textFieldWidget.moveCaretToEnd();
		textFieldWidget.setAnchorPos(0);
	}
	
	@Override public void tick() {
		super.tick();
		if (frame++ % 10 == 0) textFieldWidget.tick();
		textFieldWidget.setBorderColor(hasError()? 0xFF8080 : 0xFFFFFF);
	}
	
	@Override public void renderEntry(
      GuiGraphics gg, int index, int x, int y, int entryWidth, int entryHeight, int mouseX,
      int mouseY, boolean isHovered, float delta
	) {
		super.renderEntry(gg, index, x, y, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		labelArea.setBounds(x - 15, y, entryWidth, 24);
		if (isExpandable()) {
			SimpleConfigIcons.Entries.TEXT_EXPAND.renderCentered(
			  gg, x - 15, y + 5, 9, 9,
			  (isExpanded()? 1 : 0) + (
			    isMouseOverLabel(mouseX, mouseY) && !textFieldWidget.isMouseOver(mouseX, mouseY)
			    && !sideButtonReference.isMouseOver(mouseX, mouseY)? 2 : 0));
		}
	}
	
	@Override protected boolean isFieldFullWidth() {
		return isExpanded();
	}
	
	@Override protected void renderField(
      GuiGraphics gg, int fieldX, int fieldY, int fieldWidth, int fieldHeight, int x, int y,
      int entryWidth, int entryHeight, int index, int mouseX, int mouseY, float delta
	) {
		float p = expandAnimator.getEaseOut();
		int expandedX = Minecraft.getInstance().font.isBidirectional()? fieldX : x + 14;
		renderChild(
		  gg, (int) Mth.lerp(p, fieldX, expandedX),
		  isHeadless()? fieldY : (int) Mth.lerp(p, fieldY, y + 24),
		  (int) Mth.lerp(p, fieldWidth,
		             isHeadless()? entryWidth - 16 - resetButton.getWidth() : entryWidth - 14),
		  fieldHeight, mouseX, mouseY, delta);
	}
	
	@Override public void renderChildEntry(
      GuiGraphics gg, int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		textFieldWidget.setEditable(shouldRenderEditable());
		// Text fields render the border outside, so we inset it 1px to match other controls
		textFieldWidget.setX(x + 1);
		textFieldWidget.setY(y + 1);
		textFieldWidget.setWidth(w - 2);
		textFieldWidget.setHeight(h - 2);
		textFieldWidget.render(gg, mouseX, mouseY, delta);
	}
	
	protected boolean isMouseOverLabel(double mouseX, double mouseY) {
		return labelArea.contains(mouseX, mouseY);
	}
	
	@Override public int getItemHeight() {
		return 24 + (
		  isHeadless()
		  ? 0 : isSubEntry()
		        ? expanded? 24 : 0
		        : (int) (expandAnimator.getEaseOut() * 24));
	}
	
	@Override public Optional<Component[]> getTooltip(int mouseX, int mouseY) {
		if (textFieldWidget.isMouseOver(mouseX, mouseY))
			return Optional.empty();
		return super.getTooltip(mouseX, mouseY);
	}
	
	public void setExpanded(boolean expanded) {
		if (!isExpandable()) expanded = false;
		if (this.expanded != expanded) {
			expandAnimator.setEaseOutTarget(expanded);
			this.expanded = expanded;
			if (isEditable()) setFocused(textFieldWidget);
		}
	}
	
	public boolean isExpanded() {
		return expanded;
	}
	public boolean isExpandable() {
		return expandable && !isChildSubEntry();
	}
	
	@Override public boolean onMouseClicked(double mouseX, double mouseY, int button) {
		if (super.onMouseClicked(mouseX, mouseY, button))
			return true;
		if (isExpandable() && button == 0 && isMouseOverLabel(mouseX, mouseY)) {
			setExpanded(!isExpanded());
         getFocused().setFocused(false);
         textFieldWidget.setFocused(true);
			setFocused(textFieldWidget);
			playFeedbackTap(1F);
			return true;
		}
		return false;
	}
	
	@Override public Optional<Component> getErrorMessage() {
		final Optional<Component> opt = super.getErrorMessage();
		if (opt.isPresent()) return opt;
		if (getText().length() < minLength)
			return Optional.of(
			  minLength == 1
			  ? Component.translatable("simpleconfig.config.error.string.empty")
			  : Component.translatable("simpleconfig.config.error.string.min_length", minLength));
		return Optional.empty();
	}
	
	@Override protected @NotNull List<? extends GuiEventListener> getEntryListeners() {
		return isChildSubEntry() ? childWidgets : widgets;
	}
	
	public void setMaxLength(int maxLength) {
		this.maxLength = maxLength;
		textFieldWidget.setMaxLength(maxLength);
	}
	
	public void setMinLength(int minLength) {
		this.minLength = minLength;
	}
	
	public void setTextFormatter(TextFormatter textFormatter) {
		this.textFormatter = textFormatter;
		textFieldWidget.setFormatter(TextFormatter.cached(textFormatter));
	}
	
	public TextFormatter getTextFormatter() {
		return textFormatter;
	}
	
	@Override public boolean handleNavigationKey(int keyCode, int scanCode, int modifiers) {
		if (isExpandable() && Screen.hasAltDown()) {
			if (keyCode == GLFW.GLFW_KEY_RIGHT && !isExpanded()) {
				setExpanded(true);
				playFeedbackTap(1F);
				return true;
			} else if (keyCode == GLFW.GLFW_KEY_LEFT && isExpanded()) {
				setExpanded(false);
				playFeedbackTap(1F);
				return true;
			}
		}
		return super.handleNavigationKey(keyCode, scanCode, modifiers);
	}
	
	@Override public String seekableValueText() {
		return getText();
	}
}