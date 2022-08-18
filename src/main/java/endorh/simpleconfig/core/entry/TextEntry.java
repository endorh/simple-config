package endorh.simpleconfig.core.entry;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.google.common.collect.Lists;
import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.TextEntryBuilder;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.AbstractConfigEntryBuilder;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import endorh.simpleconfig.ui.impl.builders.TextDescriptionBuilder;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TextEntry extends AbstractConfigEntry<Void, Void, Void> {
	private static final Logger LOGGER = LogManager.getLogger();
	protected @Nullable Supplier<ITextComponent> translationSupplier = null; // Lazy
	protected boolean own = false;
	protected boolean logged = false;
	
	public TextEntry(ConfigEntryHolder parent, String name) {
		super(parent, name, null);
		nonPersistent = true;
	}
	
	public static class Builder extends AbstractConfigEntryBuilder<Void, Void, Void, TextEntry, TextEntryBuilder, Builder>
	  implements TextEntryBuilder {
		protected Supplier<ITextComponent> translationSupplier = null;
		
		public Builder() {
			super(null, null);
		}
		
		public Builder(@Nullable Supplier<ITextComponent> supplier) {
			this();
			translationSupplier = supplier;
		}
		
		@Override @Contract(pure=true) public Builder text(Supplier<ITextComponent> supplier) {
			Builder copy = copy();
			copy.translationSupplier = supplier;
			return copy;
		}
		
		@Override @Contract(pure=true) public Builder text(ITextComponent text) {
			return text(() -> text);
		}
		
		
		@Contract(pure=true) @Override @Deprecated public TextEntryBuilder nameArgs(Object... args) {
			return super.nameArgs(args);
		}
		
		@Override @Contract(pure=true) public TextEntryBuilder args(Object... args) {
			return nameArgs(args);
		}
		
		@Override
		protected TextEntry buildEntry(ConfigEntryHolder parent, String name) {
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
	
	@Override public Optional<ITextComponent> getErrorFromGUI(Void value) {
		return Optional.empty();
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
	@Override protected Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
		return Optional.empty();
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override protected ITextComponent getDebugDisplayName() {
		if (own) {
			assert translationSupplier != null;
			return new StringTextComponent("").append(
			  translationSupplier.get().copyRaw().mergeStyle(TextFormatting.DARK_AQUA));
		} else if (translation != null) {
			IFormattableTextComponent status =
			  I18n.hasKey(translation) ? new StringTextComponent("✔ ") : new StringTextComponent("✘ ");
			if (tooltip != null) {
				status = status.append(
				  I18n.hasKey(tooltip)
				  ? new StringTextComponent("✔ ").mergeStyle(TextFormatting.DARK_AQUA)
				  : new StringTextComponent("_ ").mergeStyle(TextFormatting.DARK_AQUA));
			}
			TextFormatting format = I18n.hasKey(translation) ? TextFormatting.DARK_GREEN : TextFormatting.RED;
			return new StringTextComponent("")
			  .append(status.append(new StringTextComponent(translation)).mergeStyle(format));
		} else return new StringTextComponent("").append(new StringTextComponent("⚠ " + name).mergeStyle(TextFormatting.DARK_RED));
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override protected Optional<ITextComponent[]> supplyDebugTooltip(Void value) {
		List<ITextComponent> lines = new ArrayList<>();
		lines.add(new StringTextComponent("Text entry").mergeStyle(TextFormatting.GRAY));
		if (own) {
			lines.add(new StringTextComponent(" + Provides its own translation")
			            .mergeStyle(TextFormatting.GRAY));
		} else if (translation != null) {
			lines.add(new StringTextComponent("Translation key:")
			            .mergeStyle(TextFormatting.GRAY));
			final IFormattableTextComponent status =
			  I18n.hasKey(translation)
			  ? new StringTextComponent("(✔ present)").mergeStyle(TextFormatting.DARK_GREEN)
			  : new StringTextComponent("(✘ missing)").mergeStyle(TextFormatting.RED);
			lines.add(new StringTextComponent("   " + translation + " ")
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
	
	@Override protected Void getGUI() {
		throw new IllegalArgumentException("Text entries do not have a value");
	}
	
	@Override protected void put(CommentedConfig config, Void value) {}
	
	@Override protected Void get(CommentedConfig config) {
		return null;
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override public Optional<FieldBuilder<Void, ?, ?>> buildGUIEntry(
	  ConfigFieldBuilder builder
	) {
		final TextDescriptionBuilder valBuilder = builder
		  .startTextDescription(this::getDisplayName)
		  .setTooltipSupplier(() -> getTooltip(null));
		return Optional.of(decorate(valBuilder));
	}
}
