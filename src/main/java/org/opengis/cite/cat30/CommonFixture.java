package org.opengis.cite.cat30;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import java.net.URI;
import java.util.EnumMap;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import javax.xml.validation.Schema;
import org.opengis.cite.cat30.util.ClientUtils;
import org.opengis.cite.cat30.util.HttpMessagePart;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.w3c.dom.Document;

/**
 * A supporting base class that sets up a common test fixture. These
 * configuration methods are invoked before those defined in a subclass.
 */
public class CommonFixture {

    /**
     * Root test suite package (absolute path).
     */
    public static final String ROOT_PKG_PATH = "/org/opengis/cite/cat30/";
    /**
     * HTTP client component (JAX-RS Client API).
     */
    protected Client client;
    /**
     * Service capabilities document (csw:Capabilities).
     */
    protected Document cswCapabilities;
    /**
     * An immutable Schema object for validating all CSW 3.0 messages
     * (cswAll.xsd).
     */
    protected Schema cswSchema;
    /**
     * An immutable Schema object for validating Atom feeds/entries (RFC 4287,
     * Appendix B).
     */
    protected Schema atomSchema;
    /**
     * An HTTP request message.
     */
    protected ClientRequest request;
    /**
     * An HTTP response message.
     */
    protected ClientResponse response;
    /**
     * A Map containing information about an HTTP response message.
     */
    protected EnumMap<HttpMessagePart, Object> responseInfo;
    /**
     * A Map containing information about an HTTP request message.
     */
    protected EnumMap<HttpMessagePart, Object> requestInfo;

    /**
     * Initializes the common test fixture with the following objects:
     *
     * <ul>
     * <li>a client component for interacting with HTTP endpoints</li>
     * <li>the CSW message schema (obtained from the suite attribute
     * {@link org.opengis.cite.cat30.SuiteAttribute#CSW_SCHEMA}, a thread-safe
     * Schema object).</li>
     * <li>the Atom schema (obtained from the suite attribute
     * {@link org.opengis.cite.cat30.SuiteAttribute#ATOM_SCHEMA}, a thread-safe
     * Schema object).</li>
     * <li>the service capabilities document (obtained from the suite attribute
     * {@link org.opengis.cite.cat30.SuiteAttribute#TEST_SUBJECT}, which should
     * evaluate to a DOM Document node).</li>
     * </ul>
     *
     * @param testContext The test context that contains all the information for
     * a test run, including suite attributes.
     */
    @BeforeClass
    public void initCommonFixture(ITestContext testContext) {
        Object obj = testContext.getSuite().getAttribute(SuiteAttribute.CLIENT.getName());
        if (null != obj) {
            this.client = Client.class.cast(obj);
        }
        obj = testContext.getSuite().getAttribute(SuiteAttribute.TEST_SUBJECT.getName());
        if (null == obj) {
            throw new SkipException("Capabilities document not found in ITestContext.");
        }
        this.cswCapabilities = Document.class.cast(obj);
        obj = testContext.getSuite().getAttribute(SuiteAttribute.CSW_SCHEMA.getName());
        if (null == obj) {
            throw new SkipException("CSW schema not found in ITestContext.");
        }
        this.cswSchema = Schema.class.cast(obj);
        obj = testContext.getSuite().getAttribute(SuiteAttribute.ATOM_SCHEMA.getName());
        if (null == obj) {
            throw new SkipException("Atom schema not found in ITestContext.");
        }
        this.atomSchema = Schema.class.cast(obj);
    }

    @BeforeMethod
    public void clearMessages() {
        this.request = null;
        this.response = null;
        this.requestInfo = new EnumMap(HttpMessagePart.class);
        this.responseInfo = new EnumMap(HttpMessagePart.class);
    }

    /**
     * Obtains the (XML) response entity as a DOM Document. This convenience
     * method wraps a static method call to facilitate unit testing (Mockito
     * workaround).
     *
     * @param response A representation of an HTTP response message.
     * @param targetURI The target URI from which the entity was retrieved (may
     * be null).
     * @return A Document representing the entity.
     *
     * @see
     * ClientUtils#getResponseEntityAsDocument(com.sun.jersey.api.client.ClientResponse,
     * java.lang.String)
     */
    public Document getResponseEntityAsDocument(ClientResponse response,
            String targetURI) {
        return ClientUtils.getResponseEntityAsDocument(response, targetURI);
    }

    /**
     * Builds an HTTP request message that uses the GET method. This convenience
     * method wraps a static method call to facilitate unit testing (Mockito
     * workaround).
     *
     * @param endpoint A URI indicating the target resource.
     * @param qryParams A Map containing query parameters (may be null);
     * @param mediaTypes A list of acceptable media types; if not specified,
     * generic XML ("application/xml") is preferred.
     * @return A ClientRequest object.
     *
     * @see ClientUtils#buildGetRequest(java.net.URI, java.util.Map,
     * javax.ws.rs.core.MediaType...)
     */
    public ClientRequest buildGetRequest(URI endpoint,
            Map<String, String> qryParams, MediaType... mediaTypes) {
        return ClientUtils.buildGetRequest(endpoint, qryParams, mediaTypes);
    }

}
