package endorh.simpleconfig.ui.api;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import endorh.simpleconfig.api.ui.math.Rectangle;
import endorh.simpleconfig.ui.impl.EasingMethod;
import endorh.simpleconfig.ui.impl.EasingMethod.EasingMethodImpl;
import endorh.simpleconfig.ui.math.impl.PointHelper;
import net.minecraft.util.Mth;

import static java.lang.Math.max;
import static java.lang.Math.min;

// The name ScrollingContainer is not appropriate, since this does not "contain" anything
public abstract class ScrollingHandler {
	public double scrollAmount;
	public double scrollTarget;
	public long start;
	public long duration;
	public boolean draggingScrollBar = false;
	public boolean hideScrollBar = false;
	
	public abstract Rectangle getBounds();
	
	public Rectangle getScissorBounds() {
		Rectangle bounds = getBounds();
		if (hasScrollBar()) {
			return new Rectangle(bounds.x, bounds.y, bounds.width - 6, bounds.height);
		}
		return bounds;
	}
	
	public int getScrollBarX() {
		return hasScrollBar() ? getBounds().getMaxX() - 6 : getBounds().getMaxX();
	}
	
	public boolean hasScrollBar() {
		return !isHideScrollBar() && getMaxScrollHeight() > getBounds().height;
	}
	
	public boolean isHideScrollBar() {
		return hideScrollBar;
	}
	
	public void setHideScrollBar(boolean hide) {
		this.hideScrollBar = hide;
	}
	
	public abstract int getMaxScrollHeight();
	
	public int getMaxScroll() {
		return max(0, getMaxScrollHeight() - getBounds().height);
	}
	
	public double clamp(double v) {
		return clamp(v, 200.0);
	}
	
	public double clamp(double v, double clampExtension) {
		return Mth.clamp(v, -clampExtension, (double) getMaxScroll() + clampExtension);
	}
	
	public void offset(double value, boolean animated) {
		scrollTo(scrollTarget + value, animated);
	}
	
	public void scrollTo(double value, boolean animated) {
		scrollTo(value, animated, 200L);
	}
	
	public void scrollTo(double value, boolean animated, long duration) {
		scrollTarget = clamp(value);
		if (animated) {
			start = System.currentTimeMillis();
			this.duration = duration;
		} else {
			scrollAmount = scrollTarget;
		}
	}
	
	public void scrollToShow(double start, double end) {
		scrollToShow(start, end, true);
	}
	
	public void scrollToShow(double start, double end, boolean animated) {
		int height = getBounds().getHeight();
		double margin = min(height * 0.15, 48);
		double current = scrollAmount;
		if (current > start - margin) {
			scrollTo(start - margin, animated);
		} else if (current < end + margin - height) {
			scrollTo(min(start - margin, end + margin - height), animated);
		}
	}
	
	public void updatePosition(float delta) {
		double[] target = new double[]{scrollTarget};
		scrollAmount = ScrollingHandler.handleScrollingPosition(
		  target, scrollAmount, getMaxScroll(), delta, start, duration);
		scrollTarget = target[0];
	}
	
	public static double handleScrollingPosition(
	  double[] target, double scroll, double maxScroll, float delta, double start, double duration
	) {
		return ScrollingHandler.handleScrollingPosition(
		  target, scroll, maxScroll, delta, start, duration,
		  -10.0, EasingMethodImpl.CIRC);
	}
	
	public static double handleScrollingPosition(
	  double[] target, double scroll, double maxScroll, float delta, double start, double duration,
	  double bounceBackMultiplier, EasingMethod easingMethod
	) {
		if (bounceBackMultiplier >= 0.0) {
			target[0] = ScrollingHandler.clampExtension(target[0], maxScroll);
			if (target[0] < 0.0) {
				target[0] = target[0] - target[0] * (1.0 - bounceBackMultiplier);
			} else if (target[0] > maxScroll) {
				target[0] = (target[0] - maxScroll) *
				            (1.0 - (1.0 - bounceBackMultiplier) * (double) delta / 3.0) + maxScroll;
			}
		} else {
			target[0] = ScrollingHandler.clampExtension(target[0], maxScroll, 32.0);
		}
		return ScrollingHandler.ease(
		  scroll, target[0], min(
			 ((double) System.currentTimeMillis() - start) / duration, 1.0),
		  easingMethod);
	}
	
	public static double ease(double start, double end, double amount, EasingMethod easingMethod) {
		return start + (end - start) * easingMethod.apply(amount);
	}
	
	public static double clampExtension(double value, double maxScroll) {
		return ScrollingHandler.clampExtension(value, maxScroll, 32.0);
	}
	
	public static double clampExtension(double v, double maxScroll, double clampExtension) {
		return Mth.clamp(v, -clampExtension, maxScroll + clampExtension);
	}
	
	public void renderScrollBar() {
		renderScrollBar(0, 1.0f, 1.0f);
	}
	
	public void renderScrollBar(int background, float alpha, float scrollBarAlphaOffset) {
		if (hasScrollBar()) {
			Rectangle bounds = getBounds();
			int maxScroll = getMaxScroll();
			int maxScrollHeight = getMaxScrollHeight();
			if (maxScrollHeight <= 0) maxScrollHeight = 1;
			int height = bounds.height * bounds.height / maxScrollHeight;
			height = Mth.clamp(height, 32, bounds.height);
			height = (int) ((double) height - min(
           scrollAmount < 0.0 ? (int) -scrollAmount
                              : scrollAmount > (double) maxScroll ?
                                (int) scrollAmount - maxScroll : 0,
			  (double) height * 0.95));
			height = max(10, height);
			int minY = Mth.clamp(
			  (int) scrollAmount * (bounds.height - height) / maxScroll + bounds.y,
			  bounds.y, bounds.getMaxY() - height);
			int minX = getScrollBarX();
			int maxX = minX + 6;
			boolean hovered =
			  new Rectangle(minX, minY, maxX - minX, height).contains(PointHelper.ofMouse());
			float bottomC = (hovered ? 0.67f : 0.5f) * scrollBarAlphaOffset;
			float topC = (hovered ? 0.87f : 0.67f) * scrollBarAlphaOffset;
			// @formatter:off
			RenderSystem.disableTexture();
			RenderSystem.enableBlend();
			RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ZERO, DestFactor.ONE);
			RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
			Tesselator tessellator = Tesselator.getInstance();
			BufferBuilder buffer = tessellator.getBuilder();
			float a = (float) (background >> 24 & 0xFF) / 255.0f;
			float r = (float) (background >> 16 & 0xFF) / 255.0f;
			float g = (float) (background >> 8 & 0xFF) / 255.0f;
			float b = (float) (background & 0xFF) / 255.0f;
			buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
			buffer.vertex(minX, bounds.getMaxY(), 0.0).color(r, g, b, a).endVertex();
			buffer.vertex(maxX, bounds.getMaxY(), 0.0).color(r, g, b, a).endVertex();
			buffer.vertex(maxX, bounds.y, 0.0).color(r, g, b, a).endVertex();
			buffer.vertex(minX, bounds.y, 0.0).color(r, g, b, a).endVertex();
			tessellator.end();
			buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
			buffer.vertex(minX, minY + height, 0.0).color(bottomC, bottomC, bottomC, alpha).endVertex();
			buffer.vertex(maxX, minY + height, 0.0).color(bottomC, bottomC, bottomC, alpha).endVertex();
			buffer.vertex(maxX, minY, 0.0).color(bottomC, bottomC, bottomC, alpha).endVertex();
			buffer.vertex(minX, minY, 0.0).color(bottomC, bottomC, bottomC, alpha).endVertex();
			tessellator.end();
			buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
			buffer.vertex(minX, minY + height - 1, 0.0).color(topC, topC, topC, alpha).endVertex();
			buffer.vertex(maxX - 1, minY + height - 1, 0.0).color(topC, topC, topC, alpha).endVertex();
			buffer.vertex(maxX - 1, minY, 0.0).color(topC, topC, topC, alpha).endVertex();
			buffer.vertex(minX, minY, 0.0).color(topC, topC, topC, alpha).endVertex();
			tessellator.end();
			RenderSystem.enableTexture();
			RenderSystem.disableBlend();
			// @formatter:on
		}
	}
	
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
		return mouseDragged(mouseX, mouseY, button, dx, dy, false, 0.0);
	}
	
	public boolean mouseDragged(
	  double mouseX, double mouseY, int button, double dx, double dy, boolean snapToRows,
	  double rowSize
	) {
		if (button == 0 && draggingScrollBar) {
			float height = getMaxScrollHeight();
			Rectangle bounds = getBounds();
			int actualHeight = bounds.height;
			if (mouseY >= (double) bounds.y && mouseY <= (double) bounds.getMaxY()) {
				double maxScroll = max(1, getMaxScroll());
				double int_3 =
				  Mth.clamp((double) (actualHeight * actualHeight) / (double) height, 32.0, actualHeight - 8);
				double double_6 = max(1.0, maxScroll / ((double) actualHeight - int_3));
				float to =
				  Mth.clamp((float) (scrollAmount + dy * double_6), 0.0f, (float) getMaxScroll());
				if (snapToRows) {
					double nearestRow = (double) Math.round((double) to / rowSize) * rowSize;
					scrollTo(nearestRow, false);
				} else {
					scrollTo(to, false);
				}
			}
			return true;
		}
		return false;
	}
	
	public boolean updateDraggingState(double mouseX, double mouseY, int button) {
		double scrollbarPositionMinX;
		if (!hasScrollBar()) {
			return false;
		}
		double height = getMaxScrollHeight();
		Rectangle bounds = getBounds();
		int actualHeight = bounds.height;
		if (height > (double) actualHeight &&
		    mouseY >= (double) bounds.y && mouseY <= (double) bounds.getMaxY() &&
		    mouseX >= (scrollbarPositionMinX = getScrollBarX()) - 1.0 &&
		    mouseX <= scrollbarPositionMinX + 8.0) {
			draggingScrollBar = true;
			return true;
		}
		draggingScrollBar = false;
		return false;
	}
}

