package endorh.simpleconfig.clothconfig2.math.impl;

import endorh.simpleconfig.clothconfig2.math.Point;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(value = Dist.CLIENT)
public class PointHelper {
	public static Point ofMouse() {
		Minecraft client = Minecraft.getInstance();
		double mx =
		  client.mouseHandler.xpos() * (double) client.getWindow().getGuiScaledWidth() /
		  (double) client.getWindow().getScreenWidth();
		double my =
		  client.mouseHandler.ypos() * (double) client.getWindow().getGuiScaledHeight() /
		  (double) client.getWindow().getScreenHeight();
		return new Point(mx, my);
	}
	
	public static int getMouseX() {
		return PointHelper.ofMouse().x;
	}
	
	public static int getMouseY() {
		return PointHelper.ofMouse().y;
	}
}

