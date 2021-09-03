package endorh.simple_config.clothconfig2.gui.entries;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simple_config.clothconfig2.gui.IExtendedDragAwareNestedGuiEventHandler;
import endorh.simple_config.clothconfig2.gui.widget.DynamicEntryListWidget.INavigableTarget;
import endorh.simple_config.clothconfig2.impl.ISeekableComponent;
import net.minecraft.client.gui.FocusableGui;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;
import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public abstract class BaseListCell<T> extends FocusableGui
  implements IExtendedDragAwareNestedGuiEventHandler, ISeekableComponent, INavigableTarget {
	protected Supplier<Optional<ITextComponent>> errorSupplier;
	protected Pair<Integer, IGuiEventListener> dragged = null;
	protected boolean isSelected = false;
	protected long lastHistoryTime = 0;
	protected int historyApplyColor = 0x804242FF;
	
	public final int getPreferredTextColor() {
		return this.getConfigError().isPresent() ? 0xFF5555 : 0xE0E0E0;
	}
	
	public final Optional<ITextComponent> getConfigError() {
		if (this.errorSupplier != null && this.errorSupplier.get().isPresent())
			return this.errorSupplier.get();
		return this.getError();
	}
	
	public void setErrorSupplier(Supplier<Optional<ITextComponent>> errorSupplier) {
		this.errorSupplier = errorSupplier;
	}
	
	public abstract Optional<ITextComponent> getError();
	
	public abstract int getCellHeight();
	
	public int getCellDecorationOffset() {
		return 0;
	}
	
	public void render(
	  MatrixStack mStack, int index, int y, int x, int entryWidth, int entryHeight,
	  int mouseX, int mouseY, boolean isSelected, float delta
	) {
		final long t = System.currentTimeMillis() - lastHistoryTime;
		if (t < 1000) {
			int color = historyApplyColor;
			final int offset = getCellDecorationOffset();
				fill(mStack, 16, y + offset, x + entryWidth, y + getCellHeight() + offset - 4,
			     color & 0xFFFFFF | (int) ((color >> 24 & 0xFF) * ((1000 - t) / 1000D)) << 24);
		}
	}
	
	public void updateSelected(boolean isSelected) {
		this.isSelected = isSelected;
		if (!isSelected)
			setListener(null);
	}
	
	public boolean drawsLine(int mouseX, int mouseY) {
		return false;
	}
	
	public boolean isRequiresRestart() {
		return false;
	}
	
	public boolean isEdited() {
		return this.getConfigError().isPresent();
	}
	
	public void onAdd() {}
	
	public void onDelete() {
		updateSelected(false);
	}
	
	public abstract T getValue();
	protected abstract void doSetValue(T value);
	public void setValue(T value) {
		doSetValue(value);
		getError();
	}
	
	public void setOriginal(T value) {}
	
	@Override public Pair<Integer, IGuiEventListener> getDragged() {
		return dragged;
	}
	@Override public void setDragged(Pair<Integer, IGuiEventListener> dragged) {
		this.dragged = dragged;
	}
}

