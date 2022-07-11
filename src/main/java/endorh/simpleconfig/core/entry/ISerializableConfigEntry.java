package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.clothconfig2.api.ITextFormatter;

public interface ISerializableConfigEntry<T extends ISerializableConfigEntry<T>> {
	/**
	 * An {@link IConfigEntrySerializer} for this entry type
	 */
	IConfigEntrySerializer<T> getConfigSerializer();
	
	/**
	 * The required type for backing fields of this entry type
	 */
	Class<?> getConfigEntryTypeClass();
}
