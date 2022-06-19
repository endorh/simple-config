package endorh.simpleconfig.clothconfig2.api;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.AbstractGui;

public interface IChildListEntry {
	default void renderChild(
	  MatrixStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		renderChildBg(mStack, x, y, w, h, mouseX, mouseY, delta);
		renderChildEntry(mStack, x, y, w, h, mouseX, mouseY, delta);
		renderChildOverlay(mStack, x, y, w, h, mouseX, mouseY, delta);
	}
	
	default void renderChildBg(MatrixStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta) {}
	
	void renderChildEntry(MatrixStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta);
	
	/**
	 * Should be called by implementations to render overlays after calls to renderInput
	 */
 	default void renderChildOverlay(
		MatrixStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
   ) {
		if (this instanceof AbstractConfigEntry) {
			final AbstractConfigEntry<?> self = (AbstractConfigEntry<?>) this;
			if (!self.isEditable())
				AbstractGui.fill(mStack, x, y, x + w, y + h, 0x42BDBDBD);
			final String matchedValueText = self.matchedValueText;
			if (matchedValueText != null && !matchedValueText.isEmpty()) {
				final int color = self.isFocusedMatch() ? self.focusedMatchColor : self.matchColor;
				AbstractConfigEntry.fill(mStack, x - 1, y, x + w + 1, y + h, color);
			}
			final long t = System.currentTimeMillis() - self.lastHistoryTime;
			if (t < 1000) {
				int color = self.lastHistoryError? self.historyErrorColor : self.historyApplyColor;
				AbstractGui.fill(mStack, 16, y, x + w, y + self.getCaptionHeight(),
				     color & 0xFFFFFF | (int) ((color >> 24 & 0xFF) * ((1000 - t) / 1000D)) << 24);
			}
			if (this instanceof AbstractConfigListEntry) {
				if (isChild())
					((AbstractConfigListEntry<?>) this).entryArea.setBounds(x, y, w, h);
			}
		}
	}
	boolean isChild();
	void setChild(boolean child);
}
