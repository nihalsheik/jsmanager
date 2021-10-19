package com.nihalsoft.jbean.test;

import com.nihalsoft.jsm.JSManager;

public class Test {

    public static void main(String[] args) {
        
        
        JSManager.run("com.nihalsoft.jbean");
        
        EmpDao td = (EmpDao) JSManager.get("empdao");
        

        td.test();
    }

}
