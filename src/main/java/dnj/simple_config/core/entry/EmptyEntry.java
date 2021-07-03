package dnj.simple_config.core.entry;

import dnj.simple_config.core.AbstractConfigEntry;

public class EmptyEntry<Self extends EmptyEntry<Self>> extends AbstractConfigEntry<Void, Void, Void, Self> {
	public EmptyEntry() {
		super(null, Void.class);
	}
}
