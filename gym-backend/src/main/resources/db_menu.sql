-- 菜单管理表结构脚本

-- 1. 创建菜单表 (sys_menu)
-- 用于管理前端动态路由和菜单，支持层级结构和权限控制
CREATE TABLE IF NOT EXISTS `sys_menu` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(50) DEFAULT NULL COMMENT '路由名称 (如: UserList)',
  `title` varchar(50) NOT NULL COMMENT '菜单标题 (如: 用户管理)',
  `path` varchar(100) DEFAULT NULL COMMENT '路由路径 (如: /user)',
  `component` varchar(255) DEFAULT NULL COMMENT 'vue组件路径 (如: gym/UserList)',
  `icon` varchar(50) DEFAULT NULL COMMENT '图标 (如: UserOutlined)',
  `parent_id` bigint(20) DEFAULT NULL COMMENT '父菜单ID',
  `sort` int(11) DEFAULT '0' COMMENT '排序',
  `roles` varchar(255) DEFAULT NULL COMMENT '允许访问的角色 (如: ADMIN,STAFF)',
  `hidden` tinyint(1) DEFAULT '0' COMMENT '是否隐藏 (1:是 0:否)',
  `component_style` varchar(50) DEFAULT NULL COMMENT '表格样式 (glass, default, null)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限表';

-- 2. 初始化核心菜单数据 (示例)
-- 注意: 为了不破坏现有结构，这里仅作为初始化参考，您可以根据实际需求调整
INSERT INTO `sys_menu` (`id`, `title`, `name`, `path`, `component`, `icon`, `parent_id`, `sort`, `roles`, `hidden`, `component_style`) VALUES 
(1, '工作台', 'DashboardPage', '/dashboard', 'DashboardPage', 'AppstoreOutlined', NULL, 1, 'ADMIN,STAFF,MEMBER', 0, 'glass'),
(2, '场馆实时监控', 'GymMonitor', '/gym-monitor', 'gym/GymMonitor', 'EnvironmentOutlined', NULL, 2, 'ADMIN,STAFF', 0, 'glass'),

(3, '场馆与器材管理', NULL, NULL, NULL, 'HomeOutlined', NULL, 3, 'ADMIN,STAFF', 0, 'default'),
(4, '场馆信息管理', 'VenueList', '/venue', 'gym/VenueList', 'EnvironmentOutlined', 3, 1, 'ADMIN,STAFF', 0, NULL),
(5, '器材信息管理', 'EquipmentList', '/equipment', 'gym/EquipmentList', 'ToolOutlined', 3, 2, 'ADMIN,STAFF', 0, NULL),

(6, '课程与预约管理', NULL, NULL, NULL, 'CalendarOutlined', NULL, 4, 'ADMIN,STAFF,MEMBER', 0, 'default'),
(7, '团课排期管理', 'CourseList', '/course', 'gym/CourseList', 'TeamOutlined', 6, 1, 'ADMIN,STAFF,MEMBER', 0, NULL),
(8, '预约记录管理', 'ReservationList', '/reservation', 'gym/ReservationList', 'ScheduleOutlined', 6, 2, 'ADMIN,STAFF,MEMBER', 0, NULL),

(9, '用户与运营管理', NULL, NULL, NULL, 'UserOutlined', NULL, 5, 'ADMIN,STAFF', 0, 'default'),
(10, '用户信息管理', 'UserList', '/user', 'gym/UserList', 'UserSwitchOutlined', 9, 1, 'ADMIN', 0, NULL),
(11, '器材报修管理', 'RepairList', '/repair', 'gym/RepairList', 'WarningOutlined', 9, 2, 'ADMIN,STAFF', 0, NULL),

(12, '教练与员工管理', NULL, NULL, NULL, 'TeamOutlined', NULL, 6, 'ADMIN', 0, 'default'),
(13, '教练信息管理', 'CoachList', '/coach', 'gym/CoachList', 'UserOutlined', 12, 1, 'ADMIN', 0, NULL),
(14, '菜单权限管理', 'MenuList', '/sys/menu', 'sys/MenuList', 'SettingOutlined', 9, 3, 'ADMIN', 0, 'glass')
ON DUPLICATE KEY UPDATE `id` = `id`;
