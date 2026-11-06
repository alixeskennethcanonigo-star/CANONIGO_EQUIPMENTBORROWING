package com.scc.ui;

import com.scc.dao.UserDao;

import java.util.Scanner;

public class MainMenu {

    private final Scanner scanner = new Scanner(System.in);
    private final UserDao userDao = new UserDao();

    public void show() {
        Authentication auth = new Authentication(userDao, scanner);
        auth.start();
    }
}