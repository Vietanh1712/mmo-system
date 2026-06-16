package com.mmo;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class TestBCrypt {

    public static void main(String[] args) {

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        System.out.println("111111111 :");
        System.out.println(encoder.encode("111111111"));

        System.out.println("222222222 :");
        System.out.println(encoder.encode("222222222"));
    }

}