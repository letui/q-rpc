package test.rpc;

import io.letuismart.rpc.registry.QRpcBoot;
import io.letuismart.rpc.spec.RpcScan;

@RpcScan(packages = "test.rpc")
public class TestBoot {
    public static void main(String[] args) {
        try {
            QRpcBoot.run(TestBoot.class, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
