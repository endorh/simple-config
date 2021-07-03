package endorh.simple_config.core.entry;

import endorh.simple_config.core.AbstractConfigEntry;

public class EmptyEntry<Self extends EmptyEntry<Self>> extends AbstractConfigEntry<Void, Void, Void, Self> {
	public EmptyEntry() {
		super(null, Void.class);
	}
}
