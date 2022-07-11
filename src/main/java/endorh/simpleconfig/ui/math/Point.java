package endorh.simpleconfig.ui.math;

public class Point
  implements Cloneable {
	public int x;
	public int y;
	
	public Point() {
		this(0, 0);
	}
	
	public Point(Point p) {
		this(p.x, p.y);
	}
	
	public Point(double x, double y) {
		this((int) x, (int) y);
	}
	
	public Point(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public int getX() {
		return this.x;
	}
	
	public int getY() {
		return this.y;
	}
	
	public Point getLocation() {
		return new Point(this.x, this.y);
	}
	
	@SuppressWarnings("MethodDoesntCallSuperMethod") public Point clone() {
		return this.getLocation();
	}
	
	public void setLocation(double x, double y) {
		this.x = (int) Math.floor(x + 0.5);
		this.y = (int) Math.floor(y + 0.5);
	}
	
	public void move(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public void translate(int dx, int dy) {
		this.x += dx;
		this.y += dy;
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof Point) {
			Point pt = (Point) obj;
			return this.x == pt.x && this.y == pt.y;
		}
		return super.equals(obj);
	}
	
	public int hashCode() {
		int result = 1;
		result = 31 * result + this.x;
		result = 31 * result + this.y;
		return result;
	}
	
	public String toString() {
		return this.getClass().getName() + "[x=" + this.x + ",y=" + this.y + "]";
	}
}

