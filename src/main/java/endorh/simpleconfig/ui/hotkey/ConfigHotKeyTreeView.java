package endorh.simpleconfig.ui.hotkey;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.core.SimpleConfig;
import endorh.simpleconfig.ui.api.IDialogCapableScreen;
import endorh.simpleconfig.ui.api.IModalInputCapableScreen;
import endorh.simpleconfig.ui.api.IOverlayCapableContainer;
import endorh.simpleconfig.ui.api.Tooltip;
import endorh.simpleconfig.ui.gui.SimpleConfigIcons;
import endorh.simpleconfig.ui.gui.widget.*;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import endorh.simpleconfig.ui.gui.widget.treeview.ArrangeableTreeView;
import endorh.simpleconfig.ui.gui.widget.treeview.ArrangeableTreeViewCaption;
import endorh.simpleconfig.ui.gui.widget.treeview.ArrangeableTreeViewEntry;
import endorh.simpleconfig.ui.gui.widget.treeview.DragBroadcastableControl.DragBroadcastableAction.WidgetDragBroadcastableAction;
import endorh.simpleconfig.ui.gui.widget.treeview.DragBroadcastableControl.DragBroadcastableWidget;
import endorh.simpleconfig.ui.hotkey.ConfigHotKeyManager.ConfigHotKeyGroup;
import endorh.simpleconfig.ui.hotkey.ConfigHotKeyTreeView.ConfigHotKeyTreeViewEntry;
import endorh.simpleconfig.ui.hotkey.ConfigHotKeyTreeView.ConfigHotKeyTreeViewEntry.ConfigHotKeyTreeViewGroupEntry;
import endorh.simpleconfig.ui.hotkey.ConfigHotKeyTreeView.ConfigHotKeyTreeViewEntry.ConfigHotKeyTreeViewHotKeyEntry;
import endorh.simpleconfig.ui.math.Point;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.*;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.config.ModConfig.Type;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static endorh.simpleconfig.ui.gui.WidgetUtils.pos;
import static endorh.simpleconfig.ui.gui.WidgetUtils.renderAll;
import static java.lang.Math.min;
import static net.minecraft.util.math.MathHelper.clamp;

public class ConfigHotKeyTreeView extends ArrangeableTreeView<ConfigHotKeyTreeViewEntry> {
	private final Supplier<IDialogCapableScreen> dialogScreenSupplier;
	private @Nullable String contextModId = null;
	private final BiConsumer<String, ConfigHotKey> hotKeyEditor;
	public static final WidgetDragBroadcastableAction<CheckboxButton> ENABLE_ACTION = (b, s) -> b.setToggle(s.getValue());
	
	public ConfigHotKeyTreeView(
	  Supplier<IDialogCapableScreen> screenSupplier, IOverlayCapableContainer overlayContainer,
	  ConfigHotKeyGroup group, BiConsumer<String, ConfigHotKey> hotKeyEditor
	) {
		super(overlayContainer, new ConfigHotKeyTreeViewGroupEntry(screenSupplier::get, group));
		dialogScreenSupplier = screenSupplier;
		this.hotKeyEditor = hotKeyEditor;
		setCaption(new ConfigHotKeyTreeViewCaption(screenSupplier, overlayContainer, this));
		setPlaceHolder(new TranslationTextComponent("simpleconfig.ui.no_hotkeys"));
	}
	
	@SuppressWarnings("unchecked") public <T extends Screen & IDialogCapableScreen> T getDialogScreen() {
		return (T) dialogScreenSupplier.get();
	}
	
	public @Nullable String getContextModId() {
		return contextModId;
	}
	public void setContextModId(@Nullable String contextModId) {
		this.contextModId = contextModId;
	}
	
	protected BiConsumer<String, ConfigHotKey> getHotKeyEditor() {
		return hotKeyEditor;
	}
	
	public void addHotKey() {
		tryAddEntry(new ConfigHotKeyTreeViewHotKeyEntry(this::getDialogScreen, new ConfigHotKey()));
	}
	
	public void addGroup() {
		tryAddEntry(new ConfigHotKeyTreeViewGroupEntry(this::getDialogScreen, new ConfigHotKeyGroup()));
	}
	
	public static class ConfigHotKeyTreeViewCaption
	  extends ArrangeableTreeViewCaption<ConfigHotKeyTreeViewEntry> {
		private final SavedHotKeyGroupPickerWidget savedHotKeyGroupPickerWidget;
		private final Supplier<IDialogCapableScreen> screen;
		
		protected ConfigHotKeyTreeViewCaption(
		  Supplier<IDialogCapableScreen> screen, IOverlayCapableContainer overlayContainer,
		  ConfigHotKeyTreeView tree
		) {
			super(tree);
			this.screen = screen;
			addControl(MultiFunctionIconButton.of(
			  SimpleConfigIcons.Widgets.TREE_ADD, 20, 20, ButtonAction.of(
					tree::addHotKey
				 ).tooltip(new TranslationTextComponent("simpleconfig.ui.hotkey.dialog.add.hotkey"))
				 .tint(0x6480FF80)
			));
			addControl(MultiFunctionIconButton.of(
			  SimpleConfigIcons.Widgets.TREE_ADD_GROUP, 20, 20, ButtonAction.of(
				  tree::addGroup
				 ).tooltip(new TranslationTextComponent("simpleconfig.ui.hotkey.dialog.add.group"))
				 .tint(0x6480FF80)
			));
			addControl(MultiFunctionIconButton.of(
			  SimpleConfigIcons.Widgets.TREE_REMOVE, 20, 20, ButtonAction.of(
					tree::removeSelection
				 ).tooltip(new TranslationTextComponent("simpleconfig.ui.hotkey.dialog.remove"))
				 .tint(0x64FF8080)
				 .active(() -> !tree.getSelection().isEmpty())
			));
			savedHotKeyGroupPickerWidget =
			  new SavedHotKeyGroupPickerWidget(screen, overlayContainer, tree);
			addRightControl(savedHotKeyGroupPickerWidget);
		}
		
		public IDialogCapableScreen getScreen() {
			IDialogCapableScreen screen = this.screen.get();
			if (screen == null) throw new IllegalStateException(
			  "Cannot get screen so early.");
			return screen;
		}
		
		@Override protected int getHeight() {
			return 20;
		}
		
		@Override public void render(MatrixStack mStack, int mouseX, int mouseY, float delta) {
			int width = getTree().getWidth();
			savedHotKeyGroupPickerWidget.setWidth(clamp((int) (width * 0.45), 90, 300));
			super.render(mStack, mouseX, mouseY, delta);
		}
	}
	
	public static abstract class ConfigHotKeyTreeViewEntry
	  extends ArrangeableTreeViewEntry<ConfigHotKeyTreeViewEntry> {
		@Override public ConfigHotKeyTreeView getTree() {
			return (ConfigHotKeyTreeView) super.getTree();
		}
		
		@Override public int getOwnHeight() {
			return 22;
		}
		
		@Override public int getVerticalPadding() {
			return 1;
		}
		
		@Override public boolean handleNavigationKey(int keyCode, int scanCode, int modifiers) {
			if (super.handleNavigationKey(keyCode, scanCode, modifiers)) return true;
			if (Screen.hasAltDown()) {
				ConfigHotKeyTreeView tree = getTree();
				if (keyCode == GLFW.GLFW_KEY_INSERT) {
					if (Screen.hasShiftDown()) {
						tree.addGroup();
					} else tree.addHotKey();
					return true;
				} else if (keyCode == GLFW.GLFW_KEY_DELETE) {
					tree.removeSelection();
					return true;
				}
			}
			return false;
		}
		
		public static class ConfigHotKeyTreeViewHotKeyEntry extends ConfigHotKeyTreeViewEntry {
			private final ConfigHotKey hotKey;
			private final TextFieldWidgetEx textField;
			private final HotKeyButton hotKeyButton;
			private final DragBroadcastableWidget<CheckboxButton> enabledCheckbox;
			private final Map<String, ConfigHotKeyTreeViewModEntry> entries = new HashMap<>();
			private final Comparator<String> modOrder = (l, r) -> {
				ConfigHotKeyTreeView tree = getTree();
				String contextModId = tree.getContextModId();
				return new CompareToBuilder()
				  .append(!l.equals(contextModId), !r.equals(contextModId))
				  .append(entries.get(l).getCount(), entries.get(r).getCount())
				  .append(l, r)
				  .build();
			};
			
			public ConfigHotKeyTreeViewHotKeyEntry(
			  Supplier<IModalInputCapableScreen> screenSupplier, ConfigHotKey hotKey
			) {
				hotKey = hotKey.copy();
				this.hotKey = hotKey;
				textField = TextFieldWidgetEx.of(hotKey.getName());
				textField.setMaxLength(256);
				textField.setBordered(false);
				textField.setEmptyHint(new TranslationTextComponent(
				  "simpleconfig.ui.hotkey.unnamed.hint"));
				hotKeyButton = HotKeyButton.ofKeyAndMouse(screenSupplier, hotKey.getHotKey());
				enabledCheckbox = draggable(
				  ENABLE_ACTION, CheckboxButton.of(hotKey.isEnabled(), StringTextComponent.EMPTY));
				Stream.of(hotKeyButton, textField, enabledCheckbox).forEach(listeners::add);
				for (String id: SimpleConfig.getConfigModIds())
					entries.put(id, new ConfigHotKeyTreeViewModEntry(hotKey, id));
			}
			
			public ConfigHotKey buildHotKey() {
				hotKey.setKeyCode(hotKeyButton.getKey());
				hotKey.setName(textField.getText());
				hotKey.setEnabled(enabledCheckbox.getWidget().getValue());
				return this.hotKey;
			}
			
			@Override
			public boolean canBeDroppedInto(int index, List<ConfigHotKeyTreeViewEntry> selection) {
				return false;
			}
			
			@Override public boolean canBeAddedToSelection(Set<ConfigHotKeyTreeViewEntry> selection) {
				return selection.stream().allMatch(
				  e -> e instanceof ConfigHotKeyTreeViewGroupEntry
				       || e instanceof ConfigHotKeyTreeViewHotKeyEntry);
			}
			
			@Override protected void tick() {
				List<ConfigHotKeyTreeViewEntry> subEntries = getSubEntries();
				subEntries.clear();
				entries.keySet().stream()
				  .sorted(modOrder)
				  .map(entries::get).forEach(subEntries::add);
			}
			
			@Override public void render(
			  MatrixStack mStack, int x, int y, int width, int mouseX, int mouseY, float delta
			) {
				if (getParent().getFocusedSubEntry() != this) setExpanded(false);
				super.render(mStack, x, y, width, mouseX, mouseY, delta);
			}
			
			@Override public void renderContent(
			  MatrixStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
			) {
				pos(hotKeyButton, x, y);
				int hotKeyButtonWidth = min(150, (w - 46) / 2);
				hotKeyButton.setExactWidth(hotKeyButtonWidth);
				pos(textField, x + hotKeyButtonWidth + 4, y + 6, w - 48 - hotKeyButtonWidth, h - 4);
				enabledCheckbox.setPosition(x + w - 22, y + 1);
				
				renderAll(mStack, mouseX, mouseY, delta,
				          hotKeyButton, textField, enabledCheckbox);
			}
		}
		
		public static class ConfigHotKeyTreeViewModEntry extends ConfigHotKeyTreeViewEntry {
			private final ConfigHotKey hotKey;
			private final String modId;
			private final SimpleConfig client;
			private final SimpleConfig server;
			private final MultiFunctionImageButton editButton;
			
			public ConfigHotKeyTreeViewModEntry(ConfigHotKey hotKey, String modId) {
				this.hotKey = hotKey;
				this.modId = modId;
				client = SimpleConfig.hasConfig(modId, Type.CLIENT)? SimpleConfig.getConfig(modId, Type.CLIENT) : null;
				server = SimpleConfig.hasConfig(modId, Type.SERVER)? SimpleConfig.getConfig(modId, Type.SERVER) : null;
				editButton = new MultiFunctionImageButton(
				  0, 0, 20, 20, SimpleConfigIcons.Buttons.GEAR,
				  ButtonAction.of(this::editHotKey));
				listeners.add(editButton);
			}
			
			public void editHotKey() {
				getTree().getHotKeyEditor().accept(modId, hotKey);
			}
			
			public String getModId() {
				return modId;
			}
			
			@Override public boolean isSelectable() {
				return false;
			}
			
			@Override public boolean canBeDroppedInto(int index, List<ConfigHotKeyTreeViewEntry> selection) {
				return false;
			}
			
			@Override public void renderContent(
			  MatrixStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
			) {
				pos(editButton, x + 2, y + 1, 18, 18);
				editButton.render(mStack, mouseX, mouseY, delta);
				
				int count = getCount();
				FontRenderer font = Minecraft.getInstance().fontRenderer;
				
				int textX = x + 24;
				int textY = y + h / 2 - font.FONT_HEIGHT / 2;
				drawString(
				  mStack, font, getDisplayText(),
				  textX, textY, count == 0? 0xE0A0A0A0 : 0xE0E0E0E0);
				ConfigHotKeyTreeView tree = getTree();
				if (textX <= mouseX && mouseX < tree.getArea().getMaxX()
				    && textY <= mouseY && mouseY < textY + font.FONT_HEIGHT) {
					List<ITextComponent> tooltip = getResumeTooltip();
					if (!tooltip.isEmpty()) tree.getDialogScreen().addTooltip(Tooltip.of(
					  Point.of(mouseX, mouseY), tooltip));
				}
			}
			
			protected IFormattableTextComponent getDisplayText() {
				int count = getCount();
				TextFormatting style = count > 0? TextFormatting.WHITE : TextFormatting.GRAY;
				TextFormatting dim = count > 0? TextFormatting.GRAY : TextFormatting.DARK_GRAY;
				IFormattableTextComponent name = ModList.get().getMods().stream()
				  .filter(m -> modId.equals(m.getModId()))
				  .findFirst().map(m -> new StringTextComponent(
					 m.getDisplayName()).mergeStyle(style).appendString(" ")
					 .append(new StringTextComponent(
						"(" + modId + ")"
					 ).mergeStyle(dim))
				  ).orElse(new StringTextComponent(modId).mergeStyle(style));
				if (count > 0) name.append(
				  new StringTextComponent(" [")
					 .append(new StringTextComponent(String.valueOf(count))
					           .mergeStyle(TextFormatting.AQUA))
					 .appendString("]").mergeStyle(TextFormatting.DARK_AQUA));
				return name;
			}
			
			protected List<ITextComponent> getResumeTooltip() {
				Map<SimpleConfig, Map<String, HotKeyAction<?>>> actions = hotKey.getActions();
				List<ITextComponent> tt = new ArrayList<>();
				if (client != null) {
					Map<String, HotKeyAction<?>> clientActions = actions.get(client);
					if (clientActions != null && !clientActions.isEmpty()) {
						tt.add(new TranslationTextComponent("simpleconfig.config.category.client").mergeStyle(TextFormatting.BOLD));
						clientActions.forEach((k, v) -> tt.add(formatAction(k, v)));
					}
				}
				if (server != null) {
					Map<String, HotKeyAction<?>> serverActions = actions.get(server);
					if (serverActions != null && !serverActions.isEmpty()) {
						tt.add(new TranslationTextComponent("simpleconfig.config.category.server").mergeStyle(TextFormatting.BOLD));
						serverActions.forEach((k, v) -> tt.add(formatAction(k, v)));
					}
				}
				return tt;
			}
			
			protected <T> IFormattableTextComponent formatAction(String key, HotKeyAction<T> action) {
				return new StringTextComponent("[" + key + "]: ")
				  .mergeStyle(TextFormatting.LIGHT_PURPLE)
				  .append(formatAction(action.getType(), action).deepCopy().mergeStyle(TextFormatting.GRAY));
			}
			
			@SuppressWarnings("unchecked") private <V, A extends HotKeyAction<V>, T extends HotKeyActionType<V, A>>
			ITextComponent formatAction(T type, HotKeyAction<?> action) {
				return type.formatAction((A) action);
			}
			
			protected int getCount() {
				int size = 0;
				Map<SimpleConfig, Map<String, HotKeyAction<?>>> actions = hotKey.getActions();
				if (client != null) {
					Map<String, HotKeyAction<?>> configActions = actions.get(client);
					if (configActions != null) size += configActions.size();
				}
				if (server != null) {
					Map<String, HotKeyAction<?>> configActions = actions.get(server);
					if (configActions != null) size += configActions.size();
				}
				return size;
			}
		}
		
		public static class ConfigHotKeyTreeViewGroupEntry extends ConfigHotKeyTreeViewEntry {
			private final TextFieldWidgetEx textField;
			private final HotKeyButton hotKeyButton;
			private final DragBroadcastableWidget<CheckboxButton> enabledCheckbox;
			
			public ConfigHotKeyTreeViewGroupEntry(
			  Supplier<IModalInputCapableScreen> screenSupplier, ConfigHotKeyGroup group
			) {
				subEntries = group.getEntries().stream().map(e -> {
					if (e instanceof ConfigHotKeyGroup) {
						return new ConfigHotKeyTreeViewGroupEntry(screenSupplier, (ConfigHotKeyGroup) e);
					} else if (e instanceof ConfigHotKey) {
						return new ConfigHotKeyTreeViewHotKeyEntry(screenSupplier, (ConfigHotKey) e);
					} else return null;
				}).filter(Objects::nonNull).collect(Collectors.toList());
				textField = TextFieldWidgetEx.of(group.getName());
				textField.setMaxLength(256);
				textField.setBordered(false);
				textField.setEmptyHint(new TranslationTextComponent(
				  "simpleconfig.ui.hotkey.unnamed.hint"));
				hotKeyButton = HotKeyButton.ofKeyAndMouse(screenSupplier, group.getHotKey());
				enabledCheckbox = draggable(
				  ENABLE_ACTION, CheckboxButton.of(group.isEnabled(), StringTextComponent.EMPTY));
				Stream.of(textField, hotKeyButton, enabledCheckbox).forEach(listeners::add);
				setExpanded(true, false);
			}
			
			public ConfigHotKeyGroup buildGroup() {
				ConfigHotKeyGroup group = new ConfigHotKeyGroup();
				subEntries.stream().map(e -> {
					if (e instanceof ConfigHotKeyTreeViewGroupEntry) {
						return ((ConfigHotKeyTreeViewGroupEntry) e).buildGroup();
					} else if (e instanceof ConfigHotKeyTreeViewHotKeyEntry) {
						return ((ConfigHotKeyTreeViewHotKeyEntry) e).buildHotKey();
					} else return null;
				}).filter(Objects::nonNull).forEach(group.getEntries()::add);
				group.setName(textField.getText());
				group.setKeyCode(hotKeyButton.getKey());
				group.setEnabled(enabledCheckbox.getWidget().getValue());
				return group;
			}
			
			@Override
			public boolean canBeDroppedInto(int index, List<ConfigHotKeyTreeViewEntry> selection) {
				return true;
			}
			
			@Override public boolean canBeAddedToSelection(Set<ConfigHotKeyTreeViewEntry> selection) {
				return selection.stream().allMatch(
				  e -> e instanceof ConfigHotKeyTreeViewGroupEntry
				       || e instanceof ConfigHotKeyTreeViewHotKeyEntry);
			}
			
			@Override public void renderContent(
			  MatrixStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
			) {
				int hotKeyButtonWidth = min(140, (int) (w * 0.4));
				pos(textField, x + 4, y + 6, w - 30 - hotKeyButtonWidth, h);
				pos(hotKeyButton, x + w - 24 - hotKeyButtonWidth, y, hotKeyButtonWidth);
				hotKeyButton.setExactWidth(hotKeyButtonWidth);
				enabledCheckbox.setPosition(x + w - 22, y + 1);
				
				renderAll(mStack, mouseX, mouseY, delta,
				          textField, hotKeyButton, enabledCheckbox);
			}
			
			@Override public boolean isForceRenderAsGroup() {
				return true;
			}
		}
	}
}
