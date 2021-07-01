package dnj.simple_config.core.entry;

import dnj.simple_config.core.ISimpleConfigEntryHolder;
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

public class TextEntry extends EmptyEntry {
	private static final Logger LOGGER = LogManager.getLogger();
	protected @Nullable Supplier<ITextComponent> translation = null; // Lazy
	protected boolean own = false;
	
	public TextEntry(@Nullable Supplier<ITextComponent> supplier) {
		this.translation = supplier;
		own = supplier != null;
	}
	
	public TextEntry() {
		if (super.translation != null)
			this.translation = () -> new TranslationTextComponent(super.translation);
	}
	
	@Override
	protected EmptyEntry translate(String translation) {
		if (this.translation == null) {
			super.translate(translation);
			if (super.translation != null)
				this.translation = () -> new TranslationTextComponent(super.translation);
		}
		return this;
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override protected @Nullable ITextComponent getDisplayName() {
		if (debugTranslations()) {
			return getDebugDisplayName();
		} else if (translation != null) {
			return translation.get();
		} else {
			LOGGER.warn("Malformed text entry in config with name " + name);
			return null;
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override protected ITextComponent getDebugDisplayName() {
		if (own) {
			assert translation != null;
			return new StringTextComponent("").append(
			  translation.get().copyRaw().mergeStyle(TextFormatting.DARK_AQUA));
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
	protected Optional<ITextComponent[]> supplyDebugTooltip(Void value) {
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
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public Optional<AbstractConfigListEntry<?>> buildGUIEntry(
	  ConfigEntryBuilder builder, ISimpleConfigEntryHolder c
	) {
		final ITextComponent displayName = getDisplayName();
		if (displayName != null) {
			final TextDescriptionBuilder valBuilder = builder
			  .startTextDescription(displayName)
			  .setTooltipSupplier(() -> this.supplyTooltip(null));
			return Optional.of(decorate(valBuilder).build());
		} else {
			LOGGER.warn("Malformed text entry in config with name " + name);
			return Optional.empty();
		}
	}
}
