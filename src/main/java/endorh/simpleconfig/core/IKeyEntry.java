package endorh.simpleconfig.core;

import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Optional;

/**
 * Marker interface for {@link endorh.simpleconfig.core.AbstractConfigEntry}s
 * that produce {@link IChildListEntry}
 * gui entries and thus can be used as keys in map entries
 */
public interface IKeyEntry<KG> {
	@OnlyIn(Dist.CLIENT) default <
	  KGE extends AbstractConfigListEntry<KG> & IChildListEntry,
	  KGEB extends FieldBuilder<KG, KGE, KGEB>
	> KGEB buildChildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		if (this instanceof AbstractConfigEntry) {
			//noinspection unchecked
			final Optional<FieldBuilder<KG, ?, ?>> opt =
			  ((AbstractConfigEntry<?, ?, KG>) this).buildGUIEntry(builder);
			if (!opt.isPresent())
				throw new IllegalStateException(
				  "Config entry of type " + getClass().getSimpleName() + " did not produce " +
				  "any gui entry, violating its IKeyEntry contract");
			final FieldBuilder<KG, ?, ?> fieldBuilder = opt.get();
			if (!IChildListEntry.class.isAssignableFrom(fieldBuilder.getEntryClass()))
				throw new IllegalStateException(
				  "Config entry of type " + getClass().getSimpleName() + " produced a " +
				  "gui entry that cannot be used as key, violating its IKeyEntry contract");
			
			// Child entries are persistent through their parents
			fieldBuilder.withoutTags(EntryTag.NON_PERSISTENT);
			//noinspection unchecked
			return (KGEB) fieldBuilder;
		} else throw new IllegalStateException(
		  "The IKeyEntry interface should only be implemented by " +
		  "endorh.simpleconfig.core.AbstractConfigEntry objects");
	}
}
