package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.gui.entries.KeyBindListEntry;
import endorh.simpleconfig.ui.hotkey.ExtendedKeyBindImpl;
import endorh.simpleconfig.api.ui.hotkey.ExtendedKeyBindSettings;
import endorh.simpleconfig.api.ui.hotkey.KeyBindMapping;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@OnlyIn(value = Dist.CLIENT)
public class KeyBindFieldBuilder
  extends FieldBuilder<KeyBindMapping, KeyBindListEntry, KeyBindFieldBuilder> {
	private ExtendedKeyBindSettings defaultSettings;
	private @Nullable ExtendedKeyBindImpl keyBind;
	private boolean reportOverlaps;
	
	public KeyBindFieldBuilder(
	  ConfigFieldBuilder builder, ITextComponent name, KeyBindMapping value
	) {
		super(KeyBindListEntry.class, builder, name, value.copy());
	}
	
	public KeyBindFieldBuilder setDefaultSettings(ExtendedKeyBindSettings settings) {
		defaultSettings = settings;
		return this;
	}
	
	/**
	 * The associated keybind is used for overlap reporting.
	 */
	public KeyBindFieldBuilder setAssociatedKeyBind(ExtendedKeyBindImpl keyBind) {
		this.keyBind = keyBind;
		return this;
	}
	
	/**
	 * Configure if the entry should report overlaps with other hotkeys.
	 */
	public KeyBindFieldBuilder setReportOverlaps(boolean reportOverlaps) {
		this.reportOverlaps = reportOverlaps;
		return this;
	}
	
	@Override protected @NotNull KeyBindListEntry buildEntry() {
		KeyBindListEntry entry = new KeyBindListEntry(fieldNameKey, value, keyBind);
		entry.setReportOverlaps(reportOverlaps);
		entry.setDefaultSettings(defaultSettings);
		return entry;
	}
}
