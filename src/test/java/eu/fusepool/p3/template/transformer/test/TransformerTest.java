package eu.fusepool.p3.template.transformer.test;

import java.net.ServerSocket;
import java.util.Iterator;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jayway.restassured.RestAssured;

import eu.fusepool.p3.template.transformer.TemplateTransformerFactory;
import eu.fusepool.p3.transformer.server.TransformerServer;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import eu.fusepool.p3.transformer.client.Transformer;
import eu.fusepool.p3.transformer.client.TransformerClientImpl;
import eu.fusepool.p3.transformer.commons.Entity;
import eu.fusepool.p3.transformer.commons.util.WritingEntity;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TypedLiteralImpl;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.ontologies.FOAF;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.clerezza.rdf.ontologies.XSD;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.junit.Rule;

public class TransformerTest {
	
	// data used by the mock server
	final String MOCK_SERVER_DATA = "mock-server-data.ttl";
	final static String MOCK_SERVER_DATA_MIME_TYPE = "text/turtle";
	
    private static MimeType mockDataMimeType;
    static {
        try {
        	mockDataMimeType = new MimeType(MOCK_SERVER_DATA_MIME_TYPE);
        } catch (MimeTypeParseException ex) {
            Logger.getLogger(TransformerTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
	private static int mockPort = 0;
	private int transformerServerPort = 0;
    private byte[] mockServerDataSet;
    private String transformerBaseUri;
	
	
	@BeforeClass
	public static void setMockPort() {
		mockPort = findFreePort();
		
	}
    
	
	@Before
    public void setUp() throws Exception {
		// load the data for the mock server
		mockServerDataSet = IOUtils.toByteArray(getClass().getResourceAsStream(MOCK_SERVER_DATA));
		
		// set up the transformer
		transformerServerPort = findFreePort();
        transformerBaseUri = "http://localhost:" + transformerServerPort + "/";
        RestAssured.baseURI = transformerBaseUri;
        TransformerServer server = new TransformerServer(transformerServerPort);
        server.start(new TemplateTransformerFactory());
    
	}
	
	
	@Rule
    public WireMockRule wireMockRule = new WireMockRule(mockPort);
    
    //@Test
    public void testTurtleSupported()  throws MimeTypeParseException {
        Transformer t = new TransformerClientImpl(RestAssured.baseURI);
        Set<MimeType> types = t.getSupportedInputFormats();
        Assert.assertTrue("No supported Output format", types.size() > 0);
        boolean turtleFound = false;
        for (MimeType mimeType : types) {
            if (mockDataMimeType.match(mimeType)) {
                turtleFound = true;
            }
        }
        Assert.assertTrue("None of the supported output formats is turtle", turtleFound);
    }
        

    /**
     * The transformer receives data and a url from the client, fetches the data set from the url and applies a transformation.
     * @throws Exception
     */
	@Test
    public void testTransformation() throws Exception {
	    // Set up a service in the mock server to respond to a get request that must be sent by the transformer
		// on behalf of its client to fetch the data.
        stubFor(get(urlEqualTo("/data/" + MOCK_SERVER_DATA))
                .willReturn(aResponse()
                    .withStatus(HttpStatus.SC_OK)
                    .withHeader("Content-Type", "text/turtle")
                    .withBody(mockServerDataSet)));
        
        // prepare the client data
        final MGraph graphToEnrich = new SimpleMGraph();
        final UriRef res1 = new UriRef("http://example.org/res1");
        final GraphNode node1 = new GraphNode(res1, graphToEnrich);
        node1.addProperty(RDFS.label, new PlainLiteralImpl("This is resource 1"));
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Serializer.getInstance().serialize(baos, graphToEnrich, "text/turtle");
        final byte[] ttlData = baos.toByteArray();
        String dataUrl = "http://localhost:" + mockPort + "/data/" + MOCK_SERVER_DATA ;
        // the client sends a request to the transformer with the url of the data to be fetched
        String clientRequestUrl = RestAssured.baseURI+"?data="+URLEncoder.encode(dataUrl, "UTF-8");
        Transformer t = new TransformerClientImpl(clientRequestUrl);
        // the transformer fetches the data from the mock server, applies its transformation and sends the RDF result to the client
        {
            Entity response = t.transform(new WritingEntity() {

                @Override
                public MimeType getType() {
                    return mockDataMimeType;
                }

                @Override
                public void writeData(OutputStream out) throws IOException {
                    out.write(ttlData);
                }
            }, mockDataMimeType);

            // the client receives the response from the transformer
            Assert.assertEquals("Wrong media Type of response", mockDataMimeType.toString(), response.getType().toString());            
            InputStream in = response.getData();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while((line = reader.readLine()) != null){
                System.out.println(line);
            }
            
            final Graph responseGraph = Parser.getInstance().parse(response.getData(), "text/turtle");
            //checks for the presence of a specific property added by the transformer
            final Iterator<Triple> propertyIter = responseGraph.filter(res1, RDFS.comment, null);
            Assert.assertTrue("No specific property on res1 in response", propertyIter.hasNext());
            //verify that the data has been loaded from the (mock) server (one call)
            verify(1,getRequestedFor(urlEqualTo("/data/" + MOCK_SERVER_DATA)));
        }
                
	    
	}
    
	
	public static int findFreePort() {
        int port = 0;
        try (ServerSocket server = new ServerSocket(0);) {
            port = server.getLocalPort();
        } catch (Exception e) {
            throw new RuntimeException("unable to find a free port");
        }
        return port;
    }

}
