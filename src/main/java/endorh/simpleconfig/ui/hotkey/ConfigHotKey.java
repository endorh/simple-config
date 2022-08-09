package endorh.simpleconfig.ui.hotkey;

import com.electronwill.nightconfig.core.CommentedConfig;
import endorh.simpleconfig.SimpleConfigMod.ServerConfig.permissions;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.SimpleConfig;
import endorh.simpleconfig.core.SimpleConfig.EditType;
import endorh.simpleconfig.core.SimpleConfigNetworkHandler;
import endorh.simpleconfig.ui.api.ModifierKeyCode;
import endorh.simpleconfig.ui.hotkey.ConfigHotKeyManager.IConfigHotKeyGroupEntry;
import net.minecraft.util.Util;
import net.minecraft.util.text.*;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ConfigHotKey implements IConfigHotKeyGroupEntry, IConfigHotKey {
	private ModifierKeyCode keyCode;
	private String name = "";
	private boolean enabled = true;
	private Map<Pair<String, EditType>, Map<String, HotKeyAction<?>>> actions;
	private Map<String, Map<String, Map<String, Object>>> unknown;
	
	public ConfigHotKey() {
		this(ModifierKeyCode.unknown(), new LinkedHashMap<>(), new LinkedHashMap<>());
	}
	
	public ConfigHotKey(
	  ModifierKeyCode keyCode, Map<Pair<String, EditType>, Map<String, HotKeyAction<?>>> actions,
	  Map<String, Map<String, Map<String, Object>>> unknown
	) {
		this.keyCode = keyCode;
		this.actions = actions;
		this.unknown = unknown;
	}
	
	@Override public void applyHotkey() {
		Map<Pair<String, EditType>, Map<String, HotKeyAction<?>>> actions = getActions();
		Map<Pair<String, EditType>, HotKeyExecutionContext> contexts = new HashMap<>();
		actions.forEach((pair, configActions) -> configActions.forEach((path, action) -> {
			SimpleConfig config = getConfig(pair);
			if (config != null) {
				HotKeyExecutionContext context = contexts.computeIfAbsent(
				  pair, k -> new HotKeyExecutionContext(config));
				ITextComponent r = action.apply(config, path, context.result);
				if (r != null) context.report.add(r);
			}
		}));
		List<ITextComponent> messages = new ArrayList<>();
		contexts.forEach((pair, context) -> {
			if (pair.getRight().isRemote()) {
				if (SimpleConfigNetworkHandler.applyRemoteSnapshot(
				  context.config, context.result, context.report
				)) {
					messages.addAll(context.report);
				} else messages.add(new TranslationTextComponent(
				  "simpleconfig.hotkey.no_permission",
				  new StringTextComponent(context.config.getModName())
					 .mergeStyle(TextFormatting.GRAY)));
			} else {
				context.config.loadSnapshot(context.result, false, false, null);
				context.config.save();
				messages.addAll(context.report);
			}
		});
		int size = messages.size();
		if (size > 4) {
			 messages.subList(4, size).clear();
			 messages.add(new TranslationTextComponent("simpleconfig.hotkey.more", size - 4));
		}
		ConfigHotKeyOverlay.addMessage(getTitle(), messages);
	}
	
	private static class HotKeyExecutionContext {
		public final SimpleConfig config;
		public final CommentedConfig result = CommentedConfig.inMemory();
		public final List<ITextComponent> report = new ArrayList<>();
		private HotKeyExecutionContext(SimpleConfig config) {
			this.config = config;
		}
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
	
	public Map<Pair<String, EditType>, Map<String, HotKeyAction<?>>> getActions() {
		return actions;
	}
	public void setActions(Map<Pair<String, EditType>, Map<String, HotKeyAction<?>>> actions) {
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
	
	private static SimpleConfig getConfig(Pair<String, EditType> pair) {
		if (pair.getRight().isRemote() && !permissions.permissionFor(pair.getLeft()).getLeft().canEdit())
			return null;
		return SimpleConfig.getConfigOrNull(pair.getLeft(), pair.getRight().getType());
	}
	
	protected Map<String, Map<String, Map<String, Object>>> serializeActions() {
		return Util.make(new LinkedHashMap<>(), m -> {
			getActions().forEach((pair, actions) -> {
				Map<String, Object> mm =
				  m.computeIfAbsent(pair.getLeft(), k -> new LinkedHashMap<>())
					 .computeIfAbsent(pair.getRight().getAlias(), t -> new LinkedHashMap<>());
				SimpleConfig config = getConfig(pair);
				if (config != null) actions.forEach((path, action) -> {
					if (config.hasEntry(path)) {
						try {
							HotKeyActionWrapper<?, ?> wrapper =
							  serialize(action, config.getEntry(path));
							mm.put(path, wrapper);
						} catch (ClassCastException ignored) {}
					}
				});
			});
			getUnknown().forEach((modId, mm) -> m.computeIfAbsent(
			  modId, k -> new LinkedHashMap<>()).putAll(mm));
		});
	}
	
	@SuppressWarnings("unchecked") protected <V, A extends HotKeyAction<V>,
	  E extends AbstractConfigEntry<Object, Object, V>
	> HotKeyActionWrapper<V, A> serialize(A action, E entry) {
		HotKeyActionType<V, A> type = (HotKeyActionType<V, A>) action.getType();
		Object value = type.serialize(entry, action);
		return new HotKeyActionWrapper<>(type, value);
	}
	
	public static ConfigHotKey deserialize(ModifierKeyCode keyCode, Map<Object, Object> packed) {
		boolean enabled = getAsOrElse(packed, "enabled", Boolean.class, true);
		String name = getAsOrElse(packed, "name", String.class, "");
		Map<?, ?> a = getAsOrElse(packed, "actions", Map.class, null);
		Map<Pair<String, EditType>, Map<String, HotKeyAction<?>>> actions = new LinkedHashMap<>();
		Map<String, Map<String, Map<String, Object>>> unknown = new LinkedHashMap<>();
		a.forEach((id, s) -> {
			if (id instanceof String && s instanceof Map) {
				final String modId = (String) id;
				((Map<?, ?>) s).forEach((t, ss) -> {
					if (t instanceof String && ss instanceof Map) {
						EditType type = EditType.fromAlias((String) t);
						if (type != null && SimpleConfig.hasConfig(modId, type.getType())) {
							SimpleConfig config = SimpleConfig.getConfig(modId, type.getType());
							Pair<String, EditType> pair = Pair.of(modId, type);
							Map<String, HotKeyAction<?>> aa = actions
							  .computeIfAbsent(pair, cc -> new LinkedHashMap<>());
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
							Map<String, Object> mm = unknown.computeIfAbsent(modId, k -> new LinkedHashMap<>())
							  .computeIfAbsent((String) t, k -> new LinkedHashMap<>());
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
	
	protected static <V, A extends HotKeyAction<V>, E extends AbstractConfigEntry<Object, Object, V>> A deserialize(
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
		Map<Pair<String, EditType>, Map<String, HotKeyAction<?>>> actions = new LinkedHashMap<>();
		this.actions.forEach((pair, map) -> actions.put(pair, new LinkedHashMap<>(map)));
		hotKey.setActions(actions);
		Map<String, Map<String, Map<String, Object>>> unknown = new LinkedHashMap<>();
		this.unknown.forEach((modId, sub) -> {
			Map<String, Map<String, Object>> s = unknown.computeIfAbsent(modId, k -> new LinkedHashMap<>());
			sub.forEach((t, subSub) -> s.computeIfAbsent(t, k -> new LinkedHashMap<>()).putAll(subSub));
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
