package endorh.simple_config.clothconfig2.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simple_config.clothconfig2.gui.widget.CheckboxButton;
import endorh.simple_config.clothconfig2.gui.widget.TintedButton;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.Math.min;
import static java.lang.Math.round;
import static net.minecraft.util.math.MathHelper.clamp;

public class ConfirmDialog extends AbstractButtonDialog {
	protected List<ITextComponent> body;
	protected int bodyColor = 0xffbdbdbd;
	protected int lineHeight = 10;
	protected int paragraphMarginDown = 4;
	protected List<List<IReorderingProcessor>> lines;
	protected TintedButton cancelButton;
	protected TintedButton confirmButton;
	protected BiConsumer<Boolean, boolean[]> action;
	
	protected CheckboxButton[] checkBoxes;
	
	public ConfirmDialog(
	  IOverlayCapableScreen screen, ITextComponent title, Consumer<Boolean> action,
	  List<ITextComponent> body, CheckboxButton... checkBoxes
	) { this(screen, title, (v, s) -> action.accept(v), body, checkBoxes); }
	
	public ConfirmDialog(
	  IOverlayCapableScreen screen, ITextComponent title, BiConsumer<Boolean, boolean[]> action,
	  List<ITextComponent> body, CheckboxButton... checkBoxes
	) {
		this(screen, title, action, body, DialogTexts.GUI_CANCEL, DialogTexts.GUI_PROCEED, checkBoxes);
	}
	
	public ConfirmDialog(
	  IOverlayCapableScreen screen, ITextComponent title, Consumer<Boolean> action,
	  List<ITextComponent> body, ITextComponent cancelText, ITextComponent confirmText,
	  CheckboxButton... checkBoxes
	) { this(screen, title, (v, s) -> action.accept(v), body, cancelText, confirmText, checkBoxes); }
	
	public ConfirmDialog(
	  IOverlayCapableScreen screen, ITextComponent title, BiConsumer<Boolean, boolean[]> action,
	  List<ITextComponent> body, ITextComponent cancelText, ITextComponent confirmText,
	  CheckboxButton... checkBoxes
	) {
		super(screen, title);
		this.body = body;
		this.action = action;
		this.checkBoxes = checkBoxes;
		cancelButton = new TintedButton(0, 0, 120, 20, cancelText, p -> cancel());
		confirmButton = new TintedButton(0, 0, 120, 20, confirmText, p -> confirm());
		addButton(cancelButton);
		addButton(confirmButton);
		setListener(cancelButton);
		cancelButton.changeFocus(true);
		bodyListeners.addAll(Arrays.asList(this.checkBoxes));
	}
	
	public CheckboxButton[] getCheckBoxes() {
		return checkBoxes;
	}
	
	public boolean[] checkBoxesState() {
		boolean[] state = new boolean[checkBoxes.length];
		for (int i = 0; i < checkBoxes.length; i++)
			state[i] = checkBoxes[i].getValue();
		return state;
	}
	
	public void confirm() {
		cancel(true);
	}
	
	@Override public void cancel(boolean success) {
		super.cancel(success);
		action.accept(success, checkBoxesState());
	}
	
	@Override protected void position() {
		w = (int) clamp(screen.width * 0.7, 120, 800);
		final int titleWidth = font.getStringPropertyWidth(title);
		if (titleWidth + 16 > w)
			w = min(screen.width - 32, titleWidth + 16);
		lines = getBody().stream().map(l -> font.trimStringToWidth(l, w - 16)).collect(Collectors.toList());
		h = (int) clamp(60 + getInnerHeight(), 68, screen.height * 0.9);
		super.position();
	}
	
	@Override public void renderInner(
	  MatrixStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		int tx = x + 4;
		int ty = y + 4;
		for (List<IReorderingProcessor> line : lines) {
			for (IReorderingProcessor l : line) {
				font.func_238407_a_(mStack, l, tx, ty, bodyColor);
				ty += lineHeight;
			}
			ty += paragraphMarginDown;
		}
		
		for (CheckboxButton checkBox : checkBoxes) {
			checkBox.x = x + 8;
			checkBox.y = ty + 2;
			checkBox.setWidth(w - 16);
			checkBox.render(mStack, mouseX, mouseY, delta);
			ty += 22;
		}
	}
	
	@Override public @Nullable Style getInnerTextAt(
	  int x, int y, int w, int h, double mX, double mY
	) {
		int tx = x + 4;
		int ty = y + 4;
		for (List<IReorderingProcessor> line : lines) {
			for (IReorderingProcessor l : line) {
				if (mY >= ty && mY < ty + lineHeight && mX >= tx && tx < x + w - 8)
					return font.getCharacterManager().func_243239_a(l, (int) round(mX - tx));
				ty += lineHeight;
			}
			ty += paragraphMarginDown;
		}
		return null;
	}
	
	@Override public int getInnerHeight() {
		return 4 + lines.stream().reduce(
		  0, (s, l) -> s + paragraphMarginDown + l.stream().reduce(
			 0, (ss, ll) -> ss + lineHeight, Integer::sum), Integer::sum)
		              + checkBoxes.length * 22;
	}
	
	@Override public String getText() {
		return title.getString() + "\n" + getBody().stream()
		  .map(ITextComponent::getString).collect(Collectors.joining("\n"));
	}
	
	public List<ITextComponent> getBody() {
		return body;
	}
	public void setBody(List<ITextComponent> body) {
		this.body = body;
	}
	
	public void setAction(BiConsumer<Boolean, boolean[]> action) {
		this.action = action;
	}
	public void setAction(Consumer<Boolean> action) {
		this.action = (v, s) -> action.accept(v);
	}
	
	public void setCancelText(ITextComponent text) {
		cancelButton.setMessage(text);
	}
	public void setConfirmText(ITextComponent text) {
		confirmButton.setMessage(text);
	}
	
	public void setCancelButtonTint(int color) {
		cancelButton.setTintColor(color);
	}
	public void setConfirmButtonTint(int color) {
		confirmButton.setTintColor(color);
	}
}
