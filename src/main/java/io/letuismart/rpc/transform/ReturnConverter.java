package io.letuismart.rpc.transform;

import com.google.gson.Gson;

public class ReturnConverter {
    public Object convert(String response,Class<?> tClass){
        Object value=new Gson().fromJson(response,tClass);
        return value;
    }
}
