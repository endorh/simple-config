package endorh.simpleconfig.ui.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.api.ui.math.Rectangle;
import endorh.simpleconfig.ui.api.IMultiTooltipScreen;
import endorh.simpleconfig.ui.api.IOverlayCapableContainer.IOverlayRenderer;
import endorh.simpleconfig.ui.api.IOverlayCapableContainer.OverlayTicket;
import endorh.simpleconfig.ui.api.IOverlayCapableContainer.SortedOverlayCollection;
import endorh.simpleconfig.ui.api.ScissorsHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(value=Dist.CLIENT, modid=SimpleConfigMod.MOD_ID)
public class OverlayInjector {
	private static final Map<Screen, SortedOverlayCollection> CACHE = new WeakHashMap<>();
	private static SortedOverlayCollection getOverlays(Screen screen) {
		return CACHE.computeIfAbsent(screen, s -> new SortedOverlayCollection());
	}
	
	public static void injectVisualOverlay(Rectangle area, IOverlayRenderer overlay, int priority) {
		Screen screen = Minecraft.getInstance().screen;
		if (screen != null) injectVisualOverlay(screen, area, overlay, priority);
	}
	
	public static void injectVisualOverlay(
	  Screen screen, Rectangle area, IOverlayRenderer overlay, int priority
	) {
		getOverlays(screen).add(area, overlay, priority);
	}
	
	@SubscribeEvent public static void onRenderScreen(ScreenEvent.Render.Post event) {
		final SortedOverlayCollection sortedOverlays = getOverlays(event.getScreen());
		if (sortedOverlays == null) return;
		GuiGraphics gg = event.getGuiGraphics();
		int mouseX = event.getMouseX();
		int mouseY = event.getMouseY();
		float delta = event.getPartialTick();
		final List<OverlayTicket> removed = new LinkedList<>();
		PoseStack mStack = gg.pose();
		mStack.pushPose(); {
			mStack.translate(0D, 0D, 100D);
			Screen screen = Minecraft.getInstance().screen;
			IMultiTooltipScreen tScreen =
			  screen instanceof IMultiTooltipScreen? (IMultiTooltipScreen) screen : null;
			for (OverlayTicket ticket: sortedOverlays) {
				if (tScreen != null) tScreen.removeTooltips(ticket.area);
				ScissorsHandler.INSTANCE.pushScissor(ticket.area);
				if (!ticket.renderer.renderOverlay(gg, ticket.area, mouseX, mouseY, delta))
					removed.add(ticket);
				ScissorsHandler.INSTANCE.popScissor();
			}
		} mStack.popPose();
		sortedOverlays.removeAll(removed);
	}
}
