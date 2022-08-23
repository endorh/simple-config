package endorh.simpleconfig.ui.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import endorh.simpleconfig.api.ui.icon.Icon;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons.Buttons;
import endorh.simpleconfig.api.ui.math.Rectangle;
import endorh.simpleconfig.config.ClientConfig.confirm;
import endorh.simpleconfig.ui.api.AbstractConfigField;
import endorh.simpleconfig.ui.api.IEntryHolder;
import endorh.simpleconfig.ui.api.IExtendedDragAwareGuiEventListener;
import endorh.simpleconfig.ui.api.IOverlayCapableContainer.IOverlayRenderer;
import endorh.simpleconfig.ui.api.ScissorsHandler;
import endorh.simpleconfig.ui.gui.AbstractConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Optional;

import static java.lang.Math.abs;
import static java.lang.Math.round;
import static java.util.Arrays.asList;

public class ResetButton extends MultiFunctionImageButton
  implements IExtendedDragAwareGuiEventListener, IOverlayRenderer {
	protected static List<Component> resetTooltip = asList(
	  new TranslatableComponent("simpleconfig.ui.reset"),
	  new TranslatableComponent("simpleconfig.ui.restore.alt"),
	  new TranslatableComponent("simpleconfig.ui.hotkey.ctrl"));
	protected static List<Component> resetTooltipGroup = asList(
	  new TranslatableComponent("simpleconfig.ui.reset.group"),
	  new TranslatableComponent("simpleconfig.ui.restore.alt"),
	  new TranslatableComponent("simpleconfig.ui.hotkey.ctrl"));
	protected static List<Component> restoreTooltip = asList(
	  new TranslatableComponent("simpleconfig.ui.restore"));
	protected static List<Component> restoreTooltipGroup = asList(
	  new TranslatableComponent("simpleconfig.ui.restore.group"));
	
	protected AbstractConfigField<?> entry;
	protected boolean shift = false;
	protected boolean alt = false;
	protected boolean isGroup = false;
	protected boolean isActive = false;
	protected boolean confirming = false;
	protected boolean dragging = false;
	protected int dragOffset = 0;
	protected double dragAnchor = 0;
	protected Rectangle overlay = new Rectangle();
	
	public ResetButton(AbstractConfigField<?> entry) {
		super(
		  0, 0, 20, 20, Buttons.RESET, ButtonAction.of(b -> {}),
		  NarratorChatListener.NO_TITLE);
		this.entry = entry;
		defaultActivePredicate = this::shouldBeActive;
	}
	
	protected boolean shouldSafeGuard() {
		return isRestore() ? isGroup() ? confirm.group_restore : confirm.restore :
		       isGroup() ? confirm.group_reset : confirm.reset;
	}
	
	@Override public void render(@NotNull PoseStack mStack, int mouseX, int mouseY, float delta) {
		if (dragging) {
			if (Minecraft.getInstance().font.isBidirectional())
				overlay.setBounds(x - 4, y - 4, 2 * width + 48, height + 8);
			else overlay.setBounds(x - width - 44, y - 4, 2 * width + 48, height + 8);
		} else if (confirming) {
			overlay.setBounds(x - 4, y - 4, width + 8, height + 8);
		} else overlay.setBounds(x, y, width, height);
		overlay.setBounds(ScissorsHandler.INSTANCE.getScissorsAreas().stream().reduce(overlay, Rectangle::intersection));
		if (overlay.isEmpty())
			overlay.setBounds(0, 0, 0, 0);
		super.render(mStack, mouseX, mouseY, delta);
		if (isFocused() && !active && entry != null) {
			entry.changeFocus(false);
			setFocused(false);
		}
	}
	
	protected boolean isRestore() {
		return alt;
	}
	
	protected boolean isGroup() {
		return isGroup;
	}
	
	protected boolean computeIsGroup() {
		if (!entry.isGroup()) return false;
		if (!(entry instanceof final IEntryHolder entryHolder)) return true;
		boolean restore = isRestore();
		return shift
		       || (restore? entryHolder.getSingleRestorableEntry()
		                  : entryHolder.getSingleResettableEntry()) == null
		       || (restore? entry.canRestoreGroup() : entry.canResetGroup())
		          && !(restore? entry.isRestorable() : entry.isResettable());
	}
	
	protected Icon getIcon() {
		return isRestore()?
		       isGroup()? Buttons.RESTORE_GROUP : Buttons.RESTORE :
		       isGroup()? Buttons.RESET_GROUP : Buttons.RESET;
	}
	
	@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (active && visible) {
			if (keyCode != GLFW.GLFW_KEY_ENTER && keyCode != GLFW.GLFW_KEY_SPACE && keyCode != GLFW.GLFW_KEY_KP_ENTER) return false;
			return activate(Screen.hasControlDown() ? 2 : Screen.hasShiftDown() ? 1 : 0);
		}
		return false;
	}
	
	public boolean activate() {
		return activate(0);
	}
	
	public boolean activate(int button) {
		if (!shouldSafeGuard() || confirming) {
			confirming = false;
			if (onPress(this, button)) {
				playDownSound(Minecraft.getInstance().getSoundManager());
				return true;
			} else if (button != 0 && onPress(this, 0)) {
				playDownSound(Minecraft.getInstance().getSoundManager());
				return true;
			}
			return false;
		}
		playDownSound(Minecraft.getInstance().getSoundManager());
		confirming = true;
		dragging = false;
		getScreen().addOverlay(overlay, this, 20);
		return true;
	}
	
	protected boolean shouldBeActive() {
		return isActive;
	}
	
	protected boolean computeActive() {
		if (entry == null) return false;
		if (entry.isSubEntry()) return false;
		return isGroup()? isRestore()? entry.canRestoreGroup() : entry.canResetGroup()
		                : isRestore()? entry.isRestorable() : entry.isResettable();
	}
	
	public void tick() {
		if (!confirming && !dragging && !getScreen().hasDialogs()) {
			shift = Screen.hasShiftDown();
			alt = Screen.hasAltDown();
			defaultIcon = getIcon();
		}
		isGroup = computeIsGroup();
		isActive = computeActive();
	}
	
	protected boolean reset(AbstractConfigField<?> entry, int button) {
		tick();
		if (button == 0) {
			if (entry instanceof final IEntryHolder entryHolder) {
				final AbstractConfigField<?> singleEntry = entryHolder.getSingleResettableEntry();
				if (singleEntry != null && isGroup()) {
					if (isRestore() && entry.isRestorable()) {
						entryHolder.restoreSingleEntry(singleEntry);
						return true;
					} else if (!isRestore() && entry.isResettable()) {
						entryHolder.resetSingleEntry(singleEntry);
						return true;
					}
					return false;
				}
			}
			if (isRestore() && isGroup()? entry.canRestoreGroup() : entry.isRestorable()) {
				entry.restoreValue();
				return true;
			} else if (!isRestore() && isGroup()? entry.canResetGroup() : entry.isResettable()) {
				entry.resetValue();
				return true;
			}
			return false;
		}
		return false;
	}
	
	public boolean onPress(Button widget, int button) {
		if (entry != null) {
			final boolean result = reset(entry, button);
			if (result) {
				Minecraft.getInstance().getSoundManager().play(
				  SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
			}
			return result;
		}
		return false;
	}
	
	@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
		tick();
		if (isMouseOver(mouseX, mouseY)) {
			if (!shouldSafeGuard())
				return onPress(this, button);
			if (dragging)
				return true;
			if (button == 0) {
				dragging = true;
				confirming = false;
				dragOffset = 0;
				dragAnchor = mouseX - x;
				getScreen().addOverlay(overlay, this, 20);
			}
			return true;
		}
		return false;
	}
	
	protected AbstractConfigScreen getScreen() {
		return entry != null ? entry.getScreen() : null;
	}
	
	@Override public boolean overlayMouseDragged(
	  Rectangle area, double mouseX, double mouseY, int button, double dragX, double dragY
	) {
		return IOverlayRenderer.super.overlayMouseDragged(area, mouseX, mouseY, button, dragX, dragY);
	}
	
	@Override public boolean mouseDragged(
	  double mouseX, double mouseY, int button, double dragX, double dragY
	) {
		if (!dragging) return false;
		dragOffset = (int) round(mouseX - x - dragAnchor);
		if (Minecraft.getInstance().font.isBidirectional())
			dragOffset = Mth.clamp(dragOffset, 0, 40 + width);
		else dragOffset = Mth.clamp(dragOffset, -40 - width, 0);
		return true;
	}
	
	@Override public void endDrag(double mouseX, double mouseY, int button) {
		dragging = false;
		if (abs(dragOffset) >= 40 + width)
			onPress(this, button);
		dragOffset = 0;
	}
	
	@Override public boolean renderOverlay(
	  PoseStack mStack, Rectangle area, int mouseX, int mouseY, float delta
	) {
		if (!dragging && !confirming) return false;
		getScreen().removeTooltips(area);
		RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
		RenderSystem.enableBlend();
		fill(mStack, area.x, area.y, area.getMaxX(), area.getMaxY(), 0x80FF4242);
		fill(mStack, area.x + 2, area.y + 2, area.getMaxX() - 2, area.getMaxY() - 2, 0xFFFF6464);
		if (dragging) {
			defaultIcon.renderStretch(mStack, x, y, width, height, 0);
			if (Minecraft.getInstance().font.isBidirectional()) {
				Buttons.CONFIRM_DRAG_RIGHT.renderStretch(mStack, x + width, y, 40, 20);
				defaultIcon.renderStretch(mStack, x + width + 40, y, width, height, 0);
				if (abs(dragOffset) >= width + 40)
					fill(mStack, x + width, y, x + 2 * width + 40, y + height, 0x64FF4242);
			} else {
				Buttons.CONFIRM_DRAG_LEFT.renderStretch(mStack, x - 40, y, 40, 20);
				defaultIcon.renderStretch(mStack, x - width - 40, y, width, height, 0);
				if (abs(dragOffset) >= width + 40)
					fill(mStack, x - 40, y, x + width, y + height, 0x64FF4242);
			}
		}
		defaultIcon.renderStretch(mStack, x + dragOffset, y, width, height, 2);
		if (confirming)
			fill(mStack, x + dragOffset, y, x + width, y + height, 0x64FF4242);
		return true;
	}
	
	@Override public boolean overlayEscape() {
		if (confirming) {
			confirming = false;
			return true;
		}
		return IOverlayRenderer.super.overlayEscape();
	}
	
	@Override public boolean changeFocus(boolean focus) {
		confirming = false;
		return super.changeFocus(focus);
	}
	
	@Override public void setFocused(boolean focused) {
		super.setFocused(focused);
	}
	
	@Override public List<Component> getTooltip() {
		if (isGroup()) return isRestore()? restoreTooltipGroup : resetTooltipGroup;
		return isRestore()? restoreTooltip : resetTooltip;
	}
	
	public Optional<List<Component>> getTooltip(int mouseX, int mouseY) {
		return isMouseOver(mouseX, mouseY)? Optional.of(getTooltip()) : Optional.empty();
	}
}
