package endorh.simpleconfig.ui.gui.widget.treeview;

import com.google.common.collect.Lists;
import endorh.simpleconfig.ui.api.AbstractContainerEventHandlerEx;
import endorh.simpleconfig.ui.gui.widget.IPositionableRenderable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.max;

public abstract class ArrangeableTreeViewCaption<E extends ArrangeableTreeViewEntry<E>>
  extends AbstractContainerEventHandlerEx {
	private final ArrangeableTreeView<E> tree;
	protected List<GuiEventListener> listeners = new ArrayList<>();
	protected List<IPositionableRenderable> controls = new ArrayList<>();
	protected List<IPositionableRenderable> rightControls = new ArrayList<>();
	private Component title = Component.empty();
	private int titleColor = 0xE0E0E0E0;
	
	protected ArrangeableTreeViewCaption(ArrangeableTreeView<E> tree) {
		this.tree = tree;
	}
	
	public ArrangeableTreeView<E> getTree() {
		return tree;
	}
	
	protected void addControl(AbstractWidget control) {
		addControl(controls.size(), control);
	}
	
	protected void addControl(int pos, AbstractWidget control) {
		addControl(pos, IPositionableRenderable.wrap(control));
	}
	
	protected void addControl(IPositionableRenderable control) {
		addControl(controls.size(), control);
	}
	
	protected void addControl(int pos, IPositionableRenderable control) {
		controls.add(pos, control);
		listeners.add(pos, control);
	}
	
	protected void addRightControl(AbstractWidget control) {
		addRightControl(rightControls.size(), control);
	}
	
	protected void addRightControl(int pos, AbstractWidget control) {
		addControl(pos, IPositionableRenderable.wrap(control));
	}
	
	protected void addRightControl(IPositionableRenderable control) {
		addRightControl(rightControls.size(), control);
	}
	
	protected void addRightControl(int pos, IPositionableRenderable control) {
		rightControls.add(pos, control);
		listeners.add(pos + controls.size(), control);
	}
	
	public Component getTitle() {
		return title;
	}
	
	public void setTitle(Component title) {
		this.title = title;
	}
	
	public int getTitleColor() {
		return titleColor;
	}
	
	public void setTitleColor(int titleColor) {
		this.titleColor = titleColor;
	}

	@Nullable @Override public ComponentPath nextFocusPath(@NotNull FocusNavigationEvent e) {
		return super.nextFocusPath(e);
	}

	protected int getHeight() {
		return 24;
	}
	
	public void render(GuiGraphics gg, int mouseX, int mouseY, float delta) {
		ArrangeableTreeView<E> tree = getTree();
		int pad = tree.getPadding();
		int x = tree.getX() + pad, y = tree.getY() + pad;
		int height = getHeight();
		boolean focused = tree.isFocusedCaption();
		Font font = Minecraft.getInstance().font;
		for (IPositionableRenderable control: controls) {
			if (!focused || control != getFocused()) control.setFocused(false);
			control.setPosition(x, y);
			control.render(gg, mouseX, mouseY, delta);
			x += control.getWidth() + 2;
		}
		int minX = x;
		x = tree.getX() + tree.getWidth() - pad + 2;
		boolean skip = false;
		for (IPositionableRenderable control: Lists.reverse(rightControls)) {
			x = max(x - control.getWidth() - 2, minX);
			if (!skip) {
				if (!focused || control != getFocused()) control.setFocused(false);
				control.setPosition(x, y);
				control.render(gg, mouseX, mouseY, delta);
				if (x <= minX) skip = true;
			} else {
				control.setFocused(false);
				control.setPosition(0, 0, 0, 0);
			}
		}
		int maxX = x;
		if (maxX - minX > 16) {
			gg.drawWordWrap(
				font, getTitle(), minX + 4, y + (height - font.lineHeight) / 2,
				maxX - minX - 8, getTitleColor());
		}
	}
	
	@Override public @NotNull List<? extends GuiEventListener> children() {
		return listeners;
	}
}
