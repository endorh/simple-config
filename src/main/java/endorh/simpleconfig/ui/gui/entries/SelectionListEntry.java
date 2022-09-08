package endorh.simpleconfig.ui.gui.entries;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import endorh.simpleconfig.api.ui.icon.Icon;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.api.RedirectGuiEventListener;
import endorh.simpleconfig.ui.gui.WidgetUtils;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionIconButton;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import endorh.simpleconfig.ui.hotkey.HotKeyActionType;
import endorh.simpleconfig.ui.hotkey.HotKeyActionTypes;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@OnlyIn(value = Dist.CLIENT)
public class SelectionListEntry<T> extends TooltipListEntry<T> implements IChildListEntry {
	protected final ImmutableList<T> values;
	protected int displayedIndex;
	protected final MultiFunctionIconButton buttonWidget;
	protected final IntegerListEntry intEntry;
	protected final RedirectGuiEventListener widgetReference;
	protected final Function<T, Component> nameProvider;
	protected final List<GuiEventListener> widgets;
	protected final List<GuiEventListener> childWidgets;
	
	@Internal public SelectionListEntry(
	  Component fieldName, T[] valuesArray, T value, Function<T, Component> nameProvider
	) {
		super(fieldName);
		values = valuesArray != null ? ImmutableList.copyOf(valuesArray) : ImmutableList.of(value);
		displayedIndex = values.indexOf(value);
		if (displayedIndex == -1) displayedIndex = 0;
		setOriginal(value);
		setValue(value);
		buttonWidget = MultiFunctionIconButton.of(
		  Icon.EMPTY, ButtonAction.of(b -> {
			  if (b != 2 && !isFocused()) {
				  preserveState();
				  setFocused(true);
			  }
			  final int s = values.size();
			  if (0 <= b && b <= 1) {
				  int step = b == 0 ^ Screen.hasShiftDown()? 1 : -1;
				  displayedIndex = (displayedIndex + step + s) % s;
			  }
		}).title(() -> nameProvider.apply(getDisplayedValue())));
		intEntry = new IntegerListEntry(Component.empty(), 1);
		intEntry.setSubEntry(true);
		intEntry.setParentEntry(this);
		widgetReference = new RedirectGuiEventListener(buttonWidget);
		widgets = Lists.newArrayList(widgetReference, sideButtonReference);
		childWidgets = Lists.newArrayList(widgetReference);
		this.nameProvider = nameProvider == null ? t -> Component.translatable(t instanceof Translatable ? ((Translatable) t).getKey() : t.toString()) : nameProvider;
	}
	
	@Override public T getDisplayedValue() {
		return values.get(displayedIndex);
	}
	
	@Override public void setDisplayedValue(T value) {
		final int index = values.indexOf(value);
		if (0 <= index && index < values.size())
			this.displayedIndex = index;
	}
	
	@Override public void tick() {
		super.tick();
		if (isEditingHotKeyAction()) intEntry.tick();
	}
	
	@Override public void renderChildEntry(
	  PoseStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		if (isEditingHotKeyAction()) {
			HotKeyActionType<T, ?> type = getHotKeyActionType();
			if (type == HotKeyActionTypes.ENUM_ADD) {
				intEntry.shouldRenderEditable();
				intEntry.renderChild(mStack, x, y, w, h, mouseX, mouseY, delta);
				return;
			}
		}
		buttonWidget.active = shouldRenderEditable();
		buttonWidget.x = x;
		buttonWidget.y = y;
		buttonWidget.setExactWidth(w);
		buttonWidget.setHeight(h);
		buttonWidget.render(mStack, mouseX, mouseY, delta);
	}
	
	@Override public Optional<Component[]> getTooltip(int mouseX, int mouseY) {
		if (buttonWidget.isMouseOver(mouseX, mouseY))
			return Optional.empty();
		return super.getTooltip(mouseX, mouseY);
	}
	
	@Override public void updateFocused(boolean isFocused) {
		super.updateFocused(isFocused);
		if (!isFocused)
			WidgetUtils.forceUnFocus(buttonWidget);
		intEntry.updateFocused(isFocused && isEditingHotKeyAction() && getHotKeyActionType() == HotKeyActionTypes.ENUM_ADD);
	}
	
	@Override protected @NotNull List<? extends GuiEventListener> getEntryListeners() {
		return this.isChildSubEntry() ? childWidgets : widgets;
	}
	
	public interface Translatable {
		@NotNull String getKey();
	}
	
	@Override public String seekableValueText() {
		// TODO: Expose all possible values as seekable text
		return getUnformattedString(nameProvider.apply(getValue()));
	}
}

