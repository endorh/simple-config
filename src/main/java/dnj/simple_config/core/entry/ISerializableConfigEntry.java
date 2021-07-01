package dnj.simple_config.core.entry;

public interface ISerializableConfigEntry<T extends ISerializableConfigEntry<T>> {
	IConfigEntrySerializer<T> getConfigSerializer();
}
