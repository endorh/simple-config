package endorh.simple_config.clothconfig2.gui.entries;

import com.google.common.collect.Lists;
import endorh.simple_config.clothconfig2.gui.entries.AbstractListListEntry.AbstractListCell;
import endorh.simple_config.clothconfig2.impl.ISeekableComponent;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.minecraft.util.math.MathHelper.clamp;

@OnlyIn(value = Dist.CLIENT)
public abstract class AbstractListListEntry<T, C extends AbstractListCell<T, C, SELF>, SELF extends AbstractListListEntry<T, C, SELF>>
  extends BaseListEntry<T, C, SELF> {
	protected Function<T, Optional<ITextComponent>> cellErrorSupplier = t -> Optional.empty();
	
	@Internal public AbstractListListEntry(
	  ITextComponent fieldName, List<T> value, Function<SELF, C> createNewCell
	) {
		super(fieldName, createNewCell);
		setOriginal(value);
		for (T f : value)
			this.cells.add(createCellWithValue(f));
		this.widgets.addAll(this.cells);
	}
	
	public Function<T, Optional<ITextComponent>> getCellErrorSupplier() {
		return this.cellErrorSupplier;
	}
	
	public void setCellErrorSupplier(Function<T, Optional<ITextComponent>> cellErrorSupplier) {
		this.cellErrorSupplier = cellErrorSupplier;
		setValue(getValue());
	}
	
	@Override public void setValue(List<T> value) {
		final boolean original = suppressRecords;
		if (!original) suppressRecords = true;
		for (int i = 0; i < cells.size() && i < value.size(); i++)
			cells.get(i).setValue(value.get(i));
		while (cells.size() > value.size())
			remove(cells.size() - 1);
		while (cells.size() < value.size())
			add(value.get(cells.size()));
		suppressRecords = original;
	}
	
	@Override protected C createCellWithValue(T value) {
		final C cell = cellFactory.apply(self());
		cell.setValue(value);
		cell.setOriginal(value);
		return cell;
	}
	
	@Override public void setOriginal(List<T> value) {
		this.original = new ArrayList<>(value);
	}
	
	@Internal
	public static abstract class AbstractListCell<V, Self extends AbstractListCell<V, Self, ListEntry>, ListEntry extends AbstractListListEntry<V, Self, ListEntry>>
	  extends BaseListCell<V> {
		private final WeakReference<ListEntry> listEntry;
		protected boolean isFocusedMatch = false;
		protected String matchedText = null;
		
		public AbstractListCell(ListEntry listEntry) {
			this.listEntry = new WeakReference<>(listEntry);
			this.setErrorSupplier(() -> Optional.ofNullable(listEntry.cellErrorSupplier)
			  .flatMap(cellErrorFn -> cellErrorFn.apply(this.getValue())));
		}
		
		protected ListEntry getListEntry() {
			final ListEntry listEntry = this.listEntry.get();
			if (listEntry == null)
				throw new IllegalStateException(
				  "Illegal attempt to use list entry's cell after its parent list has been disposed");
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
		
		@Override public boolean handleNavigationKey(int keyCode, int scanCode, int modifiers) {
			if (Screen.hasAltDown()) {
				final ListEntry listEntry = getListEntry();
				final IGuiEventListener listener = listEntry.getListener();
				//noinspection SuspiciousMethodCalls
				if (listener instanceof BaseListCell && listEntry.cells.contains(listener)) {
					//noinspection SuspiciousMethodCalls
					int index = listEntry.cells.indexOf(listener);
					if (Screen.hasControlDown()) { // Move
						if (keyCode == 264 && index < listEntry.cells.size() - 1) { // Down
							listEntry.move(index, index + 1);
							((BaseListCell<?>) listener).onNavigate();
							return true;
						} else if (keyCode == 265 && index > 0) { // Up
							listEntry.move(index, index - 1);
							((BaseListCell<?>) listener).onNavigate();
							return true;
						}
					}
					if (keyCode == 257 || keyCode == 260) { // Enter | Insert
						listEntry.add(index + 1);
						listEntry.cells.get(index + 1).onNavigate();
						return true;
					} else if (index != -1 && (keyCode == 259 || keyCode == 261)) { // Backspace | Delete
						listEntry.remove(index);
						if (!listEntry.cells.isEmpty())
							listEntry.cells.get(clamp(keyCode == 259? index - 1 : index, 0, listEntry.cells.size() - 1)).onNavigate();
						return true;
					}
				}
			}
			return super.handleNavigationKey(keyCode, scanCode, modifiers);
		}
	}
}

