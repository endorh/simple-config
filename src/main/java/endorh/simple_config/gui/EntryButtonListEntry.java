package endorh.simple_config.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simple_config.core.EntrySetterUtil;
import me.shedaniel.clothconfig2.api.AbstractConfigEntry;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class EntryButtonListEntry<V, Entry extends AbstractConfigListEntry<V>>
  extends TooltipListEntry<V> implements ISettableConfigListEntry<V> {
	protected static final int BUTTON_HEIGHT = 20;
	
	protected final Entry entry;
	protected final Button button;
	protected final Supplier<ITextComponent> buttonLabelSupplier;
	protected final @Nullable Consumer<V> saveConsumer;
	protected List<IGuiEventListener> listeners;
	
	@SuppressWarnings({"deprecation", "UnstableApiUsage"}) public EntryButtonListEntry(
	  ITextComponent fieldName, Entry entry, Consumer<V> action,
	  Supplier<ITextComponent> buttonLabelSupplier,
	  @Nullable Consumer<V> saveConsumer,
	  @Nullable Supplier<Optional<ITextComponent[]>> tooltipSupplier,
	  ITextComponent resetButtonKey
	) {
		super(fieldName, tooltipSupplier);
		this.entry = entry;
		this.entry.setParent(this.getParent());
		this.buttonLabelSupplier = buttonLabelSupplier;
		this.saveConsumer = saveConsumer;
		final FontRenderer fr = Minecraft.getInstance().fontRenderer;
		this.button = new Button(
		  0, 0, fr.getStringPropertyWidth(resetButtonKey) + 6, BUTTON_HEIGHT,
		  buttonLabelSupplier.get(), p -> action.accept(getValue()));
		this.listeners = Util.make(new ArrayList<>(), l -> {
			l.add(button);
			l.add(entry);
		});
		setReferencableEntries(Util.make(new ArrayList<>(), l -> l.add(entry)));
		setReferenceProviderEntries(Util.make(new ArrayList<>(), l -> l.add(entry)));
	}
	
	@Override public @NotNull AbstractConfigEntry<V> provideReferenceEntry() {
		return entry; // Test
	}
	
	@Override public void updateSelected(boolean isSelected) {
		entry.updateSelected(isSelected);
	}
	
	@Override public boolean isEdited() {
		return saveConsumer != null && entry.isEdited();
	}
	
	@Override public V getValue() {
		return entry.getValue();
	}
	
	@Override public void setValue(V value) {
		EntrySetterUtil.setValue(entry, value);
	}
	
	@Override public Optional<V> getDefaultValue() {
		return entry.getDefaultValue();
	}
	
	@Override public void save() {
		if (saveConsumer != null)
			saveConsumer.accept(getValue());
	}
	
	@Override public int getItemHeight() {
		return entry.getItemHeight();
	}
	
	@Override public Optional<ITextComponent> getError() {
		return entry.getError();
	}
	
	@Override public void setEditable(boolean editable) {
		super.setEditable(editable);
		entry.setEditable(editable);
	}
	
	@Override public void render(
	  MatrixStack mStack, int index, int y, int x, int entryWidth, int entryHeight, int mouseX,
	  int mouseY, boolean isHovered, float delta
	) {
		super.render(mStack, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		entry.setParent(getParent());
		//noinspection UnstableApiUsage
		entry.setScreen(getConfigScreen());
		entry.setEditable(isEditable());
		entry.render(mStack, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		
		final MainWindow window = Minecraft.getInstance().getMainWindow();
		final FontRenderer fontRenderer = Minecraft.getInstance().fontRenderer;
		button.setMessage(buttonLabelSupplier.get());
		button.active = isEditable() && !getError().isPresent();
		this.button.y = y;
		if (fontRenderer.getBidiFlag()) {
			fontRenderer.func_238407_a_(
			  mStack, getDisplayedFieldName().func_241878_f(),
			  (float)(window.getScaledWidth() - x - fontRenderer.getStringPropertyWidth(getDisplayedFieldName())),
			  (float)(y + 6), this.getPreferredTextColor());
			this.button.x = x;
		} else {
			fontRenderer.func_238407_a_(
			  mStack, getDisplayedFieldName().func_241878_f(), (float) x,
			  (float) (y + 6), this.getPreferredTextColor());
			this.button.x = x + entryWidth - button.getWidth();
		}
		button.render(mStack, mouseX, mouseY, 0F);
	}
	
	@Override public @NotNull List<? extends IGuiEventListener> getEventListeners() {
		return listeners;
	}
}
