package com.gym.service;

import com.gym.entity.Menu;
import com.gym.entity.User;

import java.util.List;

public interface MenuPermissionService {
    String ACTION_VIEW = "view";
    String ACTION_CREATE = "create";
    String ACTION_UPDATE = "update";
    String ACTION_DELETE = "delete";

    Menu sanitizeMenu(Menu menu);

    String sanitizeUserPermissionConfig(String permissionConfig);

    List<String> resolveGrantedActions(Menu menu, String role);

    List<String> resolveEffectiveActions(Menu menu, User user);

    boolean hasMenuAction(String menuPath, String action);

    boolean hasAnyMenuAction(String action, String... menuPaths);

    boolean hasRouteAction(String routePath, String action);

    boolean hasPageAction(String routePath, String pageKey, String action);
}
