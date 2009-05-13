package org.dspace.adapters.rdf;

import java.sql.SQLException;
import java.util.Date;

import org.dspace.adapters.rdf.vocabularies.DC;
import org.dspace.adapters.rdf.vocabularies.DCTERMS;
import org.dspace.adapters.rdf.vocabularies.DS;
import org.dspace.adapters.rdf.vocabularies.ORE;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Site;
import org.dspace.core.ConfigurationManager;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;


public class DSpaceSiteAdapter extends DSpaceObjectAdapter
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
    
    @Override
    public void handle(DSpaceObject object) throws RDFHandlerException
    {
        handle((Site) object);
    }
    
    private void handleResourceMap(DSpaceObject object, Resource aggregation) throws RDFHandlerException, SQLException
        { 

            //URI uri = valueFactory.createURI(aggregation.toString(),"#rem");
            
            // describe type as resource map
            //handleStatement(uri, RDF.TYPE, ORE.ResourceMap);

            //handleStatement(uri, DCTERMS.modified_, new Date());
            
            // describe type as resource map
            //handleStatement(uri,
            //        DC.creator_, ConfigurationManager.getProperty("dspace.name"));
            
            // identify aggregation
            //handleStatement(
            //        uri, ORE.describes, aggregation);
    }
    
    public void handle(Site object) throws RDFHandlerException
    {
        try
        {
            
            Resource aggregation = createResource(object);
            
            handleResourceMap(object, aggregation);    
              
            /* =================================================================
             * The following statements are typing for DCMI Collections
             * =================================================================
             */
            
            handleStatement(aggregation,
                    RDF.TYPE, DS.Site);
            
            // describe type as collection (seems appropriate for RDF)
            //handleStatement(aggregation,
            //        RDF.TYPE, DCMITYPE.Collection));

            // describe type as collection (specification says this is manditory)
            //handleStatement(aggregation,
            //        DC.type_, DCMITYPE.Collection));

            /* =================================================================
             * The following is a dc.identifier, title, creator and abstract for DCMI Collections 
             * =================================================================
             */
            // This isn't very useful because it isn't resolvable...
            //handleStatement(thisDSObject,
            //        DC.identifier_, valueFactory.createURI("hdl:" + object.getHandle())));

            // give it a title
            handleStatement(aggregation,
                    DC.title_, valueFactory.createLiteral(object.getName()));

            
            /* =================================================================
             * List all the Communities.
             * =================================================================
             */
            for(Community community : Community.findAllTop(getContext()))
            {
                handleStatement(
                        aggregation,
                        DS.hasCommunity, 
                        createResource(community));
            }

        }
        catch (SQLException e)
        {
            throw new RDFHandlerException(e.getMessage(), e);
        }
    }

    public void handleChildren(DSpaceObject object) throws RDFHandlerException 
    {
 
        try
        {
            for (Community comm : Community.findAll(getContext()))
            {
                getFactory().getAdapter(comm).handle(comm);
                getContext().removeCached(comm, comm.getID());
            }
           
        }
        catch (SQLException e)
        {
            throw new RDFHandlerException(e.getMessage(),e);
        }
    }
    
    


}
