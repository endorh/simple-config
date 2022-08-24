package endorh.simpleconfig.ui.hotkey;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.Set;

public interface InputMatchingContext {
	IntList getSortedPressedKeys();
	IntSet getPressedKeys();
	
	Int2ObjectMap<String> getCharMap();
	Set<String> getPressedChars();
	
	Set<ExtendedKeyBind> getRepeatableKeyBinds();
	
	boolean isTriggered();
	void setTriggered(boolean triggered);
	
	boolean isPreventFurther();
	void setPreventFurther(boolean prevent);
	
	boolean isCancelled();
	void setCancelled(boolean cancelled);
}
