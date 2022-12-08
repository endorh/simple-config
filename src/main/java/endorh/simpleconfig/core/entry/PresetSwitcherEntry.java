package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.PresetSwitcherEntryBuilder;
import endorh.simpleconfig.core.AbstractSimpleConfigEntryHolder;
import endorh.simpleconfig.core.DummyEntryHolder;
import endorh.simpleconfig.core.EntryType;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.gui.AbstractConfigScreen;
import endorh.simpleconfig.ui.impl.builders.EntryButtonFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;

import static endorh.simpleconfig.api.ConfigBuilderFactoryProxy.string;

public class PresetSwitcherEntry extends GUIOnlyEntry<String, String, PresetSwitcherEntry> {
	private static final Logger LOGGER = LogManager.getLogger();
	
	protected StringEntry inner;
	protected Map<String, Map<String, Object>> presets;
	protected boolean global;
	protected String path;
	
	public PresetSwitcherEntry(
	  ConfigEntryHolder parent, String name,
	  Map<String, Map<String, Object>> presets, String path, boolean global
	) {
		super(parent, name, firstKey(presets), false, String.class);
		if (!(parent instanceof AbstractSimpleConfigEntryHolder))
			throw new IllegalArgumentException("Invalid parent for Preset Switcher Entry");
		this.presets = presets;
		this.path = path;
		this.global = global;
		inner = DummyEntryHolder.build(parent, string(firstKey(presets))
		  .restrict(new ArrayList<>(presets.keySet())
		));
	}
	
	public static class Builder extends GUIOnlyEntry.Builder<String, String, PresetSwitcherEntry, PresetSwitcherEntryBuilder, Builder>
	  implements PresetSwitcherEntryBuilder {
		
		protected Map<String, Map<String, Object>> presets;
		protected String path;
		protected boolean global;
		
		public Builder(Map<String, Map<String, Object>> presets, String path, boolean global) {
			super(firstKey(presets), EntryType.of(String.class));
			this.presets = presets;
			this.path = path;
			this.global = global;
		}
		
		@Override protected PresetSwitcherEntry buildEntry(ConfigEntryHolder parent, String name) {
			return new PresetSwitcherEntry(parent, name, presets, path, global);
		}
		
		@Override protected Builder createCopy(String value) {
			return new Builder(new HashMap<>(presets), path, global);
		}
	}
	
	protected static String firstKey(Map<String, Map<String, Object>> presets) {
		return presets.keySet().stream().findFirst().orElseThrow(
		  () -> new IllegalArgumentException("At least one preset must be specified"));
	}
	
	@OnlyIn(Dist.CLIENT) public void applyPreset(String name) {
		if (!presets.containsKey(name))
			throw new IllegalArgumentException("Unknown preset: \"" + name + "\"");
		final Map<String, Object> preset = presets.get(name);
		final ConfigEntryHolder h =
		  (global ? parent.getRoot() : (AbstractSimpleConfigEntryHolder) parent).getChild(path);
		AbstractConfigListEntry<String> guiEntry = getGuiEntry();
		if (guiEntry != null) {
			AbstractConfigScreen screen = guiEntry.getScreen();
			screen.runAtomicTransparentAction(() -> {
				for (Entry<String, Object> entry : preset.entrySet()) {
					try {
						h.setForGUI(entry.getKey(), entry.getValue());
					} catch (RuntimeException e) {
						LOGGER.warn(
						  "Unable to set preset (" + name + ") entry: \"" + entry.getKey() + "\"\n" +
						  "Details: " + e.getClass().getSimpleName() + ": " + e.getLocalizedMessage());
					}
				}
			});
		}
	}
	
	@SuppressWarnings("unchecked") public <E extends AbstractConfigListEntry<String> & IChildListEntry,
	  B extends FieldBuilder<String, E, B>
	> EntryButtonFieldBuilder<String, E, B> makeGUIEntry(
	  ConfigFieldBuilder builder, FieldBuilder<String, ?, ?> entryBuilder, Consumer<String> action
	) {
		return builder.startButton(getDisplayName(), (B) entryBuilder, action);
	}
	
	@OnlyIn(Dist.CLIENT) @Override public Optional<FieldBuilder<String, ?, ?>> buildGUIEntry(
	  ConfigFieldBuilder builder
	) {
		EntryButtonFieldBuilder<String, ?, ?> entryBuilder = makeGUIEntry(
		  builder, inner.buildAtomicChildGUIEntry(builder), this::applyPreset)
		  .withButtonLabel(() -> Component.translatable("simpleconfig.label.preset.apply"));
		return Optional.of(entryBuilder);
	}
}
