package endorh.simpleconfig.ui.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import endorh.simpleconfig.SimpleConfigMod.ClientConfig.confirm;
import endorh.simpleconfig.ui.api.AbstractConfigEntry;
import endorh.simpleconfig.ui.api.IEntryHolder;
import endorh.simpleconfig.ui.api.ScissorsHandler;
import endorh.simpleconfig.ui.gui.AbstractConfigScreen;
import endorh.simpleconfig.ui.gui.IExtendedDragAwareGuiEventListener;
import endorh.simpleconfig.ui.gui.IOverlayCapableScreen.IOverlayRenderer;
import endorh.simpleconfig.ui.gui.Icon;
import endorh.simpleconfig.ui.gui.SimpleConfigIcons.Buttons;
import endorh.simpleconfig.ui.math.Rectangle;
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
	  new TranslationTextComponent("simpleconfig.ui.reset"),
	  new TranslationTextComponent("simpleconfig.ui.restore.alt")};
	protected static ITextComponent[] resetTooltipGroup = new ITextComponent[]{
	  new TranslationTextComponent("simpleconfig.ui.reset.group"),
	  new TranslationTextComponent("simpleconfig.ui.restore.alt")};
	protected static ITextComponent[] restoreTooltip = new ITextComponent[]{
	  new TranslationTextComponent("simpleconfig.ui.restore")};
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
		  0, 0, 20, 20, Buttons.RESET, ButtonAction.of(b -> {}),
		  NarratorChatListener.EMPTY);
		this.entry = entry;
		defaultActivePredicate = this::shouldBeActive;
	}
	
	protected boolean shouldSafeGuard() {
		return isRestore() ? isGroup() ? confirm.group_restore : confirm.restore :
		       isGroup() ? confirm.group_reset : confirm.reset;
	}
	
	@Override public void render(@NotNull MatrixStack mStack, int mouseX, int mouseY, float delta) {
		if (dragging) {
			if (Minecraft.getInstance().fontRenderer.getBidiFlag())
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
		if (isFocused() && !active && entry != null) {
			entry.changeFocus(false);
			setFocused(false);
		}
	}
	
	protected boolean isRestore() {
		return alt;
	}
	
	protected boolean isGroup() {
		if (!entry.isGroup()) return false;
		if (!(entry instanceof IEntryHolder)) return true;
		final IEntryHolder entryHolder = (IEntryHolder) entry;
		return shift ||
		       (isRestore() ? entryHolder.getSingleRestorableEntry()
		                    : entryHolder.getSingleResettableEntry()) == null;
	}
	
	protected Icon getIcon() {
		return isRestore()?
		       isGroup()? Buttons.RESTORE_GROUP : Buttons.RESTORE :
		       isGroup()? Buttons.RESET_GROUP : Buttons.RESET;
	}
	
	@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (this.active && this.visible) {
			if (keyCode != 257 && keyCode != 32 && keyCode != 335) { // !(Enter | Space | NumPadEnter)
				return false;
			} else {
				return activate(Screen.hasControlDown() ? 2 : Screen.hasShiftDown() ? 1 : 0);
			}
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
				playDownSound(Minecraft.getInstance().getSoundHandler());
				return true;
			} else if (button != 0 && onPress(this, 0)) {
				playDownSound(Minecraft.getInstance().getSoundHandler());
				return true;
			}
			return false;
		}
		playDownSound(Minecraft.getInstance().getSoundHandler());
		confirming = true;
		dragging = false;
		getScreen().claimRectangle(overlay, this, 20);
		return true;
	}
	
	protected boolean shouldBeActive() {
		if (entry == null) return false;
		if (entry.isSubEntry()) return false;
		return isGroup()? isRestore()? entry.canRestoreGroup() : entry.canResetGroup()
		                : isRestore()? entry.isRestorable() : entry.isResettable();
	}
	
	protected boolean reset(AbstractConfigEntry<?> entry, int button) {
		if (button == 0) {
			if (entry instanceof IEntryHolder) {
				final IEntryHolder entryHolder = (IEntryHolder) entry;
				final AbstractConfigEntry<?> singleEntry = entryHolder.getSingleResettableEntry();
				if (singleEntry != null && !shift) {
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
			if (isRestore() && entry.isRestorable()) {
				entry.restoreValue();
				return true;
			} else if (!isRestore() && entry.isResettable()) {
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
				Minecraft.getInstance().getSoundHandler().play(
				  SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
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
		if (Minecraft.getInstance().fontRenderer.getBidiFlag())
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
			if (Minecraft.getInstance().fontRenderer.getBidiFlag()) {
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
	
	public ITextComponent[] getTooltip() {
		if (isGroup()) return isRestore()? restoreTooltipGroup : resetTooltipGroup;
		return isRestore()? restoreTooltip : resetTooltip;
	}
	
	public Optional<ITextComponent[]> getTooltip(int mouseX, int mouseY) {
		return isMouseOver(mouseX, mouseY)? Optional.of(getTooltip()) : Optional.empty();
	}
}
