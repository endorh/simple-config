package endorh.simpleconfig.clothconfig2.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import endorh.simpleconfig.clothconfig2.api.ITextFormatter;
import net.minecraft.client.KeyboardListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static endorh.simpleconfig.core.SimpleConfigTextUtil.subText;
import static java.lang.Math.*;

/**
 * Improved text field widget.
 */
@OnlyIn(Dist.CLIENT)
public class TextFieldWidgetEx extends Widget {
	protected final FontRenderer font;
	protected String text = "";
	protected long lastClick;
	protected long lastInteraction = 0;
	protected boolean draggingText;
	protected int maxLength = 32;
	private boolean bordered = true;
	private boolean canLoseFocus = true;
	private boolean isEditable = true;
	protected int hScroll;
	protected int caretPos;
	protected int anchorPos;
	protected int lastClickWordPos = -1;
	private int borderColor = 0xFFFFFF;
	private int textColor = 0xFFE0E0E0;
	private int textColorUneditable = 0xFF707070;
	protected String hint;
	protected Consumer<String> responder;
	protected Predicate<String> filter = Objects::nonNull;
	protected ITextFormatter formatter = ITextFormatter.DEFAULT;
	
	public TextFieldWidgetEx(
	  FontRenderer font, int x, int y, int w, int h, ITextComponent title
	) {
		this(font, x, y, w, h, null, title);
	}
	
	public TextFieldWidgetEx(
	  FontRenderer font, int x, int y, int w, int h,
	  @Nullable TextFieldWidgetEx copy, ITextComponent title
	) {
		super(x, y, w, h, title);
		this.font = font;
	}
	
	public void setResponder(Consumer<String> responder) {
		this.responder = responder;
	}
	
	public void setFormatter(ITextFormatter formatter) {
		this.formatter = formatter;
	}
	
	public void tick() {}
	
	protected @NotNull IFormattableTextComponent createNarrationMessage() {
		ITextComponent itextcomponent = getMessage();
		return new TranslationTextComponent("gui.narrate.editBox", itextcomponent, text);
	}
	
	public void setText(String text) {
		if (filter.test(text)) {
			if (text.length() > maxLength) {
				this.text = text.substring(0, maxLength);
			} else this.text = text;
			
			moveCaretToEnd();
			setAnchorPos(caretPos);
			onTextChange(text);
		}
	}
	
	public String getText() {
		return text;
	}
	
	public IFormattableTextComponent getDisplayedText() {
		return formatter.formatText(text);
	}
	
	public boolean hasSelection() {
		return anchorPos != caretPos;
	}
	
	public String getSelectedText() {
		return caretPos < anchorPos
		       ? text.substring(caretPos, anchorPos)
		       : text.substring(anchorPos, caretPos);
	}
	
	public void setFilter(Predicate<String> filter) {
		this.filter = filter;
	}
	
	public void insertText(String inserted) {
		if (formatter != null) inserted = formatter.stripInsertText(inserted);
		
		int start = min(caretPos, anchorPos);
		int end = max(caretPos, anchorPos);
		int allowed = maxLength - text.length() - (start - end);
		String txt = SharedConstants.filterText(inserted);
		int length = txt.length();
		if (allowed < length) {
			txt = txt.substring(0, allowed);
			length = allowed;
		}
		
		String result = (new StringBuilder(text)).replace(start, end, txt).toString();
		if (filter.test(result)) {
			text = result;
			setCaretPosition(start + length);
			setAnchorPos(caretPos);
			onTextChange(text);
		}
	}
	
	private void onTextChange(String newText) {
		if (responder != null) responder.accept(newText);
		nextNarration = Util.getMillis() + 500L;
	}
	
	private void delete(int words) {
		if (hasSelection()) {
			insertText("");
		} else if (Screen.hasControlDown()) {
			deleteWords(words);
		} else {
			deleteFromCaret(words);
		}
	}
	
	public void deleteWords(int words) {
		if (!text.isEmpty()) {
			if (hasSelection()) {
				insertText("");
			} else {
				deleteFromCaret(getWordPosFromCaret(words) - caretPos);
			}
		}
	}
	
	public void deleteFromCaret(int chars) {
		if (!text.isEmpty()) {
			if (hasSelection()) {
				insertText("");
			} else {
				int i = expandLigaturesFromCaret(chars);
				int start = min(i, caretPos);
				int stop = max(i, caretPos);
				if (start != stop) {
					String text = getText();
					if (formatter != null && chars == -1 && stop - start == 1 && stop < text.length()) {
						String context = new StringBuilder(text).delete(start, stop + 1).toString();
						String closingPair = formatter.closingPair(text.charAt(start), context, start);
						if (closingPair != null && text.substring(stop).startsWith(closingPair))
							stop = stop + closingPair.length();
					}
					String s = new StringBuilder(text).delete(start, stop).toString();
					
					if (filter.test(s)) {
						this.text = s;
						moveCaretWithAnchor(start);
					}
				}
			}
		}
	}
	
	public int getWordPosFromCaret(int numWords) {
		return getWordPosFromPos(numWords, getCaret());
	}
	
	// Detects the following word breaks:
	//   word| end|
	//   case|Change|
	//   snake|_case|
	//   ACRONYM|Pascal|Case
	//   symbols|>>|end|
	// where numbers are treated as word characters
	private static final Pattern WORD_BREAK_RIGHT_PATTERN = Pattern.compile(
	  "(?<=\\p{Alnum})(?=\\P{Alnum})" +
	  "|(?<=\\p{Alnum})(?=\\p{Lu}[\\p{Ll}\\d])" +
	  "|(?<=[\\p{Ll}\\d])(?=\\p{Lu})" +
	  "|(?<=[\\P{Alnum}&&\\S&&[^_]])(?=[\\p{Alnum}\\s_])");
	// Must be applied on the reverse string
	// Detects the following word breaks:
	//   |word |end
	//   |case|Change
	//   |snake_|case
	//   |ACRONYM|Pascal|Case
	//   |symbols|<<|end
	// where numbers are treated as word characters
	private static final Pattern WORD_BREAK_LEFT_PATTERN = Pattern.compile(
	  "(?<=\\p{Alnum})(?=\\P{Alnum})" +
	  "|(?<=[\\p{Ll}\\d]\\p{Lu})(?=\\p{Lu})" +
	  "|(?<=\\p{Lu})(?=[\\p{Ll}\\d])" +
	  "|(?<=[\\P{Alnum}&&\\S&&[^_]])(?=[\\p{Alnum}\\s_])");
	public int getWordPosFromPos(int wordStep, int pos) {
		if (wordStep == 0) return pos;
		String text = getText();
		int length = text.length();
		boolean reverse = wordStep < 0;
		if (reverse) {
			text = new StringBuilder(text).reverse().toString();
			wordStep = -wordStep;
			pos = length - pos;
		}
		Matcher m = (reverse? WORD_BREAK_LEFT_PATTERN : WORD_BREAK_RIGHT_PATTERN).matcher(text);
		int r = -1;
		while (wordStep > 0 && m.find()) {
			if (m.end() > pos) {
				wordStep--;
				r = m.end();
			}
		}
		if (wordStep > 0) r = length;
		if (reverse) r = length - r;
		return r;
	}
	
	public void moveCaretBy(int relative) {
		moveCaret(expandLigaturesFromCaret(relative));
	}
	
	private int expandLigaturesFromCaret(int relativePos) {
		return Util.offsetByCodepoints(text, caretPos, relativePos);
	}
	
	public void moveCaret(int pos) {
		setCaretPosition(pos);
		if (!Screen.hasShiftDown()) setAnchorPos(caretPos);
		onTextChange(text);
	}
	
	public void moveCaretWithAnchor(int pos) {
		moveCaret(pos);
		setAnchorPos(pos);
	}
	
	public void setCaretPosition(int pos) {
		caretPos = MathHelper.clamp(pos, 0, text.length());
	}
	
	public void moveCaretToStart() {
		moveCaret(0);
	}
	
	public void moveCaretToEnd() {
		moveCaret(text.length());
	}
	
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (!canConsumeInput()) {
			return false;
		} else {
			lastInteraction = System.currentTimeMillis();
			if (Screen.isSelectAll(keyCode)) {
				moveCaretToEnd();
				setAnchorPos(0);
				return true;
			} else {
				KeyboardListener kl = Minecraft.getInstance().keyboardHandler;
				if (Screen.isCopy(keyCode)) {
					kl.setClipboard(getSelectedText());
					return true;
				} else if (Screen.isPaste(keyCode)) {
					if (isEditable()) insertText(kl.getClipboard());
					return true;
				} else if (Screen.isCut(keyCode)) {
					kl.setClipboard(getSelectedText());
					if (isEditable()) insertText("");
					return true;
				} else {
					switch(keyCode) {
						case 259: // Backspace
							if (isEditable()) delete(-1);
							return true;
						case 261: // Delete
							if (isEditable()) delete(1);
							return true;
						case 262: // Right
							if (hasSelection() && !Screen.hasShiftDown()) {
								moveCaretWithAnchor(max(anchorPos, caretPos));
							} else if (Screen.hasControlDown()) {
								moveCaret(getWordPosFromCaret(1));
							} else moveCaretBy(1);
							return true;
						case 263: // Left
							if (hasSelection() && !Screen.hasShiftDown()) {
								moveCaretWithAnchor(min(anchorPos, caretPos));
							} else if (Screen.hasControlDown()) {
								moveCaret(getWordPosFromCaret(-1));
							} else moveCaretBy(-1);
							return true;
						case 268: // Home
							moveCaretToStart();
							return true;
						case 269: // End
							moveCaretToEnd();
							return true;
						case 260: // Insert
						case 257: // Enter
						case 264: // Down
						case 265: // Up
						case 266: // Page Up
						case 267: // Page Down
						default:
							return false;
					}
				}
			}
		}
	}
	
	public boolean canConsumeInput() {
		return isVisible() && isFocused() && isEditable();
	}
	
	public boolean charTyped(char codePoint, int modifiers) {
		if (!canConsumeInput()) {
			return false;
		} else if (SharedConstants.isAllowedChatCharacter(codePoint)) {
			if (isEditable()) {
				String closingPair = null;
				if (formatter != null) {
					int caret = getCaret();
					String text = getText();
					if (caret < text.length() && text.charAt(caret) == codePoint
					    && formatter.shouldSkipClosingPair(codePoint, text, caret)) {
						moveCaretWithAnchor(caret + 1);
						return true;
					}
					closingPair = formatter.closingPair(codePoint, text, caret);
				}
				insertText(Character.toString(codePoint));
				if (closingPair != null && !closingPair.isEmpty()) {
					int caret = getCaret();
					insertText(closingPair);
					moveCaretWithAnchor(caret);
				}
			}
			return true;
		} else {
			return false;
		}
	}
	
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		draggingText = false;
		if (isVisible()) {
			boolean hovered = isMouseOver(mouseX, mouseY);
			if (isCanLoseFocus()) setFocused(hovered);
			if (isFocused() && hovered && button == 0) {
				lastClickWordPos = -1;
				draggingText = true;
				double relX = mouseX - x;
				if (isBordered()) relX -= 4;
				int clickedPos = getClickedCaretPos(subText(getDisplayedText(), hScroll), relX);
				lastInteraction = System.currentTimeMillis();
				if (lastInteraction - lastClick < 250) { // Double click
					int left = getWordPosFromPos(-1, clickedPos);
					int right = getWordPosFromPos(1, clickedPos);
					if (anchorPos == left && caretPos == right) { // Select line
						moveCaretToEnd();
						setAnchorPos(0);
						draggingText = false;
					} else { // Select word
						moveCaret(right);
						setAnchorPos(left);
						lastClickWordPos = clickedPos;
					}
				} else { // Move caret
					moveCaret(clickedPos);
					setAnchorPos(caretPos);
				}
				lastClick = lastInteraction;
				return true;
			}
		}
		return false;
	}
	
	@Override public boolean mouseDragged(
	  double mouseX, double mouseY, int button, double dragX, double dragY
	) {
		if (isVisible() && isFocused() && button == 0 && draggingText) {
			lastInteraction = System.currentTimeMillis();
			double relX = mouseX - x;
			if (isBordered()) relX -= 4;
			int prevAnchor = anchorPos;
			int draggedPos = getClickedCaretPos(subText(getDisplayedText(), hScroll), relX);
			if (lastClickWordPos != -1) {
				int left = getWordPosFromPos(-1, lastClickWordPos);
				int right = getWordPosFromPos(1, lastClickWordPos);
				if (draggedPos < left) {
					moveCaret(getWordPosFromPos(-1, draggedPos));
					setAnchorPos(right);
				} else if (draggedPos > right) {
					moveCaret(getWordPosFromPos(1, draggedPos));
					setAnchorPos(left);
				} else {
					boolean r = draggedPos > (left + right) / 2;
					moveCaret(r? right : left);
					setAnchorPos(r? left : right);
				}
			} else {
				moveCaret(draggedPos);
				setAnchorPos(prevAnchor);
			}
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}
	
	protected int getClickedCaretPos(IFormattableTextComponent line, double relX) {
		int lineLength = line.getString().length();
		int floor = font.substrByWidth(line, (int) relX).getString().length();
		if (floor >= lineLength) return lineLength;
		int left = font.width(subText(line, 0, floor));
		int right = font.width(subText(line, 0, floor + 1));
		return relX < (left + right) * 0.5? floor: floor + 1;
	}
	
	public void renderButton(@NotNull MatrixStack mStack, int mouseX, int mouseY, float delta) {
		if (isVisible()) {
			boolean bordered = isBordered();
			if (bordered) {
				int borderColor = isFocused() ? 0xFF000000 | this.borderColor & 0xFFFFFF
				                              : 0xA0000000 | this.borderColor & 0xFFFFFF;
				fill(mStack, x - 1, y - 1, x + width + 1, y + height + 1, borderColor);
				fill(mStack, x, y, x + width, y + height, 0xFF000000);
			}
			
			int color = isEditable() ? textColor : textColorUneditable;
			int relCaret = caretPos - hScroll;
			int relAnchor = anchorPos - hScroll;
			int innerWidth = getInnerWidth();
			
			IFormattableTextComponent displayedText = subText(getDisplayedText(), hScroll);
			String shown = font.substrByWidth(displayedText, innerWidth).getString();
			int fitLength = shown.length();
			displayedText = subText(displayedText, 0, fitLength);
			
			boolean fitCaret = relCaret >= 0 && relCaret <= fitLength;
			boolean showCaret = isFocused() && fitCaret
			                    && (System.currentTimeMillis() - lastInteraction) % 1000 < 500;
			int textX = bordered ? x + 4 : x;
			int textY = bordered ? y + (height - 8) / 2 : y;
			int caretX = fitCaret? textX + font.width(subText(displayedText, 0, relCaret)) - 1
			             : relCaret > 0? textX + innerWidth - 1 : textX;
			
			// Render text
			if (!shown.isEmpty())
				font.drawShadow(mStack, displayedText, textX, textY, color);
			
			// Render hint
			if (relCaret == shown.length() && hint != null)
				font.drawShadow(mStack, hint, caretX - 1, textY, 0xFF808080);
			
			// Render caret
			if (showCaret) {
				renderCaret(mStack, caretX, textY - 2, 1, 12);
			}
			
			// Render selection
			if (relAnchor != relCaret && isFocused()) {
				if (relAnchor > fitLength) relAnchor = fitLength;
				int aX = textX + font.width(subText(displayedText, 0, relAnchor)) - 1;
				renderSelection(mStack, caretX, textY - 3, aX, textY + 2 + 9);
			}
		}
	}
	
	protected void renderCaret(MatrixStack mStack, int x, int y, int w, int h) {
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bb = tessellator.getBuilder();
		RenderSystem.color4f(1F, 1F, 1F, 1F);
		RenderSystem.disableTexture();
		RenderSystem.enableColorLogicOp();
		RenderSystem.logicOp(GlStateManager.LogicOp.OR_REVERSE);
		Matrix4f m = mStack.last().pose();
		bb.begin(7, DefaultVertexFormats.POSITION);
		bb.vertex(m,     x, y + h, 0F).endVertex();
		bb.vertex(m, x + w, y + h, 0F).endVertex();
		bb.vertex(m, x + w,     y, 0F).endVertex();
		bb.vertex(m,     x,     y, 0F).endVertex();
		tessellator.end();
		RenderSystem.disableColorLogicOp();
		RenderSystem.enableTexture();
	}
	
	protected void renderSelection(MatrixStack mStack, int sX, int sY, int eX, int eY) {
		if (sX < eX) {
			int swap = sX;
			sX = eX;
			eX = swap;
		}
		if (sY < eY) {
			int swap = sY;
			sY = eY;
			eY = swap;
		}
		
		if (eX > x + width) eX = x + width;
		if (sX > x + width) sX = x + width;
		
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bb = tessellator.getBuilder();
		Matrix4f m = mStack.last().pose();
		RenderSystem.color4f(0F, 0F, 1F, 1F);
		RenderSystem.disableTexture();
		RenderSystem.enableColorLogicOp();
		RenderSystem.logicOp(GlStateManager.LogicOp.OR_REVERSE);
		bb.begin(7, DefaultVertexFormats.POSITION);
		bb.vertex(m, sX, eY, 0F).endVertex();
		bb.vertex(m, eX, eY, 0F).endVertex();
		bb.vertex(m, eX, sY, 0F).endVertex();
		bb.vertex(m, sX, sY, 0F).endVertex();
		tessellator.end();
		RenderSystem.disableColorLogicOp();
		RenderSystem.enableTexture();
		// Do not leak the blue filter
		RenderSystem.color4f(1F, 1F, 1F, 1F);
	}
	
	public void setMaxLength(int length) {
		maxLength = length;
		if (text.length() > length) {
			text = text.substring(0, length);
			onTextChange(text);
		}
	}
	
	private int getMaxLength() {
		return maxLength;
	}
	
	public int getCaret() {
		return caretPos;
	}
	
	private boolean isBordered() {
		return bordered;
	}
	
	public void setBordered(boolean bordered) {
		this.bordered = bordered;
	}
	
	public void setTextColor(int color) {
		textColor = color;
	}
	
	public void setTextColorUneditable(int color) {
		textColorUneditable = color;
	}
	
	public void setBorderColor(int borderColor) {
		this.borderColor = borderColor;
	}
	
	public boolean changeFocus(boolean focus) {
		lastInteraction = System.currentTimeMillis();
		return visible && isEditable() && super.changeFocus(focus);
	}
	
	public boolean isMouseOver(double mouseX, double mouseY) {
		return visible && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}
	
	protected void onFocusedChanged(boolean focused) {
	
	}
	
	public boolean isEditable() {
		return isEditable;
	}
	
	public void setEditable(boolean editable) {
		isEditable = editable;
	}
	
	public int getInnerWidth() {
		return isBordered() ? width - 8 : width;
	}
	
	public void setAnchorPos(int pos) {
		int len = text.length();
		anchorPos = MathHelper.clamp(pos, 0, len);
		if (font != null) {
			if (hScroll > len) {
				hScroll = len;
			}
			
			int w = getInnerWidth();
			String shown = font.plainSubstrByWidth(text.substring(hScroll), w);
			int lastShown = shown.length() + hScroll;
			if (anchorPos == hScroll) {
				hScroll -= font.plainSubstrByWidth(text, w, true).length();
			}
			
			if (anchorPos > lastShown) {
				hScroll += anchorPos - lastShown;
			} else if (anchorPos <= hScroll) {
				hScroll -= hScroll - anchorPos;
			}
			
			hScroll = MathHelper.clamp(hScroll, 0, len);
		}
		
	}
	
	public boolean isCanLoseFocus() {
		return canLoseFocus;
	}
	
	public void setCanLoseFocus(boolean canLoseFocus) {
		this.canLoseFocus = canLoseFocus;
	}
	
	public boolean isVisible() {
		return visible;
	}
	
	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	
	public void setHint(@Nullable String hint) {
		this.hint = hint;
	}
	
	public int getTextXForPos(int pos) {
		return pos > text.length() ? x : x + font.width(text.substring(0, pos));
	}
}