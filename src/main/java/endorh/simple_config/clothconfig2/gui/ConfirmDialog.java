package endorh.simple_config.clothconfig2.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simple_config.clothconfig2.gui.widget.CheckboxButton;
import endorh.simple_config.clothconfig2.gui.widget.TintedButton;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.ITextComponent;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.Math.min;
import static net.minecraft.util.math.MathHelper.clamp;

public class ConfirmDialog extends AbstractDialog {
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
	  Consumer<Boolean> action, ITextComponent title,
	  List<ITextComponent> body, IOverlayCapableScreen screen, CheckboxButton... checkBoxes
	) { this((v, s) -> action.accept(v), title, body, screen, checkBoxes); }
	
	public ConfirmDialog(
	  BiConsumer<Boolean, boolean[]> action, ITextComponent title,
	  List<ITextComponent> body, IOverlayCapableScreen screen, CheckboxButton... checkBoxes
	) {
		this(action, title, body, DialogTexts.GUI_CANCEL, DialogTexts.GUI_PROCEED, screen, checkBoxes);
	}
	
	public ConfirmDialog(
	  Consumer<Boolean> action, ITextComponent title, List<ITextComponent> body,
	  ITextComponent cancelText, ITextComponent confirmText,
	  IOverlayCapableScreen screen, CheckboxButton... checkBoxes
	) { this((v, s) -> action.accept(v), title, body, cancelText, confirmText, screen, checkBoxes); }
	
	public ConfirmDialog(
	  BiConsumer<Boolean, boolean[]> action, ITextComponent title, List<ITextComponent> body,
	  ITextComponent cancelText, ITextComponent confirmText,
	  IOverlayCapableScreen screen, CheckboxButton... checkBoxes
	) {
		super(title, screen);
		this.body = body;
		this.action = action;
		cancelButton = new TintedButton(0, 0, 120, 20, cancelText, p -> cancel());
		confirmButton = new TintedButton(0, 0, 120, 20, confirmText, p -> confirm());
		listeners.add(cancelButton);
		listeners.add(confirmButton);
		listeners.addAll(Arrays.asList(checkBoxes));
		setListener(cancelButton);
		cancelButton.changeFocus(true);
		this.checkBoxes = checkBoxes;
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
		lines = body.stream().map(l -> font.trimStringToWidth(l, w - 16)).collect(Collectors.toList());
		h = (int) clamp(
		  64 + lines.stream().reduce(
			 0, (s, l) -> s + paragraphMarginDown + l.stream().reduce(
				0, (ss, ll) -> ss + lineHeight, Integer::sum), Integer::sum)
		  + checkBoxes.length * 22, 96, screen.height * 0.9);
		super.position();
		int bw = min(150, (w - 12) / 2);
		cancelButton.setWidth(bw);
		confirmButton.setWidth(bw);
		cancelButton.x = x + w / 2 - 2 - bw;
		confirmButton.x = x + w / 2 + 2;
		cancelButton.y = confirmButton.y = y + h - 24;
	}
	
	@Override public void renderBody(
	  MatrixStack mStack, int mouseX, int mouseY, float delta
	) {
		int tx = x + 8;
		int ty = y + 32;
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
		
		fill(mStack, x + 1, y + h - 27, x + w - 1, y + h - 1, backgroundOverlayColor);
		fill(mStack, x + 1, y + h - 28, x + w - 1, y + h - 27, subBorderColor);
		cancelButton.render(mStack, mouseX, mouseY, delta);
		confirmButton.render(mStack, mouseX, mouseY, delta);
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
		this.cancelButton.setTintColor(color);
	}
	
	public void setConfirmButtonTint(int color) {
		this.confirmButton.setTintColor(color);
	}
}
