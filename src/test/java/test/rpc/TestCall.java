package test.rpc;

import io.letuismart.rpc.registry.QRpcClient;
import test.rpc.service.ModelRpcClient;

public class TestCall {
    public static void main(String[] args) {

        String ccc = QRpcClient.build().get(ModelRpcClient.class).sayHello2("CCC");
        System.out.println(ccc);
    }
}
