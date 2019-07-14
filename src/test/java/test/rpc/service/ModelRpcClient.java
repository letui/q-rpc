package test.rpc.service;

import io.letuismart.rpc.spec.RpcClient;
import io.letuismart.rpc.spec.RpcMethod;
import io.letuismart.rpc.spec.RpcParam;

@RpcClient(service = "queryRpc")
public interface ModelRpcClient {
    @RpcMethod()
    String sayHello();

    @RpcMethod()
    String sayHello2(@RpcParam("apf") String apf);
}
