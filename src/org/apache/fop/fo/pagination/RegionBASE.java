/*
 * $Id$
 * Copyright (C) 2001-2002 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.fo.pagination;

// FOP
import org.apache.fop.fo.FONode;

/**
 * Base class for Before, After, Start and End regions (BASE).
 */
public abstract class RegionBASE extends Region {

    private int extent;

    protected RegionBASE(FONode parent) {
        super(parent);
    }

    public void end() {
        // The problem with this is that it might not be known yet....
        // Supposing extent is calculated in terms of percentage
        this.extent = this.properties.get("extent").getLength().mvalue();
    }

    int getExtent() {
        return this.extent;
    }
}

