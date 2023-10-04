package endorh.simpleconfig.ui.api;


import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.TabOrderedElement;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.*;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Extended {@link ContainerEventHandler}.<br>
 * <br>
 * Makes focus path handling finely extensible.<br>
 * Propagates drag events with other buttons than 0.<br>
 * Adds the method {@link ContainerEventHandlerEx#endDrag(double, double, int)}
 */
public interface ContainerEventHandlerEx extends ContainerEventHandler {

	@Override
	@Nullable
	default ComponentPath getCurrentFocusPath() {
		GuiEventListener focused = getFocused();
		return focused != null? ComponentPath.path(
			this, focused.getCurrentFocusPath()) : null;
	}

	@Override
	@Nullable
   default ComponentPath nextFocusPath(@NotNull FocusNavigationEvent e) {
		GuiEventListener focused = getFocused();
		if (focused != null) {
			ComponentPath componentpath = focused.nextFocusPath(e);
			if (componentpath != null) {
				return ComponentPath.path(this, componentpath);
			}
		}

		if (e instanceof FocusNavigationEvent.TabNavigation te) {
			return handleTabNavigation(te);
		} else if (e instanceof FocusNavigationEvent.ArrowNavigation ae) {
			return handleArrowNavigation(ae);
		} else {
			return null;
		}
	}

	default boolean shouldTabNavigationCycleWithin() {
		return false;
	}

	@Nullable
   default ComponentPath handleTabNavigation(FocusNavigationEvent.TabNavigation e) {
		boolean forward = e.forward();
		GuiEventListener focused = getFocused();
		List<? extends GuiEventListener> list = new ArrayList<>(children());
		list.sort(Comparator.comparingInt(TabOrderedElement::getTabOrderGroup));

		int j = list.indexOf(focused);
		int i;
		if (focused != null && j >= 0) {
			i = j + (forward ? 1 : 0);
		} else if (forward) {
			i = 0;
		} else i = list.size();

		var iter = list.listIterator(i);
		BooleanSupplier cond = forward ? iter::hasNext : iter::hasPrevious;
		Supplier<? extends GuiEventListener> next = forward ? iter::next : iter::previous;

		while (cond.getAsBoolean()) {
			GuiEventListener l = next.get();
			ComponentPath path = l.nextFocusPath(e);
			if (path != null)
				return ComponentPath.path(this, path);
		}

		return null;
	}

	@Nullable
	default ComponentPath handleArrowNavigation(FocusNavigationEvent.ArrowNavigation e) {
		GuiEventListener focused = getFocused();
		if (focused == null) {
			ScreenDirection d = e.direction();
			ScreenRectangle rect = getRectangle().getBorder(d.getOpposite());
			return ComponentPath.path(this, nextFocusPathInDirection(rect, d, null, e));
		} else {
			ScreenRectangle rect = focused.getRectangle();
			return ComponentPath.path(this, nextFocusPathInDirection(rect, e.direction(), focused, e));
		}
	}

	@Nullable
	default ComponentPath nextFocusPathInDirection(ScreenRectangle rect, ScreenDirection dir, @Nullable GuiEventListener current, FocusNavigationEvent e) {
		ScreenAxis axis = dir.getAxis();
		ScreenAxis otherAxis = axis.orthogonal();
		ScreenDirection otherDir = otherAxis.getPositive();
		int b = rect.getBoundInDirection(dir.getOpposite());
		List<GuiEventListener> list = new ArrayList<>();

		for (GuiEventListener child : children()) {
			if (child != current) {
				ScreenRectangle childRect = child.getRectangle();
				if (childRect.overlapsInAxis(rect, otherAxis)) {
					int cb = childRect.getBoundInDirection(dir.getOpposite());
					if (dir.isAfter(cb, b)) {
						list.add(child);
					} else if (cb == b && dir.isAfter(childRect.getBoundInDirection(dir), rect.getBoundInDirection(dir))) {
						list.add(child);
					}
				}
			}
		}

		list.sort(Comparator.<GuiEventListener, Integer>comparing(l -> l
			.getRectangle().getBoundInDirection(dir.getOpposite()),
			dir.coordinateValueComparator()
		).thenComparing(l -> l
			.getRectangle().getBoundInDirection(otherDir.getOpposite()),
			otherDir.coordinateValueComparator()));

		for (GuiEventListener option : list) {
			ComponentPath path = option.nextFocusPath(e);
			if (path != null) return path;
		}

		return nextFocusPathVaguelyInDirection(rect, dir, current, e);
	}

	@Nullable
	default ComponentPath nextFocusPathVaguelyInDirection(
		ScreenRectangle rect, ScreenDirection dir,
		@Nullable GuiEventListener current, FocusNavigationEvent e
	) {
		ScreenAxis axis = dir.getAxis();
		ScreenAxis otherAxis = axis.orthogonal();
		List<Pair<GuiEventListener, Long>> list = new ArrayList<>();
		ScreenPosition pos = ScreenPosition.of(
			axis, rect.getBoundInDirection(dir),
			rect.getCenterInAxis(otherAxis));

		for (GuiEventListener child : children()) {
			if (child != current) {
				ScreenRectangle childRect = child.getRectangle();
				ScreenPosition childPos = ScreenPosition.of(
					axis, childRect.getBoundInDirection(dir.getOpposite()),
					childRect.getCenterInAxis(otherAxis));
				if (dir.isAfter(childPos.getCoordinate(axis), pos.getCoordinate(axis))) {
					long dist = Vector2i.distanceSquared(pos.x(), pos.y(), childPos.x(), childPos.y());
					list.add(Pair.of(child, dist));
				}
			}
		}

		list.sort(Comparator.comparingDouble(Pair::getRight));
		for (Pair<GuiEventListener, Long> pair : list) {
			ComponentPath path = pair.getLeft().nextFocusPath(e);
			if (path != null) return path;
		}
		return null;
	}

	Pair<Integer, GuiEventListener> getDragged();
	void setDragged(Pair<Integer, GuiEventListener> dragged);
	
	@Override default boolean mouseClicked(double mouseX, double mouseY, int button) {
		for(GuiEventListener listener : children()) {
			if (listener.mouseClicked(mouseX, mouseY, button)) {
				onMouseClickedForListener(listener, mouseX, mouseY, button);
				return true;
			}
		}
		return false;
	}
	
	default void onMouseClickedForListener(
	  GuiEventListener listener, double mouseX, double mouseY, int button
	) {
		setFocused(listener);
		if ((!isDragging() || getDragged() != null && getDragged().getLeft() == button) && (
		  button == 0
		  || listener instanceof ContainerEventHandlerEx
		  || listener instanceof GuiEventListenerEx)
		) {
			setDragging(true);
			setDragged(Pair.of(button, listener));
		}
	}
	
	@Override default boolean mouseDragged(
	  double mouseX, double mouseY, int button, double dragX, double dragY
	) {
		return getFocused() != null && isDragging()
		       && (button == 0
		           || getFocused() instanceof ContainerEventHandlerEx
		           || getFocused() instanceof GuiEventListenerEx)
		       && getFocused().mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}
	
	default void endDrag(double mouseX, double mouseY, int button) {
		final Pair<Integer, GuiEventListener> dragged = getDragged();
		if (dragged != null) {
			if (dragged.getLeft() != button)
				button = -1;
			if (dragged.getRight() instanceof ContainerEventHandlerEx) {
				((ContainerEventHandlerEx) dragged.getRight())
				  .endDrag(mouseX, mouseY, button);
				setDragging(false);
				setDragged(null);
			} else if (dragged.getRight() instanceof GuiEventListenerEx) {
				if (dragged.getLeft() == button)
					((GuiEventListenerEx) dragged.getRight())
					  .endDrag(mouseX, mouseY, button);
				setDragging(false);
				setDragged(null);
			} else {
				setDragging(false);
				setDragged(null);
			}
		}
	}
	
	@Override default boolean mouseReleased(double mouseX, double mouseY, int button) {
		handleEndDrag(mouseX, mouseY, button);
		return ContainerEventHandler.super.mouseReleased(mouseX, mouseY, button);
	}
	
	default void handleEndDrag(double mouseX, double mouseY, int button) {
		if (getDragged() != null) {
			if (button == getDragged().getLeft())
				endDrag(mouseX, mouseY, button);
		}
	}
}
