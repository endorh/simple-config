package endorh.simpleconfig.ui.gui.entries;

import com.mojang.blaze3d.vertex.PoseStack;
import endorh.simpleconfig.api.SimpleConfigTextUtil;
import endorh.simpleconfig.ui.api.EntryError;
import endorh.simpleconfig.ui.gui.AbstractConfigScreen;
import endorh.simpleconfig.ui.gui.widget.ResetButton;
import endorh.simpleconfig.ui.hotkey.HotKeyActionType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public class TextListEntry extends TooltipListEntry<Void> {
	protected Font font;
	protected int color;
	protected Supplier<Component> textSupplier;
	protected Component savedText;
	protected int savedWidth;
	protected int savedX;
	protected int savedY;
	private List<FormattedCharSequence> wrappedLines;
	
	@Internal public TextListEntry(
	  Component fieldName, Supplier<Component> textSupplier, int color
	) {
		super(fieldName);
		font = Minecraft.getInstance().font;
		savedWidth = -1;
		savedX = -1;
		savedY = -1;
		this.textSupplier = textSupplier;
		this.savedText = getText();
		this.color = color;
		wrappedLines = Collections.emptyList();
	}
	
	@Override public @Nullable ResetButton getResetButton() {
		return null;
	}
	
	@Override public List<HotKeyActionType<Void, ?>> getHotKeyActionTypes() {
		return Collections.emptyList();
	}
	
	@Override protected boolean shouldUseHelpButton() {
		return false;
	}
	
	@Override protected void renderTitle(
	  PoseStack mStack, Component title, float textX, int index, int x, int y, int entryWidth,
	  int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta
	) {
		final Component text = getText();
		if (savedWidth != entryWidth || savedX != x || savedY != y
		    || !Objects.equals(text, savedText)) {
			savedText = text;
			wrappedLines =
			  font.split(text, entryWidth);
			savedWidth = entryWidth;
			savedX = x;
			savedY = y;
		}
		int yy = y + 4;
		int lineHeight = getLineHeightWithMargin();
		for (FormattedCharSequence string : wrappedLines) {
			Minecraft.getInstance().font.drawShadow(
			  mStack, string, (float) x, (float) yy, color);
			Objects.requireNonNull(Minecraft.getInstance().font);
			yy += lineHeight;
		}
		Style style = getTextAt(mouseX, mouseY);
		AbstractConfigScreen configScreen = getScreen();
		if (style != null)
			configScreen.renderComponentHoverEffect(mStack, style, mouseX, mouseY);
	}
	
	public int getLineHeight() {
		return Minecraft.getInstance().font.lineHeight;
	}
	
	public int getLineHeightWithMargin() {
		return getLineHeight() + 3;
	}
	
	@Override public int getItemHeight() {
		int lh = getLineHeightWithMargin();
		if (savedWidth == -1) return lh;
		int lineCount = wrappedLines.size();
		return lineCount == 0 ? 0 : 15 + lineCount * lh;
	}
	
	@Override public boolean onMouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			Style style = getTextAt(mouseX, mouseY);
			AbstractConfigScreen configScreen = getScreen();
			if (configScreen.handleComponentClicked(style))
				return true;
		}
		return super.onMouseClicked(mouseX, mouseY, button);
	}
	
	@Override public int getFieldHeight() {
		return getItemHeight() - 3;
	}
	
	@Nullable private Style getTextAt(double x, double y) {
		int lineCount = wrappedLines.size();
		if (lineCount > 0) {
			int line;
			int textX = Mth.floor(x - (double) savedX);
			int textY = Mth.floor(y - 4.0 - (double) savedY);
			int lineHeight = getLineHeightWithMargin();
			if (textX >= 0 && textY >= 0 && textX <= savedWidth &&
			    textY < lineHeight * lineCount + lineCount && (line = textY / lineHeight) < wrappedLines.size()) {
				FormattedCharSequence orderedText = wrappedLines.get(line);
				return font.getSplitter().componentStyleAtWidth(orderedText, textX);
			}
		}
		return null;
	}
	
	public Component getText() {
		Component text = textSupplier.get();
		if (matchedText == null || matchedText.isEmpty())
			return text;
		String str = text.getString();
		int index = str.indexOf(matchedText);
		if (index == -1) return text;
		return SimpleConfigTextUtil.applyStyle(
		  text.copy(), isFocusedMatch()? ChatFormatting.GOLD : ChatFormatting.YELLOW,
		  index, index + matchedText.length());
	}
	
	@Override protected List<EntryError> computeErrors() {
		return Collections.emptyList();
	}
	
	@Override public boolean isSelectable() {
		return false;
	}
	
	@Override public int getCaptionHeight() {
		return getItemHeight() - 4;
	}
	
	@Override protected @NotNull List<? extends GuiEventListener> getEntryListeners() {
		return Collections.emptyList();
	}
	
	@Override public String seekableText() {
		return getUnformattedString(getText());
	}
	
	@Override public boolean isNavigable() {
		return false;
	}
}

