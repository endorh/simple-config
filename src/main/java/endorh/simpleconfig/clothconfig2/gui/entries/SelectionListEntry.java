package endorh.simpleconfig.clothconfig2.gui.entries;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.clothconfig2.api.IChildListEntry;
import endorh.simpleconfig.clothconfig2.gui.WidgetUtils;
import endorh.simpleconfig.clothconfig2.gui.widget.MultiFunctionButton;
import endorh.simpleconfig.clothconfig2.gui.widget.ResetButton;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@OnlyIn(value = Dist.CLIENT)
public class SelectionListEntry<T> extends TooltipListEntry<T> implements IChildListEntry {
	protected final ImmutableList<T> values;
	protected final AtomicInteger index;
	protected final Button buttonWidget;
	protected final ResetButton resetButton;
	protected final Function<T, ITextComponent> nameProvider;
	protected final List<IGuiEventListener> widgets;
	protected final List<IGuiEventListener> childWidgets;
	protected boolean child = false;
	
	@Internal public SelectionListEntry(
	  ITextComponent fieldName, T[] valuesArray, T value, Function<T, ITextComponent> nameProvider
	) {
		super(fieldName);
		values = valuesArray != null ? ImmutableList.copyOf(valuesArray) : ImmutableList.of(value);
		index = new AtomicInteger(values.indexOf(value));
		index.compareAndSet(-1, 0);
		original = value;
		buttonWidget = new MultiFunctionButton(0, 0, 150, 20, NarratorChatListener.EMPTY, (w, b) -> {
			if (b != 2 && !isSelected) {
				preserveState();
				isSelected = true;
			}
			if (b == 0) {
				index.incrementAndGet();
				index.compareAndSet(values.size(), 0);
				return true;
			} else if (b == 1) {
				index.decrementAndGet();
				index.compareAndSet(-1, values.size() - 1);
				return true;
			}
			return false;
		});
		resetButton = new ResetButton(this);
		widgets = Lists.newArrayList(buttonWidget, resetButton);
		childWidgets = Lists.newArrayList(buttonWidget);
		this.nameProvider = nameProvider == null ? t -> new TranslationTextComponent(
		  t instanceof Translatable ? ((Translatable) t).getKey() : t.toString()) : nameProvider;
	}
	
	@Override public T getValue() {
		return values.get(index.get());
	}
	
	@Override public void setValue(T value) {
		final int index = values.indexOf(value);
		if (index < 0)
			throw new IllegalArgumentException("Invalid value for selection: \"" + value + "\"");
		this.index.set(index);
	}
	
	@Override
	public void renderEntry(
	  MatrixStack mStack, int index, int y, int x, int entryWidth, int entryHeight, int mouseX,
	  int mouseY, boolean isHovered, float delta
	) {
		super.renderEntry(
		  mStack, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		MainWindow window = Minecraft.getInstance().getMainWindow();
		resetButton.y = y;
		int buttonX;
		ITextComponent name = getDisplayedFieldName();
		final FontRenderer font = Minecraft.getInstance().fontRenderer;
		if (font.getBidiFlag()) {
			font.func_238407_a_(
			  mStack, name.func_241878_f(),
			  (float) (window.getScaledWidth() - x - font.getStringPropertyWidth(name)),
			  (float) (y + 6), getPreferredTextColor());
			resetButton.x = x;
			buttonX = x + resetButton.getWidth() + 2;
		} else {
			font.func_238407_a_(
			  mStack, name.func_241878_f(), (float) x, (float) (y + 6), getPreferredTextColor());
			resetButton.x = x + entryWidth - resetButton.getWidth();
			buttonX = x + entryWidth - 150;
		}
		resetButton.render(mStack, mouseX, mouseY, delta);
		renderChild(mStack, buttonX, y, 150 - resetButton.getWidth() - 2, 20, mouseX, mouseY, delta);
	}
	
	@Override public void renderChildEntry(
	  MatrixStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		buttonWidget.active = isEditable();
		buttonWidget.x = x;
		buttonWidget.y = y;
		buttonWidget.setWidth(w);
		buttonWidget.setHeight(h);
		buttonWidget.setMessage(nameProvider.apply(getValue()));
		buttonWidget.render(mStack, mouseX, mouseY, delta);
	}
	
	@Override public Optional<ITextComponent[]> getTooltip(int mouseX, int mouseY) {
		if (resetButton.isMouseOver(mouseX, mouseY))
			return resetButton.getTooltip(mouseX, mouseY);
		if (buttonWidget.isMouseOver(mouseX, mouseY))
			return Optional.empty();
		return super.getTooltip(mouseX, mouseY);
	}
	
	@Override public void updateSelected(boolean isSelected) {
		super.updateSelected(isSelected);
		if (!isSelected)
			WidgetUtils.forceUnFocus(buttonWidget, resetButton);
	}
	
	public @NotNull List<? extends IGuiEventListener> getEventListeners() {
		return isChild()? childWidgets : widgets;
	}
	
	public interface Translatable {
		@NotNull String getKey();
	}
	
	@Override public boolean isChild() {
		return child;
	}
	
	@Override public void setChild(boolean child) {
		this.child = child;
	}
	
	@Override public String seekableValueText() {
		// TODO: Expose all possible values as seekable text
		return getUnformattedString(nameProvider.apply(getValue()));
	}
}

