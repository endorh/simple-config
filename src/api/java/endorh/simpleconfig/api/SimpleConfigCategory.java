package endorh.simpleconfig.api;

import endorh.simpleconfig.api.SimpleConfig.NoSuchConfigGroupError;
import org.jetbrains.annotations.NotNull;

public interface SimpleConfigCategory extends ConfigEntryHolder {
	/**
	 * Get a config group
	 *
	 * @param path Name or dot-separated path to the group
	 * @throws NoSuchConfigGroupError if the group is not found
	 */
	@NotNull SimpleConfigGroup getGroup(String path);
}
