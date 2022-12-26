package com.example;

import com.google.gson.JsonObject;
import com.netty.annotation.Service;
import com.netty.util.ObjectUtil;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yehuisheng
 */
@Service
public class PersonServiceImpl implements PersonService {

    private final Map<String, JsonObject> personMap = new ConcurrentHashMap<>();

    @Override
    public void add(String name, String sex, int age) {
        if (ObjectUtil.isEmpty(name) || ObjectUtil.isEmpty(sex)
                || (!"男".equals(sex) && !"女".equals(sex))) {
            return;
        }
        JsonObject person = new JsonObject();
        person.addProperty("name", name);
        person.addProperty("sex", sex);
        person.addProperty("age", age);
        personMap.put(name, person);
    }

    @Override
    public JsonObject get(String name) {
        return ObjectUtil.isEmpty(name) ? null : personMap.get(name);
    }

    @Override
    public JsonObject remove(String name) {
        return ObjectUtil.isEmpty(name) ? null : personMap.remove(name);
    }

    @Override
    public int count() {
        return personMap.size();
    }

    @Override
    public int number(Integer n) {
        return n == null ? -1 : n + 10;
    }

    @Override
    public int number(int n) {
        return n;
    }

}
