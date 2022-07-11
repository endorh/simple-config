package endorh.simpleconfig.ui.gui.widget;

import endorh.simpleconfig.ui.gui.IOverlayCapableScreen;
import endorh.simpleconfig.ui.gui.widget.combobox.ComboBoxWidget;
import endorh.simpleconfig.ui.gui.widget.combobox.wrapper.StringTypeWrapper;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class StringComboBoxWidget extends ComboBoxWidget<String> {
	
	public StringComboBoxWidget(
	  @NotNull Supplier<IOverlayCapableScreen> screen,
	  int x, int y, int width, int height
	) {
		super(new StringTypeWrapper(), screen, x, y, width, height);
	}
	
	public StringComboBoxWidget(
	  @NotNull Supplier<IOverlayCapableScreen> screen,
	  int x, int y, int width, int height, @NotNull ITextComponent title
	) {
		super(new StringTypeWrapper(), screen, x, y, width, height, title);
	}
	
	public StringComboBoxWidget(
	  @NotNull Supplier<IOverlayCapableScreen> screen,
	  @NotNull FontRenderer font, int x, int y, int width, int height,
	  @NotNull ITextComponent title
	) {
		super(new StringTypeWrapper(), screen, font, x, y, width, height, title);
	}
}
