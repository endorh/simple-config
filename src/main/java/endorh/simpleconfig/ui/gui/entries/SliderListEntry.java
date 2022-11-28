package endorh.simpleconfig.ui.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.api.entry.RangedEntryBuilder.InvertibleDouble2DoubleFunction;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.gui.WidgetUtils;
import endorh.simpleconfig.ui.gui.widget.TextFieldWidgetEx;
import endorh.simpleconfig.ui.hotkey.HotKeyActionType;
import endorh.simpleconfig.ui.hotkey.HotKeyActionTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AbstractSlider;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static net.minecraft.util.math.MathHelper.ceil;
import static net.minecraft.util.math.MathHelper.clamp;

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
	
	protected Function<V, ITextComponent> textGetter;
	protected List<IGuiEventListener> widgets;
	protected List<IGuiEventListener> textWidgets;
	protected List<IGuiEventListener> childWidgets;
	protected List<IGuiEventListener> textChildWidgets;
	
	public SliderListEntry(
	  ITextComponent fieldName,
	  V min, V max, V value,
	  Function<V, ITextComponent> textGetter
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
		sliderValue = value;
		sliderWidget.setValue(value);
		sliderWidget.func_230979_b_();
		if (showText && !areEqual(textFieldEntry.getDisplayedValue(), value))
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
	  MatrixStack mStack, int index, int x, int y, int entryWidth, int entryHeight,
	  int mouseX, int mouseY, boolean isHovered, float delta
	) {
		super.renderEntry(mStack, index, x, y, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		if (showText && (!canUseTextField() || getListener() != textFieldEntry)) {
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
	  MatrixStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		if (isTextFieldShown()) {
			textFieldEntry.updateFocused(isFocused() && getListener() == textFieldEntry);
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
				setListener(textFieldEntry);
				WidgetUtils.forceUnFocus(sliderWidget);
				final TextFieldWidgetEx textFieldWidget = textFieldEntry.textFieldWidget;
				textFieldEntry.setListener(textFieldWidget);
				WidgetUtils.forceFocus(textFieldWidget);
				textFieldWidget.moveCaretToEnd();
				textFieldWidget.setAnchorPos(0);
			}
		} else if (focus) {
			setListener(sliderWidget);
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
			Minecraft.getInstance().getSoundHandler().play(
			  SimpleSound.master(canUseTextField()? SimpleConfigMod.UI_TAP : SimpleConfigMod.UI_DOUBLE_TAP, 1F));
			return true;
		}
		return false;
	}
	
	@Override public boolean charTyped(char codePoint, int modifiers) {
		final IGuiEventListener listener = getListener();
		if ((listener == sliderWidget || listener == textFieldEntry)
		    && canUseTextField() && codePoint == ' ') {
			// Space to toggle, Ctrl + Space to use text, Shift + Space to use slider
			boolean state = Screen.hasControlDown() || !Screen.hasShiftDown() && !isTextFieldShown();
			boolean change = state != isTextFieldShown();
			setTextFieldShown(state, true);
			Minecraft.getInstance().getSoundHandler().play(
			  SimpleSound.master(change? SimpleConfigMod.UI_TAP : SimpleConfigMod.UI_DOUBLE_TAP, 1F));
			return true;
		}
		return super.charTyped(codePoint, modifiers);
	}
	
	@Override public boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
		final IGuiEventListener listener = getListener();
		if (Screen.hasAltDown()) return true; // Prevent navigation key from triggering slider
		if ((listener == sliderWidget || listener == textFieldEntry)
		    && canUseTextField() && keyCode == GLFW.GLFW_KEY_ENTER) {
			// Space to toggle, Ctrl + Space to use text, Shift + Space to use slider
			boolean state = Screen.hasControlDown() || !Screen.hasShiftDown() && !isTextFieldShown();
			boolean change = state != isTextFieldShown();
			setTextFieldShown(state, true);
			Minecraft.getInstance().getSoundHandler().play(
			  SimpleSound.master(change? SimpleConfigMod.UI_TAP : SimpleConfigMod.UI_DOUBLE_TAP, 1F));
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
		boolean clamped = false;
		
		public SliderWidget(int x, int y, int width, int height) {
			super(x, y, width, height, StringTextComponent.EMPTY, 0D);
		}
		
		@Override protected void func_230979_b_() {
			setMessage(textGetter.apply(getDisplayedValue()));
		}
		
		@Override protected void func_230972_a_() {
			clamped = sliderValue < 0 || sliderValue > 1;
			SliderListEntry.this.sliderValue = getValue();
		}
		
		public double getStep(boolean left) {
			float step = left ? -2.0F : 2.0F;
			if (Screen.hasShiftDown())
				step *= 0.25D;
			if (Screen.hasControlDown())
				step *= 4D;
			return step / (float) (width - 8);
		}
		
		public double applyStep(double step) {
			return clamp(sliderValue + step, 0D, 1D);
		}
		
		@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
			if (isEditable()) {
				boolean left = keyCode == GLFW.GLFW_KEY_LEFT;
				if (left || keyCode == GLFW.GLFW_KEY_RIGHT) {
					final double step = getStep(left);
					final double v = applyStep(step);
					sliderValue = v;
					setDisplayedValue(getValue());
					sliderValue = v;
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
		
		@Override
		protected void renderBg(@NotNull MatrixStack mStack, @NotNull Minecraft mc, int mouseX, int mouseY) {
			Minecraft.getInstance().getTextureManager().bindTexture(WIDGETS_LOCATION);
			if (clamped) {
				RenderSystem.color4f(1F, 0.7F, 0.5F, 1F);
			} else RenderSystem.color4f(1F, 1F, 1F, 1F);
			int bw = clamped? 4 : 8;
			int vOffset = (isHovered()? 2 : 1) * 20;
			blit(mStack, x + (int)(sliderValue * (double)(width - bw)), y, 0, 46 + vOffset, bw / 2, 20);
			blit(mStack, x + (int)(sliderValue * (double)(width - bw)) + bw / 2, y, 200 - bw / 2, 46 + vOffset, bw / 2, 20);
		}
		
		@Override
		public void renderButton(@NotNull MatrixStack mStack, int mouseX, int mouseY, float delta) {
			Minecraft mc = Minecraft.getInstance();
			FontRenderer font = mc.fontRenderer;
			mc.getTextureManager().bindTexture(WIDGETS_LOCATION);
			if (clamped) {
				RenderSystem.color4f(1F, 0.7F, 0.5F, alpha);
			} else RenderSystem.color4f(1F, 1F, 1F, alpha);
			int level = getYImage(isHovered());
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.enableDepthTest();
			blit(mStack, x, y, 0, 46 + level * 20, width / 2, height);
			blit(mStack, x + width / 2, y, 200 - width / 2, 46 + level * 20, width / 2, height);
			renderBg(mStack, mc, mouseX, mouseY);
			int color = getFGColor();
			ITextProperties message = getMessage();
			if (font.getStringPropertyWidth(message) > width - 8 && !isHovered())
				message = font.func_238417_a_(message, width - 8);
			IReorderingProcessor seq = font.trimStringToWidth(message, Integer.MAX_VALUE).get(0);
			int textWidth = font.func_243245_a(seq);
			int textX = clamp(x + (width - textWidth) / 2, 4, getScreen().width - 4 - textWidth);
			font.func_238407_a_(
			  mStack, seq, textX, y + (height - 8) / 2F,
			  color | ceil(alpha * 255F) << 24);
			if (clamped) RenderSystem.color4f(1F, 1F, 1F, 1F);
		}
		
		protected double getSliderValue() {
			return sliderMap != null? sliderMap.apply(sliderValue) : sliderValue;
		}
		
		protected void setSliderValue(double value) {
			if (sliderMap != null) value = sliderMap.inverse(value);
			clamped = value < 0 || value > 1;
			sliderValue = clamp(value, 0, 1);
		}
		
		public abstract V getValue();
		public abstract void setValue(V v);
	}
}
