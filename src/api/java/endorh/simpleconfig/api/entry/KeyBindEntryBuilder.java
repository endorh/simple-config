package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.KeyEntryBuilder;
import endorh.simpleconfig.api.ui.hotkey.ExtendedKeyBind;
import endorh.simpleconfig.api.ui.hotkey.ExtendedKeyBindSettings;
import endorh.simpleconfig.api.ui.hotkey.KeyBindMapping;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface KeyBindEntryBuilder extends ConfigEntryBuilder<
  KeyBindMapping, String, KeyBindMapping, KeyBindEntryBuilder
>, KeyEntryBuilder<KeyBindMapping> {
	/**
	 * Configure the default keybind settings for this entry.
	 */
	@Contract(pure=true) @NotNull KeyBindEntryBuilder withDefaultSettings(ExtendedKeyBindSettings settings);
	
	/**
	 * Set the associated keybind for this entry, used for overlap reporting.
	 */
	@Contract(pure=true) @NotNull KeyBindEntryBuilder bakeTo(ExtendedKeyBind keyBind);
	
	/**
	 * Configure if the entry should report global overlaps with other keybinds.
	 */
	@Contract(pure=true) @NotNull KeyBindEntryBuilder reportOverlaps(boolean reportOverlaps);
	
	/**
	 * Replace this keybind's title with the title of this entry.
	 */
	@Contract(pure=true) @NotNull KeyBindEntryBuilder inheritTitle();
	
	/**
	 * Replace this keybind's title with the title of this entry.
	 */
	@Contract(pure=true) @NotNull KeyBindEntryBuilder inheritTitle(boolean inheritTitle);
}
