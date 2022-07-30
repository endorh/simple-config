package endorh.simpleconfig.ui.hotkey;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static net.minecraft.client.gui.AbstractGui.fill;

@EventBusSubscriber
public class ConfigHotKeyOverlay {
	private static final List<QueuedHotKeyMessage> messages = new ArrayList<>();
	private static final int DURATION = 1500;
	private static final int FADE_OUT = 500;
	
	public static void addMessage(ITextComponent title, List<ITextComponent> message) {
		QueuedHotKeyMessage m = new QueuedHotKeyMessage(title, message, System.currentTimeMillis());
		synchronized (messages) {
			messages.add(m);
		}
	}
	
	@SubscribeEvent public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
		FontRenderer font = Minecraft.getInstance().fontRenderer;
		MatrixStack mStack = event.getMatrixStack();
		MainWindow window = event.getWindow();
		int w = window.getScaledWidth();
		int h = window.getScaledHeight();
		int b = (int) (h * 0.9);
		int minY = (int) (h * 0.5);
		int maxWidth = (int) (w * 0.35);
		long time = System.currentTimeMillis();
		synchronized (messages) {
			if (messages.isEmpty()) return;
			ListIterator<QueuedHotKeyMessage> iter = messages.listIterator(messages.size());
			while (iter.hasPrevious()) {
				QueuedHotKeyMessage message = iter.previous();
				if (b <= minY) message.evict();
				b -= message.getHeight();
				message.render(mStack, w, b, maxWidth);
				if (time - message.getTimestamp() > DURATION + FADE_OUT) iter.remove();
			}
		}
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
			long time = System.currentTimeMillis();
			if (time - timestamp < DURATION) timestamp = time - DURATION;
		}
		
		public void render(MatrixStack mStack, int r, int y, int maxWidth) {
			long time = System.currentTimeMillis() - getTimestamp();
			if (time > DURATION + FADE_OUT) return;
			float alpha = time > DURATION? 1F - (time - DURATION) / (float) FADE_OUT : 1F;
			FontRenderer font = Minecraft.getInstance().fontRenderer;
			List<ITextComponent> messages = getMessage();
			ITextComponent title = getTitle();
			int width = messages.stream().mapToInt(font::getStringPropertyWidth).max().orElse(0) + 14;
			width = max(width, font.getStringPropertyWidth(title) + 2);
			width = min(width, maxWidth);
			int lH = font.FONT_HEIGHT + 2;
			int h = lH * (messages.size() + 1);
			int x = r - width - 2;
			int backgroundColor = alpha(0x32323232, alpha);
			int textColor = alpha(0xE0E0E0E0, alpha);
			fill(mStack, x, y, r, y + h, backgroundColor);
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
			FontRenderer font = Minecraft.getInstance().fontRenderer;
			IReorderingProcessor line = font.trimStringToWidth(text, width).get(0);
			font.func_238422_b_(mStack, line, x, y, color);
		}
		
		private int getHeight() {
			int lineHeight = Minecraft.getInstance().fontRenderer.FONT_HEIGHT + 2;
			return lineHeight * (getMessage().size() + 1);
		}
		
		private int alpha(int color, float alpha) {
			return 0xFFFFFF & color | (int) (((color >> 24) & 0xFF) * alpha) << 24;
		}
	}
}
