package endorh.simpleconfig.ui.math.impl;

import endorh.simpleconfig.ui.math.Point;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(value = Dist.CLIENT)
public class PointHelper {
	public static Point ofMouse() {
		Minecraft client = Minecraft.getInstance();
		double mx =
		  client.mouseHelper.getMouseX() * (double) client.getMainWindow().getScaledWidth() /
		  (double) client.getMainWindow().getWidth();
		double my =
		  client.mouseHelper.getMouseY() * (double) client.getMainWindow().getScaledHeight() /
		  (double) client.getMainWindow().getHeight();
		return Point.of(mx, my);
	}
	
	public static int getMouseX() {
		return PointHelper.ofMouse().x;
	}
	
	public static int getMouseY() {
		return PointHelper.ofMouse().y;
	}
}

