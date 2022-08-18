package endorh.simpleconfig.ui.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.ui.api.AbstractConfigEntry;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import endorh.simpleconfig.ui.icon.SimpleConfigIcons.Buttons;
import net.minecraft.client.gui.FocusableGui;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.text.TranslationTextComponent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SelectionToolbar extends FocusableGui {
	private final List<IGuiEventListener> children = new ArrayList<>();
	private final List<Widget> buttons = new ArrayList<>();
	
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
		    .filter(AbstractConfigEntry::isResettable)
		    .forEach(AbstractConfigEntry::resetValue))
		).tooltip(new TranslationTextComponent("simpleconfig.ui.reset.selected"))
		  .active(() -> screen.getSelectedEntries().stream()
		    .anyMatch(AbstractConfigEntry::isResettable)));
		addButton(resetButton);
		restoreButton = new MultiFunctionImageButton(
		  x, y, 20, 20, Buttons.RESTORE_GROUP, ButtonAction.of(
		  () -> screen.runAtomicTransparentAction(() -> screen.getSelectedEntries().stream()
		    .filter(AbstractConfigEntry::isRestorable)
		    .forEach(AbstractConfigEntry::restoreValue))
		).tooltip(new TranslationTextComponent("simpleconfig.ui.restore.selected"))
		  .active(() -> screen.getSelectedEntries().stream()
		    .anyMatch(AbstractConfigEntry::isRestorable)));
		addButton(restoreButton);
		acceptButton = new MultiFunctionImageButton(
		  x, y, 20, 20, Buttons.MERGE_ACCEPT_GROUP, ButtonAction.of(
		  () -> screen.runAtomicTransparentAction(() -> screen.getSelectedEntries()
			   .forEach(AbstractConfigEntry::isSelected))
		).tooltip(new TranslationTextComponent("simpleconfig.ui.merge.accept.selected"))
		  .active(() -> false));
		addButton(acceptButton);
	}
	
	public void render(MatrixStack mStack, int mouseX, int mouseY, float delta) {
		if (visible) {
			int xx = x;
			for (Widget button : buttons) {
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
	
	protected void addListener(IGuiEventListener listener) {
		children.add(listener);
	}
	
	protected void addButton(Widget widget) {
		children.add(widget);
		buttons.add(widget);
	}
	
	@Override public @NotNull List<IGuiEventListener> getEventListeners() {
		return children;
	}
}
