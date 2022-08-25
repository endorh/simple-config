package endorh.simpleconfig.ui.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.ui.api.IMultiTooltipScreen;
import endorh.simpleconfig.ui.api.IOverlayCapableContainer.IOverlayRenderer;
import endorh.simpleconfig.ui.api.IOverlayCapableContainer.OverlayTicket;
import endorh.simpleconfig.ui.api.IOverlayCapableContainer.SortedOverlayCollection;
import endorh.simpleconfig.ui.api.ScissorsHandler;
import endorh.simpleconfig.api.ui.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiScreenEvent;
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
		Screen screen = Minecraft.getInstance().currentScreen;
		if (screen != null) injectVisualOverlay(screen, area, overlay, priority);
	}
	
	public static void injectVisualOverlay(
	  Screen screen, Rectangle area, IOverlayRenderer overlay, int priority
	) {
		getOverlays(screen).add(area, overlay, priority);
	}
	
	@SubscribeEvent public static void onRenderScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
		final SortedOverlayCollection sortedOverlays = getOverlays(event.getGui());
		if (sortedOverlays == null) return;
		MatrixStack mStack = event.getMatrixStack();
		int mouseX = event.getMouseX();
		int mouseY = event.getMouseY();
		float delta = event.getRenderPartialTicks();
		final List<OverlayTicket> removed = new LinkedList<>();
		mStack.push(); {
			mStack.translate(0D, 0D, 100D);
			Screen screen = Minecraft.getInstance().currentScreen;
			IMultiTooltipScreen tScreen =
			  screen instanceof IMultiTooltipScreen? (IMultiTooltipScreen) screen : null;
			for (OverlayTicket ticket: sortedOverlays) {
				if (tScreen != null) tScreen.removeTooltips(ticket.area);
				ScissorsHandler.INSTANCE.pushScissor(ticket.area);
				if (!ticket.renderer.renderOverlay(mStack, ticket.area, mouseX, mouseY, delta))
					removed.add(ticket);
				ScissorsHandler.INSTANCE.popScissor();
			}
		} mStack.pop();
		sortedOverlays.removeAll(removed);
	}
}
