package org.dspace.adapters.rdf;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.openrdf.rio.RDFHandlerException;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

public class DSpaceXMLReader implements XMLReader
{

    private ContentHandler handler = null;

    private Context context = null;

    private DSpaceAdapterFactory factory = new DSpaceAdapterFactory();
    
    protected void init()
    {
        if(factory == null){
            factory = new DSpaceAdapterFactory();
        }
        
        if(handler != null)
        {
            factory.setRDFHandler(new RDFContentHandlerImpl(handler));
        }
        
        if(context != null)
        {
            factory.setContext(context);
        }
    }
    
    public void parse(DSpaceObject object) throws RDFHandlerException
    {
        init();
        factory.getRDFHander().startRDF();
        factory.getAdapter(object).render(object);
        factory.getRDFHander().endRDF();
    }
    
    public void parse(String systemId) throws IOException, SAXException
    {
        try
        {
            init();
            
            DSpaceObject object = HandleManager.resolveToObject(getContext(),systemId);
            
            if(object != null)
            {
                
                // parse a handle provided as a systemId
                factory.getRDFHander().startRDF();
                factory.getAdapter(object).render(object);
                factory.getRDFHander().endRDF();
            }
        }
        catch (SQLException e)
        {
            throw new SAXException(e.getMessage(),e);
        }
        catch (RDFHandlerException e)
        {
            throw new SAXException(e.getMessage(),e);
        }
        
        
    }

    public void parse(InputSource source) throws IOException, SAXException
    {
        throw new SAXException("InputSource cannot be used in DSpaceXMLReader.");
    }
    
    public ContentHandler getContentHandler()
    {
        return handler;
    }
    
    public void setContentHandler(ContentHandler handler)
    {
        this.handler = handler;
        init();
    }
    
    
    // Unsupported XMLReader methods, not needed for this demo.

    public DTDHandler getDTDHandler()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public EntityResolver getEntityResolver()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public ErrorHandler getErrorHandler()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean getFeature(String name) throws SAXNotRecognizedException,
            SAXNotSupportedException
    {
        // TODO Auto-generated method stub
        return false;
    }

    public Object getProperty(String name) throws SAXNotRecognizedException,
            SAXNotSupportedException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void setDTDHandler(DTDHandler handler)
    {
        // TODO Auto-generated method stub
        
    }

    public void setEntityResolver(EntityResolver resolver)
    {
        // TODO Auto-generated method stub
        
    }

    public void setErrorHandler(ErrorHandler handler)
    {
        // TODO Auto-generated method stub
        
    }

    public void setFeature(String name, boolean value)
            throws SAXNotRecognizedException, SAXNotSupportedException
    {
        // TODO Auto-generated method stub
        
    }

    public void setProperty(String name, Object value)
            throws SAXNotRecognizedException, SAXNotSupportedException
    {
        // TODO Auto-generated method stub
        
    }

    public Context getContext()
    {
        return context;
    }

    public void setContext(Context context)
    {
        this.context = context;
        init();
    }
    
}
