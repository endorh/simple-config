package endorh.simpleconfig.clothconfig2.gui.entries;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.clothconfig2.api.AbstractConfigEntry;
import endorh.simpleconfig.clothconfig2.api.AbstractConfigListEntry;
import endorh.simpleconfig.clothconfig2.api.IChildListEntry;
import endorh.simpleconfig.clothconfig2.gui.entries.SubCategoryListEntry.VoidEntry;
import endorh.simpleconfig.clothconfig2.math.Rectangle;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class SubCategoryListEntry extends CaptionedSubCategoryListEntry<Void, VoidEntry> {
	public SubCategoryListEntry(
	  ITextComponent title, List<AbstractConfigListEntry<?>> entries
	) {
		super(title, entries, null);
	}
	
	@Internal public static abstract class VoidEntry extends AbstractConfigEntry<Void> implements IChildListEntry {
		private VoidEntry(ITextComponent title) {
			super(title);
		}
		@Override public void renderChildEntry(
		  MatrixStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
		) {}
		@Override public Rectangle getRowArea() {
			return null;
		}
		@Override public int getScrollY() {
			return 0;
		}
		@Override public @NotNull List<? extends IGuiEventListener> children() {
			return Collections.emptyList();
		}
	}
}
