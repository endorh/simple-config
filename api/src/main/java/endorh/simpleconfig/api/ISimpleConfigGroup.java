package endorh.simpleconfig.api;

import endorh.simpleconfig.api.ISimpleConfig.NoSuchConfigGroupError;

public interface ISimpleConfigGroup extends ISimpleConfigEntryHolder {
	/**
	 * Get the parent category of this group
	 */
	ISimpleConfigCategory getCategory();
	
	/**
	 * Get a config subgroup
	 *
	 * @param path Name or dot-separated path to the group
	 * @throws NoSuchConfigGroupError if the group is not found
	 */
	ISimpleConfigGroup getGroup(String path);
}
