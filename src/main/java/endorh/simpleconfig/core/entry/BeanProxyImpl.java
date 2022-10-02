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

public class BeanProxyImpl<B> implements BeanProxy<B> {
	private static final PropertyUtils utils = new PropertyUtils();
	private final Class<B> type;
	private final Map<String, Property> properties;
	private final Map<String, IBeanGuiAdapter> adapters;
	
	public BeanProxyImpl(Class<B> type, Map<String, IBeanGuiAdapter> adapters) {
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
	
	@Override public B create(@Nullable Map<String, Object> props) {
		try {
			B bean = type.getConstructor().newInstance();
			if (props != null) props.forEach((name, value) -> {
				Property prop = properties.get(name);
				if (prop != null) {
					try {
						prop.set(bean, value);
					} catch (Exception e) {
						throw new ConfigBeanAccessException(
						  "Cannot copy value of Java Bean property: " + type.getCanonicalName() + "$" +
						  prop.getName(), e);
					}
				}
			});
			return bean;
		} catch (
		  InstantiationException | IllegalAccessException
		  | InvocationTargetException | NoSuchMethodException e
		) {
			throw new ConfigBeanAccessException(
			  "Cannot create instance of Java Bean for config entry: " + type.getCanonicalName(), e);
		}
	}
	
	@Override public B createFrom(B def, @Nullable Map<String, Object> props) {
		B bean = create();
		for (Property prop: properties.values()) {
			String name = prop.getName();
			try {
				prop.set(
				  bean, props != null && props.containsKey(name)
				        ? props.get(name) : prop.get(def));
			} catch (Exception e) {
				throw new ConfigBeanAccessException(
				  "Cannot copy value of Java Bean property: " + type.getCanonicalName() + "$" + name, e);
			}
		}
		return bean;
	}
	
	@Override public B createFromGUI(B def, @Nullable Map<String, Object> props) {
		B bean = create();
		for (Property prop: properties.values()) {
			String name = prop.getName();
			try {
				IBeanGuiAdapter adapter = adapters.get(name);
				prop.set(
				  bean, props != null && props.containsKey(name)
				        ? adapter != null? adapter.fromGui(props.get(name)) : props.get(name)
				        : prop.get(def));
			} catch (Exception e) {
				throw new ConfigBeanAccessException(
				  "Cannot copy value of Java Bean property: " + type.getCanonicalName() + "$" + name, e);
			}
		}
		return bean;
	}
	
	@Override public Object get(B bean, String name) {
		try {
			return properties.get(name).get(bean);
		} catch (YAMLException e) {
			throw new ConfigBeanAccessException(
			  "Cannot get Java Bean property " + type.getCanonicalName() + "$" + name, e);
		}
	}
	
	@Override public Object getGUI(B bean, String name) {
		IBeanGuiAdapter adapter = adapters.get(name);
		if (adapter != null) {
			return adapter.forGui(get(bean, name));
		} else return get(bean, name);
	}
	
	@Override public String getTypeName() {
		return getType().getCanonicalName();
	}
	@Override public String getPropertyName(String name) {
		return getTypeName() + "$" + name;
	}
	
	@Override public String getTypeTranslation() {
		return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, getType().getSimpleName());
	}
	@Override public String getTranslation(String property) {
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
