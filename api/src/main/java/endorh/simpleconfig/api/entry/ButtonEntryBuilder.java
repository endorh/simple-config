package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.KeyEntryBuilder;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.Contract;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface ButtonEntryBuilder
  extends GUIOnlyEntryBuilder<Consumer<ConfigEntryHolder>, Runnable, ButtonEntryBuilder>,
          KeyEntryBuilder<Runnable> {
	@Contract(pure=true) ButtonEntryBuilder label(String translation);
	
	@Contract(pure=true) ButtonEntryBuilder label(ITextComponent label);
	
	@Contract(pure=true) ButtonEntryBuilder label(Supplier<ITextComponent> label);
}
