package endorh.simple_config.clothconfig2.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.ReferenceProvider;
import endorh.simple_config.clothconfig2.gui.entries.StringPairListListEntry.StringPairCell;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StringPairListListEntry<T, Inner extends AbstractConfigListEntry<T>>
  extends AbstractListListEntry<Pair<String, T>, StringPairCell<T, Inner>, StringPairListListEntry<T, Inner>> {
	protected final List<ReferenceProvider<?>> referencableEntries = Lists.newArrayList();
	protected final Supplier<Pair<String, T>> defaultEntrySupplier;
	protected Function<List<Pair<String, T>>, Optional<ITextComponent>> errorSupplier;
	protected Function<Pair<String, T>, Optional<ITextComponent>> entryErrorSupplier;
	protected boolean ignoreOrder;
	
	public StringPairListListEntry(
	  ITextComponent fieldName, List<Pair<String, T>> value,
	  boolean defaultExpanded, Supplier<Optional<ITextComponent[]>> tooltipSupplier,
	  Consumer<List<Pair<String, T>>> saveConsumer, Supplier<List<Pair<String, T>>> defaultValue,
	  Supplier<Pair<String, T>> defaultEntrySupplier,
	  Function<List<Pair<String, T>>, Optional<ITextComponent>> errorSupplier,
	  Function<Pair<String, T>, Optional<ITextComponent>> entryErrorSupplier,
	  ITextComponent resetButtonKey, boolean deleteButtonEnabled, boolean insertInFront,
	  BiFunction<Pair<String, T>, StringPairListListEntry<T, Inner>, Inner> createNewCell,
	  boolean ignoreOrder
	) {
		super(
		  fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue,
		  resetButtonKey, false, deleteButtonEnabled, insertInFront,
		  (t, nestedListEntry) -> {
		  	  if (t == null) t = defaultEntrySupplier.get();
		  	  return new StringPairCell<>(t, nestedListEntry, createNewCell.apply(t, nestedListEntry));
		  });
		
		this.defaultEntrySupplier = defaultEntrySupplier;
		this.errorSupplier = errorSupplier;
		this.entryErrorSupplier = entryErrorSupplier;
		this.ignoreOrder = ignoreOrder;
		for (StringPairCell<T, Inner> cell : cells)
			referencableEntries.add(cell.nestedEntry);
		setReferenceProviderEntries(referencableEntries);
	}
	
	@Override
	public void updateSelected(boolean isSelected) {
		for (StringPairCell<T, Inner> cell : this.cells) {
			cell.updateSelected(isSelected && this.getListener() == cell && this.expanded);
		}
	}
	
	protected Map<String, T> toMap(Stream<Pair<String, T>> stream) {
		return stream.collect(Collectors.toMap(Pair::getKey, Pair::getValue, (a, b) -> b));
	}
	
	@Override
	public boolean isRequiresRestart() {
		return super.isRequiresRestart();
	}
	
	@Override
	public boolean isEdited() {
		return !ignoreOrder ? getValue().equals(original) :
		       !cells.stream().map(c -> c.getValue().getKey()).collect(Collectors.toSet())
		         .equals(original.stream().map(Pair::getKey).collect(Collectors.toSet()));
	}
	
	@Override
	public Optional<ITextComponent> getError() {
		// This is preferable to just displaying "Multiple issues!" without further info
		// The names already turn red on each error anyways
		final Optional<ITextComponent> e = this.cells.stream().map(BaseListCell::getConfigError)
		  .filter(Optional::isPresent).map(Optional::get).findFirst();
		if (e.isPresent())
			return e;
		return errorSupplier.apply(getValue());
	}
	
	@Override public void setValue(List<Pair<String, T>> value) {
		for (int i = 0; i < cells.size() && i < value.size(); i++)
			cells.get(i).setValue(value.get(i));
		while (cells.size() > value.size())
			remove(cells.size() - 1);
		while (cells.size() < value.size())
			add(value.get(cells.size()));
	}
	
	public void add(Pair<String, T> element) {
		StringPairCell<T, Inner> cell = createNewCell.apply(element, this);
		cells.add(cell);
		widgets.add(cell);
		cell.onAdd();
		referencableEntries.add(cell.nestedEntry);
	}
	
	public void add(int index, Pair<String, T> element) {
		StringPairCell<T, Inner> cell = createNewCell.apply(element, this);
		cells.add(index, cell);
		widgets.add(index, cell);
		cell.onAdd();
		referencableEntries.remove(cell.nestedEntry);
	}
	
	public void remove(Pair<String, T> element) {
		final int index = getValue().indexOf(element);
		if (index >= 0)
			remove(index);
	}
	
	public void remove(int index) {
		final StringPairCell<T, Inner> cell = cells.get(index);
		cell.onDelete();
		cells.remove(cell);
		widgets.remove(cell);
	}
	
	public static class StringPairCell<V, E extends AbstractConfigListEntry<V>>
	  extends AbstractListListEntry.AbstractListCell<Pair<String, V>, StringPairCell<V, E>,
	  StringPairListListEntry<V, E>> {
		protected static final int KEY_WIDTH = 120;
		
		private static final Field TextFieldListEntry$textFieldWidget;
		
		static {
			try {
				TextFieldListEntry$textFieldWidget = TextFieldListEntry.class.getDeclaredField("textFieldWidget");
				TextFieldListEntry$textFieldWidget.setAccessible(true);
			} catch (NoSuchFieldException e) {
				throw new IllegalStateException(
				  "Couldn't access TextFieldListEntry$textFieldWidget through reflection");
			}
		}
		
		
		protected final E nestedEntry;
		protected final TextFieldWidget nameWidget;
		protected AtomicReference<String> key;
		protected final Pair<String, V> original;
		protected final List<IGuiEventListener> widgets;
		protected final boolean offsetName;
		
		protected int activeColor = 0xE0E0E0;
		protected int errorColor = 0xE04242;
		
		public StringPairCell(
		  Pair<String, V> value, StringPairListListEntry<V, E> listEntry, E nestedEntry
		) {
			super(value, listEntry);
			if (value == null) throw new IllegalArgumentException("String pair cell value must not be null");
			final FontRenderer fr = Minecraft.getInstance().fontRenderer;
			original = value;
			this.nestedEntry = nestedEntry;
			nameWidget = new TextFieldWidget(fr, 0, 0, KEY_WIDTH, 20, new StringTextComponent(value.getKey()));
			key = new AtomicReference<>(value.getKey());
			nameWidget.setText(key.get());
			widgets = Lists.newArrayList(new IGuiEventListener[]{nameWidget, nestedEntry});
			offsetName = true; // nestedEntry instanceof Expandable;
		}
		
		@Override
		public Pair<String, V> getValue() {
			key.set(nameWidget.getText());
			return Pair.of(key.get(), nestedEntry.getValue());
		}
		
		@Override
		public boolean isEdited() {
			return super.isEdited() || !getValue().getKey().equals(original.getKey()) || nestedEntry.isEdited();
		}
		
		@Override
		public int getCellHeight() {
			return nestedEntry.getItemHeight();
		}
		
		@Override
		public void updateSelected(boolean isSelected) {
			nestedEntry.updateSelected(isSelected && this.getListener() == nestedEntry);
			if (!isSelected && nameWidget.isFocused()) nameWidget.changeFocus(false); // Funny bug
		}
		
		public Optional<ITextComponent> getError() {
			Optional<ITextComponent> e = nestedEntry.getError();
			if (!e.isPresent())
				e = listListEntry.entryErrorSupplier.apply(getValue());
			final int color = e.isPresent() ? errorColor : activeColor;
			nameWidget.setTextColor(color);
			nameWidget.setFGColor(color);
			return e;
		}
		
		public E getNestedEntry() {
			return nestedEntry;
		}
		
		public TextFieldWidget getNameWidget() {
			return nameWidget;
		}
		
		public void setValue(Pair<String, V> value) {
			key.set(value.getKey());
			nestedEntry.setValue(value.getValue());
		}
		
		@SuppressWarnings({"unchecked", "rawtypes"}) @Override
		public void render(
		  MatrixStack matrices, int index, int y, int x, int entryWidth,
		  int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta
		) {
			MainWindow window = Minecraft.getInstance().getMainWindow();
			final FontRenderer fr = Minecraft.getInstance().fontRenderer;
			final int o = offsetName? 24 : 0;
			this.nameWidget.y = y;
			ITextComponent displayedFieldName = this.nestedEntry.getDisplayedFieldName();
			if (fr.getBidiFlag()) {
				fr.func_238407_a_(matrices, displayedFieldName.func_241878_f(), (float)(window.getScaledWidth() - x - fr.getStringPropertyWidth(displayedFieldName)), (float)(y + 6), getPreferredTextColor());
				this.nameWidget.x = x + entryWidth - 120 - o;
			} else {
				fr.func_238407_a_(matrices, displayedFieldName.func_241878_f(), (float)x, (float)(y + 6), this.getPreferredTextColor());
				this.nameWidget.x = x + o;
			}
			
			nestedEntry.setParent(((StringPairListListEntry) listListEntry).getParent());
			nestedEntry.setScreen(listListEntry.getConfigScreen());
			nestedEntry.render(matrices, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
			nameWidget.render(matrices, mouseX, mouseY, delta);
		}
		
		@Override
		public @Nonnull List<? extends IGuiEventListener> getEventListeners() {
			return widgets;
		}
	}
}
