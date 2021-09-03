package endorh.simple_config.core;

import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.ConfigEntryBuilder;
import endorh.simple_config.clothconfig2.api.IChildListEntry;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Marker interface for {@link endorh.simple_config.core.AbstractConfigEntry}s
 * that produce {@link IChildListEntry}
 * gui entries and thus can be used as keys in map entries
 */
public interface IKeyEntry<KC, KG> {
	default <KGE extends AbstractConfigListEntry<KG> & IChildListEntry> KGE buildChildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		if (this instanceof AbstractConfigEntry) {
			//noinspection unchecked
			final Optional<? extends AbstractConfigListEntry<KG>> opt =
			  ((AbstractConfigEntry<?, ?, KG, ?>) this).buildGUIEntry(builder);
			if (!opt.isPresent())
				throw new IllegalStateException(
				  "Config entry of type " + getClass().getSimpleName() + " did not produce " +
				  "any gui entry, violating its IKeyEntry contract");
			final AbstractConfigListEntry<KG> guiEntry = opt.get();
			if (!(guiEntry instanceof IChildListEntry))
				throw new IllegalStateException(
				  "Config entry of type " + getClass().getSimpleName() + " produced a " +
				  "gui entry that cannot be used as key, violating its IKeyEntry contract");
			//noinspection unchecked
			((AbstractConfigEntry<?, ?, KG, ?>) this).guiEntry = guiEntry;
			//noinspection unchecked
			return (KGE) guiEntry;
		} else throw new IllegalStateException(
		  "The IKeyEntry interface should be implemented by " +
		  "endorh.simple_config.core.AbstractConfigEntry objects");
	}
	
	Optional<KC> deserializeStringKey(@NotNull String key);
	default String serializeStringKey(@NotNull KC key) {
		return String.valueOf(key);
	}
}
