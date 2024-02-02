package endorh.simpleconfig.core.wrap;

import com.electronwill.nightconfig.core.CommentedConfig;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.entry.BeanProxy;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class CommentedConfigProxy implements BeanProxy<CommentedConfig> {
   private final Map<String, AbstractConfigEntry<?, ?, ?>> entries;

   public CommentedConfigProxy(
      Map<String, AbstractConfigEntry<?, ?, ?>> entries
   ) {
      this.entries = entries;
   }


   @Override public CommentedConfig create(@Nullable Map<String, Object> properties) {
      CommentedConfig config = CommentedConfig.inMemory();
      if (properties != null) properties.forEach((name, value) -> {
         //noinspection unchecked
         AbstractConfigEntry<Object, Object, ?> entry = (AbstractConfigEntry<Object, Object, ?>) entries.get(name);
         if (entry != null) try {
            entry.put(config, entry.forConfig(value));
            return;
         } catch (RuntimeException ignored) {}
         config.set(name, value);
      });
      return config;
   }

   @Override public CommentedConfig createFrom(CommentedConfig def, @Nullable Map<String, Object> properties) {
      CommentedConfig config = CommentedConfig.copy(def);
      if (properties != null) properties.forEach((name, value) -> {
         //noinspection unchecked
         AbstractConfigEntry<Object, Object, ?> entry = (AbstractConfigEntry<Object, Object, ?>) entries.get(name);
         if (entry != null) try {
            entry.put(config, entry.forConfig(value));
            return;
         } catch (RuntimeException ignored) {}
         config.set(name, value);
      });
      return config;
   }

   @Override public CommentedConfig createFromGUI(CommentedConfig def, @Nullable Map<String, Object> properties) {
      CommentedConfig config = CommentedConfig.copy(def);
      if (properties != null) {
         properties.forEach((name, value) -> {
            //noinspection unchecked
            AbstractConfigEntry<Object, Object, Object> entry = (AbstractConfigEntry<Object, Object, Object>) entries.get(name);
            if (entry != null) try {
               entry.put(config, entry.forConfig(entry.fromGui(value)));
               return;
            } catch (RuntimeException ignored) {}
            config.set(name, value);
         });
         properties.forEach(config::set);
      }
      return config;
   }

   @Override public Object get(CommentedConfig bean, String name) {
      //noinspection unchecked
      AbstractConfigEntry<Object, Object, ?> entry = (AbstractConfigEntry<Object, Object, ?>) entries.get(name);
      if (entry != null) try {
         return entry.fromConfig(entry.get(bean));
      } catch (RuntimeException ignored) {}
      return bean.get(name);
   }

   @Override
   public Object getGUI(CommentedConfig bean, String name) {
      //noinspection unchecked
      AbstractConfigEntry<Object, Object, Object> entry = (AbstractConfigEntry<Object, Object, Object>) entries.get(name);
      if (entry != null) try {
         return entry.forGui(entry.fromConfig(entry.get(bean)));
      } catch (RuntimeException ignored) {}
      return bean.get(name);
   }

   @Override public String getTypeName() {
      return CommentedConfig.class.getCanonicalName();
   }

   @Override public String getPropertyName(String name) {
      return "Config" + "[" + name + "]";
   }

   @Override public String getTypeTranslation() {
      return CommentedConfig.class.getSimpleName();
   }

   @Override public String getTranslation(String property) {
      return property;
   }
}
