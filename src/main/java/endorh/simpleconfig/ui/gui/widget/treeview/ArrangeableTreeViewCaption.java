package endorh.simpleconfig.ui.gui.widget.treeview;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.ui.gui.widget.IPositionableRenderable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FocusableGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.max;

public abstract class ArrangeableTreeViewCaption<E extends ArrangeableTreeViewEntry<E>>
  extends FocusableGui {
	private final ArrangeableTreeView<E> tree;
	protected List<IGuiEventListener> listeners = new ArrayList<>();
	protected List<IPositionableRenderable> controls = new ArrayList<>();
	protected List<IPositionableRenderable> rightControls = new ArrayList<>();
	private ITextComponent title = StringTextComponent.EMPTY;
	private int titleColor = 0xE0E0E0E0;
	
	protected ArrangeableTreeViewCaption(ArrangeableTreeView<E> tree) {
		this.tree = tree;
	}
	
	public ArrangeableTreeView<E> getTree() {
		return tree;
	}
	
	protected void addControl(Widget control) {
		addControl(controls.size(), control);
	}
	
	protected void addControl(int pos, Widget control) {
		addControl(pos, IPositionableRenderable.wrap(control));
	}
	
	protected void addControl(IPositionableRenderable control) {
		addControl(controls.size(), control);
	}
	
	protected void addControl(int pos, IPositionableRenderable control) {
		controls.add(pos, control);
		listeners.add(pos, control);
	}
	
	protected void addRightControl(Widget control) {
		addRightControl(rightControls.size(), control);
	}
	
	protected void addRightControl(int pos, Widget control) {
		addControl(pos, IPositionableRenderable.wrap(control));
	}
	
	protected void addRightControl(IPositionableRenderable control) {
		addRightControl(rightControls.size(), control);
	}
	
	protected void addRightControl(int pos, IPositionableRenderable control) {
		rightControls.add(pos, control);
		listeners.add(pos + controls.size(), control);
	}
	
	public ITextComponent getTitle() {
		return title;
	}
	
	public void setTitle(ITextComponent title) {
		this.title = title;
	}
	
	public int getTitleColor() {
		return titleColor;
	}
	
	public void setTitleColor(int titleColor) {
		this.titleColor = titleColor;
	}
	
	protected int getHeight() {
		return 24;
	}
	
	public void render(MatrixStack mStack, int mouseX, int mouseY, float delta) {
		ArrangeableTreeView<E> tree = getTree();
		int pad = tree.getPadding();
		int x = tree.getX() + pad, y = tree.getY() + pad;
		int height = getHeight();
		boolean focused = tree.isFocusedCaption();
		FontRenderer font = Minecraft.getInstance().font;
		for (IPositionableRenderable control: controls) {
			if (!focused || control != getFocused()) control.setFocused(false);
			control.setPosition(x, y);
			control.render(mStack, mouseX, mouseY, delta);
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
				control.render(mStack, mouseX, mouseY, delta);
				if (x <= minX) skip = true;
			} else {
				control.setFocused(false);
				control.setPosition(0, 0, 0, 0);
			}
		}
		int maxX = x;
		if (maxX - minX > 16) {
			font.drawWordWrap(
			  getTitle(), minX + 4, y + (height - font.lineHeight) / 2,
			  maxX - minX - 8, getTitleColor());
		}
	}
	
	@Override public @NotNull List<? extends IGuiEventListener> children() {
		return listeners;
	}
}
