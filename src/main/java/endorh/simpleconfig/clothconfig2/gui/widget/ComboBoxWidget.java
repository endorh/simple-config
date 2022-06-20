package endorh.simpleconfig.clothconfig2.gui.widget;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.clothconfig2.api.ScissorsHandler;
import endorh.simpleconfig.clothconfig2.api.ScrollingHandler;
import endorh.simpleconfig.clothconfig2.gui.IOverlayCapableScreen;
import endorh.simpleconfig.clothconfig2.gui.IOverlayCapableScreen.IOverlayRenderer;
import endorh.simpleconfig.clothconfig2.gui.Icon;
import endorh.simpleconfig.clothconfig2.math.Rectangle;
import net.minecraft.block.Block;
import net.minecraft.client.KeyboardListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.*;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import static java.lang.Math.*;

public class ComboBoxWidget<T> extends Widget implements IOverlayRenderer {
	protected static final ResourceLocation CONFIG_TEX = new ResourceLocation(
	  SimpleConfigMod.MOD_ID, "textures/gui/cloth_config.png");
	@Internal protected WeakReference<IOverlayCapableScreen> screen = new WeakReference<>(null);
	protected Supplier<IOverlayCapableScreen> screenSupplier;
	protected @NotNull ITypeWrapper<T> typeWrapper;
	protected int borderFocusedColor = 0xffffffff;
	protected int borderColor = 0xffa0a0a0;
	protected int backgroundColor = 0xff000000;
	protected boolean dropDownShown = false;
	protected boolean draggingDropDownScrollBar = false;
	protected boolean restrictToSuggestions = false;
	protected boolean autoDropDown = true;
	protected int arrowWidth = 10;
	protected int dropDownHeight = 120;
	protected double dropDownScroll = 0;
	protected double dropDownScrollTarget = 0;
	protected int suggestionHeight = 20;
	protected long scrollAnimationStart;
	protected long scrollAnimationDuration = 150L;
	protected long lastSuggestionCursorNavigation = 0;
	protected @Nullable ITextComponent hint = null;
	
	protected @NotNull ISortedSuggestionProvider<T> suggestionProvider;
	
	protected final FontRenderer font;
	/** Has the current text being edited on the textbox. */
	protected String text = "";
	protected T autoCompleteValue = null;
	protected @Nullable T value = null;
	protected @Nullable ITextComponent parseError = null;
	private boolean isEnabled = true;
	protected int maxLength = 32;
	protected int caretCounter;
	protected boolean shouldDrawBackground = true;
	/** if true the textbox can lose focus by clicking elsewhere on the screen */
	protected boolean canLoseFocus = true;
	/** The current character index that should be used as start of the rendered text. */
	protected int hScroll;
	protected int caretPos;
	/** other selection position, maybe the same as the cursor */
	protected int anchorPos;
	protected int enabledColor = 0xe0e0e0;
	protected int disabledColor = 0x707070;
	protected Consumer<String> textListener;
	protected Consumer<T> valueListener;
	// protected CompletableFuture<List<T>> futureSuggestions;
	protected List<T> lastSuggestions = Lists.newArrayList();
	protected List<T> lastSortedSuggestions = Lists.newArrayList();
	protected String lastQuery = "";
	protected List<ITextComponent> decoratedSuggestions = Lists.newArrayList();
	/** Called to check if the text is valid */
	protected Predicate<String> textValidator = Objects::nonNull;
	protected BiFunction<String, Integer, IReorderingProcessor> textFormatter =
	  (text, scroll) -> IReorderingProcessor.forward(text, Style.EMPTY);
	protected Rectangle dropDownRectangle = new Rectangle();
	protected Rectangle reportedDropDownRectangle = new Rectangle();
	protected long lastDropDownScroll = 0;
	protected int suggestionCursor = -1;
	
	public ComboBoxWidget(
	  @NotNull ITypeWrapper<T> typeWrapper, @NotNull Supplier<IOverlayCapableScreen> screen,
	  int x, int y, int width, int height
	) { this(typeWrapper, screen, x, y, width, height, NarratorChatListener.NO_TITLE); }
	
	public ComboBoxWidget(
	  @NotNull ITypeWrapper<T> typeWrapper, @NotNull Supplier<IOverlayCapableScreen> screen,
	  int x, int y, int width, int height, @NotNull ITextComponent title
	) { this(typeWrapper, screen, Minecraft.getInstance().font, x, y, width, height, title); }
	
	public ComboBoxWidget(
	  @NotNull ITypeWrapper<T> typeWrapper, @NotNull Supplier<IOverlayCapableScreen> screen,
	  @NotNull FontRenderer font, int x, int y, int width, int height, @NotNull ITextComponent title
	) {
		super(x, y, width, height, title);
		this.typeWrapper = typeWrapper;
		// this.screen = new WeakReference<>(screen);
		this.screenSupplier = screen;
		this.font = font;
		this.suggestionProvider = new SimpleSortedSuggestionProvider<>(
		  Lists.newArrayList());
	}
	
	protected @NotNull IOverlayCapableScreen getScreen() {
		IOverlayCapableScreen screen = this.screen.get();
		if (screen == null) {
			if (screenSupplier != null) {
				screen = screenSupplier.get();
				this.screen = new WeakReference<>(screen);
				screenSupplier = null;
				return screen;
			}
			throw new IllegalStateException(
			  "The screen of this widget has already been disposed of\n" +
			  "Do not reuse widget instances");
		}
		return screen;
	}
	
	public void setSuggestionProvider(@NotNull ISortedSuggestionProvider<T> provider) {
		this.suggestionProvider = provider;
		updateSuggestions();
		// onTextChanged(text);
	}
	
	public void setSuggestions(List<T> suggestions) {
		setSuggestionProvider(new SimpleSortedSuggestionProvider<>(suggestions));
	}
	
	public @Nullable ITextComponent getHint() {
		return hint;
	}
	
	public ComboBoxWidget<T> setHint(@Nullable ITextComponent hint) {
		this.hint = hint;
		return this;
	}
	
	@Override public boolean renderOverlay(
	  MatrixStack mStack, Rectangle area, int mouseX, int mouseY, float delta
	) {
		if (!isDropDownShown())
			return false;
		
		mStack.pushPose();{
			final int maxScroll = getMaxDropDownScroll();
			final double prev = dropDownScroll;
			this.dropDownScroll = ScrollingHandler.handleScrollingPosition(
			  new double[]{dropDownScrollTarget}, this.dropDownScroll, Double.POSITIVE_INFINITY,
			  0, scrollAnimationStart, scrollAnimationDuration);
			if (dropDownScroll > maxScroll && dropDownScroll > prev)
				dropDownScroll = maxScroll;
			if (dropDownScroll < 0 && prev > dropDownScroll)
				dropDownScroll = 0;
			
			area = dropDownRectangle;
			final int borderColor = isFocused() ? borderFocusedColor : this.borderColor;
			fill(mStack, area.x, area.y, area.getMaxX(), area.getMaxY(), borderColor);
			fill(mStack, area.x + 1, area.y, area.getMaxX() - 1, area.getMaxY() - 1, backgroundColor);
			fill(
			  mStack, area.x + 1, area.y, area.getMaxX() - 1, area.y + 1,
			  borderColor & 0xFFFFFF | borderColor / 2 & 0xFF000000);
			
			int suggestionWidth = area.width - 2;
			final boolean showScrollbar = maxScroll > 0;
			if (showScrollbar)
				suggestionWidth -= 5;
			
			if (!lastSortedSuggestions.isEmpty()) {
				final int suggestionHeight = getSuggestionHeight();
				int firstIdx =
				  (int) MathHelper.clamp(this.dropDownScroll / suggestionHeight, 0, lastSortedSuggestions.size() - 1);
				int lastIdx =
				  (int) MathHelper.clamp(
					 (this.dropDownScroll + dropDownHeight + suggestionHeight - 1) / suggestionHeight, 0,
					 lastSortedSuggestions.size() - 1);
				
				int yy = area.y + 1 - ((int) this.dropDownScroll) % suggestionHeight;
				for (int i = firstIdx; i <= lastIdx; i++) {
					renderSuggestion(
					  i, lastSortedSuggestions.get(i), mStack, area.x + 1, yy, suggestionWidth,
					  suggestionHeight,
					  mouseX, mouseY, delta, i == suggestionCursor);
					yy += suggestionHeight;
				}
				
				if (showScrollbar) {
					final int scrollBarX = area.getMaxX() - 6;
					fill(mStack, scrollBarX, area.y + 1, scrollBarX + 5, area.getMaxY(), 0x80646464);
					int thumbHeight =
					  max(20, (area.height - 2) / (lastSortedSuggestions.size() * suggestionHeight));
					int thumbY = area.y + 1 + (int) round(
					  (area.height - 2 - thumbHeight) * (this.dropDownScroll / maxScroll));
					int thumbColor = draggingDropDownScrollBar || (
					  mouseX >= scrollBarX && mouseX < scrollBarX + 5
					  && mouseY >= thumbY && mouseY < thumbY + thumbHeight
					) ? 0x96BDBDBD : 0x96808080;
					fill(
					  mStack, scrollBarX, thumbY, area.getMaxX() - 1, thumbY + thumbHeight, thumbColor);
				}
			} else {
				final Optional<ITextComponent> opt = suggestionProvider.getPlaceHolder(typeWrapper, text);
				if (opt.isPresent()) {
					drawTextComponent(
					  opt.get(), mStack, area.x + 4, area.y + 2,
					  area.width - 2, 10, 0xffe0e0e0);
				} else setDropDownShown(false);
			}
			
			fill(mStack, area.x + 1, area.y, area.getMaxX() - 1, area.y + 1, borderColor);
			fill(mStack, area.x + 1, area.getMaxY() - 1, area.getMaxX() - 1, area.getMaxY(), borderColor);
			
		} mStack.popPose();
		return true;
	}
	
	protected void renderSuggestion(
	  int i, T suggestion, MatrixStack mStack, int x, int y, int w, int h,
	  int mouseX, int mouseY, float delta, boolean selected
	) {
		boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
		if (selected) {
			fill(mStack, x, y, x + w, y + h, 0xA0E0E0E0);
			fill(mStack, x + 1, y + 1, x + w - 1, y + h - 1, hovered? 0x80646464 : 0x80484848);
		} else fill(mStack, x, y, x + w, y + h, hovered? 0x80646464 : 0x80242424);
		
		int textX = x + 2;
		int textY = y;
		final int iconHeight = getIconHeight();
		final int iconWidth = getIconWidth();
		if (typeWrapper.hasIcon()) {
			// mStack.push(); {
			// 	// mStack.translate(0D, 0D, CLIENT_CONFIG.getGUIDouble("demo.entries.serializable.z_test"));
				typeWrapper.renderIcon(
				  suggestion, typeWrapper.getName(suggestion), mStack, x, y, iconWidth,
				  min(iconHeight, h), mouseX, mouseY, delta);
			// } mStack.pop();
			textX += iconWidth;
			textY += (iconHeight - 10) / 2;
		}
		
		final ITextComponent name =
		  i >= 0 && i < decoratedSuggestions.size()
		  ? decoratedSuggestions.get(i) : typeWrapper.getDisplayName(suggestion);
		
		int textW = w - 4;
		if (hasIcon())
			textW -= iconWidth;
		
		drawTextComponent(
		  name, mStack, textX, textY + 1, textW, 10, 0xffffffff);
	}
	
	protected void drawTextComponent(
	  ITextComponent component, MatrixStack mStack, int x, int y, int w, int h, int color
	) {
		final List<IReorderingProcessor> processors = font.split(component, w);
		if (!processors.isEmpty())
			font.drawShadow(mStack, processors.get(0), (float) x, (float) y, color);
	}
	
	@Override public boolean overlayMouseScrolled(double mouseX, double mouseY, double amount) {
		final double lastScroll = this.dropDownScroll;
		final double lastTarget = this.dropDownScrollTarget;
		final long current = System.currentTimeMillis();
		scrollBy(-amount * min(24, getSuggestionHeight()), abs(amount) >= 1.0);
		// setDropDownScroll((int) round(lastScroll - amount * 10));
		if (lastTarget != dropDownScrollTarget || lastScroll != dropDownScroll || current - lastDropDownScroll <= 100) {
			this.lastDropDownScroll = current;
			return true;
		}
		return false;
	}
	
	public int getMaxDropDownScroll() {
		return getSuggestionHeight() * lastSortedSuggestions.size() + 2 - getDropDownHeight();
	}
	
	public boolean isScrollbarHovered(double mouseX, double mouseY) {
		final Rectangle area = dropDownRectangle;
		return mouseX >= area.getMaxX() - 6 && mouseX < area.getMaxX() - 1
		       && mouseY >= area.y + 1 && mouseY < area.getMaxY() - 1;
	}
	
	public boolean isScrollThumbHovered(double mouseX, double mouseY) {
		final Rectangle area = dropDownRectangle;
		int thumbHeight = max(20, (area.height - 2) / (lastSortedSuggestions.size() * suggestionHeight));
		int thumbY = area.y + 1 + (int) round((area.height - 2 - thumbHeight) * (dropDownScroll / getMaxDropDownScroll()));
		return mouseX >= area.getMaxX() - 6 && mouseX < area.getMaxX() - 1
		       && mouseY >= thumbY && mouseY < thumbY + thumbHeight;
	}
	
	@Override public boolean overlayMouseClicked(double mouseX, double mouseY, int button) {
		final Rectangle rect = dropDownRectangle;
		if (!rect.contains(mouseX, mouseY))
			return false;
		if (isScrollbarHovered(mouseX, mouseY)) {
			int thumbHeight = max(20, (rect.height - 2) / (lastSortedSuggestions.size() * suggestionHeight));
			double scrollRange = rect.height - 2 - thumbHeight;
			setDropDownScroll((int) round((mouseY - rect.y - thumbHeight / 2D) / scrollRange * getMaxDropDownScroll()));
			draggingDropDownScrollBar = true;
			return true;
		}
		final int suggestionHeight = getSuggestionHeight();
		int relY = (int) ((mouseY) - rect.y - 1 + dropDownScroll);
		int hoveredIndex = relY / suggestionHeight;
		if (hoveredIndex >= 0 && hoveredIndex < lastSortedSuggestions.size())
			setValue(lastSortedSuggestions.get(hoveredIndex));
		setDropDownShown(false);
		return true;
	}
	
	@Override public boolean overlayMouseDragged(
	  double mouseX, double mouseY, int button, double dragX, double dragY
	) {
		if (draggingDropDownScrollBar) {
			final Rectangle rect = dropDownRectangle;
			int thumbHeight = max(20, (rect.height - 2) / (lastSortedSuggestions.size() * suggestionHeight));
			double scrollRange = rect.height - 2 - thumbHeight;
			setDropDownScroll((int) round((mouseY - rect.y - thumbHeight / 2D) / scrollRange * getMaxDropDownScroll()));
			return true;
		}
		return false;
	}
	
	@Override public boolean isOverlayDragging() {
		return draggingDropDownScrollBar;
	}
	
	@Override public void overlayMouseDragEnd(double mouseX, double mouseY, int button) {
		draggingDropDownScrollBar = false;
	}
	
	@Override public boolean overlayEscape() {
		if (isDropDownShown()) {
			setDropDownShown(false);
			Minecraft.getInstance().getSoundManager().play(
			  SimpleSound.forUI(SimpleConfigMod.UI_TAP, 1F));
			return true;
		}
		return false;
	}
	
	public boolean isRestrictedToSuggestions() {
		return restrictToSuggestions;
	}
	
	public void setRestrictedToSuggestions(boolean restrict) {
		this.restrictToSuggestions = restrict;
	}
	
	public boolean isAutoDropDown() {
		return autoDropDown;
	}
	
	public void setAutoDropDown(boolean autoDropDown) {
		this.autoDropDown = autoDropDown;
	}
	
	public boolean isDropDownShown() {
		return dropDownShown;
	}
	
	public void setDropDownShown(boolean expanded) {
		this.dropDownShown = expanded;
		suggestionCursor = -1;
		if (expanded) {
			getScreen().claimRectangle(reportedDropDownRectangle, this, 100);
		} else {
			setDropDownScroll(0);
			draggingDropDownScrollBar = false;
		}
	}
	
	public List<T> getShownSuggestions() {
		return Collections.unmodifiableList(lastSortedSuggestions);
	}
	
	public int getDropDownHeight() {
		return lastSortedSuggestions.isEmpty() ? 12 : min(
		  dropDownHeight, getSuggestionHeight() * lastSortedSuggestions.size() + 2);
	}
	
	public int getIconHeight() {
		return typeWrapper.getIconHeight();
	}
	
	public int getIconWidth() {
		return typeWrapper.getIconWidth();
	}
	
	public int getSuggestionHeight() {
		return hasIcon() ? getIconHeight() : 10;
	}
	
	public void setDropDownScroll(int scroll) {
		dropDownScroll = MathHelper.clamp(scroll, 0, getMaxDropDownScroll());
		dropDownScrollTarget = dropDownScroll;
		scrollAnimationStart = 0;
	}
	
	public void scrollBy(double amount, boolean animated) {
		final int maxScroll = getMaxDropDownScroll();
		if (amount < 0 && dropDownScrollTarget > maxScroll)
			scrollTo(maxScroll + amount, animated);
		if (amount > 0 && dropDownScrollTarget < 0)
			scrollTo(amount, animated);
		else scrollTo(dropDownScrollTarget + amount, animated);
	}
	
	public void scrollTo(double scroll, boolean animated) {
		if (animated) {
			dropDownScrollTarget = MathHelper.clamp(scroll, -32, getMaxDropDownScroll() + 32);
			scrollAnimationStart = System.currentTimeMillis();
		} else {
			dropDownScroll = dropDownScrollTarget = MathHelper.clamp(scroll, 0, getMaxDropDownScroll());
			scrollAnimationStart = 0;
		}
	}
	
	public void setDropDownHeight(int height) {
		this.dropDownHeight = height;
	}
	
	public void setTextListener(Consumer<String> listener) {
		this.textListener = listener;
	}
	
	public void setValueListener(Consumer<T> listener) {
		this.valueListener = listener;
	}
	
	public void setTextFormatter(BiFunction<String, Integer, IReorderingProcessor> textFormatterIn) {
		textFormatter = textFormatterIn;
	}
	
	/**
	 * Increments the caret counter.<br>
	 * This serves to animate the caret blinking.
	 */
	public void tick() {
		++caretCounter;
	}
	
	@Override protected @NotNull IFormattableTextComponent createNarrationMessage() {
		ITextComponent itextcomponent = getMessage();
		// This should have its own key, but I think having it untranslated
		// is worse than reporting it as an edit box
		return new TranslationTextComponent("gui.narrate.editBox", itextcomponent, text);
	}
	
	/**
	 * Set the text of the textbox, and moves the cursor to the end.
	 */
	public void setText(String textIn) {
		if (textValidator.test(textIn)) {
			text = textIn.length() > maxLength ? textIn.substring(0, maxLength) : textIn;
			
			caretToEnd();
			setAnchor(caretPos);
			onTextChanged(textIn);
		}
	}
	
	public void setValue(T value) {
		setText(typeWrapper.getName(value));
		this.value = value;
	}
	
	public boolean hasIcon() {
		return typeWrapper.hasIcon();
	}
	
	/**
	 * @return The contents of the textbox
	 */
	public String getText() {
		return text;
	}
	
	/**
	 * @return The selected text
	 */
	public String getSelectedText() {
		return caretPos < anchorPos
		       ? text.substring(caretPos, anchorPos)
		       : text.substring(anchorPos, caretPos);
	}
	
	public void setTextValidator(Predicate<String> validator) {
		textValidator = validator;
	}
	
	/**
	 * Adds the given text after the cursor, or replaces the currently
	 * selected text if the selection is not empty.
	 */
	public void writeText(String textToWrite) {
		int start = min(caretPos, anchorPos);
		int end = max(caretPos, anchorPos);
		int allowed = maxLength - text.length() + (end - start);
		String txt = SharedConstants.filterText(textToWrite);
		int length = txt.length();
		if (allowed < length) {
			txt = txt.substring(0, allowed);
			length = allowed;
		}
		
		final String result = (new StringBuilder(text)).replace(start, end, txt).toString();
		if (textValidator.test(result)) {
			text = result;
			clampCaretPosition(start + length);
			setAnchor(caretPos);
			onTextChanged(text);
			if (isAutoDropDown() && !isDropDownShown() && !lastSortedSuggestions.isEmpty())
				setDropDownShown(true);
		}
	}
	
	protected void onTextChanged(String newText) {
		if (textListener != null)
			textListener.accept(newText);
		nextNarration = Util.getMillis() + 500L;
		if (isRestrictedToSuggestions() && isAutoDropDown())
			setDropDownShown(true);
		updateValue();
	}
	
	protected void updateValue() {
		if (!isRestrictedToSuggestions()) {
			final Pair<Optional<T>, Optional<ITextComponent>> parsed = typeWrapper.parseElement(text);
			this.value = parsed.getLeft().orElse(null);
			this.parseError = parsed.getRight().orElse(null);
			this.onValueChanged(value);
		}
		updateSuggestions();
	}
	
	protected void updateSuggestions() {
		String query = text;
		final Optional<List<T>> opt = suggestionProvider.updateSuggestions(typeWrapper, query);
		opt.ifPresent(ls -> lastSuggestions = ls);
		if (opt.isPresent() || !query.equals(lastQuery)) {
			final Pair<List<T>, List<ITextComponent>> pair =
			  suggestionProvider.pickAndDecorateSuggestions(typeWrapper, query, lastSuggestions);
			lastSortedSuggestions = pair.getLeft();
			decoratedSuggestions = pair.getRight();
			lastQuery = query;
			setDropDownScroll(0);
		}
	}
	
	protected void onValueChanged(T newValue) {
		if (valueListener != null)
			valueListener.accept(newValue);
	}
	
	protected void delete(int num) {
		if (Screen.hasControlDown()) {
			deleteWords(num);
		} else {
			deleteFromCaret(num);
		}
	}
	
	/**
	 * Delete the given number of words from the current cursor's position, unless there is currently a selection, in
	 * which case the selection is deleted instead.
	 */
	public void deleteWords(int num) {
		if (!text.isEmpty()) {
			if (anchorPos != caretPos) {
				writeText("");
			} else deleteFromCaret(getNthWordFromCaret(num) - caretPos);
		}
	}
	
	/**
	 * Delete the given number of characters from the current cursor's position, unless there is currently a selection,
	 * in which case the selection is deleted instead.
	 */
	public void deleteFromCaret(int num) {
		if (!text.isEmpty()) {
			if (anchorPos != caretPos) {
				writeText("");
			} else {
				int i = expandLigaturesFromCaret(num);
				int j = min(i, caretPos);
				int k = max(i, caretPos);
				if (j != k) {
					String s = (new StringBuilder(text)).delete(j, k).toString();
					if (textValidator.test(s)) {
						text = s;
						setCaret(j);
					}
				}
			}
		}
	}
	
	/**
	 * Get the starting index of the word at the specified number of words away from the cursor position.
	 */
	public int getNthWordFromCaret(int numWords) {
		return getNthWordFromPos(numWords, getCaret());
	}
	
	/**
	 * Get the starting index of the word at a distance of the specified number of words away from the given position.
	 */
	protected int getNthWordFromPos(int n, int pos) {
		return getNthWordFromPosWS(n, pos, true);
	}
	
	/**
	 * Like getNthWordFromPos (which wraps this), but adds option for skipping consecutive spaces
	 */
	protected int getNthWordFromPosWS(int n, int pos, boolean skipWS) {
		int p = pos;
		for(int k = 0, count = abs(n); k < count; ++k) {
			if (!(n < 0)) {
				int l = text.length();
				p = text.indexOf(32, p);
				if (p == -1) {
					p = l;
				} else if (skipWS) {
					while (p < l && text.charAt(p) == ' ') ++p;
				}
			} else {
				if (skipWS) {
					while (p > 0 && text.charAt(p - 1) == ' ') --p;
				}
				while(p > 0 && text.charAt(p - 1) != ' ') --p;
			}
		}
		return p;
	}
	
	/**
	 * Moves the text cursor by a specified number of characters and clears the selection
	 */
	public void moveCaretBy(int num) {
		setCaret(expandLigaturesFromCaret(num));
	}
	
	protected int expandLigaturesFromCaret(int chars) {
		return Util.offsetByCodepoints(text, caretPos, chars);
	}
	
	public void selectAll() {
		setSelection(0, text.length());
	}
	
	public void setSelection(int anchor, int caret) {
		setCaret(caret);
		setAnchor(anchor);
	}
	
	/**
	 * Set the current position of the cursor.
	 */
	public void setCaret(int pos) {
		clampCaretPosition(pos);
		if (!Screen.hasShiftDown())
			setAnchor(caretPos);
		onTextChanged(text);
	}
	
	public void clampCaretPosition(int pos) {
		caretPos = MathHelper.clamp(pos, 0, text.length());
	}
	
	/**
	 * Moves the cursor to the start of this text box.
	 */
	public void caretToStart() {
		setCaret(0);
	}
	
	/**
	 * Moves the cursor to the end of this text box.
	 */
	public void caretToEnd() {
		setCaret(text.length());
	}
	
	public void autoComplete() {
		suggestionCursor = -1;
		if (!isDropDownShown()) {
			setDropDownShown(true);
		} else if (autoCompleteValue != null) {
			setValue(autoCompleteValue);
			//if (isRestrictedToSuggestions() || lastSuggestions.size() == 1)
			setDropDownShown(false);
		}
	}
	
	@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (!canWrite()) {
			return false;
		} else {
			if (Screen.isSelectAll(keyCode)) {
				caretToEnd();
				setAnchor(0);
				return true;
			} else {
				final KeyboardListener kl = Minecraft.getInstance().keyboardHandler;
				if (Screen.isCopy(keyCode)) {
					kl.setClipboard(getSelectedText());
					Minecraft.getInstance().getSoundManager().play(
					  SimpleSound.forUI(SimpleConfigMod.UI_TAP, 1F));
					return true;
				} else if (Screen.isPaste(keyCode)) {
					if (canEditText())
						writeText(kl.getClipboard());
					Minecraft.getInstance().getSoundManager().play(
					  SimpleSound.forUI(SimpleConfigMod.UI_TAP, 1F));
					return true;
				} else if (Screen.isCut(keyCode)) {
					kl.setClipboard(getSelectedText());
					if (canEditText())
						writeText("");
					Minecraft.getInstance().getSoundManager().play(
					  SimpleSound.forUI(SimpleConfigMod.UI_TAP, 1F));
					return true;
				} else {
					switch(keyCode) {
						case 259: // Backspace
							if (canEditText())
								delete(-1);
							return true;
						case 264: // Down
							moveSuggestionCursor(1);
							return true;
						case 265: // Up
							moveSuggestionCursor(-1);
							return true;
						case 257: // Enter
							autoComplete();
							Minecraft.getInstance().getSoundManager().play(
							  SimpleSound.forUI(SimpleConfigMod.UI_TAP, 1F));
							return true;
						case 266: // Page Up
							moveSuggestionCursor(-max(1, dropDownHeight / getSuggestionHeight() - 1));
							return true;
						case 267: // Page Down
							moveSuggestionCursor(max(1, dropDownHeight / getSuggestionHeight() - 1));
							return true;
						case 261: // Delete
							if (canEditText())
								delete(1);
							return true;
						case 262: // Right
							if (Screen.hasControlDown()) {
								setCaret(getNthWordFromCaret(1));
							} else moveCaretBy(1);
							return true;
						case 263: // Left
							if (Screen.hasControlDown()) {
								setCaret(getNthWordFromCaret(-1));
							} else moveCaretBy(-1);
							return true;
						case 268: // Home
							caretToStart();
							return true;
						case 269: // End
							caretToEnd();
							return true;
						case 260: // Insert
						default:
							return false;
					}
				}
			}
		}
	}
	
	public void moveSuggestionCursor(int step) {
		if (!isDropDownShown())
			setDropDownShown(true);
		int prev = suggestionCursor;
		suggestionCursor = MathHelper.clamp(suggestionCursor + step, 0, lastSortedSuggestions.size() - 1);
		// Ensure visible
		final int suggestionHeight = getSuggestionHeight();
		int firstIdx = (int) MathHelper.clamp(dropDownScroll / suggestionHeight, 0, lastSortedSuggestions.size() - 1);
		int lastIdx = (int) MathHelper.clamp((dropDownScroll + dropDownHeight + suggestionHeight - 1) / suggestionHeight, 0, lastSortedSuggestions.size() - 1);
		
		final long t = System.currentTimeMillis();
		// Do not animate for very fast movement for better readability
		final boolean animated = t - lastSuggestionCursorNavigation >= 60;
		if (suggestionCursor <= firstIdx)
			scrollTo(suggestionHeight * (suggestionCursor - 0.5), animated);
		else if (suggestionCursor >= lastIdx - 1)
			scrollTo(suggestionHeight * (suggestionCursor + 1.8) - dropDownHeight, animated);
		lastSuggestionCursorNavigation = t;
		if (prev != suggestionCursor)
			Minecraft.getInstance().getSoundManager().play(
			  SimpleSound.forUI(SimpleConfigMod.UI_TAP, 1F));
	}
	
	@SuppressWarnings("BooleanMethodIsAlwaysInverted") public boolean canWrite() {
		return getVisible() && isFocused() && isEnabled();
	}
	
	@Override public boolean charTyped(char codePoint, int modifiers) {
		if (!canWrite()) {
			return false;
		} else if (codePoint == 32 && (modifiers & 2) != 0) {
			autoComplete();
			Minecraft.getInstance().getSoundManager().play(
			  SimpleSound.forUI(SimpleConfigMod.UI_TAP, 1F));
			return true;
		} else if (SharedConstants.isAllowedChatCharacter(codePoint)) {
			if (canEditText())
				writeText(Character.toString(codePoint));
			return true;
		}
		return false;
	}
	
	@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!active) return false;
		if (getVisible()) {
			if (isMouseOverArrow((int) round(mouseX), (int) round(mouseY))
			    || isMouseOverIcon((int) round(mouseX), (int) round(mouseY))) {
				if (!isFocused()) setFocused(true);
				setDropDownShown(!isDropDownShown());
				Minecraft.getInstance().getSoundManager().play(
				  SimpleSound.forUI(SoundEvents.UI_BUTTON_CLICK, 1F));
				return true;
			}
			
			boolean hovered = isMouseOver(mouseX, mouseY);
			if (isRestrictedToSuggestions() && hovered) {
				if (!isFocused()) setFocused(true);
				final boolean wasShown = isDropDownShown();
				setDropDownShown(!wasShown);
				if (!wasShown) setText("");
				Minecraft.getInstance().getSoundManager().play(
				  SimpleSound.forUI(SoundEvents.UI_BUTTON_CLICK, 1F));
				return true;
			}
			
			if (!(canLoseFocus && isFocused()))
				setFocused(hovered);
			
			if (isFocused() && hovered && button == 0) {
				int i = MathHelper.floor(mouseX) - x;
				if (shouldDrawBackground())
					i -= 4;
				String s = font.plainSubstrByWidth(text.substring(hScroll), getAdjustedWidth());
				setCaret(font.plainSubstrByWidth(s, i).length() + hScroll);
				setAnchor(caretPos); // Forced
				return true;
			}
			
			if (isFocused() && hovered && button == 1 && !isRestrictedToSuggestions()) {
				setText("");
				return true;
			}
		}
		return false;
	}
	
	@Override public void setFocused(boolean focused) {
		super.setFocused(focused);
		if (!focused)
			setDropDownShown(false);
	}
	
	protected void updateDropDownRectangle() {
		dropDownRectangle.x = x - 1;
		dropDownRectangle.y = y + height;
		dropDownRectangle.width = width + 2;
		dropDownRectangle.height = getDropDownHeight();
		
		reportedDropDownRectangle.setBounds(
		  ScissorsHandler.INSTANCE.getScissorsAreas().stream()
		    .reduce(dropDownRectangle, Rectangle::intersection));
		if (reportedDropDownRectangle.isEmpty())
			reportedDropDownRectangle.setBounds(0, 0, 0, 0);
	}
	
	@Override public void renderButton(
	  @NotNull MatrixStack mStack, int mouseX, int mouseY, float delta
	) {
		if (getVisible()) {
			updateSuggestions();
			if (shouldDrawBackground()) {
				int borderColor = isFocused() ? 0xffffffff : 0xffa0a0a0;
				fill(mStack, x - 1, y - 1, x + width + 1, y + height + 1, borderColor);
				fill(mStack, x, y, x + width, y + height, 0xff000000);
				if (!isRestrictedToSuggestions())
					fill(mStack, x + width - arrowWidth - 1, y, x + width - arrowWidth, y + height, borderColor);
			}
			
			if (!isFocused())
				setDropDownShown(false);
			
			updateDropDownRectangle();
			
			if (lastSortedSuggestions.isEmpty()) {
				autoCompleteValue = null;
			} else if (suggestionCursor >= 0 && suggestionCursor < lastSortedSuggestions.size()) {
				autoCompleteValue = lastSortedSuggestions.get(suggestionCursor);
			} else if (text.isEmpty() && !"".equals(value)) {
				autoCompleteValue = value;
			} else autoCompleteValue = lastSortedSuggestions.get(0);
			
			boolean hasIcon = hasIcon();
			final int iconHeight = getIconHeight();
			final int iconWidth = getIconWidth();
			int textX = hasIcon? x + iconWidth + 1 : shouldDrawBackground ? x + 4 : x;
			int textY = shouldDrawBackground ? y + (height - 8) / 2 : y;
			int iconX = x;
			int iconY = shouldDrawBackground? textY + 4 - iconHeight / 2 : hasIcon? y + iconHeight / 2 - 4 : y;
			final int adjustedWidth = getAdjustedWidth();
			if (isFocused() || value == null && hint == null) {
				int color = isEnabled() ? enabledColor : disabledColor;
				int relCaret = caretPos - hScroll;
				int relAnchor = anchorPos - hScroll;
				String shownString = font.plainSubstrByWidth(text.substring(hScroll), adjustedWidth);
				boolean fitCaret = relCaret >= 0 && relCaret <= shownString.length();
				boolean showCaret = isFocused() && caretCounter / 6 % 2 == 0 && fitCaret;
				int caretX = textX;
				
				int endTextX = textX;
				// Render pre-caret
				if (!shownString.isEmpty()) {
					String preCursor = fitCaret ? shownString.substring(0, relCaret) : shownString;
					caretX =
					  font.drawShadow(
					    mStack, textFormatter.apply(preCursor, hScroll), (float) textX, (float) textY, color);
					endTextX = caretX;
				}
				
				boolean caretIsInsert = caretPos < text.length() || text.length() >= getMaxLength();
				int cX = caretX;
				if (!fitCaret)
					cX = relCaret > 0 ? textX + width : textX;
				else if (caretIsInsert)
					cX = --caretX;
				
				// Render post-caret
				if (!shownString.isEmpty() && fitCaret && relCaret < shownString.length()) {
					endTextX = font.drawShadow(
					  mStack, textFormatter.apply(shownString.substring(relCaret), caretPos),
					  (float) caretX, (float) textY, color);
				}
				
				if (isDropDownShown() && autoCompleteValue != null) {
					final String autoComplete = typeWrapper.getName(autoCompleteValue);
					String shownAutocomplete =
					  autoComplete.startsWith(text)
					  ? autoComplete.substring(text.length())
					  : "→" + autoComplete;
					shownAutocomplete = font.plainSubstrByWidth(shownAutocomplete, adjustedWidth - endTextX + textX);
					font.drawShadow(
					  mStack, textFormatter.apply(shownAutocomplete, 0), (float) endTextX, (float) textY, 0x96808080);
				}
				
				// Render caret
				if (showCaret) {
					if (caretIsInsert) {
						fill(mStack, cX, textY - 1, cX + 1, textY + 1 + 9, 0xffd0d0d0);
					} else font.drawShadow(mStack, "_", (float)cX, (float)textY, color);
				}
				
				if (relAnchor != relCaret) {
					if (relAnchor > shownString.length())
						relAnchor = shownString.length();
					int anchorX = textX + font.width(shownString.substring(0, relAnchor));
					drawSelectionBox(cX, textY - 1, anchorX - 1, textY + 1 + 9);
				}
			} else if (text.isEmpty() && value == null) {
				drawTextComponent(
				  hint.plainCopy().withStyle(TextFormatting.GRAY),
				  mStack, textX, textY, adjustedWidth, 10, 0x96808080);
			} else if (value != null) {
				final ITextComponent display = typeWrapper.getDisplayName(value);
				drawTextComponent(display, mStack, textX, textY, adjustedWidth, 10, 0xffe0e0e0);
			}
			
			if (hasIcon) typeWrapper.renderIcon(
			  autoCompleteValue != null && isDropDownShown()? autoCompleteValue : value, text, mStack,
			  iconX, iconY, iconWidth, iconHeight, mouseX, mouseY, delta);
			
			int arrowX = x + width - arrowWidth, arrowY = y;
			renderArrow(mStack, arrowX, arrowY, arrowWidth, height, mouseX, mouseY, isRestrictedToSuggestions() && isHovered() ? x : arrowX);
		}
	}
	
	public boolean isMouseOverArrow(int mouseX, int mouseY) {
		int arrowX = x + width - arrowWidth, arrowY = y;
		return mouseX >= arrowX && mouseX < arrowX + arrowWidth && mouseY >= arrowY && mouseY < arrowY + height;
	}
	
	public boolean isMouseOverIcon(int mouseX, int mouseY) {
		final int iconHeight = getIconHeight();
		int iconY = shouldDrawBackground? y + height / 2 - iconHeight / 2 : y;
		return hasIcon() && mouseX >= x && mouseX < x + getIconWidth()
		       && mouseY >= iconY && mouseY < iconY + iconHeight;
	}
	
	protected void renderArrow(MatrixStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, int backgroundX) {
		final boolean hovered = mouseX >= backgroundX && mouseX < x + w && mouseY >= y && mouseY < y + h;
		int arrowBackground = hovered ? 0x80646464 : 0x80242424;
		Minecraft.getInstance().getTextureManager().bind(new ResourceLocation(
		  SimpleConfigMod.MOD_ID, "textures/gui/cloth_config.png"));
		if (backgroundX == mouseX || hovered)
			fill(mStack, backgroundX, y, x + w, y + h, arrowBackground);
		blit(mStack, x, y + (h - 10) / 2, 116, isDropDownShown()? 74 : 64, 10, 10);
	}
	
	/**
	 * Draws the blue selection box.
	 */
	protected void drawSelectionBox(int startX, int startY, int endX, int endY) {
		if (startX < endX) {
			final int swap = startX;
			startX = endX;
			endX = swap;
		}
		
		if (startY < endY) {
			final int swap = startY;
			startY = endY;
			endY = swap;
		}
		
		if (endX > x + width)
			endX = x + width;
		
		if (startX > x + width)
			startX = x + width;
		
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bb = tessellator.getBuilder();
		//noinspection deprecation
		RenderSystem.color4f(0F, 0F, 1F, 1F);
		RenderSystem.disableTexture();
		RenderSystem.enableColorLogicOp();
		RenderSystem.logicOp(GlStateManager.LogicOp.OR_REVERSE);
		bb.begin(7, DefaultVertexFormats.POSITION);
		bb.vertex(startX, endY, 0.0D).endVertex();
		bb.vertex(endX, endY, 0.0D).endVertex();
		bb.vertex(endX, startY, 0.0D).endVertex();
		bb.vertex(startX, startY, 0.0D).endVertex();
		tessellator.end();
		RenderSystem.disableColorLogicOp();
		RenderSystem.enableTexture();
		// Do not leak the blue filter
		//noinspection deprecation
		RenderSystem.color4f(1F, 1F, 1F, 1F);
	}
	
	/**
	 * Set the maximum length for the text in this text box. If the current text is longer than this length, the current
	 * text will be trimmed.
	 */
	public void setMaxLength(int length) {
		maxLength = length;
		if (text.length() > length) {
			text = text.substring(0, length);
			onTextChanged(text);
		}
	}
	
	/**
	 * @return The maximum number of character that can be contained in this textbox
	 */
	protected int getMaxLength() {
		return maxLength;
	}
	
	/**
	 * @return The current position of the cursor
	 */
	public int getCaret() {
		return caretPos;
	}
	
	/**
	 * @return Whether the background and outline of this text box should be drawn (true if so).
	 */
	protected boolean shouldDrawBackground() {
		return shouldDrawBackground;
	}
	
	/**
	 * Set whether the background and outline of this text box should be drawn.
	 */
	public void setShouldDrawBackground(boolean shouldDrawBackground) {
		this.shouldDrawBackground = shouldDrawBackground;
	}
	
	/**
	 * Set the color to use when drawing this text box's text. A different color is used if this text box is disabled.
	 */
	public void setTextColor(int color) {
		enabledColor = color;
	}
	
	/**
	 * Set the color to use for text in this text box when this text box is disabled.
	 */
	public void setDisabledTextColour(int color) {
		disabledColor = color;
	}
	
	@Override public boolean changeFocus(boolean focus) {
		return visible && isEnabled() && super.changeFocus(focus);
	}
	
	@Override public boolean isMouseOver(double mouseX, double mouseY) {
		return visible && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}
	
	@Override protected void onFocusedChanged(boolean focused) {
		if (focused) {
			caretCounter = 0;
			if (isRestrictedToSuggestions()) {
				setText("");
				if (isAutoDropDown())
					setDropDownShown(true);
			}
		} else {
			if (isRestrictedToSuggestions()) {
				if (value != null) {
					setText(typeWrapper.getName(value));
				} else setText("");
			}
			setDropDownShown(false);
		}
	}
	
	protected boolean isEnabled() {
		return isEnabled;
	}
	
	protected boolean canEditText() {
		return isEnabled();
	}
	
	/**
	 * Set whether this text box is enabled. Disabled text boxes cannot be typed in.
	 */
	public void setEnabled(boolean enabled) {
		isEnabled = enabled;
	}
	
	/**
	 * @return The width of the textbox depending on if background drawing is enabled
	 */
	public int getAdjustedWidth() {
		int w = shouldDrawBackground() ? width - 8 : width;
		if (hasIcon())
			w += 4 - getIconWidth();
		return w - arrowWidth;
	}
	
	/**
	 * Set the position of the selection anchor.<br>
	 * The selection anchor is the caret position left behind when
	 * holding shift, or the initial position clicked when dragging
	 * the mouse, and along with the caret determines the bounds
	 * of the selection.<br>
	 * If the anchor is set beyond the bounds of the current text,
	 * it will be put back inside.
	 */
	public void setAnchor(int pos) {
		int i = text.length();
		anchorPos = MathHelper.clamp(pos, 0, i);
		if (font != null) {
			if (hScroll > i)
				hScroll = i;
			
			int j = getAdjustedWidth();
			String s = font.plainSubstrByWidth(text.substring(hScroll), j);
			int k = s.length() + hScroll;
			if (anchorPos == hScroll)
				hScroll -= font.plainSubstrByWidth(text, j, true).length();
			
			if (anchorPos > k) {
				// We can't assume the font is monospace (plus the default actually isn't)
				final String rev = new StringBuilder(text.substring(0, anchorPos)).reverse().toString();
				hScroll = anchorPos - font.plainSubstrByWidth(rev, j).length();
			} else if (anchorPos <= hScroll) {
				hScroll = anchorPos;
			}
			
			hScroll = MathHelper.clamp(hScroll, 0, i);
		}
	}
	
	/**
	 * Set whether this text box loses focus when something other than it is clicked.
	 */
	public void setCanLoseFocus(boolean canLoseFocusIn) {
		canLoseFocus = canLoseFocusIn;
	}
	
	/**
	 * @return true if this textbox is visible
	 */
	public boolean getVisible() {
		return visible;
	}
	
	/**
	 * Set whether this textbox is visible
	 */
	public void setVisible(boolean isVisible) {
		visible = isVisible;
	}
	
	/**
	 * Copied from TextFieldWidget, but it's not precisely useful,
	 * since it doesn't account for the line scroll
	 */
	@Deprecated public int textXFor(int pos) {
		return pos > text.length() ? x : x + font.width(text.substring(0, pos));
	}
	
	public @Nullable T getValue() {
		return value;
	}
	
	public Optional<ITextComponent> getError() {
		return Optional.ofNullable(parseError);
	}
	
	// Extension
	
	public interface ISortedSuggestionProvider<T> {
		Pair<List<T>, List<ITextComponent>> pickAndDecorateSuggestions(
		  ITypeWrapper<T> typeWrapper, String query, List<T> suggestions);
		Optional<List<T>> updateSuggestions(ITypeWrapper<T> typeWrapper, String query);
		default Optional<ITextComponent> getPlaceHolder(ITypeWrapper<T> typeWrapper, String query) {
			return Optional.of(new TranslationTextComponent("text.cloth-config.dropdown.value.unknown"));
		}
	}
	
	public static abstract class AbstractSortedSuggestionProvider<T> implements ISortedSuggestionProvider<T> {
		/**
		 * Splits word parts
		 */
		protected static Pattern TOKEN_SPLITTER = Pattern.compile("[\\s_]++|(?<=[a-z])(?=[A-Z])");
		
		/**
		 * Extract a formatted substring from an {@link ITextComponent}.<br>
		 * Should be called on the client side only, if the component may contain translations.<br>
		 * <br>
		 * See {@code endorh.util.text.TextUtil} from the {@code endorh-util} mod
		 * @param text Component to slice
		 * @param start Start index of the substring.
		 *              Negative values are corrected counting from the end.
		 * @param end End index of the substring.
		 *            Negative values are corrected counting from the end.
		 *            Defaults to the end of the component.
		 */
		protected static IFormattableTextComponent subText(ITextComponent text, int start, int end) {
			final int n = text.getString().length();
			if (start > n) throw new StringIndexOutOfBoundsException("Index: " + start + ", Length: " + n);
			if (start < 0) {
				if (n + start < 0) throw new StringIndexOutOfBoundsException("Index: " + start + ", Length: " + n);
				start = n + start;
			}
			if (end > n) throw new StringIndexOutOfBoundsException("Index: " + end + ", Length: " + n);
			if (end < 0) {
				if (n + end < 0) throw new StringIndexOutOfBoundsException("Index: " + end + ", Length: " + n);
				end = n + end;
			}
			if (end <= start) return new StringTextComponent("");
			boolean started = false;
			final List<ITextComponent> siblings = text.getSiblings();
			IFormattableTextComponent res = new StringTextComponent("");
			String str = text.getContents();
			if (start < str.length()) {
				started = true;
				res = res.append(new StringTextComponent(
				  str.substring(start, Math.min(str.length(), end))).setStyle(text.getStyle()));
				if (end < str.length()) return res;
			}
			int o = str.length();
			for (ITextComponent sibling : siblings) {
				str = sibling.getContents();
				if (started || start - o < str.length()) {
					res = res.append(new StringTextComponent(
					  str.substring(started? 0 : start - o, Math.min(str.length(), end - o))
					).setStyle(sibling.getStyle()));
					started = true;
					if (end - o < str.length()) return res;
				}
				o += str.length();
			}
			return res;
		}
		
		/**
		 * Matches at word part starts
		 */
		protected static List<String> tokenMatches(String target, String query) {
			query = query.trim();
			target = target.trim();
			if (query.length() > target.length())
				return Collections.emptyList();
			final String[] q = TOKEN_SPLITTER.split(query);
			final String[] t = TOKEN_SPLITTER.split(target);
			if (t.length == 0)
				return Collections.emptyList();
			List<String> result = Lists.newArrayList();
			int r = -1;
			String rem;
			for (String qq : q) {
				qq = qq.toLowerCase();
				if (++r < t.length) {
					rem = t[r];
				} else return Collections.emptyList();
				while (!qq.isEmpty()) {
					int j = 0;
					int m = min(qq.length(), rem.length());
					while (j < m && qq.charAt(j) == Character.toLowerCase(rem.charAt(j))) j++;
					if (j == 0) {
						if (++r < t.length) {
							rem = t[r];
							continue;
						} else return Collections.emptyList();
					}
					result.add(rem.substring(0, j));
					qq = qq.substring(j);
					if (qq.isEmpty())
						break;
					if (++r < t.length) {
						rem = t[r];
					} else return Collections.emptyList();
				}
			}
			return result;
		}
		
		@Override public Pair<List<T>, List<ITextComponent>> pickAndDecorateSuggestions(
		  ITypeWrapper<T> typeWrapper, String query, List<T> suggestions
		) {
			if (query.isEmpty())
				return Pair.of(suggestions, suggestions.stream()
				  .map(typeWrapper::getDisplayName).collect(Collectors.toList()));
			if (suggestions.isEmpty()) return Pair.of(suggestions, new ArrayList<>());
			Set<T> set = new LinkedHashSet<>();
			List<ITextComponent> names = new ArrayList<>();
			suggestions.stream()
			  .map(e -> {
				  final String n = typeWrapper.getName(e);
				  return Triple.of(e, n, tokenMatches(n, query));
			  }).filter(t -> !t.getRight().isEmpty())
			  .sorted(Comparator.<Triple<T, String, List<String>>>comparingInt(
				 t -> t.getRight().stream().mapToInt(String::length).reduce(0, (a, b) -> a * b)
			  ).thenComparingInt(t -> t.getMiddle().length()))
			  .forEachOrdered(t -> {
				  final T value = t.getLeft();
				  if (set.add(value)) {
					  final String name = t.getMiddle();
					  String n = name;
					  final String[] sp = TOKEN_SPLITTER.split(name);
					  final List<String> matches = t.getRight();
					  int i = 0, o = 0;
					  IFormattableTextComponent stc = new StringTextComponent("");
					  for (final String frag : sp) {
						  if (i >= matches.size()) break;
						  final int s = n.indexOf(frag);
						  if (s > 0) {
							  stc = stc.append(getNonMatch(typeWrapper, value, name, o, n.substring(0, s)));
							  o += s;
							  n = n.substring(s);
						  }
						  final String tar = matches.get(i);
						  final int j = frag.indexOf(tar);
						  if (j == -1) {
							  stc = stc.append(getNonMatch(typeWrapper, value, name, o, frag));
						  } else {
							  stc = stc.append(getNonMatch(typeWrapper, value, name, o, frag.substring(0, j)))
							    .append(getMatch(typeWrapper, value, name, o, frag, o + j, tar))
							    .append(getNonMatch(typeWrapper, value, name, o + j + tar.length(), frag.substring(j + tar.length())));
							  i++;
						  }
						  o += frag.length();
						  n = n.substring(frag.length());
					  }
					  stc = stc.append(getNonMatch(typeWrapper, value, name, o, n));
					  names.add(stc);
				  }
			  });
			suggestions.stream()
			  .filter(e -> !set.contains(e))
			  .map(e -> Pair.of(e, typeWrapper.getName(e)))
			  .filter(p -> p.getRight().contains(query))
			  .sorted(Comparator
				         .<Pair<T, String>>comparingInt(p -> p.getRight().length())
				         .thenComparingInt(p -> p.getRight().indexOf(query))
				         .thenComparing(Pair::getRight))
			  .forEachOrdered(p -> {
				  final T value = p.getKey();
				  if (set.add(value)) {
					  final String name = p.getRight();
					  final int i = name.indexOf(query);
					  names.add(getNonMatch(typeWrapper, value, name, 0, name.substring(0, i)).copy()
					              .append(getMatch(typeWrapper, value, name, 0, name, i, query))
					              .append(getNonMatch(typeWrapper, value, name, i + query.length(), name.substring(i + query.length()))));
				  }
			  });
			return Pair.of(new ArrayList<>(set), names);
		}
		
		protected Style getMatchStyle() {
			return Style.EMPTY.applyFormats(TextFormatting.BLUE);
		}
		
		protected ITextComponent getMatch(
		  ITypeWrapper<T> typeWrapper, T item, String name, int fragmentPos,
		  String fragment, int matchPos, String match
		) {
			return new StringTextComponent(match).setStyle(getMatchStyle());
		}
		
		protected ITextComponent getNonMatch(
		  ITypeWrapper<T> typeWrapper, T item, String name, int fragmentPos, String fragment
		) {
			final ITextComponent title = typeWrapper.getDisplayName(item);
			if (!title.getString().equals(name))
				return new StringTextComponent(fragment);
			return subText(title, fragmentPos, fragmentPos + fragment.length());
		}
	}
	
	public static class SimpleSortedSuggestionProvider<T> extends AbstractSortedSuggestionProvider<T> {
		@NotNull protected Supplier<List<T>> suggestions;
		protected List<T> lastSuggestions = null;
		protected long lastUpdate = 0L;
		protected long updateCooldown = 250L;
		protected @Nullable Function<String, ITextComponent> placeholder = null;
		
		public SimpleSortedSuggestionProvider(
		  @NotNull List<T> suggestions
		) {
			this.suggestions = () -> suggestions;
		}
		
		public SimpleSortedSuggestionProvider(
		  @NotNull Supplier<List<T>> suggestionSupplier
		) {
			this.suggestions = suggestionSupplier;
		}
		
		@Override public Optional<List<T>> updateSuggestions(
		  ITypeWrapper<T> typeWrapper, String query
		) {
			final long time = System.currentTimeMillis();
			if (time - lastUpdate < updateCooldown)
				return Optional.empty();
			lastUpdate = time;
			final List<T> suggestions = new ArrayList<>(this.suggestions.get());
			if (!Objects.equals(suggestions, lastSuggestions)) {
				lastSuggestions = suggestions;
				return Optional.of(suggestions);
			}
			return Optional.empty();
		}
		
		@Override public Optional<ITextComponent> getPlaceHolder(
		  ITypeWrapper<T> typeWrapper, String query
		) {
			return placeholder != null? Optional.of(placeholder.apply(query))
			                          : super.getPlaceHolder(typeWrapper, query);
		}
		
		/**
		 * Set the minimum cooldown between suggestion update checks.<br>
		 * Defaults to 500ms.
		 */
		public void setUpdateCooldown(long cooldownMs) {
			this.updateCooldown = cooldownMs;
		}
		public void setPlaceholder(@Nullable Function<String, ITextComponent> getter) {
			this.placeholder = getter;
		}
		public void setPlaceholder(ITextComponent placeholder) {
			setPlaceholder(s -> placeholder);
		}
	}
	
	public interface ITypeWrapper<T> {
		Icon ICON_ERROR = new Icon(new ResourceLocation(
		  SimpleConfigMod.MOD_ID, "textures/gui/cloth_config.png"), 96, 84, 20, 20, 256, 256);
		Icon ICON_UNKNOWN = new Icon(new ResourceLocation(
		  SimpleConfigMod.MOD_ID, "textures/gui/cloth_config.png"), 96, 64, 20, 20, 256, 256);
		
		/**
		 * Whether this type has an icon to display in combo boxes<br>
		 * Subclasses that return yes should also override {@link ITypeWrapper#renderIcon}
		 */
		default boolean hasIcon() {
			return false;
		}
		
		/**
		 * Only queried if {@link ITypeWrapper#hasIcon} returns true
		 * @return The height reserved for the icon of this type
		 */
		default int getIconHeight() {
			return 20;
		}
		
		/**
		 * Only queried if {@link ITypeWrapper#hasIcon()} returns true.
		 * @return The width reserved for the icons of this type.
		 *         Defaults to {@link ITypeWrapper#getIconHeight()}.
		 */
		default int getIconWidth() {
			return getIconHeight();
		}
		
		/**
		 * Render the icon for an element, by default calls {@link ITypeWrapper#getIcon}
		 * and renders it if present.
		 */
		default void renderIcon(
		  @Nullable T element, String text, @NotNull MatrixStack mStack, int x, int y, int w, int h,
		  int mouseX, int mouseY, float delta
		) {
			final Optional<Icon> opt = getIcon(element, text);
			opt.ifPresent(icon -> icon.renderCentered(mStack, x, y, w, h));
		}
		
		/**
		 * Get the icon of an element.<br>
		 * Implementations may alternatively override
		 * {@link ITypeWrapper#renderIcon} directly.
		 *
		 * @param element The element being rendered, possibly null.
		 * @param text The text written by the user, possibly not matching
		 *             any valid element if element is null.
		 */
		default Optional<Icon> getIcon(@Nullable T element, String text) {
			return Optional.empty();
		}
		
		/**
		 * Parse an element from its string representation, if possible
		 * @return A pair containing the parsed element (or empty)
		 *         and an optional parse error message
		 */
		Pair<Optional<T>, Optional<ITextComponent>> parseElement(@NotNull String text);
		
		/**
		 * Get the display name of the element.<br>
		 * It should have the same text as the lookup name,
		 * otherwise the lookup will use the string name.
		 */
		ITextComponent getDisplayName(@NotNull T element);
		
		/**
		 * Get a string name of the element to be used
		 * for query lookup by the {@link ISortedSuggestionProvider}<br>
		 * Should have the same text as the namme returned by {@link ITypeWrapper#getDisplayName}
		 */
		default String getName(@NotNull T element) {
			return STYLE_COMPONENT.matcher(getDisplayName(element).getString()).replaceAll("");
		}
	}
	
	private static final Pattern STYLE_COMPONENT = Pattern.compile("§[0-9a-f]");
	
	public static class StringTypeWrapper implements ITypeWrapper<String> {
		@Override public Pair<Optional<String>, Optional<ITextComponent>> parseElement(@NotNull String text) {
			return Pair.of(Optional.of(text), Optional.empty());
		}
		
		@Override public ITextComponent getDisplayName(@NotNull String element) {
			return new StringTextComponent(element);
		}
	}
	
	public static class PatternTypeWrapper implements ITypeWrapper<Pattern> {
		
		protected int flags;
		
		public PatternTypeWrapper() {
			this(0);
		}
		
		public PatternTypeWrapper(int flags) {
			this.flags = flags;
		}
		
		@Override public Pair<Optional<Pattern>, Optional<ITextComponent>> parseElement(
		  @NotNull String text
		) {
			try {
				return Pair.of(Optional.of(Pattern.compile(text, flags)), Optional.empty());
			} catch (PatternSyntaxException e) {
				return Pair.of(Optional.empty(), Optional.of(new StringTextComponent(e.getLocalizedMessage())));
			}
		}
		
		@Override public ITextComponent getDisplayName(@NotNull Pattern element) {
			return new StringTextComponent(element.pattern());
		}
	}
	
	public static class ResourceLocationTypeWrapper implements ITypeWrapper<ResourceLocation> {
		
		boolean hasIcon = false;
		int iconHeight = 20;
		int iconWidth = 20;
		
		public ResourceLocationTypeWrapper() {}
		
		protected ResourceLocationTypeWrapper(int iconSize) {
			this(iconSize, iconSize);
		}
		
		protected ResourceLocationTypeWrapper(int iconWidth, int iconHeight) {
			hasIcon = true;
			this.iconHeight = iconHeight;
			this.iconWidth = iconWidth;
		}
		
		@Override public boolean hasIcon() {
			return hasIcon;
		}
		
		@Override public int getIconHeight() {
			return hasIcon ? iconHeight : ITypeWrapper.super.getIconHeight();
		}
		
		@Override public int getIconWidth() {
			return hasIcon ? iconWidth : ITypeWrapper.super.getIconWidth();
		}
		
		@Override public Pair<Optional<ResourceLocation>, Optional<ITextComponent>> parseElement(
		  @NotNull String text
		) {
			try {
				return Pair.of(Optional.of(new ResourceLocation(text)), Optional.empty());
			} catch (ResourceLocationException e) {
				return Pair.of(Optional.empty(), Optional.of(new StringTextComponent(e.getLocalizedMessage())));
			}
		}
		
		@Override public ITextComponent getDisplayName(@NotNull ResourceLocation element) {
			if (element.getNamespace().equals("minecraft"))
				return new StringTextComponent(element.getPath());
			return new StringTextComponent(element.getNamespace()).withStyle(TextFormatting.GRAY)
			  .append(new StringTextComponent(":").withStyle(TextFormatting.GRAY))
			  .append(new StringTextComponent(element.getPath()).withStyle(TextFormatting.WHITE));
		}
		
		@Override public String getName(@NotNull ResourceLocation element) {
			return element.getNamespace().equals("minecraft") ? element.getPath() : element.toString();
		}
	}
	
	public static class ItemNameTypeWrapper extends ResourceLocationTypeWrapper {
		public ItemNameTypeWrapper() {
			super(20);
		}
		
		@Override public void renderIcon(
		  @Nullable ResourceLocation element, String text, @NotNull MatrixStack mStack,
		  int x, int y, int w, int h, int mouseX,
		  int mouseY, float delta
		) {
			final Optional<Item> opt = Registry.ITEM.getOptional(element);
			if (opt.isPresent()) {
				Minecraft.getInstance().getItemRenderer()
				  .renderGuiItem(new ItemStack(opt.get()), x + 2, y + 2);
			} else ICON_UNKNOWN.renderCentered(mStack, x, y, w, h);
		}
	}
	
	public static class BlockNameTypeWrapper extends ResourceLocationTypeWrapper {
		public BlockNameTypeWrapper() {
			super(20);
		}
		
		@Override public void renderIcon(
		  @Nullable ResourceLocation element, String text, @NotNull MatrixStack mStack,
		  int x, int y, int w, int h, int mouseX, int mouseY, float delta
		) {
			final Optional<Block> opt = Registry.BLOCK.getOptional(element);
			if (opt.isPresent()) {
				Minecraft.getInstance().getItemRenderer()
				  .renderGuiItem(new ItemStack(opt.get()), x + 2, y + 2);
			} else ICON_UNKNOWN.renderCentered(mStack, x, y, w, h);
		}
	}
	
	public static class FluidNameTypeWrapper extends ResourceLocationTypeWrapper {
		public FluidNameTypeWrapper() {
			super(20);
		}
		
		@Override public void renderIcon(
		  @Nullable ResourceLocation element, String text, @NotNull MatrixStack mStack,
		  int x, int y, int w, int h,
		  int mouseX, int mouseY, float delta
		) {
			final Optional<Fluid> opt = Registry.FLUID.getOptional(element);
			if (opt.isPresent()) {
				Minecraft.getInstance().getItemRenderer().renderGuiItem(
				  new ItemStack(opt.get().getBucket()), x + 2, y + 2);
			} else ICON_UNKNOWN.renderCentered(mStack, x, y, w, h);
		}
	}
	
	public static abstract class RegistryObjectTypeWrapper<T> implements ITypeWrapper<T> {
		protected boolean hasIcon = false;
		protected int iconSize = 20;
		
		protected RegistryObjectTypeWrapper() {}
		protected RegistryObjectTypeWrapper(int iconSize) {
			hasIcon = true;
			this.iconSize = iconSize;
		}
		
		@Override public boolean hasIcon() {
			return hasIcon;
		}
		
		@Override public int getIconHeight() {
			return iconSize;
		}
		
		protected abstract ResourceLocation getRegistryName(@NotNull T element);
		protected abstract @Nullable T getFromRegistryName(@NotNull ResourceLocation name);
		protected abstract ITextComponent getUnknownError(ResourceLocation name);
		
		@Override public String getName(@NotNull T element) {
			final ResourceLocation name = getRegistryName(element);
			return name.getNamespace().equals("minecraft") ? name.getPath() : name.toString();
		}
		
		@Override public Pair<Optional<T>, Optional<ITextComponent>> parseElement(
		  @NotNull String text
		) {
			try {
				final ResourceLocation name = new ResourceLocation(text);
				final T element = getFromRegistryName(name);
				if (element != null)
					return Pair.of(Optional.of(element), Optional.empty());
				return Pair.of(Optional.empty(), Optional.of(getUnknownError(name)));
			} catch (ResourceLocationException e) {
				return Pair.of(Optional.empty(), Optional.of(new StringTextComponent(e.getLocalizedMessage())));
			}
		}
		
		@Override public ITextComponent getDisplayName(@NotNull T element) {
			final ResourceLocation name = getRegistryName(element);
			if (name.getNamespace().equals("minecraft"))
				return new StringTextComponent(name.getPath());
			return new StringTextComponent(name.getNamespace()).withStyle(TextFormatting.GRAY)
			  .append(new StringTextComponent(":").withStyle(TextFormatting.GRAY))
			  .append(new StringTextComponent(name.getPath()).withStyle(TextFormatting.WHITE));
		}
	}
	
	public static class ItemTypeWrapper extends RegistryObjectTypeWrapper<Item> {
		public ItemTypeWrapper() {
			super(20);
		}
		
		@Override protected ResourceLocation getRegistryName(@NotNull Item element) {
			return element.getRegistryName();
		}
		
		@Override protected @Nullable Item getFromRegistryName(@NotNull ResourceLocation name) {
			return Registry.ITEM.getOptional(name).orElse(null);
		}
		
		@Override protected ITextComponent getUnknownError(ResourceLocation name) {
			return new TranslationTextComponent("argument.item.id.invalid", name);
		}
		
		@Override public void renderIcon(
		  @Nullable Item element, String text, @NotNull MatrixStack mStack, int x, int y,
		  int w, int h, int mouseX, int mouseY, float delta
		) {
			if (element != null) {
				Minecraft.getInstance().getItemRenderer()
				  .renderGuiItem(new ItemStack(element), x + 2, y + 2);
			} else if (!text.isEmpty()) ICON_ERROR.renderCentered(mStack, x, y, w, h);
		}
	}
	
	public static class BlockTypeWrapper extends RegistryObjectTypeWrapper<Block> {
		public BlockTypeWrapper() {
			super(20);
		}
		
		@Override protected ResourceLocation getRegistryName(@NotNull Block element) {
			return element.getRegistryName();
		}
		
		@Override protected @Nullable Block getFromRegistryName(@NotNull ResourceLocation name) {
			return Registry.BLOCK.getOptional(name).orElse(null);
		}
		
		@Override protected ITextComponent getUnknownError(ResourceLocation name) {
			return new TranslationTextComponent("argument.block.id.invalid", name);
		}
		
		@Override public void renderIcon(
		  @Nullable Block element, String text, @NotNull MatrixStack mStack, int x, int y,
		  int w, int h, int mouseX, int mouseY, float delta
		) {
			if (element != null) {
				Minecraft.getInstance().getItemRenderer().renderGuiItem(new ItemStack(element), x + 2, y + 2);
			} else if (!text.isEmpty()) ICON_ERROR.renderCentered(mStack, x, y, w, h);
		}
	}
	
	public static class FluidTypeWrapper extends RegistryObjectTypeWrapper<Fluid> {
		public FluidTypeWrapper() {
			super(20);
		}
		
		@Override protected ResourceLocation getRegistryName(@NotNull Fluid element) {
			return element.getRegistryName();
		}
		
		@Override protected @Nullable Fluid getFromRegistryName(@NotNull ResourceLocation name) {
			return Registry.FLUID.getOptional(name).orElse(null);
		}
		
		@Override protected ITextComponent getUnknownError(ResourceLocation name) {
			return new TranslationTextComponent("argument.fluid.id.invalid", name);
		}
		
		@Override public void renderIcon(
		  @Nullable Fluid element, String text, @NotNull MatrixStack mStack, int x, int y,
		  int w, int h, int mouseX, int mouseY, float delta
		) {
			if (element != null) {
				Minecraft.getInstance().getItemRenderer().renderGuiItem(
				  new ItemStack(element.getBucket()), x + 2, y + 2);
			} else if (!text.isEmpty()) ICON_ERROR.renderCentered(mStack, x, y, w, h);
		}
	}
}
