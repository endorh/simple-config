package endorh.simpleconfig.core.wrap;

import endorh.simpleconfig.core.entry.BeanProxy;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class MapProxy implements BeanProxy<Map<String, Object>> {
   private final Map<String, IBeanGuiAdapter> adapters;

   public MapProxy(Map<String, IBeanGuiAdapter> adapters) {
      this.adapters = adapters;
   }

   @Override public Map<String, Object> create(@Nullable Map<String, Object> properties) {
      return properties == null ? new LinkedHashMap<>() : new LinkedHashMap<>(properties);
   }

   @Override public Map<String, Object> createFrom(Map<String, Object> def, @Nullable Map<String, Object> properties) {
      Map<String, Object> map = new LinkedHashMap<>(def);
      if (properties != null) map.putAll(properties);
      return map;
   }

   @Override public Map<String, Object> createFromGUI(Map<String, Object> def, @Nullable Map<String, Object> properties) {
      Map<String, Object> map = new LinkedHashMap<>(def);
      if (properties != null) for (Map.Entry<String, Object> entry : properties.entrySet()) {
         IBeanGuiAdapter adapter = adapters.get(entry.getKey());
         if (adapter != null) {
            map.put(entry.getKey(), adapter.fromGui(entry.getValue()));
         } else map.put(entry.getKey(), entry.getValue());
      }
      return map;
   }

   @Override public Object get(Map<String, Object> bean, String name) {
      return bean.get(name);
   }

   @Override public Object getGUI(Map<String, Object> bean, String name) {
      IBeanGuiAdapter adapter = adapters.get(name);
      return adapter != null? adapter.forGui(bean.get(name)) : bean.get(name);
   }

   @Override public String getTypeName() {
      return Map.class.getCanonicalName();
   }
   @Override public String getPropertyName(String name) {
      return "Map" + "[" + name + "]";
   }
   @Override public String getTypeTranslation() {
      return Map.class.getSimpleName();
   }
   @Override public String getTranslation(String property) {
      return property;
   }
}
