package endorh.simpleconfig.ui.impl;

import endorh.simpleconfig.ui.api.ScissorsHandler;
import endorh.simpleconfig.api.ui.math.Rectangle;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.opengl.GL11;

import java.util.Collection;
import java.util.Collections;
import java.util.Stack;

import static java.lang.Math.abs;
import static java.lang.Math.min;

@OnlyIn(value = Dist.CLIENT)
@ApiStatus.Internal
public final class ScissorsHandlerImpl
  implements ScissorsHandler {
	@ApiStatus.Internal
	public static final ScissorsHandler INSTANCE = new ScissorsHandlerImpl();
	private final Stack<Rectangle> scissorsAreas = new Stack<>();
	
	@Override public void clearScissors() {
		scissorsAreas.clear();
		applyScissors();
	}
	
	@Override
	public Collection<Rectangle> getScissorsAreas() {
		return Collections.unmodifiableCollection(scissorsAreas);
	}
	
	@Override public void pushScissor(Rectangle clipArea) {
		scissorsAreas.add(clipArea);
		applyScissors();
	}
	
	@Override public void popScissor() {
		if (!scissorsAreas.isEmpty())
			scissorsAreas.remove(scissorsAreas.size() - 1);
		applyScissors();
	}
	
	@Override public void withoutScissors(Runnable runnable) {
		applyScissor(null);
		runnable.run();
		applyScissors();
	}
	
	private void applyScissors() {
		if (!scissorsAreas.isEmpty()) {
			Rectangle r = scissorsAreas.stream()
			  .reduce(Rectangle::intersection)
			  .orElse(new Rectangle());
			r.setBounds(
			  min(r.x, r.x + r.width), min(r.y, r.y + r.height),
			  abs(r.width), abs(r.height));
			applyScissor(r);
		} else applyScissor(null);
	}
	
	private void applyScissor(Rectangle r) {
		if (r != null) {
			GL11.glEnable(3089);
			if (r.isEmpty()) {
				GL11.glScissor(0, 0, 0, 0);
			} else {
				MainWindow window = Minecraft.getInstance().getMainWindow();
				double scaleFactor = window.getGuiScaleFactor();
				GL11.glScissor(
              (int) ((double) r.x * scaleFactor),
              (int) ((double) (window.getScaledHeight() - r.height - r.y) * scaleFactor),
              (int) ((double) r.width * scaleFactor),
              (int) ((double) r.height * scaleFactor));
			}
		} else {
			GL11.glDisable(3089);
		}
	}
}

