package endorh.simpleconfig.ui.hotkey;

import endorh.simpleconfig.ui.hotkey.ExtendedKeyBindDispatcher.InputMatchingContext;
import endorh.simpleconfig.ui.hotkey.KeyBindMapping.KeyBindActivation;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class ExtendedKeyBind {
	private final String modId;
	private ITextComponent title;
	private KeyBindMapping keyBind;
	private @Nullable ITextComponent candidateName;
	private @Nullable KeyBindMapping candidateDefinition;
	private final Runnable callback;
	private boolean pressed = false;
	private boolean pressedToggle = false;
	
	public static ExtendedKeyBind of(String modId, ITextComponent title, Runnable callback) {
		return of(modId, title, KeyBindMapping.unset(), callback);
	}
	
	public static ExtendedKeyBind of(
	  String modId, ITextComponent title, String definition, Runnable callback
	) {
		return of(modId, title, KeyBindMapping.parse(definition), callback);
	}
	
	public static ExtendedKeyBind of(
	  String modId, ITextComponent title, KeyBindMapping keyBind, Runnable callback
	) {
		return new ExtendedKeyBind(modId, title, keyBind, callback);
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
	
	public ExtendedKeyBind(ITextComponent title, KeyBindMapping keyBind, Runnable callback) {
		this(null, title, keyBind, callback);
	}
	
	public ExtendedKeyBind(
	  @Nullable String modId, ITextComponent title,
	  KeyBindMapping keyBind, Runnable callback
	) {
		this.modId = modId;
		this.title = title;
		this.keyBind = keyBind;
		this.callback = callback;
	}
	
	public @Nullable String getModId() {
		return modId;
	}
	public ITextComponent getTitle() {
		return title;
	}
	public void setTitle(ITextComponent title) {
		this.title = title;
	}
	public KeyBindMapping getDefinition() {
		return keyBind;
	}
	public void setDefinition(KeyBindMapping keyBind) {
		this.keyBind = keyBind;
	}
	
	public ITextComponent getCandidateName() {
		return candidateName != null? candidateName : getTitle();
	}
	public void setCandidateName(@Nullable ITextComponent name) {
		candidateName = name;
	}
	public KeyBindMapping getCandidateDefinition() {
		return candidateDefinition != null? candidateDefinition : getDefinition();
	}
	public void setCandidateDefinition(@Nullable KeyBindMapping definition) {
		candidateDefinition = definition;
	}
	
	public boolean isPressed() {
		ExtendedKeyBindSettings settings = getDefinition().getSettings();
		KeyBindActivation activation = settings.getActivation();
		return activation == KeyBindActivation.TOGGLE
		       ? pressedToggle
		       : activation == KeyBindActivation.TOGGLE_RELEASE
		         ? !pressedToggle : pressed;
	}
	
	public boolean isPhysicallyPressed() {
		return pressed;
	}
	
	public boolean overlaps(ExtendedKeyBind other) {
		return getCandidateDefinition().overlaps(other.getCandidateDefinition());
	}
	
	@Internal public void trigger() {
		callback.run();
	}
	
	@Internal public void updatePressed(InputMatchingContext context) {
		KeyBindMapping keyBind = getDefinition();
		ExtendedKeyBindSettings settings = keyBind.getSettings();
		if (!settings.getContext().isActive()
		    || context.isCancelled()
		    || context.isTriggered() && settings.isExclusive()
		) {
			context.getRepeatableKeyBinds().remove(this);
			pressed = false;
			return;
		}
		IntList requiredKeys = keyBind.getRequiredKeys();
		Int2ObjectMap<String> requiredChars = keyBind.getCharMap();
		boolean matchByChar = settings.isMatchByChar() && requiredChars != null;
		if (requiredKeys.isEmpty()) {
			context.getRepeatableKeyBinds().remove(this);
			pressed = false;
			return;
		}
		
		boolean prevPressed = pressed;
		IntSet pressedKeys = context.getPressedKeys();
		IntList sortedKeys = context.getSortedPressedKeys();
		Int2ObjectMap<String> charMap = context.getCharMap();
		Set<String> pressedChars = context.getPressedChars();
		int pressedSize = pressedKeys.size();
		int requiredSize = requiredKeys.size();
		if (settings.isAllowExtraKeys()? pressedSize >= requiredSize : pressedSize == requiredSize) {
			if (matchByChar) {
				pressed = true;
				for (int key: requiredKeys) {
					String ch = requiredChars.get(key);
					if (ch != null? !pressedChars.contains(ch) : !pressedKeys.contains(key)) {
						pressed = false;
						break;
					}
				}
			} else pressed = pressedKeys.containsAll(requiredKeys);
			if (settings.isOrderSensitive()) {
				int pi = 0;
				int sizeDiff = pressedSize - requiredSize;
				//noinspection IfStatementWithIdenticalBranches
				if (matchByChar) match:for (int i = 0; i < requiredSize; i++) {
					int key = requiredKeys.getInt(i);
					String requiredChar = requiredChars.get(key);
					boolean isChar = requiredChar != null;
					while (pi <= sizeDiff + i) {
						int pressedKey = sortedKeys.getInt(pi++);
						String pressedChar = charMap.get(pressedKey);
						if (isChar? requiredChar.equals(pressedChar)
						          : pressedChar == null && key == pressedKey
						) continue match;
					}
					pressed = false;
					break;
				} else match:for (int i = 0; i < requiredSize; i++) {
					int key = requiredKeys.getInt(i);
					while (pi <= sizeDiff + i)
						if (key == sortedKeys.getInt(pi++))
							continue match;
					pressed = false;
					break;
				}
			}
		} else pressed = false;
		KeyBindActivation activation = settings.getActivation();
		if (!pressed) context.getRepeatableKeyBinds().remove(this);
		
		if (activation == KeyBindActivation.TOGGLE && pressed && !prevPressed
		    || activation == KeyBindActivation.TOGGLE_RELEASE && !pressed && prevPressed
		) pressedToggle = !pressedToggle;
		
		if (pressed == prevPressed) return;
		
		if (pressed && !context.isPreventFurther() && settings.isPreventFurther())
			context.setPreventFurther(true);
		
		if (activation == KeyBindActivation.BOTH
		    || activation == KeyBindActivation.TOGGLE && pressed && pressedToggle
		    || activation == KeyBindActivation.TOGGLE_RELEASE && !pressed && pressedToggle
		    || (activation == KeyBindActivation.PRESS
		        || activation == KeyBindActivation.REPEAT) && pressed
		    || activation == KeyBindActivation.RELEASE && !pressed
		) {
			trigger();
			context.setTriggered(true);
			if (activation == KeyBindActivation.REPEAT)
				context.getRepeatableKeyBinds().add(this);
		}
	}
	
	@Internal public void onRepeat() {
		ExtendedKeyBindSettings settings = getDefinition().getSettings();
		if (settings.getActivation() != KeyBindActivation.REPEAT || !isPhysicallyPressed()) return;
		trigger();
	}
	
	@Override public String toString() {
		return getTitle().getString() + ": " + getDefinition().getDisplayName().getString();
	}
}
