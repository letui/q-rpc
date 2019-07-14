package test.rpc.service;

import io.letuismart.rpc.spec.RpcMethod;
import io.letuismart.rpc.spec.RpcParam;
import io.letuismart.rpc.spec.RpcService;

@RpcService("queryRpc")
public class ModelRpcService implements ModelRpcClient {

    @Override
    @RpcMethod()
    public String sayHello(){
        System.out.println("XX");
        return "CDMKL";
    }

    @Override
    @RpcMethod()
    public String sayHello2(@RpcParam("apf") String apf){
        System.out.println("XX2"+Thread.currentThread().getId());

        return  apf;
    }
}
