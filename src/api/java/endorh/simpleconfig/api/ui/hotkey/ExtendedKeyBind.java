package endorh.simpleconfig.api.ui.hotkey;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Extended KeyBind, which supports arbitrary key combinations, and
 * a bunch of other {@link ExtendedKeyBindSettings}.<br><br>
 * <b>Do not implement</b>. Use the static factory methods, or you will
 * have to implement the dispatching as well.
 */
public interface ExtendedKeyBind {
	static @NotNull ExtendedKeyBind of(String modId, Component title, Runnable callback) {
		return of(modId, title, KeyBindMapping.unset(), callback);
	}
	
	static @NotNull ExtendedKeyBind of(
	  String modId, Component title, String definition, Runnable callback
	) {
		return of(modId, title, KeyBindMapping.parse(definition), callback);
	}
	
	static @NotNull ExtendedKeyBind of(
	  String modId, Component title, KeyBindMapping keyBind, Runnable callback
	) {
		return ExtendedKeyBindProxy.getFactory().create(modId, title, keyBind, callback);
	}
	
	static @NotNull ExtendedKeyBind of(String modId, String name, Runnable callback) {
		return of(modId, name, KeyBindMapping.unset(), callback);
	}
	
	static @NotNull ExtendedKeyBind of(
	  String modId, String name, String definition, Runnable callback
	) {
		return of(modId, name, KeyBindMapping.parse(definition), callback);
	}
	
	static @NotNull ExtendedKeyBind of(
	  String modId, String name, KeyBindMapping keyBind, Runnable callback
	) {
		return of(modId, Component.translatable(modId + ".keybind." + name), keyBind, callback);
	}
	
	@Nullable String getModId();
	
	@NotNull Component getTitle();
	void setTitle(@NotNull Component title);
	
	@NotNull KeyBindMapping getDefinition();
	void setDefinition(@NotNull KeyBindMapping keyBind);
	
	boolean isPressed();
	boolean isPhysicallyPressed();
	
	@Internal void trigger();
	@Internal void updatePressed(InputMatchingContext context);
	@Internal void onRepeat();
}
