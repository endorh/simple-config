package endorh.simpleconfig.ui.gui.entries;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import endorh.simpleconfig.api.ui.icon.Icon;
import endorh.simpleconfig.api.ui.math.Rectangle;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.api.IOverlayCapableContainer.IOverlayRenderer;
import endorh.simpleconfig.ui.api.RedirectGuiEventListener;
import endorh.simpleconfig.ui.gui.WidgetUtils;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionIconButton;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import endorh.simpleconfig.ui.hotkey.HotKeyActionType;
import endorh.simpleconfig.ui.hotkey.HotKeyActionTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static endorh.simpleconfig.ui.gui.WidgetUtils.pos;
import static java.lang.Math.max;
import static java.lang.Math.min;

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
	protected SelectionButtonOverlay<T> overlay;
	
	@Internal public SelectionListEntry(
	  Component fieldName, T[] valuesArray, T value, Function<T, Component> nameProvider
	) {
		super(fieldName);
		values = valuesArray != null ? ImmutableList.copyOf(valuesArray) : ImmutableList.of(value);
		displayedIndex = values.indexOf(value);
		if (displayedIndex == -1) displayedIndex = 0;
		setOriginal(value);
		setValue(value);
		this.nameProvider =
		  nameProvider == null
		  ? t -> Component.translatable(t instanceof Translatable? ((Translatable) t).getKey() : t.toString()) : nameProvider;
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
		}).title(() -> this.nameProvider.apply(getDisplayedValue())));
		intEntry = new IntegerListEntry(Component.empty(), 1);
		intEntry.setSubEntry(true);
		intEntry.setParentEntry(this);
		widgetReference = new RedirectGuiEventListener(buttonWidget);
		widgets = Lists.newArrayList(widgetReference, sideButtonReference);
		childWidgets = Lists.newArrayList(widgetReference);
	}
	
	@Override public T getDisplayedValue() {
		return values.get(displayedIndex);
	}
	
	@Override public void setDisplayedValue(T value) {
		final int index = values.indexOf(value);
		if (0 <= index && index < values.size())
			displayedIndex = index;
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
		return isChildSubEntry() ? childWidgets : widgets;
	}
	
	public interface Translatable {
		@NotNull String getKey();
	}
	
	@Override public String seekableValueText() {
		// TODO: Expose all possible values as seekable text
		return getUnformattedString(nameProvider.apply(getValue()));
	}
	
	@Override public boolean onMouseClicked(double mouseX, double mouseY, int button) {
		if (button == 2 && buttonWidget.isMouseOver(mouseX, mouseY) && isEditable()) {
			preserveState();
			setFocused(true);
			setDragging(true);
			setDragged(Pair.of(2, this));
			buttonWidget.setSuppressHoverPeek(true);
			Screen screen = Minecraft.getInstance().screen;
			if (screen == null) return false;
			Font font = Minecraft.getInstance().font;
			int width = values.stream()
			  .mapToInt(v -> font.width(nameProvider.apply(v)) + 8)
			  .max().orElse(60);
			width = max(buttonWidget.getWidth(), min(width, 300));
			int x = buttonWidget.x + width > screen.width? screen.width - width : buttonWidget.x;
			Rectangle area = new Rectangle(
			  x, buttonWidget.y - buttonWidget.getHeight() * (values.size() - 1),
			  width, buttonWidget.getHeight() * (values.size() * 2 - 1));
			if (overlay != null) overlay.cancel();
			overlay = new SelectionButtonOverlay<>(this);
			getScreen().addOverlay(area, overlay);
			return true;
		}
		return super.onMouseClicked(mouseX, mouseY, button);
	}
	
	@Override public void endDrag(double mouseX, double mouseY, int button) {
		Pair<Integer, GuiEventListener> dragged = getDragged();
		if (dragged != null && dragged.getRight() == this && dragged.getLeft() == 2) {
			if (overlay != null) overlay.commit();
			setDragging(false);
			setDragged(null);
			buttonWidget.setSuppressHoverPeek(false);
		} else super.endDrag(mouseX, mouseY, button);
	}
	
	public static class SelectionButtonOverlay<T> implements IOverlayRenderer {
		private final SelectionListEntry<T> entry;
		private int startY = -1;
		private int startIndex;
		private T lastSelected;
		private boolean cancelled = false;
		
		public SelectionButtonOverlay(SelectionListEntry<T> entry) {
			this.entry = entry;
			lastSelected = entry.getDisplayedValue();
			startIndex = entry.values.indexOf(lastSelected);
			if (startIndex == -1) startIndex = 0;
		}
		
		@Override public boolean renderOverlay(
		  PoseStack mStack, Rectangle area, int mouseX, int mouseY, float delta
		) {
			if (cancelled) return false;
			if (startY == -1)
				startY = mouseY;
			MultiFunctionIconButton button = entry.buttonWidget;
			ImmutableList<T> values = entry.values;
			int bh = button.getHeight();
			int y = button.y + mouseY - startY - startIndex * bh;
			y = Mth.clamp(y, button.y - (values.size() - 1) * bh, button.y);
			int index = Mth.clamp((button.y - y + bh / 2) / bh, 0, values.size());
			T prev = entry.getDisplayedValue();
			lastSelected = values.get(index);
			boolean prevFocused = button.isFocused();
			for (T value: values) {
				WidgetUtils.forceSetFocus(button, value == lastSelected);
				pos(button, area.x, y, area.width);
				entry.setDisplayedValue(value);
				button.render(mStack, -1, -1, delta);
				y += bh;
			}
			WidgetUtils.forceSetFocus(button, prevFocused);
			entry.setDisplayedValue(prev);
			return true;
		}
		
		public T getSelected() {
			return lastSelected;
		}
		
		public void cancel() {
			cancelled = true;
		}
		
		public void commit() {
			if (entry.isEditable()) {
				entry.preserveState();
				entry.setDisplayedValue(lastSelected);
			}
			cancel();
		}
	}
}

