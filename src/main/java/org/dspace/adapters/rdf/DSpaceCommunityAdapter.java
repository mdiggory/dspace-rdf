package org.dspace.adapters.rdf;

import java.sql.SQLException;
import java.util.Date;


import org.dspace.adapters.rdf.vocabularies.DC;
import org.dspace.adapters.rdf.vocabularies.DCMITYPE;
import org.dspace.adapters.rdf.vocabularies.DCTERMS;
import org.dspace.adapters.rdf.vocabularies.DS;
import org.dspace.adapters.rdf.vocabularies.ORE;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Site;
import org.dspace.core.ConfigurationManager;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;


public class DSpaceCommunityAdapter extends DSpaceObjectAdapter
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
        handle((Community) object);
    }

    private void handleResourceMap(DSpaceObject object, Resource aggregation) throws RDFHandlerException, SQLException
    {
        
        //URI uri = valueFactory.createURI(aggregation.toString(), "#rem");

        // describe type as resource map
        //handleStatement(uri, RDF.TYPE, ORE.ResourceMap);
           
        //handleStatement(uri, DCTERMS.modified_, new Date());
        
        // describe type as resource map
        //handleStatement(uri, DC.creator_, ConfigurationManager.getProperty("dspace.name"));
        
        // identify aggregation
        //handleStatement(uri, ORE.describes, aggregation);
    }
    
    public void handle(Community object) throws RDFHandlerException
    {
        try
        {
            Resource aggregation = createResource(object);

            //this.handleResourceMap(object, aggregation); 
            
            /* =================================================================
             * The following statements are typing for DCMI Collections
             * =================================================================
             */
            
            handleStatement(aggregation,
                    RDF.TYPE, DS.Community);
            
            /* =================================================================
             * The following is a dc.identifier, title, creator and abstract for DCMI Collections 
             * =================================================================
             */
            handleStatement(aggregation,
                    DC.identifier_, valueFactory.createLiteral("hdl:" + object.getHandle(), DCTERMS.URI));


            // give it a title
            handleStatement(aggregation, DC.title_, object.getName());

            String shortDesc = object.getMetadata("short_description");
            if (shortDesc != null && !shortDesc.trim().equals(""))
            {
                shortDesc = cleanHTML(shortDesc);
                handleStatement(
                        aggregation, DCTERMS.abstract_, shortDesc);
            }

            String intro = object.getMetadata("introductory_text");
            if (intro != null && !intro.trim().equals("")){
                intro = cleanHTML(intro);
                handleStatement(
                        aggregation, DCTERMS.abstract_, intro);
            }
            
            
            /* =================================================================
             * List all the Communities or Site this community is a part of.
             * =================================================================
             */
            Community parent = object.getParentCommunity();
            
            if(parent != null)
            {
                handleStatement(
                        aggregation,
                        DS.isPartOfCommunity, 
                        createResource(object.getParentCommunity()));
            }
            else
            {
                Site site = (Site)Site.find(getContext(), 0);
                handleStatement(
                        aggregation,
                        DS.isPartOfSite, 
                        createResource(site));
            }

            
            /* =================================================================
             * List all the Communities or Collections within this community.
             * =================================================================
             */
            for(Community community : object.getSubcommunities())
            {
                handleStatement(
                        aggregation,
                        DS.hasCommunity, 
                        createResource(community));
            }

            for (Collection coll : object.getCollections())
            {
                handleStatement(
                        aggregation,
                        DS.hasCollection, 
                        createResource(coll));
            }
            
            /* =================================================================
             * Dig around for a Logo Image.
             * =================================================================
             */
            Bitstream logo = object.getLogo();
            if (logo != null)
            {
                handleStatement(
                        aggregation, DS.logo, createResource(logo));
            }
            
        }
        catch (SQLException e)
        {
            throw new RDFHandlerException(e.getMessage(), e);
        }

    }
    
    public void handleChildren(DSpaceObject object) throws RDFHandlerException
    {
        Community community = (Community) object;
        
        try
        {
            for (Community comm : community.getSubcommunities())
            {
                getFactory().getAdapter(comm).handle(comm);
                getContext().removeCached(comm, comm.getID());
            }
            
            for (Collection coll : community.getCollections())
            {
                getFactory().getAdapter(coll).handle(coll);
                getContext().removeCached(coll, coll.getID());
            }
        }
        catch (SQLException e)
        {
            throw new RDFHandlerException(e.getMessage(),e);
        }
    }
}
