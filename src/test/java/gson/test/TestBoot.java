package gson.test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class TestBoot {
    public static void main(String[] args) {
        Map<String,Object> param=new HashMap<>();
        param.put("name","test");
        param.put("age",44);
        Gson gson=new Gson();

        String toJson = gson.toJson(param);
        System.out.println(toJson);

        JsonObject jp = gson.fromJson(toJson, new TypeToken<JsonObject>() {
        }.getType());
        System.out.println(jp.toString());

        try {

           Object[] ff2=new Object[2];
           ff2[0]=jp.get("name").getAsString();
           ff2[1]=jp.get("age").getAsInt();


           Method method= TestBoot.class.getMethod("gtest",String.class,int.class);

           Integer a=2333;
            String s = gson.toJson(a);
            System.out.println(s);

            Integer integer = gson.fromJson(s, Integer.class);
            System.out.println(integer.intValue());

            method.invoke(TestBoot.class,ff2);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    public static void gtest(String name,int age){
        System.out.println(name);
        System.out.println(age);
    }
}
