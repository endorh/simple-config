package endorh.simple_config.core.entry;

import net.minecraft.util.text.ITextComponent;

import java.util.Optional;

public interface IAbstractStringKeyEntry<V> {
	default String serializeStringKey(V key) {
		return String.valueOf(key);
	}
	
	Optional<V> deserializeStringKey(String key);
	
	default Optional<ITextComponent> stringKeyError(String key) {
		return Optional.empty();
	}
}
