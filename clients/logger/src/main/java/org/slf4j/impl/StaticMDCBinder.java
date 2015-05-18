package org.slf4j.impl;

import org.slf4j.helpers.BasicMDCAdapter;
import org.slf4j.spi.MDCAdapter;

public class StaticMDCBinder {
    /**
     * The unique instance of this class.
     */
    public static final StaticMDCBinder SINGLETON = new StaticMDCBinder();

    private StaticMDCBinder() {}
  
    public MDCAdapter getMDCA()
    {
        return new BasicMDCAdapter();
    }
  
    public String  getMDCAdapterClassStr()
    {
        return BasicMDCAdapter.class.getName();
    }
}
