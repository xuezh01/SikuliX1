package org.sikuli.idesupport;

import org.sikuli.ide.EditorPane;
import org.sikuli.script.support.IScriptRunner;

public interface IAutoCompleter {

  public String getName();

  public void setPane(EditorPane pane);

  public IScriptRunner getRunner();

}
