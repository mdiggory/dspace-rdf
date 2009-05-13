package org.dspace.adapters.rdf;

import java.sql.SQLException;
import java.util.Date;

import org.dspace.adapters.rdf.vocabularies.DC;
import org.dspace.adapters.rdf.vocabularies.DCMITYPE;
import org.dspace.adapters.rdf.vocabularies.DCTERMS;
import org.dspace.adapters.rdf.vocabularies.DS;
import org.dspace.adapters.rdf.vocabularies.ORE;
import org.dspace.browse.BrowseEngine;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;
import org.dspace.browse.BrowserScope;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.ConfigurationManager;
import org.dspace.sort.SortException;
import org.dspace.sort.SortOption;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;


public class DSpaceCollectionAdapter extends DSpaceObjectAdapter
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
    
    public void handle(DSpaceObject collection) throws RDFHandlerException 
    {
        handle((Collection) collection);
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
    
    public void handle(Collection object) throws RDFHandlerException 
    {

        try
        {
            
            Resource aggregation = createResource(object);

            //this.handleResourceMap(object, aggregation); 

            /* =================================================================
             * The following statements are typing for DCMI Collections
             * =================================================================
             */
            
            handleStatement(aggregation, RDF.TYPE, DS.Collection);
            
            // describe type as collection (seems appropriate for RDF)
            //handleStatement(aggregation, RDF.TYPE, DCMITYPE.Collection);

            // describe type as collection (specification says this is manditory)
            //handleStatement(aggregation, DC.type_, DCMITYPE.Collection);
            
            // make statements about it
            //handleStatement(aggregation, RDF.TYPE, ORE.Aggregation);
            
            /* =================================================================
             * The following is a dc.identifier, title, creator and abstract for DCMI Collections 
             * =================================================================
             */
            handleStatement(aggregation, DC.identifier_, 
                    valueFactory.createLiteral("hdl:" + object.getHandle(), DCTERMS.URI));

            // give it a title
            handleStatement(aggregation, DC.title_, object.getName());

            // describe type as resource map
            handleStatement(aggregation, DC.creator_,
                    ConfigurationManager.getProperty("dspace.name"));
            
            String shortDesc = object.getMetadata("short_description");
            if (shortDesc != null && !shortDesc.trim().equals(""))
            {
                shortDesc = cleanHTML(shortDesc);
                handleStatement(aggregation, DCTERMS.abstract_, shortDesc);
            }

            String intro = object.getMetadata("introductory_text");
            if (intro != null && !intro.trim().equals("")){
                intro = cleanHTML(intro);
                handleStatement(aggregation, DCTERMS.abstract_, intro);
            }
            
            /* =================================================================
             * Dig around for a Logo Image.
             * =================================================================
             */
            Bitstream logo = object.getLogo();
            if (logo != null)
            {
                handleStatement(aggregation, DS.logo, createResource(logo));
            }
            
            /* =================================================================
             * Time lastModified TODO: We need real timestamps for changes in db...
             * =================================================================
             */
            handleStatement(aggregation, DCTERMS.modified_, new Date());

            /* =================================================================
             * List all the Communities this collection is a part of.
             * =================================================================
             */
            for(Community community : object.getCommunities())
            {
                handleStatement(aggregation, DS.isPartOfCommunity, 
                        createResource(community));
                
                getContext().removeCached(community, community.getID());
            }
 
            /**
             * Too heavy handed to list them all here... need some paging
             * mechanism or SPARQL support here...
            for(ItemIterator iter = object.getAllItems(); iter.hasNext();)
            {
                Item item = iter.next();
                handleStatement(aggregation, ORE.aggregates, 
                        createResource(item));

                item.decache();
            }  
            */ 
        }
        catch (SQLException e)
        {
            throw new RDFHandlerException(e.getMessage(), e);
        }
    }
    
    
    @SuppressWarnings("unchecked")
    public void handleChildren(DSpaceObject object) throws RDFHandlerException
    {
        Collection collection = (Collection)object;
        
        try
        {
            String source = ConfigurationManager.getProperty("recent.submissions.sort-option");
            BrowserScope scope = new BrowserScope(getContext());
            scope.setCollection(collection);
            
            scope.setResultsPerPage(100);
            scope.setBrowseIndex(BrowseIndex.getItemBrowseIndex());
            scope.setOrder(SortOption.DESCENDING);
            
            for (SortOption so : SortOption.getSortOptions())
            {
                if (so.getName().equals(source))
                {
                    scope.setSortBy(so.getNumber());
                    scope.setOrder(SortOption.DESCENDING);
                }
            }

            BrowseEngine be = new BrowseEngine(getContext());
                
            java.util.List<Item> items = be.browse(scope).getResults();

            for (Item item : items)
            {
                //if(item.getOwningCollection().equals(collection))
                    getFactory().getAdapter(item).handle(item);
                    
                item.decache();
            }
        }
        catch (SQLException e)
        {
            throw new RDFHandlerException(e.getMessage(),e);
        }
        catch (BrowseException e)
        {
            throw new RDFHandlerException(e.getMessage(),e);
        }
        catch (SortException e)
        {
            throw new RDFHandlerException(e.getMessage(),e);
        }

    }

}
