package dnj.simple_config.core.entry;

import dnj.simple_config.core.AbstractConfigEntry;

public abstract class RangedEntry
  <V, Config, Gui, This extends RangedEntry<V, Config, Gui, This>>
  extends AbstractConfigEntry<V, Config, Gui, This> {
	public V min;
	public V max;
	protected boolean asSlider = false;
	
	public RangedEntry(V value, V min, V max) {
		super(value);
		this.min = min;
		this.max = max;
	}
	
	public This min(V min) {
		this.min = min;
		return self();
	}
	
	public This max(V max) {
		this.max = max;
		return self();
	}
}
