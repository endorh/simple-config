package endorh.simpleconfig.ui.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import endorh.simpleconfig.api.ui.math.Point;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.api.Tooltip;
import endorh.simpleconfig.ui.gui.widget.combobox.ComboBoxWidget;
import endorh.simpleconfig.ui.gui.widget.combobox.IComboBoxModel;
import endorh.simpleconfig.ui.gui.widget.combobox.wrapper.ITypeWrapper;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.lang.Math.min;

@OnlyIn(Dist.CLIENT)
public class ComboBoxListEntry<T> extends TooltipListEntry<T> implements IChildListEntry {
	protected ITypeWrapper<T> typeWrapper;
	protected ComboBoxWidget<T> comboBox;
	@NotNull protected List<T> suggestions;
	protected boolean suggestionMode = true;
	protected List<GuiEventListener> listeners;
	protected List<GuiEventListener> childListeners;
	protected int maxLength;
	private int frame = 0;
	
	public ComboBoxListEntry(
	  Component fieldName, T value, ITypeWrapper<T> typeWrapper, @Nullable Collection<T> suggestions
	) {
		super(fieldName);
		setOriginal(value);
		setValue(value);
		this.typeWrapper = typeWrapper;
		this.suggestions = suggestions != null ? new ArrayList<>(suggestions) : new ArrayList<>();
		comboBox = new ComboBoxWidget<>(
		  typeWrapper, this::getScreen, 0, 0, 150, 18);
		comboBox.setMaxLength(99999);
		comboBox.setSuggestions(this.suggestions);
		comboBox.setValue(value);
		comboBox.setTooltipRenderer((t, mx, my) -> getScreen().addTooltip(Tooltip.of(
		  fieldArea, Point.of(mx, my),
		  postProcessTooltip(t.toArray(Component[]::new)))));
		listeners = Lists.newArrayList(comboBox, sideButtonReference);
		childListeners = Lists.newArrayList(comboBox);
	}
	
	@Override public void renderChildEntry(
	  PoseStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		comboBox.setEnabled(shouldRenderEditable());
		comboBox.setRestrictedToSuggestions(!isSuggestionMode());
		// Like text fields, combo boxes render the border outside, so we inset it 1px to match
		comboBox.x = x + 1;
		comboBox.y = y + 1;
		comboBox.setWidth(w - 2);
		comboBox.setHeight(h - 2);
		comboBox.setDropDownHeight(min(120, getEntryList().bottom - getEntryList().top - 32));
		comboBox.render(mStack, mouseX, mouseY, delta);
	}
	
	@Override public void updateFocused(boolean isFocused) {
		boolean prev = isFocused();
		super.updateFocused(isFocused);
		if (!isFocused && prev) {
			comboBox.setAnchorPos(comboBox.getCaret());
			comboBox.setFocused(false);
		}
	}
	
	@Override protected void acquireFocus() {
		super.acquireFocus();
		comboBox.moveCaretToEnd();
		comboBox.setAnchorPos(0);
	}
	
	@Override public boolean onMouseClicked(double mouseX, double mouseY, int button) {
		if (!isFocused() && button == 1 && comboBox.isMouseOver(mouseX, mouseY) && !comboBox.isMouseOverArrow((int) mouseX, (int) mouseY)) {
			preserveState();
			setFocused(true);
		}
		return super.onMouseClicked(mouseX, mouseY, button);
	}
	
	public void setSuggestions(List<T> suggestions) {
		comboBox.setSuggestions(suggestions);
	}
	
	public void setSuggestionProvider(IComboBoxModel<T> provider) {
		comboBox.setSuggestionProvider(provider);
	}
	
	@Internal @Override public Optional<Component> getErrorMessage() {
		return comboBox.getError();
	}
	
	@Override public T getDisplayedValue() {
		return comboBox.getValue();
	}
	
	@Override public void setDisplayedValue(T value) {
		comboBox.setValue(value);
	}
	
	public boolean isSuggestionMode() {
		return suggestionMode;
	}
	
	public void setSuggestionMode(boolean suggestionMode) {
		this.suggestionMode = suggestionMode;
	}
	
	@Override public void tick() {
		super.tick();
		if ((frame++) % 10 == 0) comboBox.tick();
	}
	
	@Override public Optional<Component[]> getTooltip(int mouseX, int mouseY) {
		if (comboBox.isMouseOver(mouseX, mouseY))
			return Optional.empty();
		return super.getTooltip(mouseX, mouseY);
	}
	
	@Override public int getExtraScrollHeight() {
		return comboBox.isDropDownShown()? comboBox.getDropDownHeight() : 0;
	}
	
	@Override protected @NotNull List<? extends GuiEventListener> getEntryListeners() {
		return this.isChildSubEntry() ? childListeners : listeners;
	}
	
	public void setMaxLength(int maxLength) {
		this.maxLength = maxLength;
		comboBox.setMaxLength(maxLength);
	}
}
