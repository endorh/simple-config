package endorh.simpleconfig.api;

import endorh.simpleconfig.api.SimpleConfig.NoSuchConfigGroupError;

public interface SimpleConfigGroup extends ConfigEntryHolder {
	/**
	 * Get the parent category of this group
	 */
	SimpleConfigCategory getCategory();
	
	/**
	 * Get a config subgroup
	 *
	 * @param path Name or dot-separated path to the group
	 * @throws NoSuchConfigGroupError if the group is not found
	 */
	SimpleConfigGroup getGroup(String path);
}
