CREATE TABLE IF NOT EXISTS `sys_menu` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(50) DEFAULT NULL COMMENT '路由名称',
  `title` varchar(50) NOT NULL COMMENT '菜单标题',
  `path` varchar(100) DEFAULT NULL COMMENT '路由路径',
  `component` varchar(255) DEFAULT NULL COMMENT 'vue组件路径',
  `icon` varchar(50) DEFAULT NULL COMMENT '图标',
  `parent_id` bigint(20) DEFAULT NULL COMMENT '父菜单ID',
  `sort` int(11) DEFAULT '0' COMMENT '排序',
  `roles` varchar(255) DEFAULT NULL COMMENT '允许访问的角色',
  `hidden` tinyint(1) DEFAULT '0' COMMENT '是否隐藏',
  `component_style` varchar(50) DEFAULT NULL COMMENT '页面风格',
  `permission_config` longtext COMMENT '角色动作权限配置 JSON',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限表';

INSERT INTO `sys_menu` (`id`, `title`, `name`, `path`, `component`, `icon`, `parent_id`, `sort`, `roles`, `hidden`, `component_style`) VALUES
(1, '工作台', 'DashboardPage', '/dashboard', 'DashboardPage', 'AppstoreOutlined', NULL, 1, 'ADMIN,STAFF', 0, 'glass'),
(2, '场馆与资源', NULL, NULL, NULL, 'EnvironmentOutlined', NULL, 2, 'ADMIN,STAFF', 0, 'default'),
(3, '场馆资源管理', 'VenueList', '/venue', 'gym/VenueList', 'EnvironmentOutlined', 2, 1, 'ADMIN,STAFF', 0, NULL),
(4, '团课排期管理', 'CourseList', '/course', 'gym/CourseList', 'CalendarOutlined', 2, 2, 'ADMIN,STAFF', 0, NULL),
(5, '私教排班管理', 'PrivateScheduleList', '/private-schedule', 'admin/PrivateScheduleList', 'ScheduleOutlined', 2, 3, 'ADMIN,STAFF', 0, NULL),
(20, '报修工单管理', 'RepairList', '/repair', 'gym/RepairList', 'ToolOutlined', 2, 4, 'ADMIN,STAFF', 0, NULL),

(6, '会员与订单', NULL, NULL, NULL, 'UserOutlined', NULL, 3, 'ADMIN,STAFF', 0, 'default'),
(7, '预约订单管理', 'ReservationList', '/reservation', 'gym/ReservationList', 'ScheduleOutlined', 6, 1, 'ADMIN,STAFF', 0, NULL),
(8, '会员资产中心', 'MemberAssetList', '/member-assets', 'admin/MemberAssetList', 'UserSwitchOutlined', 6, 2, 'ADMIN,STAFF', 0, NULL),
(9, '支付订单管理', 'PaymentOrderList', '/payment-orders', 'admin/PaymentOrderList', 'WalletOutlined', 6, 3, 'ADMIN,STAFF', 0, NULL),
(10, '签到核销记录', 'CheckinList', '/checkin-records', 'admin/CheckinList', 'QrcodeOutlined', 6, 4, 'ADMIN,STAFF', 0, NULL),

(11, '教练与运营', NULL, NULL, NULL, 'TeamOutlined', NULL, 4, 'ADMIN,STAFF', 0, 'default'),
(12, '教练信息管理', 'CoachList', '/coach', 'gym/CoachList', 'TeamOutlined', 11, 1, 'ADMIN,STAFF', 0, NULL),
(13, '排期冲突日志', 'ConflictList', '/schedule-conflicts', 'admin/ConflictList', 'WarningOutlined', 11, 2, 'ADMIN,STAFF', 0, NULL),
(14, '经营数据分析', 'AnalyticsPage', '/analytics', 'admin/AnalyticsPage', 'BarChartOutlined', 11, 3, 'ADMIN,STAFF', 0, 'glass'),

(15, '系统管理', NULL, NULL, NULL, 'SettingOutlined', NULL, 5, 'ADMIN', 0, 'glass'),
(16, '用户信息管理', 'UserList', '/user', 'gym/UserList', 'UserOutlined', 15, 1, 'ADMIN', 0, NULL),
(17, '菜单权限管理', 'MenuList', '/sys/menu', 'sys/MenuList', 'SettingOutlined', 15, 2, 'ADMIN', 0, 'glass'),
(18, '数据字典管理', 'DictList', '/sys/dict', 'sys/DictList', 'AppstoreOutlined', 15, 3, 'ADMIN', 0, 'glass'),
(19, '表单管理', 'FormConfigList', '/sys/form-config', 'sys/FormConfigList', 'FormOutlined', 15, 4, 'ADMIN', 0, 'glass'),
(21, '角色权限管理', 'PermissionList', '/sys/permissions', 'sys/PermissionList', 'UserSwitchOutlined', 15, 5, 'ADMIN', 0, 'glass')
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
