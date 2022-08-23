package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.KeyEntryBuilder;
import endorh.simpleconfig.api.ui.hotkey.ExtendedKeyBind;
import endorh.simpleconfig.api.ui.hotkey.ExtendedKeyBindSettings;
import endorh.simpleconfig.api.ui.hotkey.KeyBindMapping;

public interface KeyBindEntryBuilder extends ConfigEntryBuilder<
  KeyBindMapping, String, KeyBindMapping, KeyBindEntryBuilder
>, KeyEntryBuilder<KeyBindMapping> {
	/**
	 * Configure the default keybind settings for this entry.
	 */
	KeyBindEntryBuilder withDefaultSettings(ExtendedKeyBindSettings settings);
	
	/**
	 * Set the associated keybind for this entry, used for overlap reporting.
	 */
	KeyBindEntryBuilder bakeTo(ExtendedKeyBind keyBind);
	
	/**
	 * Configure if the entry should report global overlaps with other keybinds.
	 */
	KeyBindEntryBuilder reportOverlaps(boolean reportOverlaps);
	
	/**
	 * Replace this keybind's title with the title of this entry.
	 */
	KeyBindEntryBuilder inheritTitle();
	
	/**
	 * Replace this keybind's title with the title of this entry.
	 */
	KeyBindEntryBuilder inheritTitle(boolean inheritTitle);
}
