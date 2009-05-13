package org.dspace.adapters.rdf;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.codec.digest.DigestUtils;
import org.dspace.adapters.rdf.vocabularies.DCTERMS;
import org.dspace.app.util.Util;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Community;
import org.dspace.content.DCDate;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Site;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;

public class AdapterSupport
{
    
    protected ValueFactory valueFactory = ValueFactoryImpl.getInstance();
    
    private RDFHandler rdfHandler = null;
    
    private Context context = null;
    
    protected String baseUri = ConfigurationManager.getProperty("dspace.url");
    
    private String metadataServiceUri = "/handle";

    public AdapterSupport()
    {
        super();
    }

    public Context getContext()
    {
        return context;
    }

    public void setContext(Context context)
    {
        this.context = context;
    }

    /**
     * 
     * @param handler
     */
    public void setRDFHandler(RDFHandler handler)
    {
        this.rdfHandler = handler;
    }

    /**
     * 
     * @return
     */
    public RDFHandler getRDFHander()
    {
        return rdfHandler;
    }

    public void setBaseUri(String baseUri)
    {
        this.baseUri = baseUri;
        
    }
    
    /**
     * 
     * @return
     */
    public String getBaseUri()
    {
        return baseUri;
    }

    public String getMetadataServiceUri()
    {
        return metadataServiceUri;
    }

    public void setMetadataServiceUri(String metadataServiceUri)
    {
        this.metadataServiceUri = metadataServiceUri;
    }

    /*
    protected String getMetadataURL(Object object)
    {
        if(object instanceof DSpaceObject)
        {
            return getMetadataURL(((DSpaceObject)object).getHandle());
        }
        
        return this.generateDefaultURI(String.valueOf(object.hashCode()));
    }
    */ 
    


    public String getMetadataURL(String identifier)
    {
        // Same URIs as history uses
        return getBaseUri() + getMetadataServiceUri() + "/" + identifier;
    }
    
    public Resource createResource(String handle)
    {
        return valueFactory.createURI(getMetadataURL(handle));
    }
    
    public Resource createResource(DSpaceObject object)
    {
        return valueFactory.createURI(getMetadataURL(object.getHandle()));
    }
    
    public Resource createResource(Community community)
    {
        return valueFactory.createURI(getMetadataURL(community.getHandle()));
    }
    
    public Resource createResource(Site site)
    {
        return valueFactory.createURI(getBaseUri());
    }



    /**
     * Return the bitstream location of the the Collections Logo
     * 
     * @param bitstream
     * @return
     * @throws UnsupportedEncodingException 
     */
    public Resource createResource(Bitstream bitstream)
    {
        String url = getBaseUri()
        + "/retrieve/"
        + bitstream.getID();
        
        try
        {
            if(bitstream.getName() != null)
                url += "/" + Util.encodeBitstreamName(bitstream.getName(),
                    Constants.DEFAULT_ENCODING);
        }
        catch (UnsupportedEncodingException e)
        {
            if(bitstream.getName() != null)
                url += "/" + bitstream.getName();
            
            e.printStackTrace();
        }
        
        return valueFactory.createURI(url);
    }

    /**
     * Generate a default URI for a String value.
     * 
     * @param string
     * @return
     */
    public String generateDefaultURI(String string)
    {
        return "sha:" + Utils.toHex(DigestUtils.sha(string));
    }

    public Resource createResource(Item item, Bitstream bitstream)
    {
        String url = getBaseUri() + "/bitstream/handle/" + item.getHandle() + "/"
        + bitstream.getSequenceID();
        
        try
        {
            url += "/" + Util.encodeBitstreamName(bitstream.getName(),
                    Constants.DEFAULT_ENCODING);
        }
        catch (UnsupportedEncodingException e)
        {
            url += "/" + bitstream.getName();
            e.printStackTrace();
        }
        
        // TODO Auto-generated method stub
        return  valueFactory.createURI(url);
    }

    public Resource createResource(BitstreamFormat format)
    {
        return valueFactory.createURI( getBaseUri() + "/format/" + format.getID());
    }

    
    public void handleStatement(Resource subject, URI predicate, Date date)
            throws RDFHandlerException
    {
        try
        {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(date);
            
            XMLGregorianCalendar xmlCal = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
            /**
             * kinda hacky, but gets around strange bug when Gregcal is
             * year, month, day is set by hand.
             */
            xmlCal.setFractionalSecond(null);
            xmlCal.setHour(DatatypeConstants.FIELD_UNDEFINED);
            xmlCal.setMinute(DatatypeConstants.FIELD_UNDEFINED);
            xmlCal.setSecond(DatatypeConstants.FIELD_UNDEFINED);
            xmlCal.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
            xmlCal.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
            handleStatement(subject, predicate, valueFactory.createLiteral(xmlCal)); 
        }
        catch (DatatypeConfigurationException e)
        {
            throw new RDFHandlerException(e.getMessage(), e);
        }
    }
    
    public void handleStatement(Resource subject, URI predicate, String literal)
            throws RDFHandlerException
    {
        handleStatement(subject, predicate, valueFactory.createLiteral(literal));
    }

    public void handleStatement(Resource subject, URI predicate, Value object)
            throws RDFHandlerException
    {
        getRDFHander().handleStatement(
                valueFactory.createStatement(subject, predicate, object));
    }

    public void handleStatement(Statement statement)
    throws RDFHandlerException
    {
        getRDFHander().handleStatement(statement);
    }
}