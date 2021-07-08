package endorh.simple_config.core;

import endorh.simple_config.core.entry.IAbstractStringKeyEntry;
import net.minecraft.util.text.ITextComponent;

import java.util.Optional;

public interface IStringKeyEntry<K> extends IAbstractStringKeyEntry<K> {
	Optional<ITextComponent> supplyError(K key);
	ITextComponent getKeySerializationError(String key);
	
	@Override
	default Optional<ITextComponent> stringKeyError(String key) {
		final Optional<K> opt = deserializeStringKey(key);
		if (!opt.isPresent())
			return Optional.of(getKeySerializationError(key));
		return supplyError(opt.get());
	}
}
