package endorh.simpleconfig.ui.gui.widget.treeview;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.ui.gui.widget.ScrollingContainerWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.ITextComponent;

import static java.lang.Math.max;

public class ArrangeableTreeViewScroller extends ScrollingContainerWidget {
	private final ArrangeableTreeView<?> tree;
	
	public ArrangeableTreeViewScroller(ArrangeableTreeView<?> tree) {
		super(tree.area.copy());
		this.tree = tree;
		listeners.add(tree.getRoot());
	}
	
	@Override public void renderInner(
	  MatrixStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		ArrangeableTreeView<?> tree = getTree();
		ArrangeableTreeViewEntry<?> root = tree.getRoot();
		if (root.getSubEntries().isEmpty()) {
			ITextComponent placeHolder = tree.getPlaceHolder();
			if (placeHolder != null) {
				FontRenderer font = Minecraft.getInstance().fontRenderer;
				ArrangeableTreeViewCaption<?> caption = tree.getCaption();
				int maxWidth = tree.getWidth() - 8;
				int width = font.getStringPropertyWidth(placeHolder);
				int textX = tree.area.getCenterX() - width / 2;
				if (textX < tree.getX() + 4) textX = tree.getX() + 4;
				int textY = tree.area.getCenterY() - font.FONT_HEIGHT / 2;
				if (caption != null) textY += caption.getHeight() / 2;
				font.func_238418_a_(placeHolder, textX, textY, maxWidth, 0xE0E0E0E0);
			}
		} else root.render(mStack, x, y, w, mouseX, mouseY, delta);
	}
	
	@Override public int getInnerHeight() {
		ArrangeableTreeView<?> tree = getTree();
		ArrangeableTreeViewCaption<?> caption = tree.getCaption();
		int captionHeight = caption != null? caption.getHeight() + tree.getCaptionSeparation() : 0;
		int padding = tree.getPadding();
		return max(tree.getHeight() - captionHeight - padding * 2, tree.getInnerHeight());
	}
	
	public ArrangeableTreeView<?> getTree() {
		return tree;
	}
}
