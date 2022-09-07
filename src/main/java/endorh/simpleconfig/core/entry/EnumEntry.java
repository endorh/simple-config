package endorh.simpleconfig.core.entry;

import com.google.common.base.CaseFormat;
import com.google.common.collect.Lists;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.EnumEntryBuilder;
import endorh.simpleconfig.config.ClientConfig.advanced;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.AbstractConfigEntryBuilder;
import endorh.simpleconfig.core.IKeyEntry;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.gui.widget.combobox.wrapper.ITypeWrapper;
import endorh.simpleconfig.ui.impl.builders.ComboBoxFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.EnumSelectorBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EnumEntry<E extends Enum<E>>
  extends AbstractConfigEntry<E, E, E> implements IKeyEntry<E> {
	protected final Class<E> enumClass;
	protected final Map<String, E> nameMap;
	protected final Map<String, E> lowerCaseNameMap;
	protected @Nullable Boolean useComboBox;
	
	@Internal public EnumEntry(ConfigEntryHolder parent, String name, E value) {
		super(parent, name, value);
		enumClass = value.getDeclaringClass();
		nameMap = Arrays.stream(enumClass.getEnumConstants())
		  .collect(Collectors.toMap(Enum::name, Function.identity()));
		lowerCaseNameMap = Arrays.stream(enumClass.getEnumConstants())
		  .collect(Collectors.toMap(e -> e.name().toLowerCase(), Function.identity(), (a, b) -> a));
	}
	
	public static class Builder<E extends Enum<E>> extends AbstractConfigEntryBuilder<
	  E, E, E, EnumEntry<E>, EnumEntryBuilder<E>, Builder<E>>
	  implements EnumEntryBuilder<E> {
		protected final Class<E> enumClass;
		protected @Nullable Boolean useComboBox = null;
		
		public Builder(E value) {
			super(value, value.getDeclaringClass());
			enumClass = value.getDeclaringClass();
		}
		
		public Builder(Class<E> enumClass) {
			super(enumClass.getEnumConstants()[0], enumClass);
			this.enumClass = enumClass;
		}
		
		@Override @Contract(pure=true) public Builder<E> useComboBox() { return useComboBox(true); }
		
		@Override @Contract(pure=true) public Builder<E> useComboBox(Boolean useComboBox) {
			Builder<E> copy = copy();
			copy.useComboBox = useComboBox;
			return copy;
		}
		
		@Override protected EnumEntry<E> buildEntry(ConfigEntryHolder parent, String name) {
			final EnumEntry<E> entry = new EnumEntry<>(parent, name, value);
			entry.useComboBox = useComboBox;
			return entry;
		}
		
		@Override protected Builder<E> createCopy() {
			final Builder<E> copy = new Builder<>(value);
			copy.useComboBox = useComboBox;
			return copy;
		}
	}
	
	public interface ITranslatedEnum {
		Component getDisplayName();
	}
	
	public String presentName(E value) {
		String name = value.name();
		String lowerCaseName = name.toLowerCase();
		if (lowerCaseNameMap.get(lowerCaseName) == value) name = lowerCaseName;
		return name; //.replace('_', ' ');
	}
	
	public @Nullable E parseName(String name) {
		name = name.trim().replace(' ', '_');
		E exact = nameMap.get(name);
		return exact != null? exact : lowerCaseNameMap.get(name.toLowerCase());
	}
	
	@Override public String forActualConfig(@Nullable E value) {
		if (value == null) return null;
		return presentName(value);
	}
	
	@Override public @Nullable E fromActualConfig(@Nullable Object value) {
		if (!(value instanceof String)) return null;
		return parseName((String) value);
	}
	
	@Override public List<String> getConfigCommentTooltips() {
		List<String> tooltips = super.getConfigCommentTooltips();
		E[] choices = enumClass.getEnumConstants();
		tooltips.add("Options: " + Arrays.stream(choices)
		  .map(c -> "'" + presentName(c) + "'").collect(Collectors.joining(", ")));
		return tooltips;
	}
	
	@Override protected Component getDebugDisplayName() {
		if (translation != null) {
			MutableComponent status =
			  I18n.exists(translation) ? Component.literal("✔ ") : Component.literal("✘ ");
			if (tooltip != null) {
				status = status.append(
				  I18n.exists(tooltip)
				  ? Component.literal("✔ ").withStyle(ChatFormatting.DARK_AQUA)
				  : Component.literal("_ ").withStyle(ChatFormatting.DARK_AQUA));
			}
			boolean correct = defValue instanceof ITranslatedEnum
			                  || Arrays.stream(enumClass.getEnumConstants())
			                    .allMatch(e -> I18n.exists(getEnumTranslationKey(e)));
			status = status.append(
			  correct ? Component.literal("✔ ").withStyle(ChatFormatting.LIGHT_PURPLE)
			          : Component.literal("✘ ").withStyle(ChatFormatting.LIGHT_PURPLE));
			ChatFormatting format =
			  I18n.exists(translation)? correct? ChatFormatting.DARK_GREEN : ChatFormatting.GOLD : ChatFormatting.RED;
			return Component.literal("")
			  .append(status.append(Component.literal(translation)).withStyle(format));
		} else return Component.literal("")
		  .append(Component.literal("⚠ " + name).withStyle(ChatFormatting.DARK_RED));
	}
	
	@OnlyIn(Dist.CLIENT) @Override protected void addTranslationsDebugInfo(List<Component> tooltip) {
		super.addTranslationsDebugInfo(tooltip);
		if (parent != null) {
			if (defValue instanceof ITranslatedEnum)
				tooltip.add(Component.literal(" + Enum provides its own translations").withStyle(
				  ChatFormatting.GRAY));
			tooltip.add(
			  Component.literal(" + Enum translation keys:").withStyle(ChatFormatting.GRAY));
			for (E elem : enumClass.getEnumConstants()) {
				final String key = getEnumTranslationKey(elem);
				final MutableComponent status =
				  I18n.exists(key)
				  ? Component.literal("(✔ present)").withStyle(ChatFormatting.DARK_GREEN)
				  : (defValue instanceof ITranslatedEnum)
				    ? Component.literal("(not present)").withStyle(ChatFormatting.DARK_GRAY)
				    : Component.literal("(✘ missing)").withStyle(ChatFormatting.RED);
				tooltip.add(Component.literal("   > ").withStyle(ChatFormatting.GRAY)
				              .append(Component.literal(key).withStyle(ChatFormatting.DARK_AQUA))
				              .append(" ").append(status));
			}
		}
	}
	
	protected String getEnumTranslationKey(E item) {
		return parent.getRoot().getModId() + ".config.enum." +
		       CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, enumClass.getSimpleName()) +
		       "." + CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_UNDERSCORE, item.name());
	}
	
	@OnlyIn(Dist.CLIENT)
	protected Component enumName(E item) {
		if (item instanceof ITranslatedEnum)
			return ((ITranslatedEnum) item).getDisplayName();
		final String key = getEnumTranslationKey(item);
		// if (debugTranslations()) return new TextComponent(key);
		if (I18n.exists(key))
			return Component.translatable(key);
		return Component.literal(item.name());
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override public Optional<FieldBuilder<E, ?, ?>> buildGUIEntry(
	  ConfigFieldBuilder builder
	) {
		if (useComboBox != null? useComboBox : advanced.prefer_combo_box < enumClass.getEnumConstants().length) {
			final List<E> choices = Lists.newArrayList(enumClass.getEnumConstants());
			final ComboBoxFieldBuilder<E> valBuilder =
			  builder.startComboBox(getDisplayName(), new ChoicesTypeWrapper<>(
				   choices, e -> e.name().toLowerCase(), this::enumName), get()
				 ).setSuggestionMode(false)
				 .setSuggestions(choices);
			return Optional.of(decorate(valBuilder));
		} else {
			final EnumSelectorBuilder<E> valBuilder = builder
			  .startEnumSelector(getDisplayName(), get());
			//noinspection unchecked
			valBuilder.setEnumNameProvider(e -> enumName((E) e));
			return Optional.of(decorate(valBuilder));
		}
	}
	
	@Override public boolean addCommandSuggestions(SuggestionsBuilder builder) {
		super.addCommandSuggestions(builder);
		E current = get();
		E[] values = enumClass.getEnumConstants();
		for (E value: values) if (value != current && value != defValue && isValidValue(value))
			builder.suggest(forCommand(value));
		return true;
	}
	
	public static class ChoicesTypeWrapper<V> implements ITypeWrapper<V> {
		protected List<V> choices;
		protected Function<V, String> nameProvider;
		protected Function<V, Component> formattedNameProvider;
		
		public ChoicesTypeWrapper(
		  List<V> choices, Function<V, String> nameProvider,
		  Function<V, Component> formattedNameProvider
		) {
			this.choices = choices;
			this.nameProvider = nameProvider;
			this.formattedNameProvider = formattedNameProvider;
		}
		
		@Override public Pair<Optional<V>, Optional<Component>> parseElement(@NotNull String text) {
			final Optional<V> opt = choices.stream().filter(c -> text.equals(nameProvider.apply(c))).findFirst();
			Optional<Component> error = opt.isPresent()? Optional.empty() : Optional.of(
			  Component.translatable("simpleconfig.config.error.unknown_value"));
			return Pair.of(opt, error);
		}
		
		@Override public Component getDisplayName(@NotNull V element) {
			return formattedNameProvider.apply(element);
		}
		
		@Override public String getName(@NotNull V element) {
			return nameProvider.apply(element);
		}
	}
}
