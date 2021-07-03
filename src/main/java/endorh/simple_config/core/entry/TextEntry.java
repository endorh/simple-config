package endorh.simple_config.core.entry;

import endorh.simple_config.core.AbstractConfigEntry;
import endorh.simple_config.core.ISimpleConfigEntryHolder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.TextDescriptionBuilder;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class TextEntry extends AbstractConfigEntry<Void, Void, Object, TextEntry> {
	private static final Logger LOGGER = LogManager.getLogger();
	protected @Nullable Supplier<ITextComponent> translationSupplier = null; // Lazy
	protected boolean own = false;
	
	public TextEntry(@Nullable Supplier<ITextComponent> supplier) {
		super(null, Void.class);
		translationSupplier = supplier;
		own = supplier != null;
	}
	
	public TextEntry() {
		super(null, Void.class);
	}
	
	/**
	 * @deprecated Use {@link TextEntry#args(Object...)} instead
	 */
	@Override @Deprecated public TextEntry nameArgs(Object... args) {
		return super.nameArgs(args);
	}
	
	/**
	 * Set the arguments that will be passed to the name translation key<br>
	 * As a special case, {@code Supplier}s passed
	 * will be invoked before being passed as arguments
	 */
	public TextEntry args(Object... args) {
		return nameArgs(args);
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override protected ITextComponent getDisplayName() {
		if (debugTranslations()) {
			return getDebugDisplayName();
		} else if (translationSupplier != null) {
			return translationSupplier.get();
		} else {
			if (translation == null)
				LOGGER.warn("Malformed config text entry " + getPath());
			return super.getDisplayName();
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override protected ITextComponent getDebugDisplayName() {
		if (own) {
			assert translationSupplier != null;
			return new StringTextComponent("").append(
			  translationSupplier.get().copyRaw().mergeStyle(TextFormatting.DARK_AQUA));
		} else if (super.translation != null) {
			IFormattableTextComponent status =
			  I18n.hasKey(super.translation) ? new StringTextComponent("✔ ") : new StringTextComponent("✘ ");
			if (tooltip != null) {
				status = status.append(
				  I18n.hasKey(tooltip)
				  ? new StringTextComponent("✔ ").mergeStyle(TextFormatting.DARK_AQUA)
				  : new StringTextComponent("_ ").mergeStyle(TextFormatting.DARK_AQUA));
			}
			TextFormatting format = I18n.hasKey(super.translation) ? TextFormatting.DARK_GREEN : TextFormatting.RED;
			return new StringTextComponent("")
			  .append(status.append(new StringTextComponent(super.translation)).mergeStyle(format));
		} else return new StringTextComponent("").append(new StringTextComponent("⚠ " + name).mergeStyle(TextFormatting.DARK_RED));
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	protected Optional<ITextComponent[]> supplyDebugTooltip(Object value) {
		List<ITextComponent> lines = new ArrayList<>();
		lines.add(new StringTextComponent("Text entry").mergeStyle(TextFormatting.GRAY));
		if (own) {
			lines.add(new StringTextComponent(" + Provides its own translation")
			            .mergeStyle(TextFormatting.GRAY));
		} else if (super.translation != null) {
			lines.add(new StringTextComponent("Translation key:")
			            .mergeStyle(TextFormatting.GRAY));
			final IFormattableTextComponent status =
			  I18n.hasKey(super.translation)
			  ? new StringTextComponent("(✔ present)").mergeStyle(TextFormatting.DARK_GREEN)
			  : new StringTextComponent("(✘ missing)").mergeStyle(TextFormatting.RED);
			lines.add(new StringTextComponent("   " + super.translation + " ")
			            .mergeStyle(TextFormatting.DARK_AQUA).append(status));
		} else {
			lines.add(new StringTextComponent("Translation key:")
			            .mergeStyle(TextFormatting.GRAY));
			lines.add(new StringTextComponent("   Error: couldn't map translation key")
			            .mergeStyle(TextFormatting.RED));
		}
		if (tooltip != null) {
			if (!name.startsWith("_text$") || I18n.hasKey(tooltip)) {
				lines.add(new StringTextComponent("Tooltip key:")
				            .mergeStyle(TextFormatting.GRAY));
				final IFormattableTextComponent status =
				  I18n.hasKey(tooltip)
				  ? new StringTextComponent("(✔ present)").mergeStyle(TextFormatting.DARK_GREEN)
				  : new StringTextComponent("(not present)").mergeStyle(TextFormatting.GOLD);
				lines.add(new StringTextComponent("   " + tooltip + " ")
				            .mergeStyle(TextFormatting.DARK_AQUA).append(status));
			}
		} else {
			lines.add(new StringTextComponent("Tooltip key:").mergeStyle(TextFormatting.GRAY));
			lines.add(new StringTextComponent("   Error: couldn't map tooltip translation key")
			            .mergeStyle(TextFormatting.RED));
		}
		addTranslationsDebugInfo(lines);
		addTranslationsDebugSuffix(lines);
		return Optional.of(lines.toArray(new ITextComponent[0]));
	}
	
	@Override
	protected Object getGUI(ISimpleConfigEntryHolder c) {
		throw new IllegalArgumentException("Text entries do not have a value");
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public Optional<AbstractConfigListEntry<Object>> buildGUIEntry(
	  ConfigEntryBuilder builder, ISimpleConfigEntryHolder c
	) {
		final ITextComponent displayName = getDisplayName();
		if (displayName != null) {
			final TextDescriptionBuilder valBuilder = builder
			  .startTextDescription(displayName)
			  .setTooltipSupplier(() -> this.supplyTooltip(null));
			return Optional.of(decorate(valBuilder).build());
		} else {
			LOGGER.warn("Malformed config text entry " + getPath());
			return Optional.empty();
		}
	}
}
