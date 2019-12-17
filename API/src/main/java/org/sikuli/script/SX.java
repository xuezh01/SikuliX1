/*
 * Copyright (c) 2010-2019, sikuli.org, sikulix.com - MIT license
 */

package org.sikuli.script;

import org.sikuli.basics.Debug;
import org.sikuli.basics.Settings;
import org.sikuli.script.support.RunTime;

import javax.swing.*;
import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SX {

  static public class Log {
    public static void error(String msg, Object... args) {
      Debug.error("SX: " + msg, args);
    }
  }

  private static Log log = new Log();

  //<editor-fold desc="01 input, popup, popAsk, popError">
  private enum PopType {
    POPUP, POPASK, POPERROR, POPINPUT
  }

  private static boolean isVersion1() {
    return true;
  }

  private static boolean isHeadless() {
    return GraphicsEnvironment.isHeadless();
  }

  private static void pause(double time) {
    try {
      Thread.sleep((int) (time * 1000));
    } catch (InterruptedException ex) {
    }
  }

  private static final ScheduledExecutorService TIMEOUT_EXECUTOR = Executors.newSingleThreadScheduledExecutor();

  /**
   * optionally timed popup (self-vanishing)
   *
   * @param args (message, title, preset, hidden = false, timeout = forever)
   * @return
   */
  public static String input(Object... args) {
    if (isHeadless()) {
      log.error("running headless: input");
    } else {
      return (String) doPop(PopType.POPINPUT, args);
    }
    return null;
  }

  /**
   * optionally timed popup (self-vanishing)
   *
   * @param args (message, title, preset, hidden = false, timeout = forever)
   * @return
   */
  public static Boolean popup(Object... args) {
    if (isHeadless()) {
      log.error("running headless: popup");
    } else {
      return (Boolean) doPop(PopType.POPUP, args);
    }
    return false;
  }

  /**
   * optionally timed popup (self-vanishing)
   *
   * @param args (message, title, preset, hidden = false, timeout = forever)
   * @return
   */
  public static Boolean popAsk(Object... args) {
    if (isHeadless()) {
      log.error("running headless: popAsk");
    } else {
      return (Boolean) doPop(PopType.POPASK, args);
    }
    return false;
  }

  /**
   * optionally timed popup (self-vanishing)
   *
   * @param args (message, title, preset, hidden = false, timeout = forever)
   * @return
   */
  public static Boolean popError(Object... args) {
    if (isHeadless()) {
      log.error("running headless: popError");
    } else {
      return (Boolean) doPop(PopType.POPERROR, args);
    }
    return false;
  }

  private static Object doPop(PopType popType, Object... args) {
    class RunInput implements Runnable {
      PopType popType = PopType.POPUP;
      JFrame frame = null;
      String title = "";
      String message = "";
      String preset = "";
      Boolean hidden = false;
      Integer timeout = 0;
      Map<String, Object> parameters = new HashMap<>();
      Object returnValue;

      public RunInput(PopType popType, Object... args) {
        this.popType = popType;
        parameters = getPopParameters(args);
        title = (String) parameters.get("title");
        message = (String) parameters.get("message");
        preset = (String) parameters.get("preset");
        hidden = (Boolean) parameters.get("hidden");
        timeout = (Integer) parameters.get("timeout");
        frame = getFrame(parameters.get("location"));
      }

      @Override
      public void run() {
        returnValue = null;
        if (PopType.POPUP.equals(popType)) {
          JOptionPane.showMessageDialog(frame, message, title, JOptionPane.PLAIN_MESSAGE);
          returnValue = Boolean.TRUE;
        } else if (PopType.POPASK.equals(popType)) {
          int ret = JOptionPane.showConfirmDialog(frame, message, title, JOptionPane.YES_NO_OPTION);
          returnValue = Boolean.TRUE;
          if (ret == JOptionPane.CLOSED_OPTION || ret == JOptionPane.NO_OPTION) {
            returnValue = Boolean.FALSE;
          }
        } else if (PopType.POPERROR.equals(popType)) {
          JOptionPane.showMessageDialog(frame, message, title, JOptionPane.ERROR_MESSAGE);
          returnValue = Boolean.TRUE;
        } else if (PopType.POPINPUT.equals(popType)) {
          if (!hidden) {
            if ("".equals(title)) {
              title = "Sikuli input request";
            }
            returnValue = JOptionPane.showInputDialog(frame, message, title,
                    JOptionPane.PLAIN_MESSAGE, null, null, preset);
          } else {
            JTextArea messageText = new JTextArea(message);
            messageText.setColumns(20);
            messageText.setLineWrap(true);
            messageText.setWrapStyleWord(true);
            messageText.setEditable(false);
            messageText.setBackground(new JLabel().getBackground());
            final JPasswordField passwordField = new JPasswordField(preset);

            frame.addWindowListener(new WindowAdapter() {
              @Override
              public void windowOpened(WindowEvent e) {
                frame.removeWindowListener(this);
                new Thread(() -> {
                  pause(0.3);
                  EventQueue.invokeLater(() -> {
                    passwordField.requestFocusInWindow();
                  });
                }).start();
              }
            });

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.add(passwordField);
            panel.add(Box.createVerticalStrut(10));
            panel.add(messageText);
            int retval = JOptionPane.showConfirmDialog(frame, panel, title, JOptionPane.OK_CANCEL_OPTION);
            returnValue = "";
            if (0 == retval) {
              char[] pwchar = passwordField.getPassword();
              for (int i = 0; i < pwchar.length; i++) {
                returnValue = (String) returnValue + pwchar[i];
                pwchar[i] = 0;
              }
            }
          }
        }

        synchronized (this) {
          dispose(); // needs to be here, frame is not always closed properly otherwise
          this.notify();
        }
      }

      public int getTimeout() {
        if (Integer.MAX_VALUE == timeout) {
          return timeout;
        }
        return timeout * 1000;
      }

      public void dispose() {
        frame.dispose();
      }

      public Object getReturnValue() {
        return returnValue;
      }
    }

    RunInput popRun = new RunInput(popType, args);
    ScheduledFuture<?> timeoutJob = TIMEOUT_EXECUTOR.schedule((() -> {
      popRun.dispose();
    }), popRun.getTimeout(), TimeUnit.MILLISECONDS);

    if (EventQueue.isDispatchThread()) {
      try {
        popRun.run();
      } finally {
        timeoutJob.cancel(false);
      }
    } else {
      synchronized (popRun) {
        EventQueue.invokeLater(popRun);
        try {
          popRun.wait();
        } catch (InterruptedException e) {
          Debug.error("Interrupted while waiting for popup close: %s", e.getMessage());
        } finally {
          timeoutJob.cancel(false);
        }
      }
    }
    return popRun.getReturnValue();
  }

  private static Map<String, Object> getPopParameters(Object... args) {
    String parameterNames = "message,title,preset,hidden,timeout,location";
    String parameterClass = "s,s,s,b,i,e";
    Object[] parameterDefault = new Object[]{"not set", "SikuliX", "", false, Integer.MAX_VALUE, on()};
    return Parameters.create(parameterNames, parameterClass, parameterDefault, args);
  }

  private static JFrame getFrame(Object point) {
    int x;
    int y;
    if (point instanceof Point) {
      x = ((Point) point).x;
      y = ((Point) point).y;
    } else {
      x = ((Region) point).getCenter().x;
      y = ((Region) point).getCenter().y;
    }
    JFrame anchor = new JFrame();
    anchor.setAlwaysOnTop(true);
    anchor.setUndecorated(true);
    anchor.setSize(1, 1);
    anchor.setLocation(x, y);
    anchor.setVisible(true);
    return anchor;
  }

  private static Region on() {
    return Screen.getPrimaryScreen();
  }

  private static class Parameters {

    private Map<String, String> parameterTypes = new HashMap<>();
    private String[] parameterNames = null;
    private Object[] parameterDefaults = new Object[0];

    private Parameters(String theNames, String theClasses, Object[] theDefaults) {
      String[] names = theNames.split(",");
      String[] classes = theClasses.split(",");
      if (names.length == classes.length) {
        for (int n = 0; n < names.length; n++) {
          String clazz = classes[n];
          if (clazz.length() == 1) {
            clazz = clazz.toLowerCase();
            if ("s".equals(clazz)) {
              clazz = "String";
            } else if ("i".equals(clazz)) {
              clazz = "Integer";
            } else if ("d".equals(clazz)) {
              clazz = "Double";
            } else if ("b".equals(clazz)) {
              clazz = "Boolean";
            } else if ("e".equals(clazz)) {
              clazz = "Region";
            }
          }
          if ("String".equals(clazz) || "Integer".equals(clazz) ||
                  "Double".equals(clazz) || "Boolean".equals(clazz) ||
                  "Element".equals(clazz) || "Region".equals(clazz)) {
            parameterTypes.put(names[n], clazz);
          }
        }
        parameterNames = names;
        parameterDefaults = theDefaults;
      } else {
        log.error("Parameters: different length: names: %s classes: %s", theNames, theClasses);
      }
    }

    public static Map<String, Object> create(Object... args) {
      String theNames = (String) args[0];
      String theClasses = (String) args[1];
      Object[] theDefaults = (Object[]) args[2];
      Object[] theArgs = (Object[]) args[3];
      Parameters theParameters = new Parameters(theNames, theClasses, theDefaults);
      return theParameters.getAll(theArgs);
    }

    private Object get(Object possibleValue, String parameterName) {
      String clazz = parameterTypes.get(parameterName);
      Object value = null;
      if ("String".equals(clazz)) {
        if (possibleValue instanceof String) {
          value = possibleValue;
        }
      } else if ("Integer".equals(clazz)) {
        if (possibleValue instanceof Integer) {
          value = possibleValue;
        }
      } else if ("Double".equals(clazz)) {
        if (possibleValue instanceof Double) {
          value = possibleValue;
        }
      } else if ("Boolean".equals(clazz)) {
        if (possibleValue instanceof Boolean) {
          value = possibleValue;
        }
      } else if ("Region".equals(clazz)) {
        if (possibleValue instanceof Region) {
          value = possibleValue;
        }
      }
      return value;
    }

    public Map<String, Object> getAll(Object[] args) {
      Map<String, Object> params = new HashMap<>();
      if (isNotNull(parameterNames)) {
        int n = 0;
        int argsn = 0;
        for (String parameterName : parameterNames) {
          params.put(parameterName, parameterDefaults[n]);
          if (args.length > 0 && argsn < args.length) {
            Object arg = get(args[argsn], parameterName);
            if (isNotNull(arg)) {
              params.put(parameterName, arg);
              argsn++;
            }
          }
          n++;
        }
      }
      return params;
    }
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="09 options handling">
  private static Options sxOptions = null;

  public static void initOptions(String path) {
    float forever = Settings.FOREVER; // to force Settings initialization
    sxOptions = Options.init(path);
    if (null == sxOptions) {
      RunTime.terminate(999, "[ERROR] SikuliX Options init: not possible");
    }
  }

  public static void closeOptions() {
    if (null != sxOptions) {
      if (sxOptions.hasOptions()) {
        sxOptions.save();
      }
    }
  }

  public static Options options() {
    if (null == sxOptions) {
      initOptions("");
    }
    return sxOptions;
  }

  public static File getOptionsFile() {
    return options().getFile();
  }

  //<editor-fold desc="03 get option">

  /**
   * if no option file is found, the option is taken as not existing<br>
   * side-effect: if no options file is there, an options store will be created in memory<br>
   * in this case and when the option is absent or empty, the given default will be stored<br>
   * you might later save the options store to a file with saveOptions()<br>
   * the default value is either the empty string, number 0 or false
   *
   * @param pName    the option key (case-sensitive)
   * @param sDefault the default to be returned if option absent or empty
   * @return the associated value, the default value if absent or empty
   */
  public static String getOption(String pName, String sDefault) {
    return options().get(pName, sDefault);
  }

  /**
   * {link getOption}
   *
   * @param pName the option key (case-sensitive)
   * @return the associated value, empty string if absent
   */
  public static String getOption(String pName) {
    return getOption(pName, "");
  }

  /**
   * {link getOption}
   *
   * @param pName    the option key (case-sensitive)
   * @param nDefault the default to be returned if option absent, empty or not convertible
   * @return the converted integer number, default if absent, empty or not possible
   */
  public static int getOptionInteger(String pName, Integer nDefault) {
    return options().getInt(pName, nDefault);
  }

  /**
   * {link getOption}
   *
   * @param pName the option key (case-sensitive)
   * @return the converted integer number, 0 if absent or not possible
   */
  public static int getOptionInteger(String pName) {
    return getOptionInteger(pName, 0);
  }

  /**
   * {link getOption}
   *
   * @param pName the option key (case-sensitive)
   * @return the converted float number, default if absent or not possible
   */
  public static double getOptionNum(String pName, double nDefault) {
    return options().getNum(pName, nDefault);
  }

  /**
   * {link getOption}
   *
   * @param pName the option key (case-sensitive)
   * @return the converted double number, 0 if absent or not possible
   */
  public static double getOptionNum(String pName) {
    return getOptionNum(pName, 0);
  }

  /**
   * {link getOption}
   *
   * @param pName    the option key (case-sensitive)
   * @param bDefault the default to be returned if option absent or empty
   * @return true if option has yes or no, false for no or false (not case-sensitive)
   */
  public static boolean isOption(String pName, boolean bDefault) {
    return options().is(pName, bDefault);
  }

  /**
   * {link getOption}
   *
   * @param pName the option key (case-sensitive)
   * @return true only if option exists and has yes or true (not case-sensitive), in all other cases false
   */
  public static boolean isOption(String pName) {
    return isOption(pName, false);
  }
  //</editor-fold>

  //<editor-fold desc="05 set option">

  /**
   * {link getOption}
   *
   * @param pName  the option key (case-sensitive)
   * @param sValue the value to be set
   */
  public static void setOption(String pName, String sValue) {
    options().set(pName, sValue);
  }

  /**
   * {link getOption}
   *
   * @param pName  the option key (case-sensitive)
   * @param nValue the value to be set
   */
  public static void setOptionInt(String pName, int nValue) {
    options().setInteger(pName, nValue);
  }

  /**
   * {link getOption}
   *
   * @param pName  the option key (case-sensitive)
   * @param nValue the value to be set
   */
  public static void setOptionNum(String pName, Number nValue) {
    options().setNum(pName, nValue);
  }

  /**
   * {link getOption}
   *
   * @param pName  the option key (case-sensitive)
   * @param bValue the value to be set
   */
  public static void setOptionBool(String pName, boolean bValue) {
    options().setBool(pName, bValue);
  }
  //</editor-fold>

  //<editor-fold desc="09 all options">

  /**
   * check whether options are defined
   *
   * @return true if at lest one option defined else false
   */
  public static boolean hasOptions() {
    return options().hasOptions();
  }

  /**
   * all options and their values
   *
   * @return a map of key-value pairs containing the found options, empty if no options file found
   */
  public static Map<String, String> getOptions() {
    return options().getOptions();
  }

  /**
   * all options and their values written to sysout as key = value
   */
  public static void dumpOptions() {
    if (hasOptions()) {
      Map<String, String> mapOptions = getOptions();
      Debug.logp("*** options dump");
      for (String sOpt : mapOptions.keySet()) {
        Debug.logp("%s = %s", sOpt, mapOptions.get(sOpt));
      }
      Debug.logp("*** options dump end");
    }
  }
  //</editor-fold>
  //</editor-fold>

  public static boolean isNotNull(Object obj) {
    return null != obj;
  }

  public static boolean isNull(Object obj) {
    return null == obj;
  }

  //<editor-fold desc="10 Python support">
  public static void reset() {
    Debug.log(3, "SX.reset()");
    Screen.resetMonitorsQuiet();
    Mouse.reset();
  }
  //</editor-fold>
}
