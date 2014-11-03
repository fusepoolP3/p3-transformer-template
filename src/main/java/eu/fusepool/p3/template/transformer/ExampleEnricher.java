package eu.fusepool.p3.template.transformer;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;

import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.ontologies.FOAF;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.eclipse.jetty.util.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enriches a graph with RDF data taken from a remote source. 
 * @author luigi
 *
 */
public class ExampleEnricher {
	
	private static final Logger log = LoggerFactory.getLogger(ExampleEnricher.class);
    
    /**
     * Takes an input RDF data and a URL of a data set to be used for the enrichment of the input data.  
     * @throws Exception 
     */
    public TripleCollection enrich(TripleCollection dataGraph, TripleCollection clientGraph) {
    	TripleCollection enrichmentsGraph = new SimpleMGraph();
        //Example enrichment: extracts a comment about res1 in the data graph
    	//then adds the comment about res1 in the client graph
        UriRef res1 = new UriRef("http://example.org/res1");
        if (dataGraph != null) {
	        Iterator<Triple> dataIter = dataGraph.filter(res1, RDFS.comment, null);
	        String comment = ((PlainLiteralImpl)dataIter.next().getObject()).getLexicalForm();	
	        log.info("A comment found about <http://example.org/res1>: " + comment );
	        enrichmentsGraph.add( new TripleImpl( res1, RDFS.comment, new PlainLiteralImpl(comment)) );
        }
        
        return enrichmentsGraph;
        
    }
    
 }
