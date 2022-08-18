package endorh.simpleconfig.ui.math;

import net.minecraft.util.math.MathHelper;

public class Point implements Cloneable {
	public int x;
	public int y;
	
	public static Point of(int x, int y) {
		return new Point(x, y);
	}
	
	public static Point of(double x, double y) {
		return new Point((int) x, (int) y);
	}
	
	public static Point origin() {
		return new Point(0, 0);
	}
	
	private Point(int x, int y) {
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
		return Point.of(this.x, this.y);
	}
	
	@Override @SuppressWarnings("MethodDoesntCallSuperMethod") public Point clone() {
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
	
	@Override public boolean equals(Object obj) {
		if (obj instanceof Point) {
			Point pt = (Point) obj;
			return this.x == pt.x && this.y == pt.y;
		}
		return super.equals(obj);
	}
	
	@Override public int hashCode() {
		int result = 1;
		result = 31 * result + this.x;
		result = 31 * result + this.y;
		return result;
	}
	
	@Override public String toString() {
		return this.getClass().getName() + "[x=" + this.x + ",y=" + this.y + "]";
	}
	
	public float distance(float x, float y) {
		float dx = x - getX(), dy = y - getY();
		return MathHelper.sqrt(dx * dx + dy * dy);
	}
	
	public double distance(double x, double y) {
		double dx = x - getX(), dy = y - getY();
		return MathHelper.sqrt(dx * dx + dy * dy);
	}
	
	public float distance(Point point) {
		return distance(point.getX(), point.getY());
	}
}

