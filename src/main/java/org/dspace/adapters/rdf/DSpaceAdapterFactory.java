package org.dspace.adapters.rdf;

import java.util.HashMap;
import java.util.Map;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Site;


public class DSpaceAdapterFactory extends AdapterSupport
{
 
    private boolean includeChildren = false;

    @SuppressWarnings("unchecked")
    private Map<Class,DSpaceObjectAdapter> adapters = new HashMap<Class,DSpaceObjectAdapter>();

    public Adapter getAdapter(DSpaceObject object)
    {
        DSpaceObjectAdapter adapter = null;
        
        if(!adapters.containsKey(object.getClass()))
        {
            if (object instanceof Site)
            {
                adapter = new DSpaceSiteAdapter();
            }
            else if (object instanceof Community)
            {
                adapter = new DSpaceCommunityAdapter();
            }
            else if (object instanceof Collection)
            {
                adapter = new DSpaceCollectionAdapter();
            }
            else if (object instanceof Item)
            {
                adapter = new DSpaceItemAdapter();
            }
            
            adapter.setFactory(this);
            adapter.setContext(getContext());
            adapter.setBaseUri(getBaseUri());
            adapter.setMetadataServiceUri(getMetadataServiceUri());
            adapter.setRDFHandler(getRDFHander());
            adapters.put(object.getClass(), adapter);
        }
        else
        {
            adapter = adapters.get(object.getClass());
        }
        
        return adapter;
    }

    public boolean isIncludeChildren()
    {
        return includeChildren;
    }

    public void setIncludeChildren(boolean includeChildren)
    {
        this.includeChildren = includeChildren;
    }
}
