package endorh.simpleconfig.api;

import endorh.simpleconfig.api.ISimpleConfig.NoSuchConfigGroupError;

public interface ISimpleConfigCategory extends ISimpleConfigEntryHolder {
	/**
	 * Get a config group
	 *
	 * @param path Name or dot-separated path to the group
	 * @throws NoSuchConfigGroupError if the group is not found
	 */
	@SuppressWarnings("unused") ISimpleConfigGroup getGroup(String path);
}
