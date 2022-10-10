package endorh.simpleconfig.api.ui.hotkey;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public interface InputMatchingContext {
	@NotNull IntList getSortedPressedKeys();
	@NotNull IntSet getPressedKeys();
	
	@NotNull Int2ObjectMap<String> getCharMap();
	@NotNull Set<String> getPressedChars();
	
	@NotNull Set<ExtendedKeyBind> getRepeatableKeyBinds();
	
	boolean isTriggered();
	void setTriggered(boolean triggered);
	
	boolean isPreventFurther();
	void setPreventFurther(boolean prevent);
	
	boolean isCancelled();
	void setCancelled(boolean cancelled);
}
