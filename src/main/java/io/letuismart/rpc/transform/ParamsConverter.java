package io.letuismart.rpc.transform;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.letuismart.rpc.spec.RpcParam;

import java.lang.reflect.Parameter;

public class ParamsConverter {

    public Object[] convert(String reqBody, Parameter[] rpcMethodParamsType) {
        Gson gson=new Gson();
        System.out.println(reqBody);
        JsonObject jsonObject = gson.fromJson(reqBody, JsonObject.class);
        Object[] values = new Object[rpcMethodParamsType.length];
        for (int i = 0; i < values.length; i++) {
            Parameter defParameter = rpcMethodParamsType[i];
            Class<?> defParamType = defParameter.getType();
            RpcParam annoRpcParam = defParameter.getAnnotation(RpcParam.class);
            if (annoRpcParam != null) {
                String annoParamName = annoRpcParam.value();
                values[i] = gson.fromJson(jsonObject.get(annoParamName),defParamType);
            }
        }
        return values;
    }
}
