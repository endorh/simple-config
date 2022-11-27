package endorh.simpleconfig.ui.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import endorh.simpleconfig.api.ui.TextFormatter;
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
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static endorh.simpleconfig.api.SimpleConfigTextUtil.subText;
import static java.lang.Math.max;
import static java.lang.Math.min;

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
	private @Nullable Function<String, Optional<ITextComponent>> hintProvider;
	protected Consumer<String> responder;
	protected Predicate<String> filter = Objects::nonNull;
	protected TextFormatter formatter = TextFormatter.DEFAULT;
	
	public static TextFieldWidgetEx of(String text) {
		TextFieldWidgetEx tf = new TextFieldWidgetEx(
		  Minecraft.getInstance().fontRenderer, 0, 0, 0, 0, StringTextComponent.EMPTY);
		tf.setText(text);
		return tf;
	}
	
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
	
	public void setFormatter(TextFormatter formatter) {
		this.formatter = formatter;
	}
	
	@Override public void setWidth(int width) {
		boolean change = width != this.width;
		super.setWidth(width);
		if (change) scrollToFitCaret();
	}
	
	public void tick() {}
	
	@Override protected @NotNull IFormattableTextComponent getNarrationMessage() {
		ITextComponent itextcomponent = getMessage();
		return new TranslationTextComponent("gui.narrate.editBox", itextcomponent, text);
	}
	
	public void setText(String text) {
		if (filter.test(text)) {
			if (text.length() > maxLength) {
				this.text = text.substring(0, maxLength);
			} else this.text = text;
			
			moveCaretToStart();
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
		String txt = SharedConstants.filterAllowedCharacters(inserted);
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
		nextNarration = Util.milliTime() + 500L;
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
	  "|(?<=[^\\p{Alnum}\\s_])(?=[\\p{Alnum}\\s_])");
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
	  "|(?<=[^\\p{Alnum}\\s_])(?=[\\p{Alnum}\\s_])");
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
		return Util.func_240980_a_(text, caretPos, relativePos);
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
		scrollToFitCaret();
	}
	
	public void moveCaretToStart() {
		moveCaret(0);
	}
	
	public void moveCaretToEnd() {
		moveCaret(text.length());
	}
	
	@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (!canConsumeInput()) {
			return false;
		} else {
			lastInteraction = System.currentTimeMillis();
			if (Screen.isSelectAll(keyCode)) {
				moveCaretToEnd();
				setAnchorPos(0);
				return true;
			} else {
				KeyboardListener kl = Minecraft.getInstance().keyboardListener;
				if (Screen.isCopy(keyCode)) {
					kl.setClipboardString(getSelectedText());
					return true;
				} else if (Screen.isPaste(keyCode)) {
					if (isEditable()) insertText(kl.getClipboardString());
					return true;
				} else if (Screen.isCut(keyCode)) {
					kl.setClipboardString(getSelectedText());
					if (isEditable()) insertText("");
					return true;
				} else {
					switch(keyCode) {
						case GLFW.GLFW_KEY_BACKSPACE:
							if (isEditable()) delete(-1);
							return true;
						case GLFW.GLFW_KEY_DELETE:
							if (isEditable()) delete(1);
							return true;
						case GLFW.GLFW_KEY_RIGHT:
							if (hasSelection() && !Screen.hasShiftDown()) {
								moveCaretWithAnchor(max(anchorPos, caretPos));
							} else if (Screen.hasControlDown()) {
								moveCaret(getWordPosFromCaret(1));
							} else moveCaretBy(1);
							return true;
						case GLFW.GLFW_KEY_LEFT:
							if (hasSelection() && !Screen.hasShiftDown()) {
								moveCaretWithAnchor(min(anchorPos, caretPos));
							} else if (Screen.hasControlDown()) {
								moveCaret(getWordPosFromCaret(-1));
							} else moveCaretBy(-1);
							return true;
						case GLFW.GLFW_KEY_HOME:
							moveCaretToStart();
							return true;
						case GLFW.GLFW_KEY_END:
							moveCaretToEnd();
							return true;
						case GLFW.GLFW_KEY_INSERT:
						case GLFW.GLFW_KEY_ENTER:
						case GLFW.GLFW_KEY_DOWN:
						case GLFW.GLFW_KEY_UP:
						case GLFW.GLFW_KEY_PAGE_UP:
						case GLFW.GLFW_KEY_PAGE_DOWN:
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
	
	@Override public boolean charTyped(char codePoint, int modifiers) {
		if (!canConsumeInput()) {
			return false;
		} else if (SharedConstants.isAllowedCharacter(codePoint)) {
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
	
	@Override public void setFocused(boolean focused) {
		super.setFocused(focused);
	}
	
	@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
		draggingText = false;
		if (isVisible()) {
			boolean hovered = isMouseOver(mouseX, mouseY);
			if (isCanLoseFocus()) setFocused(hovered);
			if (isFocused() && hovered && button == 0) {
				lastClickWordPos = -1;
				draggingText = true;
				double relX = mouseX - x;
				if (isBordered()) relX -= 4;
				int clickedPos = getClickedCaretPos(subText(getDisplayedText(), hScroll), relX) + hScroll;
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
			int draggedPos = getClickedCaretPos(subText(getDisplayedText(), hScroll), relX) + hScroll;
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
		int floor = font.func_238417_a_(line, (int) relX).getString().length();
		if (floor >= lineLength) return lineLength;
		int left = font.getStringPropertyWidth(subText(line, 0, floor));
		int right = font.getStringPropertyWidth(subText(line, 0, floor + 1));
		return relX < (left + right) * 0.5? floor: floor + 1;
	}
	
	@Override public void renderButton(@NotNull MatrixStack mStack, int mouseX, int mouseY, float delta) {
		if (isVisible()) {
			boolean bordered = isBordered();
			if (bordered) {
				int borderColor = isHovered()? 0xFF000000 | this.borderColor & 0xFFFFFF
				                             : 0xA0000000 | this.borderColor & 0xFFFFFF;
				fill(mStack, x - 1, y - 1, x + width + 1, y + height + 1, borderColor);
				fill(mStack, x, y, x + width, y + height, 0xFF000000);
			}
			
			int color = isEditable() ? textColor : textColorUneditable;
			int relCaret = caretPos - hScroll;
			int relAnchor = anchorPos - hScroll;
			int innerWidth = getInnerWidth();
			
			IFormattableTextComponent displayedText = subText(getDisplayedText(), hScroll);
			String shown = font.func_238417_a_(displayedText, innerWidth).getString();
			int fitLength = shown.length();
			displayedText = subText(displayedText, 0, fitLength);
			
			boolean fitCaret = relCaret >= 0 && relCaret <= fitLength;
			boolean showCaret = isFocused() && fitCaret
			                    && (System.currentTimeMillis() - lastInteraction) % 1000 < 500;
			int textX = bordered ? x + 4 : x;
			int textY = bordered ? y + (height - 8) / 2 : y;
			int caretX = fitCaret? textX + font.getStringPropertyWidth(subText(displayedText, 0, relCaret)) - 1
			             : relCaret > 0? textX + innerWidth - 1 : textX;
			
			// Render text
			if (!shown.isEmpty())
				font.func_243246_a(mStack, displayedText, textX, textY, color);
			
			// Render hint
			ITextComponent hint = hintProvider != null? hintProvider.apply(text).orElse(null) : null;
			if (relCaret == shown.length() && hint != null)
				font.func_243246_a(mStack, hint, caretX, textY, 0xFF808080);
			
			// Render caret
			if (showCaret) {
				renderCaret(mStack, caretX, textY - 2, 1, 12);
			}
			
			// Render selection
			if (relAnchor != relCaret && isFocused()) {
				if (relAnchor > fitLength) relAnchor = fitLength;
				if (relAnchor < 0) relAnchor = 0;
				int aX = textX + font.getStringPropertyWidth(subText(displayedText, 0, relAnchor)) - 1;
				renderSelection(mStack, caretX, textY - 3, aX, textY + 2 + 9);
			}
		}
	}
	
	protected void renderCaret(MatrixStack mStack, int x, int y, int w, int h) {
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bb = tessellator.getBuffer();
		RenderSystem.color4f(1F, 1F, 1F, 1F);
		RenderSystem.disableTexture();
		RenderSystem.enableColorLogicOp();
		RenderSystem.logicOp(GlStateManager.LogicOp.OR_REVERSE);
		Matrix4f m = mStack.getLast().getMatrix();
		bb.begin(7, DefaultVertexFormats.POSITION);
		bb.pos(m,     x, y + h, 0F).endVertex();
		bb.pos(m, x + w, y + h, 0F).endVertex();
		bb.pos(m, x + w,     y, 0F).endVertex();
		bb.pos(m,     x,     y, 0F).endVertex();
		tessellator.draw();
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
		BufferBuilder bb = tessellator.getBuffer();
		Matrix4f m = mStack.getLast().getMatrix();
		RenderSystem.color4f(0F, 0F, 1F, 1F);
		RenderSystem.disableTexture();
		RenderSystem.enableColorLogicOp();
		RenderSystem.logicOp(GlStateManager.LogicOp.OR_REVERSE);
		bb.begin(7, DefaultVertexFormats.POSITION);
		bb.pos(m, sX, eY, 0F).endVertex();
		bb.pos(m, eX, eY, 0F).endVertex();
		bb.pos(m, eX, sY, 0F).endVertex();
		bb.pos(m, sX, sY, 0F).endVertex();
		tessellator.draw();
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
	
	@Override public boolean changeFocus(boolean focus) {
		lastInteraction = System.currentTimeMillis();
		return visible && isEditable() && super.changeFocus(focus);
	}
	
	@Override public boolean isMouseOver(double mouseX, double mouseY) {
		return visible && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}
	
	@Override protected void onFocusedChanged(boolean focused) {
	
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
	
	public int getMaxHScroll() {
		String text = this.text;
		String reversed = new StringBuilder(text).reverse().toString();
		return text.length() - font.func_238412_a_(reversed, getInnerWidth()).length();
	}
	
	public void scrollToFit(int pos) {
		int maxHScroll = getMaxHScroll();
		if (font != null) {
			if (hScroll > maxHScroll) hScroll = maxHScroll;
			
			int w = getInnerWidth();
			String shown = font.func_238412_a_(text.substring(hScroll), w);
			int lastShown = shown.length() + hScroll;
			
			if (pos > lastShown) {
				hScroll += pos - lastShown + 1;
			} else if (pos <= hScroll) {
				hScroll = pos - 1;
			}
			
			hScroll = MathHelper.clamp(hScroll, 0, maxHScroll);
		}
	}
	
	public void scrollToFitCaret() {
		scrollToFit(caretPos);
	}
	
	public void setAnchorPos(int pos) {
		int len = text.length();
		anchorPos = MathHelper.clamp(pos, 0, len);
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
	
	public void setPlainHint(@Nullable String hint) {
		setHint(hint != null? new StringTextComponent(hint) : null);
	}
	
	public void setPlainHint(@Nullable Function<String, Optional<String>> hintProvider) {
		setHint(hintProvider != null? s -> hintProvider.apply(s).map(StringTextComponent::new) : null);
	}
	
	public void setHint(@Nullable ITextComponent hint) {
		setHint(hint != null? s -> Optional.of(hint) : null);
	}
	
	public void setHint(@Nullable Function<String, Optional<ITextComponent>> hintProvider) {
		this.hintProvider = hintProvider;
	}
	
	public void setEmptyHint(String hint) {
		setEmptyHint(new StringTextComponent(hint));
	}
	
	public void setEmptyHint(ITextComponent hint) {
		setHint(s -> s.isEmpty()? Optional.of(hint) : Optional.empty());
	}
	
	public int getTextXForPos(int pos) {
		return pos > text.length() ? x : x + font.getStringWidth(text.substring(0, pos));
	}
}