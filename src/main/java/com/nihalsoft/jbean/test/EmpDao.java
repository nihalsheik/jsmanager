package com.nihalsoft.jbean.test;

import com.nihalsoft.jsm.Bean;

@Bean(name = "empdao")
public class EmpDao extends BaseDao {

    public void test() {
        System.out.println("Emplayee dao");
        td.test2();
    }
}
