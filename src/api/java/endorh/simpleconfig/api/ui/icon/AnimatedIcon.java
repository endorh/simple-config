package endorh.simpleconfig.api.ui.icon;

import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Animated icon using a stripe of frames in a texture file.<br>
 * The stripe is read from left to right, top to bottom using the given
 * size and number of rowsand columns.<br>
 * The delay parameter controls the delay in ms between frames.<br>
 * The animation can be reset.<br>
 */
public class AnimatedIcon extends Icon {
	public final int rows;
	public final int cols;
	public final int frames;
	public long delay;
	protected int lastFrame = 0;
	protected long lastFrameTime = 0L;
	
	public static AnimatedIcon ofStripe(
	  ResourceLocation texture, int w, int h, int frames, long delay
	) {
		return new AnimatedIcon(
		  texture, 0, 0, w, h, w * frames, h, 1, frames, delay);
	}
	
	public AnimatedIcon(
	  ResourceLocation location, int u, int v, int w, int h, int tw, int th,
	  int rows, int cols, long delay
	) {
		this(location, u, v, w, h, Integer.MAX_VALUE, Integer.MAX_VALUE, tw, th, false, 0, rows, cols, delay);
	}
	
	public AnimatedIcon(
	  ResourceLocation location, int u, int v, int w, int h, int lX, int lY, int tw, int th,
	  boolean twoLevel, int tint, int rows, int cols, long delay
	) {
		super(location, u, v, w, h, lX, lY, tw, th, twoLevel, tint);
		this.rows = rows;
		this.cols = cols;
		this.delay = delay;
		frames = rows * cols;
	}
	
	@Override public @NotNull AnimatedIcon withTint(int tint) {
		return new AnimatedIcon(
		  getTexture(), u, v, w, h, levelOffsetX, levelOffsetY, tw, th,
		  twoLevel, tint, rows, cols, delay);
	}
	
	public AnimatedIcon copy() {
		return withTint(tint);
	}
	
	@Override public int translateLevel(int level) {
		return 0;
	}
	
	@Override public int getU() {
		return u + (lastFrame % cols) * w;
	}
	
	@Override public int getV() {
		return v + (lastFrame / rows) * h;
	}
	
	@Override protected void beforeRender(int level) {
		super.beforeRender(level);
		update();
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
			long skip = time - lastFrameTime;
			lastFrame += skip / delay;
			lastFrameTime = time - (skip % delay);
		}
	}
	
	public static class AnimatedIconBuilder {
		private ResourceLocation location;
		private int tw = 256;
		private int th = 256;
		private int w = 24;
		private int h = 24;
		private int lX = Integer.MAX_VALUE;
		private int lY = Integer.MAX_VALUE;
		private boolean twoLevel = false;
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
		
		public AnimatedIconBuilder withLevelOffset(Integer lX, Integer lY) {
			this.lX = lX != null? lX : Integer.MAX_VALUE;
			this.lY = lY != null? lY : Integer.MAX_VALUE;
			return this;
		}
		
		public AnimatedIconBuilder twoLevel(boolean twoLevel) {
			this.twoLevel = twoLevel;
			return this;
		}
		
		public AnimatedIconBuilder withFPS(float fps) {
			this.delay = (long) (1000F / fps);
			return this;
		}
		
		public AnimatedIcon create(int u, int v) {
			return new AnimatedIcon(
			  location, u, v, w, h, lX, lY, tw, th,
			  twoLevel, 0, rows, cols, delay);
		}
		
		public AnimatedIcon create(int u, int v, int w, int h) {
			return new AnimatedIcon(
			  location, u, v, w, h, lX, lY, tw, th,
			  twoLevel, 0, rows, cols, delay);
		}
	}
}
