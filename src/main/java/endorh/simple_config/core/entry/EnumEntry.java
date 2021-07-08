package endorh.simple_config.core.entry;

import com.google.common.base.CaseFormat;
import endorh.simple_config.core.AbstractConfigEntry;
import endorh.simple_config.core.AbstractConfigEntryBuilder;
import endorh.simple_config.core.ISimpleConfigEntryHolder;
import endorh.simple_config.core.IStringKeyEntry;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.EnumSelectorBuilder;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class EnumEntry<E extends Enum<E>>
  extends AbstractConfigEntry<E, E, E, EnumEntry<E>>
  implements IStringKeyEntry<E> {
	protected final Class<E> enumClass;
	
	@Internal public EnumEntry(ISimpleConfigEntryHolder parent, String name, E value) {
		super(parent, name, value);
		enumClass = value.getDeclaringClass();
	}
	
	public static class Builder<E extends Enum<E>> extends AbstractConfigEntryBuilder<E, E, E, EnumEntry<E>, Builder<E>> {
		protected final Class<E> enumClass;
		
		public Builder(E value) {
			super(value, value.getDeclaringClass());
			enumClass = value.getDeclaringClass();
		}
		
		@Override
		protected EnumEntry<E> buildEntry(ISimpleConfigEntryHolder parent, String name) {
			return new EnumEntry<>(parent, name, value);
		}
	}
	
	public interface ITranslatedEnum {
		ITextComponent getDisplayName();
	}
	
	@Override
	protected Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
		return Optional.of(decorate(builder).defineEnum(name, value, configValidator()));
	}
	
	@Override
	protected ITextComponent getDebugDisplayName() {
		if (translation != null) {
			IFormattableTextComponent status =
			  I18n.hasKey(translation) ? new StringTextComponent("✔ ") : new StringTextComponent("✘ ");
			if (tooltip != null) {
				status = status.append(
				  I18n.hasKey(tooltip)
				  ? new StringTextComponent("✔ ").mergeStyle(TextFormatting.DARK_AQUA)
				  : new StringTextComponent("_ ").mergeStyle(TextFormatting.DARK_AQUA));
			}
			boolean correct = value instanceof ITranslatedEnum
			                  || Arrays.stream(enumClass.getEnumConstants())
			                    .allMatch(e -> I18n.hasKey(getEnumTranslationKey(e)));
			status = status.append(
			  correct ? new StringTextComponent("✔ ").mergeStyle(TextFormatting.LIGHT_PURPLE)
			          : new StringTextComponent("✘ ").mergeStyle(TextFormatting.LIGHT_PURPLE));
			TextFormatting format =
			  I18n.hasKey(translation)? correct? TextFormatting.DARK_GREEN : TextFormatting.GOLD : TextFormatting.RED;
			return new StringTextComponent("").append(status.append(new StringTextComponent(translation)).mergeStyle(format));
		} else return new StringTextComponent("").append(new StringTextComponent("⚠ " + name).mergeStyle(TextFormatting.DARK_RED));
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	protected void addTranslationsDebugInfo(List<ITextComponent> tooltip) {
		super.addTranslationsDebugInfo(tooltip);
		if (parent != null) {
			if (value instanceof ITranslatedEnum)
				tooltip.add(new StringTextComponent(" + Enum provides its own translations").mergeStyle(
				  TextFormatting.GRAY));
			tooltip.add(
			  new StringTextComponent(" + Enum translation keys:").mergeStyle(TextFormatting.GRAY));
			for (E elem : enumClass.getEnumConstants()) {
				final String key = getEnumTranslationKey(elem);
				final IFormattableTextComponent status =
				  I18n.hasKey(key)
				  ? new StringTextComponent("(✔ present)").mergeStyle(TextFormatting.DARK_GREEN)
				  : (value instanceof ITranslatedEnum)
				    ? new StringTextComponent("(not present)").mergeStyle(TextFormatting.DARK_GRAY)
				    : new StringTextComponent("(✘ missing)").mergeStyle(TextFormatting.RED);
				tooltip.add(new StringTextComponent("   > ").mergeStyle(TextFormatting.GRAY)
				              .append(new StringTextComponent(key).mergeStyle(TextFormatting.DARK_AQUA))
				              .appendString(" ").append(status));
			}
		}
	}
	
	protected String getEnumTranslationKey(E item) {
		return parent.getRoot().modId + ".config.enum." +
		       CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, enumClass.getSimpleName()) +
		       "." + CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_UNDERSCORE, item.name());
	}
	
	@OnlyIn(Dist.CLIENT)
	protected ITextComponent enumName(E item) {
		if (item instanceof ITranslatedEnum)
			return ((ITranslatedEnum) item).getDisplayName();
		final String key = getEnumTranslationKey(item);
		// if (debugTranslations()) return new StringTextComponent(key);
		if (I18n.hasKey(key))
			return new TranslationTextComponent(key);
		return new StringTextComponent(item.name());
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	protected Optional<AbstractConfigListEntry<E>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		final EnumSelectorBuilder<E> valBuilder = builder
		  .startEnumSelector(getDisplayName(), enumClass, get())
		  .setDefaultValue(value)
		  .setSaveConsumer(saveConsumer())
		  .setTooltipSupplier(this::supplyTooltip)
		  .setErrorSupplier(this::supplyError);
		//noinspection unchecked
		valBuilder.setEnumNameProvider(e -> enumName((E) e));
		return Optional.of(decorate(valBuilder).build());
	}
	
	@Override
	public ITextComponent getKeySerializationError(String key) {
		return new TranslationTextComponent(
		  "simple-config.config.error.invalid_enum",
		  Arrays.stream(enumClass.getEnumConstants()).map(Enum::name)
		    .collect(Collectors.joining(", ")));
	}
	
	@Override
	public Optional<E> deserializeStringKey(String key) {
		return Arrays.stream(enumClass.getEnumConstants())
		  .filter(e -> e.name().equals(key)).findFirst();
	}
}
