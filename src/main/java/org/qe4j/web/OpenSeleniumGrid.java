package org.qe4j.web;

import java.io.IOException;
import java.net.URL;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;

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
 * @author Jeff Ekhardt <jekhardt> 2012-09-11
 *
 */
public class OpenSeleniumGrid {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
            .getLogger(OpenSeleniumGrid.class);

    /**
     * Extracts the grid node address from the web driver session
     *
     * @return the address of the grid node web driver is connected to
     * @throws JSONException
     * @throws IOException
     * @throws ClientProtocolException
     *
     * @return grid node address
     */
    public static String getNodeAddress(OpenWebDriver awd)
            throws JSONException, ClientProtocolException, IOException {
        if (awd.isLocal()) {
            return "127.0.0.1";
        }

        String gridUrl = awd.getGridUrl();

        // splitting up grid url (e.g. http://174.129.55.44:4444/wd/hub)
        String[] address = gridUrl.split("/")[2].split(":");
        String hostname = address[0];
        int port = new Integer(address[1]);
        HttpHost host = new HttpHost(hostname, port);

        // call the test session API to get the node data
        DefaultHttpClient client = new DefaultHttpClient();
        SessionId sessionId = ((RemoteWebDriver) awd.getWebDriver())
                .getSessionId();
        URL testSessionApi = new URL("http://" + hostname + ":" + port
                + "/grid/api/testsession?session=" + sessionId);
        BasicHttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest(
                "POST", testSessionApi.toExternalForm());
        HttpResponse response = client.execute(host, request);

        JSONObject object = new JSONObject(EntityUtils.toString(response
                .getEntity()));
        String proxy = object.getString("proxyId");
        log.debug("found proxy [{}] in selenium grid test session", proxy);
        String gridNodeAddress = proxy.split("//")[1].split(":")[0];

        return gridNodeAddress;
    }
}
