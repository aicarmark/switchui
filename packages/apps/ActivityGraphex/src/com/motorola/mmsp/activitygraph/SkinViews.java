package com.motorola.mmsp.activitygraph;

import java.util.HashMap;

public class SkinViews extends CommonColumn {
    public static final String ME = "reserved_views";
    public HashMap<String, Object> ids = new HashMap<String, Object>();
    
    public SkinViews() {
    }
    
    public SkinViews(HashMap<String, Object> map) {
        this.ids = map;
    }
    
    
    public void form(HashMap<String, Object> map) {
        this.ids = map;
    }
}
