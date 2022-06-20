package endorh.simpleconfig.clothconfig2.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.clothconfig2.api.ScissorsHandler;
import endorh.simpleconfig.clothconfig2.math.Rectangle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

public interface IOverlayCapableScreen extends IMultiTooltipScreen {
	interface IOverlayRenderer {
		/**
		 * Render the overlay<br>
		 * @return The rectangle to render the overlay in the next frame,
		 *         or null if the overlay should not render in the next frame
		 */
		boolean renderOverlay(
		  MatrixStack mStack, Rectangle area, int mouseX, int mouseY, float delta
		);
		
		/**
		 * Called when a click event happens within an overlay
		 * @return True if the event is handled (if the overlay has transparent
		 *         areas, clicks should fall through them)
		 */
		default boolean overlayMouseClicked(double mouseX, double mouseY, int button) {
			return false;
		}
		
		/**
		 * Called when a click event happens outside this overlay<br>
		 * Useful to dismiss overlays when clicked outside them<br>
		 * Note that this method cannot stop propagation of the click event
		 */
		default void overlayMouseClickedOutside(double mouseX, double mouseY, int button) {}
		
		/**
		 * Called when a drag operation starts within the overlay and
		 * the overlay returns true for {@link IOverlayRenderer#isOverlayDragging()}
		 * @return False to end the drag
		 */
		default boolean overlayMouseDragged(
		  double mouseX, double mouseY, int button, double dragX, double dragY
		) {
			return false;
		}
		
		/**
		 * Called when a drag operation finishes
		 */
		default void overlayMouseDragEnd(double mouseX, double mouseY, int button) {}
		
		/**
		 * Called when the mouse is released over the overlay<br>
		 * Not really useful
		 */
		default boolean overlayMouseReleased(double mouseX, double mouseY, int button) {
			return false;
		}
		
		/**
		 * Called when the mouse is scrolled over the overlay<br>
		 * @return True if the scroll is consumed
		 */
		default boolean overlayMouseScrolled(double mouseX, double mouseY, double amount) {
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
	
	SortedOverlayCollection getSortedOverlays();
	
	void claimRectangle(Rectangle area, IOverlayRenderer overlayRenderer, int priority);
	default void claimRectangle(Rectangle area, IOverlayRenderer overlayRenderer) {
		claimRectangle(area, overlayRenderer, 0);
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
				if (ticket.renderer.overlayMouseClicked(mouseX, mouseY, button)) {
					if (ticket.renderer.isOverlayDragging())
						sortedOverlays.setDragTarget(ticket.renderer);
					return true;
				}
			} else ticket.renderer.overlayMouseClickedOutside(mouseX, mouseY, button);
		}
		return false;
	}
	
	default boolean handleOverlaysMouseScrolled(double mouseX, double mouseY, double amount) {
		final SortedOverlayCollection sortedOverlays = getSortedOverlays();
		for (OverlayTicket ticket : sortedOverlays.descending()) {
			if (ticket.area.contains(mouseX, mouseY) &&
			    ticket.renderer.overlayMouseScrolled(mouseX, mouseY, amount))
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
				if (target.get().renderer.overlayMouseDragged(mouseX, mouseY, button, dragX, dragY)) {
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
				target.get().renderer.overlayMouseDragEnd(mouseX, mouseY, button);
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
			    && ticket.renderer.overlayMouseReleased(mouseX, mouseY, button))
				return true;
		}
		return false;
	}
	
	default void renderOverlays(MatrixStack mStack, int mouseX, int mouseY, float delta) {
		final SortedOverlayCollection sortedOverlays = getSortedOverlays();
		final List<OverlayTicket> removed = new LinkedList<>();
		mStack.pushPose(); {
			mStack.translate(0D, 0D, 100D);
			for (OverlayTicket ticket : sortedOverlays) {
				removeTooltips(ticket.area);
				ScissorsHandler.INSTANCE.scissor(ticket.area);
				if (!ticket.renderer.renderOverlay(mStack, ticket.area, mouseX, mouseY, delta))
					removed.add(ticket);
				ScissorsHandler.INSTANCE.removeLastScissor();
			}
		} mStack.popPose();
		sortedOverlays.removeAll(removed);
	}
	
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
		  @NotNull IOverlayCapableScreen.OverlayTicket o
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
	
	default boolean shouldOverlaysSuppressHover(int mouseX, int mouseY) {
		return getSortedOverlays().stream().anyMatch(t -> t.area.contains(mouseX, mouseY));
	}
	
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
		
		public void setDragTarget(
		  @Nullable IOverlayRenderer dragTarget
		) {
			this.dragTarget = dragTarget;
		}
	}
}
