package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.KeyBindEntryBuilder;
import endorh.simpleconfig.api.ui.hotkey.ExtendedKeyBind;
import endorh.simpleconfig.api.ui.hotkey.ExtendedKeyBindProvider;
import endorh.simpleconfig.api.ui.hotkey.ExtendedKeyBindSettings;
import endorh.simpleconfig.api.ui.hotkey.KeyBindMapping;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.AbstractConfigEntryBuilder;
import endorh.simpleconfig.core.IKeyEntry;
import endorh.simpleconfig.core.SimpleConfigGUIManager;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.hotkey.ExtendedKeyBindDispatcher;
import endorh.simpleconfig.ui.hotkey.ExtendedKeyBindImpl;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import endorh.simpleconfig.ui.impl.builders.KeyBindFieldBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


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
	private static final Int2ObjectMap<List<ExtendedKeyBind>> UNBOUND = new Int2ObjectOpenHashMap<>();
	private static List<ExtendedKeyBind> getUnbound() {
		int session = SimpleConfigGUIManager.getGuiSession();
		UNBOUND.keySet().removeIf((int i) -> i < session - 1);
		return UNBOUND.computeIfAbsent(session, s -> new ArrayList<>());
	}
	private static final ExtendedKeyBindProvider UNBOUND_PROVIDER = new ExtendedKeyBindProvider() {
		@Override public @NotNull Iterable<ExtendedKeyBind> getActiveKeyBinds() {
			return Collections.emptyList();
		}
		@Override public @NotNull Iterable<ExtendedKeyBind> getAllKeyBinds() {
			return getUnbound();
		}
		@Override public int getPriority() {
			return -1000; // Low priority
		}
	};
	static { ExtendedKeyBindDispatcher.registerProvider(UNBOUND_PROVIDER); }
	
	protected ExtendedKeyBindSettings defaultSettings;
	protected @Nullable ExtendedKeyBindImpl keyBind;
	protected boolean reportOverlaps = true;
	private ExtendedKeyBind pendingKeyBind = null;
	
	public KeyBindEntry(ConfigEntryHolder parent, String name, KeyBindMapping value) {
		super(parent, name, value);
	}
	
	public static class Builder extends AbstractConfigEntryBuilder<
	  KeyBindMapping, String, KeyBindMapping, KeyBindEntry, KeyBindEntryBuilder, Builder
	> implements KeyBindEntryBuilder {
		protected ExtendedKeyBindSettings defaultSettings = ExtendedKeyBindSettings.ingame().build();
		protected @Nullable ExtendedKeyBindImpl keyBind = null;
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
		
		@Override public @NotNull Builder withDefaultSettings(ExtendedKeyBindSettings settings) {
			Builder copy = copy();
			copy.defaultSettings = settings;
			return copy;
		}
		
		@Override public @NotNull Builder bakeTo(ExtendedKeyBind keyBind) {
			Builder copy = copy();
			if (!(keyBind instanceof ExtendedKeyBindImpl)) throw new IllegalArgumentException(
			  "Keybind is not instance of ExtendedKeyBindImpl");
			copy.keyBind = (ExtendedKeyBindImpl) keyBind;
			return copy;
		}
		
		@Override public @NotNull Builder reportOverlaps(boolean reportOverlaps) {
			Builder copy = copy();
			copy.reportOverlaps = reportOverlaps;
			return copy;
		}
		
		@Override public @NotNull Builder inheritTitle() {
			return inheritTitle(true);
		}
		
		@Override public @NotNull Builder inheritTitle(boolean inheritTitle) {
			Builder copy = copy();
			copy.inheritTitle = inheritTitle;
			return copy;
		}
		
		@Override protected KeyBindEntry buildEntry(ConfigEntryHolder parent, String name) {
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
			getUnbound().add(pendingKeyBind);
			pendingKeyBind = null;
		}
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
	
	protected @Nullable ExtendedKeyBindImpl createFallbackKeyBind() {
		ExtendedKeyBindImpl keyBind = new ExtendedKeyBindImpl(
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
	public Optional<FieldBuilder<KeyBindMapping, ?, ?>> buildGUIEntry(ConfigFieldBuilder builder) {
		final KeyBindFieldBuilder valBuilder = builder
		  .startKeyBindField(getDisplayName(), forGui(get()))
		  .setAssociatedKeyBind(keyBind != null? keyBind : createFallbackKeyBind())
		  .setReportOverlaps(reportOverlaps)
		  .setDefaultSettings(defaultSettings);
		return Optional.of(decorate(valBuilder));
	}
}
