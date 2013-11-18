package org.qe4j.web;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.qe4j.web.OpenWebDriver.Browser;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * This file is part of QE4J.
 *
 * QE4J is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * QE4J is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * QE4J. If not, see <http://www.gnu.org/licenses/>.
 *
 * Unit test class for OpenWebDriver
 *
 * @author Jeff Ekhardt <jekhardt> 2012-06-19
 *
 */
public class OpenWebDriverTest {

    private final org.slf4j.Logger log = org.slf4j.LoggerFactory
            .getLogger(getClass());

    private static final String URL = "http://127.0.0.1:9091/test/";
    private static final String URL_TITLE = "HTML TEST1";
    private static final String URL2 = "http://127.0.0.1:9091/test/htmlTest2.html";
    private static final String URL2_TITLE = "HTML TEST2";

    public Properties getProperties() {
        Properties properties = new Properties();
        properties.setProperty("webdriver.platform", "local");
        properties.setProperty("webdriver.browser", "HtmlUnit");
        properties.setProperty("webdriver.browser-version", "3.6");
        properties.setProperty("webdriver.wait", "10");
        return properties;
    }

    @Test
    public void OpenWebDriver() throws IOException {
        Properties properties = getProperties();
        WebDriver driver = new OpenWebDriver(properties);
        Assert.assertNotNull(driver, "constructor");
    }

    @Test
    public void chromeMac() throws IOException {
        Properties properties = getProperties();
        properties.setProperty("webdriver.browser", "Chrome");
        properties.setProperty("webdriver.chrome.driver", "bin/chromedriver");
        System.setProperty("os.name", "Mac");
        System.setProperty("os.name.overriden", "true");
        try {
            OpenWebDriver driver = new OpenWebDriver(properties);
            Assert.assertEquals(
                    driver.getProperties().getProperty(
                            "webdriver.chrome.driver"), "bin/chromedriver",
                    "original property");
        } catch (IllegalStateException e) {
            log.info("exception occurred, which is fine for this unit test");
        }
        File file = new File(System.getProperty("webdriver.chrome.driver"));
        Assert.assertEquals(file.getName(), "chromedriver-mac",
                "chromedriver mac");
    }

    @Test(dependsOnMethods = "chromeMac")
    public void chromeLinux() throws IOException {
        OpenWebDriver.setChromeDriverInitialized(false);
        Properties properties = getProperties();
        properties.setProperty("webdriver.browser", "Chrome");
        properties.setProperty("webdriver.chrome.driver", "bin/chromedriver");
        System.setProperty("os.name", "Linux 10");
        System.setProperty("os.name.overriden", "true");
        try {
            OpenWebDriver driver = new OpenWebDriver(properties);
            Assert.assertEquals(
                    driver.getProperties().getProperty(
                            "webdriver.chrome.driver"), "bin/chromedriver",
                    "original property");
        } catch (IllegalStateException e) {
            log.info("exception occurred, which is fine for this unit test");
        }
        File file = new File(System.getProperty("webdriver.chrome.driver"));
        Assert.assertEquals(file.getName(), "chromedriver-linux64",
                "chromedriver linux");
    }

    @Test(dependsOnMethods = "chromeLinux")
    public void chromeWindows() throws IOException {
        OpenWebDriver.setChromeDriverInitialized(false);
        Properties properties = getProperties();
        properties.setProperty("webdriver.browser", "Chrome");
        properties.setProperty("webdriver.chrome.driver", "bin/chromedriver");
        System.setProperty("os.name", "Windows 7");
        System.setProperty("os.name.overriden", "true");
        try {
            OpenWebDriver driver = new OpenWebDriver(properties);
            Assert.assertEquals(
                    driver.getProperties().getProperty(
                            "webdriver.chrome.driver"), "bin/chromedriver",
                    "original property");
        } catch (IllegalStateException e) {
            log.info("exception occurred, which is fine for this unit test");
        }
        File file = new File(System.getProperty("webdriver.chrome.driver"));
        Assert.assertEquals(file.getName(), "chromedriver-win.exe",
                "chromedriver windows");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void invalidBrowser() throws IOException {
        Properties properties = getProperties();
        properties.setProperty("webdriver.browser", "Opera");
        new OpenWebDriver(properties);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void emptyBrowser() throws IOException {
        Properties properties = getProperties();
        properties.setProperty("webdriver.browser", "");
        new OpenWebDriver(properties);
    }

    @Test
    public void close() throws IOException {
        Properties properties = getProperties();
        OpenWebDriver driver = new OpenWebDriver(properties);
        driver.get(URL);
        JavascriptExecutor jscript = (JavascriptExecutor) driver.getWebDriver();
        jscript.executeScript("window.open('" + URL2 + "')");
        Set<String> handles = driver.getWindowHandles();
        Assert.assertEquals(handles.size(), 2, "two handles to start");
        driver.close();
        handles = driver.getWindowHandles();
        Assert.assertEquals(handles.size(), 1, "one handle after close");
    }

    @Test
    public void findElement() throws IOException {
        Properties properties = getProperties();
        WebDriver driver = new OpenWebDriver(properties);
        driver.get(URL);
        WebElement div = driver.findElement(By.id("html1"));
        Assert.assertEquals(div.getTagName(), "div", "find element");
    }

    @Test
    public void findElements() throws IOException {
        Properties properties = getProperties();
        WebDriver driver = new OpenWebDriver(properties);
        driver.get(URL);
        List<WebElement> fields = driver.findElements(By.tagName("div"));
        Assert.assertEquals(fields.size(), 2, "found multiple divs");
    }

    @Test
    public void get() throws IOException {
        Properties properties = getProperties();
        WebDriver driver = new OpenWebDriver(properties);
        driver.get(URL);
    }

    @Test
    public void getCurrentUrl() throws IOException {
        Properties properties = getProperties();
        WebDriver driver = new OpenWebDriver(properties);
        driver.get(URL);
        Assert.assertEquals(driver.getCurrentUrl(), URL, "current url");
    }

    @Test
    public void getPageSource() throws IOException {
        Properties properties = getProperties();
        WebDriver driver = new OpenWebDriver(properties);
        driver.get(URL);
        Assert.assertTrue(driver.getPageSource().contains("</div>"),
                "page source");
    }

    @Test
    public void getTitle() throws IOException {
        Properties properties = getProperties();
        WebDriver driver = new OpenWebDriver(properties);
        driver.get(URL);
        Assert.assertEquals(driver.getTitle(), URL_TITLE, "title");
    }

    @Test
    public void getWindowHandle() throws IOException {
        Properties properties = getProperties();
        WebDriver driver = new OpenWebDriver(properties);
        driver.get(URL);
        Assert.assertEquals(Integer.valueOf(driver.getWindowHandle())
                .getClass(), Integer.class, "valid window handle");
    }

    @Test
    public void getWindowHandles() throws IOException {
        Properties properties = getProperties();
        OpenWebDriver driver = new OpenWebDriver(properties);
        driver.get(URL);
        JavascriptExecutor jscript = (JavascriptExecutor) driver.getWebDriver();
        jscript.executeScript("window.open('" + URL2 + "')");
        Set<String> handles = driver.getWindowHandles();
        Assert.assertEquals(handles.size(), 2, "multiple window handles");
    }

    @Test
    public void lookupPlatform() {
        Assert.assertEquals(OpenWebDriver.lookupPlatform("Xp"), Platform.XP,
                "xp platform");
        Assert.assertEquals(OpenWebDriver.lookupPlatform("VISTA"),
                Platform.VISTA, "vista/win7 platform");
        Assert.assertEquals(OpenWebDriver.lookupPlatform("linUX"),
                Platform.LINUX, "linux platform");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void lookupPlatformInvalid() {
        OpenWebDriver.lookupPlatform("win");
    }

    @Test
    public void manage() throws IOException {
        Properties properties = getProperties();
        OpenWebDriver driver = new OpenWebDriver(properties);
        driver.get(URL);
        Assert.assertEquals(driver.manage().getClass().getSimpleName(),
                "HtmlUnitOptions", "manage gets Options");
    }

    @Test
    public void navigate() throws IOException {
        Properties properties = getProperties();
        OpenWebDriver driver = new OpenWebDriver(properties);
        driver.get(URL);
        driver.navigate().to(URL2);
        Assert.assertEquals(driver.getTitle(), URL2_TITLE,
                "navigated to new url");
        driver.navigate().back();
        Assert.assertEquals(driver.getTitle(), URL_TITLE, "navigated back");
    }

    @Test(expectedExceptions = NoSuchWindowException.class)
    public void quit() throws IOException {
        Properties properties = getProperties();
        OpenWebDriver driver = new OpenWebDriver(properties);
        driver.get(URL);
        JavascriptExecutor jscript = (JavascriptExecutor) driver.getWebDriver();
        jscript.executeScript("window.open('" + URL2 + "')");
        Set<String> handles = driver.getWindowHandles();
        Assert.assertEquals(handles.size(), 2, "two handles to start");
        driver.quit();
        driver.getWindowHandle();
    }

    @Test
    public void switchTo() throws IOException {
        Properties properties = getProperties();
        OpenWebDriver driver = new OpenWebDriver(properties);
        driver.get(URL);
        JavascriptExecutor jscript = (JavascriptExecutor) driver.getWebDriver();
        jscript.executeScript("window.open('" + URL2 + "')");
        List<String> list = new ArrayList<String>(driver.getWindowHandles());
        driver.switchTo().window(list.get(0));
        Assert.assertEquals(driver.getWindowHandle(), list.get(0),
                "first window");
        driver.switchTo().window(list.get(1));
        Assert.assertEquals(driver.getWindowHandle(), list.get(1),
                "second window");
    }

    @Test
    public void switchToWindow() throws IOException {
        Properties properties = getProperties();
        OpenWebDriver driver = new OpenWebDriver(properties);
        driver.get(URL);
        JavascriptExecutor jscript = (JavascriptExecutor) driver.getWebDriver();
        jscript.executeScript("window.open('" + URL2 + "')");
        List<String> list = new ArrayList<String>(driver.getWindowHandles());
        driver.switchToWindow(0);
        Assert.assertEquals(driver.getWindowHandle(), list.get(0),
                "first window");
        driver.switchToWindow(1);
        Assert.assertEquals(driver.getWindowHandle(), list.get(1),
                "second window");
    }

    @Test
    public void newInstance() throws IOException {
        Properties properties = getProperties();
        OpenWebDriver driver1 = new OpenWebDriver(properties);
        OpenWebDriver driver2 = driver1.newInstance();
        Assert.assertNotSame(driver1, driver2, "different drivers made");
        driver1.get(URL);
        driver2.get(URL2);
        Assert.assertEquals(driver1.getTitle(), URL_TITLE, "driver1 title");
        driver1.quit();
        Assert.assertEquals(driver2.getTitle(), URL2_TITLE, "driver2 title");
    }

    @Test
    public void multipleInstances() throws IOException {
        Properties properties = getProperties();
        WebDriver driver1 = new OpenWebDriver(properties);
        WebDriver driver2 = new OpenWebDriver(properties);
        Assert.assertNotSame(driver1, driver2, "drivers not the same");
        Assert.assertNotNull(driver1, "driver1 exists");
        Assert.assertNotNull(driver2, "driver2 exists");
        Assert.assertNotSame(driver1, driver2, "different drivers made");
        driver1.get(URL);
        driver2.get(URL2);
        Assert.assertEquals(driver1.getTitle(), URL_TITLE, "driver1 title");
        driver1.quit();
        Assert.assertEquals(driver2.getTitle(), URL2_TITLE, "driver2 title");
    }

    @Test
    public void newActions() throws IOException {
        Properties properties = getProperties();
        OpenWebDriver driver = new OpenWebDriver(properties);
        Actions actions = driver.newActions();
        Assert.assertNotNull(actions, "actions created");
    }

    @Test
    public void testGetProperties() throws IOException {
        Properties properties = getProperties();
        OpenWebDriver driver = new OpenWebDriver(properties);
        Assert.assertSame(driver.getProperties(), properties, "same properties");
    }

    @Test
    public void getBrowserBinaryPathDefinedNull() throws IOException {
        Properties properties = getProperties();
        properties.setProperty("webdriver.linux.chrome.24", "");
        OpenWebDriver driver = new OpenWebDriver();
        String binaryPath = driver.getBrowserBinaryPath(Platform.LINUX,
                Browser.CHROME, "24", properties);
        Assert.assertNull(binaryPath, "prop key exists but not defined");
    }

    @Test
    public void getBrowserBinaryPathDefinedPath() throws IOException {
        Properties properties = getProperties();
        String expectedBinary = "/Test/binarypath";
        properties.setProperty("webdriver.mac.firefox.18", expectedBinary);
        OpenWebDriver driver = new OpenWebDriver();
        String binaryPath = driver.getBrowserBinaryPath(Platform.MAC,
                Browser.FIREFOX, "18", properties);
        Assert.assertEquals(binaryPath, expectedBinary, "binary path");
    }

    @Test
    public void getBrowserBinaryPathPropKeyNotPresent() throws IOException {
        Properties properties = getProperties();
        properties.setProperty("webdriver.win.chrome.22", "/Test/binarypath");
        OpenWebDriver driver = new OpenWebDriver();
        String binaryPath = driver.getBrowserBinaryPath(Platform.WINDOWS,
                Browser.CHROME, "21", properties);
        Assert.assertNull(binaryPath, "property not present");
    }
}
