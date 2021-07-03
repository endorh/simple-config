package endorh.simple_config.core.entry;

import com.google.common.base.CaseFormat;
import endorh.simple_config.core.AbstractConfigEntry;
import endorh.simple_config.core.ISimpleConfigEntryHolder;
import endorh.simple_config.core.SimpleConfig;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.EnumSelectorBuilder;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class EnumEntry<E extends Enum<E>>
  extends AbstractConfigEntry<E, E, E, EnumEntry<E>> {
	public Class<E> enumClass;
	
	public interface ITranslatedEnum {
		ITextComponent getDisplayName();
	}
	
	public EnumEntry(E value) {
		super(value, value.getDeclaringClass());
		enumClass = value.getDeclaringClass();
	}
	
	@Override
	protected Optional<ConfigValue<?>> buildConfigEntry(Builder builder) {
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
			                    .allMatch(e -> I18n.hasKey(getEnumTranslationKey(e, parent.getRoot())));
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
				final String key = getEnumTranslationKey(elem, parent.getRoot());
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
	
	protected String getEnumTranslationKey(E item, SimpleConfig config) {
		return config.modId + ".config.enum." +
		       CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, enumClass.getSimpleName()) +
		       "." + CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_UNDERSCORE, item.name());
	}
	
	@OnlyIn(Dist.CLIENT)
	protected ITextComponent enumName(E item, SimpleConfig config) {
		if (item instanceof ITranslatedEnum)
			return ((ITranslatedEnum) item).getDisplayName();
		final String key = getEnumTranslationKey(item, config);
		// if (debugTranslations()) return new StringTextComponent(key);
		if (I18n.hasKey(key))
			return new TranslationTextComponent(key);
		return new StringTextComponent(item.name());
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	protected Optional<AbstractConfigListEntry<E>> buildGUIEntry(
	  ConfigEntryBuilder builder, ISimpleConfigEntryHolder c
	) {
		final EnumSelectorBuilder<E> valBuilder = builder
		  .startEnumSelector(getDisplayName(), enumClass, c.get(name))
		  .setDefaultValue(value)
		  .setSaveConsumer(saveConsumer(c))
		  .setTooltipSupplier(this::supplyTooltip)
		  .setErrorSupplier(this::supplyError);
		//noinspection unchecked
		valBuilder.setEnumNameProvider(e -> enumName((E) e, c.getRoot()));
		return Optional.of(decorate(valBuilder).build());
	}
}
