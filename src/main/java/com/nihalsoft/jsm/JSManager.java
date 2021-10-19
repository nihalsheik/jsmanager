package com.nihalsoft.jsm;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.nihalsoft.java.util.Reflection;

public class JSManager {

    private static Map<String, Object> services = new HashMap<String, Object>();

    public static void run(String... pkgs) {

        try {

            if (pkgs == null || pkgs.length == 0) {
                System.out.println("No packages to scan");
            }

            Reflection reflections;

            for (String pkg : pkgs) {
                _log("Start scanning packages :" + pkg);
                if (pkg == null || pkg.length() == 0) {
                    System.out.println("No packages to scan");
                }
                reflections = new Reflection(pkg);
                _scanResource(reflections);
                _scanResources(reflections);

            }

            _inject();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void _scanResources(Reflection ref) throws Exception {

        _log("");
        int i = 0;

        List<Class<?>> annotated = ref.getClassesAnnotatedWith(BeanConfiguration.class);

        for (Class<?> clazz : annotated) {

            Object resourceList = clazz.newInstance();
            JSManager._injectField(resourceList, resourceList.getClass());

            List<Method> methods = ref.getMethodsAnnotatedWith(clazz, Bean.class);

            i = 0;
            for (Method method : methods) {
                Bean bean = method.getAnnotation(Bean.class);
                _log("Find bean " + bean.name());
                Object obj = method.invoke(resourceList);
                services.put(bean.name().equals("") ? clazz.getName() : bean.name(), obj);
                i++;
            }

            _log("Resource list count for " + clazz.getName() + " is " + i);
        }

    }

    private static void _scanResource(Reflection ref) throws Exception {
        _log("");
        _log("Scanning Resource");
        List<Class<?>> annotated = ref.getClassesAnnotatedWith(Bean.class);
        annotated.forEach(JSManager::_register);
        _log("Resource count " + annotated.size());
    }

    private static void _inject() {

        services.forEach((k, ins) -> {
            try {
                JSManager._injectField(ins, ins.getClass());
                JSManager._injectSetter(ins, ins.getClass());
                Method method = ins.getClass().getDeclaredMethod("onInitilized");
                if (method != null) {
                    method.invoke(ins);
                }
            } catch (Exception e) {

            }
        });
    }

    private static void _injectField(Object object, Class<?> clazz) {
        String className = clazz.getName();

        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            Inject inj = field.getAnnotation(Inject.class);
            if (inj == null) {
                continue;
            }
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
        _log("Super class " + clazz.getSuperclass().getSimpleName());
        if (!clazz.getSuperclass().getSimpleName().equals("Object")) {
            _injectField(object, clazz.getSuperclass());
        }
    }

    private static void _injectSetter(Object object, Class<?> clazz) {
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            Inject inj = method.getAnnotation(Inject.class);
            if (inj == null) {
                continue;
            }
            try {
                _log("Injecting Setter - method " + method.getName());
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
                    args[i++] = JSManager.get(name);
                }
                _log("Injecting setter - invokind");
                method.invoke(object, args);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        _log("Super class " + clazz.getSuperclass().getSimpleName());
        if (!clazz.getSuperclass().getSimpleName().equals("Object")) {
            _injectSetter(object, clazz.getSuperclass());
        }
    }

    public static Object get(String name) {
        return services.get(name);
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(String name, Class<T> clazz) {
        return (T) services.get(name);
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> clazz) {
        return (T) services.get(clazz.getName());
    }

    private static <T> void _register(Class<T> clazz) {
        try {
            Bean bean = clazz.getAnnotation(Bean.class);
            if (bean != null) {
                _log("Find bean ......" + clazz.getName());
                services.put(bean.name().equals("") ? clazz.getName() : bean.name(), clazz.newInstance());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void _log(String msg) {
        System.out.println(" -------| " + msg);
    }
}
