package endorh.simpleconfig.ui.hotkey;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.core.SimpleConfigGUIManager;
import endorh.simpleconfig.ui.api.IDialogCapableScreen;
import endorh.simpleconfig.ui.gui.AbstractButtonDialog;
import endorh.simpleconfig.ui.gui.AbstractDialog;
import endorh.simpleconfig.ui.gui.widget.TintedButton;
import endorh.simpleconfig.ui.gui.widget.treeview.ArrangeableTreeViewEntry;
import endorh.simpleconfig.ui.hotkey.ConfigHotKeyTreeView.ConfigHotKeyTreeViewEntry;
import endorh.simpleconfig.ui.hotkey.ConfigHotKeyTreeView.ConfigHotKeyTreeViewEntry.ConfigHotKeyTreeViewGroupEntry;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.util.text.TranslationTextComponent;
import org.jetbrains.annotations.Nullable;

import static java.lang.Math.min;
import static net.minecraft.util.math.MathHelper.clamp;

public class HotKeyListDialog extends AbstractButtonDialog {
	private final ConfigHotKeyTreeView treeView;
	
	public static AbstractDialog forModId(String modId) {
		return new HotKeyListDialog(modId);
	}
	
	public static AbstractDialog generic() {
		return new HotKeyListDialog(null);
	}
	
	public HotKeyListDialog(@Nullable String modId) {
		super(new TranslationTextComponent("simpleconfig.ui.hotkey.dialog.title"));
		TintedButton cancelButton = TintedButton.of(80, 20, DialogTexts.GUI_CANCEL, p -> cancel(false));
		cancelButton.setTintColor(0x64FFA080);
		addButton(cancelButton);
		TintedButton saveButton = TintedButton.of(
		  80, 20, new TranslationTextComponent("simpleconfig.ui.save"),
		  p -> cancel(true));
		saveButton.setTintColor(0x6480FFA0);
		addButton(saveButton);
		setPersistent(true);
		
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
		SimpleConfigGUIManager.showConfigGUIForHotKey(modId, screen, this, hotKey);
		screen.removeDialog(this);
	}
	
	@Override public void tick() {
		treeView.tick();
	}
	
	@Override public boolean canCopyText() {
		return false;
	}
	
	@Override public void cancel(boolean success) {
		if (success) {
			ArrangeableTreeViewEntry<ConfigHotKeyTreeViewEntry> root = treeView.getRoot();
			if (root instanceof ConfigHotKeyTreeViewGroupEntry) {
				ConfigHotKeyManager.INSTANCE.updateHotKeys(
				  ((ConfigHotKeyTreeViewGroupEntry) root).buildGroup());
			}
		}
		super.cancel(success);
	}
	
	@Override protected void layout() {
		int w = clamp((int) (getScreen().width * 0.7), 120, 800);
		final int titleWidth = font.getStringPropertyWidth(title);
		if (titleWidth + 16 > getArea().getWidth())
			w = min(getScreen().width - 32, titleWidth + 16);
		int h = clamp(60 + getInnerHeight(), 68, (int) (getScreen().height * 0.9));
		setWidth(w);
		setHeight(h);
		super.layout();
	}
	
	@Override public void renderInner(
	  MatrixStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		ConfigHotKeyTreeView treeView = getTreeView();
		treeView.setX(x);
		treeView.setY(y);
		treeView.setWidth(w);
		treeView.setHeight(h - 4);
		treeView.render(mStack, mouseX, mouseY, delta);
	}
	
	@Override public int getInnerHeight() {
		return clamp(getTreeView().getPreferredHeight() + 4, 64, (int) (getScreen().height * 0.9) - 60);
	}
}
