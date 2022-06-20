package endorh.simpleconfig.clothconfig2.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.clothconfig2.ClothConfigInitializer;
import endorh.simpleconfig.clothconfig2.api.ScrollingHandler;
import endorh.simpleconfig.clothconfig2.math.Rectangle;
import endorh.simpleconfig.clothconfig2.math.impl.PointHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import static java.lang.Math.*;

@OnlyIn(value = Dist.CLIENT)
public abstract class DynamicNewSmoothScrollingEntryListWidget<E extends DynamicEntryListWidget.Entry>
  extends DynamicEntryListWidget<E> {
	protected double target;
	protected boolean smoothScrolling = true;
	protected long start;
	protected long duration;
	protected long last = 0;
	protected Entry scrollTargetEntry = null;
	protected Entry followedEntry = null;
	protected long followEntryStart = 0L;
	
	public DynamicNewSmoothScrollingEntryListWidget(
	  Minecraft client, int width, int height, int top, int bottom,
	  ResourceLocation backgroundLocation
	) {
		super(client, width, height, top, bottom, backgroundLocation);
	}
	
	@Override public void resize(int width, int height, int top, int bottom) {
		final double prevTarget = target + (this.bottom - this.top) / 2D;
		super.resize(width, height, top, bottom);
		scrollTo(prevTarget - (this.bottom - this.top) / 2D, false);
	}
	
	public boolean isSmoothScrolling() {
		return smoothScrolling;
	}
	
	public void setSmoothScrolling(boolean smoothScrolling) {
		this.smoothScrolling = smoothScrolling;
	}
	
	@Override
	public void setScroll(double scroll) {
		if (!smoothScrolling) {
			this.scroll =
			  MathHelper.clamp(scroll, 0.0, getMaxScroll());
		} else {
			this.scroll = ScrollingHandler.clampExtension(scroll, getMaxScroll());
			target = ScrollingHandler.clampExtension(scroll, getMaxScroll());
		}
	}
	
	@Override
	public boolean mouseDragged(
	  double mouseX, double mouseY, int button, double deltaX, double deltaY
	) {
		if (!smoothScrolling) {
			return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
		}
		if (getFocusedItem() != null && isDragging() &&
		    getFocusedItem().mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
			return true;
		}
		if (button == 0 && scrolling) {
			if (mouseY < (double) top) {
				setScroll(0.0);
			} else if (mouseY > (double) bottom) {
				setScroll(getMaxScroll());
			} else {
				double double_5 = Math.max(1, getMaxScroll());
				int int_2 = bottom - top;
				int int_3 = MathHelper.clamp(
              (int) ((float) (int_2 * int_2) / (float) getMaxScrollPosition()),
              32, int_2 - 8);
				double double_6 = Math.max(1.0, double_5 / (double) (int_2 - int_3));
				setScroll(
				  MathHelper.clamp(getScroll() + deltaY * double_6, 0.0,
				                   getMaxScroll()));
			}
			return true;
		}
		return false;
	}
	
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		for (E entry : children()) {
			if (!entry.mouseScrolled(mouseX, mouseY, delta)) continue;
			return true;
		}
		if (!smoothScrolling) {
			scroll += 16.0 * -delta;
			scroll =
			  MathHelper.clamp(delta, 0.0, getMaxScroll());
			return true;
		}
		// Do not animate for smaller delta values, since they usually come from
		//   scrolling macros or special mouse wheels that already produce a smooth visual
		scrollBy(ClothConfigInitializer.getScrollStep() * -delta, abs(delta) >= 1.0);
		return true;
	}
	
	@Override protected void scrollBy(int amount) {
		scrollBy(amount, true);
	}
	
	@Override public void scrollTo(double scroll) {
		scrollTo(scroll, true);
	}
	
	public void scrollTo(Entry entry) {
		entry.expandParents();
		scrollTargetEntry = entry;
		followedEntry = null;
	}
	
	public void scrollBy(double value, boolean animated) {
		final int maxScroll = getMaxScroll();
		if (value < 0 && target > maxScroll)
			scrollTo(maxScroll + value, animated);
		if (value > 0 && target < 0)
			scrollTo(value, animated);
		else scrollTo(target + value, animated);
	}
	
	public void scrollTo(double value, boolean animated) {
		scrollTo(value, animated, ClothConfigInitializer.getScrollDuration());
	}
	
	public void scrollTo(double value, boolean animated, long duration) {
		target = ScrollingHandler.clampExtension(value, getMaxScroll());
		if (animated) {
			start = System.currentTimeMillis();
			this.duration = duration;
		} else {
			scroll = ScrollingHandler.clampExtension(target, getMaxScroll(), 0.0);
			last = System.currentTimeMillis();
		}
		scrollTargetEntry = null;
	}
	
	public boolean isScrollingNow() {
		final long t = System.currentTimeMillis();
		return t < start + duration || t < last + ClothConfigInitializer.getScrollDuration() / 2;
	}
	
	protected double entryScroll(Entry e) {
		final int half = (bottom - top) / 2;
		return max(0, e.getScrollY() - top - half + min(half, e.getCaptionHeight() / 2));
	}
	
	@Override
	public void render(@NotNull MatrixStack mStack, int mouseX, int mouseY, float delta) {
		long time = System.currentTimeMillis();
		if (followedEntry != null && time - followEntryStart < duration + 50L)
			this.target = entryScroll(followedEntry);
		double[] target = new double[]{this.target};
		double prev = scroll;
		final int maxScroll = getMaxScroll();
		scroll = ScrollingHandler.handleScrollingPosition(
		  target, scroll, Double.POSITIVE_INFINITY, delta, start, duration);
		if (scroll > maxScroll && scroll > prev)
			scroll = maxScroll;
		if (scroll < 0 && prev > scroll)
			scroll = 0;
		this.target = target[0];
		if (time > start + duration) {
			// Animate scroll on height changes
			if (scroll < 0 || scroll > maxScroll)
				scrollTo(scroll < 0 ? 0 : maxScroll, true);
			else this.target = MathHelper.clamp(this.target, 0, maxScroll);
		}
		super.render(mStack, mouseX, mouseY, delta);
		if (scrollTargetEntry != null) {
			followedEntry = scrollTargetEntry;
			scrollTo(entryScroll(scrollTargetEntry));
			followEntryStart = time;
			scrollTargetEntry = null;
		}
	}
	
	@Override
	protected void renderScrollBar(
	  MatrixStack matrices, Tessellator tessellator, BufferBuilder buffer, int maxScroll,
	  int sbMinX, int sbMaxX
	) {
		if (!smoothScrolling) {
			super.renderScrollBar(
			  matrices, tessellator, buffer, maxScroll, sbMinX, sbMaxX);
		} else if (maxScroll > 0) {
			int height =
			  (bottom - top) * (bottom - top) / getMaxScrollPosition();
			height = MathHelper.clamp(height, 32, bottom - top - 8);
			height = (int) ((double) height - Math.min(
           scroll < 0.0 ? (int) (-scroll)
                                       : (scroll > (double) getMaxScroll() ?
                                          (int) scroll - getMaxScroll() : 0),
			  (double) height * 0.95));
			height = Math.max(10, height);
			int minY = Math.min(Math.max(
			  (int) getScroll() * (bottom - top - height) / maxScroll + top,
			  top), bottom - height);
			int bottomc =
			  new Rectangle(sbMinX, minY, sbMaxX - sbMinX,
			                height).contains(PointHelper.ofMouse()) ? 168 : 128;
			int topc =
			  new Rectangle(sbMinX, minY, sbMaxX - sbMinX,
			                height).contains(PointHelper.ofMouse()) ? 222 : 172;
			Matrix4f matrix = matrices.last().pose();
			buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
			buffer.vertex(matrix, (float) sbMinX, (float) bottom, 0.0f)
			  .uv(0.0f, 1.0f).color(0, 0, 0, 255).endVertex();
			buffer.vertex(matrix, (float) sbMaxX, (float) bottom, 0.0f)
			  .uv(1.0f, 1.0f).color(0, 0, 0, 255).endVertex();
			buffer.vertex(matrix, (float) sbMaxX, (float) top, 0.0f).uv(1.0f, 0.0f)
			  .color(0, 0, 0, 255).endVertex();
			buffer.vertex(matrix, (float) sbMinX, (float) top, 0.0f).uv(0.0f, 0.0f)
			  .color(0, 0, 0, 255).endVertex();
			tessellator.end();
			buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
			buffer.vertex(matrix, (float) sbMinX, (float) (minY + height), 0.0f)
			  .uv(0.0f, 1.0f).color(bottomc, bottomc, bottomc, 255).endVertex();
			buffer.vertex(matrix, (float) sbMaxX, (float) (minY + height), 0.0f)
			  .uv(1.0f, 1.0f).color(bottomc, bottomc, bottomc, 255).endVertex();
			buffer.vertex(matrix, (float) sbMaxX, (float) minY, 0.0f).uv(1.0f, 0.0f)
			  .color(bottomc, bottomc, bottomc, 255).endVertex();
			buffer.vertex(matrix, (float) sbMinX, (float) minY, 0.0f).uv(0.0f, 0.0f)
			  .color(bottomc, bottomc, bottomc, 255).endVertex();
			tessellator.end();
			buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
			buffer.vertex(matrix, (float) sbMinX, (float) (minY + height - 1), 0.0f)
			  .uv(0.0f, 1.0f).color(topc, topc, topc, 255).endVertex();
			buffer.vertex(matrix, (float) (sbMaxX - 1), (float) (minY + height - 1), 0.0f)
			  .uv(1.0f, 1.0f).color(topc, topc, topc, 255).endVertex();
			buffer.vertex(matrix, (float) (sbMaxX - 1), (float) minY, 0.0f).uv(1.0f, 0.0f)
			  .color(topc, topc, topc, 255).endVertex();
			buffer.vertex(matrix, (float) sbMinX, (float) minY, 0.0f).uv(0.0f, 0.0f)
			  .color(topc, topc, topc, 255).endVertex();
			tessellator.end();
		}
	}
	
	public static class Precision {
		public static final float FLOAT_EPSILON = 0.001f;
		public static final double DOUBLE_EPSILON = 1.0E-7;
		
		public static boolean almostEquals(float value1, float value2, float acceptableDifference) {
			return abs(value1 - value2) <= acceptableDifference;
		}
		
		public static boolean almostEquals(
		  double value1, double value2, double acceptableDifference
		) {
			return abs(value1 - value2) <= acceptableDifference;
		}
	}
	
	public static class Interpolation {
		public static double expoEase(double start, double end, double amount) {
			return start + (end - start) * ClothConfigInitializer.getEasingMethod().apply(amount);
		}
	}
}

