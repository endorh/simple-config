package endorh.simpleconfig.api.ui;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

class HighlighterManagerProxy {
	private static HighlighterManager manager;
	
	static @NotNull HighlighterManager getHighlighterManager() {
		if (manager != null) return manager;
		try {
			Class<?> cls = Class.forName("endorh.simpleconfig.highlight.HighlighterManagerImpl");
			Field inst = cls.getDeclaredField("INSTANCE");
			inst.setAccessible(true);
			return manager = (HighlighterManager) inst.get(null);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(
			  "Missing SimpleConfig runtime. One of your mods depends on " +
			  "Simple Config, which is not present.", e);
		} catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
			throw new RuntimeException(
			  "Error loading SimpleConfig runtime. You may report this bug to the Simple Config issue tracker.", e);
		}
	}
}
