package endorh.simpleconfig.ui.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.api.ITextFormatter;
import endorh.simpleconfig.ui.gui.WidgetUtils;
import endorh.simpleconfig.ui.gui.widget.TextFieldWidgetEx;
import endorh.simpleconfig.ui.gui.widget.ToggleAnimator;
import endorh.simpleconfig.ui.icon.SimpleConfigIcons;
import endorh.simpleconfig.ui.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
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
	protected List<IGuiEventListener> widgets;
	protected List<IGuiEventListener> childWidgets;
	protected boolean expandable;
	private boolean expanded = false;
	protected Rectangle labelArea = new Rectangle();
	protected ToggleAnimator expandAnimator = new ToggleAnimator(250L);
	protected int maxLength;
	protected int minLength;
	private int frame = 0;
	protected ITextFormatter textFormatter = ITextFormatter.DEFAULT;
	
	@Internal protected TextFieldListEntry(
	  ITextComponent fieldName, V original, boolean canExpand
	) {
		super(fieldName);
		expandable = canExpand;
		textFieldWidget = new TextFieldWidgetEx(
		  Minecraft.getInstance().fontRenderer, 0, 0, 150, 18, NarratorChatListener.EMPTY);
		textFieldWidget.setMaxLength(999999);
		textFieldWidget.setFormatter(ITextFormatter.cached(textFormatter));
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
		return fromString(textFieldWidget.getText());
	}
	@Override public void setDisplayedValue(V v) {
		textFieldWidget.setText(toString(v));
	}
	public String getText() {
		return textFieldWidget.getText();
	}
	
	protected abstract @Nullable V fromString(String s);
	protected String toString(@Nullable V v) {
		return v != null? String.valueOf(v) : "";
	}
	
	@Override public void updateFocused(boolean isFocused) {
		super.updateFocused(isFocused);
		if (!isFocused)
			WidgetUtils.forceUnFocus(textFieldWidget);
	}
	
	@Override public void tick() {
		super.tick();
		if ((frame++) % 10 == 0) textFieldWidget.tick();
		textFieldWidget.setBorderColor(hasError()? 0xFF8080 : 0xFFFFFF);
	}
	
	@Override public void renderEntry(
	  MatrixStack mStack, int index, int x, int y, int entryWidth, int entryHeight, int mouseX,
	  int mouseY, boolean isHovered, float delta
	) {
		super.renderEntry(mStack, index, x, y, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		labelArea.setBounds(x - 15, y, entryWidth, 24);
		if (isExpandable()) {
			SimpleConfigIcons.Entries.TEXT_EXPAND.renderCentered(
			  mStack, x - 15, y + 5, 9, 9,
			  (isExpanded()? 1 : 0) + (
			    isMouseOverLabel(mouseX, mouseY) && !textFieldWidget.isMouseOver(mouseX, mouseY)
			    && !sideButtonReference.isMouseOver(mouseX, mouseY)? 2 : 0));
		}
	}
	
	@Override protected boolean isFieldFullWidth() {
		return isExpanded();
	}
	
	@Override protected void renderField(
	  MatrixStack mStack, int fieldX, int fieldY, int fieldWidth, int fieldHeight, int x, int y,
	  int entryWidth, int entryHeight, int index, int mouseX, int mouseY, float delta
	) {
		float p = expandAnimator.getEaseOut();
		int expandedX = Minecraft.getInstance().fontRenderer.getBidiFlag()? fieldX : x + 14;
		renderChild(
		  mStack, (int) MathHelper.lerp(p, fieldX, expandedX),
		  isHeadless()? fieldY : (int) MathHelper.lerp(p, fieldY, y + 24),
		  (int) MathHelper.lerp(p, fieldWidth,
		             isHeadless()? entryWidth - 16 - resetButton.getWidth() : entryWidth - 14),
		  fieldHeight, mouseX, mouseY, delta);
	}
	
	@Override public void renderChildEntry(
	  MatrixStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		textFieldWidget.setEditable(shouldRenderEditable());
		// Text fields render the border outside, so we inset it 1px to match other controls
		textFieldWidget.x = x + 1;
		textFieldWidget.y = y + 1;
		textFieldWidget.setWidth(w - 2);
		textFieldWidget.setHeight(h - 2);
		textFieldWidget.render(mStack, mouseX, mouseY, delta);
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
	
	@Override public Optional<ITextComponent[]> getTooltip(int mouseX, int mouseY) {
		if (textFieldWidget.isMouseOver(mouseX, mouseY))
			return Optional.empty();
		return super.getTooltip(mouseX, mouseY);
	}
	
	public void setExpanded(boolean expanded) {
		if (!isExpandable()) expanded = false;
		if (this.expanded != expanded) {
			expandAnimator.setEaseOutTarget(expanded);
			this.expanded = expanded;
			if (isEditable()) setListener(textFieldWidget);
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
			WidgetUtils.forceUnFocus(getListener());
			textFieldWidget.setFocused(true);
			setListener(textFieldWidget);
			playFeedbackTap(1F);
			return true;
		}
		return false;
	}
	
	@Override public Optional<ITextComponent> getErrorMessage() {
		final Optional<ITextComponent> opt = super.getErrorMessage();
		if (opt.isPresent()) return opt;
		if (getText().length() < minLength)
			return Optional.of(
			  minLength == 1
			  ? new TranslationTextComponent("simpleconfig.config.error.string.empty")
			  : new TranslationTextComponent("simpleconfig.config.error.string.min_length", minLength));
		return Optional.empty();
	}
	
	@Override protected @NotNull List<? extends IGuiEventListener> getEntryListeners() {
		return isChildSubEntry() ? childWidgets : widgets;
	}
	
	public void setMaxLength(int maxLength) {
		this.maxLength = maxLength;
		textFieldWidget.setMaxLength(maxLength);
	}
	
	public void setMinLength(int minLength) {
		this.minLength = minLength;
	}
	
	public void setTextFormatter(ITextFormatter textFormatter) {
		this.textFormatter = textFormatter;
		textFieldWidget.setFormatter(ITextFormatter.cached(textFormatter));
	}
	
	public ITextFormatter getTextFormatter() {
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