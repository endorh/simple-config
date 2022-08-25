package endorh.simpleconfig.api.ui.hotkey;

import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

class ExtendedKeyBindProxy {
	protected interface ExtendedKeyBindRegistrar {
		void registerProvider(ExtendedKeyBindProvider provider);
		void unregisterProvider(ExtendedKeyBindProvider provider);
	}
	protected interface ExtendedKeyBindFactory {
		ExtendedKeyBind create(@Nullable String modId, ITextComponent title, KeyBindMapping definition, Runnable action);
		KeyBindMapping parseMapping(String serialized);
		KeyBindMapping unsetMapping(ExtendedKeyBindSettings settings);
	}
	
	private static ExtendedKeyBindFactory factory;
	private static ExtendedKeyBindRegistrar registrar;
	protected static ExtendedKeyBindFactory getFactory() {
		if (factory != null) return factory;
		try {
			Class<?> cls = Class.forName("endorh.simpleconfig.ui.hotkey.ExtendedKeyBindImpl");
			Field field = cls.getDeclaredField("FACTORY");
			field.setAccessible(true);
			return factory = (ExtendedKeyBindFactory) field.get(null);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(
			  "Missing SimpleConfig runtime. One of your mods depends on " +
			  "Simple Config, which is not present.", e);
		} catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
			throw new RuntimeException(
			  "Error loading SimpleConfig runtime. You may report this bug to the Simple Config issue tracker.", e);
		}
	}
	
	protected static ExtendedKeyBindRegistrar getRegistrar() {
		if (registrar != null) return registrar;
		try {
			Class<?> cls = Class.forName("endorh.simpleconfig.ui.hotkey.ExtendedKeyBindDispatcher");
			Field field = cls.getDeclaredField("REGISTRAR");
			field.setAccessible(true);
			return registrar = (ExtendedKeyBindRegistrar) field.get(null);
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
