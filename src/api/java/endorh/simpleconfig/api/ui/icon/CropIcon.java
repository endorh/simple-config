package endorh.simpleconfig.api.ui.icon;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import static java.lang.Math.*;

/**
 * Icons that only render a subsection of their mapped texture area.<br>
 * Convenient for composed icons.<br><br>
 * While negative crop offsets, and crop sizes beyond the icon sizes are allowed,
 * they won't behave well for fill rendering, since it assumes the cropped area
 * is confined to the icon's area.<br><br>
 * Subclasses may override the {@link #getCropU()} method and its companions to
 * animate the crop area.
 */
public class CropIcon extends Icon {
	protected final int cU;
	protected final int cV;
	protected final int cW;
	protected final int cH;
	
	public CropIcon(
	  ResourceLocation texture, int u, int v, int w, int h, int lX, int lY, int tw, int th,
	  boolean twoLevel, int tint, int cU, int cV, int cW, int cH
	) {
		super(texture, u, v, w, h, lX, lY, tw, th, twoLevel, tint);
		this.cU = cU;
		this.cV = cV;
		this.cW = cW;
		this.cH = cH;
	}
	
	@Override public @NotNull Icon withTint(int tint) {
		return new CropIcon(
		  getTexture(), u, v, w, h, levelOffsetX, levelOffsetY,
		  tw, th, twoLevel, tint, cU, cV, cW, cH);
	}
	
	@Override public void renderCentered(@NotNull MatrixStack mStack, int x, int y, int w, int h, int level) {
		beforeRender(level);
		int xx = x + w / 2 - this.w / 2;
		int yy = y + h / 2 - this.h / 2;
		int l = translateLevel(level);
		int u = getU(), v = getV();
		u = max(u + x - xx, u) + l * levelOffsetX;
		v = max(v + y - yy, v) + l * levelOffsetY;
		int cU = getCropU();
		int cV = getCropV();
		int ww = min(w - cU, getCropW());
		int hh = min(h - cV, getCropH());
		blit(
		  mStack, max(x, xx) + cU, max(y, yy) + cV, ww, hh,
		  u + cU, v + cV, ww, hh, tw, th);
		afterRender(level);
	}
	
	@Override public void renderStretch(@NotNull MatrixStack mStack, int x, int y, int w, int h, int level) {
		beforeRender(level);
		int l = translateLevel(level);
		int u = getU() + l * levelOffsetX;
		int v = getV() + l * levelOffsetY;
		int cU = getCropU();
		int cV = getCropV();
		int cW = getCropW();
		int cH = getCropH();
		blit(
		  mStack, x + round(cU * w / (float) this.w), y + round(cV * h / (float) this.h),
		  round(cW * w / (float) this.w), round(cH * h / (float) this.h),
		  u + cU, v + cV, cW, cH, tw, th);
		afterRender(level);
	}
	
	@Override public void renderFill(@NotNull MatrixStack mStack, int x, int y, int w, int h, int level) {
		// Overridden to compute crop only once
		beforeRender(level);
		int l = translateLevel(level);
		int cU = getCropU();
		int cV = getCropV();
		int cW = getCropW();
		int cH = getCropH();
		int u = getU() + l * levelOffsetX + cU;
		int v = getV() + l * levelOffsetY + cV;
		int xx, yy, xw, yh;
		for (xx = x + cU; xx < x + cU + w - this.w; xx += this.w) {
			for (yy = y + cV; yy < y + cV + h - this.h; yy += this.h)
				blit(mStack, xx, yy, cW, cH, u, v, cW, cH, tw, th);
			yh = min(cH, y + h - yy - cV);
			blit(mStack, xx, yy, cW, yh, u, v, cW, yh, tw, th);
		}
		xw = min(cW, x + w - xx - cU);
		for (yy = y + cV; yy < y + cV + h - this.h; yy += this.h)
			blit(mStack, xx, yy, xw, cH, u, v, xw, cH, tw, th);
		yh = min(cH, y + h - yy - cV);
		blit(mStack, xx, yy, xw, yh, u, v, xw, yh, tw, th);
		afterRender(level);
	}
	
	public int getCropU() {
		return cU;
	}
	public int getCropV() {
		return cV;
	}
	public int getCropW() {
		return cW;
	}
	public int getCropH() {
		return cH;
	}
}
