package com.gym.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gym.entity.Menu;
import com.gym.service.MenuService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 自动迁移脚本：添加统一的系统主菜单根目录
 */
@Component
public class MenuMigrationRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(MenuMigrationRunner.class);

    @Autowired
    private MenuService menuService;

    @Override
    public void run(String... args) throws Exception {
        Long rootId = 999L;
        Menu root = menuService.getById(rootId);

        // 1. 检查是否存在根菜单
        if (root == null) {
            logger.info("系统主菜单根目录 (ID: 999) 不存在，正在创建...");
            root = new Menu();
            root.setId(rootId);
            root.setTitle("系统主菜单");
            root.setSort(0);
            root.setRoles("ADMIN,STAFF,COACH,MEMBER");
            root.setHidden(false); // 显示在菜单中
            root.setComponentStyle("default"); // 默认样式
            root.setParentId(null);

            // 为了避免 name/path 为空导致的问题，设置虚拟值，虽然作为目录只有 children
            root.setName("SystemRoot");
            root.setPath("/system-root");
            root.setComponent("Layout"); // 通常目录设置为 Layout 或空，这里仅作占位
            root.setIcon("AppstoreOutlined");

            menuService.save(root);
            logger.info("系统主菜单根目录创建成功。");

            // 2. 迁移所有现有的顶级菜单到此根目录下
            LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
            wrapper.isNull(Menu::getParentId);
            wrapper.ne(Menu::getId, rootId); // 排除自己

            List<Menu> orphans = menuService.list(wrapper);
            if (!orphans.isEmpty()) {
                logger.info("发现 {} 个顶级菜单，正在迁移至主菜单下...", orphans.size());
                for (Menu m : orphans) {
                    m.setParentId(rootId);
                    // 仅当样式为 default 时设为继承? 不，保持原样，原样是 top level 可能有自己的 style。
                    // 现有逻辑: 如果子菜单 componentStyle=null，则继承父级。
                    // 用户希望 "Gym Monitor can inherit".
                    // 如果 Gym Monitor DB里是 glass，则保持 glass。
                    // 如果用户之后改成 inherit，则变成父级(root)的 default。
                    // 这符合逻辑。
                    menuService.updateById(m);
                }
                logger.info("菜单层级迁移完成。");
            }
        } else {
            logger.info("系统主菜单 (ID: 999) 已存在，跳过迁移。");
        }

        ensureFormConfigMenu();
        ensureRepairMenu();
    }

    private void ensureFormConfigMenu() {
        Menu existing = menuService.getOne(new LambdaQueryWrapper<Menu>()
            .eq(Menu::getPath, "/sys/form-config")
            .last("limit 1"));
        if (existing != null) {
            logger.info("表单管理菜单已存在，跳过创建。");
            return;
        }

        Menu systemMenu = menuService.getOne(new LambdaQueryWrapper<Menu>()
            .eq(Menu::getTitle, "系统管理")
            .last("limit 1"));
        Long parentId = systemMenu != null ? systemMenu.getId() : 999L;

        Menu menu = new Menu();
        menu.setTitle("表单管理");
        menu.setName("FormConfigList");
        menu.setPath("/sys/form-config");
        menu.setComponent("sys/FormConfigList");
        menu.setIcon("FormOutlined");
        menu.setParentId(parentId);
        menu.setSort(4);
        menu.setRoles("ADMIN");
        menu.setHidden(false);
        menu.setComponentStyle("glass");
        menuService.save(menu);
        logger.info("表单管理菜单已创建，父级菜单 ID: {}", parentId);
    }

    private void ensureRepairMenu() {
        Menu existing = menuService.getOne(new LambdaQueryWrapper<Menu>()
            .eq(Menu::getPath, "/repair")
            .last("limit 1"));
        if (existing != null) {
            logger.info("报修工单菜单已存在，跳过创建。");
            return;
        }

        Long parentId = resolveParentId("场馆与资源", 999L);

        Menu menu = new Menu();
        menu.setTitle("报修工单管理");
        menu.setName("RepairList");
        menu.setPath("/repair");
        menu.setComponent("gym/RepairList");
        menu.setIcon("ToolOutlined");
        menu.setParentId(parentId);
        menu.setSort(4);
        menu.setRoles("ADMIN,STAFF");
        menu.setHidden(false);
        menu.setComponentStyle("default");
        menuService.save(menu);
        logger.info("报修工单菜单已创建，父级菜单 ID: {}", parentId);
    }

    private Long resolveParentId(String title, Long fallbackId) {
        Menu parent = menuService.getOne(new LambdaQueryWrapper<Menu>()
            .eq(Menu::getTitle, title)
            .last("limit 1"));
        return parent != null ? parent.getId() : fallbackId;
    }
}
