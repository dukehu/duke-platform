package com.duke.auth.util;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.*;

/**
 * API 扫描工具类。
 * 用于扫描所有 @RestController，提取接口元数据（路径、方法、权限标识）。
 * 供应用启动时同步接口信息到数据库，用于网关鉴权和权限管理。
 */
@Slf4j
public class ApiScanner {

    private final ApplicationContext applicationContext;
    private final String appId;

    public ApiScanner(ApplicationContext applicationContext, String appId) {
        this.applicationContext = applicationContext;
        this.appId = appId;
    }

    /**
     * 扫描所有 @RestController，返回接口元数据列表
     */
    public List<ApiMetadata> scanAll() {
        log.info("开始扫描 API...");
        Map<String, Object> controllers = applicationContext.getBeansWithAnnotation(RestController.class);
        List<ApiMetadata> scanned = new ArrayList<>();

        for (Object bean : controllers.values()) {
            Class<?> clazz = bean.getClass();
            String packageName = clazz.getPackage().getName();
            if (!packageName.startsWith("com.duke")) {
                continue;
            }
            // Spring AOP/事务代理生成的子类名含 "$$"，需获取父类才能读到原始注解
            if (clazz.getName().contains("$$")) clazz = clazz.getSuperclass();

            String controllerClass = clazz.getName();
            String controllerName = controllerClass;
            Tag tag = clazz.getAnnotation(Tag.class);
            if (tag != null) controllerName = tag.name();

            String classPath = getClassPath(clazz);

            for (Method method : clazz.getDeclaredMethods()) {
                String[] paths = getMethodPaths(method);
                String httpMethod = getHttpMethod(method);
                if (paths == null || httpMethod == null) continue;

                String apiName = method.getName();
                String apiDesc = null;
                Operation operation = method.getAnnotation(Operation.class);
                if (operation != null) {
                    apiName = operation.summary();
                    apiDesc = operation.description();
                }

                String permission = null;
                PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
                if (preAuthorize != null) {
                    String value = preAuthorize.value();
                    int start = value.indexOf("'");
                    int end = value.lastIndexOf("'");
                    if (start >= 0 && end > start) permission = value.substring(start + 1, end);
                }

                // 扫描所有接口，不仅仅是带 @PreAuthorize 的
                for (String path : paths) {
                    ApiMetadata api = new ApiMetadata();
                    api.setAppId(appId);
                    api.setControllerClass(controllerClass);
                    api.setControllerName(controllerName);
                    api.setApiName(apiName);
                    api.setApiPath(classPath + path);
                    api.setApiMethod(httpMethod);
                    api.setApiDesc(apiDesc);
                    api.setPermission(permission);
                    scanned.add(api);
                }
            }
        }

        log.info("API 扫描完成，共扫描 {} 个接口", scanned.size());
        return scanned;
    }

    private String getClassPath(Class<?> clazz) {
        RequestMapping rm = clazz.getAnnotation(RequestMapping.class);
        if (rm != null && rm.value().length > 0) return rm.value()[0];
        return "";
    }

    private String[] getMethodPaths(Method method) {
        if (method.isAnnotationPresent(GetMapping.class)) return method.getAnnotation(GetMapping.class).value();
        if (method.isAnnotationPresent(PostMapping.class)) return method.getAnnotation(PostMapping.class).value();
        if (method.isAnnotationPresent(PutMapping.class)) return method.getAnnotation(PutMapping.class).value();
        if (method.isAnnotationPresent(DeleteMapping.class)) return method.getAnnotation(DeleteMapping.class).value();
        if (method.isAnnotationPresent(RequestMapping.class)) return method.getAnnotation(RequestMapping.class).value();
        return null;
    }

    private String getHttpMethod(Method method) {
        if (method.isAnnotationPresent(GetMapping.class)) return "GET";
        if (method.isAnnotationPresent(PostMapping.class)) return "POST";
        if (method.isAnnotationPresent(PutMapping.class)) return "PUT";
        if (method.isAnnotationPresent(DeleteMapping.class)) return "DELETE";
        if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMethod[] methods = method.getAnnotation(RequestMapping.class).method();
            return methods.length > 0 ? methods[0].name() : "GET";
        }
        return null;
    }

    /**
     * API 元数据
     */
    public static class ApiMetadata {
        private String appId;
        private String controllerClass;
        private String controllerName;
        private String apiName;
        private String apiPath;
        private String apiMethod;
        private String apiDesc;
        private String permission;

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getControllerClass() {
            return controllerClass;
        }

        public void setControllerClass(String controllerClass) {
            this.controllerClass = controllerClass;
        }

        public String getControllerName() {
            return controllerName;
        }

        public void setControllerName(String controllerName) {
            this.controllerName = controllerName;
        }

        public String getApiName() {
            return apiName;
        }

        public void setApiName(String apiName) {
            this.apiName = apiName;
        }

        public String getApiPath() {
            return apiPath;
        }

        public void setApiPath(String apiPath) {
            this.apiPath = apiPath;
        }

        public String getApiMethod() {
            return apiMethod;
        }

        public void setApiMethod(String apiMethod) {
            this.apiMethod = apiMethod;
        }

        public String getApiDesc() {
            return apiDesc;
        }

        public void setApiDesc(String apiDesc) {
            this.apiDesc = apiDesc;
        }

        public String getPermission() {
            return permission;
        }

        public void setPermission(String permission) {
            this.permission = permission;
        }
    }
}
