package endorh.simpleconfig.ui.hotkey;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

/**
 * Extended KeyBind, which supports arbitrary key combinations, and
 * a bunch of other {@link ExtendedKeyBindSettings}.<br><br>
 * <b>Do not implement</b>. Use the static factory methods, or you will
 * have to implement the dispatching as well.
 */
public interface ExtendedKeyBind {
	static ExtendedKeyBind of(String modId, ITextComponent title, Runnable callback) {
		return of(modId, title, KeyBindMapping.unset(), callback);
	}
	
	static ExtendedKeyBind of(
	  String modId, ITextComponent title, String definition, Runnable callback
	) {
		return of(modId, title, KeyBindMapping.parse(definition), callback);
	}
	
	static ExtendedKeyBind of(
	  String modId, ITextComponent title, KeyBindMapping keyBind, Runnable callback
	) {
		return ExtendedKeyBindProxy.getFactory().create(modId, title, keyBind, callback);
	}
	
	public static ExtendedKeyBind of(String modId, String name, Runnable callback) {
		return of(modId, name, KeyBindMapping.unset(), callback);
	}
	
	public static ExtendedKeyBind of(
	  String modId, String name, String definition, Runnable callback
	) {
		return of(modId, name, KeyBindMapping.parse(definition), callback);
	}
	
	public static ExtendedKeyBind of(
	  String modId, String name, KeyBindMapping keyBind, Runnable callback
	) {
		return of(modId, new TranslationTextComponent(
		  modId + ".keybind." + name
		), keyBind, callback);
	}
	
	@Nullable String getModId();
	
	ITextComponent getTitle();
	void setTitle(ITextComponent title);
	
	KeyBindMapping getDefinition();
	void setDefinition(KeyBindMapping keyBind);
	
	boolean isPressed();
	boolean isPhysicallyPressed();
	
	@Internal void trigger();
	@Internal void updatePressed(InputMatchingContext context);
	@Internal void onRepeat();
}
