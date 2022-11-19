package endorh.simpleconfig.core.wrap;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Used by delegated entries that wrap config values from other backends
 * different from {@link ForgeConfigSpec}.
 */
public interface ConfigEntryDelegate<C> {
	C getValue();
	void setValue(C value);
}