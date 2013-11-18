package org.qe4j.web;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.internal.Coordinates;
import org.openqa.selenium.internal.Locatable;

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
 * @author Jeff Ekhardt <jekhardt> 2012-11-12
 *
 */
public class OpenWebElement implements WebElement, Locatable {

    protected final org.slf4j.Logger log = org.slf4j.LoggerFactory
            .getLogger(getClass());

    private WebElement webElement;
    private OpenWebDriver webDriver;

    public OpenWebElement(OpenWebDriver webDriver, WebElement webElement) {
        setWebDriver(webDriver);
        setWebElement(webElement);
    }

    /**
     * Extracts the identifying information from WebElement toString.
     *
     * <pre>
     * e.g. [[ChromeDriver: chrome on MAC (92494ca2cd47cb8eafa846bca4068ba5)] -> class name: platform]
     * </pre>
     *
     * @param webElement
     * @return log string (e.g. [element -> class name: platform])
     */
    public static String extractElementInfo(WebElement webElement) {
        String elementInfo = webElement.toString();
        int elementIndex = elementInfo.indexOf("->");
        if (elementIndex != -1) {
            elementInfo = elementInfo.substring(elementIndex);
        }
        return "[element " + elementInfo;
    }

    /**
     * The usual click, but with dynamic wait for ajax and logging about what's
     * being clicked.
     */
    @Override
    public void click() {
        log.info("click " + extractElementInfo(webElement));
        webElement.click();
        webDriver.waitForAjax(webElement);
    }

    /**
     * The usual WebDriver click without any additional dynamic waits.
     */
    public void clickNoWait() {
        log.info("click " + extractElementInfo(webElement));
        webElement.click();
    }

    /**
     * Clicks and does a hard sleep for the specified sleep time.
     *
     * @param sleep
     *            number of seconds to sleep before clicking
     * @throws InterruptedException
     */
    public void clickMinWait(int sleep) {
        log.debug("sleeping for {} seconds...", sleep);
        try {
            Thread.sleep(sleep * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        log.info("click " + extractElementInfo(webElement));
        webElement.click();
    }

    /**
     * The usual submit, but with dynamic wait for ajax.
     */
    @Override
    public void submit() {
        log.info("submit " + extractElementInfo(webElement));
        webElement.submit();
        webDriver.waitForAjax(webElement);
    }

    public void submitNoWait() {
        log.info("submit " + extractElementInfo(webElement));
        webElement.submit();
    }

    /**
     * Submits the form and does a hard sleep for the specified sleep time.
     *
     * @param sleep
     *            number of seconds to sleep before clicking
     * @throws InterruptedException
     */
    public void submitMinWait(int sleep) {
        log.debug("sleeping for {} seconds...", sleep);
        try {
            Thread.sleep(sleep * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        log.info("submit " + extractElementInfo(webElement));
        webElement.submit();
    }

    @Override
    public void sendKeys(CharSequence... keysToSend) {
        log.info("sendKeys {} to {}", keysToSend,
                extractElementInfo(webElement));
        webElement.sendKeys(keysToSend);
    }

    @Override
    public void clear() {
        log.info("clearing element {}", extractElementInfo(webElement));
        webElement.clear();
    }

    @Override
    public String getTagName() {
        return webElement.getTagName();
    }

    @Override
    public String getAttribute(String name) {
        return webElement.getAttribute(name);
    }

    @Override
    public boolean isSelected() {
        return webElement.isSelected();
    }

    @Override
    public boolean isEnabled() {
        return webElement.isEnabled();
    }

    @Override
    public String getText() {
        return webElement.getText();
    }

    /**
     * @return List<ApigeeWebElement> with additional functionality
     */
    @Override
    public List<WebElement> findElements(By by) {
        List<WebElement> original = webElement.findElements(by);
        List<WebElement> elements = new ArrayList<WebElement>();
        for (int i = 0; i < original.size(); i++) {
            elements.add(new OpenWebElement(webDriver, original.get(i)));
        }
        return elements;
    }

    /**
     * @return ApigeeWebElement with additional functionality
     */
    @Override
    public WebElement findElement(By by) {
        return new OpenWebElement(webDriver, webElement.findElement(by));
    }

    @Override
    public boolean isDisplayed() {
        return webElement.isDisplayed();
    }

    @Override
    public Point getLocation() {
        return webElement.getLocation();
    }

    @Override
    public Dimension getSize() {
        return webElement.getSize();
    }

    @Override
    public String getCssValue(String propertyName) {
        return webElement.getCssValue(propertyName);
    }

    public WebElement getWebElement() {
        return webElement;
    }

    public OpenWebElement setWebElement(WebElement webElement) {
        this.webElement = webElement;
        return this;
    }

    public OpenWebDriver getWebDriver() {
        return webDriver;
    }

    public void setWebDriver(OpenWebDriver webDriver) {
        this.webDriver = webDriver;
    }

    @Override
    public Coordinates getCoordinates() {
        return ((Locatable) webElement).getCoordinates();
    }

    /**
     * Performs a mouseover action relative to this element.
     *
     * @param by
     */
    public void mouseOver(By by) {
        Actions actions = new Actions(webDriver);
        WebElement element = webElement.findElement(by);
        Dimension dimension = element.getSize();
        actions.moveToElement(element, dimension.width / 2,
                dimension.height / 2);
        actions.build().perform();
    }

}
