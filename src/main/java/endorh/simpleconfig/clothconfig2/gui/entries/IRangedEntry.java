package endorh.simpleconfig.clothconfig2.gui.entries;

public interface IRangedEntry<V extends Comparable<V>> {
	void setMinimum(V min);
	void setMaximum(V max);
}
