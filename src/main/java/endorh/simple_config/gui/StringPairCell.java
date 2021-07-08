package endorh.simple_config.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.Expandable;
import me.shedaniel.clothconfig2.gui.entries.AbstractListListEntry.AbstractListCell;
import me.shedaniel.clothconfig2.gui.entries.TextFieldListEntry;
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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("UnstableApiUsage")
public class StringPairCell<V, E extends AbstractConfigListEntry<V>>
  extends AbstractListCell<Pair<String, V>, StringPairCell<V, E>, StringPairListEntry<V, E>> {
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
	
	protected static TextFieldWidget TextFieldListEntry$getTextFieldWidget(TextFieldListEntry<?> entry) {
		try {
			return (TextFieldWidget) TextFieldListEntry$textFieldWidget.get(entry);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("Could not access TextFieldListEntry$textFieldWidget through reflection");
		}
	}
	
	
	protected final E nestedEntry;
	protected final TextFieldWidget nameWidget;
	// protected TextFieldWidget valueWidget;
	// protected Button resetButton;
	protected AtomicReference<String> key;
	protected final Pair<String, V> original;
	protected final List<IGuiEventListener> widgets;
	protected final boolean offsetName;
	
	protected int activeColor = 0xE0E0E0;
	protected int errorColor = 0xE04242;
	
	public StringPairCell(
	  Pair<String, V> value, StringPairListEntry<V, E> listEntry, E nestedEntry
	) {
		//noinspection UnstableApiUsage
		super(value, listEntry);
		if (value == null) throw new IllegalArgumentException("String pair cell value must not be null");
		final FontRenderer fr = Minecraft.getInstance().fontRenderer;
		original = value;
		this.nestedEntry = nestedEntry;
		nameWidget = new TextFieldWidget(fr, 0, 0, KEY_WIDTH, 20, new StringTextComponent(value.getKey()));
		key = new AtomicReference<>(value.getKey());
		nameWidget.setText(key.get());
		widgets = Lists.newArrayList(new IGuiEventListener[]{nameWidget, nestedEntry});
		offsetName = nestedEntry instanceof Expandable;
		// this.widgets.addAll(nestedEntry.getEventListeners());
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
	
	@Override
	public void render(
	  MatrixStack matrices, int index, int y, int x, int entryWidth,
	  int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta
	) {
		MainWindow window = Minecraft.getInstance().getMainWindow();
		final FontRenderer fr = Minecraft.getInstance().fontRenderer;
		// this.resetButton.active = this.isEditable() && this.getDefaultValue().isPresent() && !this.defaultSupplier.get().equals(getValue());
		// this.resetButton.y = y;
		// this.nameWidget.active = true;
		// this.valueWidget.active = this.isEditable();
		final int o = offsetName? 24 : 0;
		this.nameWidget.y = y;
		// this.valueWidget.y = y;
		ITextComponent displayedFieldName = this.nestedEntry.getDisplayedFieldName();
		if (fr.getBidiFlag()) {
			fr.func_238407_a_(matrices, displayedFieldName.func_241878_f(), (float)(window.getScaledWidth() - x - fr.getStringPropertyWidth(displayedFieldName)), (float)(y + 6), getPreferredTextColor());
			// this.resetButton.x = x;
			// this.valueWidget.x = x + resetButton.getWidth() + 1;
			this.nameWidget.x = x + entryWidth - 120 - o;
		} else {
			fr.func_238407_a_(matrices, displayedFieldName.func_241878_f(), (float)x, (float)(y + 6), this.getPreferredTextColor());
			this.nameWidget.x = x + o;
			// this.valueWidget.x = x + entryWidth - 150;
			// this.resetButton.x = x + entryWidth - this.resetButton.getWidth();
		}
		
		// valueWidget.setWidth(150 - resetButton.getWidth() - 2);
		// resetButton.render(matrices, mouseX, mouseY, delta);
		nestedEntry.setParent(((StringPairListEntry) listListEntry).getParent());
		nestedEntry.setScreen(listListEntry.getConfigScreen());
		nestedEntry.render(matrices, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		nameWidget.render(matrices, mouseX, mouseY, delta);
		// valueWidget.render(matrices, mouseX, mouseY, delta);
		
	}
	
	@Override
	public @Nonnull List<? extends IGuiEventListener> getEventListeners() {
		return widgets;
	}
}