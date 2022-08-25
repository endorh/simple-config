package endorh.simpleconfig.core;

import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Function;

@Internal public class ByteBufUtils {
	public static <T> void writeNullable(
	  FriendlyByteBuf buf, BiConsumer<FriendlyByteBuf, T> writer, @Nullable T value
	) {
		buf.writeBoolean(value != null);
		if (value != null) writer.accept(buf, value);
	}
	
	public static <T> @Nullable T readNullable(
	  FriendlyByteBuf buf, Function<FriendlyByteBuf, T> reader
	) {
		if (buf.readBoolean()) return reader.apply(buf);
		return null;
	}
}
