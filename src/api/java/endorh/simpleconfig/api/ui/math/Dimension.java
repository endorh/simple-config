package endorh.simpleconfig.api.ui.math;

import org.jetbrains.annotations.NotNull;

public class Dimension {
	public int width;
	public int height;
	
	public Dimension() {
		this(0, 0);
	}
	
	public Dimension(Dimension d) {
		this(d.width, d.height);
	}
	
	public Dimension(int width, int height) {
		this.width = width;
		this.height = height;
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
	public void setSize(double width, double height) {
		this.width = (int) Math.ceil(width);
		this.height = (int) Math.ceil(height);
	}
	
	public @NotNull Dimension getSize() {
		return new Dimension(width, height);
	}
	
	public void setSize(@NotNull Dimension d) {
		setSize(d.width, d.height);
	}
	
	public void setSize(int width, int height) {
		this.width = width;
		this.height = height;
	}
	
	public @NotNull Dimension copy() {
		return getSize();
	}
	
	@Override public boolean equals(Object obj) {
		if (obj instanceof Dimension d) {
			return width == d.width && height == d.height;
		}
		return false;
	}
	
	@Override public int hashCode() {
		int result = 1;
		result = 31 * result + width;
		result = 31 * result + height;
		return result;
	}
	
	@Override public String toString() {
		return getClass().getName() + "[width=" + width + ",height=" + height + "]";
	}
}

