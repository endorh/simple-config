package endorh.simpleconfig.clothconfig2.gui;

import com.google.common.collect.Lists;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.clothconfig2.api.AbstractConfigEntry;
import endorh.simpleconfig.clothconfig2.api.IExpandable;
import endorh.simpleconfig.clothconfig2.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.INestedGuiEventHandler;
import net.minecraft.client.gui.screen.Screen;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.max;
import static java.lang.Math.min;

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
	
	default boolean isNavigableSubTarget() {
		return false;
	}
	
	default List<INavigableTarget> getNavigableChildren() {
		return Lists.newArrayList(this);
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
	
	void applyFocusHighlight(int color, int length);
	default void applyHistoryHighlight() {
		applyFocusHighlight(0x804242FF);
	}
	default void applyMergeHighlight() {
		applyFocusHighlight(0x80603070);
	}
	default void applyErrorHighlight() {
		applyFocusHighlight(0x80FF4242);
	}
	default void applyFocusHighlight(int color) {
		applyFocusHighlight(color, 0);
	}
	
	default @Nullable AbstractConfigEntry<?> findParentEntry() {
		INavigableTarget target = this;
		while (!(target instanceof AbstractConfigEntry) && target != null)
			target = target.getNavigableParent();
		return (AbstractConfigEntry<?>) target;
	}
}
