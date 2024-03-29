package endorh.simpleconfig.ui.gui;

import endorh.simpleconfig.ui.gui.widget.CheckboxButton;
import endorh.simpleconfig.ui.gui.widget.TintedButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Math.*;

public class ConfirmDialog extends AbstractButtonDialog {
	protected List<Component> body = Collections.emptyList();
	protected int bodyColor = 0xffbdbdbd;
	protected int lineHeight = 10;
	protected int paragraphMarginDown = 4;
	protected List<List<FormattedCharSequence>> lines;
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
	  Component title
	) {
		return create(title, d -> {});
	}
	
	public static ConfirmDialog create(
	  Component title, Consumer<ConfirmDialog> builder
	) {
		ConfirmDialog dialog = new ConfirmDialog(title);
		builder.accept(dialog);
		return dialog;
	}
	
	protected ConfirmDialog(Component title) {
		super(title);
		cancelButton = TintedButton.of(CommonComponents.GUI_CANCEL, p -> cancel());
		confirmButton = TintedButton.of(CommonComponents.GUI_PROCEED, p -> confirm());
		addButton(cancelButton);
		addButton(confirmButton);
		setFocused(cancelButton);
		cancelButton.setFocused(true);
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
		int cW = (int) Mth.clamp(getScreen().width * 0.7, 120, 800);
		int w = (int) Mth.clamp(getScreen().width * 0.4, 120, 800);
		int titleWidth = font.width(title);
		w = max(w, titleWidth + 16);
		lines = getBody().stream().map(l -> font.split(l, cW - 16)).collect(Collectors.toList());
		int bodyWidth = IntStream.concat(
		  lines.stream().flatMap(Collection::stream).mapToInt(l -> font.width(l)),
		  Arrays.stream(checkBoxes).mapToInt(AbstractWidget::getWidth)
		).max().orElse(w) + 16;
		bodyWidth = max(bodyWidth, buttons.size() * 80);
		w = max(w, bodyWidth);
		w = min(w, getScreen().width - 32);
		int h = (int) Mth.clamp(60 + getInnerHeight(), 68, getScreen().height * 0.9);
		setWidth(w);
		setHeight(h);
		super.layout();
	}
	
	@Override public void renderInner(
      GuiGraphics gg, int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		int tx = x + 4;
		int ty = y + 4;
		for (List<FormattedCharSequence> line : lines) {
			for (FormattedCharSequence l : line) {
				gg.drawString(font, l, tx, ty, bodyColor); // TODO: shadow (drawShadow)
				ty += lineHeight;
			}
			ty += paragraphMarginDown;
		}
		
		for (CheckboxButton checkBox : checkBoxes) {
			checkBox.setX(x + 8);
			checkBox.setY(ty + 2);
			checkBox.setWidth(w - 16);
			checkBox.render(gg, mouseX, mouseY, delta);
			ty += 22;
		}
	}
	
	@Override public @Nullable Style getInnerTextAt(
	  int x, int y, int w, int h, double mX, double mY
	) {
		int tx = x + 4;
		int ty = y + 4;
		for (List<FormattedCharSequence> line : lines) {
			for (FormattedCharSequence l : line) {
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
		  .map(Component::getString).collect(Collectors.joining("\n"));
	}
	
	public List<Component> getBody() {
		return body;
	}
	public void setBody(List<? extends Component> body) {
		this.body = new ArrayList<>(body);
	}
	
	public void setCancelText(Component text) {
		cancelButton.setMessage(text);
	}
	public void setConfirmText(Component text) {
		confirmButton.setMessage(text);
	}
	
	public void setCancelButtonTint(int color) {
		cancelButton.setTintColor(color);
	}
	public void setConfirmButtonTint(int color) {
		confirmButton.setTintColor(color);
	}
}
