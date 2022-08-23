package endorh.simpleconfig.ui.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import endorh.simpleconfig.ui.api.IDialogCapableScreen;
import endorh.simpleconfig.ui.api.IModalInputProcessor;
import endorh.simpleconfig.ui.api.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class AbstractDialogScreen extends Screen implements IDialogCapableScreen {
	private final SortedDialogCollection dialogs = new SortedDialogCollection();
	private final SortedOverlayCollection overlays = new SortedOverlayCollection();
	private final List<Tooltip> tooltips = Lists.newArrayList();
	private IModalInputProcessor modalInputProcessor = null;
	
	protected AbstractDialogScreen(Component title) {
		super(title);
	}
	
	@Override public final void render(@NotNull PoseStack mStack, int mouseX, int mouseY, float delta) {
		boolean hasDialog = hasDialogs();
		boolean suppressHover = hasDialog || shouldOverlaysSuppressHover(mouseX, mouseY);
		int mX = suppressHover? -1 : mouseX, mY = suppressHover? -1 : mouseY;
		super.render(mStack, mX, mY, delta);
		renderScreen(mStack, mX, mY, delta);
		renderOverlays(mStack, hasDialog? mX : mouseX, hasDialog? mY : mouseY, delta);
		if (hasDialog) tooltips.clear();
		renderDialogs(mStack, mouseX, mouseY, delta);
		renderTooltips(mStack, mouseX, mouseY, delta);
	}
	
	public void renderScreen(@NotNull PoseStack mStack, int mouseX, int mouseY, float delta) {}
	
	protected void renderTooltips(@NotNull PoseStack mStack, int mouseX, int mouseY, float delta) {
		for (Tooltip tooltip: tooltips) {
			int ty = tooltip.getY();
			if (ty <= 24) ty += 16;
			renderTooltip(mStack, tooltip.getText(), tooltip.getX(), ty);
		}
		tooltips.clear();
	}
	
	// Event hooks
	
	@MustBeInvokedByOverriders @Override public void tick() {
		tickDialogs();
		screenTick();
	}
	
	public void screenTick() {}
	
	@MustBeInvokedByOverriders
	@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (handleModalKeyPressed(keyCode, scanCode, modifiers)) return true;
		if (handleDialogsKeyPressed(keyCode, scanCode, modifiers)) return true;
		if (screenKeyPressed(keyCode, scanCode, modifiers)) return true;
		return super.keyPressed(keyCode, scanCode, modifiers);
	}
	
	protected boolean screenKeyPressed(int keyCode, int scanCode, int modifiers) {
		return false;
	}
	
	@MustBeInvokedByOverriders
	@Override public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		if (handleModalKeyReleased(keyCode, scanCode, modifiers)) return true;
		if (handleDialogsKeyReleased(keyCode, scanCode, modifiers)) return true;
		if (screenKeyReleased(keyCode, scanCode, modifiers)) return true;
		return super.keyReleased(keyCode, scanCode, modifiers);
	}
	
	protected boolean screenKeyReleased(int keyCode, int scanCode, int modifiers) {
		return false;
	}
	
	@MustBeInvokedByOverriders
	@Override public boolean charTyped(char codePoint, int modifiers) {
		if (handleModalCharTyped(codePoint, modifiers)) return true;
		if (handleDialogsCharTyped(codePoint, modifiers)) return true;
		if (screenCharTyped(codePoint, modifiers)) return true;
		return super.charTyped(codePoint, modifiers);
	}
	
	protected boolean screenCharTyped(char codePoint, int modifiers) {
		return false;
	}
	
	@MustBeInvokedByOverriders
	@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (handleModalMouseClicked(mouseX, mouseY, button)) return true;
		if (handleDialogsMouseClicked(mouseX, mouseY, button)
		    || handleOverlaysMouseClicked(mouseX, mouseY, button)
		    || screenMouseClicked(mouseX, mouseY, button)
		) return true;
		return super.mouseClicked(mouseX, mouseY, button);
	}
	
	protected boolean screenMouseClicked(double mouseX, double mouseY, int button) {
		return false;
	}
	
	@MustBeInvokedByOverriders
	@Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (handleModalMouseReleased(mouseX, mouseY, button)) return true;
		if (handleOverlaysMouseReleased(mouseX, mouseY, button)) return true;
		if (handleDialogsMouseReleased(mouseX, mouseY, button)) return true;
		if (screenMouseReleased(mouseX, mouseY, button)) return true;
		return super.mouseReleased(mouseX, mouseY, button);
	}
	
	protected boolean screenMouseReleased(double mouseX, double mouseY, int button) {
		return false;
	}
	
	@MustBeInvokedByOverriders
	@Override public boolean mouseDragged(
	  double mouseX, double mouseY, int button, double dragX, double dragY
	) {
		if (handleDialogsMouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
		if (handleOverlaysMouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
		if (screenMouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}
	
	protected boolean screenMouseDragged(
	  double mouseX, double mouseY, int button, double dragX, double dragY
	) {
		return false;
	}
	
	@MustBeInvokedByOverriders
	@Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (handleModalMouseScrolled(mouseX, mouseY, delta)) return true;
		if (handleDialogsMouseScrolled(mouseX, mouseY, delta)) return true;
		if (handleOverlaysMouseScrolled(mouseX, mouseY, delta)) return true;
		if (screenMouseScrolled(mouseX, mouseY, delta)) return true;
		return super.mouseScrolled(mouseX, mouseY, delta);
	}
	
	protected boolean screenMouseScrolled(double mouseX, double mouseY, double delta) {
		return false;
	}
	
	@Override public List<Tooltip> getTooltips() {
		return tooltips;
	}
	@Override public SortedDialogCollection getDialogs() {
		return dialogs;
	}
	@Override public SortedOverlayCollection getSortedOverlays() {
		return overlays;
	}
	
	@Override public @Nullable IModalInputProcessor getModalInputProcessor() {
		return modalInputProcessor;
	}
	@Override public void setModalInputProcessor(IModalInputProcessor processor) {
		modalInputProcessor = processor;
	}
}
