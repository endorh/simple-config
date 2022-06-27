package endorh.simpleconfig.clothconfig2.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import endorh.simpleconfig.clothconfig2.api.IChildListEntry;
import endorh.simpleconfig.clothconfig2.gui.SimpleConfigIcons;
import endorh.simpleconfig.clothconfig2.gui.WidgetUtils;
import endorh.simpleconfig.clothconfig2.gui.entries.CaptionedSubCategoryListEntry.ToggleAnimator;
import endorh.simpleconfig.clothconfig2.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

import static net.minecraft.util.math.MathHelper.lerp;

@OnlyIn(value = Dist.CLIENT)
public abstract class TextFieldListEntry<V> extends TooltipListEntry<V> implements IChildListEntry {
	protected V displayedValue;
	protected HookedTextFieldWidget textFieldWidget;
	protected List<IGuiEventListener> widgets;
	protected List<IGuiEventListener> childWidgets;
	protected boolean expandable;
	private boolean expanded = false;
	protected Rectangle labelArea = new Rectangle();
	protected ToggleAnimator expandAnimator = new ToggleAnimator(250L);
	protected int maxLength;
	protected int minLength;
	private int frame = 0;
	
	@Internal protected TextFieldListEntry(
	  ITextComponent fieldName, V original, boolean canExpand
	) {
		super(fieldName);
		this.expandable = canExpand;
		textFieldWidget = new HookedTextFieldWidget(0, 0, 150, 18, NarratorChatListener.NO_TITLE);
		textFieldWidget.setMaxLength(999999);
		setOriginal(original);
		setValue(original);
		setDisplayedValue(original);
		widgets = Lists.newArrayList(textFieldWidget, resetButton);
		childWidgets = Lists.newArrayList(textFieldWidget);
	}
	
	@Override public void setSubEntry(boolean isSubEntry) {
		super.setSubEntry(isSubEntry);
		expandAnimator.setLength(isSubEntry? 0L : 250L);
	}
	
	@Override public V getDisplayedValue() {
		return displayedValue;
	}
	@Override public void setDisplayedValue(V v) {
		displayedValue = v;
		textFieldWidget.setTextNoUpdate(toString(v));
	}
	public String getText() {
		return textFieldWidget.getValue();
	}
	
	protected abstract @Nullable V fromString(String s);
	protected String toString(@Nullable V v) {
		return v != null? String.valueOf(v) : "";
	}
	
	protected String stripAddText(String s) {
		return s;
	}
	protected void textFieldPreRender(TextFieldWidget widget) {}
	
	@Override public void updateFocused(boolean isFocused) {
		super.updateFocused(isFocused);
		if (!isFocused)
			WidgetUtils.forceUnFocus(textFieldWidget);
	}
	
	@Override public void tick() {
		super.tick();
		if ((frame++) % 10 == 0) textFieldWidget.tick();
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
			  (isExpanded()? 1 : 0) + ((
			    isMouseOverLabel(mouseX, mouseY) && !textFieldWidget.isMouseOver(mouseX, mouseY)
			    && !resetButton.isMouseOver(mouseX, mouseY))? 2 : 0));
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
		int expandedX = Minecraft.getInstance().font.isBidirectional()? fieldX : x + 14;
		renderChild(
		  mStack, (int) lerp(p, fieldX, expandedX),
		  isHeadless()? fieldY : (int) lerp(p, fieldY, y + 24),
		  (int) lerp(p, fieldWidth,
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
	
	@Override public void setPreviewingExternal(boolean previewing) {
		super.setPreviewingExternal(previewing);
		if (isPreviewingExternal()) setExpanded(false);
	}
	
	protected boolean isMouseOverLabel(double mouseX, double mouseY) {
		return labelArea.contains(mouseX, mouseY);
	}
	
	@Override public int getItemHeight() {
		return 24 + (isHeadless()? 0 : (int) (expandAnimator.getEaseOut() * 24));
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
		return this.isChildSubEntry() ? childWidgets : widgets;
	}
	
	public void setMaxLength(int maxLength) {
		this.maxLength = maxLength;
		textFieldWidget.setMaxLength(maxLength);
	}
	
	public void setMinLength(int minLength) {
		this.minLength = minLength;
	}
	
	private class HookedTextFieldWidget extends TextFieldWidget {
		public HookedTextFieldWidget(int x, int y, int w, int h, ITextComponent title) {
			super(Minecraft.getInstance().font, x, y, w, h, title);
			setResponder(t -> displayedValue = fromString(t));
		}
		
		public void render(@NotNull MatrixStack matrices, int mouseX, int mouseY, float float_1) {
			setFocused(TextFieldListEntry.this.isFocused() && getFocused() == this);
			textFieldPreRender(this);
			super.render(matrices, mouseX, mouseY, float_1);
			// drawSelectionBox() leaks its color mask
			RenderSystem.color4f(1F, 1F, 1F, 1F);
		}
		
		public void insertText(@NotNull String str) {
			super.insertText(stripAddText(str));
		}
		
		public void setTextNoUpdate(String text) {
			setResponder(t -> {});
			setValue(text);
			setResponder(t -> displayedValue = fromString(t));
		}
	}
	
	@Override public boolean handleNavigationKey(int keyCode, int scanCode, int modifiers) {
		if (isExpandable() && Screen.hasAltDown()) {
			if (keyCode == 262 && !isExpanded()) {
				setExpanded(true);
				playFeedbackTap(1F);
				return true;
			} else if (keyCode == 263 && isExpanded()) {
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