package endorh.simpleconfig.ui.gui.entries;

public interface IRangedEntry<V extends Comparable<V>> {
	void setMinimum(V min);
	void setMaximum(V max);
}
