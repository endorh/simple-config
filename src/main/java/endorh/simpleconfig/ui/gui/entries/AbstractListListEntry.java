package endorh.simpleconfig.ui.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.ui.api.EntryError;
import endorh.simpleconfig.ui.api.INavigableTarget;
import endorh.simpleconfig.ui.gui.entries.AbstractListListEntry.AbstractListCell;
import endorh.simpleconfig.ui.gui.widget.DynamicEntryListWidget;
import endorh.simpleconfig.ui.impl.ISeekableComponent;
import endorh.simpleconfig.ui.math.Rectangle;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@OnlyIn(value = Dist.CLIENT)
public abstract class AbstractListListEntry<T, C extends AbstractListCell<T, C, SELF>, SELF extends AbstractListListEntry<T, C, SELF>>
  extends BaseListEntry<T, C, SELF> {
	protected BiFunction<Integer, T, Optional<ITextComponent>> cellErrorSupplier = (i, t) -> Optional.empty();
	protected Function<List<T>, @Nullable List<Optional<ITextComponent>>> multiCellErrorSupplier = l -> null;
	protected @Nullable List<Optional<ITextComponent>> multiCellErrors = null;
	
	@Internal public AbstractListListEntry(
	  ITextComponent fieldName, List<T> value, Function<SELF, C> createNewCell
	) {
		super(fieldName, createNewCell);
		setOriginal(value);
		setValue(value);
		setDisplayedValue(value);
	}
	
	public BiFunction<Integer, T, Optional<ITextComponent>> getCellErrorSupplier() {
		return this.cellErrorSupplier;
	}
	
	public void setCellErrorSupplier(BiFunction<Integer, T, Optional<ITextComponent>> cellErrorSupplier) {
		this.cellErrorSupplier = cellErrorSupplier;
		final List<T> value = getValue();
		setValue(value);
		setDisplayedValue(value);
	}
	
	public Function<List<T>, List<Optional<ITextComponent>>> getMultiCellErrorSupplier() {
		return multiCellErrorSupplier;
	}
	
	public void setMultiCellErrorSupplier(Function<List<T>, @Nullable List<Optional<ITextComponent>>> multiCellErrorSupplier) {
		this.multiCellErrorSupplier = multiCellErrorSupplier;
		final List<T> value = getValue();
		setValue(value);
		setDisplayedValue(value);
	}
	
	@Override public void tick() {
		updateValue(false);
		multiCellErrors = multiCellErrorSupplier.apply(getValue());
		super.tick();
	}
	
	@Override protected C createCellWithValue(T value) {
		final C cell = cellFactory.apply(self());
		cell.setValue(value);
		cell.setOriginal(value);
		return cell;
	}
	
	@Internal
	public static abstract class AbstractListCell<V, Self extends AbstractListCell<V, Self, ListEntry>, ListEntry extends AbstractListListEntry<V, Self, ListEntry>>
	  extends BaseListCell<V> {
		private final ListEntry listEntry;
		protected boolean isFocusedMatch = false;
		protected String matchedText = null;
		protected Rectangle rowArea = new Rectangle();
		private INavigableTarget lastSelectedSubTarget;
		private int index = -1;
		
		public AbstractListCell(ListEntry listEntry) {
			this.listEntry = listEntry;
			this.setErrorSupplier(() -> Optional.ofNullable(listEntry.cellErrorSupplier)
			  .flatMap(cellErrorFn -> cellErrorFn.apply(index, this.getValue())));
		}
		
		protected ListEntry getListEntry() {
			return listEntry;
		}
		
		@Override public List<ISeekableComponent> search(Pattern query) {
			matchedText = null;
			final String text = seekableText();
			if (!text.isEmpty()) {
				final Matcher m = query.matcher(text);
				while (m.find()) {
					if (!m.group().isEmpty()) {
						matchedText = m.group();
						break;
					}
				}
			}
			List<ISeekableComponent> matches =
			  seekableComponents().stream().flatMap(c -> c.search(query).stream())
				 .collect(Collectors.toList());
			if (matchedText != null)
				matches.add(0, this);
			return matches;
		}
		
		@Override public Rectangle getSelectionArea() {
			return rowArea;
		}
		
		protected int getIndex() {
			return index;
		}
		
		@Override public final void onAdd() {
			super.onAdd();
			//noinspection SuspiciousMethodCalls
			index = listEntry.cells.indexOf(this);
			onAdd(index);
		}
		
		public void onAdd(int index) {}
		
		@Override public final void onMove() {
			super.onMove();
			//noinspection SuspiciousMethodCalls
			index = listEntry.cells.indexOf(this);
			onMove(index);
		}
		
		public void onMove(int index) {}
		
		@Override public Rectangle getNavigableArea() {
			return rowArea;
		}
		
		@Override public @Nullable INavigableTarget getLastSelectedNavigableSubTarget() {
			return lastSelectedSubTarget;
		}
		
		@Override public void setLastSelectedNavigableSubTarget(@Nullable INavigableTarget target) {
			lastSelectedSubTarget = target;
		}
		
		@Override protected List<EntryError> computeErrors() {
			List<EntryError> errors = super.computeErrors();
			List<Optional<ITextComponent>> multi = listEntry.multiCellErrors;
			if (multi != null && index >= 0 && index < multi.size())
				multi.get(index).ifPresent(e -> errors.add(EntryError.of(e, this)));
			return errors;
		}
		
		@Override public void renderCell(
		  MatrixStack mStack, int index, int x, int y, int cellWidth, int cellHeight, int mouseX,
		  int mouseY, boolean isSelected, float delta
		) {
			super.renderCell(mStack, index, x, y, cellWidth, cellHeight, mouseX, mouseY, isSelected, delta);
			ListEntry listEntry = getListEntry();
			DynamicEntryListWidget<?> entryList = listEntry.getEntryList();
			rowArea.setBounds(entryList.left, cellArea.y, entryList.right - entryList.left, cellArea.height);
		}
		
		protected String seekableText() {
			final V value = getValue();
			return value != null? value.toString() : "";
		}
		
		protected List<ISeekableComponent> seekableComponents() {
			return Lists.newLinkedList();
		}
		
		@Override public boolean isFocusedMatch() {
			return isFocusedMatch;
		}
		
		@Override public void setFocusedMatch(boolean isFocusedMatch) {
			this.isFocusedMatch = isFocusedMatch;
			
		}
		
		@Override public @Nullable INavigableTarget getNavigableParent() {
			return getListEntry();
		}
		
		@Override public boolean handleNavigationKey(int keyCode, int scanCode, int modifiers) {
			final ListEntry listEntry = getListEntry();
			if (listEntry.isEditable() && Screen.hasAltDown()) {
				final IGuiEventListener listener = listEntry.getFocused();
				//noinspection SuspiciousMethodCalls
				if (listener instanceof BaseListCell && listEntry.cells.contains(listener)) {
					//noinspection SuspiciousMethodCalls
					int index = listEntry.cells.indexOf(listener);
					if (Screen.hasControlDown()) { // Move
						if ( keyCode == GLFW.GLFW_KEY_DOWN &&index < listEntry.cells.size() - 1) {
							listEntry.moveTransparently(index, index + 1);
							((BaseListCell<?>) listener).navigate();
							return true;
						} else if (keyCode == GLFW.GLFW_KEY_UP && index > 0) {
							listEntry.moveTransparently(index, index - 1);
							((BaseListCell<?>) listener).navigate();
							return true;
						}
					}
					if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_INSERT) {
						listEntry.addTransparently(index + 1);
						listEntry.cells.get(index + 1).navigate();
						return true;
					} else if (index != -1 && (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE)) {
						listEntry.removeTransparently(index);
						if (!listEntry.cells.isEmpty())
							listEntry.cells.get(
							  MathHelper.clamp(keyCode == GLFW.GLFW_KEY_BACKSPACE? index - 1 : index, 0, listEntry.cells.size() - 1)).navigate();
						return true;
					}
				}
			}
			return super.handleNavigationKey(keyCode, scanCode, modifiers);
		}
	}
}

