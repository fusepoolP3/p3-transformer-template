/*
 * Copyright 2014 Bern University of Applied Sciences.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.fusepool.p3.template.transformer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fusepool.p3.transformer.HttpRequestEntity;
import eu.fusepool.p3.transformer.RdfGeneratingTransformer;

/**
 * A data transformer. The data set URI is specified at construction.
 */
class TemplateTransformer extends RdfGeneratingTransformer {

	public static final String DATA_QUERY_PARAM = "data";
	
    private static final Logger log = LoggerFactory.getLogger(TemplateTransformer.class);
    
    final ExampleEnricher exampleEnricher;
    final String dataUrl;

    TemplateTransformer(ExampleEnricher exampleEnricher, String dataUrl) {
        this.exampleEnricher = exampleEnricher;
        this.dataUrl = dataUrl;
    }

    @Override
    public Set<MimeType> getSupportedInputFormats() {
        Parser parser = Parser.getInstance();
        try {
            Set<MimeType> mimeSet = new HashSet<MimeType>();
            for (String mediaFormat : parser.getSupportedFormats()) {           
              mimeSet.add(new MimeType(mediaFormat));
            }
            return Collections.unmodifiableSet(mimeSet);
        } catch (MimeTypeParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Takes from the client some RDF data and a URL to fetch more data to be used to enrich it.
     * Returns the original RDF data with the enrichments.    
     */
    @Override
    protected TripleCollection generateRdf(HttpRequestEntity entity) throws IOException {
        String mediaType = entity.getType().toString();   
        Parser parser = Parser.getInstance();
        TripleCollection clientGraph = parser.parse( entity.getData(), mediaType);
        
        // graph containing the data feched by the url if provided.
        TripleCollection dataGraph = null;
        
        // Fetch the data from the url.
    	// The data url must be specified as a query parameter
    	log.info("Data Url : " + dataUrl);
    	if(dataUrl != null){
    		
    		dataGraph = fetchDataFromUrl(dataUrl);
    		// enrich the client data using the data fetched from the url
            try {
            	
            	exampleEnricher.enrich(dataGraph, clientGraph);    
                
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
    		
    	}
    	
        
        return clientGraph;
        
    }
    
    /**
     * Fetches the data from the url sent by the client transformer.
     * Transform the data into RDF, if in different format maybe using another transformer.
     * @param dataUrl
     * @return
     * @throws IOException
     */
    private TripleCollection fetchDataFromUrl(String dataUrl)throws IOException {
    	
        URL sourceUrl = new URL(dataUrl);
        URLConnection connection = sourceUrl.openConnection();
        InputStream in =  connection.getInputStream();
        
        return Parser.getInstance().parse(in, "text/turtle");
        
        
    	
    }
  
    @Override
    public boolean isLongRunning() {
        // downloading the dataset can be time consuming
        return true;
    }

}
