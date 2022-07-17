package endorh.simpleconfig.ui.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.ui.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.util.ResourceLocation;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class Icon {
	/**
	 * The empty icon. Draws nothing.
	 */
	public static final Icon EMPTY = new Icon(
	  new ResourceLocation(SimpleConfigMod.MOD_ID, "textures/gui/simple_config/empty"),
	  0, 0, 0, 0, 0, 0
	) { // @formatter:off
		@Override public void renderCentered(MatrixStack m, int x, int y, int w, int h, int level) {}
		@Override public void renderStretch(MatrixStack m, int x, int y, int w, int h, int level) {}
		@Override public void bindTexture() {}
	}; // @formatter:on
	
	public final ResourceLocation location;
	public final int u;
	public final int v;
	public final int w;
	public final int h;
	public final int levelOffsetX;
	public final int levelOffsetY;
	public final int tw;
	public final int th;
	public final int tint;
	public final boolean twoLevel;
	
	public Icon(ResourceLocation location, int u, int v, int w, int h, int tw, int th) {
		this(location, u, v, w, h, tw, th, false);
	}
	
	public Icon(ResourceLocation location, int u, int v, int w, int h, int tw, int th, boolean twoLevel) {
		this(location, u, v, w, h, Integer.MAX_VALUE, Integer.MAX_VALUE, tw, th, twoLevel, 0);
	}
	
	public Icon(
	  ResourceLocation location, int u, int v, int w, int h, int lX, int lY, int tw, int th,
	  boolean twoLevel, int tint
	) {
		this.location = location;
		this.u = u;
		this.v = v;
		this.w = w;
		this.h = h;
		if (lX != Integer.MAX_VALUE || lY != Integer.MAX_VALUE) {
			levelOffsetX = lX;
			levelOffsetY = lY;
		} else {
			levelOffsetX = 0;
			levelOffsetY = h;
		}
		this.tw = tw;
		this.th = th;
		this.twoLevel = twoLevel;
		this.tint = tint;
	}
	
	public Icon withTint(int tint) {
		return new Icon(location, u, v, w, h, levelOffsetX, levelOffsetY, tw, th, twoLevel, tint);
	}
	
	public static void setShaderColorMask(int color) {
		RenderSystem.color4f(
		  (color >> 16 & 0xFF) / 255F,
		  (color >> 8 & 0xFF) / 255F,
		  (color & 0xFF) / 255F,
		  (color >> 24 & 0xFF) / 255F);
	}
	
	public static void removeShaderColorMask() {
		RenderSystem.color4f(1F, 1F, 1F, 1F);
	}
	
	public int translateLevel(int level) {
		if (twoLevel)
			return max(0, level - 1);
		return level;
	}
	
	public void renderCentered(MatrixStack mStack, Rectangle rect) {
		renderCentered(mStack, rect, 0);
	}
	
	public void renderCentered(
	  MatrixStack mStack, int x, int y, int w, int h
	) {
		renderCentered(mStack, x, y, w, h, 0);
	}
	
	public void renderCentered(MatrixStack mStack, Rectangle rect, int level) {
		renderCentered(mStack, rect.x, rect.y, rect.width, rect.height, level);
	}
	
	public void renderCentered(
	  MatrixStack mStack, int x, int y, int w, int h, int level
	) {
		beforeRender();
		int xx = x + w / 2 - this.w / 2;
		int yy = y + h / 2 - this.h / 2;
		final int ww = min(w, this.w);
		final int hh = min(h, this.h);
		final int l = translateLevel(level);
		int u = getU(), v = getV();
		u = max(u + x - xx, u) + l * levelOffsetX;
		v = max(v + y - yy, v) + l * levelOffsetY;
		blit(
		  mStack, max(x, xx), max(y, yy), ww, hh,
		  u, v, ww, hh, tw, th);
		afterRender();
	}
	
	public void renderStretch(MatrixStack mStack, Rectangle rect) {
		renderStretch(mStack, rect, 0);
	}
	
	public void renderStretch(
	  MatrixStack mStack, int x, int y, int w, int h
	) {
		renderStretch(mStack, x, y, w, h, 0);
	}
	
	public void renderStretch(MatrixStack mStack, Rectangle rect, int level) {
		renderStretch(mStack, rect.x, rect.y, rect.width, rect.height, level);
	}
	
	public void renderStretch(
	  MatrixStack mStack, int x, int y, int w, int h, int level
	) {
		beforeRender();
		final int l = translateLevel(level);
		blit(mStack, x, y, w, h, getU() + l * levelOffsetX, getV() + l * levelOffsetY,
		     this.w, this.h, tw, th);
		afterRender();
	}
	
	public void renderFill(MatrixStack mStack, Rectangle rect) {
		renderFill(mStack, rect, 0);
	}
	
	public void renderFill(
	  MatrixStack mStack, int x, int y, int w, int h
	) {
		renderFill(mStack, x, y, w, h, 0);
	}
	
	public void renderFill(MatrixStack mStack, Rectangle rect, int level) {
		renderFill(mStack, rect.x, rect.y, rect.width, rect.height, level);
	}
	
	public void renderFill(
	  MatrixStack mStack, int x, int y, int w, int h, int level
	) {
		beforeRender();
		final int l = translateLevel(level);
		int u = getU() + l * levelOffsetX;
		int v = getV() + l * levelOffsetY;
		int xx, yy, xw, yh;
		for (xx = x; xx < x + w - this.w; xx += this.w) {
			for (yy = y; yy < y + h - this.h; yy += this.h)
				blit(mStack, xx, yy, this.w, this.h, u, v, this.w, this.h, tw, th);
			yh = y + h - yy;
			blit(mStack, xx, yy, this.w, yh, u, v, this.w, yh, tw, th);
		}
		xw = x + w - xx;
		for (yy = y; yy < y + h - this.h; yy += this.h)
			blit(mStack, xx, yy, xw, this.h, u, v, xw, this.h, tw, th);
		yh = y + h - yy;
		blit(mStack, xx, yy, xw, yh, u, v, xw, yh, tw, th);
		afterRender();
	}
	
	protected void beforeRender() {
		bindTexture();
		if (tint != 0) setShaderColorMask(tint);
	}
	
	protected void afterRender() {
		removeShaderColorMask();
	}
	
	public void bindTexture() {
		Minecraft.getInstance().getTextureManager().bindTexture(location);
	}
	
	public int getU() {
		return u;
	}
	
	public int getV() {
		return v;
	}
	
	protected void blit(
	  MatrixStack mStack, int x, int y, int w, int h, float u, float v,
	  int uw, int vh, int tw, int th
	) {
		RenderSystem.enableBlend();
		AbstractGui.blit(mStack, x, y, w, h, u, v, uw, vh, tw, th);
		RenderSystem.disableBlend();
	}
	
	public static class IconBuilder {
		private ResourceLocation location;
		private int ou = 0;
		private int ov = 0;
		private int tw = 256;
		private int th = 256;
		private int lX = Integer.MAX_VALUE;
		private int lY = Integer.MAX_VALUE;
		private int w = 24;
		private int h = 24;
		private boolean twoLevel = false;
		
		public static IconBuilder ofTexture(
		  ResourceLocation location, int width, int height
		) {
			return new IconBuilder(location).texture(location, width, height);
		}
		
		private IconBuilder(ResourceLocation location) {
			this.location = location;
		}
		
		/**
		 * Change the texture of the icons.
		 */
		public IconBuilder texture(ResourceLocation location, int width, int height) {
			this.location = location;
			this.tw = width;
			this.th = height;
			return this;
		}
		
		/**
		 * Change the size of the icons.
		 */
		public IconBuilder size(int w, int h) {
			this.w = w;
			this.h = h;
			return this;
		}
		
		/**
		 * Change the offset used for icon levels.<br>
		 * If both offsets are set to null, the offset defaults to a vertical offset
		 * equal to the height.
		 */
		public IconBuilder level(Integer lX, Integer lY) {
			this.lX = lX != null? lX : Integer.MAX_VALUE;
			this.lY = lY != null? lY : Integer.MAX_VALUE;
			return this;
		}
		
		/**
		 * Mark icons as only having two levels, for button rendering.
		 */
		public IconBuilder twoLevel(boolean twoLevel) {
			this.twoLevel = twoLevel;
			return this;
		}
		
		/**
		 * Change the offset applied to all created icons
		 */
		public IconBuilder offset(int ox, int oy) {
			this.ou = ox;
			this.ov = oy;
			return this;
		}
		
		/**
		 * Create an icon at the given coordinates.<br>
		 * The coordinates are relative to the current {@link #offset}
		 */
		public Icon at(int u, int v) {
			return new Icon(location, ou + u, ov + v, w, h, lX, lY, tw, th, twoLevel, 0);
		}
	}
}
