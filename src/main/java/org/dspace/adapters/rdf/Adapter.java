package org.dspace.adapters.rdf;

import org.dspace.content.DSpaceObject;
import org.openrdf.rio.RDFHandlerException;
import org.xml.sax.SAXException;

public interface Adapter
{

    /**
     * Generic RDF generation for DSpaceObject, should be overridden in upstream
     * implementations.
     * 
     * @param object
     * @throws SAXException
     */
    public abstract void handle(DSpaceObject object) throws RDFHandlerException;

    /**
     * 
     * @param object
     * @throws RDFHandlerException
     */
    public abstract void handleChildren(DSpaceObject object)
            throws RDFHandlerException;

    /**
     * 
     * @throws RDFHandlerException
     */
    public abstract void handleNamespaces() throws RDFHandlerException;

    /**
     * 
     * @param object
     * @throws RDFHandlerException 
     */
    public abstract void render(DSpaceObject object) throws RDFHandlerException;


}