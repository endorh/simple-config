package endorh.simpleconfig.core.entry;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.google.common.collect.Maps;
import endorh.simpleconfig.api.AtomicEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.EntryTag;
import endorh.simpleconfig.api.entry.CommentedConfigEntryBuilder;
import endorh.simpleconfig.api.ui.icon.Icon;
import endorh.simpleconfig.core.*;
import endorh.simpleconfig.core.wrap.CommentedConfigProxy;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.BeanFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static endorh.simpleconfig.core.entry.BeanEntry.addCaption;

public class CommentedConfigEntry extends AbstractConfigEntry<CommentedConfig, CommentedConfig, CommentedConfig> {
   private static final Logger LOGGER = LogManager.getLogger();

   private final CommentedConfigProxy proxy;
   private final Map<String, AbstractConfigEntry<?, ?, ?>> entries;
   private @Nullable String caption;
   private @Nullable Function<CommentedConfig, Icon> iconProvider = null;
   private final boolean hasSubPresentation;

   protected CommentedConfigEntry(
      ConfigEntryHolder parent, String name, CommentedConfig defValue,
      CommentedConfigProxy proxy, Map<String, AbstractConfigEntry<?, ?, ?>> entries
   ) {
      super(parent, name, defValue);
      this.proxy = proxy;
      this.entries = entries;
      hasSubPresentation = entries.values().stream().anyMatch(AbstractConfigEntry::hasPresentation);
   }

   public CommentedConfigProxy getProxy() {
      return proxy;
   }

   public AbstractConfigEntry<?, ?, ?> getEntry(String name) {
      return entries.get(name);
   }

   public static class Builder extends AbstractConfigEntryBuilder<
      CommentedConfig, CommentedConfig, CommentedConfig, CommentedConfigEntry,
      CommentedConfigEntryBuilder, Builder
   > implements CommentedConfigEntryBuilder {
      private final Map<String, AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?>> entries = new LinkedHashMap<>();
      private @Nullable String caption;
      private @Nullable Function<CommentedConfig, Icon> iconProvider = null;


      public Builder(CommentedConfig value) {
         super(value, EntryType.unchecked(CommentedConfig.class));
      }

      @Override public @NotNull Builder add(String name, ConfigEntryBuilder<?, ?, ?, ?> entryBuilder) {
         Builder copy = copy();
         if (!(entryBuilder instanceof AbstractConfigEntryBuilder)) throw new IllegalArgumentException(
            "ConfigEntryBuilder not instance of AbstractConfigEntryBuilder");
         copy.entries.put(name, (AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?>) entryBuilder);
         return copy;
      }

      @Override public @NotNull <CB extends ConfigEntryBuilder<?, ?, ?, ?> & AtomicEntryBuilder> CommentedConfigEntryBuilder caption(String name, CB entryBuilder) {
         Builder copy = add(name, entryBuilder);
         copy.caption = name;
         return copy;
      }

      @Override public @NotNull CommentedConfigEntryBuilder withoutCaption() {
         Builder copy = copy();
         copy.caption = null;
         return copy;
      }

      @Override public @NotNull CommentedConfigEntryBuilder withIcon(Function<CommentedConfig, Icon> icon) {
         Builder copy = copy();
         copy.iconProvider = icon;
         return copy;
      }

      @Override protected CommentedConfigEntry buildEntry(ConfigEntryHolder parent, String name) {
         Map<String, AbstractConfigEntry<?, ?, ?>> entries = new LinkedHashMap<>();
         this.entries.forEach((n, e) -> entries.put(n, DummyEntryHolder.build(parent, e)));
         CommentedConfigProxy proxy = new CommentedConfigProxy(entries);
         CommentedConfigEntry entry = new CommentedConfigEntry(parent, name, value, proxy, entries);
         String prefix = entry.getTranslation();
         entries.forEach((n, e) -> {
            String propKey = proxy.getTranslation(n);
            String key = prefix + propKey;
            e.setTranslation(key);
            e.setTooltipKey(key + ":help");
            e.setName(propKey);
         });
         entry.caption = caption;
         entry.iconProvider = iconProvider;
         return entry;
      }

      @Override protected Builder createCopy(CommentedConfig value) {
         Builder copy = new Builder(value);
         copy.entries.putAll(entries);
         copy.caption = caption;
         copy.iconProvider = iconProvider;
         return copy;
      }

      private static <V, G> BeanProxy.IBeanGuiAdapter createAdapter(AbstractConfigEntry<V, ?, G> entry) {
         return BeanProxy.IBeanGuiAdapter.of(v -> {
            try {
               //noinspection unchecked
               return entry.forGui((V) v);
            } catch (ClassCastException e) {
               return null;
            }
         }, entry::fromGui);
      }
   }

   @Override public boolean hasPresentation() {
      return super.hasPresentation() || hasSubPresentation;
   }

   @Override protected CommentedConfig doForPresentation(CommentedConfig value) {
      if (!hasSubPresentation) return super.doForPresentation(value);
      //noinspection unchecked
      return super.doForPresentation(proxy.createFrom(value, Maps.transformEntries(
         Maps.filterEntries(entries, e -> e.getValue().hasPresentation()),
         (n, v) -> ((AbstractConfigEntry<Object, ?, ?>) v).forPresentation(proxy.get(value, n))
      )));
   }

   @Override protected CommentedConfig doFromPresentation(CommentedConfig value) {
      value = super.doFromPresentation(value);
      if (!hasSubPresentation) return value;
      final CommentedConfig vv = value;
      //noinspection unchecked
      return proxy.createFrom(value, Maps.transformEntries(
         Maps.filterEntries(entries, e -> e.getValue().hasPresentation()),
         (n, v) -> ((AbstractConfigEntry<Object, ?, ?>) v).fromPresentation(proxy.get(vv, n))
      ));
   }

   @Override public Optional<FieldBuilder<CommentedConfig, ?, ?>> buildGUIEntry(ConfigFieldBuilder builder) {
      BeanFieldBuilder<CommentedConfig> fieldBuilder = builder
         .startBeanField(getDisplayName(), forGui(get()), proxy)
         .withIcon(iconProvider)
         .overrideEquals();
      entries.forEach((name, entry) -> {
         if (name.equals(caption)) {
            if (entry instanceof AtomicEntry<?> keyEntry) {
               addCaption(
                  builder, fieldBuilder.withoutTags(EntryTag.NON_PERSISTENT, entry.copyTag),
                  name, keyEntry);
               return;
            } else LOGGER.warn("Caption entry {} is not atomic, ignoring", getGlobalPath());
         }
         entry.buildGUIEntry(builder).ifPresent(
            g -> fieldBuilder.add(name, g.withoutTags(EntryTag.NON_PERSISTENT, entry.copyTag)));
      });
      return Optional.of(decorate(fieldBuilder));
   }
}
