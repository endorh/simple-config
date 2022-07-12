package endorh.simpleconfig.ui.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.SimpleConfigMod.ClientConfig;
import endorh.simpleconfig.SimpleConfigMod.KeyBindings;
import endorh.simpleconfig.ui.api.*;
import endorh.simpleconfig.ui.api.AbstractConfigEntry.EntryError;
import endorh.simpleconfig.ui.gui.widget.*;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import endorh.simpleconfig.ui.gui.widget.SearchBarWidget.ISearchHandler;
import endorh.simpleconfig.ui.impl.EasingMethod;
import endorh.simpleconfig.ui.math.Rectangle;
import endorh.simpleconfig.core.SimpleConfigGUIManager;
import endorh.simpleconfig.core.SimpleConfigGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.util.InputMappings;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static endorh.simpleconfig.core.SimpleConfigTextUtil.splitTtc;
import static java.lang.Math.min;

@OnlyIn(value = Dist.CLIENT)
public class ClothConfigScreen
  extends AbstractTabbedConfigScreen implements ISearchHandler {
	private final ScrollingHandler tabsScroller = new ScrollingHandler() {
		@Override public Rectangle getBounds() {
			return new Rectangle(0, 0, 1, width - 40);
		}
		
		@Override public int getMaxScrollHeight() {
			return (int) tabsMaximumScrolled;
		}
		
		@Override public void updatePosition(float delta) {
			super.updatePosition(delta);
			scrollAmount = clamp(scrollAmount, 0.0);
		}
	};
	public Map<ConfigCategory, ListWidget<AbstractConfigEntry<?>>> listWidgets = new HashMap<>();
	public ListWidget<AbstractConfigEntry<?>> listWidget;
	public final String modId;
	protected TintedButton quitButton;
	protected TintedButton saveButton;
	protected MultiFunctionIconButton clientButton;
	protected MultiFunctionIconButton serverButton;
	protected PresetPickerWidget presetPickerWidget;
	protected Widget buttonLeftTab;
	protected Widget buttonRightTab;
	protected MultiFunctionImageButton undoButton;
	protected MultiFunctionImageButton redoButton;
	protected MultiFunctionImageButton editFileButton;
	protected MultiFunctionImageButton keyboardButton;
	protected MultiFunctionImageButton settingsButton;
	protected MultiFunctionImageButton selectAllButton;
	protected MultiFunctionImageButton invertSelectionButton;
	protected SelectionToolbar selectionToolbar;
	protected Rectangle tabsBounds;
	protected Rectangle tabsLeftBounds;
	protected Rectangle tabsRightBounds;
	protected double tabsMaximumScrolled = -1.0;
	protected final List<ClothConfigTabButton> tabButtons = Lists.newArrayList();
	protected TooltipSearchBarWidget searchBar;
	protected StatusDisplayBar statusDisplayBar;
	
	protected final Set<AbstractConfigEntry<?>> selectedEntries = new HashSet<>();
	protected boolean isSelecting = false;
	
	protected ConfigCategory lastClientCategory = null;
	protected ConfigCategory lastServerCategory = null;
	
	@Internal public ClothConfigScreen(
	  Screen parent, String modId, ITextComponent title, Collection<ConfigCategory> categories,
	  Collection<ConfigCategory> serverCategories, ResourceLocation backgroundLocation
	) {
		super(parent, title, backgroundLocation, categories, serverCategories);
		this.modId = modId;
		for (ConfigCategory category : sortedCategories) {
			for (AbstractConfigEntry<?> entry : category.getHeldEntries()) {
				entry.setCategory(category);
				entry.setScreen(this);
			}
		}
		statusDisplayBar = new StatusDisplayBar(this);
		searchBar = new TooltipSearchBarWidget(this, 0, 0, 256, this);
		presetPickerWidget = new PresetPickerWidget(this, 0, 0, 70);
		if (isSelectedCategoryServer())
			lastServerCategory = selectedCategory;
		else lastClientCategory = selectedCategory;
	}
	
	protected static final Pattern DOT = Pattern.compile("\\.");
	
	@Override public @Nullable AbstractConfigEntry<?> getEntry(String path) {
		final String[] split = DOT.split(path, 3);
		if (split.length < 2) return null;
		final Optional<ConfigCategory> opt = categoryMap.values().stream()
		  .filter(c -> c.getName().equals(split[0])).findFirst();
		if (!opt.isPresent()) return null;
		final ConfigCategory cat = opt.get();
		final Optional<AbstractConfigEntry<?>> first = cat.getHeldEntries().stream()
		  .filter(e -> e.getName().equals(split[1])).findFirst();
		if (!first.isPresent()) return null;
		AbstractConfigEntry<?> entry = first.get();
		if (split.length < 3) return entry;
		if (!(entry instanceof IEntryHolder)) return null;
		return ((IEntryHolder) entry).getEntry(split[2]);
	}
	
	@Override public List<AbstractConfigEntry<?>> getHeldEntries() {
		return sortedCategories.stream()
		  .flatMap(c -> c.getHeldEntries().stream())
		  .collect(Collectors.toList());
	}
	
	protected void init() {
		super.init();
		
		// Toolbar
		searchBar.w = width;
		addWidget(searchBar);
		
		undoButton = new MultiFunctionImageButton(
		  48, 2, 20, 20, SimpleConfigIcons.UNDO, ButtonAction.of(() -> {
			undo();
			setFocused(listWidget);
		}).active(() -> history.canUndo()));
		addButton(undoButton);
		redoButton = new MultiFunctionImageButton(
		  68, 2, 20, 20, SimpleConfigIcons.REDO, ButtonAction.of(() -> {
			redo();
			setFocused(listWidget);
		}).active(() -> history.canRedo()));
		addButton(redoButton);
		
		editFileButton = new MultiFunctionImageButton(
		  24, 2, 20, 20, SimpleConfigIcons.EDIT_FILE, ButtonAction.of(
			 () -> selectedCategory.getContainingFile().ifPresent(
				f -> addDialog(EditConfigFileDialog.create(this, f.toAbsolutePath())))
		  ).active(() -> selectedCategory.getContainingFile().isPresent())
		  .tooltip(new TranslationTextComponent("simpleconfig.file.open")));
		addButton(editFileButton);
		
		clientButton = new MultiFunctionIconButton(90, 2, 20, 70, SimpleConfigIcons.CLIENT_ICON, ButtonAction
		  .of(this::showClientCategories)
		  .title(() -> new TranslationTextComponent("simpleconfig.config.category.client"))
		  .tooltip(() -> hasClient() && isSelectedCategoryServer()? Lists.newArrayList(new TranslationTextComponent(
			 "simpleconfig.ui.switch.client")) : Collections.emptyList())
		  .active(this::hasClient));
		addButton(clientButton);
		serverButton = new MultiFunctionIconButton(90, 2, 20, 70, SimpleConfigIcons.SERVER_ICON, ButtonAction
		  .of(this::showServerCategories)
		  .title(() -> new TranslationTextComponent("simpleconfig.config.category.server"))
		  .tooltip(() -> hasServer() && !isSelectedCategoryServer()? Lists.newArrayList(new TranslationTextComponent(
			 "simpleconfig.ui.switch.server")) : Collections.emptyList())
		  .active(this::hasServer));
		addButton(serverButton);
		serverButton.x = clientButton.x + clientButton.getWidth();
		
		if (serverButton.x + serverButton.getWidth() > 0.35 * width) {
			clientButton.setMaxWidth(20);
			serverButton.setMaxWidth(20);
			serverButton.x = clientButton.x + 20;
		}
		
		if (isSelectedCategoryServer()) {
			serverButton.setTintColor(0x800A426A);
		} else {
			clientButton.setTintColor(0x80287734);
		}
		
		selectionToolbar = new SelectionToolbar(this, 76, 2);
		selectionToolbar.visible = false;
		addWidget(selectionToolbar);
		
		// Right toolbar
		presetPickerWidget.w = MathHelper.clamp(width / 3, 80, 250);
		presetPickerWidget.x = width - presetPickerWidget.w - 2;
		presetPickerWidget.y = 2;
		addWidget(presetPickerWidget);
		
		// Tab bar
		if (isShowingTabs()) {
			tabsBounds = new Rectangle(0, 24, width, 24);
			tabsLeftBounds = new Rectangle(0, 24, 18, 24);
			tabsRightBounds = new Rectangle(width - 18, 24, 18, 24);
			buttonLeftTab = new MultiFunctionImageButton(
			  4, 27, 12, 18, SimpleConfigIcons.LEFT_TAB,
			  ButtonAction.of(() -> tabsScroller.offset(-48, true))
				 .active(() -> tabsScroller.scrollAmount > 0.0));
			children.add(buttonLeftTab);
			tabButtons.clear();
			int ww = 0;
			for (ConfigCategory cat : (isSelectedCategoryServer() ? sortedServerCategories
			                                                      : sortedClientCategories)) {
				final int w = font.width(cat.getTitle());
				ww += w + 2;
				tabButtons.add(new ClothConfigTabButton(
				  this, cat, -100, 26,
				  cat.getTitle(), cat.getDescription()));
			}
			tabsMaximumScrolled = ww;
			children.addAll(tabButtons);
			buttonRightTab = new MultiFunctionImageButton(
			  width - 16, 27, 12, 18, SimpleConfigIcons.RIGHT_TAB,
			  ButtonAction.of(() -> tabsScroller.offset(48, true))
				 .active(
					() -> tabsScroller.scrollAmount < tabsMaximumScrolled - (double) width + 40.0));
			children.add(buttonRightTab);
		} else tabsBounds = tabsLeftBounds = tabsRightBounds = new Rectangle();
		
		// Content
		listWidget = listWidgets.computeIfAbsent(
		  selectedCategory, c -> {
			  final ListWidget<AbstractConfigEntry<?>> w = new ListWidget<>(
				 this, minecraft, width, height, isShowingTabs() ? 50 : 24,
				 height - 28, getBackgroundLocation());
			  w.getEntries().addAll(c.getHeldEntries());
			  return w;
		  });
		listWidget.resize(width, height, isShowingTabs()? 50 : 24, height - 28);
		if (width >= 800) {
			listWidget.setLeftPos((width - 800) / 2);
			listWidget.setRightPos(width - (width - 800) / 2);
		}
		addWidget(listWidget);
		
		// Status bar
		addButton(statusDisplayBar);
		
		// Left controls
		selectAllButton = new MultiFunctionImageButton(
		  2, height - 22, 20, 20, SimpleConfigIcons.SELECT_ALL,
		  ButtonAction.of(this::selectAllEntries)
			 .tooltip(new TranslationTextComponent("simpleconfig.ui.select_all")));
		addButton(selectAllButton);
		invertSelectionButton = new MultiFunctionImageButton(
		  24, height - 22, 20, 20, SimpleConfigIcons.INVERT_SELECTION,
		  ButtonAction.of(this::invertEntrySelection)
			 .tooltip(new TranslationTextComponent("simpleconfig.ui.invert_selection")));
		addButton(invertSelectionButton);
		selectAllButton.visible = invertSelectionButton.visible = false;
		
		// Center controls
		int buttonWidths = min(200, (width - 50 - 12) / 3);
		quitButton = new TintedButton(
		  width / 2 - buttonWidths - 3, height - 24, buttonWidths, 20,
		  DialogTexts.GUI_CANCEL,
		  widget -> quit());
		// quitButton.setTintColor(0x80BD4242);
		addButton(quitButton);
		saveButton = new TintedButton(
		  width / 2 + 3, height - 24, buttonWidths, 20,
		  NarratorChatListener.NO_TITLE, button -> saveAll(true)) {
			public void render(@NotNull MatrixStack matrices, int mouseX, int mouseY, float delta) {
				final boolean hasErrors = hasErrors();
				active = isEdited() && !hasErrors;
				setMessage(new TranslationTextComponent(
				  hasErrors ? "text.cloth-config.error_cannot_save" : "text.cloth-config.save_and_done"));
				super.render(matrices, mouseX, mouseY, delta);
			}
		};
		saveButton.setTintColor(0x8042BD42);
		addButton(saveButton);
		saveButton.active = isEdited();
		
		// Right buttons
		if (!modId.equals(SimpleConfigMod.MOD_ID)) {
			settingsButton = new MultiFunctionImageButton(
			  width - 44, height - 22, 20, 20, SimpleConfigIcons.GEAR,
			  ButtonAction.of(() -> SimpleConfigGUIManager.showConfigGUI(SimpleConfigMod.MOD_ID))
				 .tooltip(new TranslationTextComponent("simpleconfig.ui.simple_config_settings")));
			addButton(settingsButton);
		}
		keyboardButton = new MultiFunctionImageButton(
		  width - 22, height - 22, 20, 20, SimpleConfigIcons.KEYBOARD,
		  ButtonAction.of(() -> addDialog(getControlsDialog()))
		    .tooltip(new TranslationTextComponent("simpleconfig.ui.controls")));
		addButton(keyboardButton);
		
		// Update UI mode
		isSelecting = false;
		updateSelection();
		
		// Post init
		if (afterInitConsumer != null) afterInitConsumer.accept(this);
	}
	
	public List<EntryError> getErrors() {
		List<EntryError> errors = Lists.newArrayList();
		// Add errors in order
		for (ConfigCategory cat : isSelectedCategoryServer()? sortedServerCategories : sortedClientCategories)
			errors.addAll(cat.getErrors());
		for (ConfigCategory cat : isSelectedCategoryServer()? sortedClientCategories : sortedServerCategories)
			errors.addAll(cat.getErrors());
		return errors;
	}
	
	public void showServerCategories() {
		if (hasServer() && !isSelectedCategoryServer()) setSelectedCategory(
		  lastServerCategory != null ? lastServerCategory : sortedServerCategories.get(0));
	}
	
	public void showClientCategories() {
		if (hasClient() && isSelectedCategoryServer()) setSelectedCategory(
		  lastClientCategory != null? lastClientCategory : sortedClientCategories.get(0));
	}
	
	public List<ITextComponent> getErrorsMessages() {
		return getErrors().stream().map(EntryError::getError).collect(Collectors.toList());
	}
	
	@Override protected boolean canSave() {
		return isEdited() && !hasErrors();
	}
	
	@Override public void setSelectedCategory(ConfigCategory category) {
		if (selectedCategory != category) {
			// Switching sides is prevented while selecting
			if (isSelecting() && isSelectedCategoryServer() != category.isServer()) return;
			if (isSelectedCategoryServer())
				lastServerCategory = selectedCategory;
			else lastClientCategory = selectedCategory;
			super.setSelectedCategory(category);
			final int index = getTabbedCategories().indexOf(category);
			int x = 0;
			for (int i = 0; i < index; i++)
				x += tabButtons.get(i).getWidth() + 2;
			x += tabButtons.get(index).getWidth() / 2;
			x -= tabsScroller.getBounds().width / 2;
			tabsScroller.scrollTo(x, true, 250L);
			ListWidget<AbstractConfigEntry<?>> prevListWidget = listWidget;
			init(Minecraft.getInstance(), width, height);
			prevListWidget.onReplaced(listWidget);
			searchBar.refresh();
			if (searchBar.isExpanded()) {
				setFocused(searchBar);
			}
		}
	}
	
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		if (tabsBounds.contains(mouseX, mouseY) &&
		    !tabsLeftBounds.contains(mouseX, mouseY) &&
		    !tabsRightBounds.contains(mouseX, mouseY) && amount != 0.0) {
			tabsScroller.offset(-amount * 16.0, true);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, amount);
	}
	
	public @Nullable AbstractConfigEntry<?> getFocusedEntry() {
		return listWidget.getSelectedEntry();
	}
	
	public void selectAllEntries() {
		selectedCategory.getHeldEntries().forEach(e -> e.setSelected(true));
	}
	
	public void invertEntrySelection() {
		selectedCategory.getAllMainEntries().stream()
		  .filter(e -> !(e instanceof IEntryHolder) && e.isSelectable())
		  .forEach(e -> e.setSelected(!e.isSelected()));
	}
	
	@Override public Set<AbstractConfigEntry<?>> getSelectedEntries() {
		return selectedEntries;
	}
	
	@Override public boolean canSelectEntries() {
		return true;
	}
	
	@Override protected void recomputeFocus() {
		if (searchBar.isExpanded() && !hasDialogs())
			setFocused(searchBar);
	}
	
	@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (super.keyPressed(keyCode, scanCode, modifiers))
			return true;
		if (keyCode == 264 || keyCode == 265) { // Up | Down
			setFocused(listWidget);
			return listWidget.keyPressed(keyCode, scanCode, modifiers);
		}
		return false;
	}
	
	@Override protected boolean screenKeyPressed(int keyCode, int scanCode, int modifiers) {
		if (super.screenKeyPressed(keyCode, scanCode, modifiers)) return true;
		InputMappings.Input key = InputMappings.getKey(keyCode, scanCode);
		// Navigation key bindings first
		if (KeyBindings.NEXT_ERROR.isActiveAndMatches(key)) { // F1
			if (hasErrors()) focusNextError(true);
			else if (hasConflictingExternalChanges()) focusNextExternalConflict(true);
			playFeedbackTap(0.4F);
			return true;
		} else if (KeyBindings.PREV_ERROR.isActiveAndMatches(key)) {
			if (hasErrors()) focusNextError(false);
			else if (hasConflictingExternalChanges()) focusNextExternalConflict(false);
			playFeedbackTap(0.4F);
			return true;
		} else if (KeyBindings.SEARCH.isActiveAndMatches(key)) {
			searchBar.open();
			setFocused(searchBar);
			playFeedbackTap(1F);
			return true;
			// History key bindings second
		} else if (KeyBindings.UNDO.isActiveAndMatches(key)) {
			if (getHistory().canUndo())
				playFeedbackTap(1F);
			undo();
			return true;
		} else if (KeyBindings.REDO.isActiveAndMatches(key)) {
			if (getHistory().canRedo())
				playFeedbackTap(1F);
			redo();
			return true;
			// Modification key bindings last
		} else if (KeyBindings.RESET_RESTORE.isActiveAndMatches(key)) {
			if (isSelecting()) {
				final Set<AbstractConfigEntry<?>> selected = getSelectedEntries();
				if (Screen.hasAltDown()) {
					if (selected.stream().anyMatch(AbstractConfigEntry::isRestorable)) {
						selected.forEach(AbstractConfigEntry::restoreValue);
						playFeedbackTap(1F);
						return true;
					}
				} else if (selected.stream().anyMatch(AbstractConfigEntry::isResettable)) {
					selected.forEach(AbstractConfigEntry::resetValue);
					playFeedbackTap(1F);
					return true;
				}
			} else {
				AbstractConfigEntry<?> entry = listWidget.getSelectedEntry();
				while (entry != null && entry.isSubEntry()) entry = entry.getParentEntry();
				if (entry != null) {
					ResetButton resetButton = entry.getResetButton();
					if (resetButton != null && resetButton.active) {
						IGuiEventListener focused = entry.getFocused();
						if (focused != resetButton) {
							if (focused instanceof Widget && ((Widget) focused).isFocused())
								WidgetUtils.forceUnFocus(focused);
							if (entry.children().contains(resetButton))
								entry.setFocused(resetButton);
							WidgetUtils.forceFocus(resetButton);
						}
						if (resetButton.activate()) return true;
					}
				}
			}
			playFeedbackTap(1F);
			return true;
		}
		return false;
	}
	
	public void updateSelection() {
		selectedEntries.clear();
		(isSelectedCategoryServer()? sortedServerCategories : sortedClientCategories).stream()
		  .flatMap(c -> c.getAllMainEntries().stream())
		  .filter(AbstractConfigEntry::isSelected)
		  .forEach(selectedEntries::add);
		final boolean selecting = !selectedEntries.isEmpty();
		if (selecting != isSelecting) {
			if (selecting) {
				undoButton.visible = redoButton.visible = false;
				selectAllButton.visible = invertSelectionButton.visible = true;
				MultiFunctionIconButton visible = clientButton;
				MultiFunctionIconButton hidden = serverButton;
				if (isSelectedCategoryServer()) {
					visible = serverButton;
					hidden = clientButton;
				}
				visible.setExactWidth(20);
				hidden.visible = false;
				visible.x = undoButton.x;
				selectionToolbar.visible = true;
			} else {
				undoButton.visible = redoButton.visible = true;
				selectAllButton.visible = invertSelectionButton.visible = false;
				clientButton.setWidthRange(20, 70);
				serverButton.setWidthRange(20, 70);
				clientButton.visible = true;
				serverButton.visible = true;
				clientButton.x = 90;
				serverButton.x = clientButton.x + clientButton.getWidth();
				selectionToolbar.visible = false;
			}
			isSelecting = selecting;
		}
	}
	
	@Override public void tick() {
		super.tick();
		listWidget.tick();
	}
	
	@Override
	public void render(@NotNull MatrixStack mStack, int mouseX, int mouseY, float delta) {
		if (minecraft == null) return;
		final boolean hasDialog = hasDialogs();
		boolean suppressHover = hasDialog || shouldOverlaysSuppressHover(mouseX, mouseY);
		final int smX = suppressHover ? -1 : mouseX;
		final int smY = suppressHover ? -1 : mouseY;
		if (getFocused() == null || getFocused() == searchBar && !searchBar.isExpanded())
			setFocused(listWidget);
		if (isShowingTabs()) {
			tabsScroller.updatePosition(delta * 3.0f);
			int xx = 24 - (int) tabsScroller.scrollAmount;
			for (ClothConfigTabButton tabButton : tabButtons) {
				tabButton.x = xx;
				xx += tabButton.getWidth() + 2;
			}
		}
		if (isTransparentBackground()) {
			fillGradient(mStack, 0, 0, width, height, 0xa0101010, 0xb0101010);
		} else renderDirtBackground(0);
		listWidget.render(mStack, smX, smY, delta);
		
		final ITextComponent title = getDisplayedTitle();
		if (isSelecting()) {
			selectionToolbar.render(mStack, mouseX, mouseY, delta);
			drawString(
			  mStack, font, new TranslationTextComponent(
				 "simpleconfig.ui.n_selected", new StringTextComponent(
					String.valueOf(selectedEntries.size())).withStyle(TextFormatting.AQUA)),
			  selectionToolbar.x + selectionToolbar.width + 6, 8, 0xffffffff);
		} else drawCenteredString(mStack, font, title, width / 2, 8, 0xffffffff);
		if (isShowingTabs()) {
			Rectangle r = new Rectangle(tabsBounds.x + 20, tabsBounds.y, tabsBounds.width - 40, tabsBounds.height);
			ScissorsHandler.INSTANCE.scissor(r); {
				if (isTransparentBackground()) {
					fillGradient(mStack, r.x, r.y, r.getMaxX(), r.getMaxY(), 0x68000000, 0x68000000);
				} else {
					overlayBackground(mStack, r, 32, 32, 32);
				}
				tabButtons.forEach(widget -> widget.render(mStack, smX, smY, delta));
				drawTabsShades(mStack, 0, isTransparentBackground() ? 120 : 255);
			} ScissorsHandler.INSTANCE.removeLastScissor();
			buttonLeftTab.render(mStack, smX, smY, delta);
			buttonRightTab.render(mStack, smX, smY, delta);
		}
		editFileButton.render(mStack, mouseX, mouseY, delta);
		searchBar.render(mStack, hasDialog ? -1 : mouseX, hasDialog ? -1 : mouseY, delta);
		if (listWidget.isScrollingNow())
			removeTooltips(
			  new Rectangle(listWidget.left, listWidget.top, listWidget.width, listWidget.height));
		presetPickerWidget.render(mStack, mouseX, mouseY, delta);
		super.render(mStack, mouseX, mouseY, delta);
	}
	
	public ITextComponent getDisplayedTitle() {
		return title;
	}
	
	public @Nullable INavigableTarget getNext(Predicate<INavigableTarget> predicate, boolean forwards) {
		List<INavigableTarget> targets = listWidget.getNavigableTargets(false);
		if (targets.isEmpty()) return null;
		INavigableTarget target = listWidget.getSelectedTarget();
		if (target == null) {
			final Optional<INavigableTarget> opt =
			  forwards ? targets.stream().filter(predicate).findFirst()
			           : Lists.reverse(targets).stream().filter(predicate).findFirst();
			if (opt.isPresent()) return opt.get();
		} else {
			int idx = targets.indexOf(target), s = targets.size();
			Function<Integer, Integer> step = forwards ? i -> (i + 1) % s : i -> (i - 1 + s) % s;
			for (int i = step.apply(idx); i != idx; i = step.apply(i))
				if (predicate.test(targets.get(i))) return targets.get(i);
			if (predicate.test(target))
				return target;
		}
		int s = sortedCategories.size();
		final int selectedIndex = sortedCategories.indexOf(selectedCategory);
		Function<Integer, Integer> step = forwards ? j -> (j + 1) % s : j -> (j - 1 + s) % s;
		for (int i = step.apply(selectedIndex); i != selectedIndex; i = step.apply(i)) {
			ConfigCategory cat = sortedCategories.get(i);
			Optional<INavigableTarget> opt = listWidgets.get(cat)
			  .getNavigableTargets(false).stream()
			  .filter(predicate).findFirst();
			if (opt.isPresent()) return opt.get();
		}
		return null;
	}
	
	public void focusNextExternalConflict(boolean forwards) {
		Predicate<INavigableTarget> predicate = t -> {
			if (!(t instanceof AbstractConfigEntry<?>)) return false;
			final AbstractConfigEntry<?> entry = (AbstractConfigEntry<?>) t;
			return !entry.isSubEntry() && entry.hasExternalDiff()
			       && !entry.hasAcceptedExternalDiff();
		};
		INavigableTarget next = getNext(predicate.and(INavigableTarget::isNavigable), forwards);
		boolean foundVisible = next != null;
		if (next == null) next = getNext(predicate, forwards);
		if (next != null) {
			if (!foundVisible) getSearchBar().close();
			next.navigate();
			next.applyMergeHighlight();
		}
	}
	
	public void focusNextError(boolean forwards) {
		Map<INavigableTarget, EntryError> errorMap = sortedCategories.stream()
		  .flatMap(c -> c.getErrors().stream())
		  .collect(Collectors.toMap(EntryError::getSource, e -> e, (a, b) -> a));
		INavigableTarget next = getNext(t -> errorMap.containsKey(t) && t.isNavigable(), forwards);
		boolean foundVisible = next != null;
		if (next == null) next = getNext(errorMap::containsKey, forwards);
		if (next != null) {
			if (!foundVisible) getSearchBar().close();
			next.navigate();
			next.applyErrorHighlight();
		}
	}
	
	@SuppressWarnings("SameParameterValue" ) private void drawTabsShades(
	  MatrixStack mStack, int lightColor, int darkColor
	) {
		drawTabsShades(mStack.last().pose(), lightColor, darkColor);
	}
	
	private void drawTabsShades(Matrix4f matrix, int lightColor, int darkColor) {
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(770, 771, 0, 1);
		RenderSystem.disableAlphaTest();
		RenderSystem.shadeModel(7425);
		RenderSystem.disableTexture();
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuilder();
		// @formatter:off
		buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
		buffer.vertex(matrix, (float) (tabsBounds.getMinX() + 20), (float) (tabsBounds.getMinY() + 4), 0.0f).uv(0.0f, 1.0f).color(0, 0, 0, lightColor).endVertex();
		buffer.vertex(matrix, (float) (tabsBounds.getMaxX() - 20), (float) (tabsBounds.getMinY() + 4), 0.0f).uv(1.0f, 1.0f).color(0, 0, 0, lightColor).endVertex();
		buffer.vertex(matrix, (float) (tabsBounds.getMaxX() - 20), (float) tabsBounds.getMinY(), 0.0f).uv(1.0f, 0.0f).color(0, 0, 0, darkColor).endVertex();
		buffer.vertex(matrix, (float) (tabsBounds.getMinX() + 20), (float) tabsBounds.getMinY(), 0.0f).uv(0.0f, 0.0f).color(0, 0, 0, darkColor).endVertex();
		tessellator.end();
		buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
		buffer.vertex(matrix, (float) (tabsBounds.getMinX() + 20), (float) tabsBounds.getMaxY(), 0.0f).uv(0.0f, 1.0f).color(0, 0, 0, darkColor).endVertex();
		buffer.vertex(matrix, (float) (tabsBounds.getMaxX() - 20), (float) tabsBounds.getMaxY(), 0.0f).uv(1.0f, 1.0f).color(0, 0, 0, darkColor).endVertex();
		buffer.vertex(matrix, (float) (tabsBounds.getMaxX() - 20), (float) (tabsBounds.getMaxY() - 4), 0.0f).uv(1.0f, 0.0f).color(0, 0, 0, lightColor).endVertex();
		buffer.vertex(matrix, (float) (tabsBounds.getMinX() + 20), (float) (tabsBounds.getMaxY() - 4), 0.0f).uv(0.0f, 0.0f).color(0, 0, 0, lightColor).endVertex();
		tessellator.end();
		// @formatter:on
		RenderSystem.enableTexture();
		RenderSystem.shadeModel(7424);
		RenderSystem.enableAlphaTest();
		RenderSystem.disableBlend();
	}
	
	public AbstractDialog getControlsDialog() {
		final SimpleConfigGroup advanced = SimpleConfigMod.CLIENT_CONFIG.getGroup("advanced");
		final String SHOW_UI_TIPS = "show_ui_tips";
		return ControlsHelpDialog.of("simpleconfig.ui.controls")
		  .category("general", c -> c
		    .key("discard", "escape")
		    .key("save", KeyBindings.SAVE)
		    .key("undo", KeyBindings.UNDO)
		    .key("redo", KeyBindings.REDO)
		    .key("reset_restore", KeyBindings.RESET_RESTORE)
		  ).category("search", c -> c
			 .key("open", KeyBindings.SEARCH)
			 .key("next_prev", "up/down, enter/shift+enter")
			 .key("toggle_case", "alt+c!")
			 .key("toggle_tooltips", "alt+t!")
			 .key("toggle_regex", "alt+r!")
		  ).category("navigation", c -> c
			 .key("up_down", "alt+up/down")
			 .key("left", "alt+left")
			 .key("right", "alt+right")
			 .key("page.prev", KeyBindings.PREV_PAGE)
			 .key("page.next", KeyBindings.NEXT_PAGE)
			 .key("error.prev", KeyBindings.PREV_ERROR)
			 .key("error.next", KeyBindings.NEXT_ERROR)
		  ).category("lists", c -> c
			 .key("move", "ctrl+alt+up/down")
			 .key("move.drag", "mouse.middle/alt+mouse.left")
			 .key("insert", "alt+insert")
			 .key("remove", "alt+delete")
		  ).category("color", c -> c
			 .key("use", "mouse.left")
			 .key("save", "mouse.right")
			 .key("delete", "mouse.middle")
		  ).text("category.options").withCheckboxes((r, b) -> {
			  if (advanced.hasGUI(SHOW_UI_TIPS)) {
				  advanced.setGUI(SHOW_UI_TIPS, b[0]);
				  // Immediate change, without triggering external changes
				  ClientConfig.advanced.show_ui_tips = b[0];
			  } else advanced.set(SHOW_UI_TIPS, b[0]);
		  }, CheckboxButton.of(
		    advanced.getGUIBoolean(SHOW_UI_TIPS),
		    new TranslationTextComponent("simpleconfig.ui.controls.show_ui_tips")))
		  .build(this);
	}
	
	public static class ListWidget<R extends AbstractConfigEntry<?>> extends DynamicElementListWidget<R> {
		private final AbstractConfigScreen screen;
		private boolean hasCurrent;
		private double currentX;
		private double currentY;
		private double currentWidth;
		private double currentHeight;
		private EntryDragAction<?> entryDragAction;
		public Rectangle target;
		public Rectangle thisTimeTarget;
		public long lastTouch;
		public long start;
		public long duration;
		
		public static abstract class EntryDragAction<T> {
			public static class SelectionDragAction extends EntryDragAction<Boolean> {
				public SelectionDragAction(Boolean value) {
					super(value);
				}
				@Override public boolean apply(
				  INavigableTarget target, int mouseX, int mouseY
				) {
					if (target instanceof AbstractConfigListEntry<?>) {
						final AbstractConfigListEntry<?> entry = (AbstractConfigListEntry<?>) target;
						if (entry.getSelectionCheckbox().isMouseOver(mouseX, mouseY)
						    && entry.isSelectable() && entry.isSelected() != value) {
							entry.setSelected(value);
							return entry.isSelected() == value;
						}
					}
					return false;
				}
			}
			
			public static class ExpandedDragAction extends EntryDragAction<Boolean> {
				public ExpandedDragAction(Boolean value) {
					super(value);
				}
				@Override public boolean apply(
				  INavigableTarget target, int mouseX, int mouseY
				) {
					if (target instanceof IExpandable) {
						if (target instanceof AbstractConfigListEntry<?>) {
							final AbstractConfigListEntry<?> entry = (AbstractConfigListEntry<?>) target;
							if (!entry.isMouseOverRow(mouseX, mouseY)) return false;
						}
						final IExpandable expandable = (IExpandable) target;
						if (expandable.isExpanded() != value) {
							expandable.setExpanded(value);
							return expandable.isExpanded() == value;
						}
					}
					return false;
				}
			}
			
			public final T value;
			public EntryDragAction(T value) {this.value = value;}
			public void applyToList(
			  DynamicEntryListWidget<?> list, int mouseX, int mouseY
			) {
				Lists.reverse(list.getNavigableTargets(true)).stream()
				  .filter(e -> e.getRowArea().contains(mouseX, mouseY))
				  .findFirst().ifPresent(entry -> {
					  if (apply(entry, mouseX, mouseY)) {
						  onSuccess(entry, mouseX, mouseY);
					  }
				  });
			}
			public void onSuccess(INavigableTarget entry, int mouseX, int mouseY) {
				Minecraft.getInstance().getSoundManager()
				  .play(SimpleSound.forUI(SimpleConfigMod.UI_TAP, 0.6F));
			}
			public abstract boolean apply(INavigableTarget entry, int mouseX, int mouseY);
		}
		
		public ListWidget(
		  AbstractConfigScreen screen, Minecraft client, int width, int height, int top, int bottom,
		  ResourceLocation backgroundLocation
		) {
			super(client, width, height, top, bottom, backgroundLocation);
			this.screen = screen;
		}
		
		@Override public int getItemWidth() {
			return (right - left) - 80;
		}
		
		@Override public int getFieldWidth() {
			return (int) MathHelper.clamp((right - left) * 0.3F, 80, 250);
		}
		
		@Override public int getKeyFieldWidth() {
			return (int) MathHelper.clamp((right - left) * 0.25F, 80, 250);
		}
		
		@Override protected int getScrollBarPosition() {
			return right - 36;
		}
		
		public void startDragAction(EntryDragAction<?> action) {
			entryDragAction = action;
		}
		
		@Override protected void renderItem(
		  MatrixStack matrices, R item, int index, int x, int y, int entryWidth, int entryHeight,
		  int mouseX, int mouseY, boolean isHovered, float delta
		) {
			item.updateFocused(getFocusedItem() == item);
			super.renderItem(matrices, item, index, x, y, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		}
		
		@Override protected void renderList(
		  MatrixStack mStack, int startX, int startY, int mouseX, int mouseY, float delta
		) {
			// Needs to be checked even when the mouse is not moved (mouse wheel)
			if (entryDragAction != null) entryDragAction.applyToList(this, mouseX, mouseY);
			long timePast;
			thisTimeTarget = null;
			if (hasCurrent) {
				timePast = System.currentTimeMillis() - lastTouch;
				int alpha = timePast <= 200L ? 255 : MathHelper.ceil(
				  255.0 - (double) (min((float) (timePast - 200L), 500.0f) / 500.0f) * 255.0);
				alpha = alpha * 36 / 255 << 24;
				fillGradient(
				  mStack, currentX, currentY, currentX + currentWidth,
				  currentY + currentHeight, 0xFFFFFF | alpha, 0xFFFFFF | alpha);
			}
			super.renderList(mStack, startX, startY, mouseX, mouseY, delta);
			if (isDragging() || thisTimeTarget != null && !thisTimeTarget.contains(mouseX, mouseY))
				thisTimeTarget = null;
			if (thisTimeTarget != null && isMouseOver(mouseX, mouseY))
				lastTouch = System.currentTimeMillis();
			if (thisTimeTarget != null)
				thisTimeTarget = new Rectangle(0, thisTimeTarget.y, width, thisTimeTarget.height);
			if (thisTimeTarget != null && !thisTimeTarget.equals(target)) {
				if (!hasCurrent) {
					currentX = thisTimeTarget.x;
					currentY = thisTimeTarget.y;
					currentWidth = thisTimeTarget.width;
					currentHeight = thisTimeTarget.height;
					hasCurrent = true;
				}
				target = thisTimeTarget.copy();
				start = lastTouch;
				duration = 40L;
			} else if (hasCurrent && target != null) {
				timePast = System.currentTimeMillis() - start;
				// @formatter:off
				currentX = (int) ScrollingHandler.ease(currentX, target.x, min((double) timePast / duration * delta * 3.0, 1.0), EasingMethod.EasingMethodImpl.LINEAR);
				currentY = (int) ScrollingHandler.ease(currentY, target.y, min((double) timePast / duration * delta * 3.0, 1.0), EasingMethod.EasingMethodImpl.LINEAR);
				currentWidth = (int) ScrollingHandler.ease(currentWidth, target.width, min((double) timePast / duration * delta * 3.0, 1.0), EasingMethod.EasingMethodImpl.LINEAR);
				currentHeight = (int) ScrollingHandler.ease(currentHeight, target.height, min((double) timePast / (double) duration * (double) delta * 3.0, 1.0), EasingMethod.EasingMethodImpl.LINEAR);
				// @formatter:on
			}
		}
		
		@Override protected IFormattableTextComponent getEmptyPlaceHolder() {
			SearchBarWidget bar = screen.getSearchBar();
			return bar.isFilter() && bar.isExpanded()
			       ? new TranslationTextComponent("simpleconfig.ui.no_matches")
			         .withStyle(TextFormatting.GOLD)
			       : super.getEmptyPlaceHolder();
		}
		
		protected void fillGradient(
		  MatrixStack mStack, double xStart, double yStart, double xEnd, double yEnd,
		  int colorStart, int colorEnd
		) {
			RenderSystem.disableTexture();
			RenderSystem.enableBlend();
			RenderSystem.disableAlphaTest();
			RenderSystem.defaultBlendFunc();
			RenderSystem.shadeModel(7425);
			AbstractConfigScreen.fillGradient(
			  mStack, xStart, yStart, xEnd, yEnd,
			  getBlitOffset(), colorStart, colorEnd);
			RenderSystem.shadeModel(7424);
			RenderSystem.disableBlend();
			RenderSystem.enableAlphaTest();
			RenderSystem.enableTexture();
		}
		
		@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
			updateScrollingState(mouseX, mouseY, button);
			if (!isMouseOver(mouseX, mouseY))
				return false;
			for (R entry : children()) {
				if (!entry.mouseClicked(mouseX, mouseY, button)) continue;
				setFocused(entry);
				if (!isDragging())
					setDragged(Pair.of(button, entry));
				setDragging(true);
				updateSelectedTarget();
				return true;
			}
			if (button == 0) {
				clickedHeader(
				  (int) (mouseX - (double) (left + width / 2 - getItemWidth() / 2)),
				  (int) (mouseY - (double) top) + (int) getScroll() - 4);
				return true;
			}
			return scrolling;
		}
		
		@Override public boolean mouseDragged(
		  double mouseX, double mouseY, int button, double deltaX, double deltaY
		) {
			return entryDragAction != null || super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
		}
		
		@Override public void endDrag(double mouseX, double mouseY, int button) {
			super.endDrag(mouseX, mouseY, button);
			entryDragAction = null;
		}
		
		@Override
		protected void renderBackBackground(
		  MatrixStack mStack, BufferBuilder buffer, Tessellator tessellator
		) {
			if (!screen.isTransparentBackground()) {
				super.renderBackBackground(mStack, buffer, tessellator);
			} else {
				fillGradient(mStack, left, top, right, bottom, 0x68000000, 0x68000000);
			}
		}
		
		@Override
		protected void renderBarBackground(
		  MatrixStack matrices, int y1, int y2, int alpha1, int alpha2
		) {
			if (!screen.isTransparentBackground()) {
				super.renderBarBackground(matrices, y1, y2, alpha1, alpha2);
			}
		}
		
		public void onReplaced(ListWidget<R> other) {
			tick();
			if (this == other) return;
			other.setExtraScroll(getExtraScroll());
			R entry = getSelectedEntry();
			if (entry != null)
				entry.updateFocused(false);
			entry = other.getSelectedEntry();
			if (entry != null) entry.navigate();
		}
	}
	
	@Override public Pair<Integer, Integer> query(Pattern query) {
		final Pair<Integer, Integer> result = listWidget.search(query);
		final Map<ConfigCategory, Long> searches =
		  sortedCategories.stream().filter(p -> p != selectedCategory)
			 .collect(Collectors.toMap(c -> c, c -> c.getHeldEntries().stream()
				.mapToLong(e -> e.search(query).size()).sum()));
		for (ClothConfigTabButton button : tabButtons)
			button.setTintColor(searches.getOrDefault(button.category, 0L) > 0 ? 0x80BDBD42 : 0);
		return result;
	}
	
	@SuppressWarnings("RegExpUnexpectedAnchor" ) protected static final Pattern NO_MATCH =
	  Pattern.compile("$^" );
	
	@Override public void dismissQuery() {
		listWidget.search(NO_MATCH);
		tabButtons.forEach(b -> b.setTintColor(0));
	}
	
	@Override public void selectMatch(int idx) {
		listWidget.changeFocusedMatch(idx);
	}
	
	@Override public void focusResults() {
		setFocused(listWidget);
	}
	
	public static class TooltipSearchBarWidget extends SearchBarWidget {
		protected static ITextComponent[] TOOLTIP_SEARCH_TOOLTIP = new ITextComponent[]{
		  new TranslationTextComponent("simpleconfig.ui.search.tooltip" ),
		  new TranslationTextComponent("modifier.cloth-config.alt", "T" ).withStyle(
			 TextFormatting.GRAY)};
		
		protected ToggleImageButton tooltipButton;
		protected boolean searchTooltips = true;
		
		public TooltipSearchBarWidget(
		  ISearchHandler handler, int x, int y, int w, IOverlayCapableScreen screen
		) {this(handler, x, y, w, 24, screen);}
		
		public TooltipSearchBarWidget(
		  ISearchHandler handler, int x, int y, int w, int h, IOverlayCapableScreen screen
		) {
			super(handler, x, y, w, h, screen);
			tooltipButton = new ToggleImageButton(
			  searchTooltips, 0, 0, 18, 18, SimpleConfigIcons.SEARCH_TOOLTIPS,
			  b -> updateModifiers());
			addOptionButton(0, tooltipButton);
		}
		
		@Override protected void updateModifiers() {
			searchTooltips = tooltipButton.getValue();
			super.updateModifiers();
		}
		
		@Override public Optional<ITextComponent[]> getTooltip(double mouseX, double mouseY) {
			if (isExpanded() && tooltipButton.isMouseOver(mouseX, mouseY))
				return Optional.of(TOOLTIP_SEARCH_TOOLTIP);
			return super.getTooltip(mouseX, mouseY);
		}
		
		@Override public boolean charTyped(char codePoint, int modifiers) {
			if (Screen.hasAltDown()) {
				if (Character.toLowerCase(codePoint) == 't') {
					tooltipButton.setValue(!tooltipButton.getValue());
					return true;
				}
			}
			return super.charTyped(codePoint, modifiers);
		}
		
		public boolean isSearchTooltips() {
			return searchTooltips;
		}
	}
	
	@Override public TooltipSearchBarWidget getSearchBar() {
		return searchBar;
	}
	
	@Override public boolean isSelecting() {
		return isSelecting;
	}
	
	public static class EditConfigFileDialog extends ConfirmDialog {
		protected TintedButton openAndExit;
		protected final Path file;
		
		public static EditConfigFileDialog create(
		  AbstractConfigScreen screen, Path file
		) {
			return create(screen, file, null);
		}
		
		public static EditConfigFileDialog create(
		  AbstractConfigScreen screen, Path file, @Nullable Consumer<EditConfigFileDialog> builder
		) {
			EditConfigFileDialog dialog = new EditConfigFileDialog(screen, file);
			if (builder != null) builder.accept(dialog);
			return dialog;
		}
		
		protected EditConfigFileDialog(
		  AbstractConfigScreen screen, Path file
		) {
			super(screen, new TranslationTextComponent("simpleconfig.file.dialog.title"));
			withAction(b -> {
				if (b) open(file);
			});
			setBody(splitTtc(
			  "simpleconfig.file.dialog.body",
			  new StringTextComponent(file.toString()).withStyle(s -> s
				 .withColor(TextFormatting.DARK_AQUA).setUnderlined(true)
				 .withHoverEvent(new HoverEvent(
					HoverEvent.Action.SHOW_TEXT, new TranslationTextComponent("chat.copy.click")))
				 .withClickEvent(new ClickEvent(
					ClickEvent.Action.COPY_TO_CLIPBOARD, file.toString())))));
			setConfirmText(new TranslationTextComponent(
			  "simpleconfig.file.dialog.option.open_n_continue"));
			setConfirmButtonTint(0xAA8000AA);
			this.file = file;
			openAndExit = new TintedButton(0, 0, 0, 20, new TranslationTextComponent(
			  "simpleconfig.file.dialog.option.open_n_discard"), p -> {
				open(file);
				cancel(true);
				screen.quit(true);
			});
			openAndExit.setTintColor(0xAA904210);
			addButton(1, openAndExit);
		}
		
		protected static void open(Path path) {
			Util.getPlatform().openFile(path.toFile());
		}
	}
}
