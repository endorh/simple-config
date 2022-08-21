package endorh.simpleconfig.ui.hotkey;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.config.ClientConfig.hotkey_log.overlay;
import endorh.simpleconfig.config.ClientConfig.hotkey_log.toast;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import static java.lang.Math.max;
import static java.lang.Math.min;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(value = Dist.CLIENT, modid = SimpleConfigMod.MOD_ID)
public class ConfigHotKeyOverlay {
	private static final List<QueuedHotKeyMessage> messages = new ArrayList<>();
	private static final int FADE_OUT = 500;
	private static ITextComponent toastMessage;
	private static long toastTimestamp;
	
	public static void addToastMessage(String message) {
		addToastMessage(new StringTextComponent(message));
	}
	
	public static void addToastMessage(ITextComponent message) {
		ConfigHotKeyOverlay.toastMessage = message;
		toastTimestamp = System.currentTimeMillis();
	}
	
	public static void addMessage(ITextComponent title, List<ITextComponent> message) {
		QueuedHotKeyMessage m = new QueuedHotKeyMessage(title, message, System.currentTimeMillis());
		synchronized (messages) {
			messages.add(m);
		}
	}
	
	@SubscribeEvent public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
		if (event.getType() == ElementType.ALL) {
			MatrixStack mStack = event.getMatrixStack();
			long time = System.currentTimeMillis();
			ITextComponent msg = toastMessage;
			if (msg != null) {
				if (time - toastTimestamp > toast.display_time_ms) toastMessage = null;
				renderToastMessage(mStack, msg);
			}
			MainWindow window = event.getWindow();
			int w = window.getGuiScaledWidth();
			int h = window.getGuiScaledHeight();
			int b = (int) (h * 0.9);
			int minY = (int) (h * 0.5);
			int maxWidth = (int) (w * 0.35);
			int duration = overlay.display_time_ms;
			synchronized (messages) {
				if (messages.isEmpty()) return;
				ListIterator<QueuedHotKeyMessage> iter = messages.listIterator(messages.size());
				while (iter.hasPrevious()) {
					QueuedHotKeyMessage message = iter.previous();
					if (b <= minY) message.evict();
					b -= message.getHeight();
					if (time - message.getTimestamp() > duration) {
						iter.remove();
					} else message.render(mStack, w, b, maxWidth);
				}
			}
		}
	}
	
	private static void renderToastMessage(MatrixStack mStack, ITextComponent message) {
		int duration = max(0, toast.display_time_ms - FADE_OUT);
		long time = System.currentTimeMillis() - toastTimestamp;
		if (time >= duration + FADE_OUT) return;
		float alpha = time > duration? 1F - (time - duration) / (float) FADE_OUT : 1F;
		if (alpha < 0.05F) return; // Small alpha values render as opaque?
		FontRenderer font = Minecraft.getInstance().font;
		int width = font.width(message);
		MainWindow window = Minecraft.getInstance().getWindow();
		int screenWidth = window.getGuiScaledWidth();
		int screenHeight = window.getGuiScaledHeight();
		int textX = screenWidth / 2 - width / 2;
		int textY = (int) ((screenHeight - font.lineHeight - 2) * (1F - toast.relative_height));
		int opacity = (int) (0xFF * (toast.background_opacity * 0.9F + 0.1F));
		int backgroundColor = alpha(opacity << 24, alpha);
		int textColor = alpha(0xE0E0E0E0, alpha);
		AbstractGui.fill(mStack, textX - 2, textY - 1, textX + width + 4,
		     textY + font.lineHeight + 2, backgroundColor);
		AbstractGui.drawString(mStack, font, message, textX, textY, textColor);
	}
	
	public static class QueuedHotKeyMessage {
		private final ITextComponent title;
		private final List<ITextComponent> message;
		private long timestamp;
		
		public QueuedHotKeyMessage(ITextComponent title, List<ITextComponent> message, long timestamp) {
			this.title = title;
			this.message = message;
			this.timestamp = timestamp;
		}
		
		public ITextComponent getTitle() {
			return title;
		}
		
		public List<ITextComponent> getMessage() {
			return message;
		}
		
		public long getTimestamp() {
			return timestamp;
		}
		
		public void evict() {
			int duration = max(0, overlay.display_time_ms - FADE_OUT);
			long time = System.currentTimeMillis();
			if (time - timestamp < duration) timestamp = time - duration;
		}
		
		public void render(MatrixStack mStack, int r, int y, int maxWidth) {
			int duration = max(0, overlay.display_time_ms - FADE_OUT);
			long time = System.currentTimeMillis() - getTimestamp();
			if (time >= duration + FADE_OUT) return;
			float alpha = time > duration? 1F - (time - duration) / (float) FADE_OUT : 1F;
			if (alpha < 0.05F) return; // Small alpha values render as opaque?
			FontRenderer font = Minecraft.getInstance().font;
			List<ITextComponent> messages = getMessage();
			ITextComponent title = getTitle();
			int width = messages.stream().mapToInt(font::width).max().orElse(0) + 14;
			width = max(width, font.width(title) + 2);
			width = min(width, maxWidth);
			int lH = font.lineHeight + 2;
			int h = lH * (messages.size() + 1);
			int x = r - width - 2;
			int opacity = (int) (0xFF * (overlay.background_opacity * 0.9F + 0.1F));
			int backgroundColor = alpha(opacity << 24, alpha);
			int textColor = alpha(0xE0E0E0E0, alpha);
			AbstractGui.fill(mStack, x, y, r, y + h, backgroundColor);
			drawStringTrimmed(mStack, title, width - 2, x + 1, y + 1, textColor);
			y += lH;
			x += 12;
			width -= 12;
			for (ITextComponent m: messages) {
				drawStringTrimmed(mStack, m, width - 2, x + 1, y + 1, textColor);
				y += lH;
			}
		}
		
		private void drawStringTrimmed(
		  MatrixStack mStack, ITextComponent text, int width, float x, float y, int color
		) {
			FontRenderer font = Minecraft.getInstance().font;
			IReorderingProcessor line = font.split(text, width).get(0);
			font.draw(mStack, line, x, y, color);
		}
		
		private int getHeight() {
			int lineHeight = Minecraft.getInstance().font.lineHeight + 2;
			return lineHeight * (getMessage().size() + 1);
		}
		
	}
	private static int alpha(int color, float alpha) {
		return 0xFFFFFF & color | (int) ((color >> 24 & 0xFF) * alpha) << 24;
	}
}
