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
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
	protected @Nullable Supplier<Component> translationSupplier = null; // Lazy
	protected boolean own = false;
	protected boolean logged = false;
	
	public TextEntry(ConfigEntryHolder parent, String name) {
		super(parent, name, null);
		nonPersistent = true;
	}
	
	public static class Builder extends AbstractConfigEntryBuilder<Void, Void, Void, TextEntry, TextEntryBuilder, Builder>
	  implements TextEntryBuilder {
		protected Supplier<Component> translationSupplier = null;
		
		public Builder() {
			super(null, null);
		}
		
		public Builder(@Nullable Supplier<Component> supplier) {
			this();
			translationSupplier = supplier;
		}
		
		@Override @Contract(pure=true) public Builder text(Supplier<Component> supplier) {
			Builder copy = copy();
			copy.translationSupplier = supplier;
			return copy;
		}
		
		@Override @Contract(pure=true) public Builder text(Component text) {
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
	
	@Override public Optional<Component> getErrorFromGUI(Void value) {
		return Optional.empty();
	}
	
	@Override protected Consumer<Void> createSaveConsumer() {
		return v -> {};
	}
	
	@Override protected void buildSpec(ConfigSpec spec, String parentPath) {}
	
	@OnlyIn(Dist.CLIENT)
	@Override protected Component getDisplayName() {
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
	
	protected Component nullAsEmpty(@Nullable Component text) {
		if (!logged && text == null) {
			LOGGER.warn("Malformed config text entry " + getGlobalPath());
			logged = true;
		}
		return text != null? text : Component.empty();
	}
	
	@Override protected List<Component> addExtraTooltip(Void value) {
		return Lists.newArrayList();
	}
	@Override protected Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
		return Optional.empty();
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override protected Component getDebugDisplayName() {
		if (own) {
			assert translationSupplier != null;
			return Component.literal("").append(
			  translationSupplier.get().plainCopy().withStyle(ChatFormatting.DARK_AQUA));
		} else if (translation != null) {
			MutableComponent status =
			  I18n.exists(translation) ? Component.literal("✔ ") : Component.literal("✘ ");
			if (tooltip != null) {
				status = status.append(
				  I18n.exists(tooltip)
				  ? Component.literal("✔ ").withStyle(ChatFormatting.DARK_AQUA)
				  : Component.literal("_ ").withStyle(ChatFormatting.DARK_AQUA));
			}
			ChatFormatting format = I18n.exists(translation) ? ChatFormatting.DARK_GREEN : ChatFormatting.RED;
			return Component.literal("")
			  .append(status.append(Component.literal(translation)).withStyle(format));
		} else return Component.literal("").append(Component.literal("⚠ " + name).withStyle(ChatFormatting.DARK_RED));
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override protected Optional<Component[]> supplyDebugTooltip(Void value) {
		List<Component> lines = new ArrayList<>();
		lines.add(Component.literal("Text entry").withStyle(ChatFormatting.GRAY));
		if (own) {
			lines.add(Component.literal(" + Provides its own translation")
			            .withStyle(ChatFormatting.GRAY));
		} else if (translation != null) {
			lines.add(Component.literal("Translation key:")
			            .withStyle(ChatFormatting.GRAY));
			final MutableComponent status =
			  I18n.exists(translation)
			  ? Component.literal("(✔ present)").withStyle(ChatFormatting.DARK_GREEN)
			  : Component.literal("(✘ missing)").withStyle(ChatFormatting.RED);
			lines.add(Component.literal("   " + translation + " ")
			            .withStyle(ChatFormatting.DARK_AQUA).append(status));
		} else {
			lines.add(Component.literal("Translation key:")
			            .withStyle(ChatFormatting.GRAY));
			lines.add(Component.literal("   Error: couldn't map translation key")
			            .withStyle(ChatFormatting.RED));
		}
		if (tooltip != null) {
			if (!name.startsWith("_text$") || I18n.exists(tooltip)) {
				lines.add(Component.literal("Tooltip key:")
				            .withStyle(ChatFormatting.GRAY));
				final MutableComponent status =
				  I18n.exists(tooltip)
				  ? Component.literal("(✔ present)").withStyle(ChatFormatting.DARK_GREEN)
				  : Component.literal("(not present)").withStyle(ChatFormatting.GOLD);
				lines.add(Component.literal("   " + tooltip + " ")
				            .withStyle(ChatFormatting.DARK_AQUA).append(status));
			}
		} else {
			lines.add(Component.literal("Tooltip key:").withStyle(ChatFormatting.GRAY));
			lines.add(Component.literal("   Error: couldn't map tooltip translation key")
			            .withStyle(ChatFormatting.RED));
		}
		addTranslationsDebugInfo(lines);
		addTranslationsDebugSuffix(lines);
		return Optional.of(lines.toArray(new Component[0]));
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
