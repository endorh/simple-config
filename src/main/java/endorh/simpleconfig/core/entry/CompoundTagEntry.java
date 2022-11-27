package endorh.simpleconfig.core.entry;

import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.CompoundTagEntryBuilder;
import endorh.simpleconfig.api.ui.TextFormatter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.SnbtPrinterTagVisitor;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class CompoundTagEntry extends AbstractSerializableEntry<CompoundTag> {
	@Internal public CompoundTagEntry(ConfigEntryHolder parent, String name, CompoundTag value) {
		super(parent, name, value, CompoundTag.class);
	}
	
	public static class Builder extends AbstractSerializableEntry.Builder<CompoundTag,
	  CompoundTagEntry, CompoundTagEntryBuilder, Builder>
	  implements CompoundTagEntryBuilder {
		public Builder(CompoundTag value) {
			super(value, CompoundTag.class);
		}
		
		@Override
		protected CompoundTagEntry buildEntry(ConfigEntryHolder parent, String name) {
			return new CompoundTagEntry(parent, name, value);
		}
		
		@Override protected Builder createCopy(CompoundTag value) {
			return new Builder(value);
		}
	}
	
	@Override protected String serialize(CompoundTag value) {
		return new SnbtPrinterTagVisitor("", 0, Lists.newArrayList()).visit(value);
	}
	
	@Override protected @Nullable CompoundTag deserialize(String value) {
		try {
			StringReader reader = new StringReader(value);
			CompoundTag tag = new TagParser(reader).readStruct();
			reader.skipWhitespace();
			return reader.canRead()? null : tag;
		} catch (CommandSyntaxException ignored) {
			return null;
		}
	}
	
	@Override
	protected Optional<Component> getErrorMessage(String value) {
		return Optional.of(Component.translatable("simpleconfig.config.error.invalid_nbt"));
	}
	
	@Override protected TextFormatter getTextFormatter() {
		return TextFormatter.forLanguageOrDefault("snbt", TextFormatter.DEFAULT);
	}
}

