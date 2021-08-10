package endorh.simple_config.clothconfig2.gui.widget;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simple_config.SimpleConfigMod;
import endorh.simple_config.SimpleConfigMod.ClientConfig.advanced;
import endorh.simple_config.clothconfig2.api.Tooltip;
import endorh.simple_config.clothconfig2.gui.IOverlayCapableScreen;
import endorh.simple_config.clothconfig2.gui.IOverlayCapableScreen.IOverlayRenderer;
import endorh.simple_config.clothconfig2.gui.SimpleConfigIcons;
import endorh.simple_config.clothconfig2.gui.widget.ComboBoxWidget.PatternTypeWrapper;
import endorh.simple_config.clothconfig2.gui.widget.ComboBoxWidget.StringTypeWrapper;
import endorh.simple_config.clothconfig2.gui.widget.MultiFunctionImageButton.ButtonAction;
import endorh.simple_config.clothconfig2.gui.widget.MultiFunctionImageButton.Modifier;
import endorh.simple_config.clothconfig2.math.Point;
import endorh.simple_config.clothconfig2.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.FocusableGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class SearchBarWidget extends FocusableGui implements IOverlayRenderer {
	protected static final ResourceLocation CONFIG_TEX = new ResourceLocation(
	  SimpleConfigMod.MOD_ID, "textures/gui/cloth_config.png");
	protected static ITextComponent[] CASE_SENSITIVE_TOOLTIP = new ITextComponent[] {
	  new TranslationTextComponent("simple-config.ui.search.case_sensitive"),
	  new TranslationTextComponent("modifier.cloth-config.alt", "C").mergeStyle(TextFormatting.GRAY)};
	protected static ITextComponent[] REGEX_TOOLTIP = new ITextComponent[] {
	  new TranslationTextComponent("simple-config.ui.search.regex"),
	  new TranslationTextComponent("modifier.cloth-config.alt", "R").mergeStyle(TextFormatting.GRAY)};
	
	public int x;
	public int y;
	public int w;
	public int h;
	
	protected IOverlayCapableScreen screen;
	protected ISearchHandler handler;
	
	protected ComboBoxWidget<String> comboBox;
	protected ComboBoxWidget<Pattern> regexComboBox;
	protected MultiFunctionImageButton up;
	protected MultiFunctionImageButton down;
	protected MultiFunctionImageButton close;
	protected MultiFunctionImageButton open;
	protected ToggleImageButton regexButton;
	protected ToggleImageButton caseButton;
	protected List<IGuiEventListener> expandedListeners;
	protected List<IGuiEventListener> regexListeners;
	protected List<IGuiEventListener> closedListeners;
	
	protected boolean overMatch = false;
	protected int currentMatch = 0;
	protected int totalMatches = 0;
	
	protected boolean caseSensitive = false;
	protected boolean regex = false;
	
	protected boolean closeOnClickOutside = false;
	protected boolean expanded = false;
	protected Rectangle overlay = new Rectangle();
	
	public SearchBarWidget(
	  ISearchHandler handler, int x, int y, int w, IOverlayCapableScreen screen
	) {
		this(handler, x, y, w, 24, screen);
	}
	
	public SearchBarWidget(
	  ISearchHandler handler, int x, int y, int w, int h, IOverlayCapableScreen screen
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
		comboBox.setSuggestions(advanced.search_history);
		regexComboBox.setSuggestions(advanced.regex_search_history);
		up = new MultiFunctionImageButton(
		  x + w - 68, y, 18, 18, SimpleConfigIcons.UP, ButtonAction.of(
			 b -> next(b == 1)
		)).on(Modifier.SHIFT, ButtonAction.of(b -> next(b != 1)).icon(SimpleConfigIcons.DOWN));
		down = new MultiFunctionImageButton(
		  x + w - 44, y, 18, 18, SimpleConfigIcons.DOWN, ButtonAction.of(
			 b -> next(b != 1)
		)).on(Modifier.SHIFT, ButtonAction.of(b -> next(b == 1)).icon(SimpleConfigIcons.UP));
		close = new MultiFunctionImageButton(
		  x + w - 20, y, 20, 20, SimpleConfigIcons.CLOSE, ButtonAction.of(this::close));
		open = new MultiFunctionImageButton(
		  x + 2, y, 20, 20, SimpleConfigIcons.SEARCH, ButtonAction.of(this::open));
		caseButton = new ToggleImageButton(false, x, y + 1, 18, 18, SimpleConfigIcons.SEARCH_CASE_SENSITIVE, b -> updateModifiers());
		regexButton = new ToggleImageButton(false, x + 18, y + 1, 18, 18, SimpleConfigIcons.SEARCH_REGEX, b -> updateModifiers());
		expandedListeners = Lists.newArrayList(caseButton, regexButton, comboBox, up, down, close);
		regexListeners = Lists.newArrayList(caseButton, regexButton, regexComboBox, up, down, close);
		closedListeners = Lists.newArrayList(open);
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
				final List<Pattern> sh = new LinkedList<>(advanced.regex_search_history);
				if (sh.indexOf(value) == 0)
					return;
				sh.remove(value);
				sh.add(0, value);
				final int size = advanced.regex_search_history_size;
				final Pair<Integer, List<Pattern>> newHistory = Pair.of(
				  size, new ArrayList<>(sh.subList(0, min(sh.size(), size))));
				SimpleConfigMod.CLIENT_CONFIG.set("advanced.regex_search_history", newHistory);
				regexComboBox.setSuggestions(newHistory.getValue());
			}
		} else {
			final String value = comboBox.getValue();
			if (value != null && !value.isEmpty()) {
				final List<String> sh = new LinkedList<>(advanced.search_history);
				if (sh.indexOf(value) == 0)
					return;
				sh.remove(value);
				sh.add(0, value);
				final int size = advanced.search_history_size;
				final Pair<Integer, List<String>> newHistory = Pair.of(
				  size, new ArrayList<>(sh.subList(0, min(sh.size(), size))));
				SimpleConfigMod.CLIENT_CONFIG.set("advanced.search_history", newHistory);
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
		Minecraft.getInstance().getSoundHandler().play(
		  SimpleSound.master(SimpleConfigMod.UI_TAP, 1F));
	}
	
	public void open() {
		expanded = true;
		final ComboBoxWidget<?> comboBox = getComboBox();
		setListener(comboBox);
		if (!comboBox.isFocused())
			comboBox.changeFocus(true);
		comboBox.selectAll();
		screen.claimRectangle(overlay, this, 50);
		Minecraft.getInstance().getSoundHandler().play(
		  SimpleSound.master(SimpleConfigMod.UI_TAP, 1F));
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
		}
		return Optional.empty();
	}
	
	@Override public boolean overlayMouseClicked(double mouseX, double mouseY, int button) {
		mouseClicked(mouseX, mouseY, button);
		return true;
	}
	
	@Override public void overlayMouseClickedOutside(double mouseX, double mouseY, int button) {
		if (closeOnClickOutside && isExpanded() && !isMouseOver(mouseX, mouseY)) close();
	}
	
	@Override public boolean renderOverlay(
	  MatrixStack mStack, Rectangle area, int mouseX, int mouseY, float delta
	) {
		if (!expanded) return false;
		drawBackground(mStack, mouseX, mouseY, delta);
		final ComboBoxWidget<?> comboBox = getComboBox();
		setListener(comboBox);
		if (!comboBox.isFocused())
			comboBox.setFocused(true);
		positionExpanded(mStack, mouseX, mouseY, delta);
		renderExpanded(mStack, mouseX, mouseY, delta);
		final Optional<ITextComponent[]> tt = getTooltip(mouseX, mouseY);
		//noinspection OptionalIsPresent
		if (tt.isPresent())
			screen.addTooltip(Tooltip.of(new Point(mouseX, mouseY + 16), tt.get()));
		return true;
	}
	
	public void render(MatrixStack mStack, int mouseX, int mouseY, float delta) {
		final ComboBoxWidget<?> comboBox = getComboBox();
		overlay.setBounds(x, y, w, comboBox.isDropDownShown()? h + comboBox.dropDownHeight : h);
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
		int comboWidth = w - 114 - max(42, textW);
		this.comboBox.setWidth(comboWidth);
		regexComboBox.setWidth(comboWidth);
		this.comboBox.x = x + 64;
		this.comboBox.y = y + 3;
		regexComboBox.x = x + 64;
		regexComboBox.y = y + 3;
		up.x = x + w - 38;
		up.y = y + 3;
		down.x = x + w - 20;
		down.y = y + 3;
		close.x = x + 2;
		close.y = y + 2;
		caseButton.x = x + 24;
		caseButton.y = y + 3;
		regexButton.x = x + 42;
		regexButton.y = y + 3;
		font.drawStringWithShadow(mStack, text, textX, textY, overMatch ? 0xffffff42 : 0xffe0e0e0);
	}
	
	protected void positionNotExpanded(MatrixStack mStack, int mouseX, int mouseY, float delta) {
		open.x = x + 2;
		open.y = y + 2;
	}
	
	protected void renderExpanded(MatrixStack mStack, int mouseX, int mouseY, float delta) {
		if (regex)
			regexComboBox.render(mStack, mouseX, mouseY, delta);
		else this.comboBox.render(mStack, mouseX, mouseY, delta);
		caseButton.render(mStack, mouseX, mouseY, delta);
		regexButton.render(mStack, mouseX, mouseY, delta);
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
			if (Character.toLowerCase(codePoint) == 'c') {
				caseButton.setValue(!caseButton.getValue());
				return true;
			} else if (Character.toLowerCase(codePoint) == 'r') {
				regexButton.setValue(!regexButton.getValue());
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
				case 256: // Escape
					if (expanded) {
						close();
						return true;
					}
					break;
				case 257: // Enter
					next(!Screen.hasShiftDown());
					return true;
				case 264: // Down
					next(true);
					return true;
				case 265: // Up
					next(false);
					return true;
			}
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}
	
	public boolean isExpanded() {
		return expanded;
	}
	
	@Override public @NotNull List<? extends IGuiEventListener> getEventListeners() {
		return expanded? regex? regexListeners : expandedListeners : closedListeners;
	}
	
	@Override public boolean changeFocus(boolean focus) {
		final boolean result = getComboBox().changeFocus(focus);
		if (!result)
			close();
		return result;
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
	}
}
