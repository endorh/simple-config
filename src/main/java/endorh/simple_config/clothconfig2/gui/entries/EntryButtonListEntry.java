package endorh.simple_config.clothconfig2.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simple_config.SimpleConfigMod;
import endorh.simple_config.clothconfig2.api.*;
import endorh.simple_config.clothconfig2.gui.AbstractConfigScreen;
import endorh.simple_config.clothconfig2.gui.widget.DynamicEntryListWidget;
import endorh.simple_config.clothconfig2.gui.widget.MultiFunctionImageButton;
import endorh.simple_config.clothconfig2.gui.widget.ResetButton;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class EntryButtonListEntry<V, Entry extends AbstractConfigListEntry<V> & IChildListEntry>
  extends TooltipListEntry<V> implements IChildListEntry {
	
	protected final Entry entry;
	protected final Button button;
	protected final ResetButton resetButton = new ResetButton(this);
	protected final Supplier<ITextComponent> buttonLabelSupplier;
	protected List<IGuiEventListener> listeners;
	protected List<IGuiEventListener> childListeners;
	protected boolean child;
	
	public EntryButtonListEntry(
	  ITextComponent fieldName, Entry entry, Consumer<V> action,
	  Supplier<ITextComponent> buttonLabelSupplier
	) {
		super(fieldName);
		this.entry = entry;
		entry.setChild(true);
		entry.setParent(getParentOrNull());
		entry.setScreen(getConfigScreenOrNull());
		entry.setName(name);
		entry.setParentEntry(parentEntry);
		entry.setListParent(listParent);
		entry.setExpandableParent(getExpandableParent());
		this.buttonLabelSupplier = buttonLabelSupplier;
		this.button = new MultiFunctionImageButton(
		  0, 0, 20, 20, 160, 128,
		  new ResourceLocation(SimpleConfigMod.MOD_ID, "textures/gui/cloth_config.png"),
		  (w, b) -> {
			  if (b == 0) {
				  action.accept(getValue());
				  return true;
			  }
			  return false;
		  });
		this.listeners = Lists.newArrayList(this.entry, button, resetButton);
		this.childListeners = Lists.newArrayList(this.entry, button);
		setReferenceProviderEntries(Util.make(new ArrayList<>(), l -> l.add(entry)));
	}
	
	@Override public @NotNull AbstractConfigEntry<?> provideReferenceEntry() {
		return entry; // Test
	}
	
	@Override public void updateSelected(boolean isSelected) {
		super.updateSelected(isSelected);
		entry.updateSelected(isSelected);
		if (!isSelected)
			EntryButtonListEntry.forceUnFocus(button);
	}
	
	@Override public boolean isEdited() {
		return saveConsumer != null && super.isEdited();
	}
	
	@Override public V getValue() {
		return entry.getValue();
	}
	
	@Override public void setValue(V value) {
		entry.setValue(value);
	}
	
	@Override public int getItemHeight() {
		return entry.getItemHeight();
	}
	
	@Internal @Override public Optional<ITextComponent> getErrorMessage() {
		return entry.getErrorMessage();
	}
	
	@Override public void setEditable(boolean editable) {
		super.setEditable(editable);
		entry.setEditable(editable);
	}
	
	@Override public void renderEntry(
	  MatrixStack mStack, int index, int y, int x, int entryWidth, int entryHeight, int mouseX,
	  int mouseY, boolean isHovered, float delta
	) {
		super.renderEntry(mStack, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		entry.setEditable(isEditable());
		
		final MainWindow window = Minecraft.getInstance().getMainWindow();
		final FontRenderer font = Minecraft.getInstance().fontRenderer;
		button.active = isEditable() && !getErrorMessage().isPresent();
		int entryX;
		this.button.y = y;
		if (font.getBidiFlag()) {
			font.func_238407_a_(
			  mStack, getDisplayedFieldName().func_241878_f(),
			  (float)(window.getScaledWidth() - x - font.getStringPropertyWidth(getDisplayedFieldName())),
			  (float)(y + 6), this.getPreferredTextColor());
			this.resetButton.x = x;
			entryX = x + 24;
		} else {
			font.func_238407_a_(
			  mStack, getDisplayedFieldName().func_241878_f(), (float) x,
			  (float) (y + 6), this.getPreferredTextColor());
			this.resetButton.x = x + entryWidth - button.getWidth();
			entryX = x + entryWidth - 148;
		}
		resetButton.y = y;
		resetButton.render(mStack, mouseX, mouseY, delta);
		renderChild(mStack, entryX, y, 146 - resetButton.getWidth(), 20, mouseX, mouseY, delta);
	}
	
	@Override public void renderChildEntry(
	  MatrixStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		entry.renderChild(mStack, x, y, w - 22, h, mouseX, mouseY, delta);
		button.x = x + w - 20;
		button.y = y;
		button.render(mStack, mouseX, mouseY, delta);
	}
	
	@Override public void setParent(DynamicEntryListWidget<?> parent) {
		super.setParent(parent);
		entry.setParent(parent);
	}
	
	@Override public void setScreen(AbstractConfigScreen screen) {
		super.setScreen(screen);
		entry.setScreen(screen);
	}
	
	@Override public void setListParent(
	  @Nullable BaseListEntry<?, ?, ?> listParent
	) {
		super.setListParent(listParent);
		entry.setListParent(listParent);
	}
	
	@Override public void setExpandableParent(IExpandable parent) {
		super.setExpandableParent(parent);
		entry.setExpandableParent(parent);
	}
	
	@Override public void setName(String name) {
		super.setName(name);
		if (entry != null) entry.setName(name);
	}
	
	@Override public void setParentEntry(AbstractConfigEntry<?> parentEntry) {
		super.setParentEntry(parentEntry);
		entry.setParentEntry(parentEntry);
	}
	
	@Override public void setCategory(ConfigCategory category) {
		super.setCategory(category);
		entry.setCategory(category);
	}
	
	@Override public void resetValue(boolean commit) {
		entry.resetValue(commit);
	}
	
	@Override public void restoreValue(boolean commit) {
		entry.restoreValue(commit);
	}
	
	@Override public boolean isResettable() {
		return entry.isResettable();
	}
	
	@Override public boolean isRestorable() {
		return entry.isRestorable();
	}
	
	@Override public void onNavigate() {
		super.onNavigate();
		setListener(entry);
	}
	
	@Override public String seekableValueText() {
		return entry.seekableValueText();
	}
	
	@Override public Optional<ITextComponent[]> getTooltip(int mouseX, int mouseY) {
		if (button.isMouseOver(mouseX, mouseY))
			return Optional.of(new ITextComponent[]{buttonLabelSupplier.get()});
		if (entry instanceof TooltipListEntry) {
			if (!((TooltipListEntry<?>) entry).getTooltip(mouseX, mouseY).isPresent()
			    && ((TooltipListEntry<?>) entry).getTooltip().isPresent())
				return Optional.empty();
		}
		return super.getTooltip(mouseX, mouseY);
	}
	
	@Override public @NotNull List<? extends IGuiEventListener> getEventListeners() {
		return isChild()? childListeners : listeners;
	}
	
	@Override public boolean isChild() {
		return child;
	}
	
	@Override public void setChild(boolean child) {
		this.child = child;
	}
}
