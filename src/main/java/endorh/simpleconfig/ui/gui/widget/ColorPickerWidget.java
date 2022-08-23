package endorh.simpleconfig.ui.gui.widget;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons;
import endorh.simpleconfig.api.ui.math.Color;
import endorh.simpleconfig.config.ClientConfig.advanced;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.lang.Math.*;

/**
 * Simple color picker with a brightness+saturation frame, a hue bar,
 * an optional transparency bar, a history bar and a palette<br>
 *
 * The history keeps the last modified colors for this instance of
 * the color picker, as well as the initial value, which is locked
 * besides the current color for comparison purposes.<br>
 * The palette is backed by the Simple Config mod config, so it's
 * persistent across reloads and between picker instances.<br>
 * The palette can be used to store colors (right click) and
 * reuse them (left click).
 */
public class ColorPickerWidget extends AbstractWidget {
	protected int colorBg = 0xFF242424;
	protected int colorBorder = 0xFF646464;
	protected int colorBorderHover = 0xFFA0A0A0;
	protected int historySize = 20;
	protected int unit = 12;
	protected int margin = 4;
	
	protected Color value;
	@Nullable protected Consumer<Color> onChange;
	protected Color initial;
	protected boolean allowAlpha = false;
	
	protected int paletteRows;
	protected int shownHistorySize;
	
	protected float lastSaturation;
	protected float lastHue;
	
	protected Color historyMemory;
	protected int historyPreview;
	
	protected List<Color> history;
	
	protected BrightnessSaturationControl brightnessSaturation = new BrightnessSaturationControl();
	protected HueBar hueBar = new HueBar();
	protected TransparencyBar transparencyBar = new TransparencyBar();
	protected HistoryBar historyBar = new HistoryBar();
	protected Palette palette = new Palette();
	
	protected List<SubWidget> subWidgets = Lists.newArrayList(hueBar, brightnessSaturation, transparencyBar, historyBar, palette);
	protected SubWidget lastClicked;
	
	protected boolean isDragging = false;
	
	public ColorPickerWidget(
	  Color initial, int x, int y, @Nullable Consumer<Color> onChange
	) { this(initial, x, y, 142, 84, onChange); }
	
	public ColorPickerWidget(
	  Color initial, int x, int y, int width, int height, @Nullable Consumer<Color> onChange
	) {
		super(x, y, width, height, NarratorChatListener.NO_TITLE);
		this.initial = initial;
		this.value = initial;
		this.onChange = onChange;
		lastHue = initial.getHue();
		lastSaturation = initial.getSaturation();
		history = new ArrayList<>();
	}
	
	public Color getValue() {
		return value;
	}
	
	public void setValue(Color value) { setValue(value, false); }
	
	public void setValue(Color value, boolean suppressUpdate) {
		this.value = value;
		lastHue = value.getHue();
		lastSaturation = value.getSaturation();
		if (!suppressUpdate)
			onChange();
	}
	
	protected void updateValue(Color value) {
		this.value = value;
		onChange();
	}
	
	public Color getInitial() {
		return initial;
	}
	
	public void setInitial(Color initial) {
		this.initial = initial;
	}
	
	@Override
	public void renderButton(@NotNull PoseStack mStack, int mouseX, int mouseY, float partialTicks) {
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		
		palette.x = x + width - 2 * (unit + margin) - margin - 1;
		palette.y = y + margin;
		palette.w = 2 * (unit + margin);
		palette.h = height - 2 * margin;
		
		if (this.allowAlpha) {
			transparencyBar.x = palette.x - unit - margin;
			transparencyBar.y = palette.y;
			transparencyBar.w = unit;
			transparencyBar.h = palette.h - unit - margin;
			
			hueBar.x = transparencyBar.x - unit - margin;
			hueBar.y = palette.y;
			hueBar.w = unit;
			hueBar.h = transparencyBar.h;
		} else {
			hueBar.x = palette.x - unit - margin;
			hueBar.y = palette.y;
			hueBar.w = unit;
			hueBar.h = palette.h - unit - margin;
		}
		
		brightnessSaturation.x = x + margin;
		brightnessSaturation.y = palette.y;
		brightnessSaturation.w = hueBar.x - margin - brightnessSaturation.x;
		brightnessSaturation.h = hueBar.h;
		
		historyBar.x = x + margin;
		historyBar.y = y + height - unit - margin;
		historyBar.w = palette.x - margin - historyBar.x;
		historyBar.h = unit;
		
		drawBox(mStack, x, y, width, height);
		for (SubWidget subWidget : subWidgets)
			subWidget.render(mStack, mouseX, mouseY);
	}
	
	protected void drawBox(PoseStack mStack, int x, int y, int w, int h) {
		fill(mStack, x, y, x + w, y + h, colorBorder);
		fill(mStack, x + 1, y + 1, x + w - 1, y + h - 1, colorBg);
	}
	
	protected boolean drawBox(PoseStack mStack, int x, int y, int w, int h, int mX, int mY) {
		final boolean hovered = mX >= x && mX < x + w && mY >= y && mY < y + h;
		fill(mStack, x, y, x + w, y + h, hovered ? colorBorderHover : colorBorder);
		fill(mStack, x + 1, y + 1, x + w - 1, y + h - 1, colorBg);
		return hovered;
	}
	
	protected List<Color> getHistory() {
		final ArrayList<Color> colors = new ArrayList<>(history);
		while (colors.size() < historySize)
			colors.add(0, initial);
		return colors;
	}
	
	protected List<Color> getShownHistory() {
		return getHistory().subList(historySize - shownHistorySize, historySize);
	}
	
	private void addToHistory(Color value) {
		while (history.size() >= historySize)
			history.remove(0);
		history.add(value);
	}
	
	protected void addToHistory() {
		Color value = this.value;
		if (historyMemory != null) {
			if (historyPreview >= 0 && historyPreview < history.size()) {
				// Promote the preview from its position to the top of the history
				final Color preview = getHistory().get(historyPreview);
				history.remove(historyPreview);
				while (history.size() >= historySize - 1)
					history.remove(0);
				// Add the not-yet-saved previous value before the promoted preview
				history.add(historyMemory);
				history.add(preview);
			} else {
				// Add the not-yet-saved previous value
				addToHistory(historyMemory);
				if (historyPreview != -3 && historyPreview != -2)
					addToHistory(value);
			}
			historyMemory = null;
		} else addToHistory(value);
	}
	
	public void allowAlpha(boolean alpha) {
		allowAlpha = alpha;
		if (alpha) {
			if (!subWidgets.contains(transparencyBar))
				subWidgets.add(transparencyBar);
		}
		if (!alpha) {
			updateValue(Color.ofOpaque(value.getColor()));
			subWidgets.remove(transparencyBar);
		}
	}
	
	protected void onChange() {
		if (onChange != null)
			onChange.accept(value);
	}
	
	@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height)
			return false;
		
		for (SubWidget subWidget : subWidgets) {
			if (subWidget.isMouseOver((int) round(mouseX), (int) round(mouseY))) {
				if (subWidget.onClick(mouseX, mouseY, button)) {
					lastClicked = subWidget;
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY,  button);
	}
	
	@Override public void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
		if (lastClicked != null)
			lastClicked.onDrag(mouseX, mouseY, dragX, dragY);
	}
	
	@Override public boolean changeFocus(boolean focus) {
		return false;
	}
	
	@Override public void updateNarration(@NotNull NarrationElementOutput out) {}
	
	public abstract static class SubWidget {
		public int x = 0;
		public int y = 0;
		public int w = 0;
		public int h = 0;
		
		public boolean isMouseOver(int mouseX, int mouseY) {
			return x <= mouseX && mouseX < x + w && y <= mouseY && mouseY < y + h;
		}
		
		public abstract void render(PoseStack mStack, int mX, int mY);
		public boolean onClick(double mouseX, double mouseY, int button) {
			return false;
		}
		public void onDrag(double mX, double mY, double dragX, double dragY) {}
	}
	
	public class BrightnessSaturationControl extends SubWidget {
		@Override public void render(PoseStack mStack, int mX, int mY) {
			drawBox(mStack, x - 1, y - 1, w + 2, h + 2, mX, mY);
			mStack.pushPose(); {
				mStack.translate(x, y, 0D);
				mStack.pushPose(); {
					mStack.mulPose(Vector3f.ZP.rotationDegrees(- 90F));
					fillGradient(mStack, -h, 0, 0, w, 0xFFFFFFFF,
					             Color.ofHSB(lastHue, 1F, 1F).getOpaque());
				} mStack.popPose();
				fillGradient(mStack, 0, 0, w, h, 0x00000000, 0xFF000000);
				int cX = (int) (lastSaturation * (w - 1)) - 5;
				int cY = (int) ((1F - value.getBrightness()) * (h - 1)) - 5;
				SimpleConfigIcons.ColorPicker.POINTER.renderCentered(mStack, cX, cY, 11, 11, isMouseOver(mX, mY)? 1 : 0);
			} mStack.popPose();
		}
		
		@Override public boolean onClick(double mouseX, double mouseY, int button) {
			if (button == 0) {
				addToHistory();
				if (lastHue != 1F || value.getHue() != 0F) lastHue = value.getHue();
				dragBrightnessSaturation(mouseX, mouseY);
				return true;
			}
			return false;
		}
		
		@Override public void onDrag(double mX, double mY, double dragX, double dragY) {
			dragBrightnessSaturation(mX, mY);
		}
		
		protected void dragBrightnessSaturation(double mouseX, double mouseY) {
			final float s = lastSaturation = Mth.clamp((float) ((mouseX - x) / (w - 1)), 0F, 1F);
			final float b = 1F - Mth.clamp((float) ((mouseY - y) / (h - 1)), 0F, 1F);
			updateValue(Color.ofHSBA(lastHue, s, b, value.getAlpha()));
		}
	}
	
	public class HueBar extends SubWidget {
		@Override public void render(PoseStack mStack, int mX, int mY) {
			drawBox(mStack, x - 1, y - 1, w + 2, h + 2, mX, mY);
			mStack.pushPose(); {
				mStack.translate(x, y, 0D);
				if (w > h) {
					mStack.mulPose(Vector3f.ZP.rotationDegrees(-90F));
					mStack.translate(-h, 0, 0D);
					w = w + h;
					h = w - h;
					w = w - h;
				}
				fillGradient(mStack, 0, 0, w, h / 6, 0xFFFF0000, 0xFFFFFF00);
				fillGradient(mStack, 0, h / 6, w, 2 * h / 6, 0xFFFFFF00, 0xFF00FF00);
				fillGradient(mStack, 0, 2 * h / 6, w, 3 * h / 6, 0xFF00FF00, 0xFF00FFFF);
				fillGradient(mStack, 0, 3 * h / 6, w, 4 * h / 6, 0xFF00FFFF, 0xFF0000FF);
				fillGradient(mStack, 0, 4 * h / 6, w, 5 * h / 6, 0xFF0000FF, 0xFFFF00FF);
				fillGradient(mStack, 0, 5 * h / 6, w, h, 0xFFFF00FF, 0xFFFF0000);
				
				int level = isMouseOver(mX, mY) ? 1 : 0;
				int aY = (int) (lastHue * (h - 1)) - 3;
				SimpleConfigIcons.ColorPicker.ARROW_RIGHT.renderCentered(mStack, -1, aY, 5, 7, level);
				SimpleConfigIcons.ColorPicker.ARROW_LEFT.renderCentered(mStack, w - 4, aY, 5, 7, level);
			} mStack.popPose();
		}
		
		@Override public boolean onClick(double mouseX, double mouseY, int button) {
			if (button == 0) {
				addToHistory();
				dragHue(mouseX, mouseY);
				return true;
			}
			return false;
		}
		
		@Override public void onDrag(double mX, double mY, double dragX, double dragY) {
			dragHue(mX, mY);
		}
		
		protected void dragHue(double mouseX, double mouseY) {
			final float h = lastHue = Mth.clamp((float) ((mouseY - y) / (this.h - 1)), 0F, 1F);
			updateValue(Color.ofHSBA(h, value.getSaturation(), value.getBrightness(), value.getAlpha()));
		}
	}
	
	public class TransparencyBar extends SubWidget {
		@Override public void render(PoseStack mStack, int mX, int mY) {
			drawBox(mStack, x - 1, y - 1, w + 2, h + 2, mX, mY);
			SimpleConfigIcons.ColorPicker.CHESS_BOARD.renderFill(mStack, x, y, w, h);
			mStack.pushPose(); {
				mStack.translate(x, y, 0D);
				if (w > h) {
					mStack.mulPose(Vector3f.ZP.rotationDegrees(90F));
					mStack.translate(0, -w, 0D);
					w = w + h;
					h = w - h;
					w = w - h;
				}
				fillGradient(mStack, 0, 0, w, h, value.getOpaque(), value.getColor() & 0x00FFFFFF);
				
				int level = isMouseOver(mX, mY)? 1 : 0;
				int aY = (int) (((255 - value.getAlpha()) / 255F) * (h - 1)) - 3;
				SimpleConfigIcons.ColorPicker.ARROW_RIGHT.renderCentered(mStack, -1, aY, 5, 7, level);
				SimpleConfigIcons.ColorPicker.ARROW_LEFT.renderCentered(mStack, w - 4, aY, 5, 7, level);
			} mStack.popPose();
		}
		
		@Override public boolean onClick(double mouseX, double mouseY, int button) {
			if (button == 0) {
				addToHistory();
				dragTransparency(mouseX, mouseY);
				return true;
			}
			return false;
		}
		
		@Override public void onDrag(double mX, double mY, double dragX, double dragY) {
			dragTransparency(mX, mY);
		}
		
		protected void dragTransparency(double mouseX, double mouseY) {
			final float a = 1F - Mth.clamp((float) ((mouseY - y) / (h - 1)), 0F, 1F);
			updateValue(Color.ofTransparent(value.getColor() & 0xFFFFFF | round(a * 255F) << 24));
		}
	}
	
	public class HistoryBar extends SubWidget {
		@Override public void render(PoseStack mStack, int mX, int mY) {
			final boolean hovered = drawBox(mStack, x - 1, y - 1, w + 2, h + 2, mX, mY);
			SimpleConfigIcons.ColorPicker.CHESS_BOARD.renderFill(mStack, x, y, w, h);
			mStack.pushPose(); {
				mStack.translate(x, y, 0D);
				if (w < h) {
					mStack.mulPose(Vector3f.ZP.rotationDegrees(90F));
					mStack.translate(0, w, 0D);
				}
				shownHistorySize = min(historySize, max(0, (w / h) - 2));
				int n = shownHistorySize + 2;
				final List<Color> history = getShownHistory();
				Function<Color, Integer> toInt = allowAlpha ? Color::getColor : Color::getOpaque;
				for (int i = 0; i < shownHistorySize; i++)
					fill(mStack, i * w / n, 0, (i + 1) * w / n, h, toInt.apply(history.get(i)));
				fill(mStack, (n - 2) * w / n, 0, (n - 1) * w / n, h, toInt.apply(historyMemory != null? historyMemory : value));
				fill(mStack, (n - 1) * w / n, 0, w, h, toInt.apply(initial));
				fill(mStack, (n - 2) * w / n, 0, (n - 2) * w / n + 1, h, hovered? colorBorderHover : colorBorder);
				fill(mStack, (n - 1) * w / n, 0, (n - 1) * w / n + 1, h, hovered? colorBorderHover : colorBorder);
			} mStack.popPose();
		}
		
		@Override public boolean onClick(double mouseX, double mouseY, int button) {
			if (button == 0) {
				final int index = hoveredHistoryIndex((int) round(mouseX), (int) round(mouseY));
				if (index == -3) {
					if (historyMemory == null)
						historyMemory = value;
					historyPreview = -3;
					setValue(initial);
					return true;
				} else if (index == -2 && historyMemory != null) {
					historyPreview = -2;
					setValue(historyMemory);
				} else if (index >= 0 && index < shownHistorySize) {
					historyPreview = historySize - shownHistorySize + index;
					if (historyMemory == null)
						historyMemory = value;
					setValue(getShownHistory().get(index));
					return true;
				}
			}
			return false;
		}
		
		protected int hoveredHistoryIndex(int mouseX, int mouseY) {
			if (!isMouseOver(mouseX, mouseY))
				return -1;
			final int rX = mouseX - x;
			final int n = shownHistorySize + 2;
			for (int i = 0; i < shownHistorySize; i++) {
				if (rX >= i * w / n && rX < (i + 1) * w / n)
					return i;
			}
			if (rX >= (n - 2) * w / n && rX < (n - 1) * w / n)
				return -2;
			if (rX >= (n - 1) * w / n && rX < n * w / n)
				return -3;
			return -1;
		}
	}
	
	public class Palette extends SubWidget {
		@Override public void render(PoseStack mStack, int mX, int mY) {
			fill(mStack, x, y, x + 1, y + h, colorBorder);
			final Map<Integer, java.awt.Color> saved_colors = advanced.color_picker_saved_colors;
			paletteRows = (h + margin) / (unit + margin);
			for (int i = 0; i < paletteRows; i++) {
				int rY = y + i * (unit + margin);
				drawPaletteEntry(saved_colors.get(2 * i), mStack, x + margin + 1, rY, mX, mY);
				drawPaletteEntry(saved_colors.get(2 * i + 1), mStack, x + 2 * margin + unit + 1, rY, mX, mY);
			}
		}
		
		protected void drawPaletteEntry(java.awt.Color color, PoseStack mStack, int x, int y, int mX, int mY) {
			drawBox(mStack, x - 1, y - 1, unit + 2, unit + 2, mX, mY);
			if (color == null) {
				SimpleConfigIcons.ColorPicker.DIAGONAL_TEXTURE.renderFill(mStack, x, y, unit, unit);
			} else {
				SimpleConfigIcons.ColorPicker.CHESS_BOARD.renderFill(mStack, x, y, unit, unit);
				fill(mStack, x, y, x + unit, y + unit, color.getRGB());
			}
		}
		
		@Override public boolean onClick(double mouseX, double mouseY, int button) {
			final int index = hoveredPaletteEntry((int) round(mouseX), (int) round(mouseY));
			if (index >= 0) {
				if (button == 0) {
					final Color c = getColorFromPalette(index);
					if (c != null) {
						addToHistory();
						setValue(c);
					}
					return true;
				} else if (button == 1) {
					saveColorToPalette(index, value);
					return true;
				} else if (button == 2) {
					saveColorToPalette(index,null);
					return true;
				}
			}
			return false;
		}
		
		protected int hoveredPaletteEntry(int mouseX, int mouseY) {
			final int rX = mouseX - x - margin - 1;
			if (rX < 0 || rX % (unit + margin) > unit)
				return -1;
			int j = rX < unit + margin ? 0 : 1;
			final int rY = mouseY - y;
			if (rY % (unit + margin) > unit)
				return -1;
			int i = rY / (unit + margin);
			return 2 * i + j;
		}
		
		protected Color getColorFromPalette(int index) {
			final java.awt.Color c = advanced.color_picker_saved_colors.get(index);
			return c != null ? Color.ofTransparent(c.getRGB()) : null;
		}
		
		protected void saveColorToPalette(int index, Color color) {
			if (color == null) {
				advanced.color_picker_saved_colors.remove(index);
			} else {
				final java.awt.Color c =
				  new java.awt.Color(allowAlpha ? color.getColor() : color.getOpaque(), true);
				advanced.color_picker_saved_colors.put(index, c);
			}
			SimpleConfigMod.CLIENT_CONFIG.set("advanced.color_picker_saved_colors", advanced.color_picker_saved_colors);
		}
	}
}
