package org.dspace.adapters.rdf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Date;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang.StringEscapeUtils;
import org.dspace.adapters.rdf.vocabularies.DC;
import org.dspace.adapters.rdf.vocabularies.DCTERMS;
import org.dspace.adapters.rdf.vocabularies.DS;
import org.dspace.adapters.rdf.vocabularies.ORE;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.core.ConfigurationManager;
import org.dspace.handle.HandleManager;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.sail.memory.MemoryStore;


public class DSpaceItemAdapter extends DSpaceObjectAdapter 
{

    public void handleNamespaces() throws RDFHandlerException
    {
        RDFHandler rdfHandler = getRDFHander();
        rdfHandler.handleNamespace("rdf", RDF.NAMESPACE);
        rdfHandler.handleNamespace("dc", DC.NAMESPACE);
        rdfHandler.handleNamespace("dcterms", DCTERMS.NAMESPACE);
        rdfHandler.handleNamespace("ds", DS.NAMESPACE);
        //rdfHandler.handleNamespace("ore", ORE.NAMESPACE);
    }

    public void handle(DSpaceObject object) throws RDFHandlerException
    {
        handle((Item) object);
    }

    private void handleResourceMap(Item item, Resource aggregation) throws RDFHandlerException, SQLException
    {
        
        URI rem = valueFactory.createURI(aggregation.stringValue(), "#rem");
        
        // describe type as resource map
        handleStatement(rem, RDF.TYPE, ORE.ResourceMap);
        
        XMLGregorianCalendar xmlCal = null;

        DCValue[] values = item.getMetadata("dc", "date", "accessioned", Item.ANY);

        if (values.length > 0)
        {
            // We use DCDate because SimpleDateFormat fails miserably on ISO 8601 subsets.
            Date created = new DCDate(values[0].value).toDate();
            handleStatement(rem, DCTERMS.created_, created);
        }

        Date lastModified = item.getLastModified();
        handleStatement(rem, DCTERMS.modified_, lastModified);

        handleStatement(rem, DC.creator_, ConfigurationManager.getProperty("dspace.name"));

        for (Bundle licenses : item.getBundles("LICENSE"))
        {
            for (Bitstream bitstream : licenses.getBitstreams())
            {
                handleStatement(rem, DCTERMS.license_, createResource(item,bitstream));

                getContext().removeCached(bitstream, bitstream.getID());
            }

            getContext().removeCached(licenses, licenses.getID());
        }

        for (Bundle licenses : item.getBundles("CC-LICENSE"))
        {
            for (Bitstream bitstream : licenses.getBitstreams())
            {
                if ("license_url".equals(bitstream.getName()))
                {
                    try
                    {
                        handleStatement(
                                rem, 
                                DCTERMS.license_,
                                valueFactory.createURI(getBitstream(bitstream)));
                    }
                    catch (Exception e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }
                getContext().removeCached(bitstream, bitstream.getID());
            }

            getContext().removeCached(licenses, licenses.getID());
        }

        getContext().clearCache();

        // identify aggregation
        handleStatement(rem, ORE.describes, aggregation);
    }
    
    @SuppressWarnings("deprecation")
    public void handle(Item item) throws RDFHandlerException
    {

        Resource aggregation = this.createResource(item);
        
        try
        {
            //this.handleResourceMap(item, aggregation);
            
            //this.handleResourceMap(rdfxml, item, aggregation);

            Bundle[] origBundles = item.getBundles("ORIGINAL");

            /**
             * generate aggregation of bitstreams.
             */
            //handleStatement(aggregation, RDF.TYPE, ORE.Aggregation);

            //handleStatement(aggregation, ORE.analogousTo, valueFactory
            //                .createURI(HandleManager.getCanonicalForm(item
            //                        .getHandle())));

            handleStatement(aggregation, RDF.TYPE, DS.Item);

            /**
             * Output metadata for Item
             */
            for (DCValue dc : item.getMetadata(Item.ANY, Item.ANY, Item.ANY,
                    Item.ANY))
            {

                /**
                 * if in dc and a recognizable real dc term, otherwise its in
                 * our DS space... (maybe make a fake space for these)
                 */
                if (dc.schema.equals("dc")
                        && (dc.element.equals("format") || dc.element
                                .equals("extent")))
                {
                    // suppress these elements
                }
                else if (dc.schema.equals("dc")
                        && !dc.element.equals("creator"))
                {
                    
                    URI predicate = null;

                    boolean isUrl = false;
                    
                    try
                    {
                        URL url = new URL(dc.value);
                        isUrl = true;
                    }
                    catch (MalformedURLException e)
                    {
                    }
                    /*
                     * Ok, we want dc elements namespace for unqualified dc and
                     * dcterms namespace for qualified, otherwise, if its
                     * non-standard put it into DSpace Namespace for now.
                     */
                    if (dc.qualifier != null)
                    {
                        predicate = DCTERMS.DSPACE_DC_2_DCTERMS.get(dc.element+ "." + dc.qualifier);
                        
                        if (predicate == null)
                        {
                            /*
                             * This happens if people make up unqualified elements or
                             * non-standard qualifiers.
                             */
                            predicate = valueFactory.createURI(DS.NAMESPACE, dc.qualifier);
                        }
                    } else if (predicate == null)
                    {
                        predicate = DC.DSPACE_DC_2_DC.get(dc.element);
                    }
                    

                    /** Finally output the statement */
                    if(isUrl)
                    {
                        handleStatement(aggregation, predicate, valueFactory.createLiteral(dc.value,DCTERMS.URI));
                    }
                    else
                    {
                        handleStatement(aggregation, predicate, dc.value);
                    }
                }
                else if (!dc.schema.equals("dc"))
                {

                    String lookup = dc.element;

                    if (dc.qualifier != null)
                        lookup += "." + dc.qualifier;

                    MetadataSchema schema;
                    try
                    {
                        schema = MetadataSchema.find(getContext(), dc.schema);

                        URI predicate = valueFactory.createURI(schema
                                .getNamespace(), lookup);

                        /** Finally output the statement */
                        handleStatement(aggregation, predicate, dc.value);
                    }
                    catch (SQLException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }

            }

            /*
             * =================================================================
             * List all the Collections this item is a part of.
             * =================================================================
             */
            try
            {
                for (Collection collection : item.getCollections())
                {
                    handleStatement(
                            aggregation, DS.isPartOfCollection, createResource(collection));

                    getContext().removeCached(collection, collection.getID());
                }

            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }

            
            Repository myRepository = new SailRepository(new MemoryStore());
            myRepository.initialize();
            RepositoryConnection connection = myRepository.getConnection();
            
            /*
             * generate our aggregates in the aggregation and prepare
             * any further resources for output.
             */
            for (Bundle bundle : origBundles)
            {
                for (Bitstream bitstream : bundle.getBitstreams())
                {
                    Resource uri = createResource(item, bitstream);
                 
                    /* write out the aggregates */
                    handleStatement(aggregation, DS.hasBitstream, uri);

                    /*
                     * cache the resources and write afterward so the rdfxml is
                     * prettier
                     */
                    connection.add(valueFactory.createStatement(uri,
                            DS.isPartOfItem, aggregation));
                    
                    connection.add(valueFactory.createStatement(uri,
                            RDF.TYPE, DS.Bitstream));
                    
                    if(bitstream.getName() != null)
                    {
                        connection.add(valueFactory.createStatement(uri, DC.title_,
                            valueFactory.createLiteral(StringEscapeUtils
                                    .escapeXml(bitstream.getName()))));
                    }
                    
                    if(bitstream.getDescription() != null)
                    {
                        connection.add(valueFactory.createStatement(uri, DC.description_,
                            valueFactory.createLiteral(StringEscapeUtils
                                    .escapeXml(bitstream.getDescription()))));
                    }
                    
                    connection.add(valueFactory.createStatement(uri, DS.size,
                            valueFactory.createLiteral(bitstream.getSize())));
                    
                    connection.add(valueFactory.createStatement(uri, DS.messageDigest,
                            valueFactory.createLiteral(StringEscapeUtils
                                    .escapeXml(bitstream.getChecksum()))));
                    
                    connection.add(valueFactory.createStatement(uri, DS.messageDigestAlgorithm,
                            valueFactory.createLiteral(StringEscapeUtils
                                    .escapeXml(bitstream.getChecksumAlgorithm()))));
                    

                    BitstreamFormat format = bitstream.getFormat();

                    Resource fu = createResource(format);
                    
                    connection.add(valueFactory.createStatement(uri,
                            DCTERMS.format_, fu));
                    
                    connection.add(valueFactory.createStatement(fu,
                            RDF.TYPE, DS.BitstreamFormat));
                    
                    connection.add(valueFactory.createStatement(fu,
                            RDF.TYPE, DCTERMS.FileFormat));
                    
                    connection.add(valueFactory.createStatement(fu,
                            RDF.VALUE, valueFactory
                                    .createLiteral(StringEscapeUtils
                                            .escapeXml(format.getMIMEType()))));
                    
                    connection.add(valueFactory.createStatement(fu,
                            DC.description_, valueFactory
                                    .createLiteral(StringEscapeUtils
                                            .escapeXml(format.getDescription()))));
                    
                    connection.add(valueFactory.createStatement(fu,
                            DC.title_, valueFactory
                                    .createLiteral(StringEscapeUtils
                                            .escapeXml(format.getShortDescription()))));
                    
                    String supportLevel = "Unknown";
                    
                    if (format.getSupportLevel() == BitstreamFormat.KNOWN)
                        supportLevel = "Known";
                    else if (format.getSupportLevel() == BitstreamFormat.SUPPORTED)
                        supportLevel = "Supported";
                    
                    connection.add(valueFactory.createStatement(fu,
                            DS.support, valueFactory
                                    .createLiteral(supportLevel)));
                    
                    
                    
                    
                    getContext().removeCached(bitstream, bitstream.getID());
                }

                getContext().removeCached(bundle, bundle.getID());
            }

            /*
             * serialize any complex graph structure created above
             */
            RepositoryResult<Statement> statements = connection.getStatements(null, null, null, true);

            try {
               while (statements.hasNext() && !statements.isClosed()) {
                  Statement st = statements.next();
                  handleStatement(st);
               }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally {
               statements.close(); // make sure the result object is closed properly
               connection.close();
               myRepository.shutDown();
            }
            

        }
        catch (SQLException e)
        {
            throw new RDFHandlerException(e.getMessage(), e);
        }
        catch (RepositoryException e)
        {
            throw new RDFHandlerException(e.getMessage(), e);
        }
    }

    private static String getBitstream(Bitstream b) throws IOException, SQLException,
            AuthorizeException
    {
        String line;
        BufferedReader in = new BufferedReader(new InputStreamReader(b
                .retrieve()));
        StringBuffer buffer = new StringBuffer();
        while ((line = in.readLine()) != null)
        {
            buffer.append(line);
        }
        return buffer.toString();
    }

    @Override
    public void handleChildren(DSpaceObject object) throws RDFHandlerException
    {
        // TODO Auto-generated method stub

    }
}
