package endorh.simple_config.clothconfig2.math;

public final class Color {
	private final int color;
	
	private Color(int color) {
		this.color = color;
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
	
	public static Color ofHSB(float hue, float saturation, float brightness) {
		return Color.ofOpaque(Color.HSBtoRGB(hue, saturation, brightness));
	}
	
	public static int HSBtoRGB(float hue, float saturation, float brightness) {
		int r = 0;
		int g = 0;
		int b = 0;
		if (saturation == 0.0f) {
			g = b = (int) (brightness * 255.0f + 0.5f);
			r = b;
		} else {
			float h = (hue - (float) Math.floor(hue)) * 6.0f;
			float f = h - (float) Math.floor(h);
			float p = brightness * (1.0f - saturation);
			float q = brightness * (1.0f - saturation * f);
			float t = brightness * (1.0f - saturation * (1.0f - f));
			switch ((int) h) {
				case 0: {
					r = (int) (brightness * 255.0f + 0.5f);
					g = (int) (t * 255.0f + 0.5f);
					b = (int) (p * 255.0f + 0.5f);
					break;
				}
				case 1: {
					r = (int) (q * 255.0f + 0.5f);
					g = (int) (brightness * 255.0f + 0.5f);
					b = (int) (p * 255.0f + 0.5f);
					break;
				}
				case 2: {
					r = (int) (p * 255.0f + 0.5f);
					g = (int) (brightness * 255.0f + 0.5f);
					b = (int) (t * 255.0f + 0.5f);
					break;
				}
				case 3: {
					r = (int) (p * 255.0f + 0.5f);
					g = (int) (q * 255.0f + 0.5f);
					b = (int) (brightness * 255.0f + 0.5f);
					break;
				}
				case 4: {
					r = (int) (t * 255.0f + 0.5f);
					g = (int) (p * 255.0f + 0.5f);
					b = (int) (brightness * 255.0f + 0.5f);
					break;
				}
				case 5: {
					r = (int) (brightness * 255.0f + 0.5f);
					g = (int) (p * 255.0f + 0.5f);
					b = (int) (q * 255.0f + 0.5f);
				}
			}
		}
		return 0xFF000000 | r << 16 | g << 8 | b;
	}
	
	public int getColor() {
		return this.color;
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
	
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || this.getClass() != other.getClass()) {
			return false;
		}
		return this.color == ((Color) other).color;
	}
	
	public int hashCode() {
		return this.color;
	}
	
	public String toString() {
		return "Color{r=" + this.getRed() + "g=" + this.getGreen() + "b=" + this.getBlue() + '}';
	}
}

