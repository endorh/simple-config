package endorh.simpleconfig.api.entry;

public interface ISerializableConfigEntry<T extends ISerializableConfigEntry<T>> {
	/**
	 * An {@link ConfigEntrySerializer} for this entry type
	 */
	ConfigEntrySerializer<T> getConfigSerializer();
	
	/**
	 * The required type for backing fields of this entry type
	 */
	Class<?> getConfigEntryTypeClass();
}
