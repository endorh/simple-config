package endorh.simpleconfig.ui.hotkey;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import endorh.simpleconfig.api.SimpleConfig;
import endorh.simpleconfig.api.SimpleConfig.EditType;
import endorh.simpleconfig.api.ui.hotkey.ExtendedKeyBind;
import endorh.simpleconfig.api.ui.hotkey.ExtendedKeyBindProvider;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons;
import endorh.simpleconfig.api.ui.math.Point;
import endorh.simpleconfig.core.SimpleConfigImpl;
import endorh.simpleconfig.ui.api.IDialogCapableScreen;
import endorh.simpleconfig.ui.api.IOverlayCapableContainer;
import endorh.simpleconfig.ui.api.Tooltip;
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
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraftforge.fml.ModList;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static endorh.simpleconfig.api.SimpleConfigTextUtil.splitTtc;
import static endorh.simpleconfig.ui.gui.WidgetUtils.renderAll;
import static java.lang.Math.min;

public class ConfigHotKeyTreeView extends ArrangeableTreeView<ConfigHotKeyTreeViewEntry> {
	private final Supplier<IDialogCapableScreen> dialogScreenSupplier;
	private final Supplier<IOverlayCapableContainer> containerSupplier;
	private @Nullable String contextModId = null;
	private final BiConsumer<String, ConfigHotKey> hotKeyEditor;
	public static final WidgetDragBroadcastableAction<CheckboxButton> ENABLE_ACTION = (b, s) -> b.setToggle(s.getValue());
	public final CandidateHotKeyProvider provider = new CandidateHotKeyProvider();
	
	public ConfigHotKeyTreeView(
	  Supplier<IDialogCapableScreen> screenSupplier, IOverlayCapableContainer overlayContainer,
	  ConfigHotKeyGroup group, BiConsumer<String, ConfigHotKey> hotKeyEditor
	) {
		super(overlayContainer, new ConfigHotKeyTreeViewGroupEntry(
		  screenSupplier, () -> overlayContainer, group));
		dialogScreenSupplier = screenSupplier;
		containerSupplier = () -> overlayContainer;
		this.hotKeyEditor = hotKeyEditor;
		setCaption(new ConfigHotKeyTreeViewCaption(screenSupplier, overlayContainer, this));
		Throwable lastError = ConfigHotKeyManager.INSTANCE.getLastLoadingError();
		setPlaceHolder(
		  lastError == null
		  ? Component.translatable("simpleconfig.ui.no_hotkeys")
		  : Component.translatable("simpleconfig.ui.error_loading_hotkeys", lastError.getLocalizedMessage())
		    .withStyle(ChatFormatting.RED));
		ExtendedKeyBindDispatcher.registerProvider(provider);
	}
	
	@SuppressWarnings("unchecked") public <T extends Screen & IDialogCapableScreen> T getDialogScreen() {
		return (T) dialogScreenSupplier.get();
	}
	
	public IOverlayCapableContainer getOverlayContainer() {
		return containerSupplier.get();
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
		ConfigHotKey hotKey = new ConfigHotKey();
		provider.getHotKeys().add(hotKey.getKeyBind());
		tryAddEntry(new ConfigHotKeyTreeViewHotKeyEntry(
		  this::getDialogScreen, containerSupplier, hotKey));
	}
	
	public void addGroup() {
		ConfigHotKeyGroup group = new ConfigHotKeyGroup();
		provider.getHotKeys().add(group.getKeyBind());
		tryAddEntry(new ConfigHotKeyTreeViewGroupEntry(
		  this::getDialogScreen, containerSupplier, group));
	}
	
	public void removeCandidates() {
		provider.getHotKeys().clear();
		ExtendedKeyBindDispatcher.unregisterProvider(provider);
		getRoot().removeCandidate();
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
				 ).tooltip(ImmutableList.of(
					Component.translatable("simpleconfig.ui.hotkey.dialog.add.hotkey"),
					Component.translatable("simpleconfig.ui.hotkey.dialog.add.hotkey:key")))
				 .tint(0x6480FF80)
			));
			addControl(MultiFunctionIconButton.of(
			  SimpleConfigIcons.Widgets.TREE_ADD_GROUP, 20, 20, ButtonAction.of(
				  tree::addGroup
				 ).tooltip(ImmutableList.of(
					Component.translatable("simpleconfig.ui.hotkey.dialog.add.group"),
					Component.translatable("simpleconfig.ui.hotkey.dialog.add.group:key")))
				 .tint(0x6480FF80)
			));
			addControl(MultiFunctionIconButton.of(
			  SimpleConfigIcons.Widgets.TREE_REMOVE, 20, 20, ButtonAction.of(
					tree::removeSelection
				 ).tooltip(ImmutableList.of(
					Component.translatable("simpleconfig.ui.hotkey.dialog.remove"),
					Component.translatable("simpleconfig.ui.hotkey.dialog.remove:key")))
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
		
		@Override public void render(PoseStack mStack, int mouseX, int mouseY, float delta) {
			int width = getTree().getWidth();
			savedHotKeyGroupPickerWidget.setWidth(Mth.clamp((int) (width * 0.45), 90, 300));
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
		
		public void removeCandidate() {}
		
		public static class ConfigHotKeyTreeViewHotKeyEntry extends ConfigHotKeyTreeViewEntry {
			private final ConfigHotKey hotKey;
			private final TextFieldWidgetEx textField;
			private final KeyBindButton hotKeyButton;
			private final DragBroadcastableWidget<CheckboxButton> enabledCheckbox;
			private final Map<String, ConfigHotKeyTreeViewModEntry> entries = new HashMap<>();
			private final ExtendedKeyBindImpl keyBind;
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
			  Supplier<IDialogCapableScreen> screenSupplier,
			  Supplier<IOverlayCapableContainer> containerSupplier,
			  ConfigHotKey hotKey
			) {
				keyBind = hotKey.getKeyBind();
				hotKey = hotKey.copy();
				this.hotKey = hotKey;
				textField = TextFieldWidgetEx.of(hotKey.getName());
				textField.setMaxLength(256);
				textField.setBordered(false);
				textField.setEmptyHint(Component.translatable("simpleconfig.ui.hotkey.unnamed.hint"));
				hotKeyButton = KeyBindButton.of(
				  screenSupplier::get, containerSupplier, keyBind);
				enabledCheckbox = draggable(
				  ENABLE_ACTION, CheckboxButton.of(hotKey.isEnabled(), Component.empty()));
				Stream.of(hotKeyButton, textField, enabledCheckbox).forEach(listeners::add);
				for (String id: SimpleConfigImpl.getConfigModIds())
					entries.put(id, new ConfigHotKeyTreeViewModEntry(hotKey, id));
			}
			
			public ConfigHotKey buildHotKey() {
				hotKey.setKeyMapping(hotKeyButton.getMapping());
				hotKey.setName(textField.getValue());
				hotKey.setEnabled(enabledCheckbox.getWidget().getValue());
				return hotKey;
			}
			
			@Override public void removeCandidate() {
				keyBind.setCandidateName(null);
				keyBind.setCandidateDefinition(null);
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

			@Override
			public @Nullable ComponentPath nextFocusPath(@NotNull FocusNavigationEvent e) {
				return super.nextFocusPath(e);
			}

			@Override protected void tick() {
				List<ConfigHotKeyTreeViewEntry> subEntries = getSubEntries();
				subEntries.clear();
				entries.keySet().stream()
				  .sorted(modOrder)
				  .map(entries::get).forEach(subEntries::add);
				keyBind.setCandidateName(Component.literal(textField.getValue()));
				hotKeyButton.tick();
			}
			
			@Override public void render(
			  PoseStack mStack, int x, int y, int width, int mouseX, int mouseY, float delta
			) {
				if (getParent().getFocusedSubEntry() != this) setExpanded(false);
				super.render(mStack, x, y, width, mouseX, mouseY, delta);
			}
			
			@Override public void renderContent(
			  PoseStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
			) {
				int hotKeyButtonWidth = min(150, (w - 46) / 2);
				hotKeyButton.setPosition(x, y, hotKeyButtonWidth);
				textField.setPosition(x + hotKeyButtonWidth + 4, y + 6);
				((AbstractWidget) textField).setWidth(w - 48 - hotKeyButtonWidth);
				textField.setHeight(h - 10);
				enabledCheckbox.setPosition(x + w - 22, y + 1);
				
				renderAll(mStack, mouseX, mouseY, delta,
				          hotKeyButton, textField, enabledCheckbox);
			}
		}
		
		public static class ConfigHotKeyTreeViewModEntry extends ConfigHotKeyTreeViewEntry {
			private final ConfigHotKey hotKey;
			private final String modId;
			private final MultiFunctionImageButton editButton;
			
			public ConfigHotKeyTreeViewModEntry(ConfigHotKey hotKey, String modId) {
				this.hotKey = hotKey;
				this.modId = modId;
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
			  PoseStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
			) {
				editButton.setPosition(x + 2, y + 1);
				editButton.setWidth(18);
				editButton.setHeight(18);
				editButton.render(mStack, mouseX, mouseY, delta);
				
				int count = getCount();
				Font font = Minecraft.getInstance().font;
				
				int textX = x + 24;
				int textY = y + h / 2 - font.lineHeight / 2;
				drawString(
				  mStack, font, getDisplayText(),
				  textX, textY, count == 0? 0xE0A0A0A0 : 0xE0E0E0E0);
				ConfigHotKeyTreeView tree = getTree();
				if (textX <= mouseX && mouseX < tree.getArea().getMaxX()
				    && textY <= mouseY && mouseY < textY + font.lineHeight) {
					List<Component> tooltip = getResumeTooltip();
					if (!tooltip.isEmpty()) tree.getDialogScreen().addTooltip(Tooltip.of(
					  Point.of(mouseX, mouseY), tooltip));
				}
			}
			
			protected MutableComponent getDisplayText() {
				int count = getCount();
				ChatFormatting style = count > 0? ChatFormatting.WHITE : ChatFormatting.GRAY;
				ChatFormatting dim = count > 0? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY;
				MutableComponent name = ModList.get().getMods().stream()
				  .filter(m -> modId.equals(m.getModId()))
				  .findFirst().map(m -> Component.literal(m.getDisplayName()).withStyle(style).append(" ")
					 .append(Component.literal("(" + modId + ")").withStyle(dim))
				  ).orElse(Component.literal(modId).withStyle(style));
				if (count > 0) name.append(
				  Component.literal(" [")
					 .append(Component.literal(String.valueOf(count))
					           .withStyle(ChatFormatting.AQUA))
					 .append("]").withStyle(ChatFormatting.DARK_AQUA));
				return name;
			}
			
			protected List<Component> getResumeTooltip() {
				Map<Pair<String, EditType>, Map<String, HotKeyAction<?>>> actions = hotKey.getActions();
				List<Component> tt = new ArrayList<>();
				for (EditType type: SimpleConfig.EditType.values()) {
					SimpleConfigImpl config = SimpleConfigImpl.getConfigOrNull(modId, type.getType());
					if (config != null) {
						Map<String, HotKeyAction<?>> a = actions.get(Pair.of(modId, type));
						if (a != null && !a.isEmpty()) {
							tt.add(Component.translatable("simpleconfig.config.category." + type.getAlias()).withStyle(ChatFormatting.BOLD));
							a.forEach((k, v) -> tt.add(formatAction(k, v)));
						}
					}
				}
				return tt;
			}
			
			protected <T> MutableComponent formatAction(String key, HotKeyAction<T> action) {
				return Component.literal("[" + key + "]: ")
				  .withStyle(ChatFormatting.LIGHT_PURPLE)
				  .append(formatAction(action.getType(), action).copy().withStyle(ChatFormatting.GRAY));
			}
			
			@SuppressWarnings("unchecked") private <V, A extends HotKeyAction<V>, T extends HotKeyActionType<V, A>>
			Component formatAction(T type, HotKeyAction<?> action) {
				return type.formatAction((A) action);
			}
			
			protected int getCount() {
				int size = 0;
				Map<Pair<String, EditType>, Map<String, HotKeyAction<?>>> actions = hotKey.getActions();
				for (EditType type: SimpleConfig.EditType.values()) {
					SimpleConfigImpl config = SimpleConfigImpl.getConfigOrNull(modId, type.getType());
					if (config != null) {
						Map<String, HotKeyAction<?>> a = actions.get(Pair.of(modId, type));
						if (a != null) size += a.size();
					}
				}
				return size;
			}
		}
		
		public static class ConfigHotKeyTreeViewGroupEntry extends ConfigHotKeyTreeViewEntry {
			private final TextFieldWidgetEx textField;
			private final KeyBindButton hotKeyButton;
			private final DragBroadcastableWidget<CheckboxButton> enabledCheckbox;
			private final ExtendedKeyBindImpl keyBind;
			
			public ConfigHotKeyTreeViewGroupEntry(
			  Supplier<IDialogCapableScreen> screenSupplier,
			  Supplier<IOverlayCapableContainer> containerSupplier,
			  ConfigHotKeyGroup group
			) {
				subEntries = group.getEntries().stream().map(e -> {
					if (e instanceof ConfigHotKeyGroup) {
						return new ConfigHotKeyTreeViewGroupEntry(
						  screenSupplier, containerSupplier, (ConfigHotKeyGroup) e);
					} else if (e instanceof ConfigHotKey) {
						return new ConfigHotKeyTreeViewHotKeyEntry(
						  screenSupplier, containerSupplier, (ConfigHotKey) e);
					} else return null;
				}).filter(Objects::nonNull).collect(Collectors.toList());
				textField = TextFieldWidgetEx.of(group.getName());
				textField.setMaxLength(256);
				textField.setBordered(false);
				textField.setEmptyHint(Component.translatable("simpleconfig.ui.hotkey.unnamed.hint"));
				keyBind = group.getKeyBind();
				hotKeyButton = KeyBindButton.of(screenSupplier::get, containerSupplier, keyBind);
				hotKeyButton.setTooltip(splitTtc("simpleconfig.ui.hotkey.group.hotkey"));
				enabledCheckbox = draggable(
				  ENABLE_ACTION, CheckboxButton.of(group.isEnabled(), Component.empty()));
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
				group.setName(textField.getValue());
				group.setKeyMapping(hotKeyButton.getMapping());
				group.setEnabled(enabledCheckbox.getWidget().getValue());
				return group;
			}
			
			@Override protected void tick() {
				keyBind.setCandidateName(Component.literal(textField.getValue()));
				hotKeyButton.tick();
			}
			
			@Override public void removeCandidate() {
				keyBind.setCandidateName(null);
				keyBind.setCandidateDefinition(null);
				subEntries.forEach(ConfigHotKeyTreeViewEntry::removeCandidate);
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
			  PoseStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
			) {
				int hotKeyButtonWidth = min(140, (int) (w * 0.4));
				textField.setPosition(x + 4, y + 6);
				((AbstractWidget) textField).setWidth(w - 30 - hotKeyButtonWidth);
				textField.setHeight(h - 10);
				hotKeyButton.setPosition(x + w - 24 - hotKeyButtonWidth, y, hotKeyButtonWidth);
				enabledCheckbox.setPosition(x + w - 22, y + 1);
				
				renderAll(mStack, mouseX, mouseY, delta,
				          textField, hotKeyButton, enabledCheckbox);
			}
			
			@Override public boolean isForceRenderAsGroup() {
				return true;
			}
		}
	}
	
	private static class CandidateHotKeyProvider implements ExtendedKeyBindProvider {
		private final Set<ExtendedKeyBind> hotKeys = new HashSet<>();
		@Override public @NotNull Iterable<ExtendedKeyBind> getActiveKeyBinds() {
			return Collections.emptyList();
		}
		@Override public @NotNull Iterable<ExtendedKeyBind> getAllKeyBinds() {
			return hotKeys;
		}
		
		@Override public int getPriority() {
			return -999;
		}
		
		public Set<ExtendedKeyBind> getHotKeys() {
			return hotKeys;
		}
	}
}
