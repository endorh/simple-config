package endorh.simpleconfig.ui.hotkey;

import com.electronwill.nightconfig.core.file.FileWatcher;
import com.google.common.collect.Lists;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.core.PairList;
import endorh.simpleconfig.core.SimpleConfigPaths;
import endorh.simpleconfig.ui.api.ModifierKeyCode;
import endorh.simpleconfig.yaml.SimpleConfigCommentedYamlFormat;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(value=Dist.CLIENT, modid=SimpleConfigMod.MOD_ID, bus=EventBusSubscriber.Bus.MOD)
public class ConfigHotKeyManager {
	private static final Logger LOGGER = LogManager.getLogger();
	public static final ConfigHotKeyManager INSTANCE = new ConfigHotKeyManager();
	
	private final File configHotKeysFile = SimpleConfigPaths.CLIENT_CONFIG_DIR
	  .resolve("simpleconfig-hotkeys.yaml").toFile();
	
	private List<IConfigHotKey> hotKeys = Lists.newArrayList();
	private ConfigHotKeyGroup group = new ConfigHotKeyGroup();
	private List<ConfigHotKeyGroup> defaultGroupQueue = new ArrayList<>();
	private final Set<String> addedDefaultGroups = new HashSet<>();
	
	private ConfigHotKeyManager() {}
	
	@SubscribeEvent public static void init(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			File file = INSTANCE.configHotKeysFile;
			try {
				if (!file.isFile()) {
					file.getParentFile().mkdirs();
					file.createNewFile();
				}
				FileWatcher.defaultInstance().addWatch(file, () -> {
					LOGGER.info("SimpleConfigHotKeyManager: Config hotkeys file changed, reloading.");
					INSTANCE.loadHotkeys();
				});
			} catch (IOException e) {
				LOGGER.error("I/O Error accessing config hotkeys file \"" + file + "\"", e);
			}
			INSTANCE.loadHotkeys();
			List<ConfigHotKeyGroup> queue = INSTANCE.defaultGroupQueue;
			if (queue != null) {
				INSTANCE.defaultGroupQueue = null;
				queue.forEach(INSTANCE::addDefaultGroup);
			}
		});
	}
	
	public void addDefaultGroup(ConfigHotKeyGroup group) {
		if (defaultGroupQueue != null) {
			defaultGroupQueue.add(group);
		} else {
			if (!addedDefaultGroups.contains(group.getName())) {
				this.group.addEntry(group);
				addedDefaultGroups.add(group.getName());
			}
			saveHotkeys();
		}
	}
	
	public void saveHotkeys() {
		Yaml yaml = SimpleConfigCommentedYamlFormat.getDefaultYaml();
		File file = configHotKeysFile;
		file.getParentFile().mkdirs();
		try (FileOutputStream os = new FileOutputStream(file)) {
			OutputStreamWriter writer = new OutputStreamWriter(os, StandardCharsets.UTF_8);
			Map<String, Object> serialized = new LinkedHashMap<>();
			serialized.put("entries", group.serialize().get("entries"));
			serialized.put("added_default_groups", addedDefaultGroups);
			yaml.dump(serialized, writer);
		} catch (IOException e) {
			LOGGER.error("I/O Error saving hotkeys to file \"" + file.getName() + "\"", e);
		} catch (RuntimeException e) {
			LOGGER.error(
			  "Unexpected error saving hotkeys to file \"" + file.getName() + "\"\n" +
			  "You may report this bug to the Simple Config mod issue tracker", e);
		}
	}
	
	public void loadHotkeys() {
		Yaml yaml = SimpleConfigCommentedYamlFormat.getDefaultYaml();
		File file = configHotKeysFile;
		try (FileInputStream is = new FileInputStream(file)) {
			InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
			Map<String, Object> map = yaml.load(reader);
			if (map == null) {
				updateHotKeys(new ConfigHotKeyGroup(), false);
				return;
			}
			PairList<?, ?> entries = (PairList<?, ?>) map.get("entries");
			updateHotKeys(ConfigHotKeyGroup.deserialize(
			  "", Collections.singletonMap("entries", entries)), false);
			Object o = map.get("added_default_groups");
			if (o instanceof Collection) {
				addedDefaultGroups.clear();
				((Collection<?>) o).stream()
				  .filter(e -> e instanceof String)
				  .forEach(e -> addedDefaultGroups.add((String) e));
			}
		} catch (IOException e) {
			LOGGER.error("I/O Error loading hotkeys from file \"" + file.getName() + "\"", e);
		} catch (RuntimeException e) {
			LOGGER.error(
			  "Unexpected error loading hotkeys from file \"" + file.getName() + "\"\n" +
			  "Ensure the file contains valid YAML.\n" +
			  "Otherwise, you may report this to the Simple Config mod issue tracker", e);
		}
	}
	
	public byte[] dump(ConfigHotKeyGroup group) {
		Yaml yaml = SimpleConfigCommentedYamlFormat.getDefaultYaml();
		try (
		  ByteArrayOutputStream os = new ByteArrayOutputStream();
		  OutputStreamWriter writer = new OutputStreamWriter(os, StandardCharsets.UTF_8)
		) {
			yaml.dump(group.serialize(), writer);
			return os.toByteArray();
		} catch (IOException e) {
			LOGGER.error("I/O Error saving hotkeys to memory", e);
			throw new RuntimeException(e);
		} catch (RuntimeException e) {
			LOGGER.error(
			  "Unexpected error saving hotkeys to memory\n" +
			  "You may report this bug to the Simple Config mod issue tracker", e);
			throw e;
		}
	}
	
	public @Nullable ConfigHotKeyGroup load(String name, byte[] data) {
		Yaml yaml = SimpleConfigCommentedYamlFormat.getDefaultYaml();
		try (
		  ByteArrayInputStream is = new ByteArrayInputStream(data);
		  InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)
		) {
			Map<?, ?> map = yaml.load(reader);
			return ConfigHotKeyGroup.deserialize(name, map);
		} catch (IOException e) {
			LOGGER.error("I/O Error loading hotkeys from memory", e);
			throw new RuntimeException(e);
		} catch (RuntimeException e) {
			LOGGER.error(
			  "Unexpected error loading hotkeys from memory\n" +
			  "Ensure the source file contains valid YAML.\n" +
			  "Otherwise, you may report this to the Simple Config mod issue tracker", e);
			throw e;
		}
	}
	
	public List<IConfigHotKey> getSortedHotKeys() {
		return hotKeys;
	}
	
	public ConfigHotKeyGroup getHotKeys() {
		return group;
	}
	
	public void updateHotKeys(ConfigHotKeyGroup hotKeys) {
		updateHotKeys(hotKeys, true);
	}
	
	protected void updateHotKeys(ConfigHotKeyGroup hotKeys, boolean save) {
		if (hotKeys == null) hotKeys = new ConfigHotKeyGroup();
		group = hotKeys;
		List<IConfigHotKey> keys = new ArrayList<>();
		updateHotKeys(keys, group);
		this.hotKeys = keys;
		if (save) saveHotkeys();
	}
	
	private void updateHotKeys(List<IConfigHotKey> keys, ConfigHotKeyGroup group) {
		if (!group.getHotKey().isUnknown()) keys.add(group);
		if (group.isEnabled()) for (IConfigHotKeyGroupEntry entry: group.getEntries()) {
			if (entry instanceof ConfigHotKeyGroup) {
				updateHotKeys(keys, (ConfigHotKeyGroup) entry);
			} else if (entry instanceof ConfigHotKey) {
				ConfigHotKey hotkey = (ConfigHotKey) entry;
				if (hotkey.isEnabled() && !hotkey.getHotKey().isUnknown()) keys.add(hotkey);
			}
		}
	}
	
	public static class ConfigHotKeyGroup implements IConfigHotKeyGroupEntry, IConfigHotKey {
		private String name = "";
		private ModifierKeyCode keyCode = ModifierKeyCode.unknown();
		private boolean enabled = true;
		private final List<IConfigHotKeyGroupEntry> entries = new ArrayList<>();
		
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		
		@Override public ModifierKeyCode getHotKey() {
			return keyCode;
		}
		public void setKeyCode(ModifierKeyCode keyCode) {
			this.keyCode = keyCode;
		}
		
		public boolean isEnabled() {
			return enabled;
		}
		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}
		
		public List<IConfigHotKeyGroupEntry> getEntries() {
			return entries;
		}
		
		public void addEntry(IConfigHotKeyGroupEntry entry) {
			entries.add(entry);
		}
		
		@Override public Object getSerializationKey() {
			return "(" + getName() + ")";
		}
		
		@Override public void applyHotkey() {
			enabled = !enabled;
			ConfigHotKeyManager manager = ConfigHotKeyManager.INSTANCE;
			manager.updateHotKeys(manager.getHotKeys());
			ConfigHotKeyOverlay.addMessage(getHotkeyReport(enabled), Collections.emptyList());
		}
		
		public ITextComponent getHotkeyReport(boolean enable) {
			return new TranslationTextComponent(
			  "simpleconfig.hotkey.group." + (enable ? "enable" : "disable"),
			  new StringTextComponent(getName()).mergeStyle(TextFormatting.AQUA)
			).mergeStyle(TextFormatting.GRAY);
		}
		
		@Override public Map<String, Object> serialize() {
			return Util.make(new LinkedHashMap<>(2), m -> {
				m.put("enabled", enabled);
				if (!keyCode.isUnknown()) m.put("key", keyCode.serializedName());
				m.put("entries", new PairList<>(entries.stream().map(
				  e -> Pair.of(e.getSerializationKey(), e.serialize())
				).collect(Collectors.toList())));
			});
		}
		
		public static ConfigHotKeyGroup deserialize(String name, Map<?, ?> value) {
			ConfigHotKeyGroup group = new ConfigHotKeyGroup();
			group.setName(name);
			if (value != null) {
				Object enabled = value.get("enabled");
				group.setEnabled(enabled instanceof Boolean ? (Boolean) enabled : true);
				Object groupKey = value.get("key");
				group.setKeyCode(groupKey instanceof String? ModifierKeyCode.parse(((String) groupKey)) : ModifierKeyCode.unknown());
				Object entries = value.get("entries");
				if (entries instanceof PairList) {
					((PairList<?, ?>) entries).forEach((k, v) -> {
						if (k instanceof String) {
							String key = (String) k;
							if (key.startsWith("(") && key.endsWith(")") && v instanceof Map) {
								String nm = key.substring(1, key.length() - 1);
								ConfigHotKeyGroup sub = deserialize(nm, (Map<?, ?>) v);
								group.addEntry(sub);
							} else {
								ModifierKeyCode keyCode = ModifierKeyCode.parse(key);
								if (v instanceof Map) {
									//noinspection unchecked
									group.addEntry(ConfigHotKey.deserialize(
									  keyCode, (Map<Object, Object>) v));
								}
							}
						}
					});
				}
			}
			return group;
		}
		
		@Override public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			ConfigHotKeyGroup that = (ConfigHotKeyGroup) o;
			return enabled == that.enabled && name.equals(that.name)
			       && keyCode.equals(that.keyCode) && entries.equals(that.entries);
		}
		
		@Override public int hashCode() {
			return Objects.hash(name, keyCode, enabled, entries);
		}
	}
	
	public interface IConfigHotKeyGroupEntry {
		Object getSerializationKey();
		Object serialize();
	}
}
