package endorh.simpleconfig.ui.gui.entries;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.ui.api.EntryError;
import endorh.simpleconfig.ui.api.IExtendedDragAwareNestedGuiEventHandler;
import endorh.simpleconfig.ui.api.INavigableTarget;
import endorh.simpleconfig.ui.gui.widget.ToggleAnimator;
import endorh.simpleconfig.ui.impl.ISeekableComponent;
import endorh.simpleconfig.ui.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FocusableGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static java.lang.Math.min;

@OnlyIn(value = Dist.CLIENT)
public abstract class BaseListCell<T> extends FocusableGui
  implements IExtendedDragAwareNestedGuiEventHandler, ISeekableComponent, INavigableTarget {
	protected Supplier<Optional<ITextComponent>> errorSupplier;
	protected Pair<Integer, IGuiEventListener> dragged = null;
	protected final Rectangle cellArea = new Rectangle();
	protected boolean isSelected = false;
	
	protected ToggleAnimator offsetAnimator = new ToggleAnimator();
	protected int lastListY = -1;
	
	protected long lastFocusHighlightTime;
	protected int focusHighlightLength;
	protected int focusHighlightColor;
	protected int errorHighlightColor = 0x80FF4242;
	protected int historyApplyColor = 0x804242FF;
	protected int historyErrorColor = 0x80FF4242;
	protected int historyInsertColor = 0x8042FF42;
	protected int historyRemoveColor = 0x80FF4242;
	
	public final int getPreferredTextColor() {
		return hasError() ? 0xFF5555 : 0xE0E0E0;
	}
	
	public List<EntryError> getErrors() {
		List<EntryError> errors = new ArrayList<>();
		if (errorSupplier != null)
			errorSupplier.get().ifPresent(e -> errors.add(EntryError.of(e, this)));
		getErrorMessage().ifPresent(e -> errors.add(EntryError.of(e, this)));
		return errors;
	}
	
	public boolean hasError() {
		return !getErrors().isEmpty();
	}
	
	public void setErrorSupplier(Supplier<Optional<ITextComponent>> errorSupplier) {
		this.errorSupplier = errorSupplier;
	}
	
	public abstract Optional<ITextComponent> getErrorMessage();
	
	public abstract int getCellHeight();
	
	@Override public void applyErrorHighlight() {
		applyFocusHighlight(errorHighlightColor);
	}
	
	@Override public void applyFocusHighlight(int color, int length) {
		lastFocusHighlightTime = System.currentTimeMillis();
		focusHighlightLength = length;
		focusHighlightColor = color;
	}
	
	public int getCellAreaOffset() {
		return 0;
	}
	
	public void render(
	  MatrixStack mStack, int index, int x, int y, int listY, int cellWidth, int cellHeight,
	  int mouseX, int mouseY, boolean isSelected, float delta
	) {
		if (listY >= 0 && listY != lastListY) {
			offsetAnimator.setOutputRange(lastListY != -1? offsetAnimator.getEaseOut() : listY, listY);
			offsetAnimator.resetTarget();
			lastListY = listY;
		}
		int offset = listY >= 0? (int) offsetAnimator.getEaseOut() - listY : 0;
		cellArea.setBounds(x, y + getCellAreaOffset(), cellWidth, cellHeight - 4);
		renderCell(
		  mStack, index, x, y + offset, cellWidth, cellHeight,
		  mouseX, mouseY, isSelected, delta);
	}
	
	public void renderCell(
	  MatrixStack mStack, int index, int x, int y, int cellWidth, int cellHeight,
	  int mouseX, int mouseY, boolean isSelected, float delta
	) {
		ITextComponent label = getLabel();
		if (label != StringTextComponent.EMPTY) {
			FontRenderer font = Minecraft.getInstance().fontRenderer;
			int textX = font.getBidiFlag() ? x + cellWidth - font.getStringPropertyWidth(label) : x;
			renderLabel(
			  mStack, label, textX, index, x, y, cellWidth, cellHeight,
			  mouseX, mouseY, isSelected, delta);
		}
		long time = System.currentTimeMillis();
		long t = time - lastFocusHighlightTime - focusHighlightLength;
		if (t < 1000L) {
			int color = focusHighlightColor;
			final int offset = getCellAreaOffset();
			fill(mStack, 16, y + offset, x + cellWidth, y + getCellHeight() + offset - 4,
			     color & 0xFFFFFF | (int) ((color >> 24 & 0xFF) * (min(1000, 1000 - t) / 1000D)) << 24);
		}
	}
	
	public void renderLabel(
	  MatrixStack mStack, ITextComponent label, int textX, int index, int x, int y,
	  int cellWidth, int cellHeight, int mouseX, int mouseY, boolean isSelected, float delta
	) {
		FontRenderer font = Minecraft.getInstance().fontRenderer;
		font.func_238407_a_(
		  mStack, label.func_241878_f(), textX,
		  (float) y + cellHeight / 2F - font.FONT_HEIGHT / 2F, getPreferredTextColor());
	}
	
	public ITextComponent getLabel() {
		return new StringTextComponent("•").mergeStyle(TextFormatting.GRAY);
	}
	
	public void updateSelected(boolean isSelected) {
		this.isSelected = isSelected;
		if (!isSelected)
			setListener(null);
	}
	
	@Override public Rectangle getRowArea() {
		return cellArea;
	}
	
	public Rectangle getSelectionArea() {
		return cellArea;
	}
	
	public boolean drawsLine(int mouseX, int mouseY) {
		return false;
	}
	
	public boolean isRequiresRestart() {
		return false;
	}
	
	public boolean isEdited() {
		return hasError();
	}
	
	public void onAdd() {}
	
	public void onDelete() {
		updateSelected(false);
	}
	
	public void onShown() {}
	
	public void onHidden() {
		lastListY = -1;
	}
	
	public void onMove() {}
	
	public void onDragged(int listY) {
		offsetAnimator.setOutputRange(listY, listY);
		lastListY = listY;
	}
	
	public abstract T getValue();
	protected abstract void doSetValue(T value);
	public void setValue(T value) {
		doSetValue(value);
		getErrorMessage();
	}
	
	public void setOriginal(T value) {}
	
	@Override public Pair<Integer, IGuiEventListener> getDragged() {
		return dragged;
	}
	
	@Override public void setDragged(Pair<Integer, IGuiEventListener> dragged) {
		this.dragged = dragged;
	}
	
	public boolean areEqual(T left, T right) {
		return Objects.equals(left, right);
	}
}
