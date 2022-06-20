package endorh.simpleconfig.clothconfig2.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import endorh.simpleconfig.SimpleConfigMod;
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
	  new ResourceLocation(SimpleConfigMod.MOD_ID, "textures/gui/empty"),
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
	public final int tw;
	public final int th;
	public final boolean twoLevel;
	
	public Icon(ResourceLocation location, int u, int v, int w, int h, int tw, int th) {
		this(location, u, v, w, h, tw, th, false);
	}
	
	public Icon(ResourceLocation location, int u, int v, int w, int h, int tw, int th, boolean twoLevel) {
		this.location = location;
		this.u = u;
		this.v = v;
		this.w = w;
		this.h = h;
		this.tw = tw;
		this.th = th;
		this.twoLevel = twoLevel;
	}
	
	public void renderCentered(
	  MatrixStack mStack, int x, int y, int w, int h
	) {renderCentered(mStack, x, y, w, h, 0);}
	
	public int translateLevel(int level) {
		if (twoLevel)
			return max(0, level - 1);
		return level;
	}
	
	public void renderCentered(
	  MatrixStack mStack, int x, int y, int w, int h, int level
	) {
		bindTexture();
		int xx = x + w / 2 - this.w / 2;
		int yy = y + h / 2 - this.h / 2;
		final int ww = min(w, this.w);
		final int hh = min(h, this.h);
		blit(
		  mStack, max(x, xx), max(y, yy), ww, hh,
		  max(u + x - xx, u), max(v + y - yy, v) + translateLevel(level) * this.h,
		  ww, hh, tw, th);
	}
	
	public void renderStretch(
	  MatrixStack mStack, int x, int y, int w, int h
	) {renderStretch(mStack, x, y, w, h, 0);}
	
	public void renderStretch(
	  MatrixStack mStack, int x, int y, int w, int h, int level
	) {
		bindTexture();
		blit(mStack, x, y, w, h, u, v + translateLevel(level) * this.h,
		     this.w, this.h, tw, th);
	}
	
	public void bindTexture() {
		Minecraft.getInstance().getTextureManager().bind(location);
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
		private int tw = 256;
		private int th = 256;
		private int w = 24;
		private int h = 24;
		private boolean twoLevel = false;
		
		public static IconBuilder ofTexture(
		  ResourceLocation location, int textureWidth, int textureHeight
		) {
			return new IconBuilder(location).withTexture(location, textureWidth, textureHeight);
		}
		
		private IconBuilder(ResourceLocation location) {
			this.location = location;
		}
		
		public IconBuilder withTexture(ResourceLocation location, int textureWidth, int textureHeight) {
			this.location = location;
			this.tw = textureWidth;
			this.th = textureHeight;
			return this;
		}
		
		public IconBuilder withSize(int w, int h) {
			this.w = w;
			this.h = h;
			return this;
		}
		
		public IconBuilder twoLevel(boolean twoLevel) {
			this.twoLevel = twoLevel;
			return this;
		}
		
		public Icon create(int u, int v) {
			return new Icon(location, u, v, w, h, tw, th, twoLevel);
		}
		
		public Icon create(int u, int v, int w, int h) {
			return new Icon(location, u, v, w, h, tw, th, twoLevel);
		}
	}
}
