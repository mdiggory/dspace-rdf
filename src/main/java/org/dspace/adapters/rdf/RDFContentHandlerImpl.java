
package org.dspace.adapters.rdf;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.NamespaceSupport;
import org.xml.sax.helpers.XMLFilterImpl;

import info.aduna.xml.XMLUtil;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;


/**
 *
 * @author mdiggory
 */
public class RDFContentHandlerImpl implements RDFHandler{

	protected ContentHandler handler;
	
	protected LexicalHandler lexer;
	
	/*
	 * SAX types for attributes and entities
	 */
	public final static String CDATA = "CDATA";

	public final static String ID = "ID";

	public final static String IDREF = "IDREF";

	public final static String IDREFS = "IDREFS";

	public final static String NMTOKEN = "NMTOKEN";

	public final static String NMTOKENS = "NMTOKENS";

	public final static String ENTITY = "ENTITY";

	public final static String ENTITIES = "ENTITIES";

	public final static String NOTATION = "NOTATION";

	/*-----------*
	 * Variables *
	 *-----------*/
	
	protected NamespaceSupport namespaces = new NamespaceSupport();
	
	protected boolean writingStarted;

	protected boolean headerWritten;

	protected Statement lastWrittenStatement;
	
    private static final Attributes EMPTY_ATTS = new AttributesImpl();

	/*--------------*
	 * Constructors *
	 *--------------*/

    /**
     *  Creates a new RDFXMLWriter that will write to the supplied Writer.
     */
	public RDFContentHandlerImpl(ContentHandler handler)
    {
        this.handler = handler;
        
        if(handler instanceof LexicalHandler)
        {
            lexer = ((LexicalHandler)handler);
        }
        
        writingStarted = false;
        headerWritten = false;
        lastWrittenStatement = null;
    }

    public RDFFormat getRDFFormat() {
		return RDFFormat.RDFXML;
	}

	public void startRDF() {
		if (writingStarted) {
			throw new RuntimeException("Document writing has already started");
		}

		try {
			handler.startDocument();
		}
		catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		writingStarted = true;

	}

	protected void writeHeader()
		throws SAXException
	{
		try {

			// This export format needs the RDF namespace to be defined, add a
			// prefix for it if there isn't one yet.
			handleNamespace("rdf", RDF.NAMESPACE);

			handler.startElement(RDF.NAMESPACE, "RDF", qName(RDF.NAMESPACE, "RDF"), EMPTY_ATTS);
		}
		finally {
			headerWritten = true;
		}
	}
	


	public void endRDF()
		throws RDFHandlerException
	{
		if (!writingStarted) {
			throw new RuntimeException("Document writing has not yet started");
		}

		try {
			if (!headerWritten) {
				writeHeader();
			}

            if (lastWrittenStatement != null) {
                handler.endElement(RDF.NAMESPACE, "Description", qName(RDF.NAMESPACE, "Description"));
                lastWrittenStatement = null;
            }
            
            handler.endElement(RDF.NAMESPACE, "RDF", qName(RDF.NAMESPACE, "RDF"));
			handler.endDocument();

		}
		catch (SAXException e) {
			throw new RDFHandlerException(e);
		}
		finally {
			writingStarted = false;
			headerWritten = false;
		}
	}

	public void handleNamespace(String prefix, String namespace) {
	    
	    if (namespace == null)
	    {
            throw new RuntimeException("namespace cannot be null");
        }
	    
        try
        {
            
            if (prefix == null || !XMLUtil.isNCName(prefix))
            {
                if(namespaces.getPrefix(namespace) == null)
                {
                    String nextPrefix = getNextPrefix();
                    namespaces.declarePrefix(nextPrefix, namespace);
                        handler.startPrefixMapping(nextPrefix, namespace);

                }
                return; 
            }

            String existingNamespace = namespaces.getURI(prefix);
            
            if (existingNamespace == null)
            {
                if (XMLUtil.isNCName(prefix))
                {
                    namespaces.declarePrefix(prefix,namespace);
                    handler.startPrefixMapping(prefix, namespace);

                }
                else
                {
                    String nextPrefix = getNextPrefix();
                    namespaces.declarePrefix(nextPrefix, namespace);
                    handler.startPrefixMapping(nextPrefix, namespace);
                }
                
            }
	    
        }
        catch (SAXException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

	    return;
	}
	
	private String getNextPrefix()
	{
	    String prefix = "ns";

        int number = 1;

        while (namespaces.getURI(prefix + number) != null) {
            number++;
        }

        prefix += number;
        
        return prefix;
	}

	public void handleStatement(Statement st)
		throws RDFHandlerException
	{
		if (!writingStarted) {
			throw new RuntimeException("Document writing has not yet been started");
		}

		Resource subj = st.getSubject();
		URI pred = st.getPredicate();
		Value obj = st.getObject();

		// Verify that an XML namespace-qualified name can be created for the
		// predicate
		String predString = pred.toString();
		int predSplitIdx = XMLUtil.findURISplitIndex(predString);
		if (predSplitIdx == -1) {
			throw new RDFHandlerException("Unable to create XML namespace-qualified name for predicate: "
					+ predString);
		}

		try {
		    
			if (!headerWritten) {
				writeHeader();
			}

		    String predNamespace = predString.substring(0, predSplitIdx);
		    String predLocalName = predString.substring(predSplitIdx);
            
			// Do we start a new block?
			if (lastWrittenStatement == null || !st.getSubject().equals(lastWrittenStatement.getSubject())) {
				
			     if (lastWrittenStatement != null) {
			            handler.endElement(RDF.NAMESPACE, "Description", qName(RDF.NAMESPACE, "Description"));
			            lastWrittenStatement = null;
			     }

				AttributesImpl atts = new AttributesImpl();

				if (subj instanceof BNode) {
					BNode bNode = (BNode)subj;
					atts.addAttribute(RDF.NAMESPACE, "nodeID", qName(RDF.NAMESPACE, "nodeID"), ID, bNode.getID());
				}
				else {
					URI uri = (URI)subj;
					atts.addAttribute(RDF.NAMESPACE, "about", qName(RDF.NAMESPACE, "about"), CDATA, uri.toString());
				}

		        handler.startElement(RDF.NAMESPACE, "Description", qName(RDF.NAMESPACE, "Description"), atts);

				this.lastWrittenStatement = st;
			}

			
			
			// writeStartOfStartTag(predNamespace, predLocalName);
			AttributesImpl atts = new AttributesImpl();

			// OBJECT
			if (obj instanceof Resource) {
				Resource objRes = (Resource)obj;

				namespaces.pushContext();
                
                // PREDICATE SETUP
                //handleNamespace(null, predNamespace);
                
				if (objRes instanceof BNode) {
					BNode bNode = (BNode)objRes;
					String qName = qName(RDF.NAMESPACE, "nodeID");
					atts.addAttribute(RDF.NAMESPACE, "nodeID", qName, ID, bNode.getID());
				}
				else {
					URI uri = (URI)objRes;
					String qName = qName(RDF.NAMESPACE, "resource");
					atts.addAttribute(RDF.NAMESPACE, "resource", qName, CDATA,
							uri.toString());

				}

	            
	            String qName = qName(predNamespace, predLocalName);
                handler.startElement(predNamespace, predLocalName, qName, atts);
                handler.endElement(predNamespace, predLocalName, qName);
                
                namespaces.popContext();
			}
			else if (obj instanceof Literal) 
			{

				Literal objLit = (Literal)obj;

				// language attribute
				if (objLit.getLanguage() != null) {
					atts.addAttribute(NamespaceSupport.XMLNS, "lang", "xml:lang", "NMTOKEN", objLit.getLanguage());
				}

				// TODO: XMLLiteral Parser is very untested.
				
				// datatype attribute
				boolean isXMLLiteral = false;
				URI datatype = objLit.getDatatype();
				if (datatype != null) {
					// Check if datatype is rdf:XMLLiteral
					isXMLLiteral = datatype.equals(RDF.XMLLITERAL);

					if (isXMLLiteral) {
						atts.addAttribute(RDF.NAMESPACE, "parseType", qName(RDF.NAMESPACE, "parseType"), CDATA,
								"Literal");
					}
					else {
						atts.addAttribute(RDF.NAMESPACE, "datatype", qName(RDF.NAMESPACE, "datatype"), CDATA,
								datatype.toString());
					}
				}

                namespaces.pushContext();
                
                // PREDICATE SETUP
                handleNamespace(null, predNamespace);
                
                handler.startElement(predNamespace, predLocalName, qName(predNamespace, predLocalName), atts);

				// label
				if (isXMLLiteral) {
					// Write XML literal as plain XML
					parseLiteral(objLit.getLabel());
				}
				else {
					char[] chars = objLit.getLabel().toCharArray();
					handler.characters(chars, 0, chars.length);
				}

	            handler.endElement(predNamespace, predLocalName, qName(predNamespace, predLocalName));
	            
                namespaces.popContext();
			}

			// Don't write </rdf:Description> yet, maybe the next statement
			// has the same subject.
		}
		catch (SAXException e) {
			throw new RDFHandlerException(e.getMessage(), e);
		}
	}

	private String qName(String namespace, String element) {
		String prefix = namespaces.getPrefix(namespace);
		return prefix == null || prefix.equals("") ? element : prefix + ":" + element ;
	}

	private void parseLiteral(String xmlString)
		throws SAXException
	{
	    if(literalReader == null)
	    {
	        literalReader = new LiteralXMLReader();
	        literalReader.setContentHandler(handler);
	    }
	    
		try {
			if (xmlString != null && xmlString.length() > 0) {
			    
			    literalReader.parse(new InputSource(new ByteArrayInputStream(xmlString.getBytes())));
			}
		}
		catch (IOException e) {
			// This should never happen
			e.printStackTrace();
		}
		finally
		{
		    
		}
	}

	private LiteralXMLReader literalReader;
	
	protected class LiteralXMLReader extends XMLFilterImpl
	{
        @Override
        public void endDocument() throws SAXException{}

        @Override
        public void startDocument() throws SAXException{}
	}

    public void handleComment(String arg0) throws RDFHandlerException
    {
        if(lexer != null)
        {
            char[] comment = arg0.toCharArray();
            try
            {
                lexer.comment(comment, 0, comment.length);
            }
            catch (SAXException e)
            {
                throw new RDFHandlerException(e.getMessage(), e);
            }
        }
    }
}
