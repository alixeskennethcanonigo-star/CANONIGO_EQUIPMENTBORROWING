package com.scc;

import com.scc.dao.UserDao;
import com.scc.ui.Authentication;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        // 1. Instantiate DAOs & Utilities (Database tables are managed manually)
        UserDao userDao = new UserDao();
        Scanner scanner = new Scanner(System.in);

        // 2. Delegate execution to Authentication start flow
        Authentication auth = new Authentication(userDao, scanner);
        auth.start();
    }
}