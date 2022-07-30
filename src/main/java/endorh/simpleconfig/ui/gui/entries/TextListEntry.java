package endorh.simpleconfig.ui.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.ui.api.INavigableTarget;
import endorh.simpleconfig.ui.gui.AbstractConfigScreen;
import endorh.simpleconfig.ui.gui.widget.ResetButton;
import endorh.simpleconfig.ui.hotkey.HotKeyActionType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
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
	protected FontRenderer font;
	protected int color;
	protected Supplier<ITextComponent> textSupplier;
	protected ITextComponent savedText;
	protected int savedWidth;
	protected int savedX;
	protected int savedY;
	private List<IReorderingProcessor> wrappedLines;
	
	@Internal public TextListEntry(
	  ITextComponent fieldName, Supplier<ITextComponent> textSupplier, int color
	) {
		super(fieldName);
		font = Minecraft.getInstance().fontRenderer;
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
	  MatrixStack mStack, ITextComponent title, float textX, int index, int x, int y, int entryWidth,
	  int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta
	) {
		final ITextComponent text = getText();
		if (savedWidth != entryWidth || savedX != x || savedY != y
		    || !Objects.equals(text, savedText)) {
			savedText = text;
			wrappedLines =
			  font.trimStringToWidth(text, entryWidth);
			savedWidth = entryWidth;
			savedX = x;
			savedY = y;
		}
		int yy = y + 4;
		int lineHeight = getLineHeightWithMargin();
		for (IReorderingProcessor string : wrappedLines) {
			Minecraft.getInstance().fontRenderer.func_238407_a_(
			  mStack, string, (float) x, (float) yy, color);
			Objects.requireNonNull(Minecraft.getInstance().fontRenderer);
			yy += lineHeight;
		}
		Style style = getTextAt(mouseX, mouseY);
		AbstractConfigScreen configScreen = getScreen();
		if (style != null)
			configScreen.renderComponentHoverEffect(mStack, style, mouseX, mouseY);
	}
	
	public int getLineHeight() {
		return Minecraft.getInstance().fontRenderer.FONT_HEIGHT;
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
			int textX = MathHelper.floor(x - (double) savedX);
			int textY = MathHelper.floor(y - 4.0 - (double) savedY);
			int lineHeight = getLineHeightWithMargin();
			if (textX >= 0 && textY >= 0 && textX <= savedWidth &&
			    textY < lineHeight * lineCount + lineCount && (line = textY / lineHeight) < wrappedLines.size()) {
				IReorderingProcessor orderedText = wrappedLines.get(line);
				return font.getCharacterManager().func_243239_a(orderedText, textX);
			}
		}
		return null;
	}
	
	public ITextComponent getText() {
		return textSupplier.get();
		// if (matchedText == null || matchedText.isEmpty())
		// 	return text;
		// final String str = text.getString();
		// final int index = str.indexOf(matchedText);
		// if (index == -1)
		// 	return text;
		// // TODO: Modify style without rewriting
		// return new StringTextComponent(str.substring(0, index))
		//   .append(new StringTextComponent(str.substring(index, index + matchedText.length()))
		//             .mergeStyle(focusedMatch? TextFormatting.GOLD : TextFormatting.YELLOW)
		//             .mergeStyle(TextFormatting.UNDERLINE))
		//   .appendString(str.substring(index + matchedText.length()));
	}
	
	@Override public boolean isSelectable() {
		return false;
	}
	
	@Override public int getCaptionHeight() {
		return getItemHeight() - 4;
	}
	
	@Override protected @NotNull List<? extends IGuiEventListener> getEntryListeners() {
		return Collections.emptyList();
	}
	
	@Override public String seekableText() {
		return getUnformattedString(getText());
	}
	
	@Override public boolean isNavigable() {
		return false;
	}
	
	@Override public List<INavigableTarget> getNavigableChildren() {
		return Lists.newArrayList();
	}
}

