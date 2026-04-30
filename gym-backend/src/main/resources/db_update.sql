CREATE TABLE IF NOT EXISTS `sys_dict` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dict_type` varchar(50) NOT NULL COMMENT '字典类型',
  `dict_label` varchar(50) NOT NULL COMMENT '字典标签',
  `dict_value` varchar(50) NOT NULL COMMENT '字典键值',
  `sort` int(11) DEFAULT '0' COMMENT '排序字段',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典表';

INSERT INTO `sys_dict` (`dict_type`, `dict_label`, `dict_value`, `sort`)
SELECT 'gender', '男', '1', 1
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `dict_type` = 'gender' AND `dict_value` = '1');

INSERT INTO `sys_dict` (`dict_type`, `dict_label`, `dict_value`, `sort`)
SELECT 'gender', '女', '0', 2
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `dict_type` = 'gender' AND `dict_value` = '0');

INSERT INTO `sys_dict` (`dict_type`, `dict_label`, `dict_value`, `sort`)
SELECT 'coach_status', '在职', '1', 1
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `dict_type` = 'coach_status' AND `dict_value` = '1');

INSERT INTO `sys_dict` (`dict_type`, `dict_label`, `dict_value`, `sort`)
SELECT 'coach_status', '离职', '0', 2
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `dict_type` = 'coach_status' AND `dict_value` = '0');

INSERT INTO `sys_dict` (`dict_type`, `dict_label`, `dict_value`, `sort`)
SELECT 'booking_status', '待确认', 'CREATED', 1
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `dict_type` = 'booking_status' AND `dict_value` = 'CREATED');

INSERT INTO `sys_dict` (`dict_type`, `dict_label`, `dict_value`, `sort`)
SELECT 'booking_status', '待支付', 'PENDING_PAY', 2
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `dict_type` = 'booking_status' AND `dict_value` = 'PENDING_PAY');

INSERT INTO `sys_dict` (`dict_type`, `dict_label`, `dict_value`, `sort`)
SELECT 'booking_status', '已确认', 'CONFIRMED', 3
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `dict_type` = 'booking_status' AND `dict_value` = 'CONFIRMED');

INSERT INTO `sys_dict` (`dict_type`, `dict_label`, `dict_value`, `sort`)
SELECT 'booking_status', '已取消', 'CANCELLED', 4
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `dict_type` = 'booking_status' AND `dict_value` = 'CANCELLED');

INSERT INTO `sys_dict` (`dict_type`, `dict_label`, `dict_value`, `sort`)
SELECT 'booking_status', '已完成', 'COMPLETED', 5
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `dict_type` = 'booking_status' AND `dict_value` = 'COMPLETED');

INSERT INTO `sys_dict` (`dict_type`, `dict_label`, `dict_value`, `sort`)
SELECT 'booking_status', '已签到', 'CHECKED_IN', 6
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `dict_type` = 'booking_status' AND `dict_value` = 'CHECKED_IN');

INSERT INTO `sys_dict` (`dict_type`, `dict_label`, `dict_value`, `sort`)
SELECT 'booking_status', '已退款', 'REFUNDED', 7
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `dict_type` = 'booking_status' AND `dict_value` = 'REFUNDED');

INSERT INTO `sys_dict` (`dict_type`, `dict_label`, `dict_value`, `sort`)
SELECT 'payment_status', '待支付', 'UNPAID', 1
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `dict_type` = 'payment_status' AND `dict_value` = 'UNPAID');

INSERT INTO `sys_dict` (`dict_type`, `dict_label`, `dict_value`, `sort`)
SELECT 'payment_status', '已支付', 'PAID', 2
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `dict_type` = 'payment_status' AND `dict_value` = 'PAID');

INSERT INTO `sys_dict` (`dict_type`, `dict_label`, `dict_value`, `sort`)
SELECT 'payment_status', '已关闭', 'CLOSED', 3
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `dict_type` = 'payment_status' AND `dict_value` = 'CLOSED');

INSERT INTO `sys_dict` (`dict_type`, `dict_label`, `dict_value`, `sort`)
SELECT 'payment_status', '已退款', 'REFUNDED', 4
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `dict_type` = 'payment_status' AND `dict_value` = 'REFUNDED');

INSERT INTO `sys_dict` (`dict_type`, `dict_label`, `dict_value`, `sort`)
SELECT 'slot_status', '开放', 'OPEN', 1
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `dict_type` = 'slot_status' AND `dict_value` = 'OPEN');

INSERT INTO `sys_dict` (`dict_type`, `dict_label`, `dict_value`, `sort`)
SELECT 'slot_status', '锁定', 'LOCKED', 2
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `dict_type` = 'slot_status' AND `dict_value` = 'LOCKED');

INSERT INTO `sys_dict` (`dict_type`, `dict_label`, `dict_value`, `sort`)
SELECT 'slot_status', '已满', 'FULL', 3
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `dict_type` = 'slot_status' AND `dict_value` = 'FULL');

INSERT INTO `sys_dict` (`dict_type`, `dict_label`, `dict_value`, `sort`)
SELECT 'slot_status', '关闭', 'CLOSED', 4
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `dict_type` = 'slot_status' AND `dict_value` = 'CLOSED');

SET @component_style_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_menu'
    AND COLUMN_NAME = 'component_style'
);
SET @component_style_sql = IF(
  @component_style_exists = 0,
  'ALTER TABLE `sys_menu` ADD COLUMN `component_style` VARCHAR(50) DEFAULT NULL COMMENT ''组件样式''',
  'SELECT 1'
);
PREPARE component_style_stmt FROM @component_style_sql;
EXECUTE component_style_stmt;
DEALLOCATE PREPARE component_style_stmt;

SET @permission_config_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_menu'
    AND COLUMN_NAME = 'permission_config'
);
SET @permission_config_sql = IF(
  @permission_config_exists = 0,
  'ALTER TABLE `sys_menu` ADD COLUMN `permission_config` LONGTEXT COMMENT ''角色动作权限配置 JSON''',
  'SELECT 1'
);
PREPARE permission_config_stmt FROM @permission_config_sql;
EXECUTE permission_config_stmt;
DEALLOCATE PREPARE permission_config_stmt;

SET @sys_user_permission_config_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_user'
    AND COLUMN_NAME = 'permission_config'
);
SET @sys_user_permission_config_sql = IF(
  @sys_user_permission_config_exists = 0,
  'ALTER TABLE `sys_user` ADD COLUMN `permission_config` LONGTEXT COMMENT ''账号菜单权限覆盖 JSON''',
  'SELECT 1'
);
PREPARE sys_user_permission_config_stmt FROM @sys_user_permission_config_sql;
EXECUTE sys_user_permission_config_stmt;
DEALLOCATE PREPARE sys_user_permission_config_stmt;

INSERT INTO `sys_menu` (`id`, `title`, `name`, `path`, `component`, `icon`, `parent_id`, `sort`, `roles`, `hidden`, `component_style`)
VALUES (21, '角色权限管理', 'PermissionList', '/sys/permissions', 'sys/PermissionList', 'UserSwitchOutlined', 15, 5, 'ADMIN', 0, 'glass')
ON DUPLICATE KEY UPDATE
`title` = VALUES(`title`),
`name` = VALUES(`name`),
`path` = VALUES(`path`),
`component` = VALUES(`component`),
`icon` = VALUES(`icon`),
`parent_id` = VALUES(`parent_id`),
`sort` = VALUES(`sort`),
`roles` = VALUES(`roles`),
`hidden` = VALUES(`hidden`),
`component_style` = VALUES(`component_style`);

CREATE TABLE IF NOT EXISTS `sys_form_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `page_key` varchar(100) NOT NULL COMMENT '页面唯一键',
  `route_path` varchar(150) NOT NULL COMMENT '路由路径',
  `page_title` varchar(100) NOT NULL COMMENT '页面标题',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `config_json` longtext NOT NULL COMMENT '页面配置JSON',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_form_config_page_key` (`page_key`),
  UNIQUE KEY `uk_form_config_route_path` (`route_path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表单配置表';

SET @booking_item_member_package_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'gym_booking_item'
    AND COLUMN_NAME = 'member_package_id'
);
SET @booking_item_member_package_sql = IF(
  @booking_item_member_package_exists = 0,
  'ALTER TABLE `gym_booking_item` ADD COLUMN `member_package_id` BIGINT DEFAULT NULL AFTER `package_id`',
  'SELECT 1'
);
PREPARE booking_item_member_package_stmt FROM @booking_item_member_package_sql;
EXECUTE booking_item_member_package_stmt;
DEALLOCATE PREPARE booking_item_member_package_stmt;
