package org.sikuli.idesupport.autocomplete;

import org.sikuli.basics.Debug;
import org.sikuli.ide.EditorPane;
import org.sikuli.ide.SikuliIDEPopUpMenu;
import org.sikuli.idesupport.IAutoCompleter;
import org.sikuli.script.support.IScriptRunner;

import javax.swing.text.Caret;
import javax.swing.text.Element;
import java.awt.Point;

public abstract class AbstractCompleter implements IAutoCompleter {

  protected void log(int level, String message, Object... args) {
    Debug.logx(level, getName() + "Completer: " + message, args);
  }

  EditorPane pane = null;

  @Override
  public void setPane(EditorPane pane) {
    this.pane = pane;
  }

  @Override
  public IScriptRunner getRunner() {
    return pane.getRunner();
  }

  @Override
  public void handle(Caret caret, int start, int pos, String lineText) {
    Point cPoint = caret.getMagicCaretPosition();
    int lineNumber = pane.getLineNumberAtCaret(caret.getDot());
    String text = lineText.substring(0, pos - start);
    String rest = lineText.substring(pos - start);
    log(3, "handle %d at (%d,%d): %s", lineNumber, cPoint.x, cPoint.y, text + " # " + rest);
  }

  private SikuliIDEPopUpMenu popCompletion = null;

  private void stuff() {
    popCompletion = new SikuliIDEPopUpMenu("POP_COMPLETION", this);
    if (!popCompletion.isValidMenu()) {
      popCompletion = null;
    }
    popCompletion = pane.getPopMenuCompletion();
    if (null == popCompletion || !popCompletion.isValidMenu()) {
      popCompletion = null;
    }
  }
}
