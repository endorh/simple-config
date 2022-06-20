package endorh.simpleconfig.clothconfig2.math;

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
		return this.x;
	}
	
	public int getMinX() {
		return this.x;
	}
	
	public int getMaxX() {
		return this.x + this.width;
	}
	
	public int getCenterX() {
		return this.x + this.width / 2;
	}
	
	public int getY() {
		return this.y;
	}
	
	public int getMinY() {
		return this.y;
	}
	
	public int getMaxY() {
		return this.y + this.height;
	}
	
	public int getCenterY() {
		return this.y + this.height / 2;
	}
	
	public int getWidth() {
		return this.width;
	}
	
	public int getHeight() {
		return this.height;
	}
	
	public Rectangle getBounds() {
		return new Rectangle(this.x, this.y, this.width, this.height);
	}
	
	public void setBounds(Rectangle r) {
		this.setBounds(r.x, r.y, r.width, r.height);
	}
	
	public void setBounds(int x, int y, int width, int height) {
		this.reshape(x, y, width, height);
	}
	
	@Deprecated
	public void reshape(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}
	
	public Point getLocation() {
		return new Point(this.x, this.y);
	}
	
	public void setLocation(Point p) {
		this.setLocation(p.x, p.y);
	}
	
	public void setLocation(int x, int y) {
		this.move(x, y);
	}
	
	@Deprecated
	public void move(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public void translate(int dx, int dy) {
		this.x += dx;
		this.y += dy;
	}
	
	public Rectangle copy() {
		return this.getBounds();
	}
	
	public Dimension getSize() {
		return new Dimension(this.width, this.height);
	}
	
	public void setSize(Dimension d) {
		this.setSize(d.width, d.height);
	}
	
	public void setSize(int width, int height) {
		this.resize(width, height);
	}
	
	@Deprecated
	public void resize(int width, int height) {
		this.width = width;
		this.height = height;
	}
	
	public boolean contains(Point p) {
		return this.contains(p.x, p.y);
	}
	
	public boolean contains(int x, int y) {
		return this.inside(x, y);
	}
	
	public boolean contains(double x, double y) {
		return this.inside((int) x, (int) y);
	}
	
	public boolean contains(Rectangle r) {
		return this.contains(r.x, r.y, r.width, r.height);
	}
	
	public boolean contains(int X, int Y, int W, int H) {
		int w = this.width;
		int h = this.height;
		if ((w | h | W | H) < 0) {
			return false;
		}
		int x = this.x;
		int y = this.y;
		if (X < x || Y < y) {
			return false;
		}
		w += x;
		if ((W += X) <= X ? w >= x || W > w : w >= x && W > w) {
			return false;
		}
		h += y;
		return !((H += Y) <= Y ? h >= y || H > h : h >= y && H > h);
	}
	
	@Deprecated
	public boolean inside(int X, int Y) {
		int w = this.width;
		int h = this.height;
		if ((w | h) < 0) {
			return false;
		}
		int x = this.x;
		int y = this.y;
		if (X < x || Y < y) {
			return false;
		}
		h += y;
		return !((w += x) >= x && w <= X || h >= y && h <= Y);
	}
	
	public boolean intersects(Rectangle r) {
		int tw = this.width;
		int th = this.height;
		int rw = r.width;
		int rh = r.height;
		if (rw <= 0 || rh <= 0 || tw <= 0 || th <= 0) {
			return false;
		}
		int tx = this.x;
		int ty = this.y;
		int rx = r.x;
		int ry = r.y;
		rh += ry;
		tw += tx;
		th += ty;
		return !((rw += rx) >= rx && rw <= tx || rh >= ry && rh <= ty || tw >= tx && tw <= rx ||
		         th >= ty && th <= ry);
	}
	
	public Rectangle intersection(Rectangle r) {
		int tx1 = this.x;
		int ty1 = this.y;
		int rx1 = r.x;
		int ry1 = r.y;
		long tx2 = tx1;
		tx2 += this.width;
		long ty2 = ty1;
		ty2 += this.height;
		long rx2 = rx1;
		rx2 += r.width;
		long ry2 = ry1;
		ry2 += r.height;
		if (tx1 < rx1)
			tx1 = rx1;
		if (ty1 < ry1)
			ty1 = ry1;
		if (tx2 > rx2)
			tx2 = rx2;
		if (ty2 > ry2)
			ty2 = ry2;
		ty2 -= ty1;
		if ((tx2 -= tx1) < Integer.MIN_VALUE)
			tx2 = Integer.MIN_VALUE;
		if (ty2 < Integer.MIN_VALUE)
			ty2 = Integer.MIN_VALUE;
		return new Rectangle(tx1, ty1, (int) tx2, (int) ty2);
	}
	
	public Rectangle union(Rectangle r) {
		long tx2 = this.width;
		long ty2 = this.height;
		if ((tx2 | ty2) < 0L) {
			return new Rectangle(r);
		}
		long rx2 = r.width;
		long ry2 = r.height;
		if ((rx2 | ry2) < 0L) {
			return new Rectangle(this);
		}
		int tx1 = this.x;
		int ty1 = this.y;
		tx2 += tx1;
		ty2 += ty1;
		int rx1 = r.x;
		int ry1 = r.y;
		rx2 += rx1;
		ry2 += ry1;
		if (tx1 > rx1) {
			tx1 = rx1;
		}
		if (ty1 > ry1) {
			ty1 = ry1;
		}
		if (tx2 < rx2) {
			tx2 = rx2;
		}
		if (ty2 < ry2) {
			ty2 = ry2;
		}
		ty2 -= ty1;
		if ((tx2 -= tx1) > Integer.MAX_VALUE) {
			tx2 = Integer.MAX_VALUE;
		}
		if (ty2 > Integer.MAX_VALUE) {
			ty2 = Integer.MAX_VALUE;
		}
		return new Rectangle(tx1, ty1, (int) tx2, (int) ty2);
	}
	
	public void add(int newx, int newy) {
		if ((this.width | this.height) < 0) {
			this.x = newx;
			this.y = newy;
			this.height = 0;
			this.width = 0;
			return;
		}
		int x1 = this.x;
		int y1 = this.y;
		long x2 = this.width;
		long y2 = this.height;
		x2 += x1;
		y2 += y1;
		if (x1 > newx) {
			x1 = newx;
		}
		if (y1 > newy) {
			y1 = newy;
		}
		if (x2 < (long) newx) {
			x2 = newx;
		}
		if (y2 < (long) newy) {
			y2 = newy;
		}
		y2 -= y1;
		if ((x2 -= x1) > Integer.MAX_VALUE) {
			x2 = Integer.MAX_VALUE;
		}
		if (y2 > Integer.MAX_VALUE) {
			y2 = Integer.MAX_VALUE;
		}
		this.reshape(x1, y1, (int) x2, (int) y2);
	}
	
	public void add(Point pt) {
		this.add(pt.x, pt.y);
	}
	
	public void add(Rectangle r) {
		long ry2;
		long rx2;
		long tx2 = this.width;
		long ty2 = this.height;
		if ((tx2 | ty2) < 0L) {
			this.reshape(r.x, r.y, r.width, r.height);
		}
		if (((rx2 = r.width) | (ry2 = r.height)) < 0L) {
			return;
		}
		int tx1 = this.x;
		int ty1 = this.y;
		tx2 += tx1;
		ty2 += ty1;
		int rx1 = r.x;
		int ry1 = r.y;
		rx2 += rx1;
		ry2 += ry1;
		if (tx1 > rx1) {
			tx1 = rx1;
		}
		if (ty1 > ry1) {
			ty1 = ry1;
		}
		if (tx2 < rx2) {
			tx2 = rx2;
		}
		if (ty2 < ry2) {
			ty2 = ry2;
		}
		ty2 -= ty1;
		if ((tx2 -= tx1) > Integer.MAX_VALUE) {
			tx2 = Integer.MAX_VALUE;
		}
		if (ty2 > Integer.MAX_VALUE) {
			ty2 = Integer.MAX_VALUE;
		}
		this.reshape(tx1, ty1, (int) tx2, (int) ty2);
	}
	
	public Rectangle grow(int left, int up, int right, int down) {
		return new Rectangle(x - left, y - up, width + left + right, height + up + down);
	}
	
	public void grow(int h, int v) {
		long x0 = this.x;
		long y0 = this.y;
		long x1 = this.width;
		long y1 = this.height;
		x1 += x0;
		y1 += y0;
		y0 -= v;
		y1 += v;
		if ((x1 += h) < (x0 -= h)) {
			if ((x1 -= x0) < Integer.MIN_VALUE) {
				x1 = Integer.MIN_VALUE;
			}
			if (x0 < Integer.MIN_VALUE) {
				x0 = Integer.MIN_VALUE;
			} else if (x0 > Integer.MAX_VALUE) {
				x0 = Integer.MAX_VALUE;
			}
		} else {
			if (x0 < Integer.MIN_VALUE) {
				x0 = Integer.MIN_VALUE;
			} else if (x0 > Integer.MAX_VALUE) {
				x0 = Integer.MAX_VALUE;
			}
			if ((x1 -= x0) < Integer.MIN_VALUE) {
				x1 = Integer.MIN_VALUE;
			} else if (x1 > Integer.MAX_VALUE) {
				x1 = Integer.MAX_VALUE;
			}
		}
		if (y1 < y0) {
			if ((y1 -= y0) < Integer.MIN_VALUE) {
				y1 = Integer.MIN_VALUE;
			}
			if (y0 < Integer.MIN_VALUE) {
				y0 = Integer.MIN_VALUE;
			} else if (y0 > Integer.MAX_VALUE) {
				y0 = Integer.MAX_VALUE;
			}
		} else {
			if (y0 < Integer.MIN_VALUE) {
				y0 = Integer.MIN_VALUE;
			} else if (y0 > Integer.MAX_VALUE) {
				y0 = Integer.MAX_VALUE;
			}
			if ((y1 -= y0) < Integer.MIN_VALUE) {
				y1 = Integer.MIN_VALUE;
			} else if (y1 > Integer.MAX_VALUE) {
				y1 = Integer.MAX_VALUE;
			}
		}
		this.reshape((int) x0, (int) y0, (int) x1, (int) y1);
	}
	
	public boolean isEmpty() {
		return this.width <= 0 || this.height <= 0;
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof Rectangle) {
			Rectangle r = (Rectangle) obj;
			return this.x == r.x && this.y == r.y && this.width == r.width && this.height == r.height;
		}
		return super.equals(obj);
	}
	
	public String toString() {
		return this.getClass().getName() +
		       "[x=" + this.x + ",y=" + this.y + ",width=" + this.width +
		       ",height=" + this.height + "]";
	}
	
	public int hashCode() {
		int result = 1;
		result = 31 * result + this.x;
		result = 31 * result + this.y;
		result = 31 * result + this.width;
		result = 31 * result + this.height;
		return result;
	}
}

