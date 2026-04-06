-- Create Database
CREATE DATABASE IF NOT EXISTS gym_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE gym_db;

-- 1. User Table (Members, Staff, Admin)
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'User ID',
  `username` VARCHAR(50) NOT NULL COMMENT 'Username',
  `password` VARCHAR(100) NOT NULL COMMENT 'Password',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT 'Real Name',
  `role` VARCHAR(20) NOT NULL COMMENT 'Role: ADMIN, STAFF, MEMBER',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT 'Phone Number',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT 'Avatar URL',
  `balance` DECIMAL(10,2) DEFAULT 0.00 COMMENT 'Account Balance/Points',
  `type` VARCHAR(20) DEFAULT 'MEMBER' COMMENT 'Member Type: REGULAR, VIP',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User Table';

-- 2. Venue Table
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
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Venue Table';

-- 3. Equipment Table
CREATE TABLE IF NOT EXISTS `gym_equipment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Equipment ID',
  `name` VARCHAR(100) NOT NULL COMMENT 'Equipment Name',
  `description` TEXT COMMENT 'Description',
  `status` VARCHAR(20) DEFAULT 'AVAILABLE' COMMENT 'Status: AVAILABLE, IN_USE, MAINTENANCE',
  `venue_id` BIGINT DEFAULT NULL COMMENT 'Located in which venue',
  `image` VARCHAR(255) DEFAULT NULL COMMENT 'Image URL',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Equipment Table';

-- 4. Reservation Table
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Reservation Table';

-- 5. Course Table
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Course Table';

-- 6. Repair/Maintenance Table
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

-- Initial Data
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `role`)
VALUES ('admin', 'admin', 'System Admin', 'ADMIN')
ON DUPLICATE KEY UPDATE `id` = `id`;
