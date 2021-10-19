package com.nihalsoft.jbean.test;

import com.nihalsoft.jsm.Bean;
import com.nihalsoft.jsm.Inject;

@Bean(name = "testdao")
public class TestDao {

    @Inject("empdao")
    private EmpDao emp;
    
    public void test() {
        System.out.println("test dao");
        emp.test();
    }

    public void test2() {
        System.out.println("test dao 2");
    }
}
