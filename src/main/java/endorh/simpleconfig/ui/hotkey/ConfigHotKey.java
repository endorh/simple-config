package endorh.simpleconfig.ui.hotkey;

import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.SimpleConfig;
import endorh.simpleconfig.ui.api.ModifierKeyCode;
import endorh.simpleconfig.ui.hotkey.ConfigHotKeyManager.IConfigHotKeyGroupEntry;
import net.minecraft.util.Util;
import net.minecraft.util.text.*;
import net.minecraftforge.fml.config.ModConfig.Type;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ConfigHotKey implements IConfigHotKeyGroupEntry, IConfigHotKey {
	private ModifierKeyCode keyCode;
	private String name = "";
	private boolean enabled = true;
	private Map<SimpleConfig, Map<String, HotKeyAction<?>>> actions;
	private Map<String, Map<String, Map<String, Object>>> unknown;
	
	public ConfigHotKey() {
		this(ModifierKeyCode.unknown(), new HashMap<>(), new HashMap<>());
	}
	
	public ConfigHotKey(
	  ModifierKeyCode keyCode, Map<SimpleConfig, Map<String, HotKeyAction<?>>> actions,
	  Map<String, Map<String, Map<String, Object>>> unknown
	) {
		this.keyCode = keyCode;
		this.actions = actions;
		this.unknown = unknown;
	}
	
	@Override public void applyHotkey() {
		Map<SimpleConfig, Map<String, HotKeyAction<?>>> actions = getActions();
		List<ITextComponent> messages = new ArrayList<>();
		actions.forEach((config, configActions) -> configActions.forEach((path, action) -> {
			ITextComponent r = action.apply(config, path);
			if (r != null) messages.add(r);
		}));
		int size = messages.size();
		if (size > 4) {
			 messages.subList(4, size).clear();
			 messages.add(new TranslationTextComponent("simpleconfig.hotkey.more", size - 4));
		}
		ConfigHotKeyOverlay.addMessage(getTitle(), messages);
	}
	
	public ITextComponent getTitle() {
		IFormattableTextComponent title = new StringTextComponent(name != null? name + " " : "")
		  .mergeStyle(TextFormatting.WHITE);
		title.append(getHotKey().getLocalizedName(TextFormatting.BLUE, TextFormatting.BLUE).deepCopy());
		return title;
	}
	
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
	
	public Map<SimpleConfig, Map<String, HotKeyAction<?>>> getActions() {
		return actions;
	}
	public void setActions(
	  Map<SimpleConfig, Map<String, HotKeyAction<?>>> actions
	) {
		this.actions = actions;
	}
	public Map<String, Map<String, Map<String, Object>>> getUnknown() {
		return unknown;
	}
	public void setUnknown(Map<String, Map<String, Map<String, Object>>> unknown) {
		this.unknown = unknown;
	}
	
	@Override public Object getSerializationKey() {
		return getHotKey().serializedName();
	}
	@Override public Object serialize() {
		return Util.make(new LinkedHashMap<>(), m -> {
			m.put("name", getName());
			m.put("enabled", isEnabled());
			m.put("actions", serializeActions());
		});
	}
	
	protected Map<String, Map<String, Map<String, Object>>> serializeActions() {
		return Util.make(new LinkedHashMap<>(), m -> {
			getActions().forEach((config, actions) -> {
				Map<String, Object> mm =
				  m.computeIfAbsent(config.getModId(), k -> new HashMap<>())
					 .computeIfAbsent(config.getType().extension(), t -> new HashMap<>());
				actions.forEach((path, action) -> {
					if (config.hasEntry(path)) {
						try {
							HotKeyActionWrapper<?, ?> wrapper = serialize(action, config.getEntry(path));
							mm.put(path, wrapper);
						} catch (ClassCastException ignored) {}
					}
				});
			});
			getUnknown().forEach((modId, mm) -> m.computeIfAbsent(
			  modId, k -> new HashMap<>()).putAll(mm));
		});
	}
	
	@SuppressWarnings("unchecked") protected <V, A extends HotKeyAction<V>,
	  E extends AbstractConfigEntry<Object, Object, V, ?>
	> HotKeyActionWrapper<V, A> serialize(A action, E entry) {
		HotKeyActionType<V, A> type = (HotKeyActionType<V, A>) action.getType();
		Object value = type.serialize(entry, action);
		return new HotKeyActionWrapper<>(type, value);
	}
	
	public static ConfigHotKey deserialize(ModifierKeyCode keyCode, Map<Object, Object> packed) {
		boolean enabled = getAsOrElse(packed, "enabled", Boolean.class, true);
		String name = getAsOrElse(packed, "name", String.class, "");
		Map<?, ?> a = getAsOrElse(packed, "actions", Map.class, null);
		Map<SimpleConfig, Map<String, HotKeyAction<?>>> actions = new HashMap<>();
		Map<String, Map<String, Map<String, Object>>> unknown = new HashMap<>();
		a.forEach((id, s) -> {
			if (id instanceof String && s instanceof Map) {
				final String modId = (String) id;
				((Map<?, ?>) s).forEach((t, ss) -> {
					if (t instanceof String && ss instanceof Map) {
						Optional<Type> opt = Arrays.stream(Type.values()).filter(tt -> tt.extension().equals(t)).findFirst();
						if (opt.isPresent() && SimpleConfig.hasConfig(modId, opt.get())) {
							SimpleConfig config = SimpleConfig.getConfig(modId, opt.get());
							Map<String, HotKeyAction<?>> aa =
							  actions.computeIfAbsent(config, cc -> new HashMap<>());
							((Map<?, ?>) ss).forEach((p, v) -> {
								if (p instanceof String && v instanceof HotKeyActionWrapper) {
									String path = (String) p;
									HotKeyActionWrapper<?, ?> w = (HotKeyActionWrapper<?, ?>) v;
									if (config.hasEntry(path)) {
										try {
											HotKeyAction<?> action = deserialize(
											  w, config.getEntry(path), w.getValue());
											if (action != null) aa.put(path, action);
										} catch (ClassCastException ignored) {}
									}
								}
							});
						} else {
							Map<String, Object> mm = unknown.computeIfAbsent(modId, k -> new HashMap<>())
							  .computeIfAbsent((String) t, k -> new HashMap<>());
							((Map<?, ?>) ss).forEach((p, v) -> {
								if (p instanceof String) mm.put((String) p, v);
							});
						}
					}
				});
			}
		});
		ConfigHotKey hotKey = new ConfigHotKey(keyCode, actions, unknown);
		hotKey.setName(name);
		hotKey.setEnabled(enabled);
		return hotKey;
	}
	
	protected static <V, A extends HotKeyAction<V>, E extends AbstractConfigEntry<Object, Object, V, ?>> A deserialize(
	  HotKeyActionWrapper<V, A> wrapper, E entry, Object value
	) {
		return wrapper.getType().deserialize(entry, value);
	}
	
	@Contract("_, _, _, !null -> !null")
	protected static <K, T> T getAsOrElse(@NotNull Map<K, ?> map, K key, @NotNull Class<T> type, T def) {
		Object o = map.get(key);
		if (type.isInstance(o)) return type.cast(o);
		return def;
	}
	
	public ConfigHotKey copy() {
		ConfigHotKey hotKey = new ConfigHotKey();
		hotKey.setKeyCode(keyCode);
		hotKey.setName(name);
		hotKey.setEnabled(enabled);
		Map<SimpleConfig, Map<String, HotKeyAction<?>>> actions = new HashMap<>();
		this.actions.forEach((config, map) -> actions.put(config, new HashMap<>(map)));
		hotKey.setActions(actions);
		Map<String, Map<String, Map<String, Object>>> unknown = new HashMap<>();
		this.unknown.forEach((modId, sub) -> {
			Map<String, Map<String, Object>> s = unknown.computeIfAbsent(modId, k -> new HashMap<>());
			sub.forEach((t, subSub) -> s.computeIfAbsent(t, k -> new HashMap<>()).putAll(subSub));
		});
		hotKey.setUnknown(unknown);
		return hotKey;
	}
	
	@Override public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ConfigHotKey that = (ConfigHotKey) o;
		return enabled == that.enabled && keyCode.equals(that.keyCode)
		       && name.equals(that.name) && actions.equals(that.actions)
		       && unknown.equals(that.unknown);
	}
	
	@Override public int hashCode() {
		return Objects.hash(keyCode, name, enabled, actions, unknown);
	}
}
