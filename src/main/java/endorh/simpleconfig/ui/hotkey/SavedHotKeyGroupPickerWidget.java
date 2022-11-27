package endorh.simpleconfig.ui.hotkey;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.api.ui.icon.Icon;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons.Buttons;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons.Hotkeys;
import endorh.simpleconfig.api.ui.math.Rectangle;
import endorh.simpleconfig.config.ServerConfig;
import endorh.simpleconfig.config.ServerConfig.permissions;
import endorh.simpleconfig.core.SimpleConfigNetworkHandler;
import endorh.simpleconfig.core.SimpleConfigPaths;
import endorh.simpleconfig.ui.api.IDialogCapableScreen;
import endorh.simpleconfig.ui.api.IOverlayCapableContainer;
import endorh.simpleconfig.ui.gui.ConfirmDialog;
import endorh.simpleconfig.ui.gui.InfoDialog;
import endorh.simpleconfig.ui.gui.ProgressDialog;
import endorh.simpleconfig.ui.gui.WidgetUtils;
import endorh.simpleconfig.ui.gui.widget.CheckboxButton;
import endorh.simpleconfig.ui.gui.widget.IPositionableRenderable.IRectanglePositionableRenderable;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.Modifier;
import endorh.simpleconfig.ui.gui.widget.TintedButton;
import endorh.simpleconfig.ui.gui.widget.combobox.ComboBoxWidget;
import endorh.simpleconfig.ui.gui.widget.combobox.SimpleComboBoxModel;
import endorh.simpleconfig.ui.gui.widget.combobox.wrapper.TypeWrapper;
import endorh.simpleconfig.ui.hotkey.ConfigHotKeyManager.ConfigHotKeyGroup;
import endorh.simpleconfig.ui.hotkey.ConfigHotKeyTreeView.ConfigHotKeyTreeViewEntry;
import endorh.simpleconfig.ui.hotkey.ConfigHotKeyTreeView.ConfigHotKeyTreeViewEntry.ConfigHotKeyTreeViewGroupEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.FocusableGui;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static endorh.simpleconfig.api.SimpleConfigTextUtil.paragraph;
import static endorh.simpleconfig.api.SimpleConfigTextUtil.splitTtc;
import static endorh.simpleconfig.core.SimpleConfigPaths.LOCAL_HOTKEYS_DIR;
import static endorh.simpleconfig.ui.gui.WidgetUtils.pos;
import static java.util.concurrent.CompletableFuture.completedFuture;

public class SavedHotKeyGroupPickerWidget extends FocusableGui implements IRectanglePositionableRenderable {
	public static boolean skipOverwriteDialog = false;
	protected final List<IGuiEventListener> listeners = new ArrayList<>();
	protected final Rectangle area = new Rectangle(0, 0, 100, 20);
	private final Supplier<IDialogCapableScreen> screen;
	protected final IOverlayCapableContainer overlayContainer;
	protected final ConfigHotKeyTreeView tree;
	protected MultiFunctionImageButton loadButton;
	protected MultiFunctionImageButton saveButton;
	protected ComboBoxWidget<SavedHotKeyGroup> selector;
	protected long UPDATE_INTERVAL = 2000L;
	protected long lastUpdate = 0L;
	protected CompletableFuture<List<RemoteSavedHotKeyGroup>> lastFuture;
	
	protected List<LocalSavedHotKeyGroup> localGroups = Lists.newArrayList();
	protected List<RemoteSavedHotKeyGroup> remoteGroups = Lists.newArrayList();
	protected List<ResourceSavedHotKeyGroup> resourceGroups = Lists.newArrayList();
	
	public SavedHotKeyGroupPickerWidget(
	  Supplier<IDialogCapableScreen> screen, IOverlayCapableContainer overlayContainer,
	  ConfigHotKeyTreeView tree
	) {
		this.screen = screen;
		this.overlayContainer = overlayContainer;
		this.tree = tree;
		loadButton = new MultiFunctionImageButton(0, 0, 20, 20, Buttons.LOAD, ButtonAction.of(
		  this::load
		).active(() -> selector.getValue() != null)
		  .tooltip(new TranslationTextComponent("simpleconfig.ui.saved_hotkeys.load_group")));
		saveButton = new MultiFunctionImageButton(0, 0, 20, 20, Buttons.SAVE, ButtonAction.of(
			 () -> saveLocal(false)
		).active(() -> !selector.getText().isEmpty() && tree.getFocusedEntry() instanceof ConfigHotKeyTreeViewGroupEntry)
		  .tooltip(new TranslationTextComponent("simpleconfig.ui.saved_hotkeys.save_group.local")))
		  .on(Modifier.SHIFT, ButtonAction.of(() -> saveRemote(false))
		    .icon(Buttons.SAVE_REMOTE)
		    .active(
				() -> !selector.getText().isEmpty()
				      && tree.getFocusedEntry() instanceof ConfigHotKeyTreeViewGroupEntry
				      && permissions.canEditServerHotKeys()
		    ).tooltip(new TranslationTextComponent("simpleconfig.ui.saved_hotkeys.save_group.remote")))
		  .on(Modifier.ALT, ButtonAction.of(() -> delete(true))
		    .icon(Buttons.DELETE)
		    .active(() -> {
			    SavedHotKeyGroup value = selector.getValue();
				 return value instanceof WritableSavedHotKeyGroup
				        && ((WritableSavedHotKeyGroup) value).canWrite();
			 })
		    .tooltip(new TranslationTextComponent("simpleconfig.ui.saved_hotkeys.delete_group")));
		selector = new ComboBoxWidget<>(
		  new SavedHotKeyGroupWrapper(this), () -> overlayContainer, 0, 0, 80, 18);
		selector.setSuggestionProvider(new SimpleComboBoxModel<>(this::getSavedGroups));
		selector.setHint(new TranslationTextComponent("simpleconfig.ui.saved_hotkeys.hint"));
		Stream.of(selector, loadButton, saveButton).forEach(listeners::add);
	}
	
	public IDialogCapableScreen getScreen() {
		IDialogCapableScreen screen = this.screen.get();
		if (screen == null) throw new IllegalStateException("Cannot get screen so early!");
		return screen;
	}
	
	@Override public boolean isFocused() {
		return selector.isFocused() || loadButton.isFocused() || saveButton.isFocused();
	}
	
	@Override public void setFocused(boolean focused) {
		if (focused == isFocused()) return;
		if (focused) {
			setListener(selector);
			selector.setFocused(true);
		} else WidgetUtils.forceUnFocus(this);
	}
	
	@Override public Rectangle getArea() {
		return area;
	}
	
	protected @Nullable ConfigHotKeyGroup getFocusedGroup() {
		ConfigHotKeyTreeViewEntry entry = tree.getFocusedEntry();
		while (entry != null && !(entry instanceof ConfigHotKeyTreeViewGroupEntry))
			entry = entry.getParent();
		if (entry == null) return null;
		return ((ConfigHotKeyTreeViewGroupEntry) entry).buildGroup();
	}
	
	public boolean existsGroup(String name, List<? extends SavedHotKeyGroup> groups) {
		return groups.stream().anyMatch(g -> name.equals(g.getName()));
	}
	
	public void load() {
		SavedHotKeyGroup group = selector.getValue();
		if (group != null) {
			String rr = group instanceof ResourceSavedHotKeyGroup? "resource" :
			            group instanceof RemoteSavedHotKeyGroup? "remote" : "local";
			IFormattableTextComponent displayName = new StringTextComponent(group.getName())
			  .mergeStyle(TextFormatting.LIGHT_PURPLE);
			getScreen().addDialog(ProgressDialog.create(
			  new TranslationTextComponent("simpleconfig.ui.saved_hotkeys.loading"),
			  group.load().thenAccept(g -> {
				  if (g != null) tree.tryAddEntry(
				    new ConfigHotKeyTreeViewGroupEntry(
						tree::getDialogScreen, tree::getOverlayContainer, g));
			  }), d -> {
				  d.setBody(paragraph(
					 "simpleconfig.ui.saved_hotkeys.loading." + rr, displayName
				  ));
				  d.setSuccessDialog(InfoDialog.create(
					 new TranslationTextComponent("simpleconfig.ui.saved_hotkeys.loaded"),
					 paragraph(
						 "simpleconfig.ui.saved_hotkeys.loaded." + rr, displayName)
				  ));
			  }
			));
		}
	}
	
	protected boolean showConfirmDialog(
	  String intent, String overwrite, ITextComponent name,
	  Runnable action, @Nullable Runnable alternative
	) {
		boolean mistaken = !intent.equals(overwrite);
		if (!mistaken && skipOverwriteDialog) return false;
		String problem = mistaken? "mistaken" : "overwrite";
		List<ITextComponent> body = splitTtc(
		  "simpleconfig.ui.saved_hotkeys." + problem + "." + overwrite, name);
		if (mistaken) {
			body.addAll(splitTtc("simpleconfig.ui.saved_hotkeys.mistaken.question." + intent));
		} else body.addAll(splitTtc("simpleconfig.ui.saved_hotkeys.overwrite.question"));
		getScreen().addDialog(ConfirmDialog.create(
		  new TranslationTextComponent(
		    "simpleconfig.ui.saved_hotkeys." + problem),
		  d -> {
			  d.setBody(body);
			  d.setConfirmText(new TranslationTextComponent(
				 mistaken? "simpleconfig.ui.saved_hotkeys.mistaken.option.create." + intent :
				 "simpleconfig.ui.confirm_overwrite.overwrite"));
			  d.setConfirmButtonTint("remote".equals(intent)? 0x6480A0FF : 0x6480FFA0);
			  if (mistaken) {
				  d.withAction(b -> {
					  if (b) action.run();
				  });
				  if (alternative != null && !"resource".equals(overwrite)) d.addButton(1, TintedButton.of(
					 new TranslationTextComponent("simpleconfig.ui.confirm_overwrite.overwrite"),
					 "remote".equals(overwrite)? 0x6480A0FF : 0x6480FFA0, p -> {
						 d.cancel(false);
						 alternative.run();
					 }));
			  } else d.withCheckBoxes((b, c) -> {
				  if (b) {
					  skipOverwriteDialog = c[0];
					  action.run();
				  }
			  }, CheckboxButton.of(false, new TranslationTextComponent(
			    "simpleconfig.ui.do_not_ask_again_this_session"
			  )));
		  }
		));
		return true;
	}
	
	public void saveLocal(boolean overwrite) {
		ConfigHotKeyGroup g = getFocusedGroup();
		if (g == null) return;
		LocalSavedHotKeyGroup group = SavedHotKeyGroup.local(selector.getText());
		String name = group.getName();
		IFormattableTextComponent displayName = new StringTextComponent(name)
		  .mergeStyle(TextFormatting.LIGHT_PURPLE);
		if (!overwrite) {
			String oo = null;
			if (existsGroup(name, localGroups)) oo = "local";
			else if (existsGroup(name, remoteGroups)) oo = "remote";
			else if (existsGroup(name, resourceGroups)) oo = "resource";
			if (oo != null && showConfirmDialog("local", oo, displayName, () -> saveLocal(true), () -> saveRemote(true)))
				return;
		}
		File file = group.getFile();
		getScreen().addDialog(
		  ProgressDialog.create(
			 new TranslationTextComponent("simpleconfig.ui.saved_hotkeys.saving"),
		    group.save(g), d -> {
				 d.setCancellableByUser(false);
			    d.setBody(paragraph(
				   "simpleconfig.ui.saved_hotkeys.saving.local", displayName
				 ));
				 d.setSuccessDialog(InfoDialog.create(
					new TranslationTextComponent("simpleconfig.ui.saved_hotkeys.saved"),
					paragraph(
					  "simpleconfig.ui.saved_hotkeys.saved.local", displayName,
					  "simpleconfig.ui.saved_hotkeys.saved.file",
					  new StringTextComponent(
					    SimpleConfigPaths.relativize(file.toPath()).toString()
					  ).modifyStyle(s -> s.setUnderlined(true).applyFormatting(TextFormatting.BLUE)
					    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TranslationTextComponent("chat.copy")))
					    .setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, file.getAbsolutePath()))))
				 ));
		    }));
	}
	
	public void saveRemote(boolean overwrite) {
		ConfigHotKeyGroup g = getFocusedGroup();
		if (g == null) return;
		RemoteSavedHotKeyGroup group = SavedHotKeyGroup.remote(selector.getText());
		String name = group.getName();
		IFormattableTextComponent displayName = new StringTextComponent(name)
		  .mergeStyle(TextFormatting.LIGHT_PURPLE);
		if (!overwrite) {
			String oo = null;
			if (existsGroup(name, remoteGroups)) oo = "remote";
			else if (existsGroup(name, localGroups)) oo = "local";
			else if (existsGroup(name, resourceGroups)) oo = "resource";
			if (oo != null && showConfirmDialog("remote", oo, displayName, () -> saveRemote(true), () -> saveLocal(true)))
				return;
		}
		if (group.canWrite()) {
			getScreen().addDialog(
			  ProgressDialog.create(
				 new TranslationTextComponent("simpleconfig.ui.saved_hotkeys.saving"),
				 group.save(g), d -> {
					 d.setCancellableByUser(false);
					 d.setBody(paragraph(
						"simpleconfig.ui.saved_hotkeys.saving.remote", displayName
					 ));
					 d.setSuccessDialog(InfoDialog.create(
						new TranslationTextComponent("simpleconfig.ui.saved_hotkeys.saved"),
						paragraph(
						  "simpleconfig.ui.saved_hotkeys.saved.remote", displayName)
					 ));
				 }));
		}
	}
	
	public void delete(boolean confirm) {
		SavedHotKeyGroup value = selector.getValue();
		if (value instanceof WritableSavedHotKeyGroup) {
			WritableSavedHotKeyGroup writable = (WritableSavedHotKeyGroup) value;
			if (writable.canWrite()) {
				IFormattableTextComponent displayName = new StringTextComponent(writable.getName())
				  .mergeStyle(TextFormatting.LIGHT_PURPLE);
				String rr = writable instanceof RemoteSavedHotKeyGroup? "remote" : "local";
				if (confirm) {
					getScreen().addDialog(ConfirmDialog.create(
					  new TranslationTextComponent("simpleconfig.ui.saved_hotkeys.confirm.delete.title"), d -> {
						  d.setBody(splitTtc(
							 "simpleconfig.ui.saved_hotkeys.confirm.delete.body." + rr, displayName
						  ));
						  d.setConfirmText(new TranslationTextComponent("simpleconfig.ui.saved_hotkeys.confirm.delete"));
						  d.setConfirmButtonTint(0x80BD2424);
						  d.withAction(b -> {
							  if (b) delete(false);
						  });
					  }));
				} else getScreen().addDialog(ProgressDialog.create(
				  new TranslationTextComponent("simpleconfig.ui.saved_hotkeys.deleting"),
				  writable.delete(), d -> {
					  d.setCancellableByUser(false);
					  d.setBody(paragraph(
						 "simpleconfig.ui.saved_hotkeys.deleting." + rr, displayName
					  ));
					  d.setSuccessDialog(InfoDialog.create(
						 new TranslationTextComponent("simpleconfig.ui.saved_hotkeys.deleted"),
						 paragraph(
							"simpleconfig.ui.saved_hotkeys.deleted." + rr, displayName)
					  ));
				  }));
			}
		}
	}
	
	@Override public void render(
	  @NotNull MatrixStack mStack, int mouseX, int mouseY, float partialTicks
	) {
		long time = System.currentTimeMillis();
		if (time - lastUpdate > UPDATE_INTERVAL) {
			lastUpdate = time;
			update();
		}
		Rectangle area = getArea();
		pos(selector, area.x + 1, area.y + 2, area.width - 42, area.height - 4);
		pos(loadButton, area.getMaxX() - 40, area.y);
		pos(saveButton, area.getMaxX() - 20, area.y);
		
		selector.render(mStack, mouseX, mouseY, partialTicks);
		loadButton.render(mStack, mouseX, mouseY, partialTicks);
		saveButton.render(mStack, mouseX, mouseY, partialTicks);
	}
	
	protected void update() {
		localGroups = getLocalGroups();
		resourceGroups = ResourceConfigHotKeyGroupHandler.INSTANCE.getResourceHotKeyGroups();
		updateSelector();
		if (lastFuture == null) {
			lastFuture = SimpleConfigNetworkHandler.getRemoteSavedHotKeyGroups();
			lastFuture.thenAccept(g -> {
				remoteGroups = g;
				lastFuture = null;
				updateSelector();
			});
		}
	}
	
	protected void updateSelector() {
		selector.setText(selector.getText());
	}
	
	@Override public @NotNull List<? extends IGuiEventListener> getEventListeners() {
		return listeners;
	}
	
	public List<SavedHotKeyGroup> getSavedGroups() {
		return Stream.of(
		  localGroups, remoteGroups, resourceGroups
		).flatMap(List::stream).collect(Collectors.toList());
	}
	
	public List<LocalSavedHotKeyGroup> getLocalGroups() {
		String YAML_EXT = ".yaml";
		File[] files = LOCAL_HOTKEYS_DIR.toFile().listFiles((dir, name) -> name.endsWith(YAML_EXT));
		List<LocalSavedHotKeyGroup> list = new ArrayList<>();
		if (files != null) for (File file: files) {
			String name = file.getName();
			list.add(SavedHotKeyGroup.local(name.substring(0, name.length() - YAML_EXT.length())));
		}
		return list;
	}
	
	public static abstract class SavedHotKeyGroup {
		private static final Pattern HYPHEN = Pattern.compile(":");
		private final String name;
		
		public static LocalSavedHotKeyGroup local(String name) {
			return new LocalSavedHotKeyGroup(name);
		}
		
		public static RemoteSavedHotKeyGroup remote(String name) {
			return new RemoteSavedHotKeyGroup(name);
		}
		
		public static ResourceSavedHotKeyGroup resource(String name, ResourceLocation location) {
			return new ResourceSavedHotKeyGroup(name, location);
		}
		
		protected SavedHotKeyGroup(String name) {
			this.name = name;
		}
		public String getName() {
			return name;
		}
		
		public CompletableFuture<ConfigHotKeyGroup> load() {
			return loadData().thenApply(data -> ConfigHotKeyManager.INSTANCE.load(getName(), data));
		}
		public abstract CompletableFuture<byte[]> loadData();
		
		protected static <T> CompletableFuture<T> failedFuture(Throwable throwable) {
			final CompletableFuture<T> future = new CompletableFuture<>();
			future.completeExceptionally(throwable);
			return future;
		}
		
		public ITextComponent getDisplayName() {
			String name = getName();
			if (name.contains(":")) {
				String[] split = HYPHEN.split(name, 2);
				return new StringTextComponent(split[0]).mergeStyle(TextFormatting.GRAY)
				  .append(new StringTextComponent(":").mergeStyle(TextFormatting.DARK_GRAY).mergeStyle(TextFormatting.BOLD))
				  .append(new StringTextComponent(split[1]).mergeStyle(getStyle()));
			}
			return new StringTextComponent(name).mergeStyle(getStyle());
		}
		public Style getStyle() {
			return Style.EMPTY.applyFormatting(TextFormatting.WHITE);
		}
		public Icon getIcon() {
			return Icon.EMPTY;
		}
		
		@Override public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			SavedHotKeyGroup that = (SavedHotKeyGroup) o;
			return name.equals(that.name);
		}
		
		@Override public int hashCode() {
			return Objects.hash(name);
		}
	}
	
	public static abstract class WritableSavedHotKeyGroup extends SavedHotKeyGroup {
		protected WritableSavedHotKeyGroup(String name) {
			super(name);
		}
		
		@OnlyIn(Dist.CLIENT) public boolean canWrite() {
			return true;
		}
		
		public CompletableFuture<Boolean> save(ConfigHotKeyGroup group) {
			return saveData(ConfigHotKeyManager.INSTANCE.dump(group));
		}
		public abstract CompletableFuture<Boolean> saveData(byte[] fileData);
		public abstract CompletableFuture<Boolean> delete();
	}
	
	public static class LocalSavedHotKeyGroup extends WritableSavedHotKeyGroup {
		protected LocalSavedHotKeyGroup(String name) {
			super(name);
		}
		
		public File getFile() {
			return LOCAL_HOTKEYS_DIR.resolve(getName() + ".yaml").toFile();
		}
		
		@Override public CompletableFuture<byte[]> loadData() {
			try {
				return completedFuture(FileUtils.readFileToByteArray(getFile()));
			} catch (IOException e) {
				return failedFuture(e);
			}
		}
		@Override public CompletableFuture<Boolean> saveData(byte[] fileData) {
			try {
				FileUtils.writeByteArrayToFile(getFile(), fileData);
				return completedFuture(true);
			} catch (IOException e) {
				return failedFuture(e);
			}
		}
		@Override public CompletableFuture<Boolean> delete() {
			File file = getFile();
			if (file.isFile()) {
				if (!file.delete())
					return failedFuture(new IOException("Failed to delete file: " + file));
				return completedFuture(true);
			} else return failedFuture(new FileNotFoundException("File not found: " + file));
		}
		
		@Override public Style getStyle() {
			return Style.EMPTY.applyFormatting(TextFormatting.GREEN);
		}
		@Override public Icon getIcon() {
			return Hotkeys.LOCAL_HOTKEY;
		}
	}
	
	public static class RemoteSavedHotKeyGroup extends WritableSavedHotKeyGroup {
		protected RemoteSavedHotKeyGroup(String name) {
			super(name);
		}
		
		@OnlyIn(Dist.CLIENT) @Override public boolean canWrite() {
			ClientPlayerEntity player = Minecraft.getInstance().player;
			if (player == null) return false;
			return ServerConfig.permissions.canEditServerHotKeys(player);
		}
		
		@Override public CompletableFuture<byte[]> loadData() {
			return SimpleConfigNetworkHandler.getRemoteSavedHotKeyGroup(getName());
		}
		@Override public CompletableFuture<Boolean> saveData(byte[] fileData) {
			return SimpleConfigNetworkHandler.saveRemoteHotKeyGroup(getName(), fileData);
		}
		@Override public CompletableFuture<Boolean> delete() {
			return saveData(null);
		}
		
		@Override public Style getStyle() {
			return Style.EMPTY.applyFormatting(TextFormatting.AQUA);
		}
		@Override public Icon getIcon() {
			return Hotkeys.REMOTE_HOTKEY;
		}
	}
	
	public static class ResourceSavedHotKeyGroup extends SavedHotKeyGroup {
		private final ResourceLocation location;
		
		public ResourceSavedHotKeyGroup(String name, ResourceLocation location) {
			super(name);
			this.location = location;
		}
		
		public ResourceLocation getLocation() {
			return location;
		}
		
		protected InputStream getInputStream() throws IOException {
			return Minecraft.getInstance().getResourceManager()
			  .getResource(getLocation()).getInputStream();
		}
		
		@Override public CompletableFuture<byte[]> loadData() {
			try {
				return completedFuture(IOUtils.toByteArray(getInputStream()));
			} catch (IOException e) {
				return failedFuture(e);
			}
		}
		
		@Override public Style getStyle() {
			return Style.EMPTY.applyFormatting(TextFormatting.DARK_GREEN);
		}
		@Override public Icon getIcon() {
			return Hotkeys.RESOURCE_HOTKEY;
		}
		
		@Override public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			if (!super.equals(o)) return false;
			ResourceSavedHotKeyGroup that = (ResourceSavedHotKeyGroup) o;
			return location.equals(that.location);
		}
		
		@Override public int hashCode() {
			return Objects.hash(super.hashCode(), location);
		}
	}
	
	public static class SavedHotKeyGroupWrapper implements TypeWrapper<SavedHotKeyGroup> {
		private final SavedHotKeyGroupPickerWidget widget;
		public SavedHotKeyGroupWrapper(SavedHotKeyGroupPickerWidget widget) {
			this.widget = widget;
		}
		
		@Override public Pair<Optional<SavedHotKeyGroup>, Optional<ITextComponent>> parseElement(
		  @NotNull String text
		) {
			SavedHotKeyGroup group = widget.getSavedGroups().stream()
			  .filter(g -> text.equals(g.getName()))
			  .findFirst().orElse(null);
			return Pair.of(Optional.ofNullable(group), Optional.empty());
		}
		
		@Override public ITextComponent getDisplayName(@NotNull SavedHotKeyGroupPickerWidget.SavedHotKeyGroup element) {
			return element.getDisplayName();
		}
		
		@Override public boolean hasIcon() {
			return true;
		}
		@Override public Optional<Icon> getIcon(@Nullable SavedHotKeyGroupPickerWidget.SavedHotKeyGroup element, String text) {
			return element != null
			       ? Optional.ofNullable(element.getIcon())
			       : text.isEmpty()? Optional.empty() : Optional.of(Hotkeys.SAVE_HOTKEY);
		}
		@Override public int getIconHeight() {
			return 14;
		}
		@Override public int getIconWidth() {
			return 14;
		}
	}
}
