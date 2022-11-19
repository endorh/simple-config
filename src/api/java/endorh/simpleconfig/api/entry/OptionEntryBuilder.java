package endorh.simpleconfig.api.entry;

import com.mojang.serialization.Codec;
import endorh.simpleconfig.api.ConfigEntryBuilder;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public interface OptionEntryBuilder<T> extends ConfigEntryBuilder<
  @NotNull T, String, T, OptionEntryBuilder<T>
> {
	@Contract(pure=true)
	@NotNull OptionEntryBuilder<T> withOptions(Supplier<List<T>> options);
	
	@Contract(pure=true)
	@NotNull OptionEntryBuilder<T> withOptions(List<T> options);
	
	@SuppressWarnings("unchecked") @Contract(pure=true)
	@NotNull OptionEntryBuilder<T> withOptions(T... options);
	
	@Contract(pure=true)
	@NotNull OptionEntryBuilder<T> withDisplay(Function<T, ITextComponent> display);
	
	@Contract(pure=true)
	@NotNull OptionEntryBuilder<T> withCodec(Codec<T> codec);
	
	@Contract(pure=true)
	@NotNull OptionEntryBuilder<T> withSerializer(Function<T, String> serializer);
	
	@Contract(pure=true) @NotNull OptionEntryBuilder<T> withSerializer(
	  Function<T, String> serializer, Function<String, T> deserializer);
}
