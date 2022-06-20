package endorh.simpleconfig.clothconfig2.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import endorh.simpleconfig.SimpleConfigMod.ClientConfig.confirm;
import endorh.simpleconfig.clothconfig2.api.AbstractConfigEntry;
import endorh.simpleconfig.clothconfig2.api.AbstractConfigListEntry;
import endorh.simpleconfig.clothconfig2.api.IExpandable;
import endorh.simpleconfig.clothconfig2.api.ScissorsHandler;
import endorh.simpleconfig.clothconfig2.gui.AbstractConfigScreen;
import endorh.simpleconfig.clothconfig2.gui.IExtendedDragAwareGuiEventListener;
import endorh.simpleconfig.clothconfig2.gui.IOverlayCapableScreen.IOverlayRenderer;
import endorh.simpleconfig.clothconfig2.gui.Icon;
import endorh.simpleconfig.clothconfig2.gui.SimpleConfigIcons;
import endorh.simpleconfig.clothconfig2.gui.entries.IEntryHoldingListEntry;
import endorh.simpleconfig.clothconfig2.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static java.lang.Math.abs;
import static java.lang.Math.round;

public class ResetButton extends MultiFunctionImageButton
  implements IExtendedDragAwareGuiEventListener, IOverlayRenderer {
	protected static ITextComponent[] resetTooltip = new ITextComponent[]{
	  new TranslationTextComponent("text.cloth-config.reset_value"),
	  new TranslationTextComponent("simpleconfig.ui.restore.alt")};
	protected static ITextComponent[] resetTooltipShift = new ITextComponent[]{
	  new TranslationTextComponent("text.cloth-config.reset_value"),
	  new TranslationTextComponent("simpleconfig.ui.restore.alt"),
	  new TranslationTextComponent("text.cloth-config.reset_value.shift")};
	protected static ITextComponent[] resetTooltipGroup = new ITextComponent[]{
	  new TranslationTextComponent("text.cloth-config.reset_value.group"),
	  new TranslationTextComponent("simpleconfig.ui.restore.alt")};
	protected static ITextComponent[] restoreTooltip = new ITextComponent[]{
	  new TranslationTextComponent("simpleconfig.ui.restore")};
	protected static ITextComponent[] restoreTooltipShift = new ITextComponent[]{
	  new TranslationTextComponent("simpleconfig.ui.restore"),
	  new TranslationTextComponent("simpleconfig.ui.restore.shift")};
	protected static ITextComponent[] restoreTooltipGroup = new ITextComponent[]{
	  new TranslationTextComponent("simpleconfig.ui.restore.group")};
	
	protected AbstractConfigEntry<?> entry;
	protected boolean shift = false;
	protected boolean alt = false;
	protected boolean confirming = false;
	protected boolean dragging = false;
	protected int dragOffset = 0;
	protected double dragAnchor = 0;
	protected Rectangle overlay = new Rectangle();
	
	public ResetButton(AbstractConfigEntry<?> entry) {
		super(
		  0, 0, 20, 20, SimpleConfigIcons.RESET, ButtonAction.of(b -> {}),
		  NarratorChatListener.NO_TITLE);
		this.entry = entry;
		defaultActivePredicate = this::shouldBeActive;
	}
	
	protected boolean shouldSafeGuard() {
		return isRestore() ? isGroup() ? confirm.group_restore : confirm.restore :
		       isGroup() ? confirm.group_reset : confirm.reset;
	}
	
	@Override public void render(@NotNull MatrixStack mStack, int mouseX, int mouseY, float delta) {
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
		if (!confirming && !dragging && !getScreen().hasDialogs()) {
			shift = Screen.hasShiftDown();
			alt = Screen.hasAltDown();
			defaultIcon = getIcon();
		}
		super.render(mStack, mouseX, mouseY, delta);
	}
	
	protected boolean isRestore() {
		return alt;
	}
	
	protected boolean isGroup() {
		if (entry == null) return false;
		if (entry instanceof IEntryHoldingListEntry) {
			final AbstractConfigListEntry<?> held = ((IEntryHoldingListEntry) entry).getHeldEntry();
			if (held != null && (isRestore()? held.isRestorable() : held.isResettable()) && !shift)
				return false;
		}
		return entry instanceof IExpandable;
	}
	
	protected Icon getIcon() {
		return isRestore()?
		       isGroup()? SimpleConfigIcons.RESTORE_GROUP : SimpleConfigIcons.RESTORE :
		       isGroup()? SimpleConfigIcons.RESET_GROUP : SimpleConfigIcons.RESET;
	}
	
	@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (this.active && this.visible) {
			if (keyCode != 257 && keyCode != 32 && keyCode != 335) { // !(Enter | Space | NumPadEnter)
				return false;
			} else {
				int button = Screen.hasControlDown() ? 2 : Screen.hasShiftDown() ? 1 : 0;
				if (!shouldSafeGuard() || confirming) {
					confirming = false;
					if (onPress(this, button)) {
						this.playDownSound(Minecraft.getInstance().getSoundManager());
						return true;
					} else if (button != 0 && onPress(this, 0)) {
						this.playDownSound(Minecraft.getInstance().getSoundManager());
						return true;
					}
					return false;
				}
				confirming = true;
				dragging = false;
				getScreen().claimRectangle(overlay, this, 20);
				return true;
			}
		}
		return false;
	}
	
	protected boolean shouldBeActive() {
		if (entry == null) return false;
		if (entry.getListParent() != null) // This only creates confusion
			return false;
		if (entry instanceof IEntryHoldingListEntry) {
			final AbstractConfigListEntry<?> held = ((IEntryHoldingListEntry) entry).getHeldEntry();
			if (held != null && (isRestore()? held.isRestorable() : held.isResettable()) && !shift)
				return true;
		}
		return isRestore()? entry.isRestorable() : entry.isResettable();
	}
	
	protected boolean reset(AbstractConfigEntry<?> entry, int button) {
		if (button == 0) {
			if (entry instanceof IEntryHoldingListEntry) {
				final AbstractConfigListEntry<?> heldEntry = ((IEntryHoldingListEntry) entry).getHeldEntry();
				if (heldEntry != null && !shift) {
					if (isRestore() && heldEntry.isRestorable()) {
						heldEntry.restoreValue();
						return true;
					} else if (!isRestore() && heldEntry.isResettable()) {
						heldEntry.resetValue();
						return true;
					}
				}
			}
			if (isRestore() && entry.isRestorable())
				entry.restoreValue();
			else if (!isRestore() && entry.isResettable())
				entry.resetValue();
		}
		return true;
	}
	
	public boolean onPress(Button widget, int button) {
		if (entry != null) {
			final boolean result = reset(entry, button);
			if (result) {
				Minecraft.getInstance().getSoundManager().play(
				  SimpleSound.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
				setFocused(false);
			}
			return result;
		}
		return false;
	}
	
	@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
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
				getScreen().claimRectangle(overlay, this, 20);
			}
			return true;
		}
		return false;
	}
	
	protected AbstractConfigScreen getScreen() {
		return entry != null ? entry.getConfigScreen() : null;
	}
	
	@Override public boolean mouseDragged(
	  double mouseX, double mouseY, int button, double dragX, double dragY
	) {
		if (!dragging) return false;
		dragOffset = (int) round(mouseX - x - dragAnchor);
		if (Minecraft.getInstance().font.isBidirectional())
			dragOffset = MathHelper.clamp(dragOffset, 0, 40 + width);
		else dragOffset = MathHelper.clamp(dragOffset, -40 - width, 0);
		return true;
	}
	
	@Override public void endDrag(double mouseX, double mouseY, int button) {
		dragging = false;
		if (abs(dragOffset) >= 40 + width)
			onPress(this, button);
		dragOffset = 0;
	}
	
	@Override public boolean renderOverlay(
	  MatrixStack mStack, Rectangle area, int mouseX, int mouseY, float delta
	) {
		if (!dragging && !confirming) return false;
		getScreen().removeTooltips(area);
		RenderSystem.color4f(1F, 1F, 1F, 1F);
		RenderSystem.enableBlend();
		fill(mStack, area.x, area.y, area.getMaxX(), area.getMaxY(), 0x80FF4242);
		fill(mStack, area.x + 2, area.y + 2, area.getMaxX() - 2, area.getMaxY() - 2, 0xFFFF6464);
		if (dragging) {
			defaultIcon.renderStretch(mStack, x, y, width, height, 0);
			if (Minecraft.getInstance().font.isBidirectional()) {
				blit(mStack, x + width, y, 216, 20, 40, 20);
				defaultIcon.renderStretch(mStack, x + width + 40, y, width, height, 0);
				if (abs(dragOffset) >= width + 40)
					fill(mStack, x + width, y, x + 2 * width + 40, y + height, 0x64FF4242);
			} else {
				blit(mStack, x - 40, y, 216, 0, 40, 20);
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
	
	public ITextComponent[] getTooltip() {
		if (isGroup()) return isRestore()? restoreTooltipGroup : resetTooltipGroup;
		if (entry instanceof IExpandable) return isRestore()? restoreTooltipShift : resetTooltipShift;
		return isRestore()? restoreTooltip : resetTooltip;
	}
	
	public Optional<ITextComponent[]> getTooltip(int mouseX, int mouseY) {
		return isMouseOver(mouseX, mouseY)? Optional.of(getTooltip()) : Optional.empty();
	}
}
