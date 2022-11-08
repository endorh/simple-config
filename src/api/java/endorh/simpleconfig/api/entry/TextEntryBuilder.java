package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public interface TextEntryBuilder extends ConfigEntryBuilder<Void, Void, Void, TextEntryBuilder> {
	/**
	 * Set the text displayed by this entry
	 */
	@Contract(pure=true) @NotNull TextEntryBuilder text(Supplier<ITextComponent> supplier);
	
	/**
	 * Set the text displayed by this entry
	 */
	@Contract(pure=true) @NotNull TextEntryBuilder text(ITextComponent text);
	
	/**
	 * @deprecated Use {@link TextEntryBuilder#args(Object...)} instead
	 */
	@Contract(pure=true) @NotNull @Override @Deprecated TextEntryBuilder nameArgs(Object... args);
	
	/**
	 * Set the arguments that will be passed to the name translation key<br>
	 * As a special case, {@code Supplier}s passed
	 * will be invoked before being passed as arguments
	 */
	@Contract(pure=true) @NotNull TextEntryBuilder args(Object... args);
}
