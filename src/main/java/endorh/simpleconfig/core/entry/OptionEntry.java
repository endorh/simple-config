package endorh.simpleconfig.core.entry;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.OptionEntryBuilder;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.AbstractConfigEntryBuilder;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import endorh.simpleconfig.ui.impl.builders.SelectorBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.StringReader;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class OptionEntry<T> extends AbstractConfigEntry<T, String, T> {
	private static final Gson GSON = new Gson();
	public Supplier<List<T>> valueSupplier;
	public Function<T, String> serializer;
	public Function<String, @Nullable T> deserializer;
	public Function<T, Component> displayer;
	
	protected OptionEntry(ConfigEntryHolder parent, String name, T defValue) {
		super(parent, name, defValue);
	}
	
	public static class Builder<T> extends AbstractConfigEntryBuilder<
	  T, String, T, OptionEntry<T>, OptionEntryBuilder<T>, Builder<T>
	> implements OptionEntryBuilder<T> {
		Supplier<List<T>> valueSupplier = () -> Lists.newArrayList(value);
		Function<T, String> serializer = Object::toString;
		@Nullable Function<String, @Nullable T> deserializer = null;
		Function<T, Component> displayer = t -> new TextComponent(t.toString());
		
		public Builder(T value) {
			this(value, value.getClass());
		}
		
		public Builder(T value, Class<?> typeClass) {
			super(value, typeClass);
		}
		
		@Override public @NotNull OptionEntryBuilder<T> withOptions(Supplier<List<T>> options) {
			Builder<T> copy = copy();
			copy.valueSupplier = options;
			return copy;
		}
		
		@Override public @NotNull OptionEntryBuilder<T> withOptions(List<T> options) {
			return withOptions(() -> options);
		}
		
		@SafeVarargs
		@Override public final @NotNull OptionEntryBuilder<T> withOptions(T... options) {
			return withOptions(Lists.newArrayList(options));
		}
		
		@Override public @NotNull OptionEntryBuilder<T> withDisplay(Function<T, Component> display) {
			Builder<T> copy = copy();
			copy.displayer = display;
			return copy;
		}
		
		@Override public @NotNull OptionEntryBuilder<T> withCodec(Codec<T> codec) {
			Builder<T> copy = copy();
			copy.serializer = t -> {
				if (t == null) return "";
				DataResult<JsonElement> dr = codec.encodeStart(JsonOps.INSTANCE, t);
				if (dr.error().isPresent())
					dr = codec.encodeStart(JsonOps.INSTANCE, value);
				if (dr.error().isPresent()) return "";
				return dr.result().map(GSON::toJson).orElse("");
			};
			copy.deserializer = s -> {
				JsonReader reader = new JsonReader(new StringReader(s == null || s.isEmpty()? "\"\"" : s));
				JsonElement elem = JsonParser.parseReader(reader);
				DataResult<T> dr = codec.parse(JsonOps.INSTANCE, elem);
				if (dr.error().isPresent()) return null;
				return dr.result().orElse(null);
			};
			return copy;
		}
		
		@Override public @NotNull OptionEntryBuilder<T> withSerializer(Function<T, String> serializer) {
			Builder<T> copy = copy();
			copy.serializer = serializer;
			copy.deserializer = null;
			return copy;
		}
		
		@Override public @NotNull OptionEntryBuilder<T> withSerializer(
		  Function<T, String> serializer, Function<String, T> deserializer
		) {
			Builder<T> copy = copy();
			copy.serializer = serializer;
			copy.deserializer = deserializer;
			return copy;
		}
		
		@Override protected OptionEntry<T> buildEntry(ConfigEntryHolder parent, String name) {
			OptionEntry<T> entry = new OptionEntry<>(parent, name, value);
			entry.valueSupplier = valueSupplier;
			entry.serializer = serializer;
			entry.deserializer = Objects.requireNonNullElseGet(
			  deserializer, () -> s -> entry.valueSupplier.get().stream()
				 .filter(t -> entry.serializer.apply(t).equals(s))
			    .findFirst().orElse(null));
			entry.displayer = displayer;
			return entry;
		}
		
		@Override protected Builder<T> createCopy(T value) {
			Builder<T> copy = new Builder<>(value, typeClass);
			copy.valueSupplier = valueSupplier;
			copy.serializer = serializer;
			copy.deserializer = deserializer;
			copy.displayer = displayer;
			return copy;
		}
	}
	
	@Override public String forConfig(T value) {
		return serializer.apply(value);
	}
	
	@Override public @Nullable T fromConfig(@Nullable String value) {
		return deserializer.apply(value);
	}
	
	@Override public List<String> getConfigCommentTooltips() {
		List<String> tooltips = super.getConfigCommentTooltips();
		tooltips.add(0, "[Option: " + valueSupplier.get().stream()
		  .map(serializer).collect(Collectors.joining(" | ")) + "]");
		return tooltips;
	}
	
	@Override public Optional<FieldBuilder<T, ?, ?>> buildGUIEntry(ConfigFieldBuilder builder) {
		//noinspection unchecked
		SelectorBuilder<T> entryBuilder =
		  builder.startSelector(getDisplayName(), (T[]) valueSupplier.get().toArray(), forGui(get()))
		    .setNameProvider(displayer);
		return Optional.of(decorate(entryBuilder));
	}
}
