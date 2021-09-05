package endorh.simple_config.clothconfig2.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simple_config.clothconfig2.api.IChildListEntry;
import endorh.simple_config.clothconfig2.gui.widget.ComboBoxWidget;
import endorh.simple_config.clothconfig2.gui.widget.ComboBoxWidget.ISortedSuggestionProvider;
import endorh.simple_config.clothconfig2.gui.widget.ComboBoxWidget.ITypeWrapper;
import endorh.simple_config.clothconfig2.gui.widget.ResetButton;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static java.lang.Math.min;

@OnlyIn(Dist.CLIENT)
public class ComboBoxListEntry<T> extends TooltipListEntry<T> implements IChildListEntry {
	protected ITypeWrapper<T> typeWrapper;
	protected ResetButton resetButton;
	protected ComboBoxWidget<T> comboBox;
	@NotNull protected List<T> suggestions;
	protected boolean suggestionMode = true;
	protected List<IGuiEventListener> listeners;
	protected List<IGuiEventListener> childListeners;
	protected boolean child;
	protected int maxLength;
	
	public ComboBoxListEntry(
	  ITextComponent fieldName, T value, ITypeWrapper<T> typeWrapper, @Nullable Collection<T> suggestions
	) {
		super(fieldName);
		this.original = value;
		this.typeWrapper = typeWrapper;
		this.suggestions = suggestions != null ? new ArrayList<>(suggestions) : new ArrayList<>();
		comboBox = new ComboBoxWidget<>(
		  typeWrapper, this::getConfigScreen, 0, 0, 150, 18);
		comboBox.setMaxLength(99999);
		comboBox.setSuggestions(this.suggestions);
		comboBox.setValue(value);
		resetButton = new ResetButton(this);
		listeners = Lists.newArrayList(comboBox, resetButton);
		childListeners = Lists.newArrayList(comboBox);
	}
	
	@Override public void renderEntry(
	  MatrixStack mStack, int index, int y, int x, int entryWidth, int entryHeight,
	  int mouseX, int mouseY, boolean isHovered, float delta
	) {
		super.renderEntry(mStack, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		MainWindow window = Minecraft.getInstance().getMainWindow();
		resetButton.y = y;
		
		int comboBoxX;
		ITextComponent name = getDisplayedFieldName();
		final FontRenderer font = Minecraft.getInstance().fontRenderer;
		if (font.getBidiFlag()) {
			font.func_238407_a_(
			  mStack, name.func_241878_f(),
			  (float) (window.getScaledWidth() - x - font.getStringPropertyWidth(name)),
			  (float) (y + 6), getPreferredTextColor());
			resetButton.x = x;
			comboBoxX = x + resetButton.getWidth() + 4;
		} else {
			font.func_238407_a_(
			  mStack, name.func_241878_f(), (float) x, (float) (y + 6),
			  getPreferredTextColor());
			resetButton.x = x + entryWidth - resetButton.getWidth();
			comboBoxX = x + entryWidth - 150 + 2;
		}
		renderChild(mStack, comboBoxX, y, 144 - resetButton.getWidth(), 20, mouseX, mouseY, delta);
		resetButton.render(mStack, mouseX, mouseY, delta);
	}
	
	@Override public void renderChildEntry(
	  MatrixStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		comboBox.setRestrictedToSuggestions(!isSuggestionMode());
		comboBox.x = x;
		comboBox.y = y + 1;
		comboBox.setWidth(w);
		comboBox.setHeight(h - 2);
		comboBox.setDropDownHeight(min(120, getParent().bottom - getParent().top - 32));
		comboBox.render(mStack, mouseX, mouseY, delta);
	}
	
	@Override public void updateSelected(boolean isSelected) {
		super.updateSelected(isSelected);
		if (!isSelected) {
			comboBox.setFocused(false);
			ComboBoxListEntry.forceUnFocus(resetButton);
		}
	}
	
	@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!isSelected && button == 1 && comboBox.isMouseOver(mouseX, mouseY) && !comboBox.isMouseOverArrow((int) mouseX, (int) mouseY)) {
			preserveState();
			isSelected = true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}
	
	public void setSuggestions(List<T> suggestions) {
		comboBox.setSuggestions(suggestions);
	}
	
	public void setSuggestionProvider(ISortedSuggestionProvider<T> provider) {
		comboBox.setSuggestionProvider(provider);
	}
	
	@Internal @Override public Optional<ITextComponent> getError() {
		return comboBox.getError();
	}
	
	@Override public T getValue() {
		return comboBox.getValue();
	}
	
	@Override public void setValue(T value) {
		comboBox.setValue(value);
	}
	
	public boolean isSuggestionMode() {
		return suggestionMode;
	}
	
	public void setSuggestionMode(boolean suggestionMode) {
		this.suggestionMode = suggestionMode;
	}
	
	@Override public Optional<ITextComponent[]> getTooltip(int mouseX, int mouseY) {
		final Optional<ITextComponent[]> tooltip = resetButton.getTooltip(mouseX, mouseY);
		if (tooltip.isPresent())
			return tooltip;
		if (comboBox.isMouseOver(mouseX, mouseY))
			return Optional.empty();
		return super.getTooltip(mouseX, mouseY);
	}
	
	@Override public int getExtraScrollHeight() {
		return comboBox.isDropDownShown()? comboBox.getDropDownHeight() : 0;
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
	
	public void setMaxLength(int maxLength) {
		this.maxLength = maxLength;
		comboBox.setMaxLength(maxLength);
	}
}
