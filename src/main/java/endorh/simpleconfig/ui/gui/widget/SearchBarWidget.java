package endorh.simpleconfig.ui.gui.widget;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons.Buttons;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons.SearchBar;
import endorh.simpleconfig.api.ui.math.Point;
import endorh.simpleconfig.api.ui.math.Rectangle;
import endorh.simpleconfig.config.ClientConfig.advanced.search;
import endorh.simpleconfig.ui.api.IDialogCapableScreen;
import endorh.simpleconfig.ui.api.IOverlayCapableContainer.IOverlayRenderer;
import endorh.simpleconfig.ui.api.Tooltip;
import endorh.simpleconfig.ui.gui.WidgetUtils;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.Modifier;
import endorh.simpleconfig.ui.gui.widget.combobox.ComboBoxWidget;
import endorh.simpleconfig.ui.gui.widget.combobox.wrapper.PatternTypeWrapper;
import endorh.simpleconfig.ui.gui.widget.combobox.wrapper.StringTypeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FocusableGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class SearchBarWidget extends FocusableGui implements IOverlayRenderer {
	protected static ITextComponent[] CASE_SENSITIVE_TOOLTIP = {
	  new TranslationTextComponent("simpleconfig.ui.search.case_sensitive"),
	  new TranslationTextComponent("key.modifier.alt").appendString(" + C").mergeStyle(TextFormatting.GRAY)};
	protected static ITextComponent[] REGEX_TOOLTIP = new ITextComponent[] {
	  new TranslationTextComponent("simpleconfig.ui.search.regex"),
	  new TranslationTextComponent("key.modifier.alt").appendString(" + R").mergeStyle(TextFormatting.GRAY)};
	protected static ITextComponent[] FILTER_TOOLTIP = new ITextComponent[] {
	  new TranslationTextComponent("simpleconfig.ui.search.filter"),
	  new TranslationTextComponent("key.modifier.alt").appendString(" + F").mergeStyle(TextFormatting.GRAY)};
	
	public int x;
	public int y;
	public int w;
	public int h;
	
	protected IDialogCapableScreen screen;
	protected ISearchHandler handler;
	
	protected ComboBoxWidget<String> comboBox;
	protected ComboBoxWidget<Pattern> regexComboBox;
	protected MultiFunctionImageButton up;
	protected MultiFunctionImageButton down;
	protected MultiFunctionImageButton close;
	protected MultiFunctionImageButton open;
	protected List<ToggleImageButton> optionButtons;
	protected ToggleImageButton regexButton;
	protected ToggleImageButton caseButton;
	protected ToggleImageButton filterButton;
	protected List<IGuiEventListener> expandedListeners;
	protected List<IGuiEventListener> regexListeners;
	protected List<IGuiEventListener> closedListeners;
	
	protected boolean overMatch = false;
	protected int currentMatch = 0;
	protected int totalMatches = 0;
	
	protected boolean caseSensitive = search.search_case_sensitive;
	protected boolean regex = search.search_regex;
	protected boolean filter = search.search_filter;
	
	protected boolean closeOnClickOutside = false;
	protected boolean expanded = false;
	protected Rectangle overlay = new Rectangle();
	
	public SearchBarWidget(
	  ISearchHandler handler, int x, int y, int w, IDialogCapableScreen screen
	) {
		this(handler, x, y, w, 24, screen);
	}
	
	public SearchBarWidget(
	  ISearchHandler handler, int x, int y, int w, int h, IDialogCapableScreen screen
	) {
		this.handler = handler;
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		this.screen = screen;
		comboBox = new ComboBoxWidget<>(new StringTypeWrapper(), () -> screen, x + 36, y + 1, w - 108, 18);
		regexComboBox = new ComboBoxWidget<>(new PatternTypeWrapper(), () -> screen, x + 36, y + 1, w - 108, 18);
		comboBox.setMaxLength(99999);
		regexComboBox.setMaxLength(99999);
		comboBox.setValueListener(s -> makeQuery());
		regexComboBox.setValueListener(s -> makeQuery());
		comboBox.setAutoDropDown(false);
		regexComboBox.setAutoDropDown(false);
		comboBox.setSuggestions(search.search_history);
		regexComboBox.setSuggestions(search.regex_search_history);
		up = new MultiFunctionImageButton(
		  x + w - 68, y, 18, 18, Buttons.UP, ButtonAction.of(
			 b -> next(b == 1)
		)).on(Modifier.SHIFT, ButtonAction.of(b -> next(b != 1)).icon(Buttons.DOWN));
		down = new MultiFunctionImageButton(
		  x + w - 44, y, 18, 18, Buttons.DOWN, ButtonAction.of(
			 b -> next(b != 1)
		)).on(Modifier.SHIFT, ButtonAction.of(b -> next(b == 1)).icon(Buttons.UP));
		close = new MultiFunctionImageButton(
		  x + w - 20, y, 20, 20, Buttons.SEARCH_CLOSE, ButtonAction.of(this::close));
		open = new MultiFunctionImageButton(
		  x + 2, y, 20, 20, Buttons.SEARCH, ButtonAction.of(this::open));
		caseButton = ToggleImageButton.of(caseSensitive, 18, SearchBar.SEARCH_CASE_SENSITIVE, b -> updateModifiers());
		regexButton = ToggleImageButton.of(regex,  18, SearchBar.SEARCH_REGEX, b -> updateModifiers());
		filterButton = ToggleImageButton.of(filter, 18, SearchBar.SEARCH_FILTER, b -> updateModifiers());
		optionButtons = Lists.newArrayList(caseButton, regexButton, filterButton);
		expandedListeners = Lists.newArrayList(caseButton, regexButton, filterButton, comboBox, up, down, close);
		regexListeners = Lists.newArrayList(caseButton, regexButton, filterButton, regexComboBox, up, down, close);
		closedListeners = Lists.newArrayList(open);
	}
	
	protected void addOptionButton(ToggleImageButton button) {
		addOptionButton(optionButtons.size(), button);
	}
	
	protected void addOptionButton(int pos, ToggleImageButton button) {
		optionButtons.add(pos, button);
		expandedListeners.add(pos, button);
		regexListeners.add(pos, button);
	}
	
	protected void removeButton(ToggleImageButton button) {
		optionButtons.remove(button);
		expandedListeners.remove(button);
		regexListeners.remove(button);
	}
	
	protected void next(boolean forward) {
		int nextMatch = currentMatch + (forward? 1 : -1);
		if (nextMatch < 0 || nextMatch >= totalMatches) {
			if (overMatch) {
				overMatch = false;
				nextMatch = forward? 0 : totalMatches - 1;
			} else overMatch = true;
		} else overMatch = false;
		if (!overMatch) {
			currentMatch = nextMatch;
			handler.selectMatch(currentMatch);
		}
		setListener(getComboBox());
		commitHistory();
	}
	
	protected void commitHistory() {
		if (regex) {
			final Pattern value = regexComboBox.getValue();
			if (value != null && !value.pattern().isEmpty()) {
				final List<Pattern> sh = new LinkedList<>(search.regex_search_history);
				if (sh.indexOf(value) == 0)
					return;
				sh.remove(value);
				sh.add(0, value);
				final int size = search.regex_search_history_size;
				final Pair<Integer, List<Pattern>> newHistory = Pair.of(
				  size, new ArrayList<>(sh.subList(0, min(sh.size(), size))));
				ConfigEntryHolder c = SimpleConfigMod.CLIENT_CONFIG.getChild("advanced.search");
				String REGEX_SEARCH_HISTORY = "regex_search_history";
				if (c.hasGUI()) {
					Pair<Integer, List<String>> newGUIHistory = Pair.of(
					  newHistory.getLeft(), newHistory.getRight().stream()
						 .map(Pattern::pattern).collect(Collectors.toList()));
					c.setGUI(REGEX_SEARCH_HISTORY, newGUIHistory);
					search.regex_search_history = newHistory.getValue();
				} else c.set(REGEX_SEARCH_HISTORY, newHistory);
				regexComboBox.setSuggestions(newHistory.getValue());
			}
		} else {
			final String value = comboBox.getValue();
			if (value != null && !value.isEmpty()) {
				final List<String> sh = new LinkedList<>(search.search_history);
				if (sh.indexOf(value) == 0)
					return;
				sh.remove(value);
				sh.add(0, value);
				final int size = search.search_history_size;
				final Pair<Integer, List<String>> newHistory = Pair.of(
				  size, new ArrayList<>(sh.subList(0, min(sh.size(), size))));
				String SEARCH_HISTORY = "search_history";
				ConfigEntryHolder c = SimpleConfigMod.CLIENT_CONFIG.getChild("advanced.search");
				if (c.hasGUI()) {
					c.setGUI(SEARCH_HISTORY, newHistory);
					search.search_history = newHistory.getValue();
				} else c.set(SEARCH_HISTORY, newHistory);
				comboBox.setSuggestions(newHistory.getValue());
			}
		}
	}
	
	protected void updateModifiers() {
		if (regexButton.getValue() != regex) {
			if (regex)
				comboBox.setText(regexComboBox.getText());
			else regexComboBox.setText(comboBox.getText());
			comboBox.setDropDownShown(false);
			regexComboBox.setDropDownShown(false);
		}
		caseSensitive = caseButton.getValue();
		regex = regexButton.getValue();
		filter = filterButton.getValue();
		ConfigEntryHolder g = SimpleConfigMod.CLIENT_CONFIG.getChild("advanced.search");
		String SEARCH_FILTER = "search_filter";
		String SEARCH_CASE_SENSITIVE = "search_case_sensitive";
		String SEARCH_REGEX = "search_regex";
		if (g.hasGUI()) {
			g.setGUI(SEARCH_FILTER, filter);
			g.setGUI(SEARCH_CASE_SENSITIVE, caseSensitive);
			g.setGUI(SEARCH_REGEX, regex);
		} else {
			g.set(SEARCH_FILTER, filter);
			g.set(SEARCH_CASE_SENSITIVE, caseSensitive);
			g.set(SEARCH_REGEX, regex);
		}
		makeQuery();
		setListener(getComboBox());
		if (!getComboBox().isFocused()) getComboBox().changeFocus(true);
		if (getOtherComboBox().isFocused()) getOtherComboBox().changeFocus(true);
	}
	
	protected ComboBoxWidget<?> getComboBox() {
		return regex? regexComboBox : comboBox;
	}
	
	protected ComboBoxWidget<?> getOtherComboBox() {
		return regex? comboBox : regexComboBox;
	}
	
	@SuppressWarnings("RegExpUnexpectedAnchor" ) private static final Pattern NO_MATCH = Pattern.compile("$^");
	protected @NotNull Pattern getQuery() {
		if (regex) {
			final Pattern value = regexComboBox.getValue();
			return value != null? caseSensitive ? value : Pattern.compile(
			  value.pattern(), Pattern.CASE_INSENSITIVE) : NO_MATCH;
		} else {
			final String value = comboBox.getValue();
			return value != null? Pattern.compile(
			  Pattern.quote(value), caseSensitive? 0 : Pattern.CASE_INSENSITIVE) : NO_MATCH;
		}
	}
	
	protected void makeQuery() {
		Pair<Integer, Integer> p = handler.query(getQuery());
		if (p != null) {
			totalMatches = p.getRight();
			currentMatch = max(0, p.getLeft());
		}
	}
	
	public void close() {
		handler.dismissQuery();
		comboBox.setDropDownShown(false);
		regexComboBox.setDropDownShown(false);
		expanded = false;
	}
	
	public void open() {
		expanded = true;
		final ComboBoxWidget<?> comboBox = getComboBox();
		setListener(comboBox);
		if (!comboBox.isFocused())
			comboBox.changeFocus(true);
		comboBox.selectAll();
		handler.selectMatch(currentMatch);
		screen.addOverlay(overlay, this, 50);
	}
	
	@Override public boolean isMouseOver(double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
	}
	
	public Optional<ITextComponent[]> getTooltip(double mouseX, double mouseY) {
		if (isExpanded()) {
			if (caseButton.isMouseOver(mouseX, mouseY))
				return Optional.of(CASE_SENSITIVE_TOOLTIP);
			if (regexButton.isMouseOver(mouseX, mouseY))
				return Optional.of(REGEX_TOOLTIP);
			if (filterButton.isMouseOver(mouseX, mouseY))
				return Optional.of(FILTER_TOOLTIP);
		}
		return Optional.empty();
	}
	
	@Override public boolean overlayMouseClicked(Rectangle area, double mouseX, double mouseY, int button) {
		mouseClicked(mouseX, mouseY, button);
		return true;
	}
	
	@Override public void overlayMouseClickedOutside(Rectangle area, double mouseX, double mouseY, int button) {
		if (closeOnClickOutside && isExpanded() && !isFilter() && !isMouseOver(mouseX, mouseY)) close();
	}
	
	@Override public boolean renderOverlay(
	  MatrixStack mStack, Rectangle area, int mouseX, int mouseY, float delta
	) {
		if (!expanded) return false;
		drawBackground(mStack, mouseX, mouseY, delta);
		final ComboBoxWidget<?> comboBox = getComboBox();
		setListener(comboBox);
		if (!comboBox.isFocused() && !isFilter())
			comboBox.setFocused(true);
		positionExpanded(mStack, mouseX, mouseY, delta);
		renderExpanded(mStack, mouseX, mouseY, delta);
		final Optional<ITextComponent[]> tt = getTooltip(mouseX, mouseY);
		if (tt.isPresent())
			screen.addTooltip(Tooltip.of(Point.of(mouseX, mouseY + 16), tt.get()));
		return true;
	}
	
	public void render(MatrixStack mStack, int mouseX, int mouseY, float delta) {
		final ComboBoxWidget<?> comboBox = getComboBox();
		overlay.setBounds(x, y, w, comboBox.isDropDownShown()? h + comboBox.getDropDownHeight() : h);
		if (!expanded) {
			positionNotExpanded(mStack, mouseX, mouseY, delta);
			renderNotExpanded(mStack, mouseX, mouseY, delta);
		}
	}
	
	protected void positionExpanded(MatrixStack mStack, int mouseX, int mouseY, float delta) {
		int textY = y + 8;
		final FontRenderer font = Minecraft.getInstance().fontRenderer;
		final String text =
		  String.format("%s / %s", totalMatches > 0 ? currentMatch + 1 : 0, totalMatches);
		final int textW = font.getStringWidth(text);
		int textX = x + w - 42 - textW;
		up.x = x + w - 38;
		up.y = y + 3;
		down.x = x + w - 20;
		down.y = y + 3;
		close.x = x + 2;
		close.y = y + 2;
		int bx = x + 24;
		for (ToggleImageButton b : optionButtons) {
			b.x = bx;
			bx += b.getWidth() + 2;
			b.y = y + 12 - b.getHeightRealms() / 2;
		}
		int comboWidth = w - bx - 42 - (filter? 0 : max(42, textW));
		comboBox.setWidth(comboWidth);
		regexComboBox.setWidth(comboWidth);
		comboBox.x = bx + 2;
		comboBox.y = y + 3;
		regexComboBox.x = bx + 2;
		regexComboBox.y = y + 3;
		if (!filter) font.drawStringWithShadow(
		  mStack, text, textX, textY, overMatch ? 0xffffff42 : 0xffe0e0e0);
	}
	
	protected void positionNotExpanded(MatrixStack mStack, int mouseX, int mouseY, float delta) {
		open.x = x + 2;
		open.y = y + 2;
	}
	
	protected void renderExpanded(MatrixStack mStack, int mouseX, int mouseY, float delta) {
		if (regex)
			regexComboBox.render(mStack, mouseX, mouseY, delta);
		else comboBox.render(mStack, mouseX, mouseY, delta);
		for (ToggleImageButton b : optionButtons) b.render(mStack, mouseX, mouseY, delta);
		up.render(mStack, mouseX, mouseY, delta);
		down.render(mStack, mouseX, mouseY, delta);
		close.render(mStack, mouseX, mouseY, delta);
	}
	
	protected void renderNotExpanded(MatrixStack mStack, int mouseX, int mouseY, float delta) {
		open.render(mStack, mouseX, mouseY, delta);
	}
	
	protected void drawBackground(MatrixStack mStack, int mouseX, int mouseY, float delta) {
		fill(mStack, x, y, x + w, y + h, 0xFF343434);
	}
	
	@Override public boolean charTyped(char codePoint, int modifiers) {
		if (Screen.hasAltDown()) {
			switch (Character.toLowerCase(codePoint)) {
				case 'c':
					caseButton.setValue(!caseButton.getValue());
					return true;
				case 'r':
					regexButton.setValue(!regexButton.getValue());
					return true;
				case 'f':
					filterButton.setValue(!filterButton.getValue());
					return true;
			}
		}
		return super.charTyped(codePoint, modifiers);
	}
	
	@Override public boolean overlayEscape() {
		final ComboBoxWidget<?> comboBox = getComboBox();
		if (comboBox.isDropDownShown()) {
			comboBox.setDropDownShown(false);
			return true;
		} else if (isExpanded()) {
			close();
			return true;
		}
		return false;
	}
	
	@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		final ComboBoxWidget<?> comboBox = getComboBox();
		if (comboBox.isDropDownShown() && comboBox.getShownSuggestions().size() == 0)
			comboBox.setDropDownShown(false);
		if (!comboBox.isDropDownShown()) {
			switch (keyCode) {
				case GLFW.GLFW_KEY_ESCAPE:
					if (expanded) {
						close();
						return true;
					}
					break;
				case GLFW.GLFW_KEY_ENTER:
					if (isFilter() || Screen.hasControlDown()) {
						WidgetUtils.forceUnFocus(getComboBox());
						handler.focusResults();
					} else {
						next(!Screen.hasShiftDown());
					}
					return true;
				case GLFW.GLFW_KEY_DOWN:
					next(true);
					return true;
				case GLFW.GLFW_KEY_UP:
					next(false);
					return true;
			}
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}
	
	public boolean isExpanded() {
		return expanded;
	}
	
	public boolean isFilter() {
		return filter;
	}
	
	public boolean isEmpty() {
		return getComboBox().getText().isEmpty();
	}
	
	@Override public @NotNull List<? extends IGuiEventListener> getEventListeners() {
		return isExpanded()? regex? regexListeners : expandedListeners : closedListeners;
	}
	
	@Override public boolean changeFocus(boolean focus) {
		if (isExpanded()) {
			final boolean result = getComboBox().changeFocus(focus);
			if (!result) {
				if (isFilter()) handler.focusResults();
				else close();
			}
			return result;
		} else return open.changeFocus(focus);
	}
	
	public void refresh() {
		if (isExpanded()) {
			makeQuery();
		} else handler.dismissQuery();
	}
	
	public interface ISearchHandler {
		/**
		 * @return A pair with the index of the current selected match
		 *         and the total matches
		 */
		Pair<Integer, Integer> query(Pattern query);
		void dismissQuery();
		void selectMatch(int idx);
		void focusResults();
	}
}
