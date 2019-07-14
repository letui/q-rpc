package io.letuismart.rpc.registry;

import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Set;

public class QRpcDescribe {
    private String rpcClassName;
    private Object rpcBeanInstance;
    private String rpcBeanName;
    private Map<String, Map<String, Parameter[]>> rpcMethods;

    public String getRpcClassName() {
        return rpcClassName;
    }

    public void setRpcClassName(String rpcClassName) {
        this.rpcClassName = rpcClassName;
    }

    public Object getRpcBeanInstance() {
        return rpcBeanInstance;
    }

    public void setRpcBeanInstance(Object rpcBeanInstance) {
        this.rpcBeanInstance = rpcBeanInstance;
    }

    public Map<String, Map<String, Parameter[]>> getRpcMethods() {
        return rpcMethods;
    }

    public void setRpcMethods(Map<String, Map<String, Parameter[]>> rpcMethods) {
        this.rpcMethods = rpcMethods;
    }
    public String getRpcBeanName() {
        return rpcBeanName;
    }

    public void setRpcBeanName(String rpcBeanName) {
        this.rpcBeanName = rpcBeanName;
    }
}
