package endorh.simpleconfig.ui.api;

import com.google.common.collect.Lists;
import endorh.simpleconfig.api.ui.math.Rectangle;
import net.minecraft.client.gui.INestedGuiEventHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Designed for row based interfaces
 */
public interface INavigableTarget extends INestedGuiEventHandler {
	void navigate();
	
	default boolean isNavigable() {
		return true;
	}
	
	Rectangle getNavigableArea();
	Rectangle getRowArea();
	
	default @Nullable INavigableTarget getLastSelectedNavigableSubTarget() {
		return null;
	}
	default void setLastSelectedNavigableSubTarget(@Nullable INavigableTarget target) {}
	
	default @Nullable INavigableTarget getNavigableParent() {
		return null;
	}
	
	default List<INavigableTarget> getNavigableChildren(boolean onlyVisible) {
		return Collections.emptyList();
	}
	
	default List<INavigableTarget> getNavigableDescendants(boolean onlyVisible) {
		return getNavigableChildren(onlyVisible).stream().flatMap(e -> Stream.concat(
		  Stream.of(e), e.getNavigableDescendants(onlyVisible).stream()
		)).collect(Collectors.toList());
	}
	
	default List<INavigableTarget> getNavigableDescendantsAndSubDescendants(boolean onlyVisible) {
		return getNavigableChildren(onlyVisible).stream().flatMap(e -> Stream.of(
		  Stream.of(e), e.getNavigableSubTargets().stream(),
		  e.getNavigableDescendantsAndSubDescendants(onlyVisible).stream()
		)).flatMap(Function.identity()).collect(Collectors.toList());
	}
	
	default boolean isNavigableSubTarget() {
		return false;
	}
	
	default List<INavigableTarget> getNavigableSubTargets() {
		return Lists.newArrayList();
	}
	
	default INavigableTarget selectClosestTarget(
	  List<INavigableTarget> targets, @Nullable INavigableTarget hint
	) {
		Rectangle area = getNavigableArea();
		if (hint != null) {
			Rectangle hintArea = hint.getNavigableArea();
			INavigableTarget closest = targets.stream().max(Comparator.comparingInt(
			  r -> r.getNavigableArea().horizontalIntersection(hintArea)
			)).orElse(null);
			if (closest != null && closest.getNavigableArea().horizontalIntersection(area) > 0)
				return closest;
		}
		return targets.stream().max(Comparator.comparingInt(
		  r -> r.getNavigableArea().horizontalIntersection(area)
		)).orElse(null);
	}
	
	default boolean handleNavigationKey(int keyCode, int scanCode, int modifiers) {
		return false;
	}
	
	/**
	 * For common colors, see {@link HighlightColors}
	 */
	void applyFocusHighlight(int color, int length);
	/**
	 * For common colors, see {@link HighlightColors}
	 */
	default void applyFocusHighlight(int color) {
		applyFocusHighlight(color, 0);
	}
	
	default @Nullable AbstractConfigField<?> findParentEntry() {
		INavigableTarget target = this;
		while (!(target instanceof AbstractConfigField) && target != null)
			target = target.getNavigableParent();
		return (AbstractConfigField<?>) target;
	}
	
	final class HighlightColors {
		private HighlightColors() {}
		
		public static int HISTORY = 0x804242FF;
		public static int MERGE = 0x80603070;
		public static int WARNING = 0xA0646448;
		public static int ERROR = 0x80FF4242;
		public static int EDITED = 0x80A0A0A0;
	}
}
