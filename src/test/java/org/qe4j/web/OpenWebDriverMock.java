package org.qe4j.web;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Properties;

import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

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
 * Mock of web driver to force html unit driver to avoid errors opening other
 * browsers during unit testing.
 *
 * @author Jeff Ekhardt <jekhardt> 2012-06-19
 *
 */
public class OpenWebDriverMock extends OpenWebDriver {

    public OpenWebDriverMock(Properties properties) throws IOException {
        super(properties);
    }

    @Override
    protected WebDriver initLocalWebDriver(Browser browser, String version,
            Platform platform, Properties properties) {
        return new HtmlUnitDriver();
    }

    @Override
    protected WebDriver initRemoteWebDriver(Browser browser, String version,
            Platform platform, Properties properties)
            throws MalformedURLException {
        return new HtmlUnitDriver();
    }
}
