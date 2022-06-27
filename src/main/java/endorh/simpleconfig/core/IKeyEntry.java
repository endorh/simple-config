package endorh.simpleconfig.core;

import endorh.simpleconfig.clothconfig2.api.AbstractConfigListEntry;
import endorh.simpleconfig.clothconfig2.api.ConfigEntryBuilder;
import endorh.simpleconfig.clothconfig2.api.EntryFlag;
import endorh.simpleconfig.clothconfig2.api.IChildListEntry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Marker interface for {@link endorh.simpleconfig.core.AbstractConfigEntry}s
 * that produce {@link IChildListEntry}
 * gui entries and thus can be used as keys in map entries
 */
public interface IKeyEntry<KC, KG> {
	@OnlyIn(Dist.CLIENT) default <KGE extends AbstractConfigListEntry<KG> & IChildListEntry> KGE buildChildGUIEntry(
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
			// Child entries are persistent through their parents
			guiEntry.removeEntryFlag(EntryFlag.NON_PERSISTENT);
			//noinspection unchecked
			return (KGE) guiEntry;
		} else throw new IllegalStateException(
		  "The IKeyEntry interface should only be implemented by " +
		  "endorh.simpleconfig.core.AbstractConfigEntry objects");
	}
	
	Optional<KC> deserializeStringKey(@NotNull String key);
	default String serializeStringKey(@NotNull KC key) {
		return String.valueOf(key);
	}
}
