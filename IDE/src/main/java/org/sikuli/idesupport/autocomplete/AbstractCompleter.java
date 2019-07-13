package org.sikuli.idesupport.autocomplete;

import org.sikuli.ide.EditorPane;
import org.sikuli.ide.SikuliIDEPopUpMenu;
import org.sikuli.idesupport.IAutoCompleter;
import org.sikuli.script.support.IScriptRunner;

public abstract class AbstractCompleter implements IAutoCompleter {

  EditorPane pane = null;

  @Override
  public void setPane(EditorPane pane) {
    this.pane = pane;
  }

  @Override
  public IScriptRunner getRunner() {
    return pane.getRunner();
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
