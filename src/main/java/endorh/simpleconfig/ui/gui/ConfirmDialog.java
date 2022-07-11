package endorh.simpleconfig.ui.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.ui.gui.widget.CheckboxButton;
import endorh.simpleconfig.ui.gui.widget.TintedButton;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.Math.min;
import static java.lang.Math.round;
import static net.minecraft.util.math.MathHelper.clamp;

public class ConfirmDialog extends AbstractButtonDialog {
	protected List<ITextComponent> body = Collections.emptyList();
	protected int bodyColor = 0xffbdbdbd;
	protected int lineHeight = 10;
	protected int paragraphMarginDown = 4;
	protected List<List<IReorderingProcessor>> lines;
	protected TintedButton cancelButton;
	protected TintedButton confirmButton;
	protected DialogAction action = (ComplexDialogAction) (v, s) -> {};
	
	protected CheckboxButton[] checkBoxes = new CheckboxButton[0];
	
	@FunctionalInterface public interface DialogAction {
		void handle(boolean success);
		default void handle(boolean success, boolean[] checkBoxes) {
			handle(success);
		}
	}
	
	@FunctionalInterface public interface ComplexDialogAction extends DialogAction {
		void handleComplex(boolean success, boolean[] checkBoxes);
		
		@Override default void handle(boolean success) {
			throw new UnsupportedOperationException("Illegal implementation");
		}
		@Override default void handle(boolean success, boolean[] checkBoxes) {
			handleComplex(success, checkBoxes);
		}
	}
	
	public static ConfirmDialog create(
	  IOverlayCapableScreen screen, ITextComponent title
	) {
		return create(screen, title, d -> {});
	}
	
	public static ConfirmDialog create(
	  IOverlayCapableScreen screen, ITextComponent title, Consumer<ConfirmDialog> builder
	) {
		ConfirmDialog dialog = new ConfirmDialog(screen, title);
		builder.accept(dialog);
		return dialog;
	}
	
	protected ConfirmDialog(IOverlayCapableScreen screen, ITextComponent title) {
		super(screen, title);
		cancelButton = TintedButton.of(120, 20, DialogTexts.GUI_CANCEL, p -> cancel());
		confirmButton = TintedButton.of(120, 20, DialogTexts.GUI_PROCEED, p -> confirm());
		addButton(cancelButton);
		addButton(confirmButton);
		setFocused(cancelButton);
		cancelButton.changeFocus(true);
	}
	
	public void withAction(DialogAction handler) {
		this.action = handler;
	}
	
	public void withCheckBoxes(
	  ComplexDialogAction action, CheckboxButton... checkBoxes
	) {
		this.action = action;
		bodyListeners.removeAll(Arrays.asList(this.checkBoxes));
		this.checkBoxes = checkBoxes;
		bodyListeners.addAll(Arrays.asList(this.checkBoxes));
	}
	
	public CheckboxButton[] getCheckBoxes() {
		return checkBoxes;
	}
	
	public boolean[] getCheckBoxesState() {
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
		action.handle(success, getCheckBoxesState());
	}
	
	@Override protected void layout() {
		w = (int) clamp(screen.width * 0.7, 120, 800);
		final int titleWidth = font.width(title);
		if (titleWidth + 16 > w)
			w = min(screen.width - 32, titleWidth + 16);
		lines = getBody().stream().map(l -> font.split(l, w - 16)).collect(Collectors.toList());
		h = (int) clamp(60 + getInnerHeight(), 68, screen.height * 0.9);
		super.layout();
	}
	
	@Override public void renderInner(
	  MatrixStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		int tx = x + 4;
		int ty = y + 4;
		for (List<IReorderingProcessor> line : lines) {
			for (IReorderingProcessor l : line) {
				font.drawShadow(mStack, l, tx, ty, bodyColor);
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
					return font.getSplitter().componentStyleAtWidth(l, (int) round(mX - tx));
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
