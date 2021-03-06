/*
 * Copyright (c) 2010-2020, sikuli.org, sikulix.com - MIT license
 */

package org.sikuli.script.test;

import org.junit.*;
import org.junit.runners.MethodSorters;
import org.sikuli.basics.Settings;
import org.sikuli.script.FindFailed;
import org.sikuli.script.Image;
import org.sikuli.script.Match;
import org.sikuli.script.support.SXTest;
import org.sikuli.util.Highlight;

import java.net.URL;
import java.util.List;
import java.util.Map;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ElementImageTest extends SXTest {

  @Before
  public void setUp() {
    setUpBase();
  }

  @Test
  public void test000_StartUp() {
    startUpBase();
  }

  @Test
  public void test090_ImageFindResizeUp() {
    testIntro();
    Image shot = new Image(testBaseX2);
    Assert.assertTrue("", shot.isValid());
    Match match = null;
    Settings.AlwaysResize = 2;
    try {
      match = shot.find(testName);
    } catch (FindFailed findFailed) {
    }
//    Image.resetCache();
    testOutro("%s in %s is %s", testName, shot, match);
    Assert.assertNotNull(testName + " not found in " + testBaseX2, match);
  }

  @Test
  public void test091_ImageFindResizeOff() {
    testIntro();
    Image shot = new Image(testBase);
    Assert.assertTrue("", shot.isValid());
    Match match = null;
    Settings.AlwaysResize = 1;
    try {
      match = shot.find(testName);
    } catch (FindFailed findFailed) {
    }
    testOutro("%s in %s is %s", testName, shot, match);
    Assert.assertNotNull(testName + " not found", match);
  }

  @Test
  public void test092_ImageFindResizeDown() {
    testIntro();
    Image shot = new Image(testBase);
    Assert.assertTrue("", shot.isValid());
    Match match = null;
    Settings.AlwaysResize = 0.5;
    try {
      match = shot.find(testNameX2);
    } catch (FindFailed findFailed) {
    }
    Image.resetCache();
    testOutro("%s in %s is %s", testName, shot, match);
    Assert.assertNotNull(testNameX2 + " not found", match);
  }

  @Test
  public void test100_ImageFind() {
    testIntro();
    Image shot = new Image(testBase);
    Match match = null;
    try {
      match = shot.find(testName);
    } catch (FindFailed findFailed) {
    }
    testOutro("%s in %s is %s", testName, shot, match);
    Assert.assertNotNull(testName + " not found", match);
  }

  @Ignore
  public void test110_ImageFindTrans() {
    testIntro();
    Image shot = new Image(testBase);
    Match match = null;
    try {
      match = shot.find(testNameTrans);
    } catch (FindFailed findFailed) {
    }
    testOutro("%s in %s is %s", testName, shot, match);
  }

  @Test
  public void test120_ImageFindChanges() {
    if (showImage) {
      testIntro(testBase);
    } else {
      testIntro();
    }
    Image original = new Image(testBase);
    Image changed = new Image(testChanged);
    List<Match> changes = original.findChanges(changed);
    long time = -1;
    if (changes.size() > 0) {
      time = changes.get(0).getTime();
    }
    for (Match change : changes) {
      if (showImage) {
        change.highlight();
      }
    }
    if (showImage) {
      Highlight.closeAll(3);
    }
    testOutro("%s%s == %s changes %d (%d)", "", original, changed, changes.size(), time);
    Assert.assertTrue("Not all changes!", changes.size() == 6);
  }

  @Test
  public void test800_ImageMissingPrompt() {
    testIntro();
    if (showImage) {
      String testMissing = makeImageFileName(testMissingName, true);
      Image.setMissingPrompt();
      Image missing = null;
      try {
        missing = new Image(testMissingName);
        testOutro("created: %s as: %s", missing, missing.url());
        Assert.assertTrue("create missing not valid", missing.isValid());
      } catch (Exception e) {
        testOutro("not created missing: %s (%s)", testMissingName, e.getMessage());
      }
    } else {
      testOutro("skipped: image missing prompting");
    }
  }

  @Test
  public void test999() {
    testIntro();
    Map<URL, List<Object>> cache = Image.getCache();
    testOutro("%s", Image.cacheStats());
  }
}
