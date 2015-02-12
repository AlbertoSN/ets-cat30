package org.opengis.cite.cat30.basic;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Validator;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmValue;
import org.opengis.cite.cat30.CAT3;
import org.opengis.cite.cat30.CommonFixture;
import org.opengis.cite.cat30.ETSAssert;
import org.opengis.cite.cat30.ErrorMessage;
import org.opengis.cite.cat30.ErrorMessageKeys;
import org.opengis.cite.cat30.Namespaces;
import org.opengis.cite.cat30.util.CSWClient;
import org.opengis.cite.cat30.util.ServiceMetadataUtils;
import org.opengis.cite.cat30.util.XMLUtils;
import org.opengis.cite.validation.RelaxNGValidator;
import org.opengis.cite.validation.ValidationErrorHandler;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Provides tests that apply to the <code>GetRecordById</code> request. This
 * request implements the abstract <em>GetResourceByID</em> operation defined in
 * the OGCWebService interface (OGC 06-121r9, Figure C.2).
 *
 * <p>
 * The KVP syntax must be supported; this encoding is generally used with the
 * GET method but may also be used with the POST method; this latter capability
 * will be advertised in the capabilities document as an operational constraint
 * as indicated below. The media type of a KVP request entity is
 * "application/x-www-form-urlencoded".
 * </p>
 *
 * <pre>{@literal
 *<Post xmlns="http://www.opengis.net/ows/2.0"
 *  xmlns:xlink="http://www.w3.org/1999/xlink"
 *  xlink:href="http://cat.example.org/csw">
 *  <Constraint name="PostEncoding">
 *    <AllowedValues>
 *      <Value>KVP</Value>
 *    </AllowedValues>
 *  </Constraint>
 *</Post>
 *}
 * </pre>
 *
 * <h6 style="margin-bottom: 0.5em">Sources</h6>
 * <ul>
 * <li>OGC 12-176r6, 7.4: GetRecordById operation</li>
 * <li>OGC 12-176r6, Table 16: Operation constraints</li>
 * <li>OGC 06-121r9, 7.4.7: OperationsMetadata section standard contents</li>
 * </ul>
 */
public class GetRecordByIdTests extends CommonFixture {

    /**
     * Service endpoint for GetRecordById using the GET method.
     */
    private URI getURI;
    /**
     * Service endpoint for GetRecordById using the POST method.
     */
    private URI postURI;
    /**
     * A list of record identifiers retrieved from the SUT.
     */
    private List<String> idList;

    /**
     * Finds the GET and POST method endpoints for the GetCapabilities request
     * in the capabilities document.
     *
     * @param testContext The test context containing various suite attributes.
     */
    @BeforeClass
    public void findRequestEndpoints(ITestContext testContext) {
        this.getURI = ServiceMetadataUtils.getOperationEndpoint(
                this.cswCapabilities, CAT3.GET_RECORD_BY_ID, HttpMethod.GET);
        this.postURI = ServiceMetadataUtils.getOperationEndpoint(
                this.cswCapabilities, CAT3.GET_RECORD_BY_ID, HttpMethod.POST);
    }

    /**
     * Submits a simple GetRecords request (with no filter criteria) and
     * extracts the record identifiers from the result set. Each csw:Record
     * element appearing in the response entity must contain at least one
     * dc:identifier element.
     *
     * @throws SaxonApiException In the unlikely event that the attempt to find
     * identifiers in the response entity fails.
     */
    @BeforeClass
    public void retrieveRecordIdentifiers() throws SaxonApiException {
        CSWClient cswClient = new CSWClient();
        cswClient.setServiceCapabilities(this.cswCapabilities);
        File results = cswClient.saveFullRecords(20, MediaType.APPLICATION_XML_TYPE);
        if (!results.isFile()) {
            throw new SkipException(
                    "Failed to save GetRecords response to temp file.");
        }
        this.idList = new ArrayList<>();
        Source src = new StreamSource(results);
        Map<String, String> nsBindings = Collections.singletonMap(Namespaces.DCMES, "dc");
        XdmValue value = XMLUtils.evaluateXPath2(src, "//dc:identifier", nsBindings);
        for (XdmItem item : value) {
            this.idList.add(item.getStringValue());
        }
    }

    /**
     * [Test] Verifies that a request for a record by identifier produces a
     * response with status code 404 (Not Found) if no matching resource
     * representation is found. A response entity (an exception report) is
     * optional; if present, the exception code shall be
     * "InvalidParameterValue".
     *
     * @see "OGC 12-176r6, 7.4.4.2: 7.4.4.2	Id parameter"
     */
    @Test(description = "Requirements: 127,141")
    public void getRecordById_noMatchingRecord() {
        MultivaluedMap<String, String> qryParams = new MultivaluedMapImpl();
        qryParams.add(CAT3.REQUEST, CAT3.GET_RECORD_BY_ID);
        qryParams.add(CAT3.SERVICE, CAT3.SERVICE_TYPE_CODE);
        qryParams.add(CAT3.VERSION, CAT3.SPEC_VERSION);
        qryParams.add(CAT3.ID, "urn:example:" + System.currentTimeMillis());
        WebResource resource = this.client.resource(this.getURI).queryParams(qryParams);
        WebResource.Builder builder = resource.accept(MediaType.APPLICATION_XML_TYPE);
        ClientResponse rsp = builder.get(ClientResponse.class);
        Assert.assertEquals(rsp.getStatus(),
                ClientResponse.Status.NOT_FOUND.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
    }

    /**
     * [Test] Verifies that a request for a record by identifier produces a
     * matching csw:SummaryRecord in the response entity. The default view
     * (element set) is "summary". The default output schema is identified by
     * the namespace name {@value org.opengis.cite.cat30.Namespaces#CSW}.
     *
     * @see "OGC 12-176r6, 7.4.4.2: Id parameter"
     * @see "OGC 12-176r6, 7.4.5: Response"
     */
    @Test(description = "Requirements: 124,134")
    public void getSummaryRecordById() {
        MultivaluedMap<String, String> qryParams = new MultivaluedMapImpl();
        qryParams.add(CAT3.REQUEST, CAT3.GET_RECORD_BY_ID);
        qryParams.add(CAT3.SERVICE, CAT3.SERVICE_TYPE_CODE);
        qryParams.add(CAT3.VERSION, CAT3.SPEC_VERSION);
        int randomIndex = ThreadLocalRandom.current().nextInt(this.idList.size());
        String id = this.idList.get(randomIndex);
        qryParams.add(CAT3.ID, id);
        WebResource resource = this.client.resource(this.getURI).queryParams(qryParams);
        WebResource.Builder builder = resource.accept(MediaType.APPLICATION_XML_TYPE);
        ClientResponse rsp = builder.get(ClientResponse.class);
        Assert.assertEquals(rsp.getStatus(),
                ClientResponse.Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        Document entity = rsp.getEntity(Document.class);
        String expr = String.format("/csw:SummaryRecord/dc:identifier = '%s'",
                id);
        ETSAssert.assertXPath(expr, entity, null);
    }

    /**
     * [Test] Verifies that a request for a brief record by identifier produces
     * a matching csw:BriefRecord in the response entity. The entity must be
     * schema-valid.
     *
     * @see "OGC 12-176r6, 7.4.4.1: ElementSetName parameter"
     */
    @Test(description = "Requirements: 123")
    public void getBriefRecordById() {
        MultivaluedMap<String, String> qryParams = new MultivaluedMapImpl();
        qryParams.add(CAT3.REQUEST, CAT3.GET_RECORD_BY_ID);
        qryParams.add(CAT3.SERVICE, CAT3.SERVICE_TYPE_CODE);
        qryParams.add(CAT3.VERSION, CAT3.SPEC_VERSION);
        qryParams.add(CAT3.ELEMENT_SET, CAT3.ELEMENT_SET_BRIEF);
        int randomIndex = ThreadLocalRandom.current().nextInt(this.idList.size());
        String id = this.idList.get(randomIndex);
        qryParams.add(CAT3.ID, id);
        WebResource resource = this.client.resource(this.getURI).queryParams(qryParams);
        WebResource.Builder builder = resource.accept(MediaType.APPLICATION_XML_TYPE);
        ClientResponse rsp = builder.get(ClientResponse.class);
        Assert.assertEquals(rsp.getStatus(),
                ClientResponse.Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        Document entity = rsp.getEntity(Document.class);
        String expr = String.format("/csw:BriefRecord/dc:identifier = '%s'",
                id);
        ETSAssert.assertXPath(expr, entity, null);
        Validator validator = this.cswSchema.newValidator();
        ETSAssert.assertSchemaValid(validator, new DOMSource(entity));
    }

    /**
     * [Test] Verifies that a request for a full record by identifier produces a
     * matching csw:Record in the response entity. The entity must be
     * schema-valid.
     *
     * @see "OGC 12-176r6, 7.4.4.1: ElementSetName parameter"
     */
    @Test(description = "Requirements: 123")
    public void getFullRecordById() {
        MultivaluedMap<String, String> qryParams = new MultivaluedMapImpl();
        qryParams.add(CAT3.REQUEST, CAT3.GET_RECORD_BY_ID);
        qryParams.add(CAT3.SERVICE, CAT3.SERVICE_TYPE_CODE);
        qryParams.add(CAT3.VERSION, CAT3.SPEC_VERSION);
        qryParams.add(CAT3.ELEMENT_SET, CAT3.ELEMENT_SET_FULL);
        int randomIndex = ThreadLocalRandom.current().nextInt(this.idList.size());
        String id = this.idList.get(randomIndex);
        qryParams.add(CAT3.ID, id);
        WebResource resource = this.client.resource(this.getURI).queryParams(qryParams);
        WebResource.Builder builder = resource.accept(MediaType.APPLICATION_XML_TYPE);
        ClientResponse rsp = builder.get(ClientResponse.class);
        Assert.assertEquals(rsp.getStatus(),
                ClientResponse.Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        Document entity = rsp.getEntity(Document.class);
        String expr = String.format("/csw:Record/dc:identifier = '%s'",
                id);
        ETSAssert.assertXPath(expr, entity, null);
        Validator validator = this.cswSchema.newValidator();
        ETSAssert.assertSchemaValid(validator, new DOMSource(entity));
    }

    /**
     * [Test] Verifies that a request for an Atom representation of a record
     * produces a matching atom:entry in the response entity. The
     * <code>Accept</code> request header indicates a preference for Atom
     * content; the outputFormat parameter is omitted (thus the header value
     * applies). The content of the entry must conform to RFC 4287.
     *
     * <p>
     * The atom:entry element is expected to include an atom:content element
     * that contains the usual collection of Dublin Core elements in accord with
     * the requested element set.
     * </p>
     *
     * <pre>{@literal
     *<entry xmlns="http://www.w3.org/2005/Atom">
     *  <!-- required entry elements -->
     *  <content type="application/xml" xml:lang="en"
     *   xmlns:dc="http://purl.org/dc/elements/1.1/">
     *    <dc:title>Title</dc:title>
     *    <dc:identifier>d4027a80-b308-11e4-ab27-0800200c9a66</dc:identifier>
     *  </content>
     *</entry>
     *}
     * </pre>
     *
     * <h6 style="margin-bottom: 0.5em">Sources</h6>
     * <ul>
     * <li>OGC 12-176r6, 7.4.4.4: outputSchema parameter</li>
     * <li><a href="https://tools.ietf.org/html/rfc4287" target="_blank">RFC
     * 4287</a>: The Atom Syndication Format</li>
     * </ul>
     */
    @Test(description = "Requirements: 003,135,140")
    public void getRecordByIdAsAtomEntry() {
        MultivaluedMap<String, String> qryParams = new MultivaluedMapImpl();
        qryParams.add(CAT3.REQUEST, CAT3.GET_RECORD_BY_ID);
        qryParams.add(CAT3.SERVICE, CAT3.SERVICE_TYPE_CODE);
        qryParams.add(CAT3.VERSION, CAT3.SPEC_VERSION);
        int randomIndex = ThreadLocalRandom.current().nextInt(this.idList.size());
        String id = this.idList.get(randomIndex);
        qryParams.add(CAT3.ID, id);
        WebResource resource = this.client.resource(this.getURI).queryParams(qryParams);
        WebResource.Builder builder = resource.accept(MediaType.APPLICATION_ATOM_XML_TYPE);
        ClientResponse rsp = builder.get(ClientResponse.class);
        Assert.assertEquals(rsp.getStatus(),
                ClientResponse.Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        Document entity = rsp.getEntity(Document.class);
        Map<String, String> nsBindings = Collections.singletonMap(Namespaces.ATOM, "atom");
        String expr = String.format("/atom:entry//dc:identifier = '%s'",
                id);
        ETSAssert.assertXPath(expr, entity, nsBindings);
        URL atomSchema = getClass().getResource(
                "/org/opengis/cite/cat30/rnc/atom.rnc");
        RelaxNGValidator rngValidator = null;
        try {
            rngValidator = new RelaxNGValidator(atomSchema);
            rngValidator.validate(new DOMSource(entity));
        } catch (SAXException | IOException ex) {
            Logger.getLogger(GetRecordByIdTests.class.getName()).log(
                    Level.WARNING, "Error attempting to validate Atom entry.", ex);
        }
        ValidationErrorHandler err = rngValidator.getErrorHandler();
        Assert.assertFalse(err.errorsDetected(),
                ErrorMessage.format(ErrorMessageKeys.NOT_SCHEMA_VALID,
                        err.getErrorCount(), err.toString()));
    }
}