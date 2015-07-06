# Dictionary Matcher Transformer [![Build Status](https://travis-ci.org/fusepoolP3/p3-transformer-template.svg)](https://travis-ci.org/fusepoolP3/p3-transformer-template/)

This template provides a starting code to make it easy to develop and test a transformer for the Fusepool P3 platform.
A transformer provides a service to transform and enrich a data set sent via HTTP POST by a client. The client can send
the url of another data set of service that will be used to accomplish the transformer's task. After receiving a POST request
the transformer starts a new job and sends a response with the location header where the client can fetch the result.
In the sample code a client sends to the transformer some RDF data about a resource and the url of a data source where additional
information can be found and added to the client data. The result graph is provided at the location specified in the location header.

Compile the project running the command

    mvn install

Start the server with the command

    mvn exec:java

A file with the client data and a file with additional information about a resource are provided in src/test/resource folder.
In order to test the transformer the client data will be used locally and the additional data will be fetched from the
project Github repository. The url of the remote source must be sent as a query parameter with 'data' as the parameter name.
From the src/test/resource folder run the following command 

    curl -i -X POST -H "Content-Type: text/turtle" -T client-data.ttl http://localhost:7001?data=https://raw.githubusercontent.com/fusepoolP3/p3-template-transformer/master/src/test/resources/eu/fusepool/p3/template/transformer/test/mock-server-data.ttl  
 
The command starts an asynchronous task and the server sends a response with the location header where the result will be made 
available to the client.

In the test class a mock server is created to serve the data requested by the transformer on behalf of the client.
