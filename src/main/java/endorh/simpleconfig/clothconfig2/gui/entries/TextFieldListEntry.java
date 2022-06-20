package endorh.simpleconfig.clothconfig2.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.clothconfig2.api.IChildListEntry;
import endorh.simpleconfig.clothconfig2.gui.WidgetUtils;
import endorh.simpleconfig.clothconfig2.gui.widget.ResetButton;
import endorh.simpleconfig.clothconfig2.math.Rectangle;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@OnlyIn(value = Dist.CLIENT)
public abstract class TextFieldListEntry<V> extends TooltipListEntry<V> implements IChildListEntry {
	protected AtomicReference<V> value;
	protected HookedTextFieldWidget textFieldWidget;
	protected HookedTextFieldWidget longTextFieldWidget;
	protected ResetButton resetButton;
	protected List<IGuiEventListener> widgets;
	protected List<IGuiEventListener> expandedWidgets;
	protected List<IGuiEventListener> childWidgets;
	protected boolean canExpand;
	protected boolean expanded = false;
	protected Rectangle labelArea = new Rectangle();
	protected boolean child = false;
	protected int maxLength;
	protected int minLength;
	
	@Internal protected TextFieldListEntry(
	  ITextComponent fieldName, V original, boolean canExpand
	) {
		super(fieldName);
		value = new AtomicReference<>(original);
		this.original = original;
		this.canExpand = canExpand;
		textFieldWidget = new HookedTextFieldWidget(0, 0, 150, 18, NarratorChatListener.NO_TITLE);
		textFieldWidget.setMaxLength(999999);
		// this.textFieldWidget.setText(String.valueOf(original));
		longTextFieldWidget = new HookedTextFieldWidget(0, 0, 150, 18, NarratorChatListener.NO_TITLE);
		longTextFieldWidget.setMaxLength(999999);
		setValue(original);
		resetButton = new ResetButton(this);
		widgets = Lists.newArrayList(textFieldWidget, resetButton);
		expandedWidgets = Lists.newArrayList(longTextFieldWidget, resetButton);
		childWidgets = Lists.newArrayList(textFieldWidget);
	}
	
	@Override public V getValue() {
		return value.get();
	}
	
	@Override public void setValue(V v) {
		value.set(v);
		textFieldWidget.setTextNoUpdate(toString(v));
		longTextFieldWidget.setTextNoUpdate(toString(v));
	}
	
	public String getText() {
		return expanded? longTextFieldWidget.getValue() : textFieldWidget.getValue();
	}
	
	protected abstract @Nullable V fromString(String s);
	
	protected String toString(@Nullable V v) {
		return v != null? String.valueOf(v) : "";
	}
	
	protected String stripAddText(String s) {
		return s;
	}
	
	protected void textFieldPreRender(TextFieldWidget widget) {}
	
	@Override public void updateSelected(boolean isSelected) {
		super.updateSelected(isSelected);
		if (!isSelected)
			WidgetUtils.forceUnFocus(textFieldWidget, longTextFieldWidget, resetButton);
	}
	
	@Override public void renderEntry(
	  MatrixStack mStack, int index, int y, int x, int entryWidth, int entryHeight, int mouseX,
	  int mouseY, boolean isHovered, float delta
	) {
		super.renderEntry(
		  mStack, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		final Minecraft mc = Minecraft.getInstance();
		MainWindow window = mc.getWindow();
		mc.getTextureManager().bind(CONFIG_TEX);
		RenderHelper.turnOff();
		RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
		labelArea.setBounds(x - 15, y, entryWidth, 24);
		resetButton.y = y;
		if (canExpand) {
			int v = 0;
			if (expanded)
				v += 9;
			if (isMouseOverLabel(mouseX, mouseY) && !textFieldWidget.isMouseOver(mouseX, mouseY)
			    && !resetButton.isMouseOver(mouseX, mouseY))
				v += 18;
			blit(mStack, x - 15, y + 5, 102, v, 9, 9);
		}
		
		longTextFieldWidget.setEditable(isEditable());
		int textFieldX;
		ITextComponent name = getDisplayedFieldName();
		final FontRenderer font = mc.font;
		if (font.isBidirectional()) {
			font.drawShadow(
			  mStack, name.getVisualOrderText(),
			  (float) (window.getGuiScaledWidth() - x - font.width(name)),
			  (float) (y + 6), getPreferredTextColor());
			resetButton.x = x;
			textFieldX = x + resetButton.getWidth();
		} else {
			font.drawShadow(
			  mStack, name.getVisualOrderText(), (float) x, (float) (y + 6),
			  getPreferredTextColor());
			resetButton.x = x + entryWidth - resetButton.getWidth();
			textFieldX = x + entryWidth - 148;
		}
		if (expanded) {
			textFieldWidget.visible = false;
			longTextFieldWidget.visible = true;
			longTextFieldWidget.x = x + 14;
			longTextFieldWidget.y = y + 24;
			longTextFieldWidget.setWidth(entryWidth - 16);
			longTextFieldWidget.render(mStack, mouseX, mouseY, delta);
		} else {
			renderChild(mStack, textFieldX, y, 148 - resetButton.getWidth() - 4, 20, mouseX, mouseY, delta);
		}
		resetButton.render(mStack, mouseX, mouseY, delta);
	}
	
	@Override public void renderChildEntry(
	  MatrixStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		textFieldWidget.setEditable(isEditable());
		longTextFieldWidget.visible = false;
		textFieldWidget.visible = true;
		textFieldWidget.x = x;
		textFieldWidget.y = y + 1;
		textFieldWidget.setWidth(w);
		textFieldWidget.setHeight(h - 2);
		textFieldWidget.render(mStack, mouseX, mouseY, delta);
	}
	
	protected boolean isMouseOverLabel(double mouseX, double mouseY) {
		return labelArea.contains(mouseX, mouseY);
	}
	
	@Override public int getItemHeight() {
		return expanded? 48 : 24;
	}
	
	@Override public Optional<ITextComponent[]> getTooltip(int mouseX, int mouseY) {
		final Optional<ITextComponent[]> tooltip = resetButton.getTooltip(mouseX, mouseY);
		if (tooltip.isPresent())
			return tooltip;
		if (textFieldWidget.isMouseOver(mouseX, mouseY))
			return Optional.empty();
		return super.getTooltip(mouseX, mouseY);
	}
	
	public void setExpanded(boolean expanded) {
		if (!canExpand)
			expanded = false;
		if (this.expanded != expanded) {
			this.expanded = expanded;
			HookedTextFieldWidget focused = expanded? longTextFieldWidget : textFieldWidget;
			if (isEditable())
				setFocused(focused);
			textFieldWidget.setTextNoUpdate(toString(value.get()));
			longTextFieldWidget.setTextNoUpdate(toString(value.get()));
		}
	}
	
	public boolean isExpanded() {
		return expanded;
	}
	
	@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button))
			return true;
		if (canExpand && button == 0 && isMouseOverLabel(mouseX, mouseY)) {
			setExpanded(!isExpanded());
			Minecraft.getInstance().getSoundManager().play(
			  SimpleSound.forUI(SimpleConfigMod.UI_TAP, 1F));
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
	
	public @NotNull List<? extends IGuiEventListener> children() {
		return isChild()? childWidgets : expanded? expandedWidgets : widgets;
	}
	
	public void setMaxLength(int maxLength) {
		this.maxLength = maxLength;
		textFieldWidget.setMaxLength(maxLength);
		longTextFieldWidget.setMaxLength(maxLength);
	}
	
	public void setMinLength(int minLength) {
		this.minLength = minLength;
	}
	
	private class HookedTextFieldWidget extends TextFieldWidget {
		public HookedTextFieldWidget(int x, int y, int w, int h, ITextComponent title) {
			super(Minecraft.getInstance().font, x, y, w, h, title);
			setResponder(t -> value.set(fromString(t)));
		}
		
		public void render(@NotNull MatrixStack matrices, int int_1, int int_2, float float_1) {
			setFocused(
			  isSelected && getFocused() == this);
			textFieldPreRender(this);
			super.render(matrices, int_1, int_2, float_1);
			// drawSelectionBox() leaks its color mask
			RenderSystem.color4f(1F, 1F, 1F, 1F);
		}
		
		public void insertText(@NotNull String str) {
			super.insertText(stripAddText(str));
		}
		
		public void setTextNoUpdate(String text) {
			setResponder(t -> {});
			setValue(text);
			setResponder(t -> value.set(fromString(t)));
		}
	}
	
	@Override public boolean isChild() {
		return child;
	}
	
	@Override public void setChild(boolean child) {
		this.child = child;
	}
	
	@Override public boolean handleNavigationKey(int keyCode, int scanCode, int modifiers) {
		if (canExpand && Screen.hasAltDown()) {
			if (keyCode == 262 && !expanded) {
				setExpanded(true);
				Minecraft.getInstance().getSoundManager().play(
				  SimpleSound.forUI(SimpleConfigMod.UI_TAP, 1F));
				return true;
			} else if (keyCode == 263 && expanded) {
				setExpanded(false);
				Minecraft.getInstance().getSoundManager().play(
				  SimpleSound.forUI(SimpleConfigMod.UI_TAP, 1F));
				return true;
			}
		}
		return super.handleNavigationKey(keyCode, scanCode, modifiers);
	}
	
	@Override public String seekableValueText() {
		return expanded? longTextFieldWidget.getValue() : textFieldWidget.getValue();
	}
}