package endorh.simpleconfig.core.entry;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.google.common.collect.Lists;
import endorh.simpleconfig.clothconfig2.api.AbstractConfigListEntry;
import endorh.simpleconfig.clothconfig2.api.ConfigEntryBuilder;
import endorh.simpleconfig.clothconfig2.impl.builders.TextDescriptionBuilder;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.AbstractConfigEntryBuilder;
import endorh.simpleconfig.core.ISimpleConfigEntryHolder;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TextEntry extends AbstractConfigEntry<Void, Void, Void, TextEntry> {
	private static final Logger LOGGER = LogManager.getLogger();
	protected @Nullable Supplier<ITextComponent> translationSupplier = null; // Lazy
	protected boolean own = false;
	protected boolean logged = false;
	
	public TextEntry(ISimpleConfigEntryHolder parent, String name) {
		super(parent, name, null);
		nonPersistent = true;
	}
	
	public static class Builder extends AbstractConfigEntryBuilder<Void, Void, Void, TextEntry, Builder> {
		protected Supplier<ITextComponent> translationSupplier = null;
		
		public Builder() {
			super(null, null);
		}
		
		public Builder(@Nullable Supplier<ITextComponent> supplier) {
			this();
			translationSupplier = supplier;
		}
		
		public Builder text(Supplier<ITextComponent> supplier) {
			Builder copy = copy();
			copy.translationSupplier = supplier;
			return copy;
		}
		
		public Builder text(ITextComponent text) {
			return text(() -> text);
		}
		
		
		/**
		 * @deprecated Use {@link Builder#args(Object...)} instead
		 */
		@Override @Deprecated public Builder nameArgs(Object... args) {
			return super.nameArgs(args);
		}
		
		/**
		 * Set the arguments that will be passed to the name translation key<br>
		 * As a special case, {@code Supplier}s passed
		 * will be invoked before being passed as arguments
		 */
		public Builder args(Object... args) {
			return nameArgs(args);
		}
		
		@Override
		protected TextEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			nonPersistent = true;
			final TextEntry e = new TextEntry(parent, name);
			e.translationSupplier = translationSupplier;
			e.own = translationSupplier != null;
			return e;
		}
		
		@Override protected Builder createCopy() {
			final Builder copy = new Builder();
			copy.translationSupplier = translationSupplier;
			return copy;
		}
	}
	
	@Override protected Consumer<Void> createSaveConsumer() {
		return v -> {};
	}
	
	@Override protected void buildSpec(ConfigSpec spec, String parentPath) {}
	
	@OnlyIn(Dist.CLIENT)
	@Override protected ITextComponent getDisplayName() {
		if (displayName != null) return displayName;
		if (debugTranslations()) {
			return nullAsEmpty(getDebugDisplayName());
		} else if (translationSupplier != null) {
			return nullAsEmpty(translationSupplier.get());
		} else {
			if (translation == null)
				LOGGER.warn("Malformed config text entry " + getGlobalPath());
			return nullAsEmpty(super.getDisplayName());
		}
	}
	
	protected ITextComponent nullAsEmpty(@Nullable ITextComponent text) {
		if (!logged && text == null) {
			LOGGER.warn("Malformed config text entry " + getGlobalPath());
			logged = true;
		}
		return text != null? text : StringTextComponent.EMPTY;
	}
	
	@Override protected List<ITextComponent> addExtraTooltip(Void value) {
		return Lists.newArrayList();
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override protected ITextComponent getDebugDisplayName() {
		if (own) {
			assert translationSupplier != null;
			return new StringTextComponent("").append(
			  translationSupplier.get().plainCopy().withStyle(TextFormatting.DARK_AQUA));
		} else if (translation != null) {
			IFormattableTextComponent status =
			  I18n.exists(translation) ? new StringTextComponent("✔ ") : new StringTextComponent("✘ ");
			if (tooltip != null) {
				status = status.append(
				  I18n.exists(tooltip)
				  ? new StringTextComponent("✔ ").withStyle(TextFormatting.DARK_AQUA)
				  : new StringTextComponent("_ ").withStyle(TextFormatting.DARK_AQUA));
			}
			TextFormatting format = I18n.exists(translation) ? TextFormatting.DARK_GREEN : TextFormatting.RED;
			return new StringTextComponent("")
			  .append(status.append(new StringTextComponent(translation)).withStyle(format));
		} else return new StringTextComponent("").append(new StringTextComponent("⚠ " + name).withStyle(TextFormatting.DARK_RED));
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override protected Optional<ITextComponent[]> supplyDebugTooltip(Void value) {
		List<ITextComponent> lines = new ArrayList<>();
		lines.add(new StringTextComponent("Text entry").withStyle(TextFormatting.GRAY));
		if (own) {
			lines.add(new StringTextComponent(" + Provides its own translation")
			            .withStyle(TextFormatting.GRAY));
		} else if (translation != null) {
			lines.add(new StringTextComponent("Translation key:")
			            .withStyle(TextFormatting.GRAY));
			final IFormattableTextComponent status =
			  I18n.exists(translation)
			  ? new StringTextComponent("(✔ present)").withStyle(TextFormatting.DARK_GREEN)
			  : new StringTextComponent("(✘ missing)").withStyle(TextFormatting.RED);
			lines.add(new StringTextComponent("   " + translation + " ")
			            .withStyle(TextFormatting.DARK_AQUA).append(status));
		} else {
			lines.add(new StringTextComponent("Translation key:")
			            .withStyle(TextFormatting.GRAY));
			lines.add(new StringTextComponent("   Error: couldn't map translation key")
			            .withStyle(TextFormatting.RED));
		}
		if (tooltip != null) {
			if (!name.startsWith("_text$") || I18n.exists(tooltip)) {
				lines.add(new StringTextComponent("Tooltip key:")
				            .withStyle(TextFormatting.GRAY));
				final IFormattableTextComponent status =
				  I18n.exists(tooltip)
				  ? new StringTextComponent("(✔ present)").withStyle(TextFormatting.DARK_GREEN)
				  : new StringTextComponent("(not present)").withStyle(TextFormatting.GOLD);
				lines.add(new StringTextComponent("   " + tooltip + " ")
				            .withStyle(TextFormatting.DARK_AQUA).append(status));
			}
		} else {
			lines.add(new StringTextComponent("Tooltip key:").withStyle(TextFormatting.GRAY));
			lines.add(new StringTextComponent("   Error: couldn't map tooltip translation key")
			            .withStyle(TextFormatting.RED));
		}
		addTranslationsDebugInfo(lines);
		addTranslationsDebugSuffix(lines);
		return Optional.of(lines.toArray(new ITextComponent[0]));
	}
	
	@Override protected Void getGUI() {
		throw new IllegalArgumentException("Text entries do not have a value");
	}
	
	@Override protected void put(CommentedConfig config, Void value) {}
	
	@Override protected Void get(CommentedConfig config) {
		return null;
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override public Optional<AbstractConfigListEntry<Void>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		final TextDescriptionBuilder valBuilder = builder
		  .startTextDescription(this::getDisplayName)
		  .setTooltipSupplier(() -> this.getTooltip(null));
		return Optional.of(decorate(valBuilder).build());
	}
}
