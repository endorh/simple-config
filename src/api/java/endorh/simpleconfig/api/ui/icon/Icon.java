package endorh.simpleconfig.api.ui.icon;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import endorh.simpleconfig.api.SimpleConfig;
import endorh.simpleconfig.api.ui.math.Rectangle;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Icon class abstracting the texture mapping logic from icon rendering.<br>
 * To map multiple icons from the same texture with ease, use the {@link IconBuilder}.<br>
 * Supports three rendering methods:
 * <ul>
 *    <li>{@link #renderCentered}</li>
 *    <li>{@link #renderStretch}</li>
 *    <li>{@link #renderFill}</li>
 * </ul>
 * Tinted copies can be created using {@link #withTint} in its many variants.<br><br>
 * Subclasses may be interested in overriding the following methods:
 * <ul>
 *    <li>{@link #withTint(int)}, to return tinted copies of your subclass.</li>
 *    <li>{@link #beforeRender(int)}, which is called from the 3 rendering methods.</li>
 *    <li>{@link #afterRender(int)}, which is called from the 3 rendering methods.</li>
 *    <li>{@link #translateLevel(int)}, to change the hover variants behavior.</li>
 *    <li>{@link #isTwoLevel()}, to change the hover variants behavior.</li>
 *    <li>{@link #bindTexture()}, if you need to swap textures.</li>
 *    <li>{@link #getU()}, for animation purposes.</li>
 *    <li>{@link #getV()}, for animation purposes.</li>
 *    <li>{@link #blit(PoseStack, int, int, int, int, float, float, int, int, int, int)}, to alter the main rendering logic</li>
 *    <li>{@link #renderCentered(PoseStack, int, int, int, int, int)}, if the above hooks aren't enough</li>
 *    <li>{@link #renderStretch(PoseStack, int, int, int, int, int)}, if the above hooks aren't enough</li>
 *    <li>{@link #renderFill(PoseStack, int, int, int, int, int)}, if the above hooks aren't enough</li>
 * </ul>
 */
public class Icon {
	/**
	 * The empty icon. Draws nothing.
	 */
	public static final Icon EMPTY = new Icon(
	  new ResourceLocation(SimpleConfig.MOD_ID, "textures/gui/simpleconfig/empty"),
	  0, 0, 0, 0, 0, 0
	) { // @formatter:off
		@Override public void renderCentered(@NotNull PoseStack m, int x, int y, int w, int h, int level) {}
		@Override public void renderStretch(@NotNull PoseStack m, int x, int y, int w, int h, int level) {}
		@Override public void bindTexture() {}
	}; // @formatter:on
	
	protected final ResourceLocation texture;
	protected final int u;
	protected final int v;
	public final int w;
	public final int h;
	public final int levelOffsetX;
	public final int levelOffsetY;
	public final int tw;
	public final int th;
	public final int tint;
	protected final boolean twoLevel;
	
	public Icon(ResourceLocation texture, int u, int v, int w, int h, int tw, int th) {
		this(texture, u, v, w, h, tw, th, false);
	}
	
	public Icon(ResourceLocation texture, int u, int v, int w, int h, int tw, int th, boolean twoLevel) {
		this(texture, u, v, w, h, Integer.MAX_VALUE, Integer.MAX_VALUE, tw, th, twoLevel, 0);
	}
	
	public Icon(
	  ResourceLocation texture, int u, int v, int w, int h, int lX, int lY, int tw, int th,
	  boolean twoLevel, int tint
	) {
		this.texture = texture;
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
	
	/**
	 * Obtain a tinted version of this icon.<br>
	 * <b>Ensure the tint has a non-zero alpha value, or the icon will be invisible.</b>
	 * @param tint Color to use, in ARGB format.
	 */
	@Contract(pure=true) public @NotNull Icon withTint(int tint) {
		return new Icon(getTexture(), u, v, w, h, levelOffsetX, levelOffsetY, tw, th, isTwoLevel(), tint);
	}
	
	/**
	 * Obtain a tinted version of this icon.<br>
	 * @param tint Color to use.
	 */
	@Contract(pure=true) public @NotNull Icon withTint(ChatFormatting tint) {
		Integer color = tint.getColor();
		if (color == null) throw new IllegalArgumentException("Not a valid color style: " + tint);
		return withTint(color | 0xFF000000);
	}
	
	/**
	 * Obtain a tinted version of this icon, using the color of a given style.<br>
	 * If the style has no color, defaults to white color.
	 */
	@Contract(pure=true) public @NotNull Icon withTint(Style style) {
		TextColor color = style.getColor();
		int c;
		if (color != null) {
			c = color.getValue();
		} else c = Objects.requireNonNull(ChatFormatting.WHITE.getColor());
		if ((c & 0xFF000000) == 0) c |= 0xFF000000;
		return withTint(c);
	}
	
	public static void setShaderColorMask(int color) {
		RenderSystem.setShaderColor(
		  (color >> 16 & 0xFF) / 255F,
		  (color >> 8 & 0xFF) / 255F,
		  (color & 0xFF) / 255F,
		  (color >> 24 & 0xFF) / 255F);
	}
	
	public static void removeShaderColorMask() {
		RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
	}
	
	public int translateLevel(int level) {
		if (isTwoLevel())
			return max(0, level - 1);
		return level;
	}
	
	public void renderCentered(@NotNull PoseStack mStack, @NotNull Rectangle rect) {
		renderCentered(mStack, rect, 0);
	}
	
	public void renderCentered(
	  @NotNull PoseStack mStack, int x, int y, int w, int h
	) {
		renderCentered(mStack, x, y, w, h, 0);
	}
	
	public void renderCentered(@NotNull PoseStack mStack, @NotNull Rectangle rect, int level) {
		renderCentered(mStack, rect.x, rect.y, rect.width, rect.height, level);
	}
	
	public void renderCentered(
	  @NotNull PoseStack mStack, int x, int y, int w, int h, int level
	) {
		beforeRender(level);
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
		afterRender(level);
	}
	
	public void renderStretch(@NotNull PoseStack mStack, @NotNull Rectangle rect) {
		renderStretch(mStack, rect, 0);
	}
	
	public void renderStretch(
	  @NotNull PoseStack mStack, int x, int y, int w, int h
	) {
		renderStretch(mStack, x, y, w, h, 0);
	}
	
	public void renderStretch(@NotNull PoseStack mStack, @NotNull Rectangle rect, int level) {
		renderStretch(mStack, rect.x, rect.y, rect.width, rect.height, level);
	}
	
	public void renderStretch(
	  @NotNull PoseStack mStack, int x, int y, int w, int h, int level
	) {
		beforeRender(level);
		final int l = translateLevel(level);
		blit(mStack, x, y, w, h, getU() + l * levelOffsetX, getV() + l * levelOffsetY,
		     this.w, this.h, tw, th);
		afterRender(level);
	}
	
	public void renderFill(@NotNull PoseStack mStack, @NotNull Rectangle rect) {
		renderFill(mStack, rect, 0);
	}
	
	public void renderFill(
	  @NotNull PoseStack mStack, int x, int y, int w, int h
	) {
		renderFill(mStack, x, y, w, h, 0);
	}
	
	public void renderFill(@NotNull PoseStack mStack, @NotNull Rectangle rect, int level) {
		renderFill(mStack, rect.x, rect.y, rect.width, rect.height, level);
	}
	
	public void renderFill(
	  @NotNull PoseStack mStack, int x, int y, int w, int h, int level
	) {
		beforeRender(level);
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
		afterRender(level);
	}
	
	protected void beforeRender(int level) {
		bindTexture();
		if (tint != 0) setShaderColorMask(tint);
	}
	
	protected void afterRender(int level) {
		removeShaderColorMask();
	}
	
	public void bindTexture() {
		RenderSystem.setShaderTexture(0, getTexture());
	}
	
	public ResourceLocation getTexture() {
		return texture;
	}
	
	public int getU() {
		return u;
	}
	
	public int getV() {
		return v;
	}
	
	public boolean isTwoLevel() {
		return twoLevel;
	}
	
	protected void blit(
	  PoseStack mStack, int x, int y, int w, int h, float u, float v,
	  int uw, int vh, int tw, int th
	) {
		RenderSystem.enableBlend();
		GuiComponent.blit(mStack, x, y, w, h, u, v, uw, vh, tw, th);
		RenderSystem.disableBlend();
	}
	
	public static class IconBuilder {
		private ResourceLocation texture;
		private int tw = 256;
		private int th = 256;
		
		private boolean revU = false;
		private boolean revV = false;
		private int ou = 0;
		private int ov = 0;
		
		private int lX = Integer.MAX_VALUE;
		private int lY = Integer.MAX_VALUE;
		
		private int u = 0;
		private int v = 0;
		private int w = 24;
		private int h = 24;
		
		private int cropU = 0;
		private int cropV = 0;
		private int cropW = 0;
		private int cropH = 0;
		
		private int patchL = 0;
		private int patchT = 0;
		private int patchR = 0;
		private int patchB = 0;
		
		private boolean twoLevel = false;
		
		public static @NotNull IconBuilder ofTexture(
		  @NotNull ResourceLocation location, int width, int height
		) {
			return new IconBuilder(location).texture(location, width, height);
		}
		
		private IconBuilder(ResourceLocation location) {
			texture = location;
		}
		
		/**
		 * Change the texture of the icons.
		 */
		public @NotNull IconBuilder texture(ResourceLocation location, int width, int height) {
			texture = location;
			tw = width;
			th = height;
			return this;
		}
		
		/**
		 * Change the size of the icons.
		 */
		public @NotNull IconBuilder size(int w, int h) {
			this.w = w;
			this.h = h;
			return this;
		}
		
		/**
		 * Change the offset used for icon levels.<br>
		 * If both offsets are set to null, the offset defaults to a vertical offset
		 * equal to the height.
		 */
		public @NotNull IconBuilder level(Integer lX, Integer lY) {
			this.lX = lX != null? lX : Integer.MAX_VALUE;
			this.lY = lY != null? lY : Integer.MAX_VALUE;
			return this;
		}
		
		/**
		 * Mark icons as only having two levels, for button rendering.
		 */
		public @NotNull IconBuilder twoLevel(boolean twoLevel) {
			this.twoLevel = twoLevel;
			return this;
		}
		
		/**
		 * Change the offset applied to all created icons
		 */
		public @NotNull IconBuilder offset(int ox, int oy) {
			ou = ox;
			ov = oy;
			return this;
		}
		
		/**
		 * Consider texture coordinates as reversed from the offset.<br>
		 * Convenient for icons arranged from right to left (at the right edge of the texture).
		 */
		public @NotNull IconBuilder reverseOffset(boolean reverseX, boolean reverseY) {
			revU = reverseX;
			revV = reverseY;
			return this;
		}
		
		/**
		 * Define icon coordinates to be used in subsequent calls to
		 * {@link #crop(int, int, int, int)}, to generate cropped icons from the same position.
		 * @see #cropArea(int, int, int, int)
		 */
		public @NotNull IconBuilder cropPos(int baseU, int baseV) {
			u = baseU;
			v = baseV;
			return this;
		}
		
		/**
		 * Define a crop area to be used in subsequent calls to {@link #cropAt(int, int)}, to generate
		 * icons with the same crop area at different positions.
		 */
		public @NotNull IconBuilder cropArea(int uOffset, int vOffset, int cropW, int cropH) {
			cropU = uOffset;
			cropV = vOffset;
			this.cropW = cropW;
			this.cropH = cropH;
			return this;
		}
		
		/**
		 * Create a cropped icon at the given pos, using the previous crop area set by
		 * {@link #cropArea(int, int, int, int)}.
		 * @see #crop(int, int, int, int)
		 * @see #cropPos(int, int)
		 */
		public @NotNull CropIcon cropAt(int baseU, int baseV) {
			return new CropIcon(
			  texture, revU? ou - baseU - w : ou + baseU, revV? ov - baseV - h : ov + baseV, w, h,
			  lX, lY, tw, th, twoLevel, 0, cropU, cropV, cropW, cropH);
		}
		
		/**
		 * Create a cropped icon using the previous crop area set by
		 * {@link #cropArea(int, int, int, int)}, but using the passed coordinates
		 * as the top left corner of the resulting crop area.<br><br>
		 * In general, you might want to use {@link #cropAt(int, int)} instead, but this
		 * variant is convenient when multiple variants of a crop icon do not fit in the same
		 * crop position.
		 */
		public @NotNull CropIcon cropFor(int cropU, int cropV) {
			return cropAt(cropU  - this.cropU, cropV - this.cropV);
		}
		
		/**
		 * Create a cropped icon using the given crop area, and the position set by {@link #cropPos(int, int)}<br>
		 * Cropped icons only render their cropped region, leaving the rest of their area
		 * transparent, which is useful for rendering composed icons.
		 * @see #cropAt(int, int)
		 * @see #cropArea(int, int, int, int)
		 */
		public @NotNull CropIcon crop(int uOffset, int vOffset, int cropW, int cropH) {
			return new CropIcon(
			  texture, revU? ou - u - w : ou + u, revV? ov - v - h : ov + v, w, h, lX, lY,
			  tw, th, twoLevel, 0, uOffset, vOffset, cropW, cropH);
		}
		
		/**
		 * Configure margin sizes to create 9 patch icons with {@link #patchAt(int, int)}.
		 */
		public @NotNull IconBuilder patchSize(int left, int top, int right, int bottom) {
			patchL = left;
			patchT = top;
			patchR = right;
			patchB = bottom;
			return this;
		}
		
		/**
		 * Create a 9 patch icon, using the margin sizes defined by
		 * a previous call to {@link #patchSize(int, int, int, int)}
		 */
		public @NotNull NinePatchIcon patchAt(int u, int v) {
			return new NinePatchIcon(
			  texture, revU? ou - u - w : ou + u, revV? ov - v - h : ov + v, w, h,
			  patchL, patchT, w - patchL - patchR, h - patchT - patchB,
			  lX, lY, tw, th, twoLevel, 0);
		}
		
		/**
		 * Create an icon at the given coordinates.<br>
		 * The coordinates are relative to the current {@link #offset}
		 */
		public @NotNull Icon at(int u, int v) {
			return new Icon(
			  texture, revU? ou - u - w : ou + u, revV? ov - v - h : ov + v, w, h,
			  lX, lY, tw, th, twoLevel, 0);
		}
	}
}
