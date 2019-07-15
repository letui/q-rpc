package gson.test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

public class TestBoot {
    public static void main(String[] args) throws IOException {


    }

    public static void gtest(String name,int age){
        System.out.println(name);
        System.out.println(age);
    }
}
