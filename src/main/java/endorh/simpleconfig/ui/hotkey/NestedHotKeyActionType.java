package endorh.simpleconfig.ui.hotkey;

import com.electronwill.nightconfig.core.CommentedConfig;
import endorh.simpleconfig.api.ui.icon.Icon;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.ui.api.AbstractConfigField;
import endorh.simpleconfig.ui.hotkey.NestedHotKeyActionType.NestedHotKeyAction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

// Type safety? what is that?
public class NestedHotKeyActionType<V> extends HotKeyActionType<V, NestedHotKeyAction<V>> {
	private static final Logger LOGGER = LogManager.getLogger();
	private final INestedHotKeyAction<V> action;
	
	public NestedHotKeyActionType(
	  String tagName, Icon icon, INestedHotKeyAction<V> action
	) {
		super(tagName, icon);
		this.action = action;
	}
	
	@Override
	public @Nullable <T, C, E extends AbstractConfigEntry<T, C, V>> NestedHotKeyAction<V> create(
	  E entry, Object value
	) {
		if (!(value instanceof Map)) return null;
		//noinspection unchecked
		Map<String, AbstractConfigField<?>> map = (Map<String, AbstractConfigField<?>>) value;
		Map<String, AbstractConfigEntry<?, ?, ?>> entries = action.getEntries(entry, map.keySet());
		if (entries == null) return null;
		Map<String, HotKeyAction<?>> actions = new LinkedHashMap<>();
		map.forEach((key, field) -> actions.put(key, createHotKeyAction(field, entries.get(key))));
		return new NestedHotKeyAction<>(this, action, actions);
	}
	
	@SuppressWarnings("unchecked") private static <T> HotKeyAction<T> createHotKeyAction(
	  AbstractConfigField<T> field, AbstractConfigEntry<?, ?, ?> entry
	) {
		try {
			return field.createHotKeyAction((AbstractConfigEntry<?, ?, T>) entry);
		} catch (ClassCastException ignored) {
			return null;
		}
	}
	
	@Override
	public @Nullable <T, C, E extends AbstractConfigEntry<T, C, V>> NestedHotKeyAction<V> deserialize(
	  E entry, Object value
	) {
		if (value instanceof Map<?, ?> m) {
			Map<String, HotKeyActionWrapper<?, ?>> storages = new LinkedHashMap<>();
			m.entrySet().stream()
			  .filter(e -> e.getKey() instanceof String && e.getValue() instanceof HotKeyActionWrapper)
			  .forEach(e -> storages.put((String) e.getKey(), (HotKeyActionWrapper<?, ?>) e.getValue()));
			Map<String, AbstractConfigEntry<?, ?, ?>> entries = action.getEntries(entry, storages.keySet());
			if (entries == null) return null;
			Map<String, HotKeyAction<?>> map = new LinkedHashMap<>();
			storages.forEach((key, wrapper) -> {
				AbstractConfigEntry<?, ?, ?> e = entries.get(key);
				if (e != null) map.put(key, deserializeNested(wrapper, entry, wrapper.value()));
			});
			if (!map.isEmpty()) return new NestedHotKeyAction<>(this, action, map);
		}
		return null;
	}
	
	protected static <V> HotKeyAction<V> deserializeNested(
	  HotKeyActionWrapper<V, ?> wrapper, AbstractConfigEntry<?, ?, ?> entry, Object value
	) {
		try {
			//noinspection unchecked
			return wrapper.type().deserialize((AbstractConfigEntry<Object, Object, V>) entry, value);
		} catch (ClassCastException e) {
			LOGGER.error("Could not deserialize hotkey action: " + entry.getGlobalPath(), e);
			return null;
		}
	}
	
	@Override public <T, C, E extends AbstractConfigEntry<T, C, V>> Object serialize(
	  E entry, NestedHotKeyAction<V> action
	) {
		Map<String, HotKeyAction<?>> storage = action.getStorage();
		LinkedHashMap<String, HotKeyActionWrapper<?, ?>> map = new LinkedHashMap<>();
		Map<String, AbstractConfigEntry<?, ?, ?>> entries = this.action.getEntries(entry, storage.keySet());
		if (entries == null) return null;
		storage.forEach((key, a) -> {
			AbstractConfigEntry<?, ?, ?> e = entries.get(key);
			HotKeyActionWrapper<?, ? extends HotKeyAction<?>> w = serializeNested(e, a);
			if (w != null) map.put(key, w);
		});
		return map;
	}
	
	@SuppressWarnings("unchecked") protected static <V, A extends HotKeyAction<V>> HotKeyActionWrapper<V, A> serializeNested(
	  AbstractConfigEntry<?, ?, ?> entry, A action
	) {
		try {
			AbstractConfigEntry<?, ?, V> e = (AbstractConfigEntry<?, ?, V>) entry;
			HotKeyActionType<V, A> type = (HotKeyActionType<V, A>) action.getType();
			Object storage = type.serialize(e, action);
			return new HotKeyActionWrapper<>(type, storage);
		} catch (ClassCastException e) {
			LOGGER.error("Could not serialize hotkey action: " + entry.getGlobalPath(), e);
		}
		return null;
	}
	
	public static class NestedHotKeyAction<V> extends HotKeyAction<V> {
		private final INestedHotKeyAction<V> action;
		private final Map<String, HotKeyAction<?>> storage;
		
		public NestedHotKeyAction(
		  HotKeyActionType<V, ?> type, INestedHotKeyAction<V> action,
		  Map<String, HotKeyAction<?>> storage
		) {
			super(type);
			this.action = action;
			this.storage = storage;
		}
		
		@Override public <T, C, E extends AbstractConfigEntry<T, C, V>>
		@Nullable Component apply(String path, E entry, CommentedConfig result) {
			try {
				Map<String, AbstractConfigEntry<?, ?, ?>> entries = action.getEntries(entry, storage.keySet());
				if (entries == null) return null;
				CommentedConfig config = CommentedConfig.inMemory();
				storage.forEach((name, action) -> {
					try {
						//noinspection unchecked
						AbstractConfigEntry<Object, Object, Object> e =
						  (AbstractConfigEntry<Object, Object, Object>) entries.get(name);
						if (e != null) {
							//noinspection unchecked
							((HotKeyAction<Object>) action).apply(name, e, config);
						}
					} catch (ClassCastException e) {
						LOGGER.error("Error applying hotkey action " + path + "." + name, e);
					}
				});
				Object value = action.applyValue(entry, config);
				if (value != null) result.set(path, value);
				return new TextComponent("Testing... blip blap blop!");
			} catch (ClassCastException e) {
				LOGGER.error("Error applying hotkey action " + path, e);
			}
			return null;
		}
		
		public INestedHotKeyAction<V> getAction() {
			return action;
		}
		
		public Map<String, HotKeyAction<?>> getStorage() {
			return storage;
		}
		
		@Override public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			if (!super.equals(o)) return false;
			NestedHotKeyAction<?> that = (NestedHotKeyAction<?>) o;
			return action.equals(that.action) && storage.equals(that.storage);
		}
		
		@Override public int hashCode() {
			return Objects.hash(super.hashCode(), action, storage);
		}
	}
	
	public interface INestedHotKeyAction<V> {
		static <V> INestedHotKeyAction<V> of(
		  BiFunction<AbstractConfigEntry<?, ?, V>, Set<String>, Map<String, AbstractConfigEntry<?, ?, ?>>> getter,
		  BiFunction<AbstractConfigEntry<?, ?, V>, CommentedConfig, Object> setter
		) {
			return new INestedHotKeyAction<>() {
				@Override public @Nullable Map<String, AbstractConfigEntry<?, ?, ?>> getEntries(
				  AbstractConfigEntry<?, ?, V> entry, Set<String> names
				) {
					return getter.apply(entry, names);
				}
				
				@Override public @Nullable Object applyValue(
				  AbstractConfigEntry<?, ?, V> entry, CommentedConfig values
				) {
					return setter.apply(entry, values);
				}
			};
		}
		
		@Nullable Map<String, AbstractConfigEntry<?, ?, ?>> getEntries(
		  AbstractConfigEntry<?, ?, V> entry, Set<String> names);
		@Nullable Object applyValue(AbstractConfigEntry<?, ?, V> entry, CommentedConfig values);
	}
}
