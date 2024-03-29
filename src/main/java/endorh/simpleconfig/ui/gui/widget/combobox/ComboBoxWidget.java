package endorh.simpleconfig.ui.gui.widget.combobox;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.GlStateManager.LogicOp;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.api.ui.TextFormatter;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons;
import endorh.simpleconfig.api.ui.math.Point;
import endorh.simpleconfig.api.ui.math.Rectangle;
import endorh.simpleconfig.ui.api.*;
import endorh.simpleconfig.ui.api.IOverlayCapableContainer.IOverlayRenderer;
import endorh.simpleconfig.ui.gui.widget.ToggleAnimator;
import endorh.simpleconfig.ui.gui.widget.combobox.wrapper.TypeWrapper;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static endorh.simpleconfig.api.SimpleConfigTextUtil.subText;
import static java.lang.Math.*;

public class ComboBoxWidget<T> extends AbstractWidget implements IOverlayRenderer {
	@Internal protected IOverlayCapableContainer screen = null;
	protected Supplier<IOverlayCapableContainer> screenSupplier;
	protected final @NotNull TypeWrapper<T> typeWrapper;
	protected int focusedBorderColor = 0xFFFFFFFF;
	protected int borderColor = 0xFFA0A0A0;
	protected int backgroundColor = 0xFF000000;
	protected boolean dropDownShown = false;
	protected boolean pendingDropDownShownChange = false;
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
	protected ToggleAnimator expandAnimator = new ToggleAnimator(250L);
	protected boolean showHelpTooltips = true;
	protected @Nullable TooltipRenderer tooltipConsumer = null;
	protected @Nullable Component hint = null;
	
	protected @NotNull IComboBoxModel<T> suggestionProvider;
	
	protected final Font font;
	/** Has the current text being edited on the textbox. */
	protected String text = "";
	protected T autoCompleteValue = null;
	protected @Nullable T value = null;
	protected @Nullable Component parseError = null;
	private boolean isEnabled = true;
	protected int maxLength = 32;
	protected long lastInteraction;
	protected long lastClick;
	protected boolean shouldDrawBackground = true;
	/** if true the textbox can lose focus by clicking elsewhere on the screen */
	protected boolean canLoseFocus = true;
	protected boolean canShowDropDown = true;
	/** The current character index that should be used as start of the rendered text. */
	protected int hScroll;
	protected int caretPos;
	/** other selection position, maybe the same as the cursor */
	protected int anchorPos;
	protected int lastClickWordPos = -1;
	protected boolean draggingText = false;
	protected int enabledColor = 0xe0e0e0;
	protected int disabledColor = 0x707070;
	protected Consumer<String> textListener;
	protected Consumer<T> valueListener;
	// protected CompletableFuture<List<T>> futureSuggestions;
	protected List<T> lastSuggestions = Lists.newArrayList();
	protected List<T> lastSortedSuggestions = Lists.newArrayList();
	protected String lastQuery = "";
	protected List<Component> decoratedSuggestions = Lists.newArrayList();
	/** Called to check if the text is valid */
	protected Predicate<String> filter = Objects::nonNull;
	protected TextFormatter formatter = TextFormatter.DEFAULT;
	protected Rectangle area = new Rectangle();
	protected Rectangle dropDownRectangle = new Rectangle();
	protected Rectangle reportedDropDownRectangle = new Rectangle();
	protected long lastDropDownScroll = 0;
	protected int suggestionCursor = -1;
	
	public ComboBoxWidget(
	  @NotNull TypeWrapper<T> typeWrapper, @NotNull Supplier<IOverlayCapableContainer> screen,
	  int x, int y, int width, int height
	) { this(typeWrapper, screen, x, y, width, height, GameNarrator.NO_TITLE); }
	
	public ComboBoxWidget(
	  @NotNull TypeWrapper<T> typeWrapper, @NotNull Supplier<IOverlayCapableContainer> screen,
	  int x, int y, int width, int height, @NotNull Component title
	) { this(typeWrapper, screen, Minecraft.getInstance().font, x, y, width, height, title); }
	
	public ComboBoxWidget(
	  @NotNull TypeWrapper<T> typeWrapper, @NotNull Supplier<IOverlayCapableContainer> screen,
	  @NotNull Font font, int x, int y, int width, int height, @NotNull Component title
	) {
		super(x, y, width, height, title);
		this.typeWrapper = typeWrapper;
		TextFormatter formatter = typeWrapper.getTextFormatter();
		if (formatter != null) this.formatter = formatter;
		screenSupplier = screen;
		this.font = font;
		suggestionProvider = new SimpleComboBoxModel<>(
		  Lists.newArrayList());
	}
	
	protected @NotNull IOverlayCapableContainer getScreen() {
		IOverlayCapableContainer screen = this.screen;
		if (screen == null) {
			if (screenSupplier != null) {
				screen = screenSupplier.get();
				this.screen = screen;
				screenSupplier = null;
				return screen;
			}
			throw new IllegalStateException("Missing screen for combo box widget");
		}
		return screen;
	}
	
	public void setSuggestionProvider(@NotNull IComboBoxModel<T> provider) {
		suggestionProvider = provider;
		updateSuggestions();
		// onTextChanged(text);
	}
	
	public void setSuggestions(List<T> suggestions) {
		setSuggestionProvider(new SimpleComboBoxModel<>(suggestions));
	}
	
	public @Nullable Component getHint() {
		return hint;
	}
	
	public ComboBoxWidget<T> setHint(@Nullable Component hint) {
		this.hint = hint;
		return this;
	}
	
	/**
	 * Read only rectangle.
	 */
	public Rectangle getArea() {
		return area;
	}
	
	public boolean isShowHelpTooltips() {
		return showHelpTooltips;
	}
	
	public void setShowHelpTooltips(boolean show) {
		showHelpTooltips = show;
	}
	
	public void setTooltipRenderer(@Nullable TooltipRenderer consumer) {
		tooltipConsumer = consumer;
	}
	
	@Override public boolean renderOverlay(
		GuiGraphics gg, Rectangle area, int mouseX, int mouseY, float delta
	) {
		if (!isDropDownShown())
			return false;

		PoseStack mStack = gg.pose();
		mStack.pushPose();{
			final int maxScroll = getMaxDropDownScroll();
			final double prev = dropDownScroll;
			dropDownScroll = ScrollingHandler.handleScrollingPosition(
			  new double[]{dropDownScrollTarget}, dropDownScroll, Double.POSITIVE_INFINITY,
			  0, scrollAnimationStart, scrollAnimationDuration);
			if (dropDownScroll > maxScroll && dropDownScroll > prev)
				dropDownScroll = maxScroll;
			if (dropDownScroll < 0 && prev > dropDownScroll)
				dropDownScroll = 0;
			
			area = dropDownRectangle;
			final int borderColor = isFocused() ? focusedBorderColor : this.borderColor;
			gg.fill(area.x, area.y, area.getMaxX(), area.getMaxY(), borderColor);
			gg.fill(area.x + 1, area.y, area.getMaxX() - 1, area.getMaxY() - 1, backgroundColor);
			gg.fill(
			  area.x + 1, area.y, area.getMaxX() - 1, area.y + 1,
			  borderColor & 0xFFFFFF | borderColor / 2 & 0xFF000000);
			
			int suggestionWidth = area.width - 2;
			final boolean showScrollbar = maxScroll > 0;
			if (showScrollbar)
				suggestionWidth -= 5;
			
			if (!lastSortedSuggestions.isEmpty()) {
				final int suggestionHeight = getSuggestionHeight();
				int firstIdx =
				  (int) Mth.clamp(dropDownScroll / suggestionHeight, 0, lastSortedSuggestions.size() - 1);
				int lastIdx =
				  (int) Mth.clamp((dropDownScroll + dropDownHeight + suggestionHeight - 1) / suggestionHeight, 0, lastSortedSuggestions.size() - 1);
				
				int yy = area.y + 1 - (int) dropDownScroll % suggestionHeight;
				for (int i = firstIdx; i <= lastIdx; i++) {
					renderSuggestion(
					  i, lastSortedSuggestions.get(i), gg, area.x + 1, yy,
					  suggestionWidth, suggestionHeight, mouseX, mouseY, delta, i == suggestionCursor);
					yy += suggestionHeight;
				}
				
				if (showScrollbar) {
					final int scrollBarX = area.getMaxX() - 6;
					gg.fill(scrollBarX, area.y + 1, scrollBarX + 5, area.getMaxY(), 0x80646464);
					int thumbHeight =
					  max(20, (area.height - 2) / (lastSortedSuggestions.size() * suggestionHeight));
					int thumbY = area.y + 1 + (int) round(
					  (area.height - 2 - thumbHeight) * (dropDownScroll / maxScroll));
					int thumbColor = draggingDropDownScrollBar
						|| mouseX >= scrollBarX && mouseX < scrollBarX + 5
						&& mouseY >= thumbY && mouseY < thumbY + thumbHeight
					? 0x96BDBDBD : 0x96808080;
					gg.fill(
					  scrollBarX, thumbY, area.getMaxX() - 1, thumbY + thumbHeight, thumbColor);
				}
			} else {
				final Optional<Component> opt = suggestionProvider.getPlaceHolder(typeWrapper, text);
				if (opt.isPresent()) {
					drawTextComponent(
					  opt.get(), gg, area.x + 4, area.y + 2,
					  area.width - 2, 10, 0xFFE0E0E0);
				} else setDropDownShown(false);
			}
			
			gg.fill(area.x + 1, area.y, area.getMaxX() - 1, area.y + 1, borderColor);
			gg.fill(area.x + 1, area.getMaxY() - 1, area.getMaxX() - 1, area.getMaxY(), borderColor);
			
		} mStack.popPose();
		return true;
	}
	
	protected void renderSuggestion(
		int i, T suggestion, GuiGraphics gg, int x, int y, int w, int h,
		int mouseX, int mouseY, float delta, boolean selected
	) {
		boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
		if (selected) {
			gg.fill(x, y, x + w, y + 1, 0xA0E0E0E0);
			gg.fill(x, y + h - 1, x + w, y + h, 0xA0E0E0E0);
			gg.fill(x, y + 1, x + 1, y + h - 1, 0xA0E0E0E0);
			gg.fill(x + w - 1, y + 1, x + w, y + h - 1, 0xA0E0E0E0);
			gg.fill(x + 1, y + 1, x + w - 1, y + h - 1, hovered? 0x80646464 : 0x80484848);
		} else gg.fill(x, y, x + w, y + h, hovered? 0x80646464 : 0x80242424);
		
		int textX = x + 2;
		int textY = y;
		final int iconHeight = getIconHeight();
		final int iconWidth = getIconWidth();
		if (typeWrapper.hasIcon()) {
			typeWrapper.renderIcon(
			  suggestion, typeWrapper.getName(suggestion), gg, x, y,
			  iconWidth, min(iconHeight, h), 200, mouseX, mouseY, delta);
			textX += iconWidth;
			textY += (iconHeight - 10) / 2;
		}
		
		final Component name =
		  i >= 0 && i < decoratedSuggestions.size()
		  ? decoratedSuggestions.get(i) : typeWrapper.getDisplayName(suggestion);
		
		int textW = w - 4;
		if (hasIcon())
			textW -= iconWidth;
		
		drawTextComponent(
		  name, gg, textX, textY + 1, textW, 10, 0xffffffff);
	}
	
	protected void drawTextComponent(
		Component component, GuiGraphics gg, int x, int y, int w, int h, int color
	) {
		final List<FormattedCharSequence> processors = font.split(component, w);
		if (!processors.isEmpty())
			gg.drawString(font, processors.get(0), x, y, color); // TODO: shadow
	}
	
	@Override public boolean overlayMouseScrolled(Rectangle area, double mouseX, double mouseY, double amount) {
		final double lastScroll = dropDownScroll;
		final double lastTarget = dropDownScrollTarget;
		final long current = System.currentTimeMillis();
		scrollBy(-amount * min(24, getSuggestionHeight()), abs(amount) >= 1.0);
		// setDropDownScroll((int) round(lastScroll - amount * 10));
		if (lastTarget != dropDownScrollTarget || lastScroll != dropDownScroll || current - lastDropDownScroll <= 100) {
			lastDropDownScroll = current;
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
	
	@Override public boolean overlayMouseClicked(Rectangle area, double mouseX, double mouseY, int button) {
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
	  Rectangle area, double mouseX, double mouseY, int button, double dragX, double dragY
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
	
	@Override public void overlayMouseDragEnd(Rectangle area, double mouseX, double mouseY, int button) {
		draggingDropDownScrollBar = false;
	}
	
	@Override public boolean overlayEscape() {
		if (isDropDownShown()) {
			setDropDownShown(false);
			playFeedbackTap(1F);
			return true;
		}
		return false;
	}
	
	public boolean isRestrictedToSuggestions() {
		return restrictToSuggestions;
	}
	
	public void setRestrictedToSuggestions(boolean restrict) {
		restrictToSuggestions = restrict;
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
		setDropDownShown(expanded, false);
	}

	public void setDropDownShown(boolean expanded, boolean lease) {
		if (!canShowDropDown())
			expanded = false;
		if (lease) {
			pendingDropDownShownChange = dropDownShown != expanded;
		} else {
			dropDownShown = expanded;
			pendingDropDownShownChange = false;
			expandAnimator.setEaseOutTarget(expanded);
			suggestionCursor = -1;
			if (expanded) {
				getScreen().addOverlay(reportedDropDownRectangle, this, 100);
			} else {
				setDropDownScroll(0);
				draggingDropDownScrollBar = false;
			}
		}
	}
	
	public List<T> getShownSuggestions() {
		return Collections.unmodifiableList(lastSortedSuggestions);
	}
	
	public int getDropDownHeight() {
		return (int) (expandAnimator.getEaseOut() * (
		  lastSortedSuggestions.isEmpty() ? 12 : min(
		  dropDownHeight, getSuggestionHeight() * lastSortedSuggestions.size() + 2)
		));
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
		dropDownScroll = Mth.clamp(scroll, 0, getMaxDropDownScroll());
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
			dropDownScrollTarget = Mth.clamp(scroll, -32, getMaxDropDownScroll() + 32);
			scrollAnimationStart = System.currentTimeMillis();
		} else {
			dropDownScroll = dropDownScrollTarget = Mth.clamp(scroll, 0, getMaxDropDownScroll());
			scrollAnimationStart = 0;
		}
	}
	
	public void setDropDownHeight(int height) {
		dropDownHeight = height;
	}
	
	public void setTextListener(Consumer<String> listener) {
		textListener = listener;
	}
	
	public void setValueListener(Consumer<T> listener) {
		valueListener = listener;
	}
	
	public void setTextFormatter(TextFormatter formatter) {
		this.formatter = formatter;
	}
	
	public void tick() {}
	
	@Override protected @NotNull MutableComponent createNarrationMessage() {
		Component msg = getMessage();
		// This should have its own key, but I think having it untranslated
		// is worse than reporting it as an edit box
		return Component.translatable("gui.narrate.editBox", msg, text);
	}
	
	/**
	 * Set the text of the textbox, and moves the cursor to the end.
	 */
	public void setText(String textIn) {
		if (filter.test(textIn)) {
			text = textIn.length() > maxLength ? textIn.substring(0, maxLength) : textIn;
			
			moveCaretToEnd();
			setAnchorPos(caretPos);
			onTextChanged(textIn);
		}
	}
	
	public void setValue(T value) {
		if (value != null) {
			setText(typeWrapper.getName(value));
		} else setText("");
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
	
	public boolean hasSelection() {
		return anchorPos != caretPos;
	}
	
	/**
	 * @return The selected text
	 */
	public String getSelectedText() {
		return caretPos < anchorPos
		       ? text.substring(caretPos, anchorPos)
		       : text.substring(anchorPos, caretPos);
	}
	
	public void setFilter(Predicate<String> validator) {
		filter = validator;
	}
	
	/**
	 * Adds the given text after the cursor, or replaces the currently
	 * selected text if the selection is not empty.
	 */
	public void insertText(String inserted) {
		if (formatter != null) inserted = formatter.stripInsertText(inserted);
		
		int start = min(caretPos, anchorPos);
		int end = max(caretPos, anchorPos);
		int allowed = maxLength - text.length() + (end - start);
		String txt = SharedConstants.filterText(inserted);
		int length = txt.length();
		if (allowed < length) {
			txt = txt.substring(0, allowed);
			length = allowed;
		}
		
		final String result = new StringBuilder(text).replace(start, end, txt).toString();
		if (filter.test(result)) {
			text = result;
			setCaretPosition(start + length);
			setAnchorPos(caretPos);
			onTextChanged(text);
			if (isAutoDropDown() && !isDropDownShown() && !lastSortedSuggestions.isEmpty())
				setDropDownShown(true);
		}
	}
	
	protected void onTextChanged(String newText) {
		if (textListener != null) textListener.accept(newText);
		if (isRestrictedToSuggestions() && isAutoDropDown())
			setDropDownShown(true);
		updateValue();
	}
	
	protected void updateValue() {
		if (!isRestrictedToSuggestions()) {
			final Pair<Optional<T>, Optional<Component>> parsed = typeWrapper.parseElement(text);
			value = parsed.getLeft().orElse(null);
			parseError = parsed.getRight().orElse(null);
			onValueChanged(value);
		}
		updateSuggestions();
	}
	
	protected void updateSuggestions() {
		String query = text;
		final Optional<List<T>> opt = suggestionProvider.updateSuggestions(typeWrapper, query);
		if (opt.isPresent()) {
			List<T> suggestions = opt.get();
			boolean changed = !suggestions.equals(lastSuggestions);
			if (changed) lastSuggestions = suggestions;
			if (!query.equals(lastQuery) || changed) {
				final Pair<List<T>, List<Component>> pair =
				  suggestionProvider.pickAndDecorateSuggestions(typeWrapper, query, lastSuggestions);
				lastSortedSuggestions = pair.getLeft();
				decoratedSuggestions = pair.getRight();
				lastQuery = query;
				setDropDownScroll(0);
			}
		}
	}
	
	protected void onValueChanged(T newValue) {
		if (valueListener != null)
			valueListener.accept(newValue);
	}
	
	protected void delete(int num) {
		if (hasSelection()) {
			insertText("");
		} else if (Screen.hasControlDown()) {
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
			if (hasSelection()) {
				insertText("");
			} else deleteFromCaret(getWordPosFromCaret(num) - caretPos);
		}
	}
	
	/**
	 * Delete the given number of characters from the current cursor's position, unless there is currently a selection,
	 * in which case the selection is deleted instead.
	 */
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
	
	/**
	 * Get the starting index of the word at the specified number of words away from the cursor position.
	 */
	public int getWordPosFromCaret(int numWords) {
		return getWordPosFromPos(numWords, getCaret());
	}
	
	
	private static final Pattern WORD_BREAK_RIGHT_PATTERN = Pattern.compile(
	  "(?<=\\p{Alnum})(?=\\P{Alnum})" +
	  "|(?<=\\p{Alnum})(?=\\p{Lu}[\\p{Ll}\\d])" +
	  "|(?<=[\\p{Ll}\\d])(?=\\p{Lu})" +
	  "|(?<=[^\\p{Alnum}\\s_])(?=[\\p{Alnum}\\s_])");
	private static final Pattern WORD_BREAK_LEFT_PATTERN = Pattern.compile(
	  "(?<=\\p{Alnum})(?=\\P{Alnum})" +
	  "|(?<=[\\p{Ll}\\d]\\p{Lu})(?=\\p{Lu})" +
	  "|(?<=\\p{Lu})(?=[\\p{Ll}\\d])" +
	  "|(?<=[^\\p{Alnum}\\s_])(?=[\\p{Alnum}\\s_])");
	/**
	 * Get the starting index of the word at a distance of the specified number of words away
	 * from the given position.
	 */
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
	
	/**
	 * Moves the text cursor by a specified number of characters and clears the selection
	 */
	public void moveCaretBy(int num) {
		moveCaret(expandLigaturesFromCaret(num));
	}
	
	protected int expandLigaturesFromCaret(int chars) {
		return Util.offsetByCodepoints(text, caretPos, chars);
	}
	
	public void selectAll() {
		setSelection(0, text.length());
	}
	
	public void setSelection(int anchor, int caret) {
		moveCaret(caret);
		setAnchorPos(anchor);
	}
	
	/**
	 * Set the current position of the cursor.
	 */
	public void moveCaret(int pos) {
		setCaretPosition(pos);
		if (!Screen.hasShiftDown())
			setAnchorPos(caretPos);
		onTextChanged(text);
	}
	
	public void moveCaretWithAnchor(int pos) {
		moveCaret(pos);
		setAnchorPos(pos);
	}
	
	public void setCaretPosition(int pos) {
		caretPos = Mth.clamp(pos, 0, text.length());
	}
	
	/**
	 * Moves the cursor to the start of this text box.
	 */
	public void moveCaretToStart() {
		moveCaret(0);
	}
	
	/**
	 * Moves the cursor to the end of this text box.
	 */
	public void moveCaretToEnd() {
		moveCaret(text.length());
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
		if (!canConsumeInput()) {
			return false;
		} else {
			lastInteraction = System.currentTimeMillis();
			if (Screen.isSelectAll(keyCode)) {
				moveCaretToEnd();
				setAnchorPos(0);
				return true;
			} else {
				final KeyboardHandler kl = Minecraft.getInstance().keyboardHandler;
				if (Screen.isCopy(keyCode)) {
					kl.setClipboard(getSelectedText());
					playFeedbackTap(1F);
					return true;
				} else if (Screen.isPaste(keyCode)) {
					if (isEditable())
						insertText(kl.getClipboard());
					playFeedbackTap(1F);
					return true;
				} else if (Screen.isCut(keyCode)) {
					kl.setClipboard(getSelectedText());
					if (isEditable())
						insertText("");
					playFeedbackTap(1F);
					return true;
				} else {
					switch(keyCode) {
						case GLFW.GLFW_KEY_BACKSPACE:
							if (isEditable()) delete(-1);
							return true;
						case GLFW.GLFW_KEY_DELETE:
							if (isEditable()) delete(1);
							return true;
						case GLFW.GLFW_KEY_DOWN:
							moveSuggestionCursor(1);
							return true;
						case GLFW.GLFW_KEY_UP:
							moveSuggestionCursor(-1);
							return true;
						case GLFW.GLFW_KEY_ENTER:
							autoComplete();
							playFeedbackTap(1F);
							return true;
						case GLFW.GLFW_KEY_PAGE_UP:
							moveSuggestionCursor(-max(1, dropDownHeight / getSuggestionHeight() - 1));
							return true;
						case GLFW.GLFW_KEY_PAGE_DOWN:
							moveSuggestionCursor(max(1, dropDownHeight / getSuggestionHeight() - 1));
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
						default:
							return false;
					}
				}
			}
		}
	}
	
	private void playFeedbackTap(float volume) {
		Minecraft.getInstance().getSoundManager().play(
		  SimpleSoundInstance.forUI(SimpleConfigMod.UI_TAP, volume));
	}
	
	private void playFeedbackClick(float volume) {
		Minecraft.getInstance().getSoundManager().play(
		  SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, volume));
	}
	
	public void moveSuggestionCursor(int step) {
		if (!isDropDownShown())
			setDropDownShown(true);
		int prev = suggestionCursor;
		suggestionCursor = Mth.clamp(suggestionCursor + step, 0, lastSortedSuggestions.size() - 1);
		// Ensure visible
		final int suggestionHeight = getSuggestionHeight();
		int firstIdx = (int) Mth.clamp(dropDownScroll / suggestionHeight, 0, lastSortedSuggestions.size() - 1);
		int lastIdx = (int) Mth.clamp((dropDownScroll + dropDownHeight + suggestionHeight - 1) / suggestionHeight, 0, lastSortedSuggestions.size() - 1);
		
		final long t = System.currentTimeMillis();
		// Do not animate for very fast movement for better readability
		final boolean animated = t - lastSuggestionCursorNavigation >= 60;
		if (suggestionCursor <= firstIdx)
			scrollTo(suggestionHeight * (suggestionCursor - 0.5), animated);
		else if (suggestionCursor >= lastIdx - 1)
			scrollTo(suggestionHeight * (suggestionCursor + 1.8) - dropDownHeight, animated);
		lastSuggestionCursorNavigation = t;
		if (prev != suggestionCursor)
			playFeedbackTap(1F);
	}
	
	@SuppressWarnings("BooleanMethodIsAlwaysInverted") public boolean canConsumeInput() {
		return isVisible() && isFocused() && isEnabled();
	}
	
	@Override public boolean charTyped(char codePoint, int modifiers) {
		if (!canConsumeInput()) {
			return false;
		} else if ( codePoint == GLFW.GLFW_KEY_SPACE &&(modifiers & 2) != 0) {
			autoComplete();
			playFeedbackTap(1F);
			return true;
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
		}
		return false;
	}
	
	@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!active) return false;
		if (isVisible()) {
			lastInteraction = System.currentTimeMillis();
			lastClickWordPos = -1;
			draggingText = false;
			
			// Click arrow
			if (isMouseOverArrow((int) round(mouseX), (int) round(mouseY))
			    || isMouseOverIcon((int) round(mouseX), (int) round(mouseY))) {
				if (!isFocused()) setFocused(true);
				setDropDownShown(!isDropDownShown());
				playFeedbackClick(1F);
				return true;
			}
			
			boolean hovered = isMouseOver(mouseX, mouseY);
			
			// Click dropdown box
			if (isRestrictedToSuggestions() && hovered) {
				if (!isFocused()) setFocused(true);
				if (isEnabled()) {
					final boolean wasShown = isDropDownShown();
					setDropDownShown(!wasShown);
					if (!wasShown) setText("");
					Minecraft.getInstance().getSoundManager().play(
					  SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1F));
				}
				return true;
			}
			
			if (!(canLoseFocus && isFocused()))
				setFocused(hovered);
			
			// Click text
			if (isFocused() && hovered && button == 0) {
				draggingText = true;
				double relX = mouseX - getX();
				if (hasIcon()) {
					relX -= getIconWidth() + 1;
				} else if (shouldDrawBackground()) relX -= 4;
				int clickedPos = getClickedCaretPos(subText(getDisplayedText(), hScroll), relX) + hScroll;
				if (lastInteraction - lastClick < 250) { // Double click;
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
			
			// Clear text
			if (isEnabled() && isFocused() && hovered && button == 1 && !isRestrictedToSuggestions()) {
				setText("");
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
			if (hasIcon()) {
				relX -= getIconWidth() + 1;
			} else if (shouldDrawBackground()) relX -= 4;
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
		return relX < (left + right) * 0.5? floor : floor + 1;
	}
	
	@Override public void setFocused(boolean focused) {
		boolean prev = isFocused();
		super.setFocused(focused);
		boolean actual = isFocused();
		if (prev != actual) onFocusedChanged(actual);
	}
	
	protected void updateDropDownRectangle() {
		dropDownRectangle.x = getX() - 1;
		dropDownRectangle.y = getY() + height;
		dropDownRectangle.width = width + 2;
		dropDownRectangle.height = getDropDownHeight();
		
		reportedDropDownRectangle.setBounds(
		  ScissorsHandler.INSTANCE.getScissorsAreas().stream()
		    .reduce(dropDownRectangle, Rectangle::intersection));
		if (reportedDropDownRectangle.isEmpty())
			reportedDropDownRectangle.setBounds(0, 0, 0, 0);
	}
	
	public MutableComponent getDisplayedText() {
		return formatter.formatText(getText());
	}
	
	@Override public void renderWidget(
		@NotNull GuiGraphics gg, int mouseX, int mouseY, float delta
	) {
		if (pendingDropDownShownChange)
			setDropDownShown(!dropDownShown);
		int x = getX();
		int y = getY();
		area.setBounds(x, y, width, height);
		if (!isVisible()) return;
		updateSuggestions();
		if (shouldDrawBackground()) {
			int color = isHoveredOrFocused() ? focusedBorderColor : borderColor;
			gg.fill(x - 1, y - 1, x + width + 1, y + height + 1, color);
			gg.fill(x, y, x + width, y + height, backgroundColor);
			if (!isRestrictedToSuggestions())
				gg.fill(x + width - arrowWidth - 1, y, x + width - arrowWidth, y + height, color);
		}
		
		if (!isFocused() || !canShowDropDown())
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
		final int innerWidth = getInnerWidth();
		final Component hint = getHint();
		int color = isEnabled() ? enabledColor : disabledColor;
		if (isFocused() || value == null && hint == null) {
			int relCaret = caretPos - hScroll;
			int relAnchor = anchorPos - hScroll;
			
			MutableComponent displayedText = subText(getDisplayedText(), hScroll);
			String shown = font.substrByWidth(displayedText, innerWidth).getString();
			int fitLength = shown.length();
			displayedText = subText(displayedText, 0, fitLength);
			
			boolean fitCaret = relCaret >= 0 && relCaret <= fitLength;
			boolean showCaret = isFocused() && fitCaret
			                    && (System.currentTimeMillis() - lastInteraction) % 1000 < 500;
			int caretX = fitCaret? textX + font.width(subText(displayedText, 0, relCaret)) - 1
			                     : relCaret > 0? textX + innerWidth - 1 : textX;
			int endTextX = textX;
			
			// Render text
			if (!shown.isEmpty())
				endTextX += gg.drawString(font, displayedText, textX, textY, color); // TODO: shadow
			
			// Render autocompletion
			if (isDropDownShown() && autoCompleteValue != null) {
				String autoComplete = typeWrapper.getName(autoCompleteValue);
				String shownAutocomplete =
				  autoComplete.startsWith(text)
				  ? autoComplete.substring(text.length()) : "→" + autoComplete;
				shownAutocomplete = font.plainSubstrByWidth(shownAutocomplete, innerWidth - endTextX + textX);
				gg.drawString(font, shownAutocomplete, endTextX, textY, 0x96808080); // TODO: shadow
			}
			
			// Render caret
			if (showCaret) {
				renderCaret(gg, caretX, textY - 2, 1, 12);
			}
			
			// Render selection
			if (relAnchor != relCaret) {
				if (relAnchor > fitLength) relAnchor = fitLength;
				int anchorX = textX + font.width(shown.substring(0, relAnchor));
				renderSelection(gg, caretX, textY - 3, anchorX - 1, textY + 2 + 9);
			}
		} else if (text.isEmpty() && value == null) {
			// Render hint
			drawTextComponent(hint.copy().withStyle(ChatFormatting.GRAY),
			  gg, textX, textY, innerWidth, 10, 0x96FFFFFF);
		} else if (value != null) {
			// Render value
			Component display = typeWrapper.getDisplayName(value);
			if (!isEnabled()) display = display.copy().withStyle(ChatFormatting.GRAY);
			drawTextComponent(display, gg, textX, textY, innerWidth, 10, 0xFFE0E0E0);
		} else {
			// Render text
			MutableComponent displayedText = subText(getDisplayedText(), hScroll);
			displayedText = subText(
			  displayedText, 0, font.substrByWidth(displayedText, innerWidth).getString().length());
			gg.drawString(font, displayedText, textX, textY, color); // TODO: shadow
		}
		
		if (hasIcon) {
			RenderSystem.setShaderColor(1F, 1F, 1F, isEnabled()? 1F : 0.7F);
			typeWrapper.renderIcon(
			  autoCompleteValue != null && isDropDownShown()? autoCompleteValue : value, text, gg,
			  iconX, iconY, iconWidth, iconHeight, 0, mouseX, mouseY, delta);
			RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
		}
		
		int arrowX = x + width - arrowWidth, arrowY = y;
		renderArrow(gg, arrowX, arrowY, arrowWidth, height, mouseX, mouseY, isRestrictedToSuggestions() && isHoveredOrFocused() ? x : arrowX);
		
		if (isShowHelpTooltips() && value != null
		    && isMouseOver(mouseX, mouseY) && !isMouseOverArrow(mouseX, mouseY)) {
			List<Component> tooltip = typeWrapper.getHelpTooltip(value);
			if (!tooltip.isEmpty()) {
				IOverlayCapableContainer screen = getScreen();
				if (tooltipConsumer != null) {
					tooltipConsumer.renderTooltip(tooltip, mouseX, mouseY);
				} else if (screen instanceof IMultiTooltipScreen ts) {
					ts.addTooltip(Tooltip.of(area, Point.of(mouseX, mouseY), tooltip));
				} else gg.renderComponentTooltip(font, tooltip, mouseX, mouseY);
			}
		}
	}
	
	protected void renderCaret(GuiGraphics gg, int x, int y, int w, int h) {
		Tesselator tessellator = Tesselator.getInstance();
		BufferBuilder bb = tessellator.getBuilder();
		RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
		RenderSystem.enableColorLogicOp();
		RenderSystem.logicOp(LogicOp.OR_REVERSE);
		Matrix4f m = gg.pose().last().pose();
		bb.begin(Mode.QUADS, DefaultVertexFormat.POSITION);
		bb.vertex(m,     x, y + h, 0F).endVertex();
		bb.vertex(m, x + w, y + h, 0F).endVertex();
		bb.vertex(m, x + w,     y, 0F).endVertex();
		bb.vertex(m,     x,     y, 0F).endVertex();
		tessellator.end();
		RenderSystem.disableColorLogicOp();
	}
	
	public boolean isMouseOverArrow(int mouseX, int mouseY) {
		int arrowX = getX() + width - arrowWidth, arrowY = getY();
		return mouseX >= arrowX && mouseX < arrowX + arrowWidth && mouseY >= arrowY && mouseY < arrowY + height;
	}
	
	public boolean isMouseOverIcon(int mouseX, int mouseY) {
		final int iconHeight = getIconHeight();
		int iconY = shouldDrawBackground? getY() + height / 2 - iconHeight / 2 : getY();
		return hasIcon() && mouseX >= getX() && mouseX < getX() + getIconWidth()
		       && mouseY >= iconY && mouseY < iconY + iconHeight;
	}
	
	protected void renderArrow(GuiGraphics gg, int x, int y, int w, int h, int mouseX, int mouseY, int backgroundX) {
		final boolean hovered = mouseX >= backgroundX && mouseX < x + w && mouseY >= y && mouseY < y + h;
		int arrowBackground = hovered ? 0x80646464 : 0x80242424;
		if (backgroundX == mouseX || hovered)
			gg.fill(backgroundX, y, x + w, y + h, arrowBackground);
		RenderSystem.setShaderColor(1F, 1F, 1F, canShowDropDown()? 1F : 0.7F);
		SimpleConfigIcons.ComboBox.DROP_DOWN_ARROW.renderCentered(
		  gg, x, y + (h - 10) / 2, 10, 10, isDropDownShown()? 1 : 0);
		RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
	}
	
	/**
	 * Draws the blue selection box.
	 */
	protected void renderSelection(GuiGraphics gg, int sX, int sY, int eX, int eY) {
		if (sX < eX) {
			final int swap = sX;
			sX = eX;
			eX = swap;
		}
		if (sY < eY) {
			final int swap = sY;
			sY = eY;
			eY = swap;
		}

		int x = getX();
		if (eX > x + width) eX = x + width;
		if (sX > x + width) sX = x + width;

		RenderSystem.enableColorLogicOp();
		RenderSystem.logicOp(LogicOp.XOR);
		gg.fill(sX, sY, eX, eY, 0xFF000080);
		RenderSystem.disableColorLogicOp();
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

	// @Override public boolean changeFocus(boolean focus) {
	// 	lastInteraction = System.currentTimeMillis();
	// 	return visible && isEnabled() && super.changeFocus(focus);
	// }
	
	@Override public boolean isMouseOver(double mouseX, double mouseY) {
		return visible && mouseX >= getX() && mouseX < getX() + width && mouseY >= getY() && mouseY < getY() + height;
	}

	protected void onFocusedChanged(boolean focused) {
		if (focused) {
			if (isRestrictedToSuggestions()) {
				setText("");
				if (isAutoDropDown())
					setDropDownShown(true);
			} else if (pendingDropDownShownChange && dropDownShown)
				setDropDownShown(true);
		} else {
			if (isRestrictedToSuggestions()) {
				if (value != null) {
					setText(typeWrapper.getName(value));
				} else setText("");
			}
			// The new AbstractContainerEventHandler impl calls `setFocused(false)` -> `setFocused(true)` on click,
			// so we only hide the drop-down on the next frame after `setFocused(false)`
			setDropDownShown(false, true);
		}
	}
	
	protected boolean isEnabled() {
		return isEnabled;
	}
	
	protected boolean isEditable() {
		return isEnabled();
	}
	
	public boolean canShowDropDown() {
		return canShowDropDown && isEnabled();
	}
	
	public void setCanShowDropDown(boolean canShowDropDown) {
		this.canShowDropDown = canShowDropDown;
		if (!canShowDropDown && isDropDownShown()) setDropDownShown(false);
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
	public int getInnerWidth() {
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
	public void setAnchorPos(int pos) {
		int i = text.length();
		anchorPos = Mth.clamp(pos, 0, i);
		if (font != null) {
			if (hScroll > i)
				hScroll = i;
			
			int j = getInnerWidth();
			String s = font.plainSubstrByWidth(text.substring(hScroll), j);
			int k = s.length() + hScroll;
			if (anchorPos == hScroll)
				hScroll -= font.plainSubstrByWidth(text, j, true).length();
			
			if (anchorPos > k) {
				// We can't assume the font is monospace (the default actually isn't)
				final String rev = new StringBuilder(text.substring(0, anchorPos)).reverse().toString();
				hScroll = anchorPos - font.plainSubstrByWidth(rev, j).length();
			} else if (anchorPos <= hScroll) {
				hScroll = anchorPos;
			}
			
			hScroll = Mth.clamp(hScroll, 0, i);
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
	public boolean isVisible() {
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
	@Deprecated public int getTextXFor(int pos) {
		return pos > text.length() ? getX() : getX() + font.width(text.substring(0, pos));
	}
	
	public @Nullable T getValue() {
		return value;
	}
	
	public Optional<Component> getError() {
		return Optional.ofNullable(parseError);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput out) {
		out.add(NarratedElementType.TITLE, Component.translatable("narration.edit_box", getValue()));
		// TODO: Narrate suggestions?
	}
}
