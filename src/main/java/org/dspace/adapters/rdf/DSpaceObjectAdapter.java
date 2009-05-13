package org.dspace.adapters.rdf;

import java.io.StringReader;
import java.io.StringWriter;

import org.dspace.content.DSpaceObject;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.RDFHandlerException;
import org.w3c.tidy.Tidy;

public abstract class DSpaceObjectAdapter extends AdapterSupport implements Adapter
{

    private DSpaceAdapterFactory factory;
 
    /* (non-Javadoc)
     * @see org.dspace.adapters.rdf.Adapter#handle(org.dspace.content.DSpaceObject)
     */
    public abstract void handle(DSpaceObject object) throws RDFHandlerException;
    
    /* (non-Javadoc)
     * @see org.dspace.adapters.rdf.Adapter#handleChildren(org.dspace.content.DSpaceObject)
     */
    public abstract void handleChildren(DSpaceObject object) throws RDFHandlerException;

    
    public final void render(DSpaceObject object) throws RDFHandlerException
    {
        
        handleNamespaces();
        
        handle(object);

        if(getFactory().isIncludeChildren())
        {
            handleChildren(object);
        }
           
    }
    
    public String cleanHTML(String original)
    {
        StringWriter writer = new StringWriter();
        Tidy tidy = new Tidy(); // obtain a new Tidy instance
        tidy.setXHTML(true);
        tidy.setXmlOut(true);
        tidy.setPrintBodyOnly(true);
        tidy.setQuiet(true);
        tidy.parse(new StringReader(original), writer); 
        return writer.getBuffer().toString(); 
    }

    public void setFactory(DSpaceAdapterFactory factory)
    {
        this.factory = factory;
    }

    public DSpaceAdapterFactory getFactory()
    {
        return factory;
    }

    /* (non-Javadoc)
     * @see org.dspace.adapters.rdf.Adapter#handleNamespaces()
     */
    public void handleNamespaces() throws RDFHandlerException
    {
        this.getRDFHander().handleNamespace("rdf", RDF.NAMESPACE);
    }
    
}