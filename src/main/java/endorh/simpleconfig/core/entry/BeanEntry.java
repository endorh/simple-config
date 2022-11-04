package endorh.simpleconfig.core.entry;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import endorh.simpleconfig.api.AtomicEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.EntryTag;
import endorh.simpleconfig.api.entry.BeanEntryBuilder;
import endorh.simpleconfig.api.ui.icon.Icon;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.AbstractConfigEntryBuilder;
import endorh.simpleconfig.core.AtomicEntry;
import endorh.simpleconfig.core.DummyEntryHolder;
import endorh.simpleconfig.core.entry.BeanProxy.IBeanGuiAdapter;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.impl.builders.BeanFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Java Bean entry.
 */
public class BeanEntry<B> extends AbstractConfigEntry<B, Map<String, Object>, B> {
	private static final Logger LOGGER = LogManager.getLogger();
	private final BeanProxyImpl<B> proxy;
	private final Map<String, AbstractConfigEntry<?, ?, ?>> entries;
	private @Nullable String caption;
	private @Nullable Function<B, Icon> iconProvider;
	
	public BeanEntry(
	  ConfigEntryHolder parent, String name, B defValue, BeanProxyImpl<B> proxy,
	  Map<String, AbstractConfigEntry<?,?,?>> entries
	) {
		super(parent, name, defValue);
		this.proxy = proxy;
		this.entries = entries;
	}
	
	public BeanProxy<B> getProxy() {
		return proxy;
	}
	
	public AbstractConfigEntry<?, ?, ?> getEntry(String name) {
		return entries.get(name);
	}
	
	public static class Builder<B> extends AbstractConfigEntryBuilder<
	  B, Map<String, Object>, B, BeanEntry<B>, BeanEntryBuilder<B>, Builder<B>
	> implements BeanEntryBuilder<B> {
		private final Class<B> type;
		private final Map<String, AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?>> entries = new LinkedHashMap<>();
		private String caption;
		private @Nullable Function<B, Icon> iconProvider = null;
		private boolean allowUneditableProperties = false;
		
		public Builder(B value) {
			super(value, value.getClass());
			//noinspection unchecked
			type = (Class<B>) value.getClass();
		}
		
		@Override public @NotNull BeanEntryBuilder<B> allowUneditableProperties(boolean allowUneditable) {
			Builder<B> copy = copy();
			copy.allowUneditableProperties = allowUneditable;
			return copy;
		}
		
		@Override protected BeanEntry<B> buildEntry(ConfigEntryHolder parent, String name) {
			Map<String, AbstractConfigEntry<?, ?, ?>> entries = new LinkedHashMap<>();
			this.entries.forEach((n, e) -> entries.put(n, DummyEntryHolder.build(parent, e)));
			BeanProxyImpl<B> proxy = new BeanProxyImpl<>(type, Util.make(
			  new HashMap<>(), m -> entries.forEach((n, e) -> m.put(n, createAdapter(e)))));
			String prefix = entries.values().stream().map(e -> e.getRoot().getModId()).findFirst().orElse("")
			                + ".config.bean." + proxy.getTypeTranslation() + ".";
			entries.forEach((n, e) -> {
				String key = prefix + proxy.getTranslation(n);
				e.setTranslation(key);
				e.setTooltipKey(key + ":help");
				e.setName(proxy.getTranslation(n));
			});
			Set<String> names = Sets.newHashSet(proxy.getPropertyNames());
			for (String n: entries.keySet()) {
				if (!names.remove(n)) throw new ConfigBeanIntrospectionException(
				  "No bean property for name " + n + " within bean class " + proxy.getTypeName());
			}
			BeanEntry<B> entry = new BeanEntry<>(parent, name, value, proxy, entries);
			if (!allowUneditableProperties && !names.isEmpty()) throw new ConfigBeanIntrospectionException(
			  "Found uneditable properties in bean class " + proxy.getTypeName() + ": ["
			  + String.join(", ", names) + "]\n" +
			  "Call allowUneditableProperties() to allow them, or define config entries for them." +
			  "\n  at " + entry.getGlobalPath());
			entries.keySet().forEach(n -> {
				if (proxy.get(value, n) == null)
					throw new ConfigBeanNullPropertyException(proxy.getPropertyName(n));
			});
			entry.caption = caption;
			entry.iconProvider = iconProvider;
			return entry;
		}
		
		private static <V, G> IBeanGuiAdapter createAdapter(AbstractConfigEntry<V, ?, G> entry) {
			return IBeanGuiAdapter.of(v -> {
				try {
					//noinspection unchecked
					return entry.forGui((V) v);
				} catch (ClassCastException e) {
					return null;
				}
			}, entry::fromGui);
		}
		
		@Override public @NotNull Builder<B> add(String name, ConfigEntryBuilder<?, ?, ?, ?> entryBuilder) {
			Builder<B> copy = copy();
			if (!(entryBuilder instanceof AbstractConfigEntryBuilder)) throw new IllegalArgumentException(
			  "ConfigEntryBuilder not instance of AbstractConfigEntryBuilder");
			copy.entries.put(name, (AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?>) entryBuilder);
			return copy;
		}
		
		@Override public <CB extends ConfigEntryBuilder<?, ?, ?, ?> & AtomicEntryBuilder>
		@NotNull BeanEntryBuilder<B> caption(String name, CB entryBuilder) {
			Builder<B> copy = add(name, entryBuilder);
			copy.caption = name;
			return copy;
		}
		
		@Override public @NotNull BeanEntryBuilder<B> withoutCaption() {
			Builder<B> copy = copy();
			copy.caption = null;
			return copy;
		}
		
		@Override public @NotNull BeanEntryBuilder<B> withIcon(Function<B, Icon> icon) {
			Builder<B> copy = copy();
			copy.iconProvider = icon;
			return copy;
		}
		
		@Override protected Builder<B> createCopy(B value) {
			Builder<B> copy = new Builder<>(value);
			copy.entries.putAll(entries);
			copy.caption = caption;
			copy.iconProvider = iconProvider;
			copy.allowUneditableProperties = allowUneditableProperties;
			return copy;
		}
	}
	
	@Override public Map<String, Object> forConfig(B value) {
		HashMap<String, Object> map = new LinkedHashMap<>(proxy.getProperties().size());
		for (String name: proxy.getPropertyNames()) {
			try {
				//noinspection unchecked
				AbstractConfigEntry<Object, ?, Object> entry =
				  (AbstractConfigEntry<Object, ?, Object>) entries.get(name);
				if (entry != null) {
					Object v = proxy.get(value, name);
					if (v == null) throw new ConfigBeanNullPropertyException(proxy.getPropertyName(name));
					map.put(name, entry.forConfig(v));
				}
			} catch (ClassCastException e) {
				throw new ConfigBeanAccessException(
				  "Error reading Java Bean for config entry " + getGlobalPath(), e);
			}
		}
		return map;
	}
	
	@Override public @Nullable B fromConfig(@Nullable Map<String, Object> value) {
		if (value == null) return null;
		//noinspection unchecked
		return proxy.createFrom(defValue, Maps.transformEntries(
		  Maps.filterKeys(entries, proxy.getPropertyNames()::contains),
		  (n, v) -> ((AbstractConfigEntry<?, Object, ?>) v).fromConfigOrDefault(value.get(n))
		));
	}
	
	@Override public Object forActualConfig(@Nullable Map<String, Object> value) {
		if (value == null) return null;
		Map<String, Object> map = new LinkedHashMap<>(value.size());
		entries.forEach((name, entry) -> {
			Object gui = value.get(name);
			try {
				//noinspection unchecked
				AbstractConfigEntry<?, Object, ?> c = (AbstractConfigEntry<?, Object, ?>) entry;
				map.put(name, c.forActualConfig(gui));
			} catch (ClassCastException e) {
				LOGGER.error("Error serializing bean entry property \"" + name + "\": " + getGlobalPath(), e);
			}
		});
		return map.isEmpty()? null : map;
	}
	
	@Override public @Nullable Map<String, Object> fromActualConfig(@Nullable Object value) {
		if (value instanceof List) {
			//noinspection unchecked
			List<Object> seq = (List<Object>) value;
			Map<String, Object> map = new LinkedHashMap<>();
			for (Object o: seq) {
				if (o instanceof Map) {
					Map<?, ?> mm = (Map<?, ?>) o;
					if (mm.entrySet().size() != 1) return null;
					Map.Entry<?, ?> e = mm.entrySet().stream().findFirst()
					  .orElseThrow(IllegalStateException::new);
					String key = tryCast(e.getKey(), String.class);
					AbstractConfigEntry<?, ?, ?> entry = entries.get(key);
					if (entry == null) continue;
					Object val = entry.fromActualConfig(e.getValue());
					if (key == null || val == null) return null;
					map.put(key, val);
				} else if (o instanceof Config) {
					Config config = (Config) o;
					if (config.entrySet().size() != 1) return null;
					Config.Entry e = config.entrySet().stream().findFirst()
					  .orElseThrow(IllegalStateException::new);
					String key = e.getKey();
					AbstractConfigEntry<?, ?, ?> entry = entries.get(key);
					if (entry == null) continue;
					Object val = entry.fromActualConfig(e.getValue());
					if (key == null || val == null) return null;
					map.put(key, val);
				}
			}
			return map;
		} else if (value instanceof Config) {
			Map<String, Object> map = new LinkedHashMap<>();
			for (CommentedConfig.Entry e: ((CommentedConfig) value).entrySet()) {
				String key = e.getKey();
				AbstractConfigEntry<?, ?, ?> entry = entries.get(key);
				if (entry == null) continue;
				Object val = entry.fromActualConfig(e.getValue());
				if (key == null || val == null) return null;
				map.put(key, val);
			}
			return map;
		} else if (value instanceof Map) {
			Map<String, Object> map = new LinkedHashMap<>();
			for (Entry<?, ?> e: ((Map<?, ?>) value).entrySet()) {
				String key = tryCast(e.getKey(), String.class);
				AbstractConfigEntry<?, ?, ?> entry = entries.get(key);
				if (entry == null) continue;
				Object val = entry.fromActualConfig(e.getValue());
				if (key == null || val == null) return null;
				map.put(key, val);
			}
			return map;
		}
		return null;
	}
	
	protected static <T> @Nullable T tryCast(Object value, Class<T> type) {
		return type.isInstance(value)? type.cast(value) : null;
	}
	
	private static final Pattern LINE_BREAK = Pattern.compile("\\R");
	
	@Override public List<String> getConfigCommentTooltips() {
		List<String> comments = super.getConfigCommentTooltips();
		comments.add(
		  "Object: \n  " + entries.entrySet().stream().map(
			 e -> e.getKey() + ": " +
			      LINE_BREAK.matcher(e.getValue().getConfigCommentTooltip()).replaceAll("\n  ").trim()
		  ).collect(Collectors.joining("\n  ")));
		return comments;
	}
	
	@Override public Optional<FieldBuilder<B, ?, ?>> buildGUIEntry(ConfigFieldBuilder builder) {
		BeanFieldBuilder<B> fieldBuilder = builder
		  .startBeanField(getDisplayName(), forGui(get()), proxy)
		  .withIcon(iconProvider);
		entries.forEach((name, entry) -> {
			if (name.equals(caption)) {
				if (!(entry instanceof AtomicEntry)) {
					LOGGER.debug("Caption for Bean entry is not a key entry: " + getGlobalPath());
				} else {
					AtomicEntry<?> keyEntry = (AtomicEntry<?>) entry;
					addCaption(
					  builder, fieldBuilder.withoutTags(EntryTag.NON_PERSISTENT, entry.copyTag),
					  name, keyEntry);
					return;
				}
			}
			entry.buildGUIEntry(builder).ifPresent(
			  g -> fieldBuilder.add(name, g.withoutTags(EntryTag.NON_PERSISTENT, entry.copyTag)));
		});
		return Optional.of(decorate(fieldBuilder));
	}
	
	private static <
	  B, KG, E extends AbstractConfigListEntry<KG> & IChildListEntry,
	  FB extends FieldBuilder<KG, E, FB>
	> void addCaption(
	  ConfigFieldBuilder builder, BeanFieldBuilder<B> fieldBuilder, String name, AtomicEntry<KG> keyEntry
	) {
		fieldBuilder.caption(name, keyEntry.<E, FB>buildAtomicChildGUIEntry(builder));
	}
	
	public static class ConfigBeanIntrospectionException extends RuntimeException {
		public ConfigBeanIntrospectionException(String message) {
			super(message);
		}
		public ConfigBeanIntrospectionException(String message, Throwable cause) {
			super(message, cause);
		}
	}
	
	public static class ConfigBeanAccessException extends RuntimeException {
		public ConfigBeanAccessException(String message) {
			super(message);
		}
		public ConfigBeanAccessException(String message, Throwable cause) {
			super(message, cause);
		}
	}
	
	public static class ConfigBeanNullPropertyException extends RuntimeException {
		private static String getMessage(String path) {
			return "Null bean property value: " + path
			       + "\nConfigurable beans cannot have nullable properties.";
		}
		public ConfigBeanNullPropertyException(String path) {
			super(getMessage(path));
		}
		public ConfigBeanNullPropertyException(String path, Throwable cause) {
			super(getMessage(path), cause);
		}
	}
}
