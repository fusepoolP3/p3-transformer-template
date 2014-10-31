/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.fusepool.p3.template.transformer;

import eu.fusepool.p3.transformer.Transformer;
import eu.fusepool.p3.transformer.TransformerFactory;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author reto
 */
public class TemplateTransformerFactory implements TransformerFactory {

    private final Map<String, Transformer> data2Transformer = 
            new HashMap<>();
    private final ExampleEnricher dataEnricher;

    public TemplateTransformerFactory() throws IOException {
        this.dataEnricher = new ExampleEnricher();
    }
    
    @Override
    public Transformer getTransformer(HttpServletRequest request) {
        final String dataUri = request.getParameter("data");
        return getTransfomerFor(dataUri);
    }

    private synchronized Transformer getTransfomerFor(String dataUri) {
        if (data2Transformer.containsKey(dataUri)) {
            return data2Transformer.get(dataUri);
        }
        final Transformer newTransformer = new TemplateTransformer(dataEnricher, dataUri);
        data2Transformer.put(dataUri, newTransformer);
        return newTransformer;
    }
    
}
