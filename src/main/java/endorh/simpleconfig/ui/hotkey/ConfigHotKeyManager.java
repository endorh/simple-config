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
@EventBusSubscriber(modid = SimpleConfigMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ConfigHotKeyManager {
	private static final Logger LOGGER = LogManager.getLogger();
	public static final ConfigHotKeyManager INSTANCE = new ConfigHotKeyManager();
	
	private final File configHotKeysFile = SimpleConfigPaths.SIMPLE_CONFIG_CONFIG_DIR
	  .resolve("config_hotkeys.yaml").toFile();
	
	private List<IConfigHotKey> hotKeys = Lists.newArrayList();
	private ConfigHotKeyGroup group = new ConfigHotKeyGroup();
	
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
		});
	}
	
	public void saveHotkeys() {
		Yaml yaml = SimpleConfigCommentedYamlFormat.getDefaultYaml();
		File file = configHotKeysFile;
		file.getParentFile().mkdirs();
		try (FileOutputStream os = new FileOutputStream(file)) {
			OutputStreamWriter writer = new OutputStreamWriter(os, StandardCharsets.UTF_8);
			yaml.dump(group.serialize().get("entries"), writer);
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
			PairList<?, ?> list = yaml.load(reader);
			updateHotKeys(ConfigHotKeyGroup.deserialize(
			  "", Collections.singletonMap("entries", list)), false);
		} catch (IOException e) {
			LOGGER.error("I/O Error loading hotkeys from file \"" + file.getName() + "\"", e);
		} catch (RuntimeException e) {
			LOGGER.error(
			  "Unexpected error loading hotkeys from file \"" + file.getName() + "\"\n" +
			  "Ensure the file contains valid YAML.\n" +
			  "Otherwise, you may report this to the Simple Config mod issue tracker", e);
		}
	}
	
	public void dump(ConfigHotKeyGroup group, File file) {
		Yaml yaml = SimpleConfigCommentedYamlFormat.getDefaultYaml();
		file.getParentFile().mkdirs();
		try (FileOutputStream os = new FileOutputStream(file)) {
			OutputStreamWriter writer = new OutputStreamWriter(os, StandardCharsets.UTF_8);
			yaml.dump(group.serialize(), writer);
		} catch (IOException e) {
			LOGGER.error("I/O Error saving hotkeys to file \"" + file.getName() + "\"", e);
		} catch (RuntimeException e) {
			LOGGER.error(
			  "Unexpected error saving hotkeys to file \"" + file.getName() + "\"\n" +
			  "You may report this bug to the Simple Config mod issue tracker", e);
		}
	}
	
	public @Nullable ConfigHotKeyGroup load(String name, File file) {
		Yaml yaml = SimpleConfigCommentedYamlFormat.getDefaultYaml();
		try (FileInputStream is = new FileInputStream(file)) {
			InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
			Map<?, ?> map = yaml.load(reader);
			return ConfigHotKeyGroup.deserialize(name, map);
		} catch (IOException e) {
			LOGGER.error("I/O Error loading hotkeys from file \"" + file.getName() + "\"", e);
		} catch (RuntimeException e) {
			LOGGER.error(
			  "Unexpected error loading hotkeys from file \"" + file.getName() + "\"\n" +
			  "Ensure the file contains valid YAML.\n" +
			  "Otherwise, you may report this to the Simple Config mod issue tracker", e);
		}
		return null;
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
		private ModifierKeyCode keyCode;
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
	}
	
	public interface IConfigHotKeyGroupEntry {
		Object getSerializationKey();
		Object serialize();
	}
}
