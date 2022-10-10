package endorh.simpleconfig.api.ui.math;

import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class Point implements Cloneable {
	public int x;
	public int y;
	
	public static @NotNull Point of(int x, int y) {
		return new Point(x, y);
	}
	
	public static @NotNull Point of(double x, double y) {
		return new Point((int) x, (int) y);
	}
	
	public static @NotNull Point origin() {
		return new Point(0, 0);
	}
	
	private Point(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	
	public @NotNull Point getPos() {
		return Point.of(x, y);
	}
	
	@Override @SuppressWarnings("MethodDoesntCallSuperMethod") public @NotNull Point clone() {
		return getPos();
	}
	
	public void setPos(double x, double y) {
		this.x = (int) Math.floor(x + 0.5);
		this.y = (int) Math.floor(y + 0.5);
	}
	
	public void move(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public void translate(int dx, int dy) {
		x += dx;
		y += dy;
	}
	
	@Override public boolean equals(Object obj) {
		if (obj instanceof Point pt) {
			return x == pt.x && y == pt.y;
		}
		return super.equals(obj);
	}
	
	@Override public int hashCode() {
		int result = 1;
		result = 31 * result + x;
		result = 31 * result + y;
		return result;
	}
	
	@Override public String toString() {
		return getClass().getName() + "[x=" + x + ",y=" + y + "]";
	}
	
	public float distance(float x, float y) {
		float dx = x - getX(), dy = y - getY();
		return Mth.sqrt(dx * dx + dy * dy);
	}
	
	public double distance(double x, double y) {
		double dx = x - getX(), dy = y - getY();
		return Mth.sqrt((float) (dx * dx + dy * dy));
	}
	
	public float distance(Point point) {
		return distance(point.getX(), point.getY());
	}
}

