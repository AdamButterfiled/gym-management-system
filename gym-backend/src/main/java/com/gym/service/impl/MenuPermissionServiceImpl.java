package com.gym.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.entity.Menu;
import com.gym.entity.User;
import com.gym.mapper.MenuMapper;
import com.gym.mapper.UserMapper;
import com.gym.service.MenuPermissionService;
import com.gym.utils.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class MenuPermissionServiceImpl implements MenuPermissionService {
    private static final TypeReference<Map<String, List<String>>> PERMISSION_CONFIG_TYPE =
            new TypeReference<>() {
            };
    private static final List<String> CRUD_ACTIONS = List.of(
            ACTION_VIEW,
            ACTION_CREATE,
            ACTION_UPDATE,
            ACTION_DELETE
    );

    private final MenuMapper menuMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    public MenuPermissionServiceImpl(MenuMapper menuMapper, UserMapper userMapper, ObjectMapper objectMapper) {
        this.menuMapper = menuMapper;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Menu sanitizeMenu(Menu menu) {
        if (menu == null) {
            return null;
        }

        List<String> roles = parseRoles(menu.getRoles());
        boolean leafMenu = supportsCrud(menu);
        Map<String, List<String>> rawConfig = parsePermissionConfig(menu.getPermissionConfig());
        Map<String, List<String>> normalizedConfig = new LinkedHashMap<>();

        for (String role : roles) {
            List<String> actions = normalizeActions(rawConfig.get(role), leafMenu);
            if (actions.isEmpty()) {
                actions = defaultActionsFor(leafMenu);
            }
            normalizedConfig.put(role, actions);
        }

        menu.setRoles(String.join(",", roles));
        menu.setPermissionConfig(writePermissionConfig(normalizedConfig));
        return menu;
    }

    @Override
    public String sanitizeUserPermissionConfig(String permissionConfig) {
        Map<String, List<String>> rawConfig = parsePermissionConfig(permissionConfig);
        if (rawConfig.isEmpty()) {
            return null;
        }

        Map<String, Menu> menuByKey = loadMenuByKey();
        Map<String, List<String>> normalizedConfig = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : rawConfig.entrySet()) {
            String normalizedKey = normalizePermissionKey(entry.getKey());
            if (!StringUtils.hasText(normalizedKey)) {
                continue;
            }

            Menu targetMenu = menuByKey.get(normalizedKey);
            if (targetMenu == null) {
                continue;
            }

            normalizedConfig.put(
                    normalizedKey,
                    normalizeUserActions(entry.getValue(), supportsCrud(targetMenu))
            );
        }
        return writePermissionConfig(normalizedConfig);
    }

    @Override
    public List<String> resolveGrantedActions(Menu menu, String role) {
        if (menu == null || !StringUtils.hasText(role)) {
            return Collections.emptyList();
        }

        List<String> roles = parseRoles(menu.getRoles());
        if (!roles.contains(role.trim())) {
            return Collections.emptyList();
        }

        boolean leafMenu = supportsCrud(menu);
        Map<String, List<String>> config = parsePermissionConfig(menu.getPermissionConfig());
        List<String> actions = normalizeActions(config.get(role.trim()), leafMenu);
        if (actions.isEmpty()) {
            actions = defaultActionsFor(leafMenu);
        }
        return actions;
    }

    @Override
    public List<String> resolveEffectiveActions(Menu menu, User user) {
        if (menu == null || user == null) {
            return Collections.emptyList();
        }

        Map<String, List<String>> userConfig = parsePermissionConfig(user.getPermissionConfig());
        String permissionKey = buildPermissionKey(menu);
        if (StringUtils.hasText(permissionKey) && userConfig.containsKey(permissionKey)) {
            return normalizeUserActions(userConfig.get(permissionKey), supportsCrud(menu));
        }

        return resolveGrantedActions(menu, user.getRole());
    }

    @Override
    public boolean hasMenuAction(String menuPath, String action) {
        User currentUser = findCurrentUser();
        if (currentUser == null) {
            return false;
        }

        return hasMenuAction(currentUser, menuPath, action);
    }

    @Override
    public boolean hasAnyMenuAction(String action, String... menuPaths) {
        if (menuPaths == null || menuPaths.length == 0) {
            return false;
        }

        User currentUser = findCurrentUser();
        if (currentUser == null) {
            return false;
        }

        return Arrays.stream(menuPaths)
                .filter(StringUtils::hasText)
                .anyMatch(menuPath -> hasMenuAction(currentUser, menuPath, action));
    }

    @Override
    public boolean hasRouteAction(String routePath, String action) {
        User currentUser = findCurrentUser();
        if (currentUser == null) {
            return false;
        }
        return hasMenuAction(currentUser, routePath, action);
    }

    @Override
    public boolean hasPageAction(String routePath, String pageKey, String action) {
        User currentUser = findCurrentUser();
        if (currentUser == null) {
            return false;
        }

        if (hasMenuAction(currentUser, routePath, action)) {
            return true;
        }

        if (!StringUtils.hasText(pageKey)) {
            return false;
        }

        for (String candidate : buildPageCandidates(pageKey)) {
            if (hasMenuAction(currentUser, candidate, action)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasMenuAction(User user, String menuPath, String action) {
        Menu menu = findMenu(menuPath);
        if (menu == null) {
            return false;
        }

        return resolveEffectiveActions(menu, user).contains(normalizeAction(action));
    }

    private Menu findMenu(String menuPath) {
        String normalizedPath = normalizePath(menuPath);
        if (!StringUtils.hasText(normalizedPath)) {
            return null;
        }

        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Menu::getPath, normalizedPath).last("LIMIT 1");
        return menuMapper.selectOne(wrapper);
    }

    private List<String> buildPageCandidates(String pageKey) {
        Set<String> candidates = new LinkedHashSet<>();
        String trimmed = pageKey == null ? "" : pageKey.trim();
        if (!StringUtils.hasText(trimmed)) {
            return Collections.emptyList();
        }

        candidates.add(normalizePath(trimmed));
        candidates.add(normalizePath(trimmed.replace('_', '-')));
        candidates.add(normalizePath(trimmed.replace('.', '/')));

        return candidates.stream()
                .filter(StringUtils::hasText)
                .toList();
    }

    private List<String> parseRoles(String roles) {
        if (!StringUtils.hasText(roles)) {
            return Collections.emptyList();
        }

        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private Map<String, List<String>> parsePermissionConfig(String permissionConfig) {
        if (!StringUtils.hasText(permissionConfig)) {
            return Collections.emptyMap();
        }

        try {
            Map<String, List<String>> parsed = objectMapper.readValue(permissionConfig, PERMISSION_CONFIG_TYPE);
            return parsed == null ? Collections.emptyMap() : parsed;
        } catch (Exception ex) {
            return Collections.emptyMap();
        }
    }

    private String writePermissionConfig(Map<String, List<String>> permissionConfig) {
        if (permissionConfig == null || permissionConfig.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(permissionConfig);
        } catch (Exception ex) {
            return null;
        }
    }

    private List<String> normalizeActions(Collection<String> actions, boolean leafMenu) {
        if (actions == null || actions.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> supportedActions = leafMenu ? CRUD_ACTIONS : Collections.singletonList(ACTION_VIEW);
        Set<String> normalized = new LinkedHashSet<>();
        for (String action : actions) {
            String normalizedAction = normalizeAction(action);
            if (supportedActions.contains(normalizedAction)) {
                normalized.add(normalizedAction);
            }
        }

        if (!normalized.isEmpty()) {
            normalized.add(ACTION_VIEW);
        }
        return new ArrayList<>(normalized);
    }

    private List<String> normalizeUserActions(Collection<String> actions, boolean leafMenu) {
        if (actions == null) {
            return Collections.emptyList();
        }
        return normalizeActions(actions, leafMenu);
    }

    private List<String> defaultActionsFor(boolean leafMenu) {
        return leafMenu ? new ArrayList<>(CRUD_ACTIONS) : new ArrayList<>(Collections.singletonList(ACTION_VIEW));
    }

    private User findCurrentUser() {
        String currentUsername = SecurityUtils.getCurrentUsername();
        if (!StringUtils.hasText(currentUsername)) {
            return null;
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, currentUsername).last("LIMIT 1");
        return userMapper.selectOne(wrapper);
    }

    private Map<String, Menu> loadMenuByKey() {
        List<Menu> menus = menuMapper.selectList(new LambdaQueryWrapper<>());
        Map<String, Menu> menuByKey = new LinkedHashMap<>();
        for (Menu menu : menus) {
            String permissionKey = buildPermissionKey(menu);
            if (StringUtils.hasText(permissionKey)) {
                menuByKey.put(permissionKey, menu);
            }
        }
        return menuByKey;
    }

    private String buildPermissionKey(Menu menu) {
        if (menu == null) {
            return null;
        }

        String normalizedPath = normalizePath(menu.getPath());
        if (StringUtils.hasText(normalizedPath)) {
            return normalizedPath;
        }

        if (menu.getId() != null) {
            return "menu:" + menu.getId();
        }
        return null;
    }

    private String normalizePermissionKey(String permissionKey) {
        if (!StringUtils.hasText(permissionKey)) {
            return null;
        }

        String trimmed = permissionKey.trim();
        if (trimmed.regionMatches(true, 0, "menu:", 0, "menu:".length())) {
            String idText = trimmed.substring("menu:".length()).trim();
            if (!StringUtils.hasText(idText)) {
                return null;
            }

            try {
                return "menu:" + Long.parseLong(idText);
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        return normalizePath(trimmed);
    }

    private boolean supportsCrud(Menu menu) {
        return StringUtils.hasText(normalizePath(menu.getPath())) && !isPlaceholderComponent(menu.getComponent());
    }

    private boolean isPlaceholderComponent(String component) {
        String normalized = component == null ? "" : component.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() || "layout".equals(normalized);
    }

    private String normalizeAction(String action) {
        return action == null ? "" : action.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePath(String path) {
        if (!StringUtils.hasText(path)) {
            return null;
        }

        String normalized = path.trim().replace("\\", "/");
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
