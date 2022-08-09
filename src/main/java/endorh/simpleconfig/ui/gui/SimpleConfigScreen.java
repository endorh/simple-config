package endorh.simpleconfig.ui.gui;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.SimpleConfigMod.ClientConfig;
import endorh.simpleconfig.SimpleConfigMod.KeyBindings;
import endorh.simpleconfig.core.SimpleConfig;
import endorh.simpleconfig.core.SimpleConfig.EditType;
import endorh.simpleconfig.core.SimpleConfig.Type;
import endorh.simpleconfig.core.SimpleConfigGUIManager;
import endorh.simpleconfig.core.SimpleConfigGroup;
import endorh.simpleconfig.ui.api.*;
import endorh.simpleconfig.ui.api.ConfigScreenBuilder.IConfigScreenGUIState;
import endorh.simpleconfig.ui.gui.SimpleConfigIcons.Buttons;
import endorh.simpleconfig.ui.gui.SimpleConfigIcons.Types;
import endorh.simpleconfig.ui.gui.entries.CaptionedSubCategoryListEntry;
import endorh.simpleconfig.ui.gui.widget.*;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import endorh.simpleconfig.ui.gui.widget.SearchBarWidget.ISearchHandler;
import endorh.simpleconfig.ui.hotkey.ConfigHotKey;
import endorh.simpleconfig.ui.hotkey.HotKeyAction;
import endorh.simpleconfig.ui.hotkey.HotKeyActionType;
import endorh.simpleconfig.ui.hotkey.HotKeyListDialog;
import endorh.simpleconfig.ui.impl.EasingMethod;
import endorh.simpleconfig.ui.math.Rectangle;
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
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static endorh.simpleconfig.core.SimpleConfigTextUtil.splitTtc;
import static endorh.simpleconfig.ui.gui.WidgetUtils.pos;
import static java.lang.Math.min;

@OnlyIn(value = Dist.CLIENT)
public class SimpleConfigScreen
  extends AbstractConfigScreen implements ISearchHandler {
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
	protected Map<ConfigCategory, ListWidget<AbstractConfigEntry<?>>> listWidgets;
	protected ListWidget<AbstractConfigEntry<?>> listWidget;
	protected ITextComponent displayTitle;
	protected TintedButton quitButton;
	protected SaveButton saveButton;
	protected List<MultiFunctionIconButton> modeButtons = Lists.newArrayList();
	protected EnumMap<EditType, MultiFunctionIconButton> modeButtonMap;
	protected MultiFunctionIconButton clientButton;
	protected MultiFunctionIconButton commonButton;
	protected MultiFunctionIconButton remoteCommonButton;
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
	protected MultiFunctionIconButton hotKeyButton;
	protected HotKeyButton editedHotKeyButton;
	protected TextFieldWidgetEx editedHotKeyNameTextField;
	protected SelectionToolbar selectionToolbar;
	protected Rectangle tabsBounds;
	protected Rectangle tabsLeftBounds;
	protected Rectangle tabsRightBounds;
	protected double tabsMaximumScrolled = -1.0;
	protected final List<ConfigCategoryButton> tabButtons = Lists.newArrayList();
	protected TooltipSearchBarWidget searchBar;
	protected StatusDisplayBar statusDisplayBar;
	
	protected final Set<AbstractConfigEntry<?>> selectedEntries = new HashSet<>();
	protected boolean isSelecting = false;
	
	protected EnumMap<EditType, ConfigCategory> lastCategories = new EnumMap<>(EditType.class);
	
	// Tick caches
	private List<EntryError> errors = new ArrayList<>();
	private boolean isEdited = false;
	
	@Internal public SimpleConfigScreen(
	  Screen parent, String modId, ITextComponent title,
	  Collection<ConfigCategory> clientCategories, Collection<ConfigCategory> commonCategories,
	  Collection<ConfigCategory> serverCommonCategories, Collection<ConfigCategory> serverCategories,
	  ResourceLocation backgroundLocation
	) {
		super(parent, modId, title, backgroundLocation,
		      clientCategories, commonCategories, serverCommonCategories, serverCategories);
		displayTitle = new StringTextComponent(getModNameOrId(modId));
		for (ConfigCategory category : sortedCategories) {
			for (AbstractConfigEntry<?> entry : category.getHeldEntries()) {
				entry.setCategory(category);
				entry.setScreen(this);
			}
		}
		selectedCategory = sortedCategories.stream().findFirst().orElseThrow(
		  () -> new IllegalArgumentException("No categories for config GUI"));
		lastCategories.put(selectedCategory.getType(), selectedCategory);
		statusDisplayBar = new StatusDisplayBar(this);
		searchBar = new TooltipSearchBarWidget(this, 0, 0, 256, this);
		presetPickerWidget = new PresetPickerWidget(this, 0, 0, 70);
		editedHotKeyButton = HotKeyButton.ofKeyAndMouse(() -> this, ModifierKeyCode.unknown());
		editedHotKeyButton.setTintColor(0x8080A0FF);
		editedHotKeyNameTextField = TextFieldWidgetEx.of("");
		editedHotKeyNameTextField.setBordered(false);
		editedHotKeyNameTextField.setEmptyHint(new TranslationTextComponent(
		  "simpleconfig.ui.hotkey.unnamed_hotkey.hint"));
		minecraft = Minecraft.getInstance();
		listWidgets = new HashMap<>();
		modeButtonMap = Util.make(new EnumMap<>(EditType.class), m -> {
			m.put(EditType.CLIENT, clientButton = createModeButton(EditType.CLIENT));
			m.put(EditType.COMMON, commonButton = createModeButton(EditType.COMMON));
			m.put(EditType.SERVER_COMMON, remoteCommonButton = createModeButton(EditType.SERVER_COMMON));
			m.put(EditType.SERVER, serverButton = createModeButton(EditType.SERVER));
		});
		
		editFileButton = MultiFunctionImageButton.of(20, 20, Buttons.EDIT_FILE, ButtonAction.of(
			 () -> selectedCategory.getContainingFile().ifPresent(
				f -> addDialog(EditConfigFileDialog.create(this, f.toAbsolutePath())))
		  ).active(() -> selectedCategory.getContainingFile().isPresent())
		  .tooltip(new TranslationTextComponent("simpleconfig.file.open")));
		undoButton = MultiFunctionImageButton.of(20, 20, Buttons.UNDO, ButtonAction.of(() -> {
			undo();
			setListener(listWidget);
		}).active(() -> history.canUndo()));
		redoButton = MultiFunctionImageButton.of(20, 20, Buttons.REDO, ButtonAction.of(() -> {
			redo();
			setListener(listWidget);
		}).active(() -> history.canRedo()));
		
		hotKeyButton = MultiFunctionIconButton.of(
		  Buttons.KEYBOARD, 20, 20,
		  ButtonAction.of(() -> addDialog(HotKeyListDialog.forModId(modId)))
			 .tooltip(new TranslationTextComponent("simpleconfig.ui.hotkey.edit")));
		
		buttonLeftTab = MultiFunctionImageButton.of(
		  12, 18, Buttons.LEFT_TAB, ButtonAction.of(
				() -> tabsScroller.offset(-48, true))
			 .active(() -> tabsScroller.scrollAmount > 0.0));
		buttonRightTab = MultiFunctionImageButton.of(
		  12, 18, Buttons.RIGHT_TAB,
		  ButtonAction.of(() -> tabsScroller.offset(48, true)).active(
			 () -> tabsScroller.scrollAmount < tabsMaximumScrolled - (double) width + 40.0));
		
		quitButton = TintedButton.of(DialogTexts.GUI_CANCEL, widget -> {
			if (isEditingConfigHotKey()) {
				discardHotkey();
			} else quit();
		});
		// quitButton.setTintColor(0x80BD4242);
		saveButton = new SaveButton(this);
		
		settingsButton = MultiFunctionImageButton.of(
		  18, 18, SimpleConfigIcons.Buttons.GEAR,
		  ButtonAction.of(() -> SimpleConfigGUIManager.showConfigGUI(SimpleConfigMod.MOD_ID))
			 .tooltip(new TranslationTextComponent("simpleconfig.ui.simple_config_settings")));
		keyboardButton = MultiFunctionImageButton.of(
		  18, 18, SimpleConfigIcons.Buttons.KEYBOARD,
		  ButtonAction.of(() -> addDialog(getControlsDialog()))
			 .tooltip(new TranslationTextComponent("simpleconfig.ui.controls")));
	}
	
	@NotNull private MultiFunctionIconButton createModeButton(EditType type) {
		String alias = type.getAlias().replace('-', '.');
		return MultiFunctionIconButton.of(Types.iconFor(type), 20, 70, ButtonAction.of(
			 () -> showType(type)
		  ).title(() -> new TranslationTextComponent("simpleconfig.config.category." + alias))
		  .tooltip(
			 () -> hasType(type) && getEditedType() != type
			       ? Lists.newArrayList(new TranslationTextComponent("simpleconfig.ui.switch." + alias))
			       : Collections.emptyList()
		  ).active(() -> hasType(type)));
	}
	
	protected static String getModNameOrId(String modId) {
		final Optional<ModInfo> first = ModList.get().getMods().stream()
		  .filter(m -> modId.equals(m.getModId())).findFirst();
		if (first.isPresent())
			return first.get().getDisplayName();
		return modId;
	}
	
	protected static final Pattern DOT = Pattern.compile("\\.");
	@Override public @Nullable AbstractConfigEntry<?> getEntry(String path) {
		final String[] split = DOT.split(path, 3);
		if (split.length < 3) return null;
		Map<String, ConfigCategory> map = categoryMap.get(split[0]);
		if (map == null) return null;
		ConfigCategory cat = map.get(split[1]);
		if (cat == null) return null;
		return cat.getEntry(split[2]);
	}
	
	@Override public boolean isEditingServer() {
		return super.isEditingServer() || getEditedType().isOnlyRemote();
	}
	
	@Override public List<AbstractConfigEntry<?>> getHeldEntries() {
		return sortedCategories.stream()
		  .flatMap(c -> c.getHeldEntries().stream())
		  .collect(Collectors.toList());
	}
	
	protected boolean hasUndoButtons() {
		return !isEditingConfigHotKey();
	}
	
	@Override public void setEditedConfigHotKey(
	  @Nullable ConfigHotKey hotkey, Consumer<Boolean> hotKeySaver
	) {
		super.setEditedConfigHotKey(hotkey, hotKeySaver);
		if (hotkey != null) {
			editedHotKeyButton.setKey(hotkey.getHotKey());
			editedHotKeyNameTextField.setText(hotkey.getName());
			categoryMap.forEach((alias, map) -> loadConfigHotKeyActions(hotkey, EditType.fromAlias(alias), map));
		}
		getAllMainEntries().forEach(e -> e.setEditingHotKeyAction(isEditingConfigHotKey()));
	}
	
	protected void loadConfigHotKeyActions(
	  ConfigHotKey hotkey, EditType type, Map<String, ConfigCategory> categoryMap
	) {
		Type configType = type.getType();
		if (SimpleConfig.hasConfig(modId, configType)) {
			Map<String, HotKeyAction<?>> actionMap = hotkey.getActions()
			  .get(SimpleConfig.getConfig(modId, configType));
			if (actionMap == null) return;
			actionMap.forEach((k, a) -> {
				String[] path = DOT.split(k, 2);
				ConfigCategory cat = categoryMap.get(path[0]);
				if (cat != null) {
					AbstractConfigEntry<?> entry = cat.getEntry(path[1]);
					if (entry != null) loadHotKeyAction(entry, a);
				}
			});
		}
	}
	protected <T, V, A extends HotKeyAction<V>> void loadHotKeyAction(
	  AbstractConfigEntry<T> entry, A action
	) {
		HotKeyActionType<V, ?> type = action.getType();
		List<HotKeyActionType<T, ?>> types = entry.getHotKeyActionTypes();
		int idx = types.indexOf(type);
		if (idx >= 0) {
			// noinspection unchecked
			entry.setHotKeyActionType(types.get(idx), (HotKeyAction<T>) action);
		}
	}
	
	@Override protected void init() {
		super.init();
		
		// Toolbar
		searchBar.w = width;
		addListener(searchBar);
		
		int bx = 24;
		pos(editFileButton, bx, 2);
		bx += 20 + 4;
		pos(undoButton, bx, 2);
		bx += 20;
		pos(redoButton, bx, 2);
		bx += 20 + 4;
		addButton(editFileButton);
		if (hasUndoButtons()) {
			addButton(undoButton);
			addButton(redoButton);
		} else bx -= 44;
		
		EditType type = getEditedType();
		clientButton.setTintColor(type == EditType.CLIENT? 0x80287734 : 0);
		commonButton.setDefaultIcon(mayHaveType(EditType.SERVER_COMMON)? Types.COMMON_CLIENT : Types.COMMON);
		commonButton.setTintColor(type == EditType.COMMON? 0x80906434 : 0);
		remoteCommonButton.setTintColor(type == EditType.SERVER_COMMON? 0x80906434 : 0);
		serverButton.setTintColor(type == EditType.SERVER? 0x800A426A : 0);
		
		clientButton.setWidthRange(20, 80);
		commonButton.setWidthRange(20, 80);
		remoteCommonButton.setExactWidth(20);
		serverButton.setWidthRange(20, 80);
		
		modeButtons = new ArrayList<>();
		if (hasType(EditType.CLIENT)) modeButtons.add(clientButton);
		if (hasType(EditType.COMMON)) {
			modeButtons.add(commonButton);
			if (mayHaveType(EditType.SERVER_COMMON)) modeButtons.add(remoteCommonButton);
		}
		if (hasType(EditType.SERVER)) modeButtons.add(serverButton);
		
		if (bx + modeButtons.stream().mapToInt(MultiFunctionIconButton::getWidth).sum() > 0.35 * width)
			modeButtons.forEach(b -> b.setExactWidth(20));
		
		for (MultiFunctionIconButton b: modeButtons) {
			pos(b, bx, 2);
			bx += b.getWidth();
			addButton(b);
		}
		
		selectionToolbar = new SelectionToolbar(this, 76, 2);
		selectionToolbar.visible = false;
		addListener(selectionToolbar);
		
		// Right toolbar
		if (!isEditingConfigHotKey()) {
			int presetPickerWidth = MathHelper.clamp(width / 3, 80, 250);
			presetPickerWidget.setPosition(width - presetPickerWidth - 24, 2, presetPickerWidth);
			pos(hotKeyButton, width - 22, 2);
			addListener(presetPickerWidget);
			addButton(hotKeyButton);
		}
		
		// Tab bar
		if (isShowingTabs()) {
			tabsBounds = new Rectangle(0, 24, width, 24);
			tabsLeftBounds = new Rectangle(0, 24, 18, 24);
			tabsRightBounds = new Rectangle(width - 18, 24, 18, 24);
			pos(buttonLeftTab, 4, 27);
			children.add(buttonLeftTab);
			tabButtons.clear();
			int ww = 0;
			for (ConfigCategory cat : getSortedTypeCategories()) {
				final int w = font.getStringPropertyWidth(cat.getTitle());
				ww += w + 2;
				tabButtons.add(new ConfigCategoryButton(
				  this, cat, -100, 26,
				  cat.getTitle(), cat.getDescription()));
			}
			tabsMaximumScrolled = ww;
			children.addAll(tabButtons);
			pos(buttonRightTab, width - 16, 27);
			children.add(buttonRightTab);
		} else tabsBounds = tabsLeftBounds = tabsRightBounds = new Rectangle();
		
		// Content
		listWidget = getListWidget(selectedCategory);
		listWidget.resize(width, height, isShowingTabs()? 50 : 24, height - 28);
		if (width >= 800) {
			listWidget.setLeftPos((width - 800) / 2);
			listWidget.setRightPos(width - (width - 800) / 2);
		}
		addListener(listWidget);
		
		// Status bar
		addButton(statusDisplayBar);
		
		// Left controls
		selectAllButton = new MultiFunctionImageButton(
		  2, height - 22, 20, 20, SimpleConfigIcons.Buttons.SELECT_ALL,
		  ButtonAction.of(this::selectAllEntries)
			 .tooltip(new TranslationTextComponent("simpleconfig.ui.select_all")));
		addButton(selectAllButton);
		invertSelectionButton = new MultiFunctionImageButton(
		  24, height - 22, 20, 20, SimpleConfigIcons.Buttons.INVERT_SELECTION,
		  ButtonAction.of(this::invertEntrySelection)
			 .tooltip(new TranslationTextComponent("simpleconfig.ui.invert_selection")));
		addButton(invertSelectionButton);
		selectAllButton.visible = invertSelectionButton.visible = false;
		
		// Center controls
		int buttonWidths = min(200, (width - 88) / 3);
		int cX = width / 2;
		int by = height - 24;
		if (isEditingConfigHotKey()) {
			buttonWidths = min(180, (width - 88) / 5);
			pos(editedHotKeyButton, cX - buttonWidths * 2 - 6, by);
			editedHotKeyButton.setExactWidth(buttonWidths);
			pos(editedHotKeyNameTextField, cX + buttonWidths + 6, by + 10 - font.FONT_HEIGHT / 2, buttonWidths, 20);
			addButton(editedHotKeyButton);
		}
		pos(quitButton, cX - buttonWidths - 2, by, buttonWidths);
		addButton(quitButton);
		pos(saveButton, cX + 2, by, buttonWidths);
		addButton(saveButton);
		saveButton.active = isEdited();
		if (isEditingConfigHotKey())
			addButton(editedHotKeyNameTextField);
		
		// Right buttons
		if (!modId.equals(SimpleConfigMod.MOD_ID)) {
			pos(settingsButton, width - 41, height - 21);
			addButton(settingsButton);
		}
		pos(keyboardButton, width - 21, height - 21);
		addButton(keyboardButton);
		
		// Update UI mode
		isSelecting = false;
		updateSelection();
		
		// Post init
		if (afterInitConsumer != null) afterInitConsumer.accept(this);
	}
	
	public ListWidget<AbstractConfigEntry<?>> getListWidget(ConfigCategory category) {
		return listWidgets.computeIfAbsent(category, c -> {
			final ListWidget<AbstractConfigEntry<?>> w = new ListWidget<>(
			  this, minecraft, width, height, isShowingTabs()? 50 : 24,
			  height - 28, c.getBackground() != null? c.getBackground() : backgroundLocation);
			w.getEntries().addAll(c.getHeldEntries());
			return w;
		});
	}
	
	@Override public List<EntryError> getErrors() {
		return errors;
	}
	
	public void updateErrors() {
		List<EntryError> errors = Lists.newArrayList();
		EditType type = getEditedType();
		// First add for the current type
		updateErrors(errors, getSortedTypeCategories());
		// Then for the other types
		for (EditType t: EditType.values())
			if (t != type) updateErrors(errors, sortedCategoriesMap.get(t));
		this.errors = errors;
	}
	
	protected void updateErrors(List<EntryError> errors, List<ConfigCategory> categories) {
		for (ConfigCategory cat: categories) errors.addAll(cat.getErrors());
	}
	
	public void showType(EditType type) {
		EditType prevType = getEditedType();
		if (hasType(type) && prevType != type) {
			ConfigCategory lastCategory = lastCategories.get(type);
			setSelectedCategory(lastCategory != null? lastCategory : sortedCategoriesMap.get(type).get(0));
			if (!hasLoadedType(type)) loadType(type);
		}
	}
	
	protected void loadType(EditType type) {
		if (hasLoadedType(type)) return;
		if (remoteConfigProvider == null) throw new IllegalStateException("Missing remote config provider");
		CommentedConfig config = remoteConfigs.get(type);
		if (config == null) throw new IllegalStateException("Can't load type yet: " + type.getAlias());
		history.runUnrecordedAction(() -> remoteConfigProvider.loadRemoteConfig(type, config, false));
		sortedCategoriesMap.get(type).stream()
		  .flatMap(c -> c.getAllMainEntries().stream())
		  .forEach(SimpleConfigScreen::resetOriginal);
		loadedRemoteConfigs.add(type);
	}
	
	private static <T> void resetOriginal(AbstractConfigEntry<T> entry) {
		entry.setOriginal(entry.getValue());
	}
	
	@Override public void setRemoteCommonConfigProvider(
	  @Nullable ConfigScreenBuilder.IRemoteConfigProvider remoteConfigProvider
	) {
		super.setRemoteCommonConfigProvider(remoteConfigProvider);
		if (getEditedType().isOnlyRemote()) {
			showType(Arrays.stream(EditType.values()).filter(
			  t -> !t.isOnlyRemote() && hasType(t)
			).findFirst().orElseThrow(
			  () -> new IllegalStateException("Config screen cannot have only remote configs")));
		}
		remoteConfigs.clear();
		loadedRemoteConfigs.clear();
		if (remoteConfigProvider != null) Arrays.stream(EditType.values())
		  .filter(EditType::isOnlyRemote)
		  .forEach(t -> remoteConfigProvider.getRemoteConfig(t).thenAccept(c -> {
			  if (c != null) {
				  remoteConfigs.put(t, c);
				  loadedRemoteConfigs.remove(t);
			  }
		  }));
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
			int prevIndex = sortedCategories.indexOf(selectedCategory);
			int index = sortedCategories.indexOf(category);
			boolean typeChange = getEditedType() != category.getType();
			if (isSelecting() && typeChange) return;
			lastCategories.put(selectedCategory.getType(), selectedCategory);
			super.setSelectedCategory(category);
			ListWidget<AbstractConfigEntry<?>> prevListWidget = listWidget;
			init(Minecraft.getInstance(), width, height);
			prevListWidget.onReplaced(listWidget);
			if (typeChange) presetPickerWidget.refresh();
			listWidget.playTabSlideAnimation(prevIndex > index);
			if (isShowingTabs()) {
				final int innerIndex = getSortedTypeCategories().indexOf(category);
				int x = 0;
				for (int i = 0; i < innerIndex; i++)
					x += tabButtons.get(i).getWidth() + 2;
				x += tabButtons.get(innerIndex).getWidth() / 2;
				x -= tabsScroller.getBounds().width / 2;
				tabsScroller.scrollTo(x, true, 250L);
			}
			searchBar.refresh();
			if (searchBar.isExpanded()) {
				setListener(searchBar);
			}
		}
	}
	@Override public boolean isEdited() {
		return isEdited;
	}
	
	protected void updateIsEdited() {
		isEdited = super.isEdited();
	}
	
	@Override public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		if (tabsBounds.contains(mouseX, mouseY) &&
		    !tabsLeftBounds.contains(mouseX, mouseY) &&
		    !tabsRightBounds.contains(mouseX, mouseY) && amount != 0.0) {
			tabsScroller.offset(-amount * 16.0, true);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, amount);
	}
	
	@Override public @Nullable AbstractConfigEntry<?> getFocusedEntry() {
		return listWidget.getSelectedEntry();
	}
	
	public void selectAllEntries() {
		selectedCategory.getHeldEntries().forEach(e -> e.setSelected(true));
	}
	
	public void invertEntrySelection() {
		selectedCategory.getAllMainEntries().stream()
		  .filter(e -> !(e instanceof CaptionedSubCategoryListEntry) && e.isSelectable())
		  .forEach(e -> e.setSelected(!e.isSelected()));
	}
	
	@Override public Set<AbstractConfigEntry<?>> getSelectedEntries() {
		return selectedEntries;
	}
	
	@Override public boolean canSelectEntries() {
		return true;
	}
	
	@Override public void loadConfigScreenGUIState(IConfigScreenGUIState state) {
		if (state != null) {
			// TODO
		}
	}
	@Override public IConfigScreenGUIState saveConfigScreenGUIState() {
		return super.saveConfigScreenGUIState();
	}
	
	@Override protected void saveHotkey() {
		ConfigHotKey hotkey = editedConfigHotKey;
		if (!isEditingConfigHotKey() || hotkey == null) return;
		sortedCategoriesMap.forEach((type, categories) -> {
			addHotKeyActions(hotkey, type, categories);
		});
		hotkey.setName(editedHotKeyNameTextField.getText());
		hotkey.setKeyCode(editedHotKeyButton.getKey());
		super.saveHotkey();
	}
	
	private void addHotKeyActions(
	  ConfigHotKey hotKey, EditType type, Collection<ConfigCategory> categories
	) {
		Map<Pair<String, EditType>, Map<String, HotKeyAction<?>>> actions = hotKey.getActions();
		Pair<String, EditType> pair = Pair.of(modId, type);
		Map<String, HotKeyAction<?>> actionMap = actions
		  .computeIfAbsent(pair, k -> new LinkedHashMap<>());
		actionMap.clear();
		categories.stream()
		  .flatMap(c -> c.getAllMainEntries().stream())
		  .filter(e -> e.getHotKeyActionType() != null)
		  .forEach(e -> {
			  String path = e.getRelPath();
			  HotKeyAction<?> action = e.createHotKeyAction();
			  if (action != null) actionMap.put(path, action);
		  });
		if (actionMap.isEmpty()) actions.remove(pair);
	}
	
	@Override protected void recomputeFocus() {
		if (searchBar.isExpanded() && !hasDialogs())
			setListener(searchBar);
	}
	
	@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (super.keyPressed(keyCode, scanCode, modifiers))
			return true;
		if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN) {
			setListener(listWidget);
			return listWidget.keyPressed(keyCode, scanCode, modifiers);
		}
		return false;
	}
	
	@Override protected boolean screenKeyPressed(int keyCode, int scanCode, int modifiers) {
		if (super.screenKeyPressed(keyCode, scanCode, modifiers)) return true;
		InputMappings.Input key = InputMappings.getInputByCode(keyCode, scanCode);
		// Navigation key bindings first
		if (KeyBindings.NEXT_TYPE.isActiveAndMatches(key)) {
			MultiFunctionIconButton modeButton = modeButtonMap.get(getEditedType());
			int i = modeButtons.indexOf(modeButton);
			if (i < 0) return true;
			int s = modeButtons.size();
			i = (i + 1) % s;
			while (modeButton != modeButtons.get(i) && !modeButtons.get(i).active)
				i = (i + 1) % s;
			if (modeButton != modeButtons.get(i)) modeButtons.get(i).press(0);
			return true;
		} else if(KeyBindings.PREV_TYPE.isActiveAndMatches(key)) {
			MultiFunctionIconButton modeButton = modeButtonMap.get(getEditedType());
			int i = modeButtons.indexOf(modeButton);
			if (i < 0) return true;
			int s = modeButtons.size();
			i = (i + s - 1) % s;
			while (modeButton != modeButtons.get(i) && !modeButtons.get(i).active)
				i = (i + s - 1) % s;
			if (modeButton != modeButtons.get(i)) modeButtons.get(i).press(0);
			return true;
		} else if (KeyBindings.NEXT_ERROR.isActiveAndMatches(key)) { // F1
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
			setListener(searchBar);
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
						IGuiEventListener focused = entry.getListener();
						if (focused != resetButton) {
							if (focused instanceof Widget && ((Widget) focused).isFocused())
								WidgetUtils.forceUnFocus(focused);
							if (entry.getEventListeners().contains(resetButton))
								entry.setListener(resetButton);
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
	
	@Override public void updateSelection() {
		selectedEntries.clear();
		getSortedTypeCategories().stream()
		  .flatMap(c -> c.getAllMainEntries().stream())
		  .filter(AbstractConfigEntry::isSelected)
		  .forEach(selectedEntries::add);
		final boolean selecting = !selectedEntries.isEmpty();
		if (selecting != isSelecting) {
			if (selecting) {
				if (hasUndoButtons()) undoButton.visible = redoButton.visible = false;
				selectAllButton.visible = invertSelectionButton.visible = true;
				MultiFunctionIconButton visible = modeButtonMap.get(getEditedType());
				modeButtons.stream().filter(b -> b != visible).forEach(b -> b.visible = false);
				visible.setExactWidth(20);
				visible.x = editFileButton.x + 24;
				selectionToolbar.visible = true;
			} else {
				int bx = 48;
				if (hasUndoButtons()) {
					undoButton.visible = redoButton.visible = true;
					bx += 44;
				}
				selectAllButton.visible = invertSelectionButton.visible = false;
				modeButtons.forEach(b -> b.setWidthRange(20, 70));
				remoteCommonButton.setExactWidth(20);
				if (modeButtons.stream().mapToInt(Widget::getWidth).sum() > 0.35 * width)
					modeButtons.forEach(b -> b.setExactWidth(20));
				for (MultiFunctionIconButton b: modeButtons) {
					b.x = bx;
					bx += b.getWidth();
					b.visible = true;
				}
				selectionToolbar.visible = false;
			}
			isSelecting = selecting;
		}
	}
	
	@Override public void tick() {
		super.tick();
		listWidget.tick();
		updateErrors();
		updateIsEdited();
		statusDisplayBar.tick();
		saveButton.tick();
	}
	
	@Override
	public void render(@NotNull MatrixStack mStack, int mouseX, int mouseY, float delta) {
		if (minecraft == null) return;
		final boolean hasDialog = hasDialogs();
		boolean suppressHover = hasDialog || shouldOverlaysSuppressHover(mouseX, mouseY);
		final int smX = suppressHover ? -1 : mouseX;
		final int smY = suppressHover ? -1 : mouseY;
		if (getListener() == null || getListener() == searchBar && !searchBar.isExpanded())
			setListener(listWidget);
		if (isShowingTabs()) {
			tabsScroller.updatePosition(delta * 3.0f);
			int xx = 24 - (int) tabsScroller.scrollAmount;
			for (ConfigCategoryButton tabButton : tabButtons) {
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
					String.valueOf(selectedEntries.size())).mergeStyle(TextFormatting.AQUA)),
			  selectionToolbar.x + selectionToolbar.width + 6, 8, 0xffffffff);
		} else drawCenteredString(mStack, font, title, width / 2, 8, 0xffffffff);
		if (isShowingTabs()) {
			Rectangle r = new Rectangle(tabsBounds.x + 20, tabsBounds.y, tabsBounds.width - 40, tabsBounds.height);
			ScissorsHandler.INSTANCE.pushScissor(r); {
				if (isTransparentBackground()) {
					fillGradient(mStack, r.x, r.y, r.getMaxX(), r.getMaxY(), 0x68000000, 0x68000000);
				} else {
					overlayBackground(mStack, r, 32, 32, 32);
				}
				tabButtons.forEach(widget -> widget.render(mStack, smX, smY, delta));
				drawTabsShades(mStack, 0, isTransparentBackground() ? 120 : 255);
			} ScissorsHandler.INSTANCE.popScissor();
			buttonLeftTab.render(mStack, smX, smY, delta);
			buttonRightTab.render(mStack, smX, smY, delta);
		}
		editFileButton.render(mStack, mouseX, mouseY, delta);
		searchBar.render(mStack, hasDialog ? -1 : mouseX, hasDialog ? -1 : mouseY, delta);
		if (listWidget.isScrollingNow())
			removeTooltips(
			  new Rectangle(listWidget.left, listWidget.top, listWidget.width, listWidget.height));
		if (!isEditingConfigHotKey())
			presetPickerWidget.render(mStack, mouseX, mouseY, delta);
		super.render(mStack, mouseX, mouseY, delta);
	}
	
	public ITextComponent getDisplayedTitle() {
		if (isEditingConfigHotKey())
			return new TranslationTextComponent("simpleconfig.ui.title.editing_hotkey", displayTitle);
		return displayTitle;
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
			Optional<INavigableTarget> opt = getListWidget(cat)
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
	
	public void focusNextRequiresRestart(boolean forwards) {
		Predicate<INavigableTarget> predicate = t -> {
			if (!(t instanceof AbstractConfigEntry<?>)) return false;
			final AbstractConfigEntry<?> entry = (AbstractConfigEntry<?>) t;
			return !entry.isSubEntry() && entry.isRequiresRestart() && entry.isEdited();
		};
		INavigableTarget next = getNext(predicate.and(INavigableTarget::isNavigable), forwards);
		boolean foundVisible = next != null;
		if (next == null) next = getNext(predicate, forwards);
		if (next != null) {
			if (!foundVisible) getSearchBar().close();
			next.navigate();
			next.applyWarningHighlight();
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
		drawTabsShades(mStack.getLast().getMatrix(), lightColor, darkColor);
	}
	
	private void drawTabsShades(Matrix4f matrix, int lightColor, int darkColor) {
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(770, 771, 0, 1);
		RenderSystem.disableAlphaTest();
		RenderSystem.shadeModel(7425);
		RenderSystem.disableTexture();
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		// @formatter:off
		buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
		buffer.pos(matrix, (float) (tabsBounds.getMinX() + 20), (float) (tabsBounds.getMinY() + 4), 0.0f).tex(0.0f, 1.0f).color(0, 0, 0, lightColor).endVertex();
		buffer.pos(matrix, (float) (tabsBounds.getMaxX() - 20), (float) (tabsBounds.getMinY() + 4), 0.0f).tex(1.0f, 1.0f).color(0, 0, 0, lightColor).endVertex();
		buffer.pos(matrix, (float) (tabsBounds.getMaxX() - 20), (float) tabsBounds.getMinY(), 0.0f).tex(1.0f, 0.0f).color(0, 0, 0, darkColor).endVertex();
		buffer.pos(matrix, (float) (tabsBounds.getMinX() + 20), (float) tabsBounds.getMinY(), 0.0f).tex(0.0f, 0.0f).color(0, 0, 0, darkColor).endVertex();
		tessellator.draw();
		buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
		buffer.pos(matrix, (float) (tabsBounds.getMinX() + 20), (float) tabsBounds.getMaxY(), 0.0f).tex(0.0f, 1.0f).color(0, 0, 0, darkColor).endVertex();
		buffer.pos(matrix, (float) (tabsBounds.getMaxX() - 20), (float) tabsBounds.getMaxY(), 0.0f).tex(1.0f, 1.0f).color(0, 0, 0, darkColor).endVertex();
		buffer.pos(matrix, (float) (tabsBounds.getMaxX() - 20), (float) (tabsBounds.getMaxY() - 4), 0.0f).tex(1.0f, 0.0f).color(0, 0, 0, lightColor).endVertex();
		buffer.pos(matrix, (float) (tabsBounds.getMinX() + 20), (float) (tabsBounds.getMaxY() - 4), 0.0f).tex(0.0f, 0.0f).color(0, 0, 0, lightColor).endVertex();
		tessellator.draw();
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
			 .key("type.prev", KeyBindings.PREV_TYPE)
			 .key("type.next", KeyBindings.NEXT_TYPE)
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
		  .build();
	}
	
	public static class ListWidget<R extends AbstractConfigEntry<?>> extends DynamicElementListWidget<R> {
		private final AbstractConfigScreen screen;
		private boolean hasCurrent;
		private double currentX;
		private double currentY;
		private double currentWidth;
		private double currentHeight;
		private ToggleAnimator tabSlideAnimator = new ToggleAnimator(100);
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
				Minecraft.getInstance().getSoundHandler()
				  .play(SimpleSound.master(SimpleConfigMod.UI_TAP, 0.6F));
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
		
		public void playTabSlideAnimation(boolean fromRight) {
			tabSlideAnimator.resetTarget();
			tabSlideAnimator.setOutputRange(fromRight? width * 0.2F : -width * 0.2F, 0);
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
			boolean animated = tabSlideAnimator.isInProgress();
			if (animated) {
				mStack.push();
				mStack.translate(-tabSlideAnimator.getEaseOut(), 0, 0);
			}
			/* mStack */ {
				// Needs to be checked even when the mouse is not moved (due to the mouse wheel)
				if (entryDragAction != null) entryDragAction.applyToList(this, mouseX, mouseY);
				long timePast;
				thisTimeTarget = null;
				if (hasCurrent) {
					timePast = System.currentTimeMillis() - lastTouch;
					int alpha = timePast <= 200L? 255 : MathHelper.ceil(
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
			if (animated) mStack.pop();
		}
		
		@Override protected IFormattableTextComponent getEmptyPlaceHolder() {
			SearchBarWidget bar = screen.getSearchBar();
			return bar.isFilter() && bar.isExpanded()
			       ? new TranslationTextComponent("simpleconfig.ui.no_matches")
			         .mergeStyle(TextFormatting.GOLD)
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
			for (R entry : getEventListeners()) {
				if (!entry.mouseClicked(mouseX, mouseY, button)) continue;
				setListener(entry);
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
				fillGradient(mStack, 0, top, width, bottom, 0x68000000, 0x68000000);
			}
		}
		
		@Override
		protected void renderBarBackground(
		  MatrixStack matrices, int y1, int y2, int alpha1, int alpha2
		) {
			if (!screen.isTransparentBackground()) {
				super.renderBarBackground(matrices, y1, y2, alpha1, alpha2);
			}
			if (screen.isEditingConfigHotKey()) {
				fill(new MatrixStack(), 0, y1, width, y2, 0x4880A0FF);
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
		for (ConfigCategoryButton button : tabButtons)
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
		setListener(listWidget);
	}
	
	public static class TooltipSearchBarWidget extends SearchBarWidget {
		protected static ITextComponent[] TOOLTIP_SEARCH_TOOLTIP = new ITextComponent[]{
		  new TranslationTextComponent("simpleconfig.ui.search.tooltip" ),
		  new TranslationTextComponent("modifier.cloth-config.alt", "T" ).mergeStyle(
			 TextFormatting.GRAY)};
		
		protected ToggleImageButton tooltipButton;
		protected boolean searchTooltips = true;
		
		public TooltipSearchBarWidget(
		  ISearchHandler handler, int x, int y, int w, IDialogCapableScreen screen
		) {this(handler, x, y, w, 24, screen);}
		
		public TooltipSearchBarWidget(
		  ISearchHandler handler, int x, int y, int w, int h, IDialogCapableScreen screen
		) {
			super(handler, x, y, w, h, screen);
			tooltipButton = new ToggleImageButton(
			  searchTooltips, 0, 0, 18, 18, SimpleConfigIcons.SearchBar.SEARCH_TOOLTIPS,
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
			super(new TranslationTextComponent("simpleconfig.file.dialog.title"));
			withAction(b -> {
				if (b) open(file);
			});
			setBody(splitTtc(
			  "simpleconfig.file.dialog.body",
			  new StringTextComponent(file.toString()).modifyStyle(s -> s
				 .setFormatting(TextFormatting.DARK_AQUA).setUnderlined(true)
				 .setHoverEvent(new HoverEvent(
					HoverEvent.Action.SHOW_TEXT, new TranslationTextComponent("chat.copy.click")))
				 .setClickEvent(new ClickEvent(
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
			Util.getOSType().openFile(path.toFile());
		}
	}
	
	protected static class SaveButton extends TintedButton {
		private final SimpleConfigScreen screen;
		
		public SaveButton(SimpleConfigScreen screen) {
			super(0, 0, 80, 20, NarratorChatListener.EMPTY, button -> {
				if (screen.isEditingConfigHotKey()) {
					screen.saveHotkey();
				} else screen.saveAll(true);
			});
			this.screen = screen;
			setTintColor(0x8042BD42);
		}
		
		public void tick() {
			boolean hasErrors = screen.hasErrors();
			active = !hasErrors && screen.isEdited();
			boolean editingHotKey = screen.isEditingConfigHotKey();
			setMessage(new TranslationTextComponent(
			  hasErrors
			  ? "simpleconfig.ui.error_cannot_save"
			  : editingHotKey? "simpleconfig.ui.save_hotkey" : "simpleconfig.ui.save"));
		}
		
		@Override public void render(@NotNull MatrixStack matrices, int mouseX, int mouseY, float delta) {
			super.render(matrices, mouseX, mouseY, delta);
		}
	}
}
