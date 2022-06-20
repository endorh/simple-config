package endorh.simpleconfig.clothconfig2.impl;

import com.google.common.collect.Lists;
import endorh.simpleconfig.clothconfig2.api.ScissorsHandler;
import endorh.simpleconfig.clothconfig2.api.ScissorsScreen;
import endorh.simpleconfig.clothconfig2.math.Rectangle;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

import static java.lang.Math.abs;
import static java.lang.Math.min;

@OnlyIn(value = Dist.CLIENT)
@ApiStatus.Internal
public final class ScissorsHandlerImpl
  implements ScissorsHandler {
	@ApiStatus.Internal
	public static final ScissorsHandler INSTANCE = new ScissorsHandlerImpl();
	private final List<Rectangle> scissorsAreas = Lists.newArrayList();
	
	@Override
	public void clearScissors() {
		this.scissorsAreas.clear();
		this.applyScissors();
	}
	
	@Override
	public List<Rectangle> getScissorsAreas() {
		return Collections.unmodifiableList(this.scissorsAreas);
	}
	
	@Override
	public void scissor(Rectangle rectangle) {
		this.scissorsAreas.add(rectangle);
		this.applyScissors();
	}
	
	@Override
	public void removeLastScissor() {
		if (!this.scissorsAreas.isEmpty())
			this.scissorsAreas.remove(this.scissorsAreas.size() - 1);
		this.applyScissors();
	}
	
	@Override
	public void applyScissors() {
		final Minecraft mc = Minecraft.getInstance();
		if (!this.scissorsAreas.isEmpty()) {
			Rectangle r = this.scissorsAreas.get(0).copy();
			for (int i = 1; i < this.scissorsAreas.size(); ++i) {
				Rectangle r1 = this.scissorsAreas.get(i);
				if (!r.intersects(r1)) {
					if (mc.screen instanceof ScissorsScreen) {
						this._applyScissor(
						  ((ScissorsScreen) mc.screen).handleScissor(
							 new Rectangle()));
					} else {
						this._applyScissor(new Rectangle());
					}
					return;
				}
				r.setBounds(r.intersection(r1));
			}
			r.setBounds(
			  min(r.x, r.x + r.width), min(r.y, r.y + r.height),
			  abs(r.width), abs(r.height));
			if (mc.screen instanceof ScissorsScreen) {
				this._applyScissor(
				  ((ScissorsScreen) mc.screen).handleScissor(r));
			} else {
				this._applyScissor(r);
			}
		} else if (mc.screen instanceof ScissorsScreen) {
			this._applyScissor(
			  ((ScissorsScreen) mc.screen).handleScissor(null));
		} else {
			this._applyScissor(null);
		}
	}
	
	public void _applyScissor(Rectangle r) {
		if (r != null) {
			GL11.glEnable(3089);
			if (r.isEmpty()) {
				GL11.glScissor(0, 0, 0, 0);
			} else {
				MainWindow window = Minecraft.getInstance().getWindow();
				double scaleFactor = window.getGuiScale();
				GL11.glScissor(
              (int) ((double) r.x * scaleFactor),
              (int) ((double) (window.getGuiScaledHeight() - r.height - r.y) * scaleFactor),
              (int) ((double) r.width * scaleFactor),
              (int) ((double) r.height * scaleFactor));
			}
		} else {
			GL11.glDisable(3089);
		}
	}
}

