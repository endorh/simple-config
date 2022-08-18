package endorh.simpleconfig.ui.math;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class Rectangle {
	public int x;
	public int y;
	public int width;
	public int height;
	
	public Rectangle() {
		this(0, 0, 0, 0);
	}
	
	public Rectangle(Rectangle r) {
		this(r.x, r.y, r.width, r.height);
	}
	
	public Rectangle(int width, int height) {
		this(0, 0, width, height);
	}
	
	public Rectangle(Point p, Dimension d) {
		this(p.x, p.y, d.width, d.height);
	}
	
	public Rectangle(Point p) {
		this(p.x, p.y, 0, 0);
	}
	
	public Rectangle(Dimension d) {
		this(0, 0, d.width, d.height);
	}
	
	public Rectangle(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}
	
	public int getX() {
		return x;
	}
	public int getMinX() {
		return x;
	}
	public int getMaxX() {
		return x + width;
	}
	public int getCenterX() {
		return x + width / 2;
	}
	
	public int getY() {
		return y;
	}
	public int getMinY() {
		return y;
	}
	public int getMaxY() {
		return y + height;
	}
	public int getCenterY() {
		return y + height / 2;
	}
	
	public int getWidth() {
		return width;
	}
	public int getHeight() {
		return height;
	}
	
	public Rectangle getBounds() {
		return new Rectangle(x, y, width, height);
	}
	public void setBounds(Rectangle r) {
		setBounds(r.x, r.y, r.width, r.height);
	}
	public void setBounds(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}
	
	public Point getLocation() {
		return Point.of(x, y);
	}
	public void setLocation(Point p) {
		setLocation(p.x, p.y);
	}
	public void setLocation(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public void translate(int dx, int dy) {
		x += dx;
		y += dy;
	}
	
	public Rectangle copy() {
		return getBounds();
	}
	
	public Dimension getSize() {
		return new Dimension(width, height);
	}
	public void setSize(Dimension d) {
		setSize(d.width, d.height);
	}
	public void setSize(int width, int height) {
		this.width = width;
		this.height = height;
	}
	
	public boolean contains(Point p) {
		return contains(p.x, p.y);
	}
	public boolean contains(int xx, int yy) {
		if ((width | height) < 0) return false;
		return xx >= x && yy >= y && xx < x + width && yy < y + height;
	}
	public boolean contains(double x, double y) {
		return contains((int) x, (int) y);
	}
	public boolean contains(Rectangle r) {
		return contains(r.x, r.y, r.width, r.height);
	}
	public boolean contains(int xx, int yy, int ww, int hh) {
		return (width | height | ww | hh) >= 0
		       && xx >= x && yy >= y
		       && xx + ww <= x + width
		       && yy + hh <= y + height;
	}
	
	public boolean intersects(Rectangle r) {
		return (r.width | r.height | width | height) >= 0
		       && r.x + r.width > x && r.y + r.height > y
		       && x + width > r.x && y + height > r.y;
	}
	public int horizontalIntersection(Rectangle r) {
		return min(getMaxX(), r.getMaxX()) - max(x, r.x);
	}
	public int verticalIntersection(Rectangle r) {
		return min(getMaxY(), r.getMaxY()) - max(y, r.y);
	}
	
	public Rectangle intersection(Rectangle r) {
		int x = this.x;
		int y = this.y;
		int rx = r.x;
		int ry = r.y;
		long w = (long) x + width;
		long h = (long) y + height;
		long rw = (long) rx + r.width;
		long rh = (long) ry + r.height;
		if (x < rx) x = rx;
		if (y < ry) y = ry;
		if (w > rw) w = rw;
		if (h > rh) h = rh;
		if ((w -= x) < Integer.MIN_VALUE) w = Integer.MIN_VALUE;
		if ((h -= y) < Integer.MIN_VALUE) h = Integer.MIN_VALUE;
		return new Rectangle(x, y, (int) w, (int) h);
	}
	
	public Rectangle union(Rectangle r) {
		long w = width;
		long h = height;
		if ((w | h) < 0L) return new Rectangle(r);
		long rw = r.width;
		long rh = r.height;
		if ((rw | rh) < 0L) return new Rectangle(this);
		int x = this.x;
		int y = this.y;
		w += x;
		h += y;
		int rx = r.x;
		int ry = r.y;
		rw += rx;
		rh += ry;
		if (x > rx) x = rx;
		if (y > ry) y = ry;
		if (w < rw) w = rw;
		if (h < rh) h = rh;
		if ((w -= x) > Integer.MAX_VALUE) w = Integer.MAX_VALUE;
		if ((h -= y) > Integer.MAX_VALUE) h = Integer.MAX_VALUE;
		return new Rectangle(x, y, (int) w, (int) h);
	}
	
	public void add(int addX, int addY) {
		if ((width | height) < 0) {
			x = addX;
			y = addY;
			height = 0;
			width = 0;
			return;
		}
		int x = this.x;
		int y = this.y;
		long w = (long) width + x;
		long h = (long) height + y;
		if (x > addX) x = addX;
		if (y > addY) y = addY;
		if (w < (long) addX) w = addX;
		if (h < (long) addY) h = addY;
		if ((w -= x) > Integer.MAX_VALUE) w = Integer.MAX_VALUE;
		if ((h -= y) > Integer.MAX_VALUE) h = Integer.MAX_VALUE;
		setBounds(x, y, (int) w, (int) h);
	}
	public void add(Point pt) {
		add(pt.x, pt.y);
	}
	public void add(Rectangle r) {
		long w = width;
		long h = height;
		if ((w | h) < 0L) setBounds(r.x, r.y, r.width, r.height);
		long rw = r.width;
		long rh = r.height;
		if ((rw | rh) < 0L) return;
		int x = this.x;
		int y = this.y;
		w += x;
		h += y;
		int rx = r.x;
		int ry = r.y;
		rw += rx;
		rh += ry;
		if (x > rx) x = rx;
		if (y > ry) y = ry;
		if (w < rw) w = rw;
		if (h < rh) h = rh;
		if ((w -= x) > Integer.MAX_VALUE) w = Integer.MAX_VALUE;
		if ((h -= y) > Integer.MAX_VALUE) h = Integer.MAX_VALUE;
		setBounds(x, y, (int) w, (int) h);
	}
	
	public Rectangle grow(int left, int up, int right, int down) {
		return new Rectangle(x - left, y - up, width + left + right, height + up + down);
	}
	public void grow(int h, int v) {
		long x = this.x;
		long y = this.y;
		long ww = width;
		long hh = height;
		ww += x;
		hh += y;
		y -= v;
		hh += v;
		if ((ww += h) < (x -= h)) {
			if ((ww -= x) < Integer.MIN_VALUE) ww = Integer.MIN_VALUE;
			if (x < Integer.MIN_VALUE) {
				x = Integer.MIN_VALUE;
			} else if (x > Integer.MAX_VALUE) x = Integer.MAX_VALUE;
		} else {
			if (x < Integer.MIN_VALUE) {
				x = Integer.MIN_VALUE;
			} else if (x > Integer.MAX_VALUE) x = Integer.MAX_VALUE;
			if ((ww -= x) > Integer.MAX_VALUE) ww = Integer.MAX_VALUE;
		}
		if (hh < y) {
			if ((hh -= y) < Integer.MIN_VALUE) hh = Integer.MIN_VALUE;
			if (y < Integer.MIN_VALUE) {
				y = Integer.MIN_VALUE;
			} else if (y > Integer.MAX_VALUE) y = Integer.MAX_VALUE;
		} else {
			if (y < Integer.MIN_VALUE) {
				y = Integer.MIN_VALUE;
			} else if (y > Integer.MAX_VALUE) y = Integer.MAX_VALUE;
			if ((hh -= y) > Integer.MAX_VALUE) hh = Integer.MAX_VALUE;
		}
		setBounds((int) x, (int) y, (int) ww, (int) hh);
	}
	
	public boolean isEmpty() {
		return width <= 0 || height <= 0;
	}
	
	@Override public boolean equals(Object obj) {
		if (obj instanceof Rectangle) {
			Rectangle r = (Rectangle) obj;
			return x == r.x && y == r.y && width == r.width && height == r.height;
		}
		return super.equals(obj);
	}
	
	@Override public String toString() {
		return getClass().getName() +
		       "[x=" + x + ",y=" + y + ",width=" + width +
		       ",height=" + height + "]";
	}
	
	@Override public int hashCode() {
		int result = 1;
		result = 31 * result + x;
		result = 31 * result + y;
		result = 31 * result + width;
		result = 31 * result + height;
		return result;
	}
}

