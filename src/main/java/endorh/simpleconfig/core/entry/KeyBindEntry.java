package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.AbstractConfigEntryBuilder;
import endorh.simpleconfig.core.IKeyEntry;
import endorh.simpleconfig.core.ISimpleConfigEntryHolder;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.hotkey.ExtendedKeyBind;
import endorh.simpleconfig.ui.hotkey.ExtendedKeyBindDispatcher;
import endorh.simpleconfig.ui.hotkey.ExtendedKeyBindDispatcher.ExtendedKeyBindProvider;
import endorh.simpleconfig.ui.hotkey.ExtendedKeyBindSettings;
import endorh.simpleconfig.ui.hotkey.KeyBindMapping;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import endorh.simpleconfig.ui.impl.builders.KeyBindFieldBuilder;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.*;


/**
 * Key binding entry. Supports advanced key combinations, and other advanced
 * settings such as exclusivity, order sensitivity, activation on release/repeat/toggle.<br>
 * Register extended keybinds by registering an {@link ExtendedKeyBindProvider} for them
 * using {@link ExtendedKeyBindDispatcher#registerProvider(ExtendedKeyBindProvider)}<br><br>
 * <b>Consider registering regular {@link KeyBinding}s through
 * {@link net.minecraftforge.fml.client.registry.ClientRegistry#registerKeyBinding(KeyBinding)}
 * </b><br>
 */
@OnlyIn(Dist.CLIENT)
public class KeyBindEntry extends AbstractConfigEntry<
  KeyBindMapping, String, KeyBindMapping
> implements IKeyEntry<KeyBindMapping> {
	// Give entries without an assigned keybind a fallback keybind that can be used
	//   to identify them for overlap detection.
	private static final Map<AbstractConfigListEntry<?>, ExtendedKeyBind> UNBOUND_KEYBINDS = new HashMap<>();
	private static final ExtendedKeyBindProvider UNBOUND_PROVIDER = new ExtendedKeyBindProvider() {
		@Override public Iterable<ExtendedKeyBind> getActiveKeyBinds() {
			return Collections.emptyList();
		}
		@Override public Iterable<ExtendedKeyBind> getAllKeyBinds() {
			return UNBOUND_KEYBINDS.values();
		}
		@Override public int getPriority() {
			return -1000; // Low priority
		}
	};
	static { ExtendedKeyBindDispatcher.registerProvider(UNBOUND_PROVIDER); }
	
	protected ExtendedKeyBindSettings defaultSettings;
	protected @Nullable ExtendedKeyBind keyBind;
	protected boolean reportOverlaps = true;
	private ExtendedKeyBind pendingKeyBind = null;
	
	public KeyBindEntry(ISimpleConfigEntryHolder parent, String name, KeyBindMapping value) {
		super(parent, name, value);
	}
	
	public static class Builder extends AbstractConfigEntryBuilder<
	  KeyBindMapping, String, KeyBindMapping, KeyBindEntry, Builder
	> {
		protected ExtendedKeyBindSettings defaultSettings = ExtendedKeyBindSettings.ingame().build();
		protected @Nullable ExtendedKeyBind keyBind = null;
		protected boolean reportOverlaps = true;
		protected boolean inheritTitle = false;
		
		public Builder() {
			this(KeyBindMapping.unset());
		}
		
		public Builder(String key) {
			this(KeyBindMapping.parse(key));
		}
		
		public Builder(KeyBindMapping value) {
			super(value, KeyBindMapping.class);
		}
		
		/**
		 * Configure the default keybind settings for this entry.
		 */
		public Builder withDefaultSettings(ExtendedKeyBindSettings settings) {
			Builder copy = copy();
			copy.defaultSettings = settings;
			return copy;
		}
		
		/**
		 * Set the associated keybind for this entry, used for overlap reporting.
		 */
		public Builder bakeTo(ExtendedKeyBind keyBind) {
			Builder copy = copy();
			copy.keyBind = keyBind;
			return copy;
		}
		
		/**
		 * Configure if the entry should report global overlaps with other keybinds.
		 */
		public Builder reportOverlaps(boolean reportOverlaps) {
			Builder copy = copy();
			copy.reportOverlaps = reportOverlaps;
			return copy;
		}
		
		/**
		 * Replace this' keybind's title with the title of this entry.
		 */
		public Builder inheritTitle() {
			return inheritTitle(true);
		}
		
		/**
		 * Replace this' keybind's title with the title of this entry.
		 */
		public Builder inheritTitle(boolean inheritTitle) {
			Builder copy = copy();
			copy.inheritTitle = inheritTitle;
			return copy;
		}
		
		@Override protected KeyBindEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			final KeyBindEntry entry = new KeyBindEntry(parent, name, value);
			entry.defaultSettings = defaultSettings;
			entry.keyBind = keyBind;
			entry.reportOverlaps = reportOverlaps;
			if (inheritTitle && keyBind != null)
				keyBind.setTitle(entry.getDisplayName());
			return entry;
		}
		
		@Override protected Builder createCopy() {
			final Builder copy = new Builder(value.copy());
			copy.defaultSettings = defaultSettings;
			copy.keyBind = keyBind;
			copy.reportOverlaps = reportOverlaps;
			return copy;
		}
	}
	
	@Override public String forConfig(KeyBindMapping value) {
		return value != null ? value.serialize() : "";
	}
	
	@Override public KeyBindMapping fromConfig(@Nullable String value) {
		return value == null? null : KeyBindMapping.parse(value);
	}
	
	@Override protected void setGuiEntry(@Nullable AbstractConfigListEntry<KeyBindMapping> guiEntry) {
		if (guiEntry != null && pendingKeyBind != null) {
			UNBOUND_KEYBINDS.put(guiEntry, pendingKeyBind);
			pendingKeyBind = null;
		} else if (guiEntry == null)
			UNBOUND_KEYBINDS.remove(getGuiEntry(false));
		super.setGuiEntry(guiEntry);
		if (keyBind != null) keyBind.setCandidateDefinition(null);
	}
	
	protected String getTypeComment() {
		return "Hotkey";
	}
	
	protected String getFormatComment() {
		return "!key+\"char\"+mouse.left!>:press#game";
	}
	
	@Override public List<String> getConfigCommentTooltips() {
		List<String> tooltips = super.getConfigCommentTooltips();
		tooltips.add(getTypeComment() + ": " + getFormatComment());
		return tooltips;
	}
	
	protected @Nullable ExtendedKeyBind createFallbackKeyBind() {
		ExtendedKeyBind keyBind = new ExtendedKeyBind(
		  parent.getRoot().getModId(), getDisplayName(),
		  get(), () -> {});
		pendingKeyBind = keyBind;
		return keyBind;
	}
	
	@Override protected void bakeField() {
		super.bakeField();
		if (keyBind != null) keyBind.setDefinition(get());
	}
	
	@OnlyIn(Dist.CLIENT) @Override
	public Optional<FieldBuilder<KeyBindMapping, ?, ?>> buildGUIEntry(ConfigEntryBuilder builder) {
		final KeyBindFieldBuilder valBuilder = builder
		  .startKeyBindField(getDisplayName(), forGui(get()))
		  .setAssociatedKeyBind(keyBind != null? keyBind : createFallbackKeyBind())
		  .setReportOverlaps(reportOverlaps)
		  .setDefaultSettings(defaultSettings);
		return Optional.of(decorate(valBuilder));
	}
}
