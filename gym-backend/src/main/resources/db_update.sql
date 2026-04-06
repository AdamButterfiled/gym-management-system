-- 教练管理与字典表结构脚本

-- 1. 创建教练表 (gym_coach)
-- 如果表不存在则创建
CREATE TABLE IF NOT EXISTS `gym_coach` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(50) NOT NULL COMMENT '教练姓名',
  `gender` int(11) DEFAULT NULL COMMENT '性别 1:男 0:女',
  `age` int(11) DEFAULT NULL COMMENT '年龄',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `specialization` varchar(255) DEFAULT NULL COMMENT '擅长项目(如: 瑜伽, 力量训练)',
  `entry_date` date DEFAULT NULL COMMENT '入职日期',
  `intro` text COMMENT '个人简介',
  `status` int(11) DEFAULT '1' COMMENT '状态 1:在职 0:离职',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教练表';

-- 2. 创建字典表 (sys_dict)
-- 用于存储系统通用的字典配置，如性别、状态等
CREATE TABLE IF NOT EXISTS `sys_dict` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dict_type` varchar(50) NOT NULL COMMENT '字典类型 (如: gender)',
  `dict_label` varchar(50) NOT NULL COMMENT '字典标签 (如: 男)',
  `dict_value` varchar(50) NOT NULL COMMENT '字典键值 (如: 1)',
  `sort` int(11) DEFAULT '0' COMMENT '排序字段',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典表';

-- 3. 初始化字典数据
INSERT INTO `sys_dict` (`dict_type`, `dict_label`, `dict_value`, `sort`)
SELECT 'gender', '男', '1', 1
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_dict` WHERE `dict_type` = 'gender' AND `dict_value` = '1'
);

INSERT INTO `sys_dict` (`dict_type`, `dict_label`, `dict_value`, `sort`)
SELECT 'gender', '女', '0', 2
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_dict` WHERE `dict_type` = 'gender' AND `dict_value` = '0'
);

INSERT INTO `sys_dict` (`dict_type`, `dict_label`, `dict_value`, `sort`)
SELECT 'coach_status', '在职', '1', 1
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_dict` WHERE `dict_type` = 'coach_status' AND `dict_value` = '1'
);

INSERT INTO `sys_dict` (`dict_type`, `dict_label`, `dict_value`, `sort`)
SELECT 'coach_status', '离职', '0', 2
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_dict` WHERE `dict_type` = 'coach_status' AND `dict_value` = '0'
);

-- Add styling column to sys_menu
SET @component_style_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_menu'
    AND COLUMN_NAME = 'component_style'
);
SET @component_style_sql = IF(
  @component_style_exists = 0,
  'ALTER TABLE `sys_menu` ADD COLUMN `component_style` VARCHAR(50) DEFAULT NULL COMMENT ''组件样式 (NULL:Inherit, glass:Glass, default:Default)''',
  'SELECT 1'
);
PREPARE component_style_stmt FROM @component_style_sql;
EXECUTE component_style_stmt;
DEALLOCATE PREPARE component_style_stmt;

-- Initial data update for component_style
-- Make '系统管理' (System Management) menu use 'glass' style by default as an example
UPDATE `sys_menu`
SET `component_style` = 'glass'
WHERE `path` = '/sys/menu'
  AND (`component_style` IS NULL OR `component_style` = '');
