package org.dspace.adapters.rdf.vocabularies;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;

public class LW
{

    public static final String NAMESPACE = "http://simile.mit.edu/longwell/";
    
    private static ValueFactory vf = ValueFactoryImpl.getInstance();

    public static final URI NS = vf.createURI(NAMESPACE);
    
    public static final URI textref = vf.createURI(NAMESPACE, "textref");

}
