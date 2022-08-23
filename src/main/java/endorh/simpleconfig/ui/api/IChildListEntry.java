package endorh.simpleconfig.ui.api;

import com.mojang.blaze3d.vertex.PoseStack;
import endorh.simpleconfig.ui.gui.widget.DynamicEntryListWidget;
import net.minecraft.client.gui.GuiComponent;

import static java.lang.Math.min;

/**
 * Marker interface for {@link AbstractConfigListEntry} which support being
 * rendered as a child entry.<br>
 * Must only be implemented by subclasses of {@link AbstractConfigListEntry}.<br>
 * Owners should call {@code tick()} on child entries every tick.
 */
public interface IChildListEntry {
	default void renderChild(
	  PoseStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		if (this instanceof final AbstractConfigListEntry<?> self && isChildSubEntry()) {
			self.entryArea.setBounds(x, y, w, h);
			self.fieldArea.setBounds(x, y, w, h);
			DynamicEntryListWidget<?> entryList = self.getEntryList();
			self.rowArea.setBounds(entryList.left, y, entryList.right - entryList.left, h);
		}
		renderChildBg(mStack, x, y, w, h, mouseX, mouseY, delta);
		renderChildEntry(mStack, x, y, w, h, mouseX, mouseY, delta);
		renderChildOverlay(mStack, x, y, w, h, mouseX, mouseY, delta);
	}
	
	/**
	 * Do not call directly, instead call {@link IChildListEntry#renderChild}
	 */
	default void renderChildBg(PoseStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta) {
		if (this instanceof AbstractConfigListEntry<?> self) {
			final DynamicEntryListWidget<?> entryList = self.getEntryList();
			
			// Focus overlay
			final long t = System.currentTimeMillis() - self.lastFocusHighlightTime - self.focusHighlightLength;
			if (t < 1000) {
				int color = self.focusHighlightColor;
				GuiComponent.fill(mStack, 0, y, self.getScreen().width, y + self.getCaptionHeight(),
				     color & 0xFFFFFF | (int) ((color >> 24 & 0xFF) * (min(1000, 1000 - t) / 1000D) * 0.3D) << 24);
			}
		}
	}
	
	/**
	 * Do not call directly, instead call {@link IChildListEntry#renderChild}
	 */
	void renderChildEntry(PoseStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta);
	
	/**
	 * Do not call directly, instead call {@link IChildListEntry#renderChild}
	 */
 	default void renderChildOverlay(
		PoseStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
   ) {
		if (this instanceof final AbstractConfigListEntry<?> self) {
			// Uneditable gray overlay
			if (!self.shouldRenderEditable())
				GuiComponent.fill(mStack, x, y, x + w, y + h, 0x42BDBDBD);
			
			// Yellow match overlay
			final String matchedValueText = self.matchedValueText;
			if (matchedValueText != null && !matchedValueText.isEmpty()) {
				final int color = self.isFocusedMatch() ? self.focusedMatchColor : self.matchColor;
				GuiComponent.fill(mStack, x, y, x + w, y + h, color);
			}
			
			// Focus overlay
			final long t = System.currentTimeMillis() - self.lastFocusHighlightTime - self.focusHighlightLength;
			if (t < 1000) {
				int color = self.focusHighlightColor;
				GuiComponent.fill(mStack, x, y, x + w, y + self.getCaptionHeight(),
				     color & 0xFFFFFF | (int) ((color >> 24 & 0xFF) * (min(1000, 1000 - t) / 1000D)) << 24);
			}
		}
	}
	
	default boolean isChildSubEntry() {
		return this instanceof AbstractConfigListEntry<?>
		       && ((AbstractConfigListEntry<?>) this).isSubEntry();
	}
	default void setChildSubEntry(boolean child) {
		if (this instanceof AbstractConfigListEntry<?>)
			((AbstractConfigListEntry<?>) this).setSubEntry(child);
	}
}
