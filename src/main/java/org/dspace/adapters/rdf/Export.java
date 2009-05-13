package org.dspace.adapters.rdf;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Site;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.xml.sax.ContentHandler;

public class Export
{

    /**
     * @param args
     * @throws Throwable 
     * @throws TransformerConfigurationException 
     */
    public static void main(String[] args) throws Throwable
    {
        // create an options object and populate it
        CommandLineParser parser = new PosixParser();
        Options options = new Options();

        options.addOption("i", "id", true,
                "handle of community, collection or item to export to RDF");
        
        options.addOption("f", "format", true, "Format of Export: [rdfxml|atom]");

        options.addOption("h", "help", false, "help");
        
        CommandLine line = parser.parse(options, args);

        if (line.hasOption('h'))
        {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp(Export.class.getName(), options);
            System.exit(0);
        }
        
        DSpaceObject obj = null;
        
        Context ctx = new Context();
        
        if(line.hasOption("i"))
        {
            obj = HandleManager.resolveToObject(ctx, line.getOptionValue("i"));
        }
        
        if(obj == null)
        {
            obj = Site.find(ctx,0);
        }
        
        // Pickup a SAX Transformer
        SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();

        // SAX ContentHandler.
        TransformerHandler th = null;
        
        if(line.hasOption("f") && line.getOptionValue("f").equals("atom"))
            th = tf.newTransformerHandler(new StreamSource(Export.class.getResourceAsStream("atom.xsl")));
        else
            th = tf.newTransformerHandler(new StreamSource(Export.class.getResourceAsStream("identity.xsl")));
        
        // Stream out result of the transform to system out
        StreamResult streamResult = new StreamResult(System.out);
        th.setResult(streamResult);
   
        // Setup a DSpaceModelReader to parse the object to RDF/XML
        DSpaceXMLReader reader = new DSpaceXMLReader();
        
        reader.setContext(ctx);
 
        // Set the readers ContentHandler so that it can be used in Transform
        reader.setContentHandler((ContentHandler)th);
        
        reader.parse(obj);
        
        //System.out.println("");
        //System.out.println("");
        
        //reader.parse(Site.find(ctx,0));
        
    }

}
