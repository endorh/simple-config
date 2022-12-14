package endorh.simpleconfig.ui.hotkey;

import endorh.simpleconfig.api.ui.hotkey.ExtendedKeyBind;
import endorh.simpleconfig.api.ui.hotkey.ExtendedKeyBindProxy.ExtendedKeyBindFactory;
import endorh.simpleconfig.api.ui.hotkey.ExtendedKeyBindSettings;
import endorh.simpleconfig.api.ui.hotkey.InputMatchingContext;
import endorh.simpleconfig.api.ui.hotkey.KeyBindMapping;
import endorh.simpleconfig.api.ui.hotkey.KeyBindMapping.KeyBindActivation;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class ExtendedKeyBindImpl implements ExtendedKeyBind {
	private final String modId;
	private @NotNull Component title;
	private @NotNull KeyBindMapping keyBind;
	private @Nullable Component candidateName;
	private @Nullable KeyBindMapping candidateDefinition;
	private final Runnable callback;
	private boolean pressed = false;
	private boolean pressedToggle = false;
	
	protected static final ExtendedKeyBindFactory FACTORY = new ExtendedKeyBindFactory() {
		@Override public @NotNull ExtendedKeyBind create(
		  @Nullable String modId, Component title, KeyBindMapping definition, Runnable action
		) {
			return new ExtendedKeyBindImpl(modId, title, definition, action);
		}
		
		@Override public @NotNull KeyBindMapping parseMapping(String serialized) {
			return KeyBindMappingImpl.parse(serialized);
		}
		
		@Override public @NotNull KeyBindMapping unsetMapping(ExtendedKeyBindSettings settings) {
			return KeyBindMappingImpl.unset(settings);
		}
	};
	
	public ExtendedKeyBindImpl(Component title, KeyBindMapping keyBind, Runnable callback) {
		this(null, title, keyBind, callback);
	}
	
	public ExtendedKeyBindImpl(
	  @Nullable String modId, @NotNull Component title,
	  @NotNull KeyBindMapping keyBind, Runnable callback
	) {
		this.modId = modId;
		this.title = title;
		this.keyBind = keyBind;
		this.callback = callback;
	}
	
	@Override public @Nullable String getModId() {
		return modId;
	}
	@Override public @NotNull Component getTitle() {
		return title;
	}
	@Override public void setTitle(@NotNull Component title) {
		this.title = title;
	}
	@Override public @NotNull KeyBindMapping getDefinition() {
		return keyBind;
	}
	@Override public void setDefinition(@NotNull KeyBindMapping keyBind) {
		this.keyBind = keyBind;
	}
	
	public Component getCandidateName() {
		return candidateName != null? candidateName : getTitle();
	}
	public void setCandidateName(@Nullable Component name) {
		candidateName = name;
	}
	public KeyBindMapping getCandidateDefinition() {
		return candidateDefinition != null? candidateDefinition : getDefinition();
	}
	public void setCandidateDefinition(@Nullable KeyBindMapping definition) {
		candidateDefinition = definition;
	}
	
	@Override public boolean isPressed() {
		ExtendedKeyBindSettings settings = getDefinition().getSettings();
		KeyBindActivation activation = settings.activation();
		return activation == KeyBindActivation.TOGGLE
		       ? pressedToggle
		       : activation == KeyBindActivation.TOGGLE_RELEASE
		         ? !pressedToggle : pressed;
	}
	
	@Override public boolean isPhysicallyPressed() {
		return pressed;
	}
	
	public boolean overlaps(ExtendedKeyBind other) {
		return getCandidateDefinition().overlaps(((ExtendedKeyBindImpl) other).getCandidateDefinition());
	}
	
	@Override @Internal public void trigger() {
		callback.run();
	}
	
	@Override @Internal public void updatePressed(InputMatchingContext context) {
		KeyBindMapping keyBind = getDefinition();
		ExtendedKeyBindSettings settings = keyBind.getSettings();
		if (!settings.context().isActive()
		    || context.isCancelled()
		    || context.isTriggered() && settings.exclusive()
		) {
			context.getRepeatableKeyBinds().remove(this);
			pressed = false;
			return;
		}
		IntList requiredKeys = keyBind.getRequiredKeys();
		Int2ObjectMap<String> requiredChars = keyBind.getCharMap();
		boolean matchByChar = settings.matchByChar() && requiredChars != null;
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
		if (settings.allowExtraKeys()? pressedSize >= requiredSize : pressedSize == requiredSize) {
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
			if (settings.orderSensitive()) {
				int pi = 0;
				int sizeDiff = pressedSize - requiredSize;
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
		KeyBindActivation activation = settings.activation();
		if (!pressed) context.getRepeatableKeyBinds().remove(this);
		
		if (activation == KeyBindActivation.TOGGLE && pressed && !prevPressed
		    || activation == KeyBindActivation.TOGGLE_RELEASE && !pressed && prevPressed
		) pressedToggle = !pressedToggle;
		
		if (pressed == prevPressed) return;
		
		if (pressed && !context.isPreventFurther() && settings.preventFurther())
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
	
	@Override @Internal public void onRepeat() {
		ExtendedKeyBindSettings settings = getDefinition().getSettings();
		if (settings.activation() != KeyBindActivation.REPEAT || !isPhysicallyPressed()) return;
		trigger();
	}
	
	@Override public String toString() {
		return getTitle().getString() + ": " + getDefinition().getDisplayName().getString();
	}
}
