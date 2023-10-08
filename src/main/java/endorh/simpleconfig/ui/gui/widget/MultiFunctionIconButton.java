package endorh.simpleconfig.ui.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import endorh.simpleconfig.api.ui.icon.Icon;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons.Backgrounds;
import endorh.simpleconfig.api.ui.math.Point;
import endorh.simpleconfig.api.ui.math.Rectangle;
import endorh.simpleconfig.ui.api.IMultiTooltipScreen;
import endorh.simpleconfig.ui.api.ScissorsHandler;
import endorh.simpleconfig.ui.api.Tooltip;
import endorh.simpleconfig.ui.gui.OverlayInjector;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction.ButtonActionBuilder;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.Modifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
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
	protected Supplier<Component> defaultTitle;
	protected Supplier<List<Component>> defaultTooltip;
	protected Supplier<Optional<SoundInstance>> defaultSound;
	protected TreeSet<Pair<Modifier, ButtonAction>> actions =
	  new TreeSet<>(Comparator.comparing(Pair::getLeft));
	protected @NotNull ButtonAction activeAction;
	protected @NotNull ButtonAction defaultAction;
	protected Boolean activeOverride = null;
	protected int maxWidth;
	protected int minWidth;
	protected int defaultTint = 0;
	protected boolean suppressHoverPeek = false;
	
	public static MultiFunctionIconButton of(
	  @NotNull Icon icon,   ButtonActionBuilder builder
	) {
		return of(icon, icon.w, icon.w, builder);
	}
	
	public static MultiFunctionIconButton of(
	  @NotNull Icon icon, int minWidth, int maxWidth, ButtonActionBuilder builder
	) {
		return new MultiFunctionIconButton(0, 0, minWidth, maxWidth, icon, builder);
	}
	
	public MultiFunctionIconButton(
	  int x, int y, int minWidth, int maxWidth, @NotNull Icon icon,
	  ButtonActionBuilder action
	) {
		super(x, y, minWidth, 20, Component.empty(), b -> {});
		final ButtonAction defaultAction = action.build();
		defaultIcon = icon;
		defaultActivePredicate = defaultAction.activePredicate != null? defaultAction.activePredicate : () -> true;
		defaultTitle = defaultAction.titleSupplier != null? defaultAction.titleSupplier : Component::empty;
		defaultTooltip = defaultAction.tooltipSupplier != null? defaultAction.tooltipSupplier : Collections::emptyList;
		defaultSound = defaultAction.sound != null? defaultAction.sound : () -> Optional.of(
		  SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
		actions.add(Pair.of(Modifier.NONE, defaultAction));
		this.defaultAction = activeAction = defaultAction;
		this.minWidth = minWidth;
		this.maxWidth = maxWidth;
		overlay = new ButtonOverlay(this);
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
			  .ifPresent(s -> Minecraft.getInstance().getSoundManager().play(s));
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
	
	public Component getTitle() {
		ButtonAction action = activeAction;
		return action.titleSupplier != null? action.titleSupplier.get() : defaultTitle.get();
	}
	
	@Override public void renderWidget(
      @NotNull GuiGraphics gg, int mouseX, int mouseY, float partialTicks
	) {
		activeAction = actions.stream().filter(
		  p -> p.getLeft().isActive()
		).map(Pair::getRight).filter(
		  a -> a.activePredicate == null || a.activePredicate.get()
		).findFirst().orElse(defaultAction);
		final ButtonAction action = activeAction;
		active = activeOverride != null? activeOverride : (action.activePredicate != null? action.activePredicate : defaultActivePredicate).get();
		
		Minecraft mc = Minecraft.getInstance();
		Font font = mc.font;
		Component title = getTitle();
		Icon icon = action.icon != null ? action.icon.get() : null;
		if (icon == null) icon = getDefaultIcon();
		final int textWidth = font.width(title);
		final int iconWidth = icon != Icon.EMPTY ? 20 : 4;
		final int contentWidth = textWidth + iconWidth + 4;
		if (minWidth != -1 && maxWidth != -1)
			width = max(minWidth, min(maxWidth, (contentWidth + 1) / 2 * 2));
		if (action.tint != null) {
			Integer tint = action.tint.get();
			super.setTintColor(tint != null? tint : 0);
		} else super.setTintColor(defaultTint);
		
		RenderSystem.setShaderColor(1F, 1F, 1F, alpha);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		
		RenderSystem.setShaderTexture(0, WIDGETS_LOCATION);
		int level = getTextureLevel();
		Backgrounds.BUTTON_BACKGROUND.renderStretch(gg, getX(), getY(), width, height, level);
		renderTint(gg, getX(), getY(), getX() + width, getY() + height);
		int color = getFGColor();
		contentArea.setBounds(area.x + 2, area.y + 2, area.width - 4, area.height - 4);
		PoseStack mStack = gg.pose();
		mStack.pushPose(); {
			if (contentWidth < width) mStack.translate((width - contentWidth) / 2.0, 0.0, 0.0);
			if (icon != Icon.EMPTY) {
				RenderSystem.setShaderColor(0.1F, 0.1F, 0.1F, 0.8F);
				icon.renderCentered(gg, getX() + 1, getY() + 1, 20, 20);
				float intensity = active ? 1F : 0.3F;
				RenderSystem.setShaderColor(intensity, intensity, intensity, 1F);
				icon.renderCentered(gg, getX(), getY(), 20, 20);
			}
			RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
			if (width > iconWidth) {
				if (contentWidth > width && !suppressHoverPeek) {
					ScissorsHandler.INSTANCE.withScissor(
					  contentArea, () -> gg.drawString(
						 font, title, getX() + iconWidth, getY() + (height - 8) / 2,
						 color | Mth.ceil(alpha * 255.0F) << 24));
					if (isMouseOver(mouseX, mouseY) && !overlay.isRendering()) {
						Screen screen = mc.screen;
						if (overlayArea == null) {
							overlayArea = new Rectangle();
							OverlayInjector.injectVisualOverlay(overlayArea, overlay, 10);
						}
						overlayArea.setBounds(getX(), getY(), contentWidth + 4, height + 1);
						if (screen != null && overlayArea.getMaxX() > screen.width)
							overlayArea.x = max(4, screen.width - 4 - overlayArea.getWidth());
					}
				} else {
					if (overlayArea != null) overlayArea.setBounds(getX(), getY(), width, height + 1);
					gg.drawString(
					  font, title, getX() + iconWidth, getY() + (height - 8) / 2,
					  color | Mth.ceil(alpha * 255.0F) << 24);
				}
			}
		} mStack.popPose();
		
		if (isHoveredOrFocused()) renderToolTip(gg, mouseX, mouseY);
	}
	
	public boolean isSuppressHoverPeek() {
		return suppressHoverPeek;
	}
	
	public void setSuppressHoverPeek(boolean suppressHoverPeek) {
		this.suppressHoverPeek = suppressHoverPeek;
		if (suppressHoverPeek) overlayArea = null;
	}
	
	@Override public void renderToolTip(@NotNull GuiGraphics gg, int mouseX, int mouseY) {
		final List<Component> ls = getTooltipContents();
		if (!ls.isEmpty()) {
			final Screen screen = Minecraft.getInstance().screen;
			boolean hovered = isMouseOver(mouseX, mouseY);
			int tooltipX = hovered? mouseX : getX() + width / 2;
			int tooltipY = hovered? mouseY : getY() < 64? getY() + height : getY();
			if (screen instanceof IMultiTooltipScreen ts) {
				ts.addTooltip(Tooltip.of(
				  area, Point.of(tooltipX, tooltipY), ls
				).asKeyboardTooltip(!hovered));
			} else if (screen != null) gg.renderComponentTooltip(Minecraft.getInstance().font, ls, tooltipX, tooltipY);
		}
	}
	
	@Override
	public List<Component> getTooltipContents() {
		ButtonAction action = activeAction;
		return (action.tooltipSupplier != null? action.tooltipSupplier : defaultTooltip).get();
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
		if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_KP_ENTER)
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
	
	@Override public void setWidth(int width) {
		setExactWidth(width);
	}
	
	public void setExactWidth(int width) {
		setMinWidth(width);
		setMaxWidth(width);
	}
	
	public Icon getDefaultIcon() {
		return defaultIcon;
	}
	
	public void setDefaultIcon(Icon defaultIcon) {
		this.defaultIcon = defaultIcon;
	}
	
	@Override public void setTintColor(int color) {
		super.setTintColor(color);
		defaultTint = color;
	}
	
	@Override public int getWidth() {
		final ButtonAction action = actions.stream().filter(p -> p.getLeft().isActive()).map(Pair::getRight).findFirst()
		  .orElseThrow(() -> new IllegalStateException("Button without default action"));
		Minecraft mc = Minecraft.getInstance();
		Font font = mc.font;
		Component title = action.titleSupplier != null ? action.titleSupplier.get() : defaultTitle.get();
		Icon icon = action.icon != null ? action.icon.get() : null;
		if (icon == null) icon = getDefaultIcon();
		final int textWidth = font.width(title);
		final int contentWidth = textWidth + (icon != Icon.EMPTY? 20 : 4) + 4;
		return max(minWidth, min(maxWidth, (contentWidth + 1) / 2 * 2));
	}
	
	public static class ButtonOverlay extends TintedButton.ButtonOverlay {
		private final MultiFunctionIconButton button;
		
		public ButtonOverlay(MultiFunctionIconButton button) {
			super(button);
			this.button = button;
		}
		
		@Override public boolean renderOverlay(
         GuiGraphics gg, Rectangle area, int mouseX, int mouseY, float delta
		) {
			if (!button.isMouseOver(mouseX, mouseY) || button.overlayArea == null) {
				button.overlayArea = null;
				animator.stopAndSet(0F);
				return false;
			}
			if (animator.getTarget() == 0F) {
				animator.resetTarget();
				animator.setOutputRange(button.maxWidth, area.width);
			}
			if (lastWidth != area.width) {
				animator.setOutputRange(animator.getEaseOut(), area.width);
				animator.resetTarget();
				lastWidth = area.width;
			}
			rendering = true;
			int x = button.getX();
			int y = button.getY();
			int minW = button.minWidth;
			int maxW = button.maxWidth;
			int h = button.height;
			int w = (int) animator.getEaseOut();
			button.setPosition(area.x, area.y);
			((AbstractWidget) button).setWidth(w);
			button.setHeight(area.height);
			button.setExactWidth(w);
			this.area.setBounds(area.x, area.y, w, area.height);
			ScissorsHandler.INSTANCE.withSingleScissor(
			  this.area, () -> button.render(gg, mouseX, mouseY, delta));
			button.setX(x);
			button.setY(y);
			button.minWidth = minW;
			button.maxWidth = maxW;
			button.height = h;
			rendering = false;
			return true;
		}
	}
}