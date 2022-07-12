package endorh.simpleconfig.ui.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import endorh.simpleconfig.ui.api.ScissorsHandler;
import endorh.simpleconfig.ui.api.Tooltip;
import endorh.simpleconfig.ui.gui.IMultiTooltipScreen;
import endorh.simpleconfig.ui.gui.Icon;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction.ButtonActionBuilder;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.Modifier;
import endorh.simpleconfig.ui.math.Point;
import endorh.simpleconfig.ui.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.Supplier;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class MultiFunctionIconButton extends TintedButton {
	protected Icon defaultIcon;
	protected Supplier<Boolean> defaultActivePredicate;
	protected Supplier<ITextComponent> defaultTitle;
	protected Supplier<List<ITextComponent>> defaultTooltip;
	protected Supplier<Optional<ISound>> defaultSound;
	protected TreeSet<Pair<Modifier, ButtonAction>> actions =
	  new TreeSet<>(Comparator.comparing(Pair::getLeft));
	protected @NotNull ButtonAction activeAction;
	protected @NotNull ButtonAction defaultAction;
	protected Boolean activeOverride = null;
	protected int maxWidth;
	protected int minWidth;
	protected int defaultTint = 0;
	
	public static MultiFunctionIconButton of(
	  @NotNull Icon icon, int minWidth, int maxWidth, ButtonActionBuilder builder
	) {
		return new MultiFunctionIconButton(0, 0, minWidth, maxWidth, icon, builder);
	}
	
	public MultiFunctionIconButton(
	  int x, int y, int minWidth, int maxWidth, @NotNull Icon icon,
	  ButtonActionBuilder action
	) {
		super(x, y, minWidth, 20, StringTextComponent.EMPTY, b -> {}, field_238486_s_);
		final ButtonAction defaultAction = action.build();
		defaultIcon = icon;
		defaultActivePredicate = defaultAction.activePredicate != null? defaultAction.activePredicate : () -> true;
		defaultTitle = defaultAction.titleSupplier != null? defaultAction.titleSupplier : () -> StringTextComponent.EMPTY;
		defaultTooltip = defaultAction.tooltipSupplier != null? defaultAction.tooltipSupplier : Collections::emptyList;
		defaultSound = defaultAction.sound != null? defaultAction.sound : () -> Optional.of(
		  SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
		actions.add(Pair.of(Modifier.NONE, defaultAction));
		this.defaultAction = activeAction = defaultAction;
		this.minWidth = minWidth;
		this.maxWidth = maxWidth;
	}
	
	@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
		return active && visible && clicked(mouseX, mouseY) && click(button);
	}
	
	public boolean click(int button) {
		return onClick(activeAction, button);
	}
	
	public boolean click(boolean ctrl, boolean alt, boolean shift, int button) {
		return click(modifiers(ctrl, alt, shift), button);
	}
	
	public boolean click(int modifiers, int button) {
		return onClick(getActiveAction(modifiers), button);
	}
	
	protected boolean onClick(ButtonAction action, int button) {
		if ((action.activePredicate != null? action.activePredicate : defaultActivePredicate).get()) {
			action.action.accept(button);
			(action.sound != null ? action.sound : defaultSound).get()
			  .ifPresent(s -> Minecraft.getInstance().getSoundHandler().play(s));
			return true;
		}
		return false;
	}
	
	public @NotNull ButtonAction getActiveAction() {
		return activeAction;
	}
	
	public @NotNull ButtonAction getActiveAction(int modifiers) {
		return actions.stream().filter(
		  p -> p.getLeft().isActive(modifiers)
		).map(Pair::getRight).filter(
		  a -> a.activePredicate == null || a.activePredicate.get()
		).findFirst().orElse(defaultAction);
	}
	
	public MultiFunctionIconButton on(
	  Modifier modifier, ButtonActionBuilder action
	) {
		final ButtonAction a = action.build();
		actions.add(Pair.of(modifier, a));
		return this;
	}
	
	@Override public void renderButton(
	  @NotNull MatrixStack mStack, int mouseX, int mouseY, float partialTicks
	) {
		activeAction = actions.stream().filter(
		  p -> p.getLeft().isActive()
		).map(Pair::getRight).filter(
		  a -> a.activePredicate == null || a.activePredicate.get()
		).findFirst().orElse(defaultAction);
		final ButtonAction action = activeAction;
		active = activeOverride != null? activeOverride : (action.activePredicate != null? action.activePredicate : defaultActivePredicate).get();
		
		Minecraft mc = Minecraft.getInstance();
		FontRenderer font = mc.fontRenderer;
		ITextComponent title = action.titleSupplier != null ? action.titleSupplier.get() : defaultTitle.get();
		Icon icon = action.icon != null ? action.icon.get() : null;
		if (icon == null) icon = defaultIcon;
		final int textWidth = font.getStringPropertyWidth(title);
		final int iconWidth = icon != Icon.EMPTY ? 20 : 4;
		final int contentWidth = textWidth + iconWidth + 4;
		if (minWidth != -1 && maxWidth != -1) {
			width = max(minWidth, min(maxWidth, (contentWidth + 1) / 2 * 2));
		}
		if (action.tint != null) {
			Integer tint = action.tint.get();
			super.setTintColor(tint != null? tint : 0);
		} else super.setTintColor(defaultTint);
		
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, alpha);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		
		mc.getTextureManager().bindTexture(WIDGETS_LOCATION);
		int i = getYImage(isHovered());
		blit(mStack, x, y, 0, 46 + i * 20, width / 2, height);
		blit(mStack, x + width / 2, y, 200 - width / 2, 46 + i * 20, width / 2, height);
		renderBg(mStack, mc, mouseX, mouseY);
		int color = getFGColor();
		mStack.push(); {
			if (contentWidth < width) mStack.translate((width - contentWidth) / 2.0, 0.0, 0.0);
			if (icon != Icon.EMPTY) {
				RenderSystem.color4f(0.1F, 0.1F, 0.1F, 0.8F);
				icon.renderCentered(mStack, x + 1, y + 1, 20, 20);
				float intensity = active ? 1F : 0.3F;
				RenderSystem.color4f(intensity, intensity, intensity, 1F);
				icon.renderCentered(mStack, x, y, 20, 20);
			}
			RenderSystem.color4f(1F, 1F, 1F, 1F);
			if (width > iconWidth) {
				if (contentWidth > width) {
					ScissorsHandler.INSTANCE.scissor(new Rectangle(x, y, width, height));
					drawString(mStack, font, title, x + iconWidth, y + (height - 8) / 2, color | MathHelper.ceil(alpha * 255.0F) << 24);
					ScissorsHandler.INSTANCE.removeLastScissor();
				} else drawString(mStack, font, title, x + iconWidth, y + (height - 8) / 2, color | MathHelper.ceil(alpha * 255.0F) << 24);
			}
		} mStack.pop();
		
		if (isHovered())
			renderToolTip(mStack, mouseX, mouseY);
	}
	
	private static final ITextComponent[] EMPTY_TEXT_COMPONENT_ARRAY = new ITextComponent[0];
	@Override public void renderToolTip(@NotNull MatrixStack mStack, int mouseX, int mouseY) {
		final ButtonAction action = activeAction;
		final List<ITextComponent> ls = (action.tooltipSupplier != null? action.tooltipSupplier : defaultTooltip).get();
		if (!ls.isEmpty()) {
			final Screen screen = Minecraft.getInstance().currentScreen;
			if (screen instanceof IMultiTooltipScreen) {
				((IMultiTooltipScreen) screen).addTooltip(Tooltip.of(
				  new Point(mouseX, mouseY), ls.toArray(EMPTY_TEXT_COMPONENT_ARRAY)));
			} else if (screen != null)
				screen.renderWrappedToolTip(mStack, ls, mouseX, mouseY, Minecraft.getInstance().fontRenderer);
		}
	}
	
	public boolean press(boolean ctrl, boolean alt, boolean shift) {
		return press(modifiers(ctrl, alt, shift));
	}
	
	private static int modifiers(boolean ctrl, boolean alt, boolean shift) {
		int modifiers = 0;
		if (ctrl) modifiers |= GLFW.GLFW_MOD_CONTROL;
		if (alt) modifiers |= GLFW.GLFW_MOD_ALT;
		if (shift) modifiers |= GLFW.GLFW_MOD_SHIFT;
		return modifiers;
	}
	
	public boolean press(int modifiers) {
		if (active && visible) {
			final ButtonAction action = getActiveAction(modifiers);
			return onClick(action, -1);
		}
		return false;
	}
	
	@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == 257 || keyCode == 32 || keyCode == 335) // Enter | Space | NumPadEnter
			return press(modifiers);
		return false;
	}
	
	public Boolean getActiveOverride() {
		return activeOverride;
	}
	
	public void setActiveOverride(Boolean activeOverride) {
		this.activeOverride = activeOverride;
	}
	
	public int getMaxWidth() {
		return maxWidth;
	}
	
	public void setMaxWidth(int maxWidth) {
		this.maxWidth = maxWidth;
	}
	
	public int getMinWidth() {
		return minWidth;
	}
	
	public void setMinWidth(int minWidth) {
		this.minWidth = minWidth;
	}
	
	public void setWidthRange(int min, int max) {
		setMinWidth(min);
		setMaxWidth(max);
	}
	
	public void setExactWidth(int width) {
		setMinWidth(width);
		setMaxWidth(width);
	}
	
	@Override public void setTintColor(int color) {
		super.setTintColor(color);
		defaultTint = color;
	}
	
	@Override public int getWidth() {
		final ButtonAction action = actions.stream().filter(p -> p.getLeft().isActive()).map(Pair::getRight).findFirst()
		  .orElseThrow(() -> new IllegalStateException("Button without default action"));
		Minecraft mc = Minecraft.getInstance();
		FontRenderer font = mc.fontRenderer;
		ITextComponent title = action.titleSupplier != null ? action.titleSupplier.get() : defaultTitle.get();
		Icon icon = action.icon != null ? action.icon.get() : null;
		if (icon == null) icon = defaultIcon;
		final int textWidth = font.getStringPropertyWidth(title);
		final int contentWidth = textWidth + (icon != Icon.EMPTY? 20 : 4) + 4;
		return max(minWidth, min(maxWidth, (contentWidth + 1) / 2 * 2));
	}
}