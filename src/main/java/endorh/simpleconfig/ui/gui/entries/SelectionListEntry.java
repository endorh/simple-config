package endorh.simpleconfig.ui.gui.entries;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import endorh.simpleconfig.api.ui.icon.Icon;
import endorh.simpleconfig.api.ui.math.Rectangle;
import endorh.simpleconfig.config.ClientConfig.advanced;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.api.IOverlayCapableContainer.IOverlayRenderer;
import endorh.simpleconfig.ui.api.RedirectGuiEventListener;
import endorh.simpleconfig.ui.api.ScissorsHandler;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionIconButton;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import endorh.simpleconfig.ui.gui.widget.ToggleAnimator;
import endorh.simpleconfig.ui.hotkey.HotKeyActionType;
import endorh.simpleconfig.ui.hotkey.HotKeyActionTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static java.lang.Math.*;

@OnlyIn(Dist.CLIENT)
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
	protected double accumulatedScroll;
	protected long lastScrollTime = 0;
	
	@Internal public SelectionListEntry(
	  Component fieldName, List<T> valuesList, T value, Function<T, Component> nameProvider
	) {
		super(fieldName);
		values = valuesList != null ? ImmutableList.copyOf(valuesList) : ImmutableList.of(value);
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
			  if (b < 0) b = 0;
			  if (b <= 1) {
				  int step = b == 0 ^ Screen.hasShiftDown()? 1 : -1;
				  displayedIndex = (displayedIndex + step + s) % s;
			  }
		}).title(() -> this.nameProvider.apply(getDisplayedValue()))
			 .active(this::shouldRenderEditable));
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
      GuiGraphics gg, int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		if (isEditingHotKeyAction()) {
			HotKeyActionType<T, ?> type = getHotKeyActionType();
			if (type == HotKeyActionTypes.ENUM_ADD) {
				intEntry.shouldRenderEditable();
				intEntry.renderChild(gg, x, y, w, h, mouseX, mouseY, delta);
				return;
			}
		}
		buttonWidget.setX(x);
		buttonWidget.setY(y);
		buttonWidget.setExactWidth(w);
		buttonWidget.setHeight(h);
		buttonWidget.render(gg, mouseX, mouseY, delta);
	}
	
	@Override public Optional<Component[]> getTooltip(int mouseX, int mouseY) {
		if (buttonWidget.isMouseOver(mouseX, mouseY))
			return Optional.empty();
		return super.getTooltip(mouseX, mouseY);
	}
	
	@Override public void updateFocused(boolean isFocused) {
		super.updateFocused(isFocused);
		if (!isFocused)
         ((GuiEventListener) buttonWidget).setFocused(false);
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
		if ((button == 2 || button == 0 && Screen.hasAltDown())
		    && buttonWidget.isMouseOver(mouseX, mouseY) && isEditable()
		) {
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
			int x = buttonWidget.getX() + width > screen.width? screen.width - width : buttonWidget.getX();
			Rectangle area = new Rectangle(
			  x, buttonWidget.getY() - buttonWidget.getHeight() * (values.size() - 1),
			  width, buttonWidget.getHeight() * (values.size() * 2 - 1));
			if (overlay != null) overlay.cancel();
			overlay = new SelectionButtonOverlay<>(this);
			overlay.startAnimation();
			getScreen().addOverlay(area, overlay);
			return true;
		}
		return super.onMouseClicked(mouseX, mouseY, button);
	}
	
	@Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (advanced.cycle_with_scroll && isEditable() && buttonWidget.isMouseOver(mouseX, mouseY)) {
			int i = values.indexOf(getDisplayedValue());
			delta /= Minecraft.getInstance().options.mouseWheelSensitivity().get();
			delta = accumulateScroll(delta);
			if (abs(delta) >= 1D) {
				preserveState();
				int step = delta > 0D? -(int) delta : (int) -delta;
				i += step;
				accumulatedScroll += step;
				int ic = Mth.clamp(i, 0, values.size() - 1);
				setDisplayedValue(values.get(ic));
				playFeedbackTap(i != ic? 0.1F : 0.5F);
			}
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, delta);
	}
	
	protected double accumulateScroll(double delta) {
		long time = System.currentTimeMillis();
		if (time - lastScrollTime > 250) accumulatedScroll = 0;
		lastScrollTime = time;
		accumulatedScroll = accumulatedScroll + delta;
		return accumulatedScroll;
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
		private ToggleAnimator animator = new ToggleAnimator(150L);
		
		public SelectionButtonOverlay(SelectionListEntry<T> entry) {
			this.entry = entry;
			lastSelected = entry.getDisplayedValue();
			startIndex = entry.values.indexOf(lastSelected);
			if (startIndex == -1) startIndex = 0;
		}
		
		public void startAnimation() {
			animator.resetTarget();
		}
		
		@Override public boolean renderOverlay(
         GuiGraphics gg, Rectangle area, int mouseX, int mouseY, float delta
		) {
			if (cancelled) return false;
			if (startY == -1)
				startY = mouseY;
			MultiFunctionIconButton button = entry.buttonWidget;
			ImmutableList<T> values = entry.values;
			int bh = button.getHeight();
			if (animator.isInProgress()) {
				float p = animator.getEaseOut();
				Rectangle rect = new Rectangle(
						button.getX(), button.getY() + mouseY - startY - round(startIndex * bh * p),
				  area.width, round(Mth.lerp(p, bh, bh * values.size())));
				renderOverlayShadow(gg, area, mouseX, mouseY, delta, rect);
				ScissorsHandler.INSTANCE.withSingleScissor(
				  rect, () -> doRenderOverlay(gg, area, mouseX, mouseY, delta));
			} else {
				renderOverlayShadow(gg, area, mouseX, mouseY, delta, null);
				doRenderOverlay(gg, area, mouseX, mouseY, delta);
			}
			return true;
		}
		
		public void renderOverlayShadow(
         GuiGraphics gg, Rectangle area, int mouseX, int mouseY, float delta,
         @Nullable Rectangle overlayArea
		) {
			if (overlayArea == null) {
				MultiFunctionIconButton button = entry.buttonWidget;
				ImmutableList<T> values = entry.values;
				int bh = button.getHeight();
				int y = button.getY() + mouseY - startY - startIndex * bh;
				y = Mth.clamp(y, button.getY() - (values.size() - 1) * bh, button.getY());
				overlayArea = new Rectangle(
						button.getX(), y,
				  area.width, bh * values.size());
			}
			final Rectangle r = overlayArea;
			ScissorsHandler.INSTANCE.withoutScissors(() -> {
				for (int i = 4; i > 0; i--) gg.fill(
				  r.x - i, r.y - i, r.getMaxX() + i, r.getMaxY() + i,
				  (int) ((4 - i + 1) / 4F * 0.8F * 255F) << 24 | 0x161616);
			});
		}
		
		public void doRenderOverlay(
         GuiGraphics gg, Rectangle area, int mouseX, int mouseY, float delta
		) {
			MultiFunctionIconButton button = entry.buttonWidget;
			ImmutableList<T> values = entry.values;
			int bh = button.getHeight();
			int y = button.getY() + mouseY - startY - startIndex * bh;
			y = Mth.clamp(y, button.getY() - (values.size() - 1) * bh, button.getY());
			int index = Mth.clamp((button.getY() - y + bh / 2) / bh, 0, values.size());
			T prev = entry.getDisplayedValue();
			lastSelected = values.get(index);
			boolean prevFocused = button.isFocused();
			for (T value: values) {
				((GuiEventListener) button).setFocused(value == lastSelected);
				button.setPosition(area.x, y);
				((AbstractWidget) button).setWidth(area.width);
				entry.setDisplayedValue(value);
				button.render(gg, -1, -1, delta);
				y += bh;
			}
			((GuiEventListener) button).setFocused(prevFocused);
			entry.setDisplayedValue(prev);
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

