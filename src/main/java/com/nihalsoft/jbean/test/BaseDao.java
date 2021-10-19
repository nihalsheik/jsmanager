package com.nihalsoft.jbean.test;

import com.nihalsoft.jsm.Inject;

public class BaseDao {

    protected TestDao td;

    @Inject
    public void setTestDao(@Inject("testdao") TestDao td) {
        this.td = td;
    }
}
