package org.qe4j.web;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.interactions.HasInputDevices;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.interactions.Keyboard;
import org.openqa.selenium.interactions.Mouse;
import org.openqa.selenium.Alert;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Platform;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.Select;

import com.gargoylesoftware.htmlunit.BrowserVersion;

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
 * OpenWebDriver is an implementation of the WebDriver interface to centrally
 * configure concrete WebDriver instances through a configuration file or system
 * properties for local and remote drivers, access to grids and remote grid
 * services, provide detailed logging per action, dynamic AJAX wait
 * functionality, and other helpful simplifications to improve efficiency with
 * forms and API-based user interfaces.
 *
 * @author Jeff Ekhardt <jekhardt> 2012-06-18
 *
 */
public class OpenWebDriver implements WebDriver, HasInputDevices {

    public enum Browser {
        FIREFOX,
        IEXPLORE,
        CHROME,
        HTMLUNIT;

        private static final Map<String, Browser> lookup = new HashMap<String, Browser>();

        static {
            for (Browser browser : EnumSet.allOf(Browser.class)) {
                lookup.put(browser.toString(), browser);
            }
        }

        public static Browser get(String browser) {
            return lookup.get(browser);
        }
    }

    public static final String PLATFORM_PROP_KEY = "webdriver.platform";
    public static final String BROWSER_PROP_KEY = "webdriver.browser";
    public static final String BROWSER_VERSION_PROP_KEY = "webdriver.browser-version";
    public static final String CHROME_DRIVER_PROP_KEY = "webdriver.chrome.driver";
    public static final String IEXPLORE_DRIVER_PROP_KEY = "webdriver.ie.driver";
    public static final String WAIT_PROP_KEY = "webdriver.wait";
    public static final String GRID_URL_PROP_KEY = "webdriver.grid.url";
    public static final String ASYNC_TIMEOUT_PROP_KEY = "webdriver.async.timeout";
    public static final String ASYNC_SLEEP_INTERVAL_PROP_KEY = "webdriver.async.sleep.interval";
    public static final String ASYNC_IDLE_PROP_KEY = "webdriver.async.idle";
    public static final String ASYNC_SLEEP_AFTER_PROP_KEY = "webdriver.async.sleep.after";
    public static final String ASYNC_ENABLED_PROP_KEY = "webdriver.async.enabled";
    public static final String SCREENSHOT_DIR_PROP_KEY = "webdriver.screenshot.directory";
    public static final String DOWNLOAD_DIR_PROP_KEY = "webdriver.download.directory";
    public static final String REMOTE_HOME_DIR_PROP_KEY = "webdriver.grid.node.homedir";
    public static final String BROWSER_MAC_CHROME_PROP_KEY = "webdriver.mac.chrome.";
    public static final String BROWSER_MAC_FIREFOX_PROP_KEY = "webdriver.mac.firefox.";
    public static final String BROWSER_LINUX_CHROME_PROP_KEY = "webdriver.linux.chrome.";
    public static final String BROWSER_LINUX_FIREFOX_PROP_KEY = "webdriver.linux.firefox.";
    public static final String BROWSER_WIN_CHROME_PROP_KEY = "webdriver.windows.chrome.";
    public static final String BROWSER_WIN_FIREFOX_PROP_KEY = "webdriver.windows.firefox.";
    public static final String BROWSER_WIN_INTERNETEXPLORER_PROP_KEY = "webdriver.windows.iexplore.";

    private static boolean chromeDriverInitialized;

    private final org.slf4j.Logger log = org.slf4j.LoggerFactory
            .getLogger(getClass());

    private WebDriver webDriver;
    private Browser browserType;
    private final Keyboard keyboard;
    private final Mouse mouse;
    private Properties properties;
    private String gridUrl;
    private boolean local;
    private boolean waitForAjaxEnabled;
    private int waitForAjaxTimeout;
    private int waitForAjaxIdle;
    private int waitForAjaxSleep;
    private int waitForAjaxSleepAfter;

    /**
     * Takes properties configuration and instantiates a local or remote
     * WebDriver depending on the configuration.
     *
     * @param properties
     * @throws IOException
     */
    public OpenWebDriver(Properties properties) throws IOException {
        setProperties(properties);
        initWaitForAjax();

        String browserProperty = properties.getProperty(BROWSER_PROP_KEY);
        Browser browser = Browser.get(browserProperty.toUpperCase());
        if (browser == null) {
            throw new IllegalArgumentException("browser property "
                    + browserProperty + " is not supported");
        }
        String platformProperty = properties.getProperty(PLATFORM_PROP_KEY)
                .toUpperCase();
        boolean local = false;
        if (platformProperty.toLowerCase().equals("local")) {
            local = true;

            // auto detect platform
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("linux")) {
                platformProperty = "linux";
            } else if (os.contains("mac")) {
                platformProperty = "mac";
            } else if (os.contains("win")) {
                platformProperty = "windows";
            }
        }
        setBrowserType(browser);
        String version = properties.getProperty(BROWSER_VERSION_PROP_KEY);
        Platform platform = lookupPlatform(platformProperty);

        if (local) {
            webDriver = initLocalWebDriver(browser, version, platform,
                    properties);
        } else {
            webDriver = initRemoteWebDriver(browser, version, platform,
                    properties);
        }

        keyboard = ((HasInputDevices) webDriver).getKeyboard();
        mouse = ((HasInputDevices) webDriver).getMouse();

        resetImplicitWaitTime();
    }

    /**
     * Resets the implicit wait time to the configured wait time.
     */
    public void resetImplicitWaitTime() {
        /*
         * TODO consider overrides to be configurable per application. Maybe
         * have a default wait, then a custom override in properties.
         */

        // configure default implicit wait time
        int wait = Integer.valueOf(properties.getProperty(WAIT_PROP_KEY));
        log.trace(
                "resetting implicit wait time to default configuration of {} seconds",
                wait);
        webDriver.manage().timeouts().implicitlyWait(wait, TimeUnit.SECONDS);
    }

    /**
     * Shortcut method to set implicit wait time.
     *
     * @param wait
     * @param timeUnit
     */
    public void setImplicitWaitTime(int wait, TimeUnit timeUnit) {
        log.trace("setting implicit wait time to {} {}", wait,
                timeUnit.toString());
        webDriver.manage().timeouts().implicitlyWait(wait, timeUnit);
    }

    public OpenWebDriver() {
        keyboard = null;
        mouse = null;
    }

    /**
     * Builds a local WebDriver based on properties.
     *
     * @param browser
     * @return local WebDriver
     * @throws IOException
     */
    protected WebDriver initLocalWebDriver(Browser browser, String version,
            Platform platform, Properties properties) throws IOException {
        local = true;
        log.info("attempting to instantiate local web driver using {}", browser);
        WebDriver driver = null;

        // determine the browser binary path key by version
        String browserBinaryPath = getBrowserBinaryPath(platform, browser,
                version, properties);

        DesiredCapabilities capabilities = null;
        switch (browser) {
            case FIREFOX:
            default:
                FirefoxProfile profile = getFireFoxProfile();
                // TODO investigate adding JSErrorCollector data extraction
                // JavaScriptError.addExtension(profile);
                driver = OpenWebDriver.newLocalFirefoxDriver(browserBinaryPath,
                        profile);
                break;
            case IEXPLORE:
                /*
                 * TODO investigate possibility of dynamically running
                 * non-default version of IE with IEDriverServer
                 */

                // pass driver path to system properties
                System.setProperty(IEXPLORE_DRIVER_PROP_KEY,
                        properties.getProperty(IEXPLORE_DRIVER_PROP_KEY));

                capabilities = DesiredCapabilities.internetExplorer();
                driver = new InternetExplorerDriver(capabilities);
                break;
            case HTMLUNIT:
                log.info("creating HtmlUnit driver emulating "
                        + "FireFox17 with javascript enabled");
                driver = new HtmlUnitDriver(BrowserVersion.FIREFOX_17);
                ((HtmlUnitDriver) driver).setJavascriptEnabled(true);
                break;
            case CHROME:
                initChromeDriver(properties);

                // enable testability of this without actually executing
                if (Boolean.parseBoolean(System
                        .getProperty("os.name.overriden"))) {
                    throw new IllegalStateException(
                            "os overriden so can't detect driver reliably");
                }

                capabilities = DesiredCapabilities.chrome();

                if (browserBinaryPath != null) {
                    capabilities.setCapability("chrome.binary",
                            browserBinaryPath);
                }

                initChromeProfile(capabilities);

                driver = new ChromeDriver(capabilities);
                break;
        }

        return driver;
    }

    /**
     * Start a new FireFox browser.
     *
     * @param browserBinaryPath
     * @param profile
     * @return WebDriver instance for FireFox
     */
    protected synchronized static WebDriver newLocalFirefoxDriver(
            String browserBinaryPath, FirefoxProfile profile) {
        WebDriver driver;
        if (browserBinaryPath != null) {
            FirefoxBinary binary = new FirefoxBinary(
                    new File(browserBinaryPath));
            driver = new FirefoxDriver(binary, profile);
        } else {
            driver = new FirefoxDriver(profile);
        }
        return driver;
    }

    /**
     * Builds a remote WebDriver suitable for use on Selenium Grid, configured
     * by properties.
     *
     * @param browser
     * @param platform
     * @param properties
     * @return RemoteWebDriver
     * @throws IOException
     */
    protected WebDriver initRemoteWebDriver(Browser browser, String version,
            Platform platform, Properties properties) throws IOException {

        // determine browser capabilities
        DesiredCapabilities capabilities = null;
        String browserCapabilityKey = null;
        switch (browser) {
            case FIREFOX:
            default:
                capabilities = DesiredCapabilities.firefox();
                FirefoxProfile profile = getFireFoxProfile();
                // JavaScriptError.addExtension(profile);
                // TODO add JSErrorCollector to maven dependency
                capabilities.setCapability(FirefoxDriver.PROFILE, profile);
                browserCapabilityKey = FirefoxDriver.BINARY;
                break;
            case IEXPLORE:
                capabilities = DesiredCapabilities.internetExplorer();
                /*
                 * TODO determine how to access different version of IE on
                 * remote server
                 */
                break;
            case HTMLUNIT:
                capabilities = DesiredCapabilities.htmlUnit();
                break;
            case CHROME:
                capabilities = DesiredCapabilities.chrome();
                browserCapabilityKey = "chrome.binary";
                initChromeProfile(capabilities);
                break;
        }
        capabilities.setJavascriptEnabled(true);
        capabilities.setPlatform(platform);
        capabilities.setBrowserName(browser.toString().toLowerCase());
        capabilities.setVersion(version);

        // determine the browser binary path key by version
        String browserBinaryPath = getBrowserBinaryPath(platform, browser,
                version, properties);
        if (browserBinaryPath != null) {
            capabilities.setCapability(browserCapabilityKey, browserBinaryPath);
        } // else use default

        /*
         * TODO remote grid system setup for setting IE and Chrome drivers
         * http://element34.ca/blog/iedriverserver-webdriver-and-python
         */

        /*
         * TODO add safari support
         * http://code.google.com/p/selenium/wiki/SafariDriver
         */

        // define a name to appear in SauceLabs for the activity of this driver
        // TODO investigate making this more configurable
        int timestamp = (int) (System.currentTimeMillis() / 1000);
        capabilities.setCapability("name", browser + "-" + version + " on "
                + platform + " time " + timestamp);

        // create remote web driver
        WebDriver remoteDriver = null;
        gridUrl = properties.getProperty(GRID_URL_PROP_KEY);
        if (gridUrl != null && !gridUrl.equals("")) {
            remoteDriver = OpenWebDriver.newRemoteWebDriver(gridUrl,
                    capabilities);
        } else {
            // TODO remoteDriver = initSauceWebDriver(properties, capabilities);
        }

        return remoteDriver;
    }

    protected static synchronized RemoteWebDriver newRemoteWebDriver(
            String gridUrl, Capabilities capabilities)
            throws MalformedURLException {
        return new RemoteWebDriver(new URL(gridUrl), capabilities);
    }

    /**
     * Constructs the property key to lookup the browser binary path.
     *
     * @param platform
     * @param browser
     * @param version
     * @return property key to lookup browser binary else null if the binary is
     *         not found or definition indicates it should use the default
     *         binary
     */
    protected String getBrowserBinaryPath(Platform platform, Browser browser,
            String version, Properties properties) {
        // determine platform part of property key
        String platformKeyPart = null;
        if (platform.is(Platform.LINUX)) {
            platformKeyPart = "linux";
        } else if (platform.is(Platform.WINDOWS)) {
            platformKeyPart = "win";
        } else if (platform.is(Platform.MAC)) {
            platformKeyPart = "mac";
        } else {
            throw new IllegalArgumentException("platform " + platform
                    + " not currently supported");
        }

        String browserBinaryPropertyKey = "webdriver." + platformKeyPart + "."
                + browser.toString().toLowerCase() + "." + version;
        log.info("looking up webdriver browser binary path property {}",
                browserBinaryPropertyKey);
        String browserBinary = properties.getProperty(browserBinaryPropertyKey);

        if (browserBinary != null) {
            if (browserBinary.equals("")) {
                browserBinary = null;
            } else {
                log.info("using browser binary {}", browserBinary);
            }
        }

        return browserBinary;
    }

    /**
     * Creates a remote webdriver configured to use Sauce Labs.
     *
     * @param properties
     * @param capability
     * @return webdriver instance for Sauce Labs
     * @throws MalformedURLException
     */
    protected WebDriver initSauceWebDriver(Properties properties,
            DesiredCapabilities capability) throws MalformedURLException {
        String sauceUser = properties.getProperty("saucelabs.user");
        String sauceKey = properties.getProperty("saucelabs.key");
        String sauceUrl = properties.getProperty("saucelabs.url");
        WebDriver remoteDriver = new RemoteWebDriver(new URL("http://"
                + sauceUser + ":" + sauceKey + "@" + sauceUrl), capability);
        return remoteDriver;
    }

    /**
     * Sets the webdriver.chrome.driver with the full absolute OS-specific
     * binary path. Assumes the webdriver.chrome.driver property has been set
     * with a basic file name with relative base path to the bin directory (e.g.
     * chromedriver, and if you're running on mac you'll get chromedriver-mac
     * auto selected.
     *
     * @param properties
     */
    protected void initChromeDriver(Properties properties) {
        if (chromeDriverInitialized) {
            return;
        }
        // assuming bin directory is peer to config directory
        String chromePath = new File(
                properties.getProperty(CHROME_DRIVER_PROP_KEY))
                .getAbsolutePath();

        // determine which binary to execute based on OS
        String os = System.getProperty("os.name").toLowerCase();
        String osSuffix = "-";
        if (os.contains("linux")) {
            osSuffix += "linux64";
        } else if (os.contains("mac")) {
            osSuffix += "mac";
        } else if (os.contains("win")) {
            osSuffix += "win.exe";
        }

        // set the appropriate OS-specific chrome binary
        String chromeBinary = chromePath + osSuffix;
        log.debug("setting System property {} to {}", CHROME_DRIVER_PROP_KEY,
                chromeBinary);
        if (!new File(chromeBinary).isFile()) {
            throw new IllegalArgumentException(
                    "chromeBinary path constructed [" + chromeBinary
                            + "] does not match an existing file");
        }
        System.setProperty(CHROME_DRIVER_PROP_KEY, chromeBinary);
        chromeDriverInitialized = true;
    }

    /**
     * Add profile configuration to desired capabilities for Chrome. Modifies
     * the specified capabilities object directly.
     *
     * @param capabilities
     * @return updated desired capabilities
     */
    protected DesiredCapabilities initChromeProfile(
            DesiredCapabilities capabilities) {
        Map<String, String> prefs = new HashMap<String, String>();
        prefs.put("download.prompt_for_download", "false");

        // directory has to be absolute path
        prefs.put("download.default_directory", System.getenv("HOME")
                + File.separator + getDownloadDir());
        prefs.put("download.extensions_to_open", "pdf");

        capabilities.setCapability("chrome.prefs", prefs);

        return capabilities;
    }

    /**
     * Construct a new fire fox profile to set auto download to desktop.
     *
     * @return FirefoxProfile
     */
    protected FirefoxProfile getFireFoxProfile() {
        FirefoxProfile profile = new FirefoxProfile();
        profile.setPreference("browser.download.dir", getDownloadDir());
        profile.setPreference("browser.download.folderList", 0); // desktop
        profile.setPreference("browser.download.manager.showWhenStarting",
                false);
        profile.setPreference("browser.helperApps.neverAsk.saveToDisk",
                "application/octet-stream,application/zip");
        return profile;
    }

    /**
     * Initialize and validate wait for ajax configuration.
     */
    protected void initWaitForAjax() {
        waitForAjaxEnabled = Boolean.parseBoolean(properties
                .getProperty(ASYNC_ENABLED_PROP_KEY));
        if (waitForAjaxEnabled) {
            waitForAjaxTimeout = new Integer(
                    properties.getProperty(ASYNC_TIMEOUT_PROP_KEY));
            if (waitForAjaxTimeout < 1) {
                throw new IllegalArgumentException(ASYNC_TIMEOUT_PROP_KEY
                        + " must be one or more");
            }

            waitForAjaxIdle = new Integer(
                    properties.getProperty(ASYNC_IDLE_PROP_KEY));
            if (waitForAjaxIdle < 0) {
                throw new IllegalArgumentException(ASYNC_IDLE_PROP_KEY
                        + " must be zero or more");
            }

            waitForAjaxSleep = new Integer(
                    properties.getProperty(ASYNC_SLEEP_INTERVAL_PROP_KEY));
            if (waitForAjaxSleep < 1) {
                throw new IllegalArgumentException(
                        ASYNC_SLEEP_INTERVAL_PROP_KEY + " must be one or more");
            }

            waitForAjaxSleepAfter = new Integer(
                    properties.getProperty(ASYNC_SLEEP_AFTER_PROP_KEY));
            if (waitForAjaxSleepAfter < 0) {
                throw new IllegalArgumentException(ASYNC_SLEEP_AFTER_PROP_KEY
                        + " must be zero or more");
            }
        }
    }

    /**
     * Converts the property definition of the platform to the Selenium enum
     * representation of it. Throws exception if the platform is not supported
     * by SauceLabs (current remote execution environment).
     *
     * @param platformProperty
     * @return Selenium platform enum
     */
    public static Platform lookupPlatform(String platformProperty) {
        String platform = platformProperty.toLowerCase();
        if (platform.equals("xp")) {
            return Platform.XP;
        } else if (platform.equals("vista")) {
            return Platform.VISTA;
        } else if (platform.equals("linux")) {
            return Platform.LINUX;
        } else if (platform.equals("windows")) {
            return Platform.WINDOWS;
        } else if (platform.equals("mac")) {
            return Platform.MAC;
        } else {
            throw new IllegalArgumentException("platform property "
                    + platformProperty
                    + " is not currently supported for remote web driver");
        }
    }

    public Browser getBrowserType() {
        return browserType;
    }

    public void setBrowserType(Browser browserType) {
        this.browserType = browserType;
    }

    @Override
    public void close() {
        webDriver.close();
    }

    /**
     * @return ApigeeWebElement with additional functionality
     */
    @Override
    public OpenWebElement findElement(By arg0) {
        log.debug("findElement by {}...", arg0);
        WebElement element = null;
        try {
            element = webDriver.findElement(arg0);
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("Failed to find element by "
                    + arg0 + " ." + e.getMessage());
        }
        return new OpenWebElement(this, element);
    }

    /**
     * @return List<ApigeeWebElement> with additional functionality
     */
    @Override
    public List<WebElement> findElements(By arg0) {
        log.debug("findElements by {}...", arg0);
        List<WebElement> original = webDriver.findElements(arg0);
        List<WebElement> elements = new ArrayList<WebElement>();
        for (int i = 0; i < original.size(); i++) {
            elements.add(new OpenWebElement(this, original.get(i)));
        }
        return elements;
    }

    /**
     * @return ApigeeWebElement with additional functionality that is also
     *         visible
     */
    public WebElement findVisibleElement(By arg0) {
        List<WebElement> elements = findElements(arg0);
        for (WebElement element : elements) {
            if (element.isDisplayed()) {
                return element;
            }
        }
        if (elements.size() > 0) {
            throw new NoSuchElementException(
                    "An element was found but was not visible identifiedy by "
                            + arg0.toString());
        }
        return null;
    }

    /**
     * Finds visible elements.
     *
     * @param by
     * @return List<ApigeeWebElement> that are visible
     */
    public List<WebElement> findVisibleElements(By by) {
        List<WebElement> visibleElements = new ArrayList<WebElement>();
        List<WebElement> allElements = findElements(by);
        for (WebElement element : allElements) {
            if (element.isDisplayed()) {
                visibleElements.add(element);
            }
        }
        return visibleElements;
    }

    @Override
    public void get(String arg0) {
        webDriver.get(arg0);
    }

    /**
     * Does a get and then clears alert.
     *
     * @param url
     */
    public void getForce(String url) {
        get(url);
        clearAlert();
    }

    @Override
    public String getCurrentUrl() {
        return webDriver.getCurrentUrl();
    }

    @Override
    public String getPageSource() {
        return webDriver.getPageSource();
    }

    @Override
    public String getTitle() {
        return webDriver.getTitle();
    }

    @Override
    public String getWindowHandle() {
        return webDriver.getWindowHandle();
    }

    @Override
    public Set<String> getWindowHandles() {
        if (webDriver == null) {
            throw new IllegalStateException();
        }
        return webDriver.getWindowHandles();
    }

    @Override
    public Options manage() {
        return webDriver.manage();
    }

    @Override
    public Navigation navigate() {
        return webDriver.navigate();
    }

    @Override
    public void quit() {
        webDriver.quit();
    }

    @Override
    public TargetLocator switchTo() {
        return webDriver.switchTo();
    }

    /**
     * Shortcut method to switching windows.
     *
     * @param handleIndex
     *            the number of the window handle index from a list returned by
     *            driver.getWindowHandles()
     * @return the window handle switched to
     */
    public String switchToWindow(int handleIndex) {
        log.info("switching to window handleIndex {}", handleIndex);
        List<String> list = new ArrayList<String>(getWindowHandles());
        String windowHandle = list.get(handleIndex);
        log.debug("switching to windowHandle {}", windowHandle);
        switchTo().window(windowHandle);
        return windowHandle;
    }

    /**
     * Switches to an alert window if present and accepts it.
     */
    public void clearAlert() {
        log.info("checking for alerts...");
        try {
            Alert alert = switchTo().alert();
            log.info("alert present: " + alert.getText());
            alert.accept();
            log.info("alert accepted");
        } catch (NoAlertPresentException e) {
            log.info("no alerts present, continuing...");
        }
    }

    /**
     * Creates a new instance of the web driver with the same properties as this
     * one.
     *
     * @return a new instance of the same type of web driver
     * @throws IOException
     */
    public OpenWebDriver newInstance() throws IOException {
        return new OpenWebDriver(properties);
    }

    /**
     * Creates a new action set based on the web driver.
     *
     * @return actions object
     */
    public Actions newActions() {
        return new Actions(webDriver);
    }

    public WebDriver getWebDriver() {
        return webDriver;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public Keyboard getKeyboard() {
        return keyboard;
    }

    @Override
    public Mouse getMouse() {
        return mouse;
    }

    public boolean isLocal() {
        return local;
    }

    public String getGridUrl() {
        return gridUrl;
    }

    /**
     * Uses Selenium Actions to move the mouse over the center of the element
     * specified.
     *
     * @param by
     */
    public void mouseOver(By by) {
        Actions actions = new Actions(webDriver);
        WebElement element = findElement(by);
        log.info("mouse over " + OpenWebElement.extractElementInfo(element));
        Dimension dimension = element.getSize();
        actions.moveToElement(element, dimension.width / 2,
                dimension.height / 2);
        actions.build().perform();
    }

    public Object executeJavaScript(String script) {
        JavascriptExecutor javascript = (JavascriptExecutor) webDriver;
        Object response = null;
        log.trace("executing javascript script: [{}]", script);
        try {
            response = javascript.executeScript(script);
        } catch (Exception e) {
            log.error("javascript failed to execute [{}]", script);
            throw new WebDriverException("javascript failed to execute: "
                    + e.getMessage(), e);
        }
        log.trace("javascript response [{}]", response.toString());
        return response;
    }

    /**
     * Overload of waitForAjax() which prints a message about the element it's
     * waiting for.
     *
     * @param element
     */
    public void waitForAjax(WebElement element) {
        log.debug("checking for ajax calls to compelete after interacting with "
                + "element " + element.toString());
        waitForAjax();
    }

    /**
     * Waits for all asynchronous calls to complete by checking javascript
     * framework used by the product to have zero active connections. Increases
     * sleep interval time as duration increases, finally using configured sleep
     * interval after 30 seconds.
     *
     * For jQuery products, resets counter jQuery.activeError to zero at the end
     * of each wait.
     */
    public void waitForAjax() {
        if (!waitForAjaxEnabled) {
            return;
        }
        int timeout = waitForAjaxTimeout;
        int idle = waitForAjaxIdle;
        int sleep = 1;
        int sleepAfter = waitForAjaxSleepAfter;
        int duration = 0;
        StringBuilder jsQuery = new StringBuilder();
        jsQuery.append("return (");
        jsQuery.append("(typeof jQuery === 'undefined' || jQuery == null) ? 0 : ");
        jsQuery.append("((jQuery.active == 0) ? 0 : (jQuery.active - ");
        jsQuery.append("((jQuery.activeError == undefined) ? 0 : jQuery.activeError)");
        jsQuery.append(")))");
        while (true) {
            // wrap in a try block in case javascript executes fail
            long ajaxActive = 0;
            try {
                ajaxActive = (Long) executeJavaScript(jsQuery.toString());
                log.debug("ajaxActive count: {}", ajaxActive);

                if (ajaxActive < 0) {
                    log.warn("connection errors more than active connections");
                }

                // when active calls are zero wait in a cooling off period
                if (ajaxActive <= 0) {
                    log.debug("ajax active calls is zero");
                    if (idle > 0) {
                        log.debug("sleeping {} seconds before confirming idle state...");
                        try {
                            Thread.sleep(idle * 1000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    // TODO change idles and other waits to milliseconds

                    // if ajax remains inactive, wait is complete
                    ajaxActive = (Long) executeJavaScript(jsQuery.toString());
                    log.debug("ajaxActive count: {}", ajaxActive);
                    if (ajaxActive <= 0) {
                        log.info("confirmed no ajax calls currently pending");
                        break;
                    }
                }

                // increase sleep interval as duration increases
                if (duration < 15) {
                    sleep = 1;
                } else if (duration < 30) {
                    sleep = 2;
                } else if (duration < 45) {
                    sleep = 3;
                } else {
                    sleep = waitForAjaxSleep;
                }
            } finally {
                // only poll if there are active connections
                if (ajaxActive > 0) {
                    try {
                        if (duration >= timeout) {
                            String message = "timeout waiting for active ajax count to be zero,"
                                    + " currently "
                                    + ajaxActive
                                    + " active after " + duration + " seconds";
                            log.warn(message);
                            throw new TimeoutException(message);
                        }

                        // updating polling
                        log.debug("sleeping for {} seconds...", sleep);
                        Thread.sleep(sleep * 1000);

                        duration += sleep;
                        log.debug("...polled for " + duration + " seconds");

                    } catch (Exception e) {
                        // this is not typically a fatal error to timeout
                        // TODO add configurable to let exception through
                        break;
                    }
                }
            }
        }
        if (sleepAfter > 0) {
            log.debug("sleeping for {} seconds after async idle...", sleepAfter);
            try {
                Thread.sleep(sleepAfter * 1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Takes a screenshot and saves it to the configured directory, else the
     * current working directory under a folder name specified.
     *
     * @param folderName
     *            name of folder to store files, which could be an absolute path
     *            starting with / or directly in the current working directory
     *            if null or in a subfolder if a single directory name
     * @param fileName
     *            without extension
     * @throws IOException
     */
    public void screenshot(String folderName, String fileName)
            throws IOException {
        if (folderName == null || !folderName.startsWith("/")) {
            // establish base working directory for screenshots
            String baseDirName = properties
                    .getProperty(SCREENSHOT_DIR_PROP_KEY);
            if (baseDirName == null) {
                baseDirName = "./";
            }

            // update base directory to be full path
            File baseDir = new File(baseDirName);
            baseDirName = baseDir.getCanonicalPath();

            // make base directory if it doesn't exist
            if (!baseDir.exists()) {
                baseDir.mkdirs();
            }

            // determine whether subfolder should be added into the file path
            if (folderName == null) {
                folderName = "";
            } else {
                // normalize the sub folder argument
                folderName = File.separator
                        + folderName.replaceAll(File.separator, "");

                // make the screenshot folder in the base directory
                folderName = baseDirName + folderName;
                File subFolder = new File(folderName);
                if (!subFolder.exists()) {
                    subFolder.mkdir();
                }
            }
        } else {
            // if folder name starts with / then use absolute path
            folderName = new File(folderName).getCanonicalPath();
        }

        // update file name to include absolute path
        fileName = folderName + File.separator + fileName + ".png";

        // take screenshot
        WebDriver screenshotDriver = webDriver;
        if (webDriver.getClass().equals(RemoteWebDriver.class)) {
            WebDriver augment = new Augmenter().augment(webDriver);
            screenshotDriver = augment;
        }

        File srcFile = ((TakesScreenshot) screenshotDriver)
                .getScreenshotAs(OutputType.FILE);
        log.debug("screenshot srcFile: {}", srcFile.getCanonicalPath());

        // store screen shot in specified location
        FileUtils.copyFile(srcFile, new File(fileName));
        log.info("saved screenshot " + fileName);
    }

    public static boolean isChromeDriverInitialized() {
        return chromeDriverInitialized;
    }

    public static void setChromeDriverInitialized(
            boolean chromeDriverInitialized) {
        OpenWebDriver.chromeDriverInitialized = chromeDriverInitialized;
    }

    /**
     * Simplifies clicking an element by combining find and click into one
     * method.
     *
     * @param by
     *            web driver identifier
     * @return WebElement clicked on
     */
    public WebElement clickElement(By by) {
        OpenWebElement webElement = findElement(by);
        log.debug("found element {}", by);
        webElement.click();
        return webElement;
    }

    /**
     * Simplifies clicking an element by combining find and click into one
     * method and uses the clickNoWait method to skip dynamic polling.
     *
     * @param by
     *            web driver identifier
     * @return WebElement clicked on
     */
    public WebElement clickElementNoWait(By by) {
        OpenWebElement webElement = findElement(by);
        webElement.clickNoWait();
        return webElement;
    }

    /**
     * Finds and gets input value attribute of specified element.
     *
     * @param by
     *            web driver identifier
     *
     * @return String value else null if no element is found to be selected
     */
    public String getInputValue(By by) {
        WebElement webElement = null;
        List<WebElement> elements = findElements(by);

        // for single elements, check if it's a select menu
        if (elements.size() == 1) {
            if (elements.get(0).getTagName().equals("select")) {
                webElement = new Select(elements.get(0))
                        .getFirstSelectedOption();
            } else {
                webElement = elements.get(0);
            }
        } else {
            // search for the selected value to return
            if (elements.get(0).getAttribute("type").equals("radio")
                    || elements.get(0).getAttribute("type").equals("checkbox")) {
                for (WebElement element : elements) {
                    if (element.isSelected()) {
                        webElement = element;
                        break;
                    }
                }
            } else {
                log.warn("multiple elements found, but unable to process more than the first");
                webElement = elements.get(0);
            }
        }

        // return null if nothing is selected
        if (webElement == null) {
            log.warn(
                    "no input element was found to be selected based on locator {}",
                    by.toString());
            return null;
        }

        // normalize empty string values to nulls as they would be defined in
        // most expected output objects
        String value = webElement.getAttribute("value");
        if (value.equals("")) {
            return null;
        }
        return value;
    }

    /**
     * Finds and sends input to field only if the value specified is not null.
     * If field is already populated with something (e.g. a text field), the
     * value specified will be appended to the existing value in the field.
     *
     * @param by
     *            web driver identifier
     * @param value
     *
     * @return null if value is null, else the WebElement operated on
     */
    public WebElement sendInput(By by, Object value) {
        if (value == null) {
            return null;
        }
        log.info("sending input value [{}] to element [{}]...",
                value.toString(), by.toString());
        return setInput(by, value, false);
    }

    /**
     * Finds and edits field's values only if the value specified is not null.
     * Clears input element and then sends keys.
     *
     * @param by
     *            web driver identifier
     * @param value
     *
     * @return null if value is null, else the WebElement operated on
     */
    public WebElement editInput(By by, Object value) {
        if (value == null) {
            return null;
        }
        log.info("editing input value [{}] to element [{}]...",
                value.toString(), by.toString());
        return setInput(by, value, true);
    }

    /**
     * Finds the input element specified and sets the value on it depending on
     * the value and whether it should be cleared first.
     *
     * @param by
     *            web driver identifier
     * @param value
     * @param clear
     *            whether to clear the field before setting the input
     * @return null if value is null, else the WebElement operated on
     */
    private WebElement setInput(By by, Object value, boolean clear) {
        OpenWebElement webElement = null;
        if (value != null) {
            // get all elements in case of radios or other multi input types
            List<WebElement> elements = findElements(by);
            if (elements.size() == 0) {
                throw new NoSuchElementException(
                        "input element not found with by: " + by.toString());
            }
            webElement = (OpenWebElement) elements.get(0);

            // identifying string used for log messages
            String elementInfo = OpenWebElement.extractElementInfo(webElement);

            // process elements by tag name
            String tagName = webElement.getTagName().toLowerCase();
            if (tagName.toLowerCase().equals("input")) {

                // process elements of input tag by type
                String type = webElement.getAttribute("type").toLowerCase();
                if (type.equals("text") || type.equals("password")
                        || type.equals("number") || type.equals("url")) {
                    if (clear) {
                        webElement.clear();
                    }
                    webElement.sendKeys(value.toString());
                } else if (type.equals("radio")) {
                    // search through radio elements for the matching value
                    for (WebElement element : elements) {
                        if (element.getAttribute("value").equals(
                                value.toString())) {
                            webElement = (OpenWebElement) element;
                            break;
                        }
                    }
                    log.info("clicking radio element value [{}]...",
                            value.toString());
                    webElement.clickNoWait();
                } else if (type.equals("checkbox")) {
                    Boolean check = (Boolean) value;
                    if (webElement.isSelected()) {
                        log.info("checkbox [{}] is currently selected...",
                                elementInfo);
                        if (!check) {
                            log.info("unchecking checkbox [{}]...", elementInfo);
                            webElement.clickNoWait();
                        }
                    } else {
                        log.info("checkbox [{}] is not currently selected...",
                                elementInfo);
                        if (check) {
                            log.info("checking checkbox [{}]...", elementInfo);
                            webElement.clickNoWait();
                        }
                    }
                } else {
                    throw new UnsupportedOperationException(
                            "not able to handle input type " + type);
                }
            } else if (tagName.equals("textarea")) {
                if (clear) {
                    webElement.clear();
                }
                webElement.sendKeys(value.toString());
            } else if (tagName.equals("select")) {
                log.info("selecting value [{}] from select menu [{}]...",
                        value.toString(), elementInfo);
                Select select = new Select(webElement);
                // TODO fix elementInfo displaying as obj ref for select
                select.selectByValue(value.toString());
            } else {
                throw new UnsupportedOperationException(
                        "not able to handle tag name " + tagName);
            }

        }
        return webElement;
    }

    /**
     * Finds and selects option by display text from specified select menu only
     * if the value specified is not null. In the case that the by argument is
     * ambiguous and finds more than one element, the first element is used.
     *
     * @param by
     *            web driver identifier
     * @param text
     *            the display text of the option to select
     *
     * @return null if value is null, else the WebElement operated on
     */
    public WebElement selectByText(By by, Object text) {
        if (text == null) {
            return null;
        }
        log.info(
                "selecting option by visible text [{}] from select menu [{}]...",
                text.toString(), by.toString());

        List<WebElement> elements = findElements(by);
        if (elements.size() == 0) {
            throw new NoSuchElementException(
                    "input element not found with by: " + by.toString());
        }

        OpenWebElement webElement = (OpenWebElement) elements.get(0);

        // TODO fix elementInfo displaying as obj ref for select
        String elementInfo = OpenWebElement.extractElementInfo(webElement);

        log.info("select text [{}] from menu [{}]...", text.toString(),
                elementInfo);
        Select select = new Select(webElement);
        select.selectByVisibleText(text.toString());

        return webElement;
    }

    // TODO multi checkbox selection
    // TODO multi select menus

    /**
     * Shortcut to refreshing the page and waits for AJAX.
     *
     * @throws InterruptedException
     */
    public void refresh() throws InterruptedException {
        log.info("refreshing current page {}...", getCurrentUrl());
        webDriver.navigate().refresh();
        waitForAjax();
    }

    /**
     * Shortcut to refreshing the page, clears alerts, and waits for AJAX.
     *
     * @throws InterruptedException
     */
    public void refreshForce() throws InterruptedException {
        log.info("refreshing current page {}...", getCurrentUrl());
        webDriver.navigate().refresh();
        clearAlert();
        waitForAjax();
    }

    /**
     * Checks if an element exists without any implicit wait. Temporarily
     * changes the driver implicit wait to nothing and then restores it after
     * the check. WARNING: It can be misleading to check if an element is found
     * when really you want to know if it is actually displayed. Use
     * isElementVisible if you really care whether the element is displayed.
     *
     * @param By
     *            element identifier
     * @return true if the element exists, and false if not
     */
    public boolean isElementFound(By by) {
        webDriver.manage().timeouts().implicitlyWait(10, TimeUnit.MILLISECONDS);
        WebElement webElement = null;
        try {
            webElement = findElement(by);
        } catch (NoSuchElementException e) {
            log.info("element not found by " + by);
        }
        resetImplicitWaitTime();
        if (webElement == null) {
            return false;
        }
        return true;
    }

    /**
     * Checks if an element exists and is visible without any implicit wait.
     * Temporarily changes the driver implicit wait to nothing and then restores
     * it after the check.
     *
     * @param By
     *            element identifier
     * @return true if the element exists and is visible, and false if not
     */
    public boolean isElementVisible(By by) {
        webDriver.manage().timeouts().implicitlyWait(10, TimeUnit.MILLISECONDS);
        WebElement webElement = null;
        try {
            webElement = findElement(by);
            if (webElement.isDisplayed()) {
                return true;
            } else {
                log.info("element was found but was not visible by " + by);
                return false;
            }
        } catch (NoSuchElementException e) {
            log.info("element not found by " + by);
        }
        resetImplicitWaitTime();
        if (webElement == null) {
            return false;
        }
        return true;
    }

    /**
     * @return the download directory name. This directory should exist in the
     *         current user's home directory.
     */
    public String getDownloadDir() {
        return properties.getProperty(DOWNLOAD_DIR_PROP_KEY);
    }

    /**
     * @return the remote driver home directory
     */
    public String getRemoteHomeDir() {
        return properties.getProperty(REMOTE_HOME_DIR_PROP_KEY);
    }
}
