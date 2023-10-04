package endorh.simpleconfig.ui.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.api.entry.RangedEntryBuilder.InvertibleDouble2DoubleFunction;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.gui.widget.TextFieldWidgetEx;
import endorh.simpleconfig.ui.hotkey.HotKeyActionType;
import endorh.simpleconfig.ui.hotkey.HotKeyActionTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
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
	protected V sliderMin;
	protected V sliderMax;
	protected @Nullable InvertibleDouble2DoubleFunction sliderMap = null;
	
	protected Function<V, Component> textGetter;
	protected List<GuiEventListener> widgets;
	protected List<GuiEventListener> textWidgets;
	protected List<GuiEventListener> childWidgets;
	protected List<GuiEventListener> textChildWidgets;
	
	public SliderListEntry(
	  Component fieldName,
	  V min, V max, V value,
	  Function<V, Component> textGetter
	) {
		super(fieldName);
		this.min = min;
		this.max = max;
		sliderMin = min;
		sliderMax = max;
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
			throw new IllegalStateException("initWidgets called twice");
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
	
	@Override public Object getHotKeyActionValue() {
		if (isTextFieldEnforced())
			return textFieldEntry.getDisplayedValue();
		return super.getHotKeyActionValue();
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
		if (min.compareTo(sliderMin) > 0) sliderMin = min;
	}
	
	public V getMax() {
		return max;
	}
	public void setMax(V max) {
		this.max = max;
		if (max.compareTo(sliderMax) < 0) sliderMax = max;
	}
	
	public V getSliderMin() {
		return sliderMin;
	}
	public void setSliderMin(V sliderMin) {
		V v = getDisplayedValue();
		this.sliderMin = min.compareTo(sliderMin) > 0? min : sliderMin;
		setDisplayedValue(v);
	}
	
	public V getSliderMax() {
		return sliderMax;
	}
	public void setSliderMax(V sliderMax) {
		V v = getDisplayedValue();
		this.sliderMax = max.compareTo(sliderMax) < 0? max : sliderMax;
		setDisplayedValue(v);
	}
	
	public @Nullable InvertibleDouble2DoubleFunction getSliderMap() {
		return sliderMap;
	}
	public void setSliderMap(@Nullable InvertibleDouble2DoubleFunction sliderMap) {
		double v = sliderWidget.getSliderValue();
		this.sliderMap = sliderMap;
		sliderWidget.setSliderValue(v);
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
		sliderValue = value;
		sliderWidget.setValue(value);
		sliderWidget.updateMessage();
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
		return isTextFieldShown()? isChildSubEntry() ? textChildWidgets : textWidgets
		                         : isChildSubEntry() ? childWidgets : widgets;
	}
	
	@Override public boolean handleNavigationKey(int keyCode, int scanCode, int modifiers) {
		switch (keyCode) {
			case 262: // Right
				if (!isTextFieldShown() && !isChildSubEntry()) {
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
		if (showText && !isTextFieldEnforced() && (!canUseTextField() || getFocused() != textFieldEntry))
			setTextFieldShown(false, false);
		if (isTextFieldShown()) {
			textFieldEntry.updateFocused(isFocused() && getFocused() == textFieldEntry);
			textFieldEntry.setEditable(shouldRenderEditable());
			textFieldEntry.renderChild(mStack, x, y, w, h, mouseX, mouseY, delta);
			if (textFieldEntry.getErrorMessage().isEmpty() || isTextFieldEnforced()) {
				V value = textFieldEntry.getDisplayedValue();
				if (value != null) setDisplayedValue(value);
			}
		} else {
			sliderWidget.active = shouldRenderEditable();
			sliderWidget.setX(x);
			sliderWidget.setY(y);
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
         sideButtonReference.setFocused(false);
		if (show) {
			textFieldEntry.setDisplayedValue(getDisplayedValue());
			if (focus) {
				setFocused(textFieldEntry);
            ((GuiEventListener) sliderWidget).setFocused(false);
            final TextFieldWidgetEx textFieldWidget = textFieldEntry.textFieldWidget;
				textFieldEntry.setFocused(textFieldWidget);
				((GuiEventListener) textFieldWidget).setFocused(true);
				textFieldWidget.moveCaretToEnd();
				textFieldWidget.setAnchorPos(0);
			}
		} else if (focus) {
			setFocused(sliderWidget);
			((GuiEventListener) sliderWidget).setFocused(true);
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
		if (!isFocused) ((GuiEventListener) sliderWidget).setFocused(false);
	}
	
	public abstract class SliderWidget extends AbstractSliderButton {
		boolean clamped = false;
		
		protected SliderWidget(int x, int y, int width, int height) {
			super(x, y, width, height, Component.empty(), 0D);
		}
		
		@Override protected void updateMessage() {
			setMessage(textGetter.apply(getDisplayedValue()));
		}
		
		@Override protected void applyValue() {
			clamped = value < 0 || value > 1;
			sliderValue = getValue();
		}
		
		public double getStep(boolean left) {
			float step = left ? -2.0F : 2.0F;
			if (Screen.hasShiftDown())
				step *= 0.25F;
			if (Screen.hasControlDown())
				step *= 4F;
			return step / (float) (width - 8);
		}
		
		public double applyStep(double step) {
			return Mth.clamp(value + step, 0D, 1D);
		}
		
		@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
			if (isEditable()) {
				boolean left = keyCode == GLFW.GLFW_KEY_LEFT;
				if (left || keyCode == GLFW.GLFW_KEY_RIGHT) {
					final double step = getStep(left);
					final double v = applyStep(step);
					value = v;
					setDisplayedValue(getValue());
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
		
		protected void renderBg(@NotNull PoseStack mStack, @NotNull Minecraft mc, int mouseX, int mouseY) {
			RenderSystem.setShaderTexture(0, SLIDER_LOCATION);
			if (clamped) {
				RenderSystem.setShaderColor(1F, 0.7F, 0.5F, 1F);
			} else RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
			int bw = clamped? 4 : 8;
			int vOffset = (isHoveredOrFocused()? 3 : 2) * 20;
			blitNineSliced(mStack, getX() + (int) (value * (double) (width - bw)), getY(), bw, 20, 20, 4, 200, 20, 0, vOffset);
		}
		
		@Override
		public void renderWidget(@NotNull PoseStack mStack, int mouseX, int mouseY, float delta) {
			Minecraft mc = Minecraft.getInstance();
			Font font = mc.font;
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			RenderSystem.setShaderTexture(0, SLIDER_LOCATION);
			if (clamped) {
				RenderSystem.setShaderColor(1F, 0.7F, 0.5F, alpha);
			} else RenderSystem.setShaderColor(1F, 1F, 1F, alpha);
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.enableDepthTest();
			blit(mStack, getX(), getY(), 0, 0, width / 2, height);
			blit(mStack, getX() + width / 2, getY(), 200 - width / 2, 0, width / 2, height);
			renderBg(mStack, mc, mouseX, mouseY);
			int color = getFGColor();
			FormattedText message = getMessage();
			if (font.width(message) > width - 8 && !isHoveredOrFocused())
				message = font.substrByWidth(message, width - 8);
			FormattedCharSequence seq = font.split(message, Integer.MAX_VALUE).get(0);
			int textWidth = font.width(seq);
			int textX = Mth.clamp(getX() + (width - textWidth) / 2, 4, getScreen().width - 4 - textWidth);
			drawString(
			  mStack, font, seq, textX, getY() + (height - 8) / 2,
			  color | Mth.ceil(alpha * 255F) << 24);;
			if (clamped) RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
		}
		
		protected double getSliderValue() {
			return sliderMap != null? sliderMap.apply(value) : value;
		}
		
		protected void setSliderValue(double value) {
			if (sliderMap != null) value = sliderMap.inverse(value);
			clamped = value < 0 || value > 1;
			this.value = Mth.clamp(value, 0, 1);
		}
		
		public abstract V getValue();
		public abstract void setValue(V v);
	}
}
