package endorh.simple_config.clothconfig2.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.util.ResourceLocation;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class AnimatedIcon extends Icon {
	public final int rows;
	public final int cols;
	public final int frames;
	public long delay;
	protected int lastFrame = 0;
	protected long lastFrameTime = 0L;
	
	public AnimatedIcon(
	  ResourceLocation location, int u, int v, int w, int h, int tw, int th,
	  int rows, int cols, long delay
	) {
		super(location, u, v, w, h, tw, th);
		this.rows = rows;
		this.cols = cols;
		this.delay = delay;
		frames = rows * cols;
	}
	
	@Override public int translateLevel(int level) {
		return 0;
	}
	
	public int getU() {
		return u + (lastFrame % cols) * w;
	}
	
	public int getV() {
		return v + (lastFrame / rows) * h;
	}
	
	@Override public void renderCentered(MatrixStack mStack, int x, int y, int w, int h, int level) {
		bindTexture();
		int xx = x + w / 2 - this.w / 2;
		int yy = y + h / 2 - this.h / 2;
		final int ww = min(w, this.w);
		final int hh = min(h, this.h);
		final int u = getU();
		final int v = getV();
		blit(
		  mStack, max(x, xx), max(y, yy), ww, hh,
		  max(u + x - xx, u), max(v + y - yy, v) + translateLevel(level) * this.h,
		  ww, hh, tw, th);
	}
	
	@Override public void renderStretch(MatrixStack mStack, int x, int y, int w, int h, int level) {
		update();
		bindTexture();
		blit(mStack, x, y, w, h, getU(), getV() + translateLevel(level) * this.h,
		     this.w, this.h, tw, th);
	}
	
	public void reset() {
		lastFrame = 0;
		lastFrameTime = 0L;
	}
	
	public void setFrame(int frame) {
		lastFrame = frame;
		lastFrameTime = 0L;
	}
	
	protected void update() {
		final long time = System.currentTimeMillis();
		if (lastFrameTime == 0L) {
			lastFrameTime = time;
		} else if (time - lastFrameTime > delay) {
			lastFrame += (time - lastFrameTime) / delay;
			lastFrameTime = time;
		}
	}
	
	public static class AnimatedIconBuilder {
		private ResourceLocation location;
		private int tw = 256;
		private int th = 256;
		private int w = 24;
		private int h = 24;
		private long delay = 40;
		private int rows = 1;
		private int cols = 1;
		
		public static AnimatedIconBuilder ofTexture(
		  ResourceLocation location, int textureWidth, int textureHeight
		) {
			return new AnimatedIconBuilder(location)
			  .withTexture(location, textureWidth, textureHeight);
		}
		
		private AnimatedIconBuilder(ResourceLocation location) {
			this.location = location;
		}
		
		public AnimatedIconBuilder withTexture(ResourceLocation location, int textureWidth, int textureHeight) {
			this.location = location;
			this.tw = textureWidth;
			this.th = textureHeight;
			return this;
		}
		
		public AnimatedIconBuilder withSize(int w, int h) {
			this.w = w;
			this.h = h;
			return this;
		}
		
		public AnimatedIconBuilder withFrames(int rows, int cols) {
			this.rows = rows;
			this.cols = cols;
			return this;
		}
		
		public AnimatedIconBuilder withFPS(float fps) {
			this.delay = (long) (1000F / fps);
			return this;
		}
		
		public AnimatedIcon create(int u, int v) {
			return new AnimatedIcon(location, u, v, w, h, tw, th, rows, cols, delay);
		}
		
		public AnimatedIcon create(int u, int v, int w, int h) {
			return new AnimatedIcon(location, u, v, w, h, tw, th, rows, cols, delay);
		}
	}
}
