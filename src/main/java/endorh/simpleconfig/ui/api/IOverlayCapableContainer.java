package endorh.simpleconfig.ui.api;

import com.mojang.blaze3d.vertex.PoseStack;
import endorh.simpleconfig.api.ui.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

/**
 * Feature interface for UI components (or Screens) that support the rendering and
 * event handling of overlays (see {@link IOverlayRenderer}).
 * <br><br>
 * Overlays are rendered after the main content of the component, and receive mouse events
 * before other components.
 * <br><br>
 * In addition to implementing the {@link #getSortedOverlays()} method as a final property,
 * implementations must insert hook calls in the respective methods to <ul>
 *    <li>{@link #renderOverlays(PoseStack, int, int, float)}</li>
 *    <li>{@link #handleOverlaysMouseClicked(double, double, int)}</li>
 *    <li>{@link #handleOverlaysMouseReleased(double, double, int)}</li>
 *    <li>{@link #handleOverlaysMouseDragged(double, double, int, double, double)}</li>
 *    <li>{@link #handleOverlaysMouseScrolled(double, double, double)}</li>
 *    <li>{@link #handleOverlaysEscapeKey()}</li>
 *    <li>{@link #handleOverlaysDragEnd(double, double, int)} (if also extends from
 *    {@link ContainerEventHandlerEx})</li>
 * </ul>.
 * Additionally, implementations may also call {@link #shouldOverlaysSuppressHover(int, int)} in
 * their {@code render} method, to check if they should pass (-1, -1) as the mouse coordinates
 * for all regularly rendered components, to prevent incorrect rendering of hover state of
 * components under overlays.
 */
public interface IOverlayCapableContainer {
	/**
	 * UI component able to render an overlay when added to an {@link IOverlayCapableContainer}.
	 * @see IOverlayCapableContainer#addOverlay(Rectangle, IOverlayRenderer)
	 */
	interface IOverlayRenderer {
		/**
		 * Render the overlay<br>
		 * @return {@code true} if the overlay should still be rendered in the next frame,
		 *         {@code false} if the overlay should be removed.
		 */
		boolean renderOverlay(
		  PoseStack mStack, Rectangle area, int mouseX, int mouseY, float delta);
		
		/**
		 * Called when a click event happens within an overlay
		 * @return True if the event is handled (if the overlay has transparent
		 *         areas, clicks should fall through them)
		 */
		default boolean overlayMouseClicked(Rectangle area, double mouseX, double mouseY, int button) {
			return false;
		}
		
		/**
		 * Called when a click event happens outside this overlay<br>
		 * Useful to dismiss overlays when clicked outside them<br>
		 * Note that this method cannot stop propagation of the click event
		 */
		default void overlayMouseClickedOutside(Rectangle area, double mouseX, double mouseY, int button) {}
		
		/**
		 * Called when a drag operation starts within the overlay and
		 * the overlay returns true for {@link IOverlayRenderer#isOverlayDragging()}
		 * @return False to end the drag
		 */
		default boolean overlayMouseDragged(
		  Rectangle area, double mouseX, double mouseY, int button, double dragX, double dragY
		) {
			return false;
		}
		
		/**
		 * Called when a drag operation finishes
		 */
		default void overlayMouseDragEnd(Rectangle area, double mouseX, double mouseY, int button) {}
		
		/**
		 * Called when the mouse is released over the overlay<br>
		 * Not really useful
		 */
		default boolean overlayMouseReleased(Rectangle area, double mouseX, double mouseY, int button) {
			return false;
		}
		
		/**
		 * Called when the mouse is scrolled over the overlay<br>
		 * @return True if the scroll is consumed
		 */
		default boolean overlayMouseScrolled(Rectangle area, double mouseX, double mouseY, double amount) {
			return false;
		}
		
		/**
		 * Return true to be able to receive drag event calls
		 */
		default boolean isOverlayDragging() {
			return false;
		}
		
		/**
		 * Called when the escape key is pressed
		 * @return True if the key is consumed
		 */
		default boolean overlayEscape() {
			return false;
		}
	}
	
	int getScreenWidth();
	int getScreenHeight();
	
	/**
	 * Get the sorted list of overlays of this container.
	 */
	SortedOverlayCollection getSortedOverlays();
	
	default void addOverlay(Rectangle area, IOverlayRenderer overlayRenderer, int priority) {
		getSortedOverlays().add(area, overlayRenderer, priority);
	}
	
	default void addOverlay(Rectangle area, IOverlayRenderer overlayRenderer) {
		addOverlay(area, overlayRenderer, 0);
	}
	
	default boolean handleOverlaysEscapeKey() {
		final SortedOverlayCollection sortedOverlays = getSortedOverlays();
		for (OverlayTicket ticket : sortedOverlays.descending()) {
			if (ticket.renderer.overlayEscape())
				return true;
		}
		return false;
	}
	
	default boolean handleOverlaysMouseClicked(
	  double mouseX, double mouseY, int button
	) {
		final SortedOverlayCollection sortedOverlays = getSortedOverlays();
		for (OverlayTicket ticket : sortedOverlays.descending()) {
			if (ticket.area.contains(mouseX, mouseY)) {
				if (ticket.renderer.overlayMouseClicked(ticket.area, mouseX, mouseY, button)) {
					if (ticket.renderer.isOverlayDragging())
						sortedOverlays.setDragTarget(ticket.renderer);
					return true;
				}
			} else ticket.renderer.overlayMouseClickedOutside(ticket.area, mouseX, mouseY, button);
		}
		return false;
	}
	
	default boolean handleOverlaysMouseScrolled(double mouseX, double mouseY, double amount) {
		final SortedOverlayCollection sortedOverlays = getSortedOverlays();
		for (OverlayTicket ticket : sortedOverlays.descending()) {
			if (ticket.area.contains(mouseX, mouseY) &&
			    ticket.renderer.overlayMouseScrolled(ticket.area, mouseX, mouseY, amount))
				return true;
		}
		return false;
	}
	
	default boolean handleOverlaysMouseDragged(
	  double mouseX, double mouseY, int button, double dragX, double dragY
	) {
		final SortedOverlayCollection sortedOverlays = getSortedOverlays();
		final IOverlayRenderer dragTarget = sortedOverlays.getDragTarget();
		if (dragTarget != null) {
			final Optional<OverlayTicket> target =
			  sortedOverlays.stream().filter(ticket -> ticket.renderer == dragTarget).findFirst();
			if (target.isPresent()) {
				final OverlayTicket ticket = target.get();
				if (ticket.renderer.overlayMouseDragged(
				  ticket.area, mouseX, mouseY, button, dragX, dragY)) {
					return true;
				} else sortedOverlays.setDragTarget(null);
			}
		}
		return false;
	}
	
	default boolean handleOverlaysDragEnd(
	  double mouseX, double mouseY, int button
	) {
		final SortedOverlayCollection sortedOverlays = getSortedOverlays();
		final IOverlayRenderer dragTarget = sortedOverlays.getDragTarget();
		if (dragTarget != null) {
			sortedOverlays.setDragTarget(null);
			final Optional<OverlayTicket> target =
			  sortedOverlays.stream().filter(ticket -> ticket.renderer == dragTarget).findFirst();
			if (target.isPresent()) {
				final OverlayTicket ticket = target.get();
				ticket.renderer.overlayMouseDragEnd(ticket.area, mouseX, mouseY, button);
				return true;
			}
		}
		return false;
	}
	
	default boolean handleOverlaysMouseReleased(
	  double mouseX, double mouseY, int button
	) {
		if (handleOverlaysDragEnd(mouseX, mouseY, button))
			return true;
		final SortedOverlayCollection sortedOverlays = getSortedOverlays();
		for (OverlayTicket ticket : sortedOverlays.descending()) {
			if (ticket.area.contains(mouseX, mouseY)
			    && ticket.renderer.overlayMouseReleased(ticket.area, mouseX, mouseY, button))
				return true;
		}
		return false;
	}
	
	default void renderOverlays(PoseStack mStack, int mouseX, int mouseY, float delta) {
		final SortedOverlayCollection sortedOverlays = getSortedOverlays();
		final List<OverlayTicket> removed = new LinkedList<>();
		mStack.pushPose(); {
			mStack.translate(0D, 0D, 100D);
			Screen screen = Minecraft.getInstance().screen;
			IMultiTooltipScreen tScreen =
			  screen instanceof IMultiTooltipScreen? (IMultiTooltipScreen) screen : null;
			for (OverlayTicket ticket : sortedOverlays) {
				if (tScreen != null) tScreen.removeTooltips(ticket.area);
				ScissorsHandler.INSTANCE.pushScissor(ticket.area);
				if (!ticket.renderer.renderOverlay(mStack, ticket.area, mouseX, mouseY, delta))
					removed.add(ticket);
				ScissorsHandler.INSTANCE.popScissor();
			}
		} mStack.popPose();
		sortedOverlays.removeAll(removed);
	}
	
	default boolean shouldOverlaysSuppressHover(int mouseX, int mouseY) {
		return getSortedOverlays().stream().anyMatch(t -> t.area.contains(mouseX, mouseY));
	}
	
	/**
	 * Registration information for an overlay, used to determine the rendering order
	 * and mouse hit testing.
	 */
	class OverlayTicket implements Comparable<OverlayTicket> {
		public final int priority;
		protected final int tieBreaker;
		public final Rectangle area;
		public final IOverlayRenderer renderer;
		
		protected OverlayTicket(
		  int priority, int tieBreaker, Rectangle area, IOverlayRenderer renderer
		) {
			this.priority = priority;
			this.tieBreaker = tieBreaker;
			this.area = area;
			this.renderer = renderer;
		}
		
		@Override public int compareTo(
		  @NotNull IOverlayCapableContainer.OverlayTicket o
		) {
			int c = Integer.compare(priority, o.priority);
			if (c == 0)
				c = Integer.compare(tieBreaker, o.tieBreaker);
			return c;
		}
		
		@Override public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			OverlayTicket that = (OverlayTicket) o;
			return priority == that.priority && tieBreaker == that.tieBreaker;
		}
		
		@Override public int hashCode() {
			return Objects.hash(priority, tieBreaker);
		}
	}
	
	/**
	 * Sorted list containing all overlays in the order they should be rendered in.<br>
	 * Also supports a descending iterator, for the mouse event handling order.
	 */
	class SortedOverlayCollection implements Iterable<OverlayTicket> {
		int count = 0;
		protected TreeSet<OverlayTicket> tree = new TreeSet<>();
		@Nullable protected IOverlayRenderer dragTarget = null;
		
		public boolean add(Rectangle area, IOverlayRenderer renderer, int priority) {
			return tree.add(new OverlayTicket(priority, count++, area, renderer));
		}
		public boolean add(OverlayTicket ticket) {
			return tree.add(new OverlayTicket(ticket.priority, count++, ticket.area, ticket.renderer));
		}
		public boolean addAll(Collection<? extends OverlayTicket> collection) {
			return tree.addAll(collection);
		}
		
		public boolean remove(OverlayTicket ticket) {
			return tree.remove(ticket);
		}
		public boolean removeAll(Collection<? extends OverlayTicket> collection) {
			return tree.removeAll(collection);
		}
		public void clear() {
			tree.clear();
			count = 0;
		}
		
		public int size() {
			return tree.size();
		}
		public boolean isEmpty() {
			return tree.isEmpty();
		}
		@NotNull @Override public Iterator<OverlayTicket> iterator() {
			return tree.iterator();
		}
		public Iterable<OverlayTicket> descending() {
			return () -> tree.descendingIterator();
		}
		public Stream<OverlayTicket> stream() {
			return tree.stream();
		}
		
		public @Nullable IOverlayRenderer getDragTarget() {
			return dragTarget;
		}
		public void setDragTarget(@Nullable IOverlayRenderer dragTarget) {
			this.dragTarget = dragTarget;
		}
	}
}
