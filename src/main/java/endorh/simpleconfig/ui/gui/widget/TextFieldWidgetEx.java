package endorh.simpleconfig.ui.gui.widget;

import com.mojang.blaze3d.platform.GlStateManager.LogicOp;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import endorh.simpleconfig.api.ui.TextFormatter;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
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
public class TextFieldWidgetEx extends AbstractWidget {
	protected final Font font;
	protected String value = "";
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
	private @Nullable Function<String, Optional<Component>> hintProvider;
	protected Consumer<String> responder;
	protected Predicate<String> filter = Objects::nonNull;
	protected TextFormatter formatter = TextFormatter.DEFAULT;
	
	public static TextFieldWidgetEx of(String text) {
		TextFieldWidgetEx tf = new TextFieldWidgetEx(
		  Minecraft.getInstance().font, 0, 0, 0, 0, Component.empty());
		tf.setValue(text);
		return tf;
	}
	
	public TextFieldWidgetEx(
	  Font font, int x, int y, int w, int h, Component title
	) {
		this(font, x, y, w, h, null, title);
	}
	
	public TextFieldWidgetEx(
	  Font font, int x, int y, int w, int h,
	  @Nullable TextFieldWidgetEx copy, Component title
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
	
	@Override protected @NotNull MutableComponent createNarrationMessage() {
		Component component = getMessage();
		return Component.translatable("gui.narrate.editBox", component, value);
	}
	
	public void setValue(String value) {
		if (filter.test(value)) {
			if (value.length() > maxLength) {
				this.value = value.substring(0, maxLength);
			} else this.value = value;
			
			moveCaretToStart();
			setAnchorPos(caretPos);
			onValueChange(value);
		}
	}
	
	public String getValue() {
		return value;
	}
	
	public MutableComponent getDisplayedText() {
		return formatter.formatText(value);
	}
	
	public boolean hasSelection() {
		return anchorPos != caretPos;
	}
	
	public String getHighlighted() {
		return caretPos < anchorPos
		       ? value.substring(caretPos, anchorPos)
		       : value.substring(anchorPos, caretPos);
	}
	
	public void setFilter(Predicate<String> filter) {
		this.filter = filter;
	}
	
	public void insertText(String inserted) {
		if (formatter != null) inserted = formatter.stripInsertText(inserted);
		
		int start = min(caretPos, anchorPos);
		int end = max(caretPos, anchorPos);
		int allowed = maxLength - value.length() - (start - end);
		String txt = SharedConstants.filterText(inserted);
		int length = txt.length();
		if (allowed < length) {
			txt = txt.substring(0, allowed);
			length = allowed;
		}
		
		String result = (new StringBuilder(value)).replace(start, end, txt).toString();
		if (filter.test(result)) {
			value = result;
			setCaretPosition(start + length);
			setAnchorPos(caretPos);
			onValueChange(value);
		}
	}
	
	private void onValueChange(String newText) {
		if (responder != null) responder.accept(newText);
	}
	
	private void deleteText(int words) {
		if (hasSelection()) {
			insertText("");
		} else if (Screen.hasControlDown()) {
			deleteWords(words);
		} else {
			deleteFromCaret(words);
		}
	}
	
	public void deleteWords(int words) {
		if (!value.isEmpty()) {
			if (hasSelection()) {
				insertText("");
			} else {
				deleteFromCaret(getWordPosFromCaret(words) - caretPos);
			}
		}
	}
	
	public void deleteFromCaret(int chars) {
		if (!value.isEmpty()) {
			if (hasSelection()) {
				insertText("");
			} else {
				int i = expandLigaturesFromCaret(chars);
				int start = min(i, caretPos);
				int stop = max(i, caretPos);
				if (start != stop) {
					String text = getValue();
					if (formatter != null && chars == -1 && stop - start == 1 && stop < text.length()) {
						String context = new StringBuilder(text).delete(start, stop + 1).toString();
						String closingPair = formatter.closingPair(text.charAt(start), context, start);
						if (closingPair != null && text.substring(stop).startsWith(closingPair))
							stop = stop + closingPair.length();
					}
					String s = new StringBuilder(text).delete(start, stop).toString();
					
					if (filter.test(s)) {
						value = s;
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
		String text = getValue();
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
		return Util.offsetByCodepoints(value, caretPos, relativePos);
	}
	
	public void moveCaret(int pos) {
		setCaretPosition(pos);
		if (!Screen.hasShiftDown()) setAnchorPos(caretPos);
		onValueChange(value);
	}
	
	public void moveCaretWithAnchor(int pos) {
		moveCaret(pos);
		setAnchorPos(pos);
	}
	
	public void setCaretPosition(int pos) {
		caretPos = Mth.clamp(pos, 0, value.length());
		scrollToFitCaret();
	}
	
	public void moveCaretToStart() {
		moveCaret(0);
	}
	
	public void moveCaretToEnd() {
		moveCaret(value.length());
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
				KeyboardHandler kl = Minecraft.getInstance().keyboardHandler;
				if (Screen.isCopy(keyCode)) {
					kl.setClipboard(getHighlighted());
					return true;
				} else if (Screen.isPaste(keyCode)) {
					if (isEditable()) insertText(kl.getClipboard());
					return true;
				} else if (Screen.isCut(keyCode)) {
					kl.setClipboard(getHighlighted());
					if (isEditable()) insertText("");
					return true;
				} else {
					switch(keyCode) {
						case GLFW.GLFW_KEY_BACKSPACE:
							if (isEditable()) deleteText(-1);
							return true;
						case GLFW.GLFW_KEY_DELETE:
							if (isEditable()) deleteText(1);
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
		} else if (SharedConstants.isAllowedChatCharacter(codePoint)) {
			if (isEditable()) {
				String closingPair = null;
				if (formatter != null) {
					int caret = getCaret();
					String text = getValue();
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
				double relX = mouseX - getX();
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
			double relX = mouseX - getX();
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
	
	protected int getClickedCaretPos(MutableComponent line, double relX) {
		int lineLength = line.getString().length();
		int floor = font.substrByWidth(line, (int) relX).getString().length();
		if (floor >= lineLength) return lineLength;
		int left = font.width(subText(line, 0, floor));
		int right = font.width(subText(line, 0, floor + 1));
		return relX < (left + right) * 0.5? floor: floor + 1;
	}
	
	@Override public void renderButton(@NotNull PoseStack mStack, int mouseX, int mouseY, float delta) {
		if (isVisible()) {
			boolean bordered = isBordered();
			if (bordered) {
				int borderColor = isHoveredOrFocused()
				                  ? 0xFF000000 | this.borderColor & 0xFFFFFF
				                  : 0xA0000000 | this.borderColor & 0xFFFFFF;
				fill(mStack, getX() - 1, getY() - 1, getX() + width + 1, getY() + height + 1, borderColor);
				fill(mStack, getX(), getY(), getX() + width, getY() + height, 0xFF000000);
			}
			
			int color = isEditable() ? textColor : textColorUneditable;
			int relCaret = caretPos - hScroll;
			int relAnchor = anchorPos - hScroll;
			int innerWidth = getInnerWidth();
			
			MutableComponent displayedText = subText(getDisplayedText(), hScroll);
			String shown = font.substrByWidth(displayedText, innerWidth).getString();
			int fitLength = shown.length();
			displayedText = subText(displayedText, 0, fitLength);
			
			boolean fitCaret = relCaret >= 0 && relCaret <= fitLength;
			boolean showCaret = isFocused() && fitCaret
			                    && (System.currentTimeMillis() - lastInteraction) % 1000 < 500;
			int textX = bordered ? getX() + 4 : getX();
			int textY = bordered ? getY() + (height - 8) / 2 : getY();
			int caretX = fitCaret? textX + font.width(subText(displayedText, 0, relCaret)) - 1
			             : relCaret > 0? textX + innerWidth - 1 : textX;
			
			// Render text
			if (!shown.isEmpty())
				font.drawShadow(mStack, displayedText, textX, textY, color);
			
			// Render hint
			Component hint = hintProvider != null? hintProvider.apply(value).orElse(null) : null;
			if (relCaret == shown.length() && hint != null)
				font.drawShadow(mStack, hint, caretX, textY, 0xFF808080);
			
			// Render caret
			if (showCaret) {
				renderCaret(mStack, caretX, textY - 2, 1, 12);
			}
			
			// Render selection
			if (relAnchor != relCaret && isFocused()) {
				if (relAnchor > fitLength) relAnchor = fitLength;
				if (relAnchor < 0) relAnchor = 0;
				int aX = textX + font.width(subText(displayedText, 0, relAnchor)) - 1;
				renderSelection(mStack, caretX, textY - 3, aX, textY + 2 + 9);
			}
		}
	}
	
	protected void renderCaret(PoseStack mStack, int x, int y, int w, int h) {
		Tesselator tessellator = Tesselator.getInstance();
		BufferBuilder bb = tessellator.getBuilder();
		RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
		RenderSystem.disableTexture();
		RenderSystem.enableColorLogicOp();
		RenderSystem.logicOp(LogicOp.OR_REVERSE);
		Matrix4f m = mStack.last().pose();
		bb.begin(Mode.QUADS, DefaultVertexFormat.POSITION);
		bb.vertex(m,     x, y + h, 0F).endVertex();
		bb.vertex(m, x + w, y + h, 0F).endVertex();
		bb.vertex(m, x + w,     y, 0F).endVertex();
		bb.vertex(m,     x,     y, 0F).endVertex();
		tessellator.end();
		RenderSystem.disableColorLogicOp();
		RenderSystem.enableTexture();
	}
	
	protected void renderSelection(PoseStack mStack, int sX, int sY, int eX, int eY) {
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
		
		if (eX > getX() + width) eX = getX() + width;
		if (sX > getX() + width) sX = getX() + width;
		
		Tesselator tessellator = Tesselator.getInstance();
		BufferBuilder bb = tessellator.getBuilder();
		Matrix4f m = mStack.last().pose();
		RenderSystem.setShaderColor(0F, 0F, 1F, 1F);
		RenderSystem.disableTexture();
		RenderSystem.enableColorLogicOp();
		RenderSystem.logicOp(LogicOp.OR_REVERSE);
		bb.begin(Mode.QUADS, DefaultVertexFormat.POSITION);
		bb.vertex(m, sX, eY, 0F).endVertex();
		bb.vertex(m, eX, eY, 0F).endVertex();
		bb.vertex(m, eX, sY, 0F).endVertex();
		bb.vertex(m, sX, sY, 0F).endVertex();
		tessellator.end();
		RenderSystem.disableColorLogicOp();
		RenderSystem.enableTexture();
		// Do not leak the blue filter
		RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
	}
	
	public void setMaxLength(int length) {
		maxLength = length;
		if (value.length() > length) {
			value = value.substring(0, length);
			onValueChange(value);
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
		return visible && mouseX >= getX() && mouseX < getX() + width && mouseY >= getY() && mouseY < getY() + height;
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
		String text = value;
		String reversed = new StringBuilder(text).reverse().toString();
		return text.length() - font.plainSubstrByWidth(reversed, getInnerWidth()).length();
	}
	
	public void scrollToFit(int pos) {
		int maxHScroll = getMaxHScroll();
		if (font != null) {
			if (hScroll > maxHScroll) hScroll = maxHScroll;
			
			int w = getInnerWidth();
			String shown = font.plainSubstrByWidth(value.substring(hScroll), w);
			int lastShown = shown.length() + hScroll;
			
			if (pos > lastShown) {
				hScroll += pos - lastShown + 1;
			} else if (pos <= hScroll) {
				hScroll = pos - 1;
			}
			
			hScroll = Mth.clamp(hScroll, 0, maxHScroll);
		}
	}
	
	public void scrollToFitCaret() {
		scrollToFit(caretPos);
	}
	
	public void setAnchorPos(int pos) {
		int len = value.length();
		anchorPos = Mth.clamp(pos, 0, len);
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
		setHint(hint != null? Component.literal(hint) : null);
	}
	
	public void setPlainHint(@Nullable Function<String, Optional<String>> hintProvider) {
		setHint(hintProvider != null? s -> hintProvider.apply(s).map(Component::literal) : null);
	}
	
	public void setHint(@Nullable Component hint) {
		setHint(hint != null? s -> Optional.of(hint) : null);
	}
	
	public void setHint(@Nullable Function<String, Optional<Component>> hintProvider) {
		this.hintProvider = hintProvider;
	}
	
	public void setEmptyHint(String hint) {
		setEmptyHint(Component.literal(hint));
	}
	
	public void setEmptyHint(Component hint) {
		setHint(s -> s.isEmpty()? Optional.of(hint) : Optional.empty());
	}
	
	public int getScreenX(int pos) {
		return pos > value.length()? getX() : getX() + font.width(value.substring(0, pos));
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput out) {
		out.add(NarratedElementType.TITLE, Component.translatable("narration.edit_box", getValue()));
	}
}