CREATE DATABASE IF NOT EXISTS gym_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE gym_db;

CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'User ID',
  `username` VARCHAR(50) NOT NULL COMMENT 'Username',
  `password` VARCHAR(100) NOT NULL COMMENT 'Password',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT 'Real Name',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT 'Nickname',
  `email` VARCHAR(100) DEFAULT NULL COMMENT 'Email',
  `role` VARCHAR(20) NOT NULL COMMENT 'Role: ADMIN, MEMBER, COACH',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT 'Phone Number',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT 'Avatar URL',
  `balance` DECIMAL(10,2) DEFAULT 0.00 COMMENT 'Account Balance',
  `type` VARCHAR(20) DEFAULT 'REGULAR' COMMENT 'Member Type',
  `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'User Status',
  `permission_config` LONGTEXT DEFAULT NULL COMMENT 'Account permission override JSON',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Unified user table';

SET @sys_user_avatar_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_user'
    AND COLUMN_NAME = 'avatar'
);
SET @sys_user_avatar_sql = IF(
  @sys_user_avatar_exists = 0,
  'ALTER TABLE `sys_user` ADD COLUMN `avatar` VARCHAR(255) DEFAULT NULL COMMENT ''Avatar URL''',
  'SELECT 1'
);
PREPARE sys_user_avatar_stmt FROM @sys_user_avatar_sql;
EXECUTE sys_user_avatar_stmt;
DEALLOCATE PREPARE sys_user_avatar_stmt;

SET @sys_user_balance_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_user'
    AND COLUMN_NAME = 'balance'
);
SET @sys_user_balance_sql = IF(
  @sys_user_balance_exists = 0,
  'ALTER TABLE `sys_user` ADD COLUMN `balance` DECIMAL(10,2) DEFAULT 0.00 COMMENT ''Account Balance''',
  'SELECT 1'
);
PREPARE sys_user_balance_stmt FROM @sys_user_balance_sql;
EXECUTE sys_user_balance_stmt;
DEALLOCATE PREPARE sys_user_balance_stmt;

SET @sys_user_type_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_user'
    AND COLUMN_NAME = 'type'
);
SET @sys_user_type_sql = IF(
  @sys_user_type_exists = 0,
  'ALTER TABLE `sys_user` ADD COLUMN `type` VARCHAR(20) DEFAULT ''REGULAR'' COMMENT ''Member Type''',
  'SELECT 1'
);
PREPARE sys_user_type_stmt FROM @sys_user_type_sql;
EXECUTE sys_user_type_stmt;
DEALLOCATE PREPARE sys_user_type_stmt;

SET @sys_user_status_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_user'
    AND COLUMN_NAME = 'status'
);
SET @sys_user_status_sql = IF(
  @sys_user_status_exists = 0,
  'ALTER TABLE `sys_user` ADD COLUMN `status` VARCHAR(20) DEFAULT ''ACTIVE'' COMMENT ''User Status''',
  'SELECT 1'
);
PREPARE sys_user_status_stmt FROM @sys_user_status_sql;
EXECUTE sys_user_status_stmt;
DEALLOCATE PREPARE sys_user_status_stmt;

SET @sys_user_created_at_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_user'
    AND COLUMN_NAME = 'created_at'
);
SET @sys_user_created_at_sql = IF(
  @sys_user_created_at_exists = 0,
  'ALTER TABLE `sys_user` ADD COLUMN `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT ''Creation Time''',
  'SELECT 1'
);
PREPARE sys_user_created_at_stmt FROM @sys_user_created_at_sql;
EXECUTE sys_user_created_at_stmt;
DEALLOCATE PREPARE sys_user_created_at_stmt;

SET @sys_user_nickname_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_user'
    AND COLUMN_NAME = 'nickname'
);
SET @sys_user_nickname_sql = IF(
  @sys_user_nickname_exists = 0,
  'ALTER TABLE `sys_user` ADD COLUMN `nickname` VARCHAR(50) DEFAULT NULL COMMENT ''Nickname''',
  'SELECT 1'
);
PREPARE sys_user_nickname_stmt FROM @sys_user_nickname_sql;
EXECUTE sys_user_nickname_stmt;
DEALLOCATE PREPARE sys_user_nickname_stmt;

SET @sys_user_email_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_user'
    AND COLUMN_NAME = 'email'
);
SET @sys_user_email_sql = IF(
  @sys_user_email_exists = 0,
  'ALTER TABLE `sys_user` ADD COLUMN `email` VARCHAR(100) DEFAULT NULL COMMENT ''Email''',
  'SELECT 1'
);
PREPARE sys_user_email_stmt FROM @sys_user_email_sql;
EXECUTE sys_user_email_stmt;
DEALLOCATE PREPARE sys_user_email_stmt;

SET @sys_user_permission_config_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_user'
    AND COLUMN_NAME = 'permission_config'
);
SET @sys_user_permission_config_sql = IF(
  @sys_user_permission_config_exists = 0,
  'ALTER TABLE `sys_user` ADD COLUMN `permission_config` LONGTEXT DEFAULT NULL COMMENT ''Account permission override JSON''',
  'SELECT 1'
);
PREPARE sys_user_permission_config_stmt FROM @sys_user_permission_config_sql;
EXECUTE sys_user_permission_config_stmt;
DEALLOCATE PREPARE sys_user_permission_config_stmt;

CREATE TABLE IF NOT EXISTS `gym_venue` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Venue ID',
  `name` VARCHAR(100) NOT NULL COMMENT 'Venue Name',
  `location` VARCHAR(100) DEFAULT NULL COMMENT 'Location',
  `capacity` INT DEFAULT 0 COMMENT 'Max Capacity',
  `description` TEXT COMMENT 'Description',
  `status` TINYINT DEFAULT 1 COMMENT 'Status: 1-Open, 0-Closed',
  `open_time` TIME DEFAULT NULL COMMENT 'Opening Time',
  `close_time` TIME DEFAULT NULL COMMENT 'Closing Time',
  `image` VARCHAR(255) DEFAULT NULL COMMENT 'Image URL',
  `cover_image` VARCHAR(255) DEFAULT NULL COMMENT 'Cover Image',
  `layout_json` LONGTEXT COMMENT 'Layout JSON for canvas seat map',
  `price_per_hour` DECIMAL(10,2) DEFAULT 0.00 COMMENT 'Venue booking price per hour',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Venue Table';

SET @gym_venue_cover_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'gym_venue'
    AND COLUMN_NAME = 'cover_image'
);
SET @gym_venue_cover_sql = IF(
  @gym_venue_cover_exists = 0,
  'ALTER TABLE `gym_venue` ADD COLUMN `cover_image` VARCHAR(255) DEFAULT NULL COMMENT ''Cover Image''',
  'SELECT 1'
);
PREPARE gym_venue_cover_stmt FROM @gym_venue_cover_sql;
EXECUTE gym_venue_cover_stmt;
DEALLOCATE PREPARE gym_venue_cover_stmt;

SET @gym_venue_layout_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'gym_venue'
    AND COLUMN_NAME = 'layout_json'
);
SET @gym_venue_layout_sql = IF(
  @gym_venue_layout_exists = 0,
  'ALTER TABLE `gym_venue` ADD COLUMN `layout_json` LONGTEXT COMMENT ''Layout JSON for canvas seat map''',
  'SELECT 1'
);
PREPARE gym_venue_layout_stmt FROM @gym_venue_layout_sql;
EXECUTE gym_venue_layout_stmt;
DEALLOCATE PREPARE gym_venue_layout_stmt;

SET @gym_venue_price_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'gym_venue'
    AND COLUMN_NAME = 'price_per_hour'
);
SET @gym_venue_price_sql = IF(
  @gym_venue_price_exists = 0,
  'ALTER TABLE `gym_venue` ADD COLUMN `price_per_hour` DECIMAL(10,2) DEFAULT 0.00 COMMENT ''Venue booking price per hour''',
  'SELECT 1'
);
PREPARE gym_venue_price_stmt FROM @gym_venue_price_sql;
EXECUTE gym_venue_price_stmt;
DEALLOCATE PREPARE gym_venue_price_stmt;

CREATE TABLE IF NOT EXISTS `gym_equipment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Equipment ID',
  `name` VARCHAR(100) NOT NULL COMMENT 'Equipment Name',
  `description` TEXT COMMENT 'Description',
  `status` VARCHAR(20) DEFAULT 'AVAILABLE' COMMENT 'Status: AVAILABLE, IN_USE, MAINTENANCE',
  `venue_id` BIGINT DEFAULT NULL COMMENT 'Located in which venue',
  `quantity` INT DEFAULT 0 COMMENT 'Equipment quantity',
  `image` VARCHAR(255) DEFAULT NULL COMMENT 'Image URL',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Equipment Table';

SET @gym_equipment_quantity_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'gym_equipment'
    AND COLUMN_NAME = 'quantity'
);
SET @gym_equipment_quantity_sql = IF(
  @gym_equipment_quantity_exists = 0,
  'ALTER TABLE `gym_equipment` ADD COLUMN `quantity` INT DEFAULT 0 COMMENT ''Equipment quantity''',
  'SELECT 1'
);
PREPARE gym_equipment_quantity_stmt FROM @gym_equipment_quantity_sql;
EXECUTE gym_equipment_quantity_stmt;
DEALLOCATE PREPARE gym_equipment_quantity_stmt;

CREATE TABLE IF NOT EXISTS `gym_reservation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Reservation ID',
  `user_id` BIGINT NOT NULL COMMENT 'User ID',
  `target_id` BIGINT NOT NULL COMMENT 'Venue/Equipment/Course ID',
  `target_type` VARCHAR(20) NOT NULL COMMENT 'Type: VENUE, EQUIPMENT, COURSE',
  `start_time` DATETIME NOT NULL COMMENT 'Start Time',
  `end_time` DATETIME NOT NULL COMMENT 'End Time',
  `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT 'Status: PENDING, CONFIRMED, CANCELLED, COMPLETED, CHECKED_IN',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Time',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Legacy Reservation Table';

CREATE TABLE IF NOT EXISTS `gym_course` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Course ID',
  `name` VARCHAR(100) NOT NULL COMMENT 'Course Name',
  `coach_id` BIGINT DEFAULT NULL COMMENT 'Coach User ID',
  `venue_id` BIGINT DEFAULT NULL COMMENT 'Venue ID',
  `start_time` DATETIME NOT NULL COMMENT 'Start Time',
  `end_time` DATETIME NOT NULL COMMENT 'End Time',
  `max_participants` INT DEFAULT 20 COMMENT 'Max Participants',
  `current_participants` INT DEFAULT 0 COMMENT 'Current Participants',
  `description` TEXT COMMENT 'Description',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Legacy Course Table';

CREATE TABLE IF NOT EXISTS `gym_repair` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Repair ID',
  `equipment_id` BIGINT DEFAULT NULL COMMENT 'Equipment ID',
  `venue_id` BIGINT DEFAULT NULL COMMENT 'Venue ID',
  `reporter_id` BIGINT DEFAULT NULL COMMENT 'Reported By User ID',
  `description` TEXT COMMENT 'Issue Description',
  `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT 'Status: PENDING, PROCESSING, FIXED',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Report Time',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Repair Table';

CREATE TABLE IF NOT EXISTS `gym_coach` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Coach Profile ID',
  `user_id` BIGINT DEFAULT NULL COMMENT 'Linked user id',
  `name` VARCHAR(50) NOT NULL COMMENT 'Coach name',
  `gender` INT DEFAULT NULL COMMENT 'Gender 1 male 0 female',
  `age` INT DEFAULT NULL COMMENT 'Age',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT 'Phone',
  `specialization` VARCHAR(255) DEFAULT NULL COMMENT 'Specialization',
  `entry_date` DATE DEFAULT NULL COMMENT 'Entry date',
  `intro` TEXT COMMENT 'Coach intro',
  `status` INT DEFAULT 1 COMMENT '1 active 0 inactive',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT 'Avatar',
  `hourly_price` DECIMAL(10,2) DEFAULT 199.00 COMMENT 'Private lesson hourly price',
  `rating` DECIMAL(3,1) DEFAULT 4.8 COMMENT 'Coach rating',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Coach profile table';

SET @gym_coach_user_id_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'gym_coach'
    AND COLUMN_NAME = 'user_id'
);
SET @gym_coach_user_id_sql = IF(
  @gym_coach_user_id_exists = 0,
  'ALTER TABLE `gym_coach` ADD COLUMN `user_id` BIGINT DEFAULT NULL COMMENT ''Linked user id''',
  'SELECT 1'
);
PREPARE gym_coach_user_id_stmt FROM @gym_coach_user_id_sql;
EXECUTE gym_coach_user_id_stmt;
DEALLOCATE PREPARE gym_coach_user_id_stmt;

SET @gym_coach_avatar_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'gym_coach'
    AND COLUMN_NAME = 'avatar'
);
SET @gym_coach_avatar_sql = IF(
  @gym_coach_avatar_exists = 0,
  'ALTER TABLE `gym_coach` ADD COLUMN `avatar` VARCHAR(255) DEFAULT NULL COMMENT ''Avatar''',
  'SELECT 1'
);
PREPARE gym_coach_avatar_stmt FROM @gym_coach_avatar_sql;
EXECUTE gym_coach_avatar_stmt;
DEALLOCATE PREPARE gym_coach_avatar_stmt;

SET @gym_coach_hourly_price_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'gym_coach'
    AND COLUMN_NAME = 'hourly_price'
);
SET @gym_coach_hourly_price_sql = IF(
  @gym_coach_hourly_price_exists = 0,
  'ALTER TABLE `gym_coach` ADD COLUMN `hourly_price` DECIMAL(10,2) DEFAULT 199.00 COMMENT ''Private lesson hourly price''',
  'SELECT 1'
);
PREPARE gym_coach_hourly_price_stmt FROM @gym_coach_hourly_price_sql;
EXECUTE gym_coach_hourly_price_stmt;
DEALLOCATE PREPARE gym_coach_hourly_price_stmt;

SET @gym_coach_rating_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'gym_coach'
    AND COLUMN_NAME = 'rating'
);
SET @gym_coach_rating_sql = IF(
  @gym_coach_rating_exists = 0,
  'ALTER TABLE `gym_coach` ADD COLUMN `rating` DECIMAL(3,1) DEFAULT 4.8 COMMENT ''Coach rating''',
  'SELECT 1'
);
PREPARE gym_coach_rating_stmt FROM @gym_coach_rating_sql;
EXECUTE gym_coach_rating_stmt;
DEALLOCATE PREPARE gym_coach_rating_stmt;

CREATE TABLE IF NOT EXISTS `gym_membership_package` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL,
  `price` DECIMAL(10,2) NOT NULL,
  `days` INT NOT NULL,
  `daily_limit` INT DEFAULT 1,
  `description` VARCHAR(255) DEFAULT NULL,
  `status` VARCHAR(20) DEFAULT 'ACTIVE',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Membership packages';

CREATE TABLE IF NOT EXISTS `gym_member_membership` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `package_id` BIGINT NOT NULL,
  `membership_name` VARCHAR(100) NOT NULL,
  `start_date` DATE NOT NULL,
  `end_date` DATE NOT NULL,
  `status` VARCHAR(20) DEFAULT 'ACTIVE',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Purchased memberships';

CREATE TABLE IF NOT EXISTS `gym_private_package` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `coach_id` BIGINT DEFAULT NULL,
  `name` VARCHAR(100) NOT NULL,
  `price` DECIMAL(10,2) NOT NULL,
  `total_sessions` INT NOT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `status` VARCHAR(20) DEFAULT 'ACTIVE',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Private lesson packages';

CREATE TABLE IF NOT EXISTS `gym_member_private_package` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `coach_id` BIGINT DEFAULT NULL,
  `package_id` BIGINT NOT NULL,
  `package_name` VARCHAR(100) NOT NULL,
  `total_sessions` INT NOT NULL,
  `remaining_sessions` INT NOT NULL,
  `status` VARCHAR(20) DEFAULT 'ACTIVE',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Purchased private lesson packages';

CREATE TABLE IF NOT EXISTS `gym_course_schedule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL,
  `coach_id` BIGINT DEFAULT NULL,
  `venue_id` BIGINT DEFAULT NULL,
  `start_time` DATETIME NOT NULL,
  `end_time` DATETIME NOT NULL,
  `capacity` INT DEFAULT 20,
  `booked_count` INT DEFAULT 0,
  `normal_price` DECIMAL(10,2) DEFAULT 0.00,
  `flash_sale` TINYINT DEFAULT 0,
  `flash_sale_price` DECIMAL(10,2) DEFAULT 0.00,
  `description` TEXT,
  `status` VARCHAR(20) DEFAULT 'PUBLISHED',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Group course schedule';

CREATE TABLE IF NOT EXISTS `gym_private_schedule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `coach_id` BIGINT NOT NULL,
  `venue_id` BIGINT DEFAULT NULL,
  `start_time` DATETIME NOT NULL,
  `end_time` DATETIME NOT NULL,
  `capacity` INT DEFAULT 1,
  `booked_count` INT DEFAULT 0,
  `description` VARCHAR(255) DEFAULT NULL,
  `status` VARCHAR(20) DEFAULT 'OPEN',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Private lesson schedules';

CREATE TABLE IF NOT EXISTS `gym_resource_slot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `resource_type` VARCHAR(30) NOT NULL,
  `resource_id` BIGINT NOT NULL,
  `venue_id` BIGINT DEFAULT NULL,
  `coach_id` BIGINT DEFAULT NULL,
  `slot_date` DATE NOT NULL,
  `start_time` DATETIME NOT NULL,
  `end_time` DATETIME NOT NULL,
  `capacity` INT DEFAULT 1,
  `occupied_count` INT DEFAULT 0,
  `version` INT DEFAULT 0,
  `status` VARCHAR(20) DEFAULT 'OPEN',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Unified reservable slots';

CREATE TABLE IF NOT EXISTS `gym_booking_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(40) NOT NULL,
  `user_id` BIGINT NOT NULL,
  `resource_type` VARCHAR(30) NOT NULL,
  `resource_id` BIGINT NOT NULL,
  `resource_name` VARCHAR(100) NOT NULL,
  `venue_id` BIGINT DEFAULT NULL,
  `coach_id` BIGINT DEFAULT NULL,
  `booking_date` DATE NOT NULL,
  `start_time` DATETIME NOT NULL,
  `end_time` DATETIME NOT NULL,
  `amount` DECIMAL(10,2) DEFAULT 0.00,
  `payment_status` VARCHAR(20) DEFAULT 'UNPAID',
  `status` VARCHAR(20) DEFAULT 'CREATED',
  `source` VARCHAR(20) DEFAULT 'WEB',
  `idempotent_key` VARCHAR(64) DEFAULT NULL,
  `qr_token` VARCHAR(80) DEFAULT NULL,
  `qr_expire_time` DATETIME DEFAULT NULL,
  `checked_in_at` DATETIME DEFAULT NULL,
  `remark` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_booking_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Unified booking orders';

CREATE TABLE IF NOT EXISTS `gym_booking_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `booking_order_id` BIGINT NOT NULL,
  `slot_id` BIGINT NOT NULL,
  `package_id` BIGINT DEFAULT NULL,
  `member_package_id` BIGINT DEFAULT NULL,
  `quantity` INT DEFAULT 1,
  `amount` DECIMAL(10,2) DEFAULT 0.00,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Booking line items';

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

CREATE TABLE IF NOT EXISTS `gym_payment_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `payment_no` VARCHAR(40) NOT NULL,
  `user_id` BIGINT NOT NULL,
  `payment_type` VARCHAR(30) NOT NULL,
  `target_type` VARCHAR(30) NOT NULL,
  `target_id` BIGINT DEFAULT NULL,
  `amount` DECIMAL(10,2) NOT NULL,
  `status` VARCHAR(20) DEFAULT 'UNPAID',
  `payload_json` LONGTEXT,
  `paid_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_no` (`payment_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Payment simulation orders';

CREATE TABLE IF NOT EXISTS `gym_checkin` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `booking_order_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `checkin_code` VARCHAR(80) NOT NULL,
  `checkin_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `status` VARCHAR(20) DEFAULT 'SUCCESS',
  `operator_name` VARCHAR(50) DEFAULT 'SELF',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Checkin records';

CREATE TABLE IF NOT EXISTS `gym_body_metric` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `coach_id` BIGINT NOT NULL,
  `height` DECIMAL(5,2) DEFAULT 0.00,
  `weight` DECIMAL(5,2) DEFAULT 0.00,
  `body_fat` DECIMAL(5,2) DEFAULT 0.00,
  `bmi` DECIMAL(5,2) DEFAULT 0.00,
  `remark` VARCHAR(255) DEFAULT NULL,
  `measured_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Body metrics';

CREATE TABLE IF NOT EXISTS `gym_training_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `coach_id` BIGINT NOT NULL,
  `booking_order_id` BIGINT DEFAULT NULL,
  `train_date` DATE NOT NULL,
  `focus_area` VARCHAR(100) DEFAULT NULL,
  `content` TEXT,
  `remark` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Training logs';

CREATE TABLE IF NOT EXISTS `gym_schedule_conflict` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `resource_type` VARCHAR(30) NOT NULL,
  `resource_id` BIGINT DEFAULT NULL,
  `conflict_type` VARCHAR(30) NOT NULL,
  `reference_id` BIGINT DEFAULT NULL,
  `message` VARCHAR(255) NOT NULL,
  `start_time` DATETIME NOT NULL,
  `end_time` DATETIME NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Schedule conflict logs';

CREATE TABLE IF NOT EXISTS `gym_operate_snapshot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `snapshot_date` DATE NOT NULL,
  `revenue` DECIMAL(10,2) DEFAULT 0.00,
  `booking_count` INT DEFAULT 0,
  `checkin_count` INT DEFAULT 0,
  `venue_usage_rate` DECIMAL(5,2) DEFAULT 0.00,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Daily operation snapshots';

INSERT INTO `sys_user` (`username`, `password`, `real_name`, `nickname`, `email`, `role`, `phone`, `balance`, `type`, `status`)
SELECT 'admin', 'admin', '系统管理员', '管理员', 'admin@gvs.local', 'ADMIN', '13800000000', 9999.00, 'VIP', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `username` = 'admin');

INSERT INTO `sys_user` (`username`, `password`, `real_name`, `nickname`, `email`, `role`, `phone`, `balance`, `type`, `status`)
SELECT 'member01', '123456', '体验会员', '体验会员', 'member01@gvs.local', 'MEMBER', '13800000001', 500.00, 'REGULAR', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `username` = 'member01');

INSERT INTO `sys_user` (`username`, `password`, `real_name`, `nickname`, `email`, `role`, `phone`, `balance`, `type`, `status`)
SELECT 'coach01', '123456', '李教练', '李教练', 'coach01@gvs.local', 'COACH', '13800000002', 0.00, 'COACH', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `username` = 'coach01');

INSERT INTO `gym_venue` (`name`, `location`, `capacity`, `description`, `status`, `open_time`, `close_time`, `price_per_hour`, `layout_json`)
SELECT '力量训练区', '一楼A区', 30, '自由力量与综合训练区', 1, '06:00:00', '22:00:00', 39.90,
       '[{"id":"A1","x":28,"y":24,"status":"OPEN"},{"id":"A2","x":128,"y":24,"status":"OPEN"},{"id":"A3","x":228,"y":24,"status":"OPEN"}]'
WHERE NOT EXISTS (SELECT 1 FROM `gym_venue` WHERE `name` = '力量训练区');

INSERT INTO `gym_venue` (`name`, `location`, `capacity`, `description`, `status`, `open_time`, `close_time`, `price_per_hour`, `layout_json`)
SELECT '有氧训练区', '二楼B区', 24, '跑步机与椭圆机区域', 1, '06:00:00', '22:00:00', 29.90,
       '[{"id":"B1","x":24,"y":20,"status":"OPEN"},{"id":"B2","x":124,"y":20,"status":"OPEN"},{"id":"B3","x":224,"y":20,"status":"OPEN"}]'
WHERE NOT EXISTS (SELECT 1 FROM `gym_venue` WHERE `name` = '有氧训练区');

INSERT INTO `gym_coach` (`user_id`, `name`, `gender`, `age`, `phone`, `specialization`, `entry_date`, `intro`, `status`, `hourly_price`, `rating`)
SELECT
  (SELECT id FROM `sys_user` WHERE `username` = 'coach01' LIMIT 1),
  '李教练', 1, 29, '13800000002', '减脂塑形, 力量提升', CURDATE(), '擅长减脂塑形与基础力量计划', 1, 299.00, 4.9
WHERE NOT EXISTS (SELECT 1 FROM `gym_coach` WHERE `name` = '李教练');

INSERT INTO `gym_membership_package` (`name`, `price`, `days`, `daily_limit`, `description`, `status`)
SELECT '月卡', 199.00, 30, 1, '适合日常入门健身会员', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM `gym_membership_package` WHERE `name` = '月卡');

INSERT INTO `gym_membership_package` (`name`, `price`, `days`, `daily_limit`, `description`, `status`)
SELECT '季卡', 499.00, 90, 1, '适合稳定训练会员', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM `gym_membership_package` WHERE `name` = '季卡');

INSERT INTO `gym_private_package` (`coach_id`, `name`, `price`, `total_sessions`, `description`, `status`)
SELECT (SELECT id FROM `gym_coach` WHERE `name` = '李教练' LIMIT 1), '李教练私教5节', 999.00, 5, '适合减脂塑形初期跟练', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM `gym_private_package` WHERE `name` = '李教练私教5节');

INSERT INTO `gym_course_schedule` (`name`, `coach_id`, `venue_id`, `start_time`, `end_time`, `capacity`, `booked_count`, `normal_price`, `flash_sale`, `flash_sale_price`, `description`, `status`)
SELECT '燃脂搏击团课',
       (SELECT id FROM `gym_coach` WHERE `name` = '李教练' LIMIT 1),
       (SELECT id FROM `gym_venue` WHERE `name` = '力量训练区' LIMIT 1),
       DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY) + INTERVAL 19 HOUR,
       DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY) + INTERVAL 20 HOUR,
       18, 0, 69.00, 1, 29.90, '晚间热门燃脂课，支持秒杀', 'PUBLISHED'
WHERE NOT EXISTS (
  SELECT 1 FROM `gym_course_schedule`
  WHERE `name` = '燃脂搏击团课' AND DATE(`start_time`) = DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY)
);

INSERT INTO `gym_private_schedule` (`coach_id`, `venue_id`, `start_time`, `end_time`, `capacity`, `booked_count`, `description`, `status`)
SELECT
  (SELECT id FROM `gym_coach` WHERE `name` = '李教练' LIMIT 1),
  (SELECT id FROM `gym_venue` WHERE `name` = '力量训练区' LIMIT 1),
  DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY) + INTERVAL 16 HOUR,
  DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY) + INTERVAL 17 HOUR,
  1, 0, '李教练一对一体验课时段', 'OPEN'
WHERE NOT EXISTS (
  SELECT 1 FROM `gym_private_schedule`
  WHERE DATE(`start_time`) = DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY)
);

INSERT INTO `gym_member_membership` (`user_id`, `package_id`, `membership_name`, `start_date`, `end_date`, `status`)
SELECT
  (SELECT id FROM `sys_user` WHERE `username` = 'member01' LIMIT 1),
  (SELECT id FROM `gym_membership_package` WHERE `name` = '月卡' LIMIT 1),
  '月卡',
  CURRENT_DATE,
  DATE_ADD(CURRENT_DATE, INTERVAL 30 DAY),
  'ACTIVE'
WHERE NOT EXISTS (
  SELECT 1 FROM `gym_member_membership`
  WHERE `user_id` = (SELECT id FROM `sys_user` WHERE `username` = 'member01' LIMIT 1)
);

INSERT INTO `gym_member_private_package` (`user_id`, `coach_id`, `package_id`, `package_name`, `total_sessions`, `remaining_sessions`, `status`)
SELECT
  (SELECT id FROM `sys_user` WHERE `username` = 'member01' LIMIT 1),
  (SELECT id FROM `gym_coach` WHERE `name` = '李教练' LIMIT 1),
  (SELECT id FROM `gym_private_package` WHERE `name` = '李教练私教5节' LIMIT 1),
  '李教练私教5节',
  5,
  3,
  'ACTIVE'
WHERE NOT EXISTS (
  SELECT 1 FROM `gym_member_private_package`
  WHERE `user_id` = (SELECT id FROM `sys_user` WHERE `username` = 'member01' LIMIT 1)
);
