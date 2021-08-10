package endorh.simple_config.clothconfig2.math;

public class Dimension
  implements Cloneable {
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
		return this.width;
	}
	
	public int getHeight() {
		return this.height;
	}
	
	public void setSize(double width, double height) {
		this.width = (int) Math.ceil(width);
		this.height = (int) Math.ceil(height);
	}
	
	public Dimension getSize() {
		return new Dimension(this.width, this.height);
	}
	
	public void setSize(Dimension d) {
		this.setSize(d.width, d.height);
	}
	
	public void setSize(int width, int height) {
		this.width = width;
		this.height = height;
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof Dimension) {
			Dimension d = (Dimension) obj;
			return this.width == d.width && this.height == d.height;
		}
		return false;
	}
	
	public int hashCode() {
		int result = 1;
		result = 31 * result + this.width;
		result = 31 * result + this.height;
		return result;
	}
	
	public String toString() {
		return this.getClass().getName() + "[width=" + this.width + ",height=" + this.height + "]";
	}
	
	@SuppressWarnings("MethodDoesntCallSuperMethod") public Dimension clone() {
		return this.getSize();
	}
}

