package endorh.simpleconfig.ui.gui.widget;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import endorh.simpleconfig.api.ui.math.Rectangle;
import endorh.simpleconfig.config.ClientConfig.advanced;
import endorh.simpleconfig.ui.api.ScrollingHandler;
import endorh.simpleconfig.ui.gui.widget.DynamicEntryListWidget.ListEntry;
import endorh.simpleconfig.ui.impl.EasingMethod;
import endorh.simpleconfig.ui.impl.EasingMethod.EasingMethodImpl;
import endorh.simpleconfig.ui.math.impl.PointHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import static java.lang.Math.*;

@OnlyIn(value = Dist.CLIENT)
public abstract class DynamicNewSmoothScrollingEntryListWidget<E extends ListEntry>
  extends DynamicEntryListWidget<E> {
	protected double target;
	protected boolean smoothScrolling = true;
	protected long start;
	protected long duration;
	protected long last = 0;
	protected ListEntry scrollTargetEntry = null;
	protected ListEntry followedEntry = null;
	protected long followEntryStart = 0L;
	protected int userOverScroll;
	
	protected DynamicNewSmoothScrollingEntryListWidget(
      Minecraft client, int width, int height, int top, int bottom,
      ResourceLocation backgroundLocation
   ) {
		super(client, width, height, top, bottom, backgroundLocation);
	}
	
	@Override public void resize(int width, int height, int top, int bottom) {
		double center = (this.bottom - this.top) / 2D;
		if (center < 0) center = (bottom - top) / 2D;
		final double prevTarget = target + center;
		super.resize(width, height, top, bottom);
		scrollTo(prevTarget - (this.bottom - this.top) / 2D, false);
	}
	
	protected int getActualMaxScrollPosition() {
		return super.getMaxScrollPosition();
	}
	
	@Override protected int getMaxScrollPosition() {
		return max(getActualMaxScrollPosition(), userOverScroll + bottom - top - 4);
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
			this.scroll = Mth.clamp(scroll, 0.0, getMaxScroll());
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
				double double_5 = max(1, getMaxScroll());
				int int_2 = bottom - top;
				int int_3 = Mth.clamp((int) ((float) (int_2 * int_2) / (float) getMaxScrollPosition()), 32, int_2 - 8);
				double double_6 = max(1.0, double_5 / (double) (int_2 - int_3));
				setScroll(
				  Mth.clamp(getScroll() + deltaY * double_6, 0.0, getMaxScroll()));
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
			  Mth.clamp(delta, 0.0, getMaxScroll());
			return true;
		}
		// Do not animate for smaller delta values, since they usually come from
		//   scrolling macros or special mouse wheels that already produce a smooth visual
		scrollBy(16.0 * -delta, abs(delta) >= 1.0);
		return true;
	}
	
	@Override protected void scrollBy(int amount) {
		scrollBy(amount, true);
	}
	
	@Override public void scrollTo(double scroll) {
		scrollTo(scroll, true);
	}
	
	@Override public void scrollTo(ListEntry entry) {
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
		scrollTo(value, animated, 200L);
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
		return t < start + duration || t < last + 200L / 2;
	}

	@Override public @Nullable ComponentPath getCurrentFocusPath() {
		return super.getCurrentFocusPath();
		// ComponentPath path = super.getCurrentFocusPath();
		// if (path instanceof ComponentPath.Path p)
		// 	return ListPath.pathAsListPath(p);
		// return path;
	}

	@Override public @Nullable ComponentPath nextFocusPath(@NotNull FocusNavigationEvent e) {
		return super.nextFocusPath(e);
		// ComponentPath path = super.nextFocusPath(e);
		// if (path instanceof ComponentPath.Path p)
		// 	return ListPath.pathAsListPath(p);
		// return path;
	}

	public record ListPath(
		@NotNull DynamicElementListWidget<?> list,
		@NotNull ComponentPath child
	) implements ComponentPath {
		public static ComponentPath listPath(@NotNull DynamicElementListWidget<?> list, @NotNull ComponentPath child) {
			return new ListPath(list, child);
		}

		public static ComponentPath pathAsListPath(ComponentPath.Path path) {
			ContainerEventHandler component = path.component();
			if (!(component instanceof DynamicElementListWidget<?>))
				throw new IllegalArgumentException("Path component is not a DynamicElementListWidget");
			return listPath((DynamicElementListWidget<?>) component, path.childPath());
		}

		@Override public @NotNull GuiEventListener component() {
			return list;
		}

		@Override public void applyFocus(boolean focused) {
			list.setFocused(focused);
			child.applyFocus(focused);
			if (focused) list.ensureFocusedVisible();
		}
	}

	protected double entryScroll(ListEntry e) {
		return scrollFor(e.getScrollY(), e.getCaptionHeight());
	}
	
	@Override
	public void render(@NotNull PoseStack mStack, int mouseX, int mouseY, float delta) {
		long time = System.currentTimeMillis();
		double[] target = {this.target};
		double prev = scroll;
		userOverScroll = advanced.allow_over_scroll? min(getActualMaxScrollPosition(), (int) scroll) : 0;
		int maxScroll = getMaxScroll();
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
			else this.target = Mth.clamp(this.target, 0, maxScroll);
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
	  PoseStack matrices, Tesselator tessellator, BufferBuilder buffer, int maxScroll,
	  int sbMinX, int sbMaxX
	) {
		if (!smoothScrolling) {
			super.renderScrollBar(
			  matrices, tessellator, buffer, maxScroll, sbMinX, sbMaxX);
		} else if (maxScroll > 0) {
			RenderSystem.setShader(GameRenderer::getPositionColorShader);
			RenderSystem.disableDepthTest();
			RenderSystem.enableBlend();
			RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ZERO, DestFactor.ONE);
			RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
			int height = (bottom - top) * (bottom - top) / getMaxScrollPosition();
			height = Mth.clamp(height, 32, bottom - top - 8);
			height = (int) ((double) height - min(
			  scroll < 0.0
           ? (int) -scroll
           : scroll > (double) getMaxScroll()? (int) scroll - getMaxScroll() : 0,
			  (double) height * 0.95));
			height = max(10, height);
			int minY = min(max(
			  (int) getScroll() * (bottom - top - height) / maxScroll + top,
			  top), bottom - height);
			int bc = new Rectangle(sbMinX, minY, sbMaxX - sbMinX, height).contains(PointHelper.ofMouse()) ? 168 : 128;
			int tc = new Rectangle(sbMinX, minY, sbMaxX - sbMinX, height).contains(PointHelper.ofMouse()) ? 222 : 172;
			Matrix4f matrix = matrices.last().pose();
			// @formatter:off
			buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
			buffer.vertex(matrix, (float) sbMinX, (float) bottom, 0F).color(0, 0, 0, 255).endVertex();
			buffer.vertex(matrix, (float) sbMaxX, (float) bottom, 0F).color(0, 0, 0, 255).endVertex();
			buffer.vertex(matrix, (float) sbMaxX, (float) top,    0F).color(0, 0, 0, 255).endVertex();
			buffer.vertex(matrix, (float) sbMinX, (float) top,    0F).color(0, 0, 0, 255).endVertex();
			tessellator.end();
			buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
			buffer.vertex(matrix, (float) sbMinX, (float) (minY + height), 0F).color(bc, bc, bc, 255).endVertex();
			buffer.vertex(matrix, (float) sbMaxX, (float) (minY + height), 0F).color(bc, bc, bc, 255).endVertex();
			buffer.vertex(matrix, (float) sbMaxX, (float)  minY,           0F).color(bc, bc, bc, 255).endVertex();
			buffer.vertex(matrix, (float) sbMinX, (float)  minY,           0F).color(bc, bc, bc, 255).endVertex();
			tessellator.end();
			buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
			buffer.vertex(matrix, (float)  sbMinX,      (float) (minY + height - 1), 0F).color(tc, tc, tc, 255).endVertex();
			buffer.vertex(matrix, (float) (sbMaxX - 1), (float) (minY + height - 1), 0F).color(tc, tc, tc, 255).endVertex();
			buffer.vertex(matrix, (float) (sbMaxX - 1), (float)  minY,               0F).color(tc, tc, tc, 255).endVertex();
			buffer.vertex(matrix, (float)  sbMinX,      (float)  minY,               0F).color(tc, tc, tc, 255).endVertex();
			tessellator.end();
			// @formatter:on
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
			return start + (end - start) * ((EasingMethod) EasingMethodImpl.CIRC).apply(amount);
		}
	}
}

