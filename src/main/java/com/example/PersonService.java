package com.example;

/**
 * @author yehuisheng
 */
public interface PersonService {

    void add(String name, String sex, int age);

    Object get(String name);

    Object remove(String name);

    int count();

    int number(Integer n);

    int number(int n);

}
