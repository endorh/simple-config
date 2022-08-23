package endorh.simpleconfig.api.ui.hotkey;

import endorh.simpleconfig.api.ui.hotkey.KeyBindMapping.KeyBindActivation;
import endorh.simpleconfig.api.ui.hotkey.KeyBindMapping.KeyBindContext;

record ExtendedKeyBindSettingsImpl(
  KeyBindActivation activation,
  KeyBindContext context,
  boolean allowExtraKeys,
  boolean orderSensitive,
  boolean exclusive,
  boolean matchByChar,
  boolean preventFurther
) implements ExtendedKeyBindSettings {
	@Override public ExtendedKeyBindSettings copy() {
		return new ExtendedKeyBindSettingsBuilder(this).build();
	}
}
