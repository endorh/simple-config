package endorh.simpleconfig.core.entry;

import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.TagEntryBuilder;
import endorh.simpleconfig.api.ui.ITextFormatter;
import net.minecraft.nbt.SnbtPrinterTagVisitor;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class TagEntry extends AbstractSerializableEntry<Tag> {
	@Internal public TagEntry(ConfigEntryHolder parent, String name, Tag value) {
		super(parent, name, value, Tag.class);
	}
	
	public static class Builder extends AbstractSerializableEntry.Builder<Tag, TagEntry, TagEntryBuilder, Builder>
	  implements TagEntryBuilder {
		public Builder(Tag value) {
			super(value, Tag.class);
		}
		
		@Override
		protected TagEntry buildEntry(ConfigEntryHolder parent, String name) {
			return new TagEntry(parent, name, value);
		}
		
		@Contract(value="_ -> new", pure=true) @Override protected Builder createCopy(Tag value) {
			return new Builder(value);
		}
	}
	
	@Override
	protected String serialize(Tag value) {
		return new SnbtPrinterTagVisitor("", 0, Lists.newArrayList()).visit(value);
	}
	
	@Override
	protected @Nullable Tag deserialize(String value) {
		try {
			StringReader reader = new StringReader(value);
			Tag tag = new TagParser(reader).readValue();
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
	
	@Override protected ITextFormatter getTextFormatter() {
		return ITextFormatter.forLanguageOrDefault("snbt", ITextFormatter.DEFAULT);
	}
}
