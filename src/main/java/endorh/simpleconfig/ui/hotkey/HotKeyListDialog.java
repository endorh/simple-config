package endorh.simpleconfig.ui.hotkey;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.SimpleConfigMod.KeyBindings;
import endorh.simpleconfig.api.SimpleConfigGroup;
import endorh.simpleconfig.config.ClientConfig.confirm;
import endorh.simpleconfig.core.SimpleConfigGUIManagerImpl;
import endorh.simpleconfig.ui.api.IDialogCapableScreen;
import endorh.simpleconfig.ui.gui.AbstractButtonDialog;
import endorh.simpleconfig.ui.gui.AbstractDialog;
import endorh.simpleconfig.ui.gui.ConfirmDialog;
import endorh.simpleconfig.ui.gui.widget.CheckboxButton;
import endorh.simpleconfig.ui.gui.widget.TintedButton;
import endorh.simpleconfig.ui.gui.widget.treeview.ArrangeableTreeViewEntry;
import endorh.simpleconfig.ui.hotkey.ConfigHotKeyManager.ConfigHotKeyGroup;
import endorh.simpleconfig.ui.hotkey.ConfigHotKeyTreeView.ConfigHotKeyTreeViewEntry;
import endorh.simpleconfig.ui.hotkey.ConfigHotKeyTreeView.ConfigHotKeyTreeViewEntry.ConfigHotKeyTreeViewGroupEntry;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import static endorh.simpleconfig.api.SimpleConfigTextUtil.splitTtc;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class HotKeyListDialog extends AbstractButtonDialog {
	private final ConfigHotKeyTreeView treeView;
	
	public static AbstractDialog forModId(String modId) {
		return new HotKeyListDialog(modId);
	}
	
	public static AbstractDialog generic() {
		return new HotKeyListDialog(null);
	}
	
	public HotKeyListDialog(@Nullable String modId) {
		super(new TranslatableComponent("simpleconfig.ui.hotkey.dialog.title"));
		TintedButton cancelButton = TintedButton.of(CommonComponents.GUI_CANCEL, p -> cancel(false));
		cancelButton.setTintColor(0x64FFA080);
		addButton(cancelButton);
		TintedButton saveButton = TintedButton.of(
		  new TranslatableComponent("simpleconfig.ui.save"),
		  p -> cancel(true));
		saveButton.setTintColor(0x6480FFA0);
		addButton(saveButton);
		
		treeView = new ConfigHotKeyTreeView(
		  this::getScreen, this,
		  ConfigHotKeyManager.INSTANCE.getHotKeys(), this::editHotKey);
		treeView.setContextModId(modId);
		treeView.setTransparent(true);
		bodyListeners.add(treeView);
	}
	
	public ConfigHotKeyTreeView getTreeView() {
		if (treeView == null)
			throw new IllegalStateException("TreeView cannot be accessed until a screen is attached");
		return treeView;
	}
	
	public void editHotKey(String modId, ConfigHotKey hotKey) {
		IDialogCapableScreen screen = getScreen();
		SimpleConfigGUIManagerImpl.INSTANCE.showConfigGUIForHotKey(modId, screen, this, hotKey);
		screen.removeDialog(this);
	}
	
	@Override public void tick(boolean top) {
		treeView.tick();
	}
	
	@Override public boolean canCopyText() {
		return false;
	}
	
	public boolean isEdited() {
		return !ConfigHotKeyManager.INSTANCE.getHotKeys().equals(getValue());
	}
	
	public ConfigHotKeyGroup getValue() {
		ArrangeableTreeViewEntry<ConfigHotKeyTreeViewEntry> root = getTreeView().getRoot();
		if (!(root instanceof ConfigHotKeyTreeViewGroupEntry))
			throw new IllegalStateException("Tree View root entry is not group");
		return ((ConfigHotKeyTreeViewGroupEntry) root).buildGroup();
	}
	
	@Override public void cancel(boolean success) {
		SimpleConfigGroup CONFIRM = SimpleConfigMod.CLIENT_CONFIG.getGroup("confirm");
		String SAVE_HOTKEYS = "save_hotkeys";
		String DISCARD_HOTKEYS = "discard_hotkeys";
		if (success) {
			if (confirm.save_hotkeys && isEdited()) {
				getScreen().addDialog(ConfirmDialog.create(
				  new TranslatableComponent("simpleconfig.ui.hotkey.dialog.save.title"),
				  d -> {
					  d.setBody(splitTtc("simpleconfig.ui.hotkey.dialog.save.body"));
					  d.setConfirmText(new TranslatableComponent("simpleconfig.ui.controls.general.save"));
					  d.setConfirmButtonTint(0x8080FF80);
					  d.withCheckBoxes((b, c) -> {
						  if (b) {
							  if (CONFIRM.hasGUI()) {
								  CONFIRM.setGUI(SAVE_HOTKEYS, !c[0]);
							  } else CONFIRM.set(SAVE_HOTKEYS, !c[0]);
							  save();
							  super.cancel(true);
						  }
					  }, CheckboxButton.of(false, new TranslatableComponent(
						 "simpleconfig.ui.do_not_ask_again"
					  )));
				  }
				));
			} else {
				save();
				getTreeView().removeCandidates();
				super.cancel(true);
			}
		} else if (confirm.discard_hotkeys && isEdited()) {
			getScreen().addDialog(ConfirmDialog.create(
			  new TranslatableComponent(
				 "simpleconfig.ui.hotkey.dialog.discard.title"),
			  d -> {
				  d.setBody(splitTtc("simpleconfig.ui.hotkey.dialog.discard.body"));
				  d.setConfirmText(new TranslatableComponent("simpleconfig.ui.controls.general.discard"));
				  d.setConfirmButtonTint(0x80FF8080);
				  d.withCheckBoxes((b, c) -> {
					  if (b) {
						  if (CONFIRM.hasGUI()) {
							  CONFIRM.setGUI(DISCARD_HOTKEYS, !c[0]);
						  } else CONFIRM.set(DISCARD_HOTKEYS, !c[0]);
						  super.cancel(false);
					  }
				  }, CheckboxButton.of(false, new TranslatableComponent(
					 "simpleconfig.ui.do_not_ask_again"
				  )));
			  }
			));
		} else {
			getTreeView().removeCandidates();
			super.cancel(false);
		}
	}
	
	protected void save() {
		ConfigHotKeyManager.INSTANCE.updateHotKeys(getValue());
	}
	
	@Override protected void layout() {
		int w = Mth.clamp((int) (getScreen().width * 0.7), 120, 800);
		final int titleWidth = font.width(title);
		w = max(w, titleWidth + 16);
		int h = Mth.clamp(60 + getInnerHeight(), 68, (int) (getScreen().height * 0.9));
		w = max(w, buttons.size() * 80);
		w = min(w, getScreen().width - 16);
		setWidth(w);
		setHeight(h);
		super.layout();
	}
	
	@Override public void renderInner(
	  PoseStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		ConfigHotKeyTreeView treeView = getTreeView();
		treeView.setX(x);
		treeView.setY(y);
		treeView.setWidth(w);
		treeView.setHeight(h - 4);
		treeView.render(mStack, mouseX, mouseY, delta);
	}
	
	@Override public int getInnerHeight() {
		return Mth.clamp(getTreeView().getPreferredHeight() + 4, 64, (int) (getScreen().height * 0.9) - 60);
	}
	
	@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (KeyBindings.SAVE.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
			cancel(true);
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}
}
