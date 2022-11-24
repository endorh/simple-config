package endorh.simpleconfig.api;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

class SimpleConfigGUIManagerProxy {
	private static SimpleConfigGUIManager manager;
	
	static @NotNull SimpleConfigGUIManager getSimpleConfigGUIManager() {
		if (manager != null) return manager;
		try {
			Class<?> cls = Class.forName("endorh.simpleconfig.core.SimpleConfigGUIManagerImpl");
			Field inst = cls.getDeclaredField("INSTANCE");
			inst.setAccessible(true);
			return manager = (SimpleConfigGUIManager) inst.get(null);
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
