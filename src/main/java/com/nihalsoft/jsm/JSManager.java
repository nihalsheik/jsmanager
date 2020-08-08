package com.nihalsoft.jsm;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.nihalsoft.java.util.Reflection;

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

    public static void init(String... pkgs) {
        JSManager.instance().scan(pkgs);
    }

    public void scan(String pkg) {
        this.scan(new String[] { pkg });
    }

    public void scan(String[] pkgs) {
        try {

            if (pkgs == null || pkgs.length == 0) {
                System.out.println("No packages to scan");
            }

            Reflection reflections;

            for (String pkg : pkgs) {
                _log("Start scanning packages :" + pkg);
                reflections = new Reflection(pkg);
                _scanResources(reflections);
                _scanResource(reflections);
            }

            _inject();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void _scanResources(Reflection ref) throws Exception {

        _log("");
        int i = 0;

        List<Class<?>> annotated = ref.getClassesAnnotatedWith(BeanConfiguration.class);

        for (Class<?> clazz : annotated) {

            List<Method> methods = ref.getMethodsAnnotatedWith(clazz, Bean.class);
            Object resourceList = clazz.newInstance();

            for (Method method : methods) {
                Bean res = method.getAnnotation(Bean.class);
                if (res == null) {
                    continue;
                }
                _log("Find bean " + res.name() + " - Obect " + clazz.getName());
                Object obj = method.invoke(resourceList);
                services.put(res.name().equals("") ? obj.getClass().getName() : res.name(), obj);
                i++;
            }

        }
        _log("Resource list count " + i);
    }

    private void _scanResource(Reflection ref) throws Exception {
        _log("");
        _log("Scanning Resource");
        List<Class<?>> annotated = ref.getClassesAnnotatedWith(Bean.class);
        annotated.forEach(clazz -> this._addBean(clazz));
        _log("Resource count " + annotated.size());
    }

    public <T> T getInstance(Class<T> klass) throws Exception {
        T ins = klass.newInstance();
        _injectClass(ins);
        return ins;
    }

    public Object getInstance(Object obj) throws Exception {
        _injectClass(obj);
        return obj;
    }

    private void _inject() {

        services.forEach((k, ins) -> this._injectField(ins));
        services.forEach((k, ins) -> this._injectSetter(ins));

        services.forEach((k, ins) -> {
            try {
                Method method = ins.getClass().getMethod("$postConstruct");
                method.invoke(ins);
            } catch (Exception e) {

            }
        });
    }

    private void _injectClass(Object object) {
        _injectField(object);
        _injectSetter(object);
    }

    private void _injectField(Object object) {
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

    }

    private void _injectSetter(Object object) {
        _log("Injecting Setter");

        Method[] methods = object.getClass().getMethods();
        for (Method method : methods) {
            Inject inj = method.getAnnotation(Inject.class);
            if (inj == null) {
                continue;
            }
            try {
                _log("Injecting setter - method " + method.getName());
                Object[] args = new Object[method.getParameterCount()];
                int i = 0;
                for (Parameter p : method.getParameters()) {
                    Inject pann = p.getAnnotation(Inject.class);
                    String name = "";
                    if (pann != null && !pann.value().equals("")) {
                        name = pann.value();
                    } else {
                        name = p.getType().getName();
                    }
                    _log("P name -->" + name);
                    args[i++] = this.get(name);
                }
                _log("Injecting setter - invokind");
                method.invoke(object, args);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public <T> T addBean(Class<T> clazz) {
        T ins = this._addBean(clazz);
        _inject();
        return ins;
    }

    public <T> T addBean(Class<T> clazz, boolean attachToContainer) throws Exception {
        if (attachToContainer) {
            return this.addBean(clazz);
        }
        T ins = clazz.newInstance();
        _injectClass(ins);
        return ins;
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

    public <T> T _addBean(Class<T> clazz) {
        T ins = null;
        try {
            Bean bean = clazz.getAnnotation(Bean.class);
            _log("Find bean ......" + clazz.getName() + " constructor length : " + clazz.getConstructors().length);

            ins = clazz.newInstance();

            services.put(bean.name().equals("") ? clazz.getName() : bean.name(), ins);

            Class<?>[] interfaces = clazz.getInterfaces();
            for (Class<?> iface : interfaces) {
                _log("Interface -------------------> " + iface.getName());
                if (!services.containsKey(iface.getName())) {
                    services.put(iface.getName(), ins);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return ins;
    }

    private void _log(String msg) {
        System.out.println(" -------| " + msg);
    }
}
