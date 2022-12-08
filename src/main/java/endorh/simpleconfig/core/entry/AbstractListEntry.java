package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.ListEntryBuilder;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.AbstractConfigEntryBuilder;
import endorh.simpleconfig.core.EntryType;
import endorh.simpleconfig.ui.impl.builders.ListFieldBuilder;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public abstract class AbstractListEntry
  <V, Config, Gui, Self extends AbstractListEntry<V, Config, Gui, Self>>
  extends AbstractConfigEntry<List<V>, List<Config>, List<Gui>> {
	protected Class<?> innerType;
	protected Function<V, Optional<ITextComponent>> elemErrorSupplier;
	protected boolean expand;
	protected int minSize = 0;
	protected int maxSize = Integer.MAX_VALUE;
	
	public AbstractListEntry(
	  ConfigEntryHolder parent, String name, @Nullable List<V> value
	) {
		super(parent, name, value != null ? value : new ArrayList<>());
	}
	
	public static abstract class Builder<V, Config, Gui,
	  Entry extends AbstractListEntry<V, Config, Gui, Entry>,
	  Self extends ListEntryBuilder<V, Config, Gui, Self>,
	  SelfImpl extends Builder<V, Config, Gui, Entry, Self, SelfImpl>
	> extends AbstractConfigEntryBuilder<List<V>, List<Config>, List<Gui>, Entry, Self, SelfImpl>
	  implements ListEntryBuilder<V, Config, Gui, Self> {
		protected Function<V, Optional<ITextComponent>> elemErrorSupplier = v -> Optional.empty();
		protected boolean expand = false;
		protected Class<?> innerType;
		protected int minSize = 0;
		protected int maxSize = Integer.MAX_VALUE;
		
		public Builder(List<V> value, EntryType<?> innerType) {
			super(value, EntryType.of(List.class, innerType));
			this.innerType = innerType.type();
		}
		
		@Override @Contract(pure=true) public @NotNull Self expand() {
			return expand(true);
		}
		
		@Override @Contract(pure=true) public @NotNull Self expand(boolean expand) {
			SelfImpl copy = copy();
			copy.expand = expand;
			return copy.castSelf();
		}
		
		@Override @Contract(pure=true) public @NotNull Self minSize(int minSize) {
			SelfImpl copy = copy();
			copy.minSize = minSize;
			return copy.castSelf();
		}
		
		@Override @Contract(pure=true) public @NotNull Self maxSize(int maxSize) {
			SelfImpl copy = copy();
			copy.maxSize = maxSize;
			return copy.castSelf();
		}
		
		@Override @Contract(pure=true) public @NotNull Self elemError(
		  Function<V, Optional<ITextComponent>> errorSupplier
		) {
			SelfImpl copy = copy();
			copy.elemErrorSupplier = errorSupplier;
			return copy.castSelf();
		}
		
		@Override protected Entry build(@NotNull ConfigEntryHolder parent, String name) {
			final Entry e = super.build(parent, name);
			e.elemErrorSupplier = elemErrorSupplier;
			e.expand = expand;
			e.innerType = innerType;
			e.minSize = minSize;
			e.maxSize = maxSize;
			return e;
		}
		
		@Override public SelfImpl copy(List<V> value) {
			final SelfImpl copy = super.copy(value);
			copy.elemErrorSupplier = elemErrorSupplier;
			copy.expand = expand;
			copy.innerType = innerType;
			copy.minSize = minSize;
			copy.maxSize = maxSize;
			return copy;
		}
	}
	
	@Override public List<Gui> forGui(List<V> list) {
		return list.stream().map(this::elemForGui).collect(Collectors.toList());
	}
	
	@Override public @Nullable List<V> fromGui(@Nullable List<Gui> list) {
		if (list == null) return null;
		List<V> res = new ArrayList<>();
		for (Gui g: list) {
			V v = elemFromGui(g);
			if (v == null) return null;
			res.add(v);
		}
		return res;
	}
	
	@Override public List<Config> forConfig(List<V> list) {
		return list.stream().map(this::elemForConfig).collect(Collectors.toList());
	}
	
	@Override public @Nullable List<V> fromConfig(@Nullable List<Config> list) {
		if (list == null) return null;
		List<V> res = new ArrayList<>();
		for (Config c : list) {
			V v = elemFromConfig(c);
			if (v == null) return null;
			res.add(v);
		}
		return res;
	}
	
	protected Gui elemForGui(V value) {
		//noinspection unchecked
		return (Gui) value;
	}
	
	protected @Nullable V elemFromGui(Gui value) {
		//noinspection unchecked
		return (V) value;
	}
	
	protected Config elemForConfig(V value) {
		//noinspection unchecked
		return (Config) value;
	}
	
	protected @Nullable V elemFromConfig(Config value) {
		//noinspection unchecked
		return (V) value;
	}
	
	protected static ITextComponent addIndex(ITextComponent message, int index) {
		if (index < 0) return message;
		return message.copyRaw().appendString(", ").append(new TranslationTextComponent(
		  "simpleconfig.config.error.at_index",
		  new StringTextComponent(String.format("%d", index + 1)).mergeStyle(TextFormatting.DARK_AQUA)));
	}
	
	@Override public List<ITextComponent> getErrorsFromGUI(List<Gui> value) {
		return Stream.concat(
		  Stream.of(getErrorFromGUI(value)).filter(Optional::isPresent).map(Optional::get),
		  IntStream.range(0, value.size()).boxed()
		    .flatMap(i -> getElementErrors(i, value.get(i)).stream())
		).collect(Collectors.toList());
	}
	
	@Override public Optional<ITextComponent> getErrorFromGUI(List<Gui> value) {
		int size = value.size();
		if (size < minSize) {
			return Optional.of(new TranslationTextComponent(
			  "simpleconfig.config.error.list." + (minSize == 1? "empty" : "too_small"),
			  new StringTextComponent(String.valueOf(minSize)).mergeStyle(TextFormatting.DARK_AQUA)));
		} else if (size > maxSize) {
			return Optional.of(new TranslationTextComponent(
			  "simpleconfig.config.error.list.too_large",
			  new StringTextComponent(String.valueOf(maxSize)).mergeStyle(TextFormatting.DARK_AQUA)));
		}
		return super.getErrorFromGUI(value);
	}
	
	public Optional<ITextComponent> getElementError(int index, Gui value) {
		V elem = elemFromGui(value);
		if (elem == null) return Optional.of(addIndex(new TranslationTextComponent(
		  "simpleconfig.config.error.missing_value"), index));
		return elemErrorSupplier.apply(elem).map(e -> addIndex(e, index));
	}
	
	public List<ITextComponent> getElementErrors(int index, Gui value) {
		return Stream.of(getElementError(index, value))
		  .filter(Optional::isPresent).map(Optional::get)
		  .collect(Collectors.toList());
	}
	
	protected @Nullable String getListTypeComment() {
		return null;
	}
	
	@Override public List<String> getConfigCommentTooltips() {
		List<String> tooltips = super.getConfigCommentTooltips();
		String typeComment = getListTypeComment();
		if (typeComment != null) tooltips.add("List: " + typeComment);
		return tooltips;
	}
	
	@OnlyIn(Dist.CLIENT) protected <F extends ListFieldBuilder<Gui, ?, F>> F decorate(F builder) {
		builder = super.decorate(builder);
		builder.setCellErrorSupplier(this::getElementError)
		  .setExpanded(expand)
		  .setCaptionControlsEnabled(false)
		  .setInsertInFront(false);
		return builder;
	}
}
