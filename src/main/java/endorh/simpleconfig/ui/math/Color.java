package endorh.simpleconfig.ui.math;

public final class Color {
	private final int color;
	private final float h;
	private final float s;
	private final float b;
	
	private Color(int color) {
		this.color = color;
		float[] hsb = java.awt.Color.RGBtoHSB(getRed(), getGreen(), getBlue(), new float[3]);
		h = hsb[0];
		s = hsb[1];
		b = hsb[2];
	}
	
	public static Color ofTransparent(int color) {
		return new Color(color);
	}
	
	public static Color ofOpaque(int color) {
		return new Color(0xFF000000 | color);
	}
	
	public static Color ofRGB(float r, float g, float b) {
		return Color.ofRGBA(r, g, b, 1.0f);
	}
	
	public static Color ofRGB(int r, int g, int b) {
		return Color.ofRGBA(r, g, b, 255);
	}
	
	public static Color ofRGBA(float r, float g, float b, float a) {
		return Color.ofRGBA(
		  (int) ((double) (r * 255.0f) + 0.5), (int) ((double) (g * 255.0f) + 0.5),
		  (int) ((double) (b * 255.0f) + 0.5), (int) ((double) (a * 255.0f) + 0.5));
	}
	
	public static Color ofRGBA(int r, int g, int b, int a) {
		return new Color((a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | b & 0xFF);
	}
	
	public static Color ofHSB(float h, float s, float b) {
		return Color.ofOpaque(java.awt.Color.HSBtoRGB(h, s, b));
	}
	
	public static Color ofHSBA(float h, float s, float b, int a) {
		return Color.ofTransparent(java.awt.Color.HSBtoRGB(h, s, b) & 0xFFFFFF | a << 24);
	}
	
	public int getColor() {
		return this.color;
	}
	
	public int getOpaque() {
		return this.color | 0xFF000000;
	}
	
	public int getRGB() {
		return this.color & 0xFFFFFF;
	}
	
	public int getAlpha() {
		return this.color >> 24 & 0xFF;
	}
	
	public int getRed() {
		return this.color >> 16 & 0xFF;
	}
	
	public int getGreen() {
		return this.color >> 8 & 0xFF;
	}
	
	public int getBlue() {
		return this.color & 0xFF;
	}
	
	public Color brighter(double factor) {
		int r = this.getRed();
		int g = this.getGreen();
		int b = this.getBlue();
		int i = (int) (1.0 / (1.0 - 1.0 / factor));
		if (r == 0 && g == 0 && b == 0) {
			return Color.ofRGBA(i, i, i, this.getAlpha());
		}
		if (r > 0 && r < i) {
			r = i;
		}
		if (g > 0 && g < i) {
			g = i;
		}
		if (b > 0 && b < i) {
			b = i;
		}
		return Color.ofRGBA(
		  Math.min((int) ((double) r / (1.0 / factor)), 255),
		  Math.min((int) ((double) g / (1.0 / factor)), 255),
		  Math.min((int) ((double) b / (1.0 / factor)), 255), this.getAlpha());
	}
	
	public Color darker(double factor) {
		return Color.ofRGBA(
		  Math.max((int) ((double) this.getRed() * (1.0 / factor)), 0),
		  Math.max((int) ((double) this.getGreen() * (1.0 / factor)), 0),
		  Math.max((int) ((double) this.getBlue() * (1.0 / factor)), 0), this.getAlpha());
	}
	
	@Override public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || this.getClass() != other.getClass()) {
			return false;
		}
		return this.color == ((Color) other).color;
	}
	
	@Override public int hashCode() {
		return this.color;
	}
	
	@Override public String toString() {
		return "Color{r=" + this.getRed() + "g=" + this.getGreen() + "b=" + this.getBlue() + '}';
	}
	
	public float getHue() {
		return h;
	}
	
	public float getSaturation() {
		return s;
	}
	
	public float getBrightness() {
		return b;
	}
}

