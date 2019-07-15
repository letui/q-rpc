package test.rpc;

import io.letuismart.rpc.registry.QRpcClient;
import test.rpc.service.ModelRpcClient;

public class TestCall {
    public static void main(String[] args) {

//        for(int i = 0;i< 1;i++){
//            new Thread(new Runnable() {
//                @Override
//                public void run() {

        ModelRpcClient client = QRpcClient.build(ModelRpcClient.class);
        System.out.println(System.currentTimeMillis());
        String ccc = client.sayHello2("CCC");
        System.out.println(ccc);
        System.out.println(System.currentTimeMillis());
//                }
//            }).start();
//        }
    }
}
