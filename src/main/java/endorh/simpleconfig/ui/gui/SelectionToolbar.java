package endorh.simpleconfig.ui.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons.Buttons;
import endorh.simpleconfig.ui.api.AbstractConfigField;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SelectionToolbar extends AbstractContainerEventHandler implements NarratableEntry {
	private final List<GuiEventListener> children = new ArrayList<>();
	private final List<AbstractWidget> buttons = new ArrayList<>();
	
	public final MultiFunctionImageButton resetButton;
	public final MultiFunctionImageButton restoreButton;
	public final MultiFunctionImageButton acceptButton;
	
	public int x, y;
	protected int width, height;
	public boolean visible;
	
	public SelectionToolbar(AbstractConfigScreen screen, int x, int y) {
		this.x = x;
		this.y = y;
		resetButton = new MultiFunctionImageButton(
		  x, y, 20, 20, Buttons.RESET_GROUP, ButtonAction.of(
		  () -> screen.runAtomicTransparentAction(() -> screen.getSelectedEntries().stream()
		    .filter(AbstractConfigField::isResettable)
		    .forEach(AbstractConfigField::resetValue))
		).tooltip(Component.translatable("simpleconfig.ui.reset.selected"))
		  .active(() -> screen.getSelectedEntries().stream()
		    .anyMatch(AbstractConfigField::isResettable)));
		addButton(resetButton);
		restoreButton = new MultiFunctionImageButton(
		  x, y, 20, 20, Buttons.RESTORE_GROUP, ButtonAction.of(
		  () -> screen.runAtomicTransparentAction(() -> screen.getSelectedEntries().stream()
		    .filter(AbstractConfigField::isRestorable)
		    .forEach(AbstractConfigField::restoreValue))
		).tooltip(Component.translatable("simpleconfig.ui.restore.selected"))
		  .active(() -> screen.getSelectedEntries().stream()
		    .anyMatch(AbstractConfigField::isRestorable)));
		addButton(restoreButton);
		acceptButton = new MultiFunctionImageButton(
		  x, y, 20, 20, Buttons.MERGE_ACCEPT_GROUP, ButtonAction.of(
		  () -> screen.runAtomicTransparentAction(() -> screen.getSelectedEntries().stream()
		    .filter(AbstractConfigField::hasConflictingExternalDiff)
		    .forEach(AbstractConfigField::acceptExternalValue))
		).tooltip(Component.translatable("simpleconfig.ui.merge.accept.selected"))
		  .active(() -> screen.getSelectedEntries().stream()
		    .anyMatch(AbstractConfigField::hasConflictingExternalDiff)));
		addButton(acceptButton);
	}
	
	public void render(PoseStack mStack, int mouseX, int mouseY, float delta) {
		if (visible) {
			int xx = x;
			for (AbstractWidget button : buttons) {
				button.x = xx;
				button.y = y;
				xx += button.getWidth();
				button.render(mStack, mouseX, mouseY, delta);
			}
			height = 20;
			width = xx - x;
		}
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
	protected void addListener(GuiEventListener listener) {
		children.add(listener);
	}
	
	protected void addButton(AbstractWidget widget) {
		children.add(widget);
		buttons.add(widget);
	}
	
	@Override public @NotNull List<GuiEventListener> children() {
		return children;
	}
	
	@Override public @NotNull NarrationPriority narrationPriority() {
		return NarrationPriority.NONE;
	}
	@Override public void updateNarration(@NotNull NarrationElementOutput out) {}
}
