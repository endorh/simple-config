package endorh.simpleconfig.ui.hotkey;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.ui.api.IOverlayCapableContainer;
import endorh.simpleconfig.ui.gui.SimpleConfigIcons.Buttons;
import endorh.simpleconfig.ui.gui.WidgetUtils;
import endorh.simpleconfig.ui.gui.widget.IPositionableRenderable.IRectanglePositionableRenderable;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import endorh.simpleconfig.ui.gui.widget.combobox.ComboBoxWidget;
import endorh.simpleconfig.ui.gui.widget.combobox.SimpleComboBoxModel;
import endorh.simpleconfig.ui.gui.widget.combobox.wrapper.ITypeWrapper;
import endorh.simpleconfig.ui.hotkey.ConfigHotKeyManager.ConfigHotKeyGroup;
import endorh.simpleconfig.ui.hotkey.ConfigHotKeyTreeView.ConfigHotKeyTreeViewEntry;
import endorh.simpleconfig.ui.hotkey.ConfigHotKeyTreeView.ConfigHotKeyTreeViewEntry.ConfigHotKeyTreeViewGroupEntry;
import endorh.simpleconfig.ui.math.Rectangle;
import net.minecraft.client.gui.FocusableGui;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static endorh.simpleconfig.core.SimpleConfigPaths.LOCAL_HOTKEYS_DIR;
import static endorh.simpleconfig.ui.gui.WidgetUtils.pos;

public class SavedHotKeyGroupPickerWidget extends FocusableGui implements IRectanglePositionableRenderable {
	protected final List<IGuiEventListener> listeners = new ArrayList<>();
	protected final Rectangle area = new Rectangle(0, 0, 100, 20);
	protected final ConfigHotKeyTreeView tree;
	protected MultiFunctionImageButton loadButton;
	protected MultiFunctionImageButton saveButton;
	protected ComboBoxWidget<SavedHotKeyGroup> selector;
	
	public SavedHotKeyGroupPickerWidget(
	  IOverlayCapableContainer overlayContainer, ConfigHotKeyTreeView tree
	) {
		this.tree = tree;
		loadButton = new MultiFunctionImageButton(0, 0, 20, 20, Buttons.LOAD, ButtonAction.of(
		  this::load
		).active(() -> selector.getValue() != null)
		  .tooltip(new TranslationTextComponent("simpleconfig.ui.hotkey.dialog.load_group")));
		saveButton = new MultiFunctionImageButton(0, 0, 20, 20, Buttons.SAVE, ButtonAction.of(
		  this::save
		).active(() -> !selector.getText().isEmpty() && tree.getFocusedEntry() instanceof ConfigHotKeyTreeViewGroupEntry)
		  .tooltip(new TranslationTextComponent("simpleconfig.ui.hotkey.dialog.save_group")));
		selector = new ComboBoxWidget<>(
		  new SavedHotKeyGroupWrapper(), () -> overlayContainer, 0, 0, 20, 20);
		selector.setSuggestionProvider(new SimpleComboBoxModel<>(this::getSavedGroups));
		Stream.of(selector, loadButton, saveButton).forEach(listeners::add);
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
	
	public void load() {
		SavedHotKeyGroup group = selector.getValue();
		if (group != null) {
			ConfigHotKeyGroup g = ConfigHotKeyManager.INSTANCE.load(group.name, group.getFile());
			if (g != null) tree.tryAddEntry(
			  new ConfigHotKeyTreeViewGroupEntry(tree::getDialogScreen, g));
		}
	}
	
	public void save() {
		ConfigHotKeyTreeViewEntry entry = tree.getFocusedEntry();
		while (entry != null && !(entry instanceof ConfigHotKeyTreeViewGroupEntry))
			entry = entry.getParent();
		if (entry == null) return;
		ConfigHotKeyGroup g = ((ConfigHotKeyTreeViewGroupEntry) entry).buildGroup();
		SavedHotKeyGroup value = selector.getValue();
		if (value == null) value = new SavedHotKeyGroup(selector.getText());
		ConfigHotKeyManager.INSTANCE.dump(g, value.getFile());
		selector.setText(selector.getText());
	}
	
	@Override public void render(
	  @NotNull MatrixStack mStack, int mouseX, int mouseY, float partialTicks
	) {
		Rectangle area = getArea();
		pos(selector, area.x + 1, area.y + 1, area.width - 42, area.height - 2);
		pos(loadButton, area.getMaxX() - 40, area.y);
		pos(saveButton, area.getMaxX() - 20, area.y);
		
		selector.render(mStack, mouseX, mouseY, partialTicks);
		loadButton.render(mStack, mouseX, mouseY, partialTicks);
		saveButton.render(mStack, mouseX, mouseY, partialTicks);
	}
	
	@Override public @NotNull List<? extends IGuiEventListener> getEventListeners() {
		return listeners;
	}
	
	public List<SavedHotKeyGroup> getSavedGroups() {
		String YAML_EXT = ".yaml";
		File[] files = LOCAL_HOTKEYS_DIR.toFile().listFiles((dir, name) -> name.endsWith(YAML_EXT));
		List<SavedHotKeyGroup> list = new ArrayList<>();
		if (files != null) for (File file: files) {
			String name = file.getName();
			list.add(new SavedHotKeyGroup(name.substring(0, name.length() - YAML_EXT.length())));
		}
		return list;
	}
	
	public static class SavedHotKeyGroup {
		private final String name;
		
		public SavedHotKeyGroup(String name) {
			this.name = name;
		}
		
		public static @Nullable SavedHotKeyGroup ifExists(String name) {
			SavedHotKeyGroup group = new SavedHotKeyGroup(name);
			return group.getFile().isFile()? group : null;
		}
		
		public String getName() {
			return name;
		}
		
		public File getFile() {
			return LOCAL_HOTKEYS_DIR.resolve(name + ".yaml").toFile();
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
	
	public static class SavedHotKeyGroupWrapper implements ITypeWrapper<SavedHotKeyGroup> {
		@Override public Pair<Optional<SavedHotKeyGroup>, Optional<ITextComponent>> parseElement(
		  @NotNull String text
		) {
			SavedHotKeyGroup group = SavedHotKeyGroup.ifExists(text);
			return Pair.of(Optional.ofNullable(group), Optional.empty());
		}
		
		@Override public ITextComponent getDisplayName(@NotNull SavedHotKeyGroup element) {
			return new StringTextComponent(element.getName());
		}
	}
}
