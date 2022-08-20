package endorh.simpleconfig.core.entry;

import com.google.common.base.CaseFormat;
import endorh.simpleconfig.core.entry.BeanEntry.ConfigBeanAccessException;
import endorh.simpleconfig.core.entry.BeanEntry.ConfigBeanIntrospectionException;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class BeanProxy<B> {
	private static final PropertyUtils utils = new PropertyUtils();
	private final Class<B> type;
	private final Map<String, Property> properties;
	private final Map<String, IBeanGuiAdapter> adapters;
	
	public interface IBeanGuiAdapter {
		static <T> IBeanGuiAdapter of(Function<Object, T> forGui, Function<T, Object> fromGui) {
			return new IBeanGuiAdapter() {
				@Override public @Nullable Object forGui(@Nullable Object value) {
					return forGui.apply(value);
				}
				
				@SuppressWarnings("unchecked") @Override
				public @Nullable Object fromGui(@Nullable Object value) {
					try {
						return fromGui.apply((T) value);
					} catch (ClassCastException e) {
						return null;
					}
				}
			};
		}
		
		public @Nullable Object forGui(@Nullable Object value);
		public @Nullable Object fromGui(@Nullable Object value);
	}
	
	public BeanProxy(Class<B> type, Map<String, IBeanGuiAdapter> adapters) {
		this.type = type;
		this.adapters = adapters;
		try {
			Set<Property> set = utils.getProperties(type);
			properties = new LinkedHashMap<>(set.size());
			for (Property prop: set) properties.put(prop.getName(), prop);
		} catch (YAMLException e) {
			throw new ConfigBeanIntrospectionException(
			  "Invalid Java Bean for config entry: " + type.getCanonicalName(), e.getCause());
		}
	}
	
	public B create() {
		try {
			return type.getConstructor().newInstance();
		} catch (
		  InstantiationException | IllegalAccessException
		  | InvocationTargetException | NoSuchMethodException e
		) {
			throw new ConfigBeanAccessException(
			  "Cannot create instance of Java Bean for config entry: " + type.getCanonicalName(), e);
		}
	}
	
	public B createFrom(B def) {
		B bean = create();
		for (Property prop: properties.values()) {
			try {
				prop.set(bean, prop.get(def));
			} catch (Exception e) {
				throw new ConfigBeanAccessException(
				  "Cannot copy value of Java Bean property: " + type.getCanonicalName() + "$" +
				  prop.getName(), e);
			}
		}
		return bean;
	}
	
	public Object get(B bean, String name) {
		try {
			return properties.get(name).get(bean);
		} catch (YAMLException e) {
			throw new ConfigBeanAccessException(
			  "Cannot get Java Bean property " + type.getCanonicalName() + "$" + name, e);
		}
	}
	
	public Object getGUI(B bean, String name) {
		IBeanGuiAdapter adapter = adapters.get(name);
		if (adapter != null) {
			return adapter.forGui(get(bean, name));
		} else return get(bean, name);
	}
	
	public void set(B bean, String name, Object value) {
		try {
			properties.get(name).set(bean, value);
		} catch (Exception e) {
			throw new ConfigBeanAccessException(
			  "Cannot set Java Bean property " + type.getCanonicalName() + "$" + name, e);
		}
	}
	
	public void setGUI(B bean, String name, Object value) {
		IBeanGuiAdapter adapter = adapters.get(name);
		if (adapter != null) {
			set(bean, name, adapter.fromGui(value));
		} else set(bean, name, value);
	}
	
	public String getTypeName() {
		return getType().getCanonicalName();
	}
	public String getPropertyName(String name) {
		return getTypeName() + "$" + name;
	}
	
	public String getTypeTranslation() {
		return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, getType().getSimpleName());
	}
	public String getTranslation(String property) {
		return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, property);
	}
	
	public Class<B> getType() {
		return type;
	}
	
	public Map<String, Property> getPropertyMap() {
		return properties;
	}
	public Set<String> getPropertyNames() {
		return properties.keySet();
	}
	public Collection<Property> getProperties() {
		return properties.values();
	}
}
