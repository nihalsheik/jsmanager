package com.nihalsoft.jsm;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.annotation.Resources;

import org.reflections.Reflections;

import com.nihalsoft.jsm.annotation.Inject;

public class JSManager {

    private static JSManager instance;

    private Map<String, Object> services = new HashMap<String, Object>();

    public JSManager() {
        services.put(JSManager.class.getName(), this);
    }

    public static JSManager instance() {
        if (instance == null) {
            synchronized (JSManager.class) {
                if (instance == null) {
                    instance = new JSManager();
                }
            }
        }
        return instance;
    }

    public void scan(String[] pkgs) {
        try {

            if (pkgs == null || pkgs.length == 0) {
                System.out.println("No packages to scan");
            }

            Reflections reflections;

            for (String pkg : pkgs) {
                _log("Start scanning packages :" + pkg);
                reflections = new Reflections(pkg);
                _scanResources(reflections);
                _scanResource(reflections);
            }

            _inject();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void _scanResources(Reflections ref) throws Exception {
        _log("");
        int i = 0;
        Set<Class<?>> annotated = ref.getTypesAnnotatedWith(Resources.class);
        for (Class<?> clazz : annotated) {
            Object resourceList = clazz.newInstance();
            _log("Scanning by Resources......" + clazz.getName());
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                Resource res = method.getAnnotation(Resource.class);
                if (res != null) {
                    _log("Find bean " + res.name() + " - Obect " + clazz.getName());
                    method.setAccessible(true);
                    services.put(res.name(), method.invoke(resourceList));
                    i++;
                }
            }
        }
        _log("Resource list count " + i);
    }

    private void _scanResource(Reflections ref) throws Exception {
        _log("");
        _log("Scanning Resource");
        Set<Class<?>> annotated = ref.getTypesAnnotatedWith(Resource.class);
        int i = 0;
        for (Class<?> clazz : annotated) {
            _log("Find bean ......" + clazz.getName());
            services.put(clazz.getName(), clazz.newInstance());
            i++;
        }
        _log("Resource count " + i);
    }

    public Object createBean(Class<?> klass) throws Exception {
        Object instance = klass.newInstance();
        _injectClass(instance);
        return instance;
    }

    private void _inject() {
        services.forEach((k, ins) -> this._injectClass(ins));
        services.forEach((k, ins) -> {
            try {
                Method method = ins.getClass().getMethod("postConstruct");
                method.invoke(ins);
            } catch (Exception e) {

            }
        });
    }

    private void _injectClass(Object object) {

        String className = object.getClass().getName();

        Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            Inject inj = field.getAnnotation(Inject.class);
            if (inj != null) {
                try {
                    String name = inj.value().equals("") ? field.getType().getName() : inj.value();
                    _log("Injecting for " + className + " , Field: " + field.getName() + ", Key " + name);
                    Object t = services.get(name);
                    if (t != null) {
                        _log("---------> Injecting Object " + t.getClass().getName());
                        field.setAccessible(true);
                        field.set(object, services.get(name));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        _log("---------------------");

    }

    public JSManager addBean(Class<?> klass) {
        try {
            services.put(klass.getName(), klass.newInstance());
            _inject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public JSManager addBean(String name, Object object) {
        services.put(name, object);
        _inject();
        return this;
    }

    public JSManager addBean(Object object) {
        services.put(object.getClass().getName(), object);
        _inject();
        return this;
    }

    public Object get(String name) {
        return services.get(name);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> clazz) {
        return (T) services.get(clazz.getName());
    }

    private void _log(String msg) {
        System.out.println(" ----> " + msg);
    }
}
