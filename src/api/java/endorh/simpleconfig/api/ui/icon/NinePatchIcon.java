package endorh.simpleconfig.api.ui.icon;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * 9 patch icon, for resizable textures with borders.<br>
 * All three rendering methods resolve to {@link #renderStretch},
 * which properly stretches the 9 patch to fill the requested area.
 */
public class NinePatchIcon extends Icon {
	private final int iu;
	private final int iv;
	private final int iw;
	private final int ih;
	
	public NinePatchIcon(
	  ResourceLocation texture, int u, int v, int w, int h,
	  int iu, int iv, int iw, int ih,
	  int lX, int lY, int tw, int th, boolean twoLevel, int tint
	) {
		super(texture, u, v, w, h, lX, lY, tw, th, twoLevel, tint);
		this.iu = iu;
		this.iv = iv;
		this.iw = iw;
		this.ih = ih;
	}
	
	@Override public void renderCentered(PoseStack mStack, int x, int y, int w, int h, int level) {
		renderStretch(mStack, x, y, w, h, level);
	}
	
	@Override public void renderStretch(PoseStack mStack, int x, int y, int w, int h, int level) {
		beforeRender(level);
		final int l = translateLevel(level);
		int u = getU() + l * levelOffsetX;
		int v = getV() + l * levelOffsetY;
		int rw = this.w - iw - iu;
		int bh = this.h - ih - iv;
		int lw = iu;
		int uh = iv;
		if (w < lw + rw) {
			lw = min(lw, max(w / 2, w - rw));
			rw = w - lw;
		}
		if (h < uh + bh) {
			uh = min(uh, max(h / 2, h - bh));
			bh = h - uh;
		}
		int iU = w - lw - rw;
		int iV = h - uh - bh;
		
		if (uh > 0) {
			if (lw > 0) blit(mStack, x, y, lw, uh, u, v, lw, uh, tw, th);
			if (iU > 0) blitFill(mStack, x + lw, y, iU, uh, u + iu, v, iw, uh, tw, th);
			if (rw > 0) blit(mStack, x + w - rw, y, rw, uh, u + iu + iw, v, rw, uh, tw, th);
		}
		if (iV > 0) {
			if (lw > 0) blitFill(mStack, x, y + uh, lw, iV, u, v + iv, lw, ih, tw, th);
			if (iU > 0) blitFill(mStack, x + lw, y + uh, iU, iV, u + iu, v + iv, iw, ih, tw, th);
			if (rw > 0) blitFill(mStack, x + w - rw, y + uh, rw, iV, u + iu + iw, v + iv, rw, ih, tw, th);
		}
		if (bh > 0) {
			if (lw > 0) blit(mStack, x, y + h - bh, lw, bh, u, v + iv + ih, lw, bh, tw, th);
			if (iU > 0) blitFill(mStack, x + lw, y + h - bh, iU, bh, u + iu, v + iv + ih, iw, bh, tw, th);
			if (rw > 0) blit(mStack, x + w - rw, y + h - bh, rw, bh, u + iu + iw, v + iv + ih, rw, bh, tw, th);
		}
		afterRender(level);
	}
	
	@Override public void renderFill(PoseStack mStack, int x, int y, int w, int h, int level) {
		renderStretch(mStack, x, y, w, h, level);
	}
	
	protected void blitFill(
	  PoseStack mStack, int x, int y, int w, int h, int u, int v, int uw, int vh, int tw, int th
	) {
		int xx, yy;
		for (xx = x; xx < x + w - uw; xx += uw) {
			for (yy = y; yy < y + h - vh; yy += vh)
				blit(mStack, xx, yy, uw, vh, u, v, uw, vh, tw, th);
			int yh = y + h - yy;
			blit(mStack, xx, yy, uw, yh, u, v, uw, yh, tw, th);
		}
		int xw = x + w - xx;
		for (yy = y; yy < y + h - vh; yy += vh)
			blit(mStack, xx, yy, xw, vh, u, v, xw, vh, tw, th);
		int yh = y + h - yy;
		blit(mStack, xx, yy, xw, yh, u, v, xw, yh, tw, th);
	}
	
	@Override public Icon withTint(int tint) {
		return new NinePatchIcon(
		  getTexture(), u, v, w, h, iu, iv, iw, ih,
		  levelOffsetX, levelOffsetY, tw, th, isTwoLevel(), tint);
	}
}
