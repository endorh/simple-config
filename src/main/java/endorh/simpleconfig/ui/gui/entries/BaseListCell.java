package endorh.simpleconfig.ui.gui.entries;

import com.mojang.blaze3d.vertex.PoseStack;
import endorh.simpleconfig.api.ui.math.Rectangle;
import endorh.simpleconfig.ui.api.EntryError;
import endorh.simpleconfig.ui.api.IExtendedDragAwareNestedGuiEventHandler;
import endorh.simpleconfig.ui.api.INavigableTarget;
import endorh.simpleconfig.ui.gui.widget.ToggleAnimator;
import endorh.simpleconfig.ui.impl.ISeekableComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.Supplier;

import static java.lang.Math.min;

@OnlyIn(value = Dist.CLIENT)
public abstract class BaseListCell<T> extends AbstractContainerEventHandler
  implements IExtendedDragAwareNestedGuiEventHandler, ISeekableComponent, INavigableTarget {
	protected Supplier<Optional<Component>> errorSupplier;
	protected Pair<Integer, GuiEventListener> dragged = null;
	protected final Rectangle cellArea = new Rectangle();
	protected boolean isSelected = false;
	
	private boolean isEdited = false;
	private List<EntryError> errors = Collections.emptyList();
	
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
	
	public void tick() {
		errors = computeErrors();
		isEdited = computeIsEdited();
	}
	
	public List<EntryError> getErrors() {
		return errors;
	}
	
	protected List<EntryError> computeErrors() {
		List<EntryError> errors = new ArrayList<>();
		if (errorSupplier != null)
			errorSupplier.get().ifPresent(e -> errors.add(EntryError.of(e, this)));
		getErrorMessage().ifPresent(e -> errors.add(EntryError.of(e, this)));
		return errors;
	}
	
	public boolean hasError() {
		return !getErrors().isEmpty();
	}
	
	public void setErrorSupplier(Supplier<Optional<Component>> errorSupplier) {
		this.errorSupplier = errorSupplier;
	}
	
	public abstract Optional<Component> getErrorMessage();
	
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
	  PoseStack mStack, int index, int x, int y, int listY, int cellWidth, int cellHeight,
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
	  PoseStack mStack, int index, int x, int y, int cellWidth, int cellHeight,
	  int mouseX, int mouseY, boolean isSelected, float delta
	) {
		Component label = getLabel();
		if (label != Component.empty()) {
			Font font = Minecraft.getInstance().font;
			int textX = font.isBidirectional() ? x + cellWidth - font.width(label) : x;
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
	  PoseStack mStack, Component label, int textX, int index, int x, int y,
	  int cellWidth, int cellHeight, int mouseX, int mouseY, boolean isSelected, float delta
	) {
		Font font = Minecraft.getInstance().font;
		font.drawShadow(
		  mStack, label.getVisualOrderText(), textX,
		  (float) y + cellHeight / 2F - font.lineHeight / 2F, getPreferredTextColor());
	}
	
	public Component getLabel() {
		return Component.literal("•").withStyle(ChatFormatting.GRAY);
	}
	
	public void updateSelected(boolean isSelected) {
		this.isSelected = isSelected;
		if (!isSelected)
			setFocused(null);
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
		return isEdited;
	}
	
	protected boolean computeIsEdited() {
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
	
	@Override public Pair<Integer, GuiEventListener> getDragged() {
		return dragged;
	}
	
	@Override public void setDragged(Pair<Integer, GuiEventListener> dragged) {
		this.dragged = dragged;
	}
	
	public boolean areEqual(T left, T right) {
		return Objects.equals(left, right);
	}
}
