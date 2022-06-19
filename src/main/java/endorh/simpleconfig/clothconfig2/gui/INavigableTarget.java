package endorh.simpleconfig.clothconfig2.gui;

import com.google.common.collect.Lists;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.clothconfig2.api.AbstractConfigEntry;
import endorh.simpleconfig.clothconfig2.api.AbstractConfigEntry.EntryError;
import endorh.simpleconfig.clothconfig2.api.IExpandable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.screen.Screen;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public interface INavigableTarget {
	default @Nullable INavigableTarget getNavigableParent() {
		return null;
	}
	
	default List<INavigableTarget> getNavigableChildren() {
		return Lists.newArrayList(this);
	}
	
	void onNavigate();
	
	default boolean handleNavigationKey(int keyCode, int scanCode, int modifiers) {
		if (keyCode == 263 && this instanceof IExpandable) { // Left
			final IExpandable ex = (IExpandable) this;
			if (ex.isExpanded()) {
				ex.setExpanded(false, Screen.hasShiftDown());
				Minecraft.getInstance().getSoundHandler().play(
				  SimpleSound.master(SimpleConfigMod.UI_TAP, 1F));
				return true;
			}
		}
		return false;
	}
	
	default List<EntryError> getErrors() {
		final LinkedList<EntryError> errors = getNavigableChildren().stream()
		  .filter(t -> t != this).map(INavigableTarget::getError)
		  .filter(Optional::isPresent).map(Optional::get)
		  .collect(Collectors.toCollection(LinkedList::new));
		getError().ifPresent(e -> errors.add(0, e));
		return errors;
	}
	
	default boolean hasErrors() {
		return !getErrors().isEmpty();
	}
	
	/**
	 * Get the error of this target
	 */
	default Optional<EntryError> getError() {
		return Optional.empty();
	}
	
	default @Nullable AbstractConfigEntry<?> findParentEntry() {
		INavigableTarget target = this;
		while (!(target instanceof AbstractConfigEntry) && target != null)
			target = target.getNavigableParent();
		return (AbstractConfigEntry<?>) target;
	}
}
