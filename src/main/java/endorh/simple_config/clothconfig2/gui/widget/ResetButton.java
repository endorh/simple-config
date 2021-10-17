package endorh.simple_config.clothconfig2.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import endorh.simple_config.SimpleConfigMod;
import endorh.simple_config.SimpleConfigMod.ClientConfig.confirm;
import endorh.simple_config.clothconfig2.api.AbstractConfigEntry;
import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.IExpandable;
import endorh.simple_config.clothconfig2.api.ScissorsHandler;
import endorh.simple_config.clothconfig2.gui.AbstractConfigScreen;
import endorh.simple_config.clothconfig2.gui.IExtendedDragAwareGuiEventListener;
import endorh.simple_config.clothconfig2.gui.IOverlayCapableScreen.IOverlayRenderer;
import endorh.simple_config.clothconfig2.gui.entries.IEntryHoldingListEntry;
import endorh.simple_config.clothconfig2.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.Optional;

import static java.lang.Math.abs;
import static java.lang.Math.round;
import static net.minecraft.util.math.MathHelper.clamp;

public class ResetButton extends MultiFunctionImageButton
  implements IExtendedDragAwareGuiEventListener, IOverlayRenderer {
	protected WeakReference<AbstractConfigEntry<?>> entry;
	protected static ITextComponent[] resetTooltip = new ITextComponent[]{
	  new TranslationTextComponent("text.cloth-config.reset_value"),
	  new TranslationTextComponent("simple-config.ui.restore.alt")};
	protected static ITextComponent[] resetTooltipShift = new ITextComponent[]{
	  new TranslationTextComponent("text.cloth-config.reset_value"),
	  new TranslationTextComponent("simple-config.ui.restore.alt"),
	  new TranslationTextComponent("text.cloth-config.reset_value.shift")};
	protected static ITextComponent[] resetTooltipGroup = new ITextComponent[]{
	  new TranslationTextComponent("text.cloth-config.reset_value.group"),
	  new TranslationTextComponent("simple-config.ui.restore.alt")};
	protected static ITextComponent[] restoreTooltip = new ITextComponent[]{
	  new TranslationTextComponent("simple-config.ui.restore")};
	protected static ITextComponent[] restoreTooltipShift = new ITextComponent[]{
	  new TranslationTextComponent("simple-config.ui.restore"),
	  new TranslationTextComponent("simple-config.ui.restore.shift")};
	protected static ITextComponent[] restoreTooltipGroup = new ITextComponent[]{
	  new TranslationTextComponent("simple-config.ui.restore.group")};
	
	protected boolean shift = false;
	protected boolean alt = false;
	protected boolean confirming = false;
	protected boolean dragging = false;
	protected int dragOffset = 0;
	protected double dragAnchor = 0;
	protected Rectangle overlay = new Rectangle();
	
	public ResetButton(AbstractConfigEntry<?> entry) {
		super(
		  0, 0, 20, 20, 0, 128, new ResourceLocation(
			 SimpleConfigMod.MOD_ID, "textures/gui/cloth_config.png"),
		  256, 256, (w, b) -> false,
		  (button, matrixStack, mouseX, mouseY) -> {
			 
		  }, NarratorChatListener.EMPTY);
		this.entry = new WeakReference<>(entry);
		this.setActivePredicate(w -> shouldBeActive());
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
			u = getU();
		}
		super.render(mStack, mouseX, mouseY, delta);
	}
	
	protected boolean isRestore() {
		return alt;
	}
	
	protected boolean isGroup() {
		final AbstractConfigEntry<?> entry = this.entry.get();
		if (entry == null) return false;
		if (entry instanceof IEntryHoldingListEntry) {
			final AbstractConfigListEntry<?> held = ((IEntryHoldingListEntry) entry).getHeldEntry();
			if (held != null && (isRestore()? held.isRestorable() : held.isResettable()) && !shift)
				return false;
		}
		return entry instanceof IExpandable;
	}
	
	protected int getU() {
		return isRestore()? isGroup()? 60 : 40 : isGroup()? 20 : 0;
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
						this.playDownSound(Minecraft.getInstance().getSoundHandler());
						return true;
					} else if (button != 0 && onPress(this, 0)) {
						this.playDownSound(Minecraft.getInstance().getSoundHandler());
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
		final AbstractConfigEntry<?> entry = this.entry.get();
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
		final AbstractConfigEntry<?> e = entry.get();
		if (e != null) {
			final boolean result = reset(e, button);
			if (result) {
				Minecraft.getInstance().getSoundHandler().play(
				  SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
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
		final AbstractConfigEntry<?> entry = this.entry.get();
		if (entry != null)
			return entry.getConfigScreen();
		return null;
	}
	
	@Override public boolean mouseDragged(
	  double mouseX, double mouseY, int button, double dragX, double dragY
	) {
		if (!dragging) return false;
		dragOffset = (int) round(mouseX - x - dragAnchor);
		if (Minecraft.getInstance().fontRenderer.getBidiFlag())
			dragOffset = clamp(dragOffset, 0, 40 + width);
		else dragOffset = clamp(dragOffset, -40 - width, 0);
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
		// getScreen().addTooltip(Tooltip.of(new Point(mouseX, mouseY), getRese));
		Minecraft.getInstance().getTextureManager().bindTexture(texture);
		RenderSystem.color4f(1F, 1F, 1F, 1F);
		RenderSystem.enableBlend();
		fill(mStack, area.x, area.y, area.getMaxX(), area.getMaxY(), 0x80FF4242);
		fill(mStack, area.x + 2, area.y + 2, area.getMaxX() - 2, area.getMaxY() - 2, 0xFFFF6464);
		if (dragging) {
			blit(mStack, x, y, u, v, width, height);
			if (Minecraft.getInstance().fontRenderer.getBidiFlag()) {
				blit(mStack, x + width, y, 216, 148, 40, 20);
				blit(mStack, x + width + 40, y, u, v, width, height);
				if (abs(dragOffset) >= width + 40)
					fill(mStack, x + width, y, x + 2 * width + 40, y + height, 0x64FF4242);
			} else {
				blit(mStack, x - 40, y, 216, 128, 40, 20);
				blit(mStack, x - 40 - width, y, u, v, width, height);
				if (abs(dragOffset) >= width + 40)
					fill(mStack, x - 40, y, x + width, y + height, 0x64FF4242);
			}
		}
		blit(mStack, x + dragOffset, y, u, v + 2 * height, width, height);
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
		if (entry.get() instanceof IExpandable) return isRestore()? restoreTooltipShift : resetTooltipShift;
		return isRestore()? restoreTooltip : resetTooltip;
	}
	
	public Optional<ITextComponent[]> getTooltip(int mouseX, int mouseY) {
		return isMouseOver(mouseX, mouseY)? Optional.of(getTooltip()) : Optional.empty();
	}
}
