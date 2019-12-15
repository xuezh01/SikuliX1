/*
 * Copyright (c) 2010-2019, sikuli.org, sikulix.com - MIT license
 */

package org.sikuli.script;

import org.sikuli.basics.Debug;
import org.sikuli.basics.Settings;
import org.sikuli.script.support.RunTime;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Options {

  int lvl = 3;

  private void log(int level, String message, Object... args) {
    Debug.logx(level, "Options: " + message, args);
  }

  //<editor-fold desc="01 init, load, save">
  private Options() {
  }

  public Options(String path) {
    options = new Options();
    load(path);
  }

  public Options(File file) {
    this(file.getAbsolutePath());
  }

  private static Options options = null;

  private Properties pOptions = null;
  private File optionsFile = null;

  static boolean testing = false;

  private String fnSXOptions = "SikulixOptions.txt";
  private File fOptionsFolder = RunTime.getWorkDir();

  public File getFile() {
    return optionsFile;
  }

  public String getPath() {
    return optionsFile.getAbsolutePath();
  }

  public static String getDefaultContent() {
    String defaultContent = "# key = value";
    return defaultContent;
  }

  public static Options init() {
    return init("");
  }

  public static Options init(String path) {
    if (options == null) {
      options = new Options();
      options.sxLoad(path);
    }
    return options;
  }

  private void sxLoad(String path) {
    fOptionsFolder = RunTime.getSikulixStore();
    if (path == null || path.isEmpty()) {
      load(fnSXOptions);
    } else {
      load(path);
    }
    if (hasOptions()) {
      testing = is("testing", false);
      if (testing) {
        Debug.setDebugLevel(3);
      }
      Class cSettings;
      String settingsOptions = "";
      try {
        cSettings = Class.forName("org.sikuli.basics.Settings");
        for (Object oKey : pOptions.keySet()) {
          String sKey = (String) oKey;
          String[] parts = sKey.split("\\.");
          if (parts.length == 1) {
            continue;
          }
          String sClass = parts[0].trim();
          String sAttr = parts[1].trim();
          Field field;
          String fType;
          if (sClass.equals("Settings")) {
            try {
              field = cSettings.getField(sAttr);
              fType = field.getType().getName();
              if (fType == "boolean") {
                field.setBoolean(null, is(sKey));
              } else if (fType == "int") {
                field.setInt(null, getInteger(sKey));
              } else if (fType == "float") {
                field.setFloat(null, getFloat(sKey));
              } else if (fType == "double") {
                field.setDouble(null, getDouble(sKey));
              } else if (fType == "String") {
                field.set(null, get(sKey));
              }
              settingsOptions += field.getName() + ",";
            } catch (Exception ex) {
              log(-1, "loadOptions: not possible: %s = %s", sKey, pOptions.getProperty(sKey));
            }
          }
        }
      } catch (ClassNotFoundException e) {
      }
      // public Settings::fields
      Field[] fields = Settings.class.getDeclaredFields();
      Object value = null;
      for (Field field : fields) {
        try {
          if (field.getName().substring(0, 1).matches("[a-z]")) {
            continue;
          }
          Field theField = Settings.class.getField(field.getName());
          if (9 != field.getModifiers()) { //public static
            continue;
          }
          value = theField.get(null);
        } catch (NoSuchFieldException e) {
          continue;
        } catch (IllegalAccessException e) {
          continue;
        }
        if (settingsOptions.contains(field.getName() + ",")) {
          Debug.log(3,"StartUp: SikulixOptions: %s (%s) %s", field.getName(), field.getType(), value);
        }
      }
    }
  }

  void load(String fpOptions) {
    File fOptions = new File(fpOptions);
    boolean shouldSearch = true;
    if (fOptions.isAbsolute()) {
      if (!fOptions.exists()) {
        fpOptions = fOptions.getName();
        log(3, "loadOptions: will be created: %s", fOptions);
        shouldSearch = false;
      }
    }
    if (shouldSearch) {
      for (File aFile : new File[]{RunTime.getUserHome(), RunTime.getWorkDir(), RunTime.getSikulixStore()}) {
        fOptions = new File(aFile, fpOptions);
        if (fOptions.exists()) {
          break;
        } else {
          fOptions = null;
        }
      }
    }
    pOptions = new Properties();
    if (fOptions != null) {
      try {
        InputStream is;
        is = new FileInputStream(fOptions);
        pOptions.load(is);
        is.close();
        log(lvl, "loadOptions: Options file: %s", fOptions);
        optionsFile = new File(fOptions.getAbsolutePath());
      } catch (Exception ex) {
        log(-1, "loadOptions: %s: %s", fOptions, ex.getMessage());
        pOptions = null;
      }
    } else {
      optionsFile = new File(fOptionsFolder, fpOptions);
    }
  }

  /**
   * save a properties store to a file (prop: this.comingfrom = abs. filepath)
   *
   * @return success
   */
  public boolean save() {
    String fpOptions = optionsFile.getAbsolutePath();
    if (null == fpOptions) {
      log(-1, "saveOptions: not saved - optionsFile == null");
      return false;
    }
    return save(fpOptions);
  }

  /**
   * save a properties store to the given file
   *
   * @param fpOptions path to a file
   * @return success
   */
  public boolean save(String fpOptions) {
    File fOptions = new File(fpOptions);
    if (!fOptions.isAbsolute()) {
      fOptions = new File(RunTime.getWorkDir(), fpOptions);
    }
    try {
      OutputStream os;
      os = new FileOutputStream(fOptions);
      pOptions.store(os, "");
      os.close();
    } catch (Exception ex) {
      log(-1, "saveOptions: %s (error %s)", fOptions, ex.getMessage());
      return false;
    }
    log(lvl, "saved: %s", fpOptions);
    return true;
  }
  //</editor-fold>

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
  public String get(String pName, String sDefault) {
    if (pOptions == null) {
      return sDefault;
    }
    String pVal = pOptions.getProperty(pName, sDefault);
    if (pVal.isEmpty()) {
      pOptions.setProperty(pName, sDefault);
      return sDefault;
    }
    return pVal;
  }

  /**
   * {link getOption}
   *
   * @param pName the option key (case-sensitive)
   * @return the associated value, empty string if absent
   */
  public String get(String pName) {
    return get(pName, "");
  }

  /**
   * {link getOption}
   *
   * @param pName    the option key (case-sensitive)
   * @param nDefault the default to be returned if option absent, empty or not convertible
   * @return the converted integer number, default if absent, empty or not possible
   */
  public int getInteger(String pName, Integer nDefault) {
    if (pOptions == null) {
      return nDefault;
    }
    String pVal = pOptions.getProperty(pName, nDefault.toString());
    int nVal = nDefault;
    try {
      nVal = Integer.decode(pVal);
    } catch (Exception ex) {
    }
    return nVal;
  }

  /**
   * {link getOption}
   *
   * @param pName the option key (case-sensitive)
   * @return the converted integer number, 0 if absent or not possible
   */
  public int getInteger(String pName) {
    return getInteger(pName, 0);
  }

  /**
   * {link getOption}
   *
   * @param pName the option key (case-sensitive)
   * @return the converted float number, default if absent or not possible
   */
  public float getFloat(String pName, float nDefault) {
    if (pOptions == null) {
      return nDefault;
    }
    String pVal = pOptions.getProperty(pName, "0");
    float nVal = nDefault;
    try {
      nVal = Float.parseFloat(pVal);
    } catch (Exception ex) {
    }
    return nVal;
  }

  /**
   * {link getOption}
   *
   * @param pName the option key (case-sensitive)
   * @return the converted float number, 0 if absent or not possible
   */
  public float getFloat(String pName) {
    return getFloat(pName, 0);
  }

  /**
   * {link getOption}
   *
   * @param pName the option key (case-sensitive)
   * @return the converted float number, default if absent or not possible
   */
  public double getDouble(String pName, double nDefault) {
    if (pOptions == null) {
      return nDefault;
    }
    String pVal = pOptions.getProperty(pName, "0");
    double nVal = nDefault;
    try {
      nVal = Double.parseDouble(pVal);
    } catch (Exception ex) {
    }
    return nVal;
  }

  /**
   * {link getOption}
   *
   * @param pName the option key (case-sensitive)
   * @return the converted double number, 0 if absent or not possible
   */
  public double getDouble(String pName) {
    return getDouble(pName, 0);
  }

  /**
   * {link getOption}
   *
   * @param pName    the option key (case-sensitive)
   * @param bDefault the default to be returned if option absent or empty
   * @return true if option has yes or no, false for no or false (not case-sensitive)
   */
  public boolean is(String pName, boolean bDefault) {
    if (pOptions == null) {
      return bDefault;
    }
    String pVal = pOptions.getProperty(pName, bDefault ? "true" : "false").toLowerCase();
    if (pVal.isEmpty()) {
      return bDefault;
    } else if (pVal.contains("yes") || pVal.contains("true") || pVal.contains("on")) {
      return true;
    }
    return false;
  }

  /**
   * {link getOption}
   *
   * @param pName the option key (case-sensitive)
   * @return true only if option exists and has yes or true (not case-sensitive), in all other cases false
   */
  public boolean is(String pName) {
    return is(pName, false);
  }
  //</editor-fold>

  //<editor-fold desc="05 set option">

  /**
   * {link getOption}
   *
   * @param pName  the option key (case-sensitive)
   * @param sValue the value to be set
   */
  public void set(String pName, String sValue) {
    pOptions.setProperty(pName, sValue);
  }

  /**
   * {link getOption}
   *
   * @param pName  the option key (case-sensitive)
   * @param nValue the value to be set
   */
  public void setInteger(String pName, int nValue) {
    pOptions.setProperty(pName, "" + nValue);
  }

  /**
   * {link getOption}
   *
   * @param pName  the option key (case-sensitive)
   * @param nValue the value to be set
   */
  public void setFloat(String pName, float nValue) {
    pOptions.setProperty(pName, "" + nValue);
  }

  /**
   * {link getOption}
   *
   * @param pName  the option key (case-sensitive)
   * @param nValue the value to be set
   */
  public void setDouble(String pName, double nValue) {
    pOptions.setProperty(pName, "" + nValue);
  }

  /**
   * {link getOption}
   *
   * @param pName  the option key (case-sensitive)
   * @param bValue the value to be set
   */
  public void setBoolean(String pName, boolean bValue) {
    pOptions.setProperty(pName, is(pName, bValue) ? "true" : "false");
  }
  //</editor-fold>

  //<editor-fold desc="09 all options">

  /**
   * check whether options are defined
   *
   * @return true if at lest one option defined else false
   */
  public boolean hasOptions() {
    return pOptions != null && pOptions.size() > 0;
  }

  public int getCount() {
    return pOptions.size();
  }

  /**
   * all options and their values
   *
   * @return a map of key-value pairs containing the found options, empty if no options file found
   */
  public Map<String, String> getOptions() {
    Map<String, String> mapOptions = new HashMap<String, String>();
    if (pOptions != null) {
      Enumeration<?> optionNames = pOptions.propertyNames();
      String optionName;
      while (optionNames.hasMoreElements()) {
        optionName = (String) optionNames.nextElement();
        mapOptions.put(optionName, get(optionName));
      }
    }
    return mapOptions;
  }

  /**
   * all options and their values written to sysout as key = value
   */
  public void dumpOptions() {
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
}
