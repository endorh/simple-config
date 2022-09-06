package endorh.simpleconfig.ui.gui;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Matrix4f;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.SimpleConfigMod.KeyBindings;
import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.SimpleConfig;
import endorh.simpleconfig.api.SimpleConfig.EditType;
import endorh.simpleconfig.api.SimpleConfigGroup;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons.Buttons;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons.Types;
import endorh.simpleconfig.api.ui.math.Rectangle;
import endorh.simpleconfig.config.ClientConfig;
import endorh.simpleconfig.config.ClientConfig.advanced.search;
import endorh.simpleconfig.core.SimpleConfigGUIManager;
import endorh.simpleconfig.ui.api.*;
import endorh.simpleconfig.ui.api.ConfigScreenBuilder.IConfigScreenGUIState;
import endorh.simpleconfig.ui.api.ConfigScreenBuilder.IConfigScreenGUIState.IConfigCategoryGUIState;
import endorh.simpleconfig.ui.gui.entries.CaptionedSubCategoryListEntry;
import endorh.simpleconfig.ui.gui.widget.*;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import endorh.simpleconfig.ui.gui.widget.SearchBarWidget.ISearchHandler;
import endorh.simpleconfig.ui.hotkey.ConfigHotKey;
import endorh.simpleconfig.ui.hotkey.HotKeyAction;
import endorh.simpleconfig.ui.hotkey.HotKeyActionType;
import endorh.simpleconfig.ui.hotkey.HotKeyListDialog;
import endorh.simpleconfig.ui.impl.ConfigScreenBuilderImpl.ConfigScreenGUIState;
import endorh.simpleconfig.ui.impl.ConfigScreenBuilderImpl.ConfigScreenGUIState.ConfigCategoryGUIState;
import endorh.simpleconfig.ui.impl.EasingMethod;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.HoverEvent.Action;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;
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

import static endorh.simpleconfig.api.SimpleConfigTextUtil.splitTtc;
import static endorh.simpleconfig.ui.gui.WidgetUtils.pos;
import static java.lang.Math.min;

@OnlyIn(Dist.CLIENT) public class SimpleConfigScreen
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
	protected Map<ConfigCategory, ListWidget<AbstractConfigField<?>>> listWidgets;
	protected ListWidget<AbstractConfigField<?>> listWidget;
	protected Component displayTitle;
	protected TintedButton quitButton;
	protected SaveButton saveButton;
	protected List<MultiFunctionIconButton> modeButtons = Lists.newArrayList();
	protected EnumMap<EditType, MultiFunctionIconButton> modeButtonMap;
	protected MultiFunctionIconButton clientButton;
	protected MultiFunctionIconButton commonButton;
	protected MultiFunctionIconButton remoteCommonButton;
	protected MultiFunctionIconButton serverButton;
	protected PresetPickerWidget presetPickerWidget;
	protected AbstractWidget buttonLeftTab;
	protected AbstractWidget buttonRightTab;
	protected MultiFunctionImageButton undoButton;
	protected MultiFunctionImageButton redoButton;
	protected MultiFunctionImageButton editFileButton;
	protected MultiFunctionImageButton keyboardButton;
	protected MultiFunctionImageButton settingsButton;
	protected MultiFunctionImageButton selectAllButton;
	protected MultiFunctionImageButton invertSelectionButton;
	protected MultiFunctionIconButton hotKeyButton;
	protected KeyBindButton editedHotKeyButton;
	protected TextFieldWidgetEx editedHotKeyNameTextField;
	protected SelectionToolbar selectionToolbar;
	protected Rectangle tabsBounds;
	protected Rectangle tabsLeftBounds;
	protected Rectangle tabsRightBounds;
	protected double tabsMaximumScrolled = -1.0;
	protected final List<ConfigCategoryButton> tabButtons = Lists.newArrayList();
	protected TooltipSearchBarWidget searchBar;
	protected StatusDisplayBar statusDisplayBar;
	
	protected final Set<AbstractConfigField<?>> selectedEntries = new HashSet<>();
	protected boolean isSelecting = false;
	
	protected EnumMap<EditType, ConfigCategory> lastCategories = new EnumMap<>(EditType.class);
	protected IConfigScreenGUIState prevState;
	protected boolean showingHelp = false;
	
	// Tick caches
	private List<EntryError> errors = new ArrayList<>();
	private boolean isEdited = false;
	
	@Internal public SimpleConfigScreen(
	  Screen parent, String modId, Component title,
	  Collection<ConfigCategory> clientCategories, Collection<ConfigCategory> commonCategories,
	  Collection<ConfigCategory> serverCommonCategories, Collection<ConfigCategory> serverCategories,
	  ResourceLocation backgroundLocation
	) {
		super(parent, modId, title, backgroundLocation,
		      clientCategories, commonCategories, serverCommonCategories, serverCategories);
		displayTitle = Component.literal(getModNameOrId(modId));
		for (ConfigCategory category : sortedCategories) {
			for (AbstractConfigField<?> entry : category.getHeldEntries()) {
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
		editedHotKeyButton = KeyBindButton.of(() -> this, () -> this);
		editedHotKeyButton.setTintColor(0x424280FF);
		editedHotKeyNameTextField = TextFieldWidgetEx.of("");
		editedHotKeyNameTextField.setBordered(false);
		editedHotKeyNameTextField.setEmptyHint(
		  Component.translatable("simpleconfig.ui.hotkey.unnamed_hotkey.hint"));
		minecraft = Minecraft.getInstance();
		listWidgets = new HashMap<>();
		modeButtonMap = Util.make(new EnumMap<>(EditType.class), m -> {
			m.put(SimpleConfig.EditType.CLIENT, clientButton = createModeButton(SimpleConfig.EditType.CLIENT));
			m.put(SimpleConfig.EditType.COMMON, commonButton = createModeButton(SimpleConfig.EditType.COMMON));
			m.put(SimpleConfig.EditType.SERVER_COMMON, remoteCommonButton = createModeButton(
			  SimpleConfig.EditType.SERVER_COMMON));
			m.put(SimpleConfig.EditType.SERVER, serverButton = createModeButton(SimpleConfig.EditType.SERVER));
		});
		
		editFileButton = MultiFunctionImageButton.of(20, 20, Buttons.EDIT_FILE, ButtonAction.of(
			 () -> selectedCategory.getContainingFile().ifPresent(
				f -> addDialog(EditConfigFileDialog.create(this, f.toAbsolutePath())))
		  ).active(() -> selectedCategory.getContainingFile().isPresent())
		  .tooltip(Component.translatable("simpleconfig.file.open")));
		undoButton = MultiFunctionImageButton.of(20, 20, Buttons.UNDO, ButtonAction.of(() -> {
			undo();
			setFocused(listWidget);
		}).active(() -> history.canUndo()));
		redoButton = MultiFunctionImageButton.of(20, 20, Buttons.REDO, ButtonAction.of(() -> {
			redo();
			setFocused(listWidget);
		}).active(() -> history.canRedo()));
		
		hotKeyButton = MultiFunctionIconButton.of(
		  Buttons.KEYBOARD, 20, 20,
		  ButtonAction.of(() -> addDialog(HotKeyListDialog.forModId(modId)))
			 .tooltip(Component.translatable("simpleconfig.ui.hotkey.edit")));
		
		buttonLeftTab = MultiFunctionImageButton.of(
		  12, 18, Buttons.LEFT_TAB, ButtonAction.of(
				() -> tabsScroller.offset(-48, true))
			 .active(() -> tabsScroller.scrollAmount > 0.0));
		buttonRightTab = MultiFunctionImageButton.of(
		  12, 18, Buttons.RIGHT_TAB,
		  ButtonAction.of(() -> tabsScroller.offset(48, true)).active(
			 () -> tabsScroller.scrollAmount < tabsMaximumScrolled - (double) width + 40.0));
		
		quitButton = TintedButton.of(CommonComponents.GUI_CANCEL, widget -> {
			if (isEditingConfigHotKey()) {
				discardHotkey();
			} else quit();
		});
		// quitButton.setTintColor(0x80BD4242);
		saveButton = new SaveButton(this);
		
		settingsButton = MultiFunctionImageButton.of(
		  18, 18, SimpleConfigIcons.Buttons.GEAR,
		  ButtonAction.of(() -> SimpleConfigGUIManager.showConfigGUI(SimpleConfigMod.MOD_ID))
			 .tooltip(Component.translatable("simpleconfig.ui.simple_config_settings")));
		keyboardButton = MultiFunctionImageButton.of(
		  18, 18, SimpleConfigIcons.Buttons.KEYBOARD,
		  ButtonAction.of(() -> addDialog(getControlsDialog()))
			 .tooltip(Component.translatable("simpleconfig.ui.controls")));
	}
	
	@NotNull private MultiFunctionIconButton createModeButton(EditType type) {
		String alias = type.getAlias().replace('-', '.');
		return MultiFunctionIconButton.of(Types.iconFor(type), 20, 70, ButtonAction.of(
			 () -> showType(type)
		  ).title(() -> Component.translatable("simpleconfig.config.category." + alias))
		  .tooltip(
			 () -> hasType(type) && getEditedType() != type
			       ? Lists.newArrayList(Component.translatable("simpleconfig.ui.switch." + alias))
			       : Collections.emptyList()
		  ).active(() -> hasType(type)));
	}
	
	protected static String getModNameOrId(String modId) {
		final Optional<IModInfo> first = ModList.get().getMods().stream()
		  .filter(m -> modId.equals(m.getModId())).findFirst();
		if (first.isPresent())
			return first.get().getDisplayName();
		return modId;
	}
	
	protected static final Pattern DOT = Pattern.compile("\\.");
	@Override public @Nullable AbstractConfigField<?> getEntry(String path) {
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
	
	@Override public List<AbstractConfigField<?>> getHeldEntries() {
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
			editedHotKeyButton.setMapping(hotkey.getKeyMapping());
			editedHotKeyNameTextField.setValue(hotkey.getName());
			categoryMap.forEach((alias, map) -> loadConfigHotKeyActions(hotkey, SimpleConfig.EditType.fromAlias(alias), map));
		}
		getAllMainEntries().forEach(e -> e.setEditingHotKeyAction(isEditingConfigHotKey()));
	}
	
	protected void loadConfigHotKeyActions(
	  ConfigHotKey hotkey, EditType type, Map<String, ConfigCategory> categoryMap
	) {
		Map<String, HotKeyAction<?>> actionMap = hotkey.getActions().get(Pair.of(modId, type));
		if (actionMap == null) return;
		actionMap.forEach((k, a) -> {
			String[] path = DOT.split(k, 2);
			ConfigCategory cat = categoryMap.get(path[0]);
			if (cat != null) {
				AbstractConfigField<?> entry = cat.getEntry(path[1]);
				if (entry != null) loadHotKeyAction(entry, a);
			}
		});
	}
	protected <T, V, A extends HotKeyAction<V>> void loadHotKeyAction(
	  AbstractConfigField<T> entry, A action
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
		addWidget(searchBar);
		
		int bx = 24;
		pos(editFileButton, bx, 2);
		bx += 20 + 4;
		pos(undoButton, bx, 2);
		bx += 20;
		pos(redoButton, bx, 2);
		bx += 20 + 4;
		addRenderableWidget(editFileButton);
		if (hasUndoButtons()) {
			addRenderableWidget(undoButton);
			addRenderableWidget(redoButton);
		} else bx -= 44;
		
		EditType type = getEditedType();
		clientButton.setTintColor(type == SimpleConfig.EditType.CLIENT? 0x80287734 : 0);
		commonButton.setDefaultIcon(mayHaveType(SimpleConfig.EditType.SERVER_COMMON)? Types.COMMON_CLIENT : Types.COMMON);
		commonButton.setTintColor(type == SimpleConfig.EditType.COMMON? 0x80906434 : 0);
		remoteCommonButton.setTintColor(type == SimpleConfig.EditType.SERVER_COMMON? 0x80906434 : 0);
		serverButton.setTintColor(type == SimpleConfig.EditType.SERVER? 0x800A426A : 0);
		
		clientButton.setWidthRange(20, 80);
		commonButton.setWidthRange(20, 80);
		remoteCommonButton.setExactWidth(20);
		serverButton.setWidthRange(20, 80);
		
		modeButtons = new ArrayList<>();
		if (hasType(SimpleConfig.EditType.CLIENT)) modeButtons.add(clientButton);
		if (hasType(SimpleConfig.EditType.COMMON)) {
			modeButtons.add(commonButton);
			if (mayHaveType(SimpleConfig.EditType.SERVER_COMMON)) modeButtons.add(remoteCommonButton);
		}
		if (hasType(SimpleConfig.EditType.SERVER)) modeButtons.add(serverButton);
		
		if (bx + modeButtons.stream().mapToInt(MultiFunctionIconButton::getWidth).sum() > 0.35 * width)
			modeButtons.forEach(b -> b.setExactWidth(20));
		
		for (MultiFunctionIconButton b: modeButtons) {
			pos(b, bx, 2);
			bx += b.getWidth();
			addRenderableWidget(b);
		}
		
		selectionToolbar = new SelectionToolbar(this, 76, 2);
		selectionToolbar.visible = false;
		addWidget(selectionToolbar);
		
		if (isEditingConfigHotKey()) {
			int textFieldWidth = Mth.clamp(width / 5, 100, 300);
			pos(editedHotKeyNameTextField, width / 2 - textFieldWidth / 2, 8, textFieldWidth, 12);
			addRenderableWidget(editedHotKeyNameTextField);
		}
		
		// Right toolbar
		if (!isEditingConfigHotKey()) {
			int presetPickerWidth = Mth.clamp(width / 3, 80, 250);
			presetPickerWidget.setPosition(width - presetPickerWidth - 24, 2, presetPickerWidth);
			pos(hotKeyButton, width - 22, 2);
			addWidget(presetPickerWidget);
			addRenderableWidget(hotKeyButton);
		} else {
			int hotKeyButtonWidth = Mth.clamp(width / 3, 80, 250);
			editedHotKeyButton.setPosition(width - hotKeyButtonWidth - 2, 2, hotKeyButtonWidth);
			addWidget(editedHotKeyButton);
		}
		
		// Tab bar
		if (isShowingTabs()) {
			tabsBounds = new Rectangle(0, 24, width, 24);
			tabsLeftBounds = new Rectangle(0, 24, 18, 24);
			tabsRightBounds = new Rectangle(width - 18, 24, 18, 24);
			pos(buttonLeftTab, 4, 27);
			addWidget(buttonLeftTab);
			tabButtons.clear();
			int ww = 0;
			for (ConfigCategory cat : getSortedTypeCategories()) {
				final int w = font.width(cat.getTitle());
				ww += w + 2;
				tabButtons.add(new ConfigCategoryButton(
				  this, cat, -100, 26,
				  cat.getTitle(), cat.getDescription()));
			}
			tabsMaximumScrolled = ww;
			tabButtons.forEach(this::addWidget);
			pos(buttonRightTab, width - 16, 27);
			addWidget(buttonRightTab);
		} else tabsBounds = tabsLeftBounds = tabsRightBounds = new Rectangle();
		
		// Content
		listWidget = getListWidget(selectedCategory);
		listWidget.resize(width, height, isShowingTabs()? 50 : 24, height - 28);
		if (width >= 800) {
			listWidget.setLeftPos((width - 800) / 2);
			listWidget.setRightPos(width - (width - 800) / 2);
		}
		addWidget(listWidget);
		
		// Status bar
		addRenderableWidget(statusDisplayBar);
		
		// Left controls
		selectAllButton = new MultiFunctionImageButton(
		  3, height - 21, 18, 18, SimpleConfigIcons.Buttons.SELECT_ALL,
		  ButtonAction.of(this::selectAllEntries)
			 .tooltip(Component.translatable("simpleconfig.ui.select_all")));
		addRenderableWidget(selectAllButton);
		invertSelectionButton = new MultiFunctionImageButton(
		  25, height - 21, 18, 18, SimpleConfigIcons.Buttons.INVERT_SELECTION,
		  ButtonAction.of(this::invertEntrySelection)
			 .tooltip(Component.translatable("simpleconfig.ui.invert_selection")));
		addRenderableWidget(invertSelectionButton);
		selectAllButton.visible = invertSelectionButton.visible = false;
		
		// Center controls
		int buttonWidths = min(200, (width - 88) / 3);
		int cX = width / 2;
		int by = height - 24;
		pos(quitButton, cX - buttonWidths - 2, by, buttonWidths);
		addRenderableWidget(quitButton);
		pos(saveButton, cX + 2, by, buttonWidths);
		addRenderableWidget(saveButton);
		saveButton.active = isEdited();
		
		// Right buttons
		if (!modId.equals(SimpleConfigMod.MOD_ID)) {
			pos(settingsButton, width - 41, height - 21);
			addRenderableWidget(settingsButton);
		}
		pos(keyboardButton, width - 21, height - 21);
		addRenderableWidget(keyboardButton);
		
		// Update UI mode
		isSelecting = false;
		updateSelection();
		
		// Post init
		if (afterInitConsumer != null) afterInitConsumer.accept(this);
		
		if (prevState != null) {
			IConfigScreenGUIState state = prevState;
			prevState = null;
			loadConfigScreenGUIState(state);
		}
	}
	
	public ListWidget<AbstractConfigField<?>> getListWidget(ConfigCategory category) {
		return listWidgets.computeIfAbsent(category, c -> {
			final ListWidget<AbstractConfigField<?>> w = new ListWidget<>(
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
		for (EditType t: SimpleConfig.EditType.values())
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
	
	private static <T> void resetOriginal(AbstractConfigField<T> entry) {
		entry.setOriginal(entry.getValue());
	}
	
	@Override public void setRemoteCommonConfigProvider(
	  @Nullable ConfigScreenBuilder.IRemoteConfigProvider remoteConfigProvider
	) {
		super.setRemoteCommonConfigProvider(remoteConfigProvider);
		if (getEditedType().isOnlyRemote()) {
			showType(Arrays.stream(SimpleConfig.EditType.values()).filter(
			  t -> !t.isOnlyRemote() && hasType(t)
			).findFirst().orElseThrow(
			  () -> new IllegalStateException("Config screen cannot have only remote configs")));
		}
		remoteConfigs.clear();
		loadedRemoteConfigs.clear();
		if (remoteConfigProvider != null) Arrays.stream(SimpleConfig.EditType.values())
		  .filter(SimpleConfig.EditType::isOnlyRemote)
		  .forEach(t -> remoteConfigProvider.getRemoteConfig(t).thenAccept(c -> {
			  if (c != null) {
				  remoteConfigs.put(t, c);
				  loadedRemoteConfigs.remove(t);
			  }
		  }));
	}
	
	public List<Component> getErrorsMessages() {
		return getErrors().stream().map(EntryError::getError).collect(Collectors.toList());
	}
	
	@Override protected boolean canSave() {
		return isEdited() && !hasErrors();
	}
	
	@Override public boolean isShowingHelp() {
		return showingHelp;
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
			ListWidget<AbstractConfigField<?>> prevListWidget = listWidget;
			if (width > 0) {
				init(Minecraft.getInstance(), width, height);
				if (prevListWidget != null) prevListWidget.onReplaced(listWidget);
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
					setFocused(searchBar);
				}
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
	
	@Override public @Nullable AbstractConfigField<?> getFocusedEntry() {
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
	
	@Override public Set<AbstractConfigField<?>> getSelectedEntries() {
		return selectedEntries;
	}
	
	@Override public boolean canSelectEntries() {
		return true;
	}
	
	@Override public void loadConfigScreenGUIState(@Nullable IConfigScreenGUIState state) {
		if (state != null) {
			if (width == 0) {
				prevState = state;
				return;
			}
			showType(state.getEditedType());
			state.getSelectedCategories().forEach((t, n) -> {
				ConfigCategory c = getTypeCategories(t).get(n);
				if (c != null) {
					lastCategories.put(t, c);
					if (t == getEditedType()) setSelectedCategory(c);
				}
			});
			state.getCategoryStates().forEach((t, m) -> {
				Map<String, ConfigCategory> typeCategories = getTypeCategories(t);
				m.forEach((n, s) -> {
					ConfigCategory c = typeCategories.get(n);
					if (c != null) loadConfigCategoryGUIState(c, s);
				});
			});
		}
	}
	
	protected void loadConfigCategoryGUIState(ConfigCategory category, IConfigCategoryGUIState state) {
		state.getExpandStates().forEach((p, e) -> {
			AbstractConfigField<?> entry = category.getEntry(p);
			if (entry instanceof IExpandable) ((IExpandable) entry).setExpanded(e);
		});
		ListWidget<AbstractConfigField<?>> widget = getListWidget(category);
		widget.setScroll(state.getScrollOffset());
		String selected = state.getSelectedEntry();
		if (selected != null) {
			AbstractConfigField<?> entry = category.getEntry(selected);
			if (entry != null) {
				widget.setSelectedTarget(entry);
				if (getSelectedCategory() == category) entry.navigate();
			}
		}
	}
	
	@Override public IConfigScreenGUIState saveConfigScreenGUIState() {
		ConfigScreenGUIState state = new ConfigScreenGUIState();
		state.setEditedType(getEditedType());
		Map<EditType, String> selectedCategories = state.getSelectedCategories();
		Map<EditType, Map<String, IConfigCategoryGUIState>> categoryStates = state.getCategoryStates();
		lastCategories.forEach((t, c) -> selectedCategories.put(t, c.getName()));
		selectedCategories.put(getEditedType(), getSelectedCategory().getName());
		sortedCategoriesMap.forEach((t, l) -> {
			Map<String, IConfigCategoryGUIState> states = new HashMap<>();
			categoryStates.put(t, states);
			l.forEach(c -> states.put(c.getName(), saveConfigCategoryGUIState(c)));
		});
		return state;
	}
	
	protected IConfigCategoryGUIState saveConfigCategoryGUIState(ConfigCategory category) {
		ConfigCategoryGUIState state = new ConfigCategoryGUIState();
		ListWidget<AbstractConfigField<?>> widget = listWidgets.get(category);
		Map<String, Boolean> states = state.getExpandStates();
		category.getAllEntries(e -> e instanceof IExpandable)
		  .forEach(e -> states.put(e.getCatPath(), ((IExpandable) e).isExpanded()));
		if (widget != null) {
			AbstractConfigField<?> selected = widget.getSelectedEntry();
			if (selected != null) state.setSelectedEntry(selected.getCatPath());
			state.setScrollOffset((int) widget.getScroll());
		}
		return state;
	}
	
	@Override protected void saveHotkey() {
		ConfigHotKey hotkey = editedConfigHotKey;
		if (!isEditingConfigHotKey() || hotkey == null) return;
		sortedCategoriesMap.forEach(
		  (type, categories) -> addHotKeyActions(hotkey, type, categories));
		hotkey.setName(editedHotKeyNameTextField.getValue());
		hotkey.setKeyMapping(editedHotKeyButton.getMapping());
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
			setFocused(searchBar);
	}
	
	@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (super.keyPressed(keyCode, scanCode, modifiers))
			return true;
		if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN) {
			setFocused(listWidget);
			return listWidget.keyPressed(keyCode, scanCode, modifiers);
		}
		return false;
	}
	
	@Override protected boolean screenKeyPressed(int keyCode, int scanCode, int modifiers) {
		if (super.screenKeyPressed(keyCode, scanCode, modifiers)) return true;
		Key key = InputConstants.getKey(keyCode, scanCode);
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
				final Set<AbstractConfigField<?>> selected = getSelectedEntries();
				if (Screen.hasAltDown()) {
					if (selected.stream().anyMatch(AbstractConfigField::isRestorable)) {
						selected.forEach(AbstractConfigField::restoreValue);
						playFeedbackTap(1F);
						return true;
					}
				} else if (selected.stream().anyMatch(AbstractConfigField::isResettable)) {
					selected.forEach(AbstractConfigField::resetValue);
					playFeedbackTap(1F);
					return true;
				}
			} else {
				AbstractConfigField<?> entry = listWidget.getSelectedEntry();
				while (entry != null && entry.isSubEntry()) entry = entry.getParentEntry();
				if (entry != null) {
					ResetButton resetButton = entry.getResetButton();
					if (resetButton != null && resetButton.active) {
						GuiEventListener focused = entry.getFocused();
						if (focused != resetButton) {
							if (focused instanceof AbstractWidget && ((AbstractWidget) focused).isFocused())
								WidgetUtils.forceUnFocus(focused);
							if (entry.children().contains(resetButton))
								entry.setFocused(resetButton);
							WidgetUtils.forceFocus(resetButton);
						}
						if (resetButton.activate()) return true;
					}
				}
			}
			playFeedbackTap(0.4F);
			return true;
		} else if (KeyBindings.HOTKEY.isActiveAndMatches(key)) {
			if (!isSelecting()) {
				AbstractConfigField<?> entry = listWidget.getSelectedEntry();
				while (entry != null && entry.isSubEntry()) entry = entry.getParentEntry();
				if (entry != null) {
					HotKeyActionButton<?> button = entry.getHotKeyActionTypeButton();
					if (button != null && button.active) {
						GuiEventListener focused = entry.getFocused();
						if (focused != button) {
							if (focused instanceof AbstractWidget && ((AbstractWidget) focused).isFocused())
								WidgetUtils.forceUnFocus(focused);
							if (entry.children().contains(button))
								entry.setFocused(button);
							WidgetUtils.forceFocus(button);
						}
						if (button.click(0)) return true;
					}
				}
				playFeedbackTap(0.4F);
				return true;
			}
		} else if (KeyBindings.HELP.isActiveAndMatches(key)) {
			showingHelp = !showingHelp;
			playFeedbackTap(0.4F);
			return true;
		}
		return false;
	}
	
	@Override public void updateSelection() {
		selectedEntries.clear();
		getSortedTypeCategories().stream()
		  .flatMap(c -> c.getAllMainEntries().stream())
		  .filter(AbstractConfigField::isSelected)
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
				if (modeButtons.stream().mapToInt(AbstractWidget::getWidth).sum() > 0.35 * width)
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
		if (isEditingConfigHotKey()) editedHotKeyButton.tick();
	}
	
	@Override
	public void render(@NotNull PoseStack mStack, int mouseX, int mouseY, float delta) {
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
			for (ConfigCategoryButton tabButton : tabButtons) {
				tabButton.x = xx;
				xx += tabButton.getWidth() + 2;
			}
		}
		if (isTransparentBackground()) {
			fillGradient(mStack, 0, 0, width, height, 0xa0101010, 0xb0101010);
		} else renderDirtBackground(0);
		listWidget.render(mStack, smX, smY, delta);
		
		if (!isEditingConfigHotKey()) {
			final Component title = getDisplayedTitle();
			if (isSelecting()) {
				selectionToolbar.render(mStack, smX, smY, delta);
				drawString(
				  mStack, font, Component.translatable("simpleconfig.ui.n_selected", Component.literal(String.valueOf(selectedEntries.size())).withStyle(ChatFormatting.AQUA)),
				  selectionToolbar.x + selectionToolbar.width + 6, 8, 0xffffffff);
			} else drawCenteredString(mStack, font, title, width / 2, 8, 0xffffffff);
		}
		if (isShowingTabs()) {
			Rectangle r = new Rectangle(tabsBounds.x + 20, tabsBounds.y, tabsBounds.width - 40, tabsBounds.height);
			boolean suppressHoverTabs = suppressHover || !r.contains(smX, smY);
			int smtX = suppressHoverTabs? -1 : smX;
			int smtY = suppressHoverTabs? -1 : smY;
			ScissorsHandler.INSTANCE.pushScissor(r); {
				fillGradient(mStack, r.x, r.y, r.getMaxX(), r.getMaxY(), 0x68000000, 0x68000000);
				drawTabsShades(mStack, 0, isTransparentBackground() ? 120 : 255);
				tabButtons.forEach(widget -> widget.render(mStack, smtX, smtY, delta));
			} ScissorsHandler.INSTANCE.popScissor();
			buttonLeftTab.render(mStack, smX, smY, delta);
			buttonRightTab.render(mStack, smX, smY, delta);
		}
		searchBar.render(mStack, smX, smY, delta);
		if (listWidget.isScrollingNow())
			removeTooltips(
			  new Rectangle(listWidget.left, listWidget.top, listWidget.width, listWidget.height));
		if (!isEditingConfigHotKey()) {
			presetPickerWidget.render(mStack, smX, smY, delta);
		} else editedHotKeyButton.render(mStack, smX, smY, delta);
		super.render(mStack, mouseX, mouseY, delta);
	}
	
	public Component getDisplayedTitle() {
		return displayTitle;
	}
	
	public @Nullable INavigableTarget getNext(Predicate<INavigableTarget> predicate, boolean forwards) {
		List<INavigableTarget> targets = listWidget.getNavigableTargets(false, true);
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
			  .getNavigableTargets(false, true).stream()
			  .filter(predicate).findFirst();
			if (opt.isPresent()) return opt.get();
		}
		return null;
	}
	
	public void focusNextExternalConflict(boolean forwards) {
		Predicate<INavigableTarget> predicate = t -> {
			if (!(t instanceof final AbstractConfigField<?> entry)) return false;
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
			if (!(t instanceof final AbstractConfigField<?> entry)) return false;
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
	  PoseStack mStack, int lightColor, int darkColor
	) {
		drawTabsShades(mStack.last().pose(), lightColor, darkColor);
	}
	
	private void drawTabsShades(Matrix4f matrix, int lightColor, int darkColor) {
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ZERO, DestFactor.ONE);
		RenderSystem.enableBlend();
		RenderSystem.disableDepthTest();
		RenderSystem.disableTexture();
		Tesselator tessellator = Tesselator.getInstance();
		BufferBuilder bb = tessellator.getBuilder();
		// @formatter:off
		bb.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		bb.vertex(matrix, (float) (tabsBounds.getMinX() + 20), (float) (tabsBounds.getMinY() + 4), 0F).color(0, 0, 0, lightColor).endVertex();
		bb.vertex(matrix, (float) (tabsBounds.getMaxX() - 20), (float) (tabsBounds.getMinY() + 4), 0F).color(0, 0, 0, lightColor).endVertex();
		bb.vertex(matrix, (float) (tabsBounds.getMaxX() - 20), (float) tabsBounds.getMinY(), 0F).color(0, 0, 0, darkColor).endVertex();
		bb.vertex(matrix, (float) (tabsBounds.getMinX() + 20), (float) tabsBounds.getMinY(), 0F).color(0, 0, 0, darkColor).endVertex();
		tessellator.end();
		bb.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		bb.vertex(matrix, (float) (tabsBounds.getMinX() + 20), (float) tabsBounds.getMaxY(), 0F).color(0, 0, 0, darkColor).endVertex();
		bb.vertex(matrix, (float) (tabsBounds.getMaxX() - 20), (float) tabsBounds.getMaxY(), 0F).color(0, 0, 0, darkColor).endVertex();
		bb.vertex(matrix, (float) (tabsBounds.getMaxX() - 20), (float) (tabsBounds.getMaxY() - 4), 0F).color(0, 0, 0, lightColor).endVertex();
		bb.vertex(matrix, (float) (tabsBounds.getMinX() + 20), (float) (tabsBounds.getMaxY() - 4), 0F).color(0, 0, 0, lightColor).endVertex();
		tessellator.end();
		// @formatter:on
		RenderSystem.enableTexture();
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
		    .key("hotkey", KeyBindings.HOTKEY)
		    .key("help", KeyBindings.HELP)
		  ).category("search", c -> c
			 .key("open", KeyBindings.SEARCH)
			 .key("next_prev", "up/down, enter/left.shift+enter")
			 .key("toggle_filter", "left.alt+\"f\"")
			 .key("toggle_case", "left.alt+\"c\"")
			 .key("toggle_tooltips", "left.alt+\"t\"")
			 .key("toggle_regex", "left.alt+\"r\"")
		  ).category("navigation", c -> c
			 .key("up_down", "left.alt+up/down")
			 .key("left", "left.alt+left")
			 .key("right", "left.alt+right")
			 .key("type.prev", KeyBindings.PREV_TYPE)
			 .key("type.next", KeyBindings.NEXT_TYPE)
			 .key("page.prev", KeyBindings.PREV_PAGE)
			 .key("page.next", KeyBindings.NEXT_PAGE)
			 .key("error.prev", KeyBindings.PREV_ERROR)
			 .key("error.next", KeyBindings.NEXT_ERROR)
		  ).category("lists", c -> c
			 .key("move", "left.control+left.alt+up/down")
			 .key("move.drag", "mouse.middle/left.alt+mouse.left")
			 .key("insert", "left.alt+insert")
			 .key("remove", "left.alt+delete")
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
		    Component.translatable("simpleconfig.ui.controls.show_ui_tips")))
		  .build();
	}
	
	public static class ListWidget<R extends AbstractConfigField<?>> extends DynamicElementListWidget<R> {
		private final AbstractConfigScreen screen;
		private boolean hasCurrent;
		private double currentX;
		private double currentY;
		private double currentWidth;
		private double currentHeight;
		private final ToggleAnimator tabSlideAnimator = new ToggleAnimator(100);
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
					if (target instanceof final AbstractConfigListEntry<?> entry) {
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
					if (target instanceof final IExpandable expandable) {
						if (target instanceof final AbstractConfigListEntry<?> entry) {
							if (!entry.isMouseOverRow(mouseX, mouseY)) return false;
						}
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
				Lists.reverse(list.getNavigableTargets(true, false)).stream()
				  .filter(e -> e.getRowArea().contains(mouseX, mouseY))
				  .findFirst().ifPresent(entry -> {
					  if (apply(entry, mouseX, mouseY)) {
						  onSuccess(entry, mouseX, mouseY);
					  }
				  });
			}
			public void onSuccess(INavigableTarget entry, int mouseX, int mouseY) {
				Minecraft.getInstance().getSoundManager()
				  .play(SimpleSoundInstance.forUI(SimpleConfigMod.UI_TAP, 0.6F));
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
			return (int) Mth.clamp((right - left) * 0.3F, 80, 250);
		}
		
		@Override public int getKeyFieldWidth() {
			return (int) Mth.clamp((right - left) * 0.25F, 80, 250);
		}
		
		@Override protected int getScrollBarPosition() {
			return right - 36;
		}
		
		public void startDragAction(EntryDragAction<?> action) {
			entryDragAction = action;
		}
		
		@Override protected void renderItem(
		  PoseStack matrices, R item, int index, int x, int y, int entryWidth, int entryHeight,
		  int mouseX, int mouseY, boolean isHovered, float delta
		) {
			item.updateFocused(getFocusedItem() == item);
			super.renderItem(matrices, item, index, x, y, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		}
		
		@Override protected void renderList(
		  PoseStack mStack, int startX, int startY, int mouseX, int mouseY, float delta
		) {
			boolean animated = tabSlideAnimator.isInProgress();
			if (animated) {
				mStack.pushPose();
				mStack.translate(-tabSlideAnimator.getEaseOut(), 0, 0);
			}
			/* mStack */ {
				// Needs to be checked even when the mouse is not moved (due to the mouse wheel)
				if (entryDragAction != null) entryDragAction.applyToList(this, mouseX, mouseY);
				long timePast;
				thisTimeTarget = null;
				if (hasCurrent) {
					timePast = System.currentTimeMillis() - lastTouch;
					int alpha = timePast <= 200L? 255 : Mth.ceil(
					  255.0 - (double) (min((float) (timePast - 200L), 500F) / 500F) * 255.0);
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
			if (animated) mStack.popPose();
		}
		
		@Override protected MutableComponent getEmptyPlaceHolder() {
			SearchBarWidget bar = screen.getSearchBar();
			return bar.isFilter() && bar.isExpanded()
			       ? Component.translatable("simpleconfig.ui.no_matches")
			         .withStyle(ChatFormatting.GOLD)
			       : super.getEmptyPlaceHolder();
		}
		
		protected void fillGradient(
		  PoseStack mStack, double xStart, double yStart, double xEnd, double yEnd,
		  int colorStart, int colorEnd
		) {
			RenderSystem.disableTexture();
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.setShader(GameRenderer::getPositionColorShader);
			AbstractConfigScreen.fillGradient(
			  mStack, xStart, yStart, xEnd, yEnd,
			  getBlitOffset(), colorStart, colorEnd);
			RenderSystem.disableBlend();
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
		  PoseStack mStack, BufferBuilder buffer, Tesselator tessellator
		) {
			if (!screen.isTransparentBackground()) {
				super.renderBackBackground(mStack, buffer, tessellator);
			} else {
				fillGradient(mStack, 0, top, width, bottom, 0x68000000, 0x68000000);
			}
		}
		
		@Override
		protected void renderBarBackground(
		  PoseStack mStack, int y1, int y2, int alpha1, int alpha2
		) {
			if (!screen.isTransparentBackground()) {
				super.renderBarBackground(mStack, y1, y2, alpha1, alpha2);
			}
			if (screen.isEditingConfigHotKey()) {
				fill(mStack, 0, y1, width, y2, 0x4880A0FF);
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
		setFocused(listWidget);
	}
	
	public static class TooltipSearchBarWidget extends SearchBarWidget {
		protected static Component[] TOOLTIP_SEARCH_TOOLTIP = {
		  Component.translatable("simpleconfig.ui.search.tooltip"),
		  Component.translatable("key.modifier.alt").append(" + T").withStyle(ChatFormatting.GRAY)};
		
		protected ToggleImageButton tooltipButton;
		protected boolean searchTooltips = search.search_tooltips;
		
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
			ConfigEntryHolder g = SimpleConfigMod.CLIENT_CONFIG.getChild("advanced.search");
			String SEARCH_TOOLTIPS = "search_tooltips";
			if (g.hasGUI()) {
				g.setGUI(SEARCH_TOOLTIPS, searchTooltips);
			} else g.set(SEARCH_TOOLTIPS, searchTooltips);
			super.updateModifiers();
		}
		
		@Override public Optional<Component[]> getTooltip(double mouseX, double mouseY) {
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
			super(Component.translatable("simpleconfig.file.dialog.title"));
			withAction(b -> {
				if (b) open(file);
			});
			setBody(splitTtc(
			  "simpleconfig.file.dialog.body",
			  Component.literal(file.toString()).withStyle(s -> s
				 .withColor(ChatFormatting.DARK_AQUA).withUnderlined(true)
				 .withHoverEvent(new HoverEvent(
					Action.SHOW_TEXT, Component.translatable("chat.copy.click")))
				 .withClickEvent(new ClickEvent(
					ClickEvent.Action.COPY_TO_CLIPBOARD, file.toString())))));
			setConfirmText(Component.translatable("simpleconfig.file.dialog.option.open_n_continue"));
			setConfirmButtonTint(0xAA8000AA);
			this.file = file;
			openAndExit = new TintedButton(0, 0, 0, 20, Component.translatable("simpleconfig.file.dialog.option.open_n_discard"), p -> {
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
	
	protected static class SaveButton extends TintedButton {
		private final SimpleConfigScreen screen;
		
		public SaveButton(SimpleConfigScreen screen) {
			super(0, 0, 80, 20, GameNarrator.NO_TITLE, button -> {
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
			setMessage(Component.translatable(hasErrors
			  ? "simpleconfig.ui.error_cannot_save"
			  : editingHotKey? "simpleconfig.ui.save_hotkey" : "simpleconfig.ui.save"));
		}
		
		@Override public void render(@NotNull PoseStack matrices, int mouseX, int mouseY, float delta) {
			super.render(matrices, mouseX, mouseY, delta);
		}
	}
}
