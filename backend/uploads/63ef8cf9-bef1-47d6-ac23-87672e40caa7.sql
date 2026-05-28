-- MySQL dump 10.13  Distrib 9.6.0, for macos15.7 (arm64)
--
-- Host: localhost    Database: tech_manage
-- ------------------------------------------------------
-- Server version	9.6.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
SET @MYSQLDUMP_TEMP_LOG_BIN = @@SESSION.SQL_LOG_BIN;
SET @@SESSION.SQL_LOG_BIN= 0;

--
-- GTID state at the beginning of the backup 
--

SET @@GLOBAL.GTID_PURGED=/*!80000 '+'*/ 'a442bf1a-5128-11f1-bcdf-b6f19722e30c:1-143';

--
-- Table structure for table `checklists`
--

DROP TABLE IF EXISTS `checklists`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `checklists` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `actual_date` date DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `planned_date` date DEFAULT NULL,
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `responsible_person` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `source` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint NOT NULL,
  `source_detail` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `checklists`
--

LOCK TABLES `checklists` WRITE;
/*!40000 ALTER TABLE `checklists` DISABLE KEYS */;
INSERT INTO `checklists` VALUES (1,'2026-05-20 22:46:33.996540','2026-05-20 22:46:33.996570',NULL,NULL,'客服就额头','2026-05-19','','杨小院','领导交办','待办',1,NULL);
/*!40000 ALTER TABLE `checklists` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `departments`
--

DROP TABLE IF EXISTS `departments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `departments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `enabled` bit(1) NOT NULL,
  `leader` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `departments`
--

LOCK TABLES `departments` WRITE;
/*!40000 ALTER TABLE `departments` DISABLE KEYS */;
INSERT INTO `departments` VALUES (1,_binary '','张三','信息科技部'),(2,_binary '','张明贵','党委办公室（纪委办公室）'),(3,_binary '','邱悦','人力资源部'),(4,_binary '','张开第','计划财务部'),(5,_binary '','刘晓勇','资金营运部'),(6,_binary '','赵玉龙','风险管理部'),(7,_binary '','赵玉龙','授信审批部'),(8,_binary '','庞卢璐','资产管理部'),(9,_binary '','夏彦明','资产保全部'),(10,_binary '','黄巍','法律合规部'),(11,_binary '','杨现菊','审计部'),(12,_binary '','张明贵','办公室（董事会办公室）'),(13,_binary '','李吾一','营销管理部'),(14,_binary '','王浩','生态协同部'),(15,_binary '','张盛','产业金融业务部'),(16,_binary '','李飞','航运业务部'),(17,_binary '','马自润','绿色金融业务部'),(18,_binary '','李吾一','普惠金融业务部'),(19,_binary '','张铭','京津冀业务中心'),(20,_binary '','李忻哲','长三角业务中心'),(21,_binary '','杨帆','大湾区业务中心'),(22,_binary '','丁力','西南区域业务中心'),(23,_binary '','龙誉','中部区域业务中心');
/*!40000 ALTER TABLE `departments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `issue_categories`
--

DROP TABLE IF EXISTS `issue_categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `issue_categories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `enabled` bit(1) NOT NULL,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sort_order` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `issue_categories`
--

LOCK TABLES `issue_categories` WRITE;
/*!40000 ALTER TABLE `issue_categories` DISABLE KEYS */;
INSERT INTO `issue_categories` VALUES (1,_binary '','系统缺陷',0),(2,_binary '','功能优化',1),(3,_binary '','网络安全',2),(4,_binary '','易用性问题',3),(5,_binary '','数据问题',4),(6,_binary '','其他',5),(7,_binary '','硬件',0),(8,_binary '','软件',1),(9,_binary '','网络',2),(10,_binary '','安全',3),(11,_binary '','数据',4);
/*!40000 ALTER TABLE `issue_categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `issue_feedbacks`
--

DROP TABLE IF EXISTS `issue_feedbacks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `issue_feedbacks` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `description` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `submitter_id` bigint NOT NULL,
  `title` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL,
  `issue_code` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `issue_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `meeting_date` date DEFAULT NULL,
  `meeting_department` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `occasion_id` bigint DEFAULT NULL,
  `permanent_deadline` date DEFAULT NULL,
  `permanent_solution` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `responsible_person_id` bigint DEFAULT NULL,
  `responsible_team` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `root_cause` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `submitter_department` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `temporary_deadline` date DEFAULT NULL,
  `temporary_solution` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKf4y0g6abp3u1l0xwbp3nej89j` (`issue_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `issue_feedbacks`
--

LOCK TABLES `issue_feedbacks` WRITE;
/*!40000 ALTER TABLE `issue_feedbacks` DISABLE KEYS */;
INSERT INTO `issue_feedbacks` VALUES (1,'2026-05-20 23:40:09.648660','2026-05-22 18:52:42.595081','办公室大门无法打开','已关闭',1,'办公室大门无法打开','',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'',NULL,NULL),(2,'2026-05-22 17:43:30.114086','2026-05-22 18:55:17.709087','这是测试','已关闭',1,'测试问题','ISS-20260522-001','软件',NULL,NULL,NULL,NULL,NULL,2,'业务开发团队-开发二组',NULL,'信息科技部',NULL,NULL),(3,'2026-05-22 18:24:15.429062','2026-05-22 18:55:13.113826','网络太慢网络太慢网络太慢网络太慢网络太慢网络太慢网络太慢网络太慢网络太慢网络太慢网络太慢网络太慢网络太慢网络太慢网络太慢网络太慢网络太慢网络太慢网络太慢网络太慢网络太慢网络太慢网络太慢网络太慢网络太慢网络太慢网络太慢网络太慢网络太慢','已关闭',2,'网络太慢','ISS-20260522-002','',NULL,'',3,NULL,'暂时不知道',2,'业务开发团队-开发二组','钱不够','信息科技部','2026-05-27','买宽带');
/*!40000 ALTER TABLE `issue_feedbacks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `issue_logs`
--

DROP TABLE IF EXISTS `issue_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `issue_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `action` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `issue_id` bigint NOT NULL,
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `issue_logs`
--

LOCK TABLES `issue_logs` WRITE;
/*!40000 ALTER TABLE `issue_logs` DISABLE KEYS */;
INSERT INTO `issue_logs` VALUES (1,'提交问题','2026-05-20 23:40:09.664381',1,NULL,1),(2,'分派责任人','2026-05-20 23:40:18.976078',1,'指派给杨小院',1),(3,'提交问题','2026-05-22 17:43:30.128190',2,NULL,1),(4,'分配团队和责任人','2026-05-22 18:17:03.830598',2,'团队: 业务开发团队-开发二组, 责任人: 杨小院',1),(5,'提交问题','2026-05-22 18:24:15.435376',3,NULL,2),(6,'分配团队和责任人','2026-05-22 18:24:31.338926',3,'团队: 业务开发团队-开发二组, 责任人: 杨小院',2),(7,'提交整改方案','2026-05-22 18:25:08.782308',3,'临时时限: 2026-05-27',2),(8,'提交完成','2026-05-22 18:25:15.209377',3,'等待提出人确认',2),(9,'确认完成','2026-05-22 18:25:18.373209',3,'问题已关闭',2),(10,'关闭问题','2026-05-22 18:52:42.601760',1,'关闭原因: test',1),(11,'关闭问题','2026-05-22 18:55:13.137502',3,'关闭原因: 垃圾',1),(12,'关闭问题','2026-05-22 18:55:17.712104',2,'关闭原因: 测试',1);
/*!40000 ALTER TABLE `issue_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `issue_occasions`
--

DROP TABLE IF EXISTS `issue_occasions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `issue_occasions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `enabled` bit(1) NOT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `issue_occasions`
--

LOCK TABLES `issue_occasions` WRITE;
/*!40000 ALTER TABLE `issue_occasions` DISABLE KEYS */;
INSERT INTO `issue_occasions` VALUES (1,_binary '','20260515吐槽大会-信息科技部','MEETING'),(2,_binary '','业务协调会','MEETING'),(3,_binary '','线上提出','GENERAL'),(4,_binary '','其他','GENERAL');
/*!40000 ALTER TABLE `issue_occasions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rewards_punishments`
--

DROP TABLE IF EXISTS `rewards_punishments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rewards_punishments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `attachment_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `creator_id` bigint NOT NULL,
  `decision_date` date NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `department` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `document_no` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `involved_person` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rewards_punishments`
--

LOCK TABLES `rewards_punishments` WRITE;
/*!40000 ALTER TABLE `rewards_punishments` DISABLE KEYS */;
/*!40000 ALTER TABLE `rewards_punishments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKch1113horj4qr56f91omojv8` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `roles`
--

LOCK TABLES `roles` WRITE;
/*!40000 ALTER TABLE `roles` DISABLE KEYS */;
INSERT INTO `roles` VALUES (1,'ROLE_USER','默认角色，使用清单管理、查询奖惩、提交问题','普通用户'),(2,'ROLE_CLERK','管理部门奖惩记录','部门文书'),(3,'ROLE_ISSUE_ADMIN','问题分派、分类管理','问题管理员'),(4,'ROLE_ADMIN','用户管理、角色分配、系统配置','系统管理员');
/*!40000 ALTER TABLE `roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `system_info`
--

DROP TABLE IF EXISTS `system_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_info` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `enabled` bit(1) NOT NULL,
  `leader` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `team` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `system_info`
--

LOCK TABLES `system_info` WRITE;
/*!40000 ALTER TABLE `system_info` DISABLE KEYS */;
INSERT INTO `system_info` VALUES (1,_binary '','赵六','ERP系统','研发一组');
/*!40000 ALTER TABLE `system_info` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `teams`
--

DROP TABLE IF EXISTS `teams`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teams` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `enabled` bit(1) NOT NULL,
  `leader` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `members` text COLLATE utf8mb4_unicode_ci,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `department` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `teams`
--

LOCK TABLES `teams` WRITE;
/*!40000 ALTER TABLE `teams` DISABLE KEYS */;
INSERT INTO `teams` VALUES (1,_binary '','杨小院','王梓涵,李  超,贾  伟','科技管理组','信息科技部'),(2,_binary '','曹国威','胡伯涵,朱宏坤,曹国威','安全运维团队','信息科技部'),(3,_binary '','袁启勋','毕淑卉,罗  京,汤银辉,周明润,袁启勋','业务开发团队-开发一组','信息科技部'),(4,_binary '','杨小院','张  真,琚  寰,王艺霖,杨小院','业务开发团队-开发二组','信息科技部'),(5,_binary '','山  伟','谷  静,郭红军,王重阳,樊泽璞,山  伟','业务开发团队-开发三组','信息科技部'),(6,_binary '','付  蓉','沈  亮,邓  鹏,宋超林,曲  宁,陈天龙,王峰军,史小龙,刘文龙','管理开发团队','信息科技部');
/*!40000 ALTER TABLE `teams` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_roles`
--

DROP TABLE IF EXISTS `user_roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_roles` (
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `FKh8ciramu9cc9q3qcqiv4ue8a6` (`role_id`),
  CONSTRAINT `FKh8ciramu9cc9q3qcqiv4ue8a6` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`),
  CONSTRAINT `FKhfh9dx7w3ubf1co1vdev94g3f` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_roles`
--

LOCK TABLES `user_roles` WRITE;
/*!40000 ALTER TABLE `user_roles` DISABLE KEYS */;
INSERT INTO `user_roles` VALUES (2,1),(3,1),(1001,1),(2,2),(3,2),(2,3),(3,3),(1,4),(2,4);
/*!40000 ALTER TABLE `user_roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `department` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `enabled` bit(1) NOT NULL,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `position` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKr43af9ap4edm43mmtq01oddj6` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=1295 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'2026-05-20 22:34:29.508680','2026-05-20 22:34:29.508696','信息科技部',_binary '','系统管理员','$2a$10$OWflRSKvrMwS2QDECp7icOkXTRtCGOmrL0I2I4axyF/xXsSzPNEiK',NULL,NULL,'admin'),(2,'2026-05-20 22:59:42.176865','2026-05-20 23:06:34.922441','信息科技部',_binary '','杨小院','$2a$10$tR9J0yH/cpFDYuEYHcLvFO7L3W74xCEFgO3gdXNLcwYDb47HWW04C','15910707497','架构管理岗','zhyangxy'),(3,'2026-05-20 23:00:57.697531','2026-05-20 23:00:57.697542','信息科技部',_binary '','王梓涵','$2a$10$/U5/XPulCu2I3XnbgK7.luBTaZ.5l8W79DDhmxr7VhlQGpa4/NZ7i','18156099535','综合管理岗','zlwzh'),(1001,'2025-05-21 10:00:01.000000','2025-05-21 10:00:01.000000','办公室（董事会办公室）',_binary '','张明贵','$2b$12$Qq9fUMwPx24P.YDbq1Quy.WOzlFL4hI3GZTWlmFiicwCzpxPUe2/u','13241886575','部门负责人','13241886575'),(1002,'2025-05-21 10:00:02.000000','2025-05-21 10:00:02.000000','办公室（董事会办公室）',_binary '','李雪松','$2b$12$tTUnC5gCcExryOW7GBmsROXoRKc/BplNPdDoGnTndv4Yoxaap9Uve','13911862476','主任助理','13911862476'),(1003,'2025-05-21 10:00:03.000000','2025-05-21 10:00:03.000000','办公室（董事会办公室）',_binary '','李  毅','$2b$12$E9V/JDLZynPLsg5n7goKZ.uKeIidW/7U5W25S2q2ZehaHCvrCsaT2','13401038288','资深经理','13401038288'),(1004,'2025-05-21 10:00:04.000000','2025-05-21 10:00:04.000000','办公室（董事会办公室）',_binary '','陈述文','$2b$12$WtBno1BnDBKsCuaJtYwgNeUpUILOPUR5k6vCvuAoqHZ/XogXpunKu','18604717332','资深经理','18604717332'),(1005,'2025-05-21 10:00:05.000000','2025-05-21 10:00:05.000000','办公室（董事会办公室）',_binary '','卢  旭','$2b$12$dwaGVjqRulzXjsIrr0pwVuN9QsCoKiLZZMKfwEp9.lVmuL/yWAs02','13910521009','高级经理','13910521009'),(1006,'2025-05-21 10:00:06.000000','2025-05-21 10:00:06.000000','办公室（董事会办公室）',_binary '','马连英','$2b$12$Yn0qQEiwE6eJRL2fiQLOee7jLegnZ7LcoipqdnlkU19vZDWuUb312','13520744009','高级经理','13520744009'),(1007,'2025-05-21 10:00:07.000000','2025-05-21 10:00:07.000000','办公室（董事会办公室）',_binary '','袁庆华','$2b$12$z.00XRlz/uaisa9hoh3c5OVr0hasc583rFt9s49MU11E9H8r7JOj6','13717793240','高级经理','13717793240'),(1008,'2025-05-21 10:00:08.000000','2025-05-21 10:00:08.000000','办公室（董事会办公室）',_binary '','袁立佳','$2b$12$56L3hhOayy1.BY13WDF5M.uaRIoHL8darhxUCglir1k1Z1fLUrhr6','15822056785','高级经理','15822056785'),(1009,'2025-05-21 10:00:09.000000','2025-05-21 10:00:09.000000','办公室（董事会办公室）',_binary '','李雨薇','$2b$12$J91NcK1q.xZuIXZUVkHuauaKKdfRKU6amfNgweDMxf9/jsd/1UPra','18292716098','助理','18292716098'),(1010,'2025-05-21 10:00:10.000000','2025-05-21 10:00:10.000000','办公室（董事会办公室）',_binary '','曹秋月','$2b$12$clgN8UPFTcpts5rT2qbB3.dnsJvvJluNc/67oPgRdboQ5rEMO2ZLq','13161681558','助理','13161681558'),(1012,'2025-05-21 10:00:12.000000','2025-05-21 10:00:12.000000','党委办公室（纪委办公室）',_binary '','马  亮','$2b$12$Ur3cQcEbdK.qXytIxMA47.pSR.h5DVrTo0DUom1EMQ9mk5gpzjRsO','18911933639','主任助理','18911933639'),(1013,'2025-05-21 10:00:13.000000','2025-05-21 10:00:13.000000','党委办公室（纪委办公室）',_binary '','姜  静','$2b$12$5zOuFBU2k7fB5zbFYa5ox.US5Y652lxhl6Bluyg021/VhVutKlGQO','13810823569','高级经理','13810823569'),(1014,'2025-05-21 10:00:14.000000','2025-05-21 10:00:14.000000','党委办公室（纪委办公室）',_binary '','赵晟皓','$2b$12$9/VqQGPPlprfYsILwIPqGOIyQ77/rVdgCy/q4jtKm5LPsAi/gdFpC','18601174198','经理','18601174198'),(1015,'2025-05-21 10:00:15.000000','2025-05-21 10:00:15.000000','党委办公室（纪委办公室）',_binary '','万小玉','$2b$12$A90jLs1vMwP2n7T13FRIOOXdcQFYRv9gzX59GaF.abR67wdK6n3Yy','17888808779','经理','17888808779'),(1016,'2025-05-21 10:00:16.000000','2025-05-21 10:00:16.000000','人力资源部',_binary '','邱悦','$2b$12$UpMKLkDln2rPS0stYOn0c.4UzA5dbuni0OJICVMCDuJDD53MAW822','13910067106','部门负责人','13910067106'),(1017,'2025-05-21 10:00:17.000000','2025-05-21 10:00:17.000000','人力资源部',_binary '','薛京晶','$2b$12$aU4AIamAF1MAcBoH61CQI.bpqGBTD7M48WKNMw3Gk.8KiC7mrL6QC','13520375913','高级资深经理','13520375913'),(1018,'2025-05-21 10:00:18.000000','2025-05-21 10:00:18.000000','人力资源部',_binary '','毕祎洋','$2b$12$N8bZnd7J8kwYU6Guyt7v/eCWoIV9vevghON284aTCQtTyEo5LLZMq','13910763579','资深经理','13910763579'),(1019,'2025-05-21 10:00:19.000000','2025-05-21 10:00:19.000000','人力资源部',_binary '','杨春娜','$2b$12$yRC1VaiLrYPvEGMBQYgeG.39PKosfO/n4tfwVOeKPpUQ0aHs53lJW','18611640523','高级经理','18611640523'),(1020,'2025-05-21 10:00:20.000000','2025-05-21 10:00:20.000000','人力资源部',_binary '','马晴子','$2b$12$kjTAHJCJNJJEGBClSIM5tuk1PIXkP8D/ROBPaMfI4ESbqOQdlXs0q','13581789085','高级经理','13581789085'),(1021,'2025-05-21 10:00:21.000000','2025-05-21 10:00:21.000000','人力资源部',_binary '','岳龙飞','$2b$12$6/iZOWBxy4pc1.j/1aeiG.N9STkwQ.svSCOzA0KThD6EyUQgiBhDu','15210345017','经理','15210345017'),(1022,'2025-05-21 10:00:22.000000','2025-05-21 10:00:22.000000','计划财务部',_binary '','张开第','$2b$12$K2Uf8/xWibIQ218ZXi3ga.9HemXCwsLx6ZeLqZPHh7lKfENWEWcOS','18611756651','部门负责人','18611756651'),(1023,'2025-05-21 10:00:23.000000','2025-05-21 10:00:23.000000','计划财务部',_binary '','孟庆龙','$2b$12$x9JHPBwqmer.DiTg.37cweCzbpPVFK/KQ3QHH.dwclbij5RJCEQgC','15811360122','高级资深经理','15811360122'),(1024,'2025-05-21 10:00:24.000000','2025-05-21 10:00:24.000000','计划财务部',_binary '','林  洁','$2b$12$Ht8BzPP5f/mCtGTSNyamqexLy7xXQhR9Vwwz/MqKofP52hnZHKLvi','13520693021','资深经理','13520693021'),(1025,'2025-05-21 10:00:25.000000','2025-05-21 10:00:25.000000','计划财务部',_binary '','孙  倩','$2b$12$Scn5QtPCGk7umDFx.skIqOeYJ1AKQE0Oxozc86Rx4I9NVYLPMM25m','13161711628','资深经理','13161711628'),(1026,'2025-05-21 10:00:26.000000','2025-05-21 10:00:26.000000','计划财务部',_binary '','周艾琳','$2b$12$W6BpJWPZBci9N1WtK3bY/ep/acPLEy3ZyD.BSvGSBE4h5olrMO76C','18810661707','高级经理','18810661707'),(1027,'2025-05-21 10:00:27.000000','2025-05-21 10:00:27.000000','计划财务部',_binary '','崔睿萌','$2b$12$7sywXkY7S.ebM6J4oraN4uj86XGm5I7cfmzs62kCoHY4TI5YFWlJK','18311153756','高级经理','18311153756'),(1028,'2025-05-21 10:00:28.000000','2025-05-21 10:00:28.000000','计划财务部',_binary '','戴  为','$2b$12$E4NSv0/xyr.DlyCivkZ0Fu2SsERa/0fAlnobPBhwhprgjmqinqhyO','13911652121','高级经理','13911652121'),(1029,'2025-05-21 10:00:29.000000','2025-05-21 10:00:29.000000','计划财务部',_binary '','刘晨雨','$2b$12$BWRjmwT.9QDB4g5fa0MmsuRhZB5AlSsFFKg2jqvaySesF/6e3MqYK','17810200989','经理','17810200989'),(1030,'2025-05-21 10:00:30.000000','2025-05-21 10:00:30.000000','计划财务部',_binary '','税嘉晨','$2b$12$cPkGXpaUiRbiZECe1Th7GO2ASqp/MgkQs444WhmUn7CZoJB6fS92i','18611209681','经理','18611209681'),(1031,'2025-05-21 10:00:31.000000','2025-05-21 10:00:31.000000','计划财务部',_binary '','商颢薇','$2b$12$2pRFI0onig7AAdhVGmPsSO8IUm/i39wogqQjIk5ixPWcWF1jCaDPW','18810600497','经理','18810600497'),(1032,'2025-05-21 10:00:32.000000','2025-05-21 10:00:32.000000','计划财务部',_binary '','温 馨','$2b$12$8J.M8bSd83kw1BPzwT/Xe.c1MGVizRILSGsVGIQGF.PxGE/MMvhb.','17801365170','经理','17801365170'),(1033,'2025-05-21 10:00:33.000000','2025-05-21 10:00:33.000000','计划财务部',_binary '','孙  文','$2b$12$vLbHy3p7Erm1ruhRVs6JcOgjHS3FRFlLWBRFbVvMsChNE6JByohYK','13717813221','助理','13717813221'),(1034,'2025-05-21 10:00:34.000000','2025-05-21 10:00:34.000000','计划财务部',_binary '','徐  巍','$2b$12$h4lAYIhr7wen3IMr7KDDdOpLkwJ.68cSeHIaX5hzM.5ydNED8FDw2','13901066436','主任','13901066436'),(1035,'2025-05-21 10:00:35.000000','2025-05-21 10:00:35.000000','计划财务部',_binary '','陈  珺','$2b$12$nWgSJAtODCrNdBgCcKtr3eJGT4vFJZwKJsCe0WR5weT3McQcNUKRW','13611204880','高级经理','13611204880'),(1036,'2025-05-21 10:00:36.000000','2025-05-21 10:00:36.000000','计划财务部',_binary '','刘梦佳','$2b$12$OJMRmEMAgcyPgvocptJYDezOjZOxnPzKRhB.IHPDvLjQkZyE6enqe','18210621993','派遣','18210621993'),(1037,'2025-05-21 10:00:37.000000','2025-05-21 10:00:37.000000','计划财务部',_binary '','杨  洋','$2b$12$zoGq1GGong0HtxOPttwKxO/v6NUblzFZWhD.WGH8wRkZfkiQBdRxu','19855596571','派遣','19855596571'),(1038,'2025-05-21 10:00:38.000000','2025-05-21 10:00:38.000000','资金营运部',_binary '','刘晓勇','$2b$12$v3fGuhuiaiqNjx9mYUNFQ.4QXyiSB8xX.ZkJ5.tk7UUy0BfCLjQle','13801271689','部门负责人','13801271689'),(1039,'2025-05-21 10:00:39.000000','2025-05-21 10:00:39.000000','资金营运部',_binary '','王  帆','$2b$12$YAu1V61uTMQCwBggrl5gEO34bLwfExr681zh44wUIAscmRXfI47Y.','15501190813','副总经理','15501190813'),(1040,'2025-05-21 10:00:40.000000','2025-05-21 10:00:40.000000','资金营运部',_binary '','王燕萍','$2b$12$uucgkcyOkM5Trz5nAUiMkuc2IYA4amIGlBchltFumMHTkxTP5IRDu','13601021887','高级资深经理','13601021887'),(1041,'2025-05-21 10:00:41.000000','2025-05-21 10:00:41.000000','资金营运部',_binary '','林家正','$2b$12$ITu4aeOcmeneKEaoTOWamOrlTkrQVXFNQLgR6SEHbyLlBSYFlOT86','13366617177','资深经理','13366617177'),(1042,'2025-05-21 10:00:42.000000','2025-05-21 10:00:42.000000','资金营运部',_binary '','王  莉','$2b$12$QMcDteU239dmpOufKFhpAutOUCSQhMhaU6iWKScEskMUt/xhsWqKS','13611273360','资深经理','13611273360'),(1043,'2025-05-21 10:00:43.000000','2025-05-21 10:00:43.000000','资金营运部',_binary '','林  桦','$2b$12$cpiy5cKJHqq0WV.iTrwnKegHseeGqaMCTXVwLb9LwV2z7180BXuQu','18911759596\n','高级经理','18911759596'),(1044,'2025-05-21 10:00:44.000000','2025-05-21 10:00:44.000000','资金营运部',_binary '','程  诚','$2b$12$3aW4zxvrz/EAYrj53QTaoOnd/V3m4r9ztvMmuytOlSMAmWrarE42W','18518138656','高级经理','18518138656'),(1045,'2025-05-21 10:00:45.000000','2025-05-21 10:00:45.000000','资金营运部',_binary '','岳  峥','$2b$12$vE.d2zuP/ATOlDteka6MOuW0QG8upuyaLudBWZD7FX2peeccajgyG','13520379022','高级经理','13520379022'),(1046,'2025-05-21 10:00:46.000000','2025-05-21 10:00:46.000000','资金营运部',_binary '','宋  英','$2b$12$.eHVmdWpyhFecwLpUV9br.ZbDr..q7OAnBm38UU.C0U5CR0QmyBvC','13903567000','高级经理','13903567000'),(1047,'2025-05-21 10:00:47.000000','2025-05-21 10:00:47.000000','资金营运部',_binary '','罗天成','$2b$12$0vy03gtR0u/fFD4AB0yBWeAiqWc8WkEris8XL4ireLrjOKXPcAWsm','13439287898','经理','13439287898'),(1048,'2025-05-21 10:00:48.000000','2025-05-21 10:00:48.000000','资金营运部',_binary '','陈潇潇','$2b$12$6iTI8m1Y31h2uE6XCq4K4Ofzrp0DG8/YYZjowNgFYEvMLWUQW9fpC','15901273419','经理','15901273419'),(1049,'2025-05-21 10:00:49.000000','2025-05-21 10:00:49.000000','资金营运部',_binary '','刘思远','$2b$12$w4Yrnsm.g9K/IDAJfLGn1uMnLVEp0YCs.ZxoDS78jULgNghZNvp8i','15910934610','经理','15910934610'),(1050,'2025-05-21 10:00:50.000000','2025-05-21 10:00:50.000000','资金营运部',_binary '','胡永杰','$2b$12$mT9MTX0B5HGVI9XpANtYYO7CLyrbOq26ZK93gaWCi1oyvJ4dd6yt6','18810599500','助理','18810599500'),(1051,'2025-05-21 10:00:51.000000','2025-05-21 10:00:51.000000','风险管理部',_binary '','赵玉龙','$2b$12$rrae107Atze/x/Pkfp3VGOwrRH4jBKpI9b35B5qH0YalStM0nlBeq','18600068811','部门负责人','18600068811'),(1052,'2025-05-21 10:00:52.000000','2025-05-21 10:00:52.000000','风险管理部',_binary '','王  虹','$2b$12$jrOBLFGnBN/VPXNiB7So0.Tq9wLx1MuiBN88L9H34kYKSI1PEUTj.','13911619026','副总经理','13911619026'),(1053,'2025-05-21 10:00:53.000000','2025-05-21 10:00:53.000000','风险管理部',_binary '','张  禹','$2b$12$gRZBy4B2iXx4V9Jqptm72.vO9Sj36f8ycsFPSJl4MT8C6e1JXe4zW','18611068716','资深经理','18611068716'),(1054,'2025-05-21 10:00:54.000000','2025-05-21 10:00:54.000000','风险管理部',_binary '','徐  京','$2b$12$yGBFYaU3o.bW1EMD63f73.9o8F8SpukdK5AC7i00..9jrItFa5R0K','13683621297','资深经理','13683621297'),(1055,'2025-05-21 10:00:55.000000','2025-05-21 10:00:55.000000','风险管理部',_binary '','任  娟','$2b$12$z1ZI4fwC2TKXMtOE1M5SkOQ8QEwZ3dIsLE.ks8CplaRb0R9RzHS0i','17610030015','资深经理','17610030015'),(1056,'2025-05-21 10:00:56.000000','2025-05-21 10:00:56.000000','风险管理部',_binary '','王小岑','$2b$12$bKjSE5SOGhvWiVlBLSNZTeoqW0ft9Q8pck0DZ5Y1BS9mj2pEL1O5K','18600736651','资深经理','18600736651'),(1057,'2025-05-21 10:00:57.000000','2025-05-21 10:00:57.000000','风险管理部',_binary '','姜  川','$2b$12$WcuqgSsjwLPwEFZ/c.Jto.EmxFCRzGbNMBc0jZYzw1XgOYYa7nm4O','18600688296','资深经理','18600688296'),(1058,'2025-05-21 10:00:58.000000','2025-05-21 10:00:58.000000','风险管理部',_binary '','熊  辉','$2b$12$HgHK9ryjGRkJ6Jfh9xF79ONdy36n0H1YRqX7WfmaCKqRwvv7PT9KG','13651045367','经理','13651045367'),(1059,'2025-05-21 10:00:59.000000','2025-05-21 10:00:59.000000','风险管理部',_binary '','刘雪慧','$2b$12$7MjTNOP4/NYDlBrJ.CynH.RuzZRom./UJkujDaUMfiNKqGr8dwu5C','15202294957','经理','15202294957'),(1060,'2026-05-21 14:20:50.000000','2026-05-21 14:20:50.000000','风险管理部',_binary '','李科科','$2b$12$vTB0uWMOcaaMk5AW2zCjf.84KoPmcWdiaXSQS1/uDi5Ry1NUJtiAK','17600805434','经理','17600805434'),(1061,'2026-05-21 14:20:50.000000','2026-05-21 14:20:50.000000','风险管理部',_binary '','杨  强','$2b$12$yrBxYkwgYTgJ1iK4J.8N.eAgo5jahre5y9XrCPYxnwEDskXjJtCVa','18618166553','经理','18618166553'),(1062,'2026-05-21 14:20:51.000000','2026-05-21 14:20:51.000000','风险管理部',_binary '','孙丽超','$2b$12$Iq1neRncDtgslDcyflBhlOvMF9yHBtol7VHvcAgpCQ1y1b0CuL.4C','13718590112','经理','13718590112'),(1063,'2026-05-21 14:20:51.000000','2026-05-21 14:20:51.000000','风险管理部',_binary '','吴可奕','$2b$12$lVTB8.9TKBPmxBqYDWW2guejHJMQ1Yq4QGbEsextQMqKOcvRosQHm','13582920728','助理','13582920728'),(1064,'2026-05-21 14:20:51.000000','2026-05-21 14:20:51.000000','风险管理部',_binary '','郭端月','$2b$12$cFEhBt8n8arta5RWbIu05.jUW2IDipi8ubZVVhYnrJOQ4zTFiMskS','18587976105','助理','18587976105'),(1066,'2026-05-21 14:20:51.000000','2026-05-21 14:20:51.000000','授信审批部',_binary '','陆煜桦','$2b$12$7DBz61uup2kaf2CPRwGSLudHwcVtDoKgPrrOULSBoT6DsdKEzm9iO','13426165578','副总经理','13426165578'),(1067,'2026-05-21 14:20:52.000000','2026-05-21 14:20:52.000000','授信审批部',_binary '','朱晓琳','$2b$12$F3ZkUuTUPR8Y.bqO9nCPv.KC4oLGRLICUUqmW4FGkADN/IhES1FJW','13651031168','副总经理','13651031168'),(1068,'2026-05-21 14:20:52.000000','2026-05-21 14:20:52.000000','授信审批部',_binary '','李震锋','$2b$12$cHGRIsMF3ct3NbwrkZbH.eBO5YdvIRED2JQgdrtKga3MxzS.pBy.2','18610345963','资深经理','18610345963'),(1069,'2026-05-21 14:20:52.000000','2026-05-21 14:20:52.000000','授信审批部',_binary '','孙万真','$2b$12$P3d8Ey0MCVTziGWxKJOYruxlD81ldA3GBiBT86zMiqe5tqh2UD..y','13311383203','资深经理','13311383203'),(1070,'2026-05-21 14:20:52.000000','2026-05-21 14:20:52.000000','授信审批部',_binary '','张兴龙','$2b$12$rE1clzThjQ4k8nmpaE7Ai.JtqF2HO9wLYx0NsAhas5E451JHd/LFO','15901017598','资深经理','15901017598'),(1071,'2026-05-21 14:20:53.000000','2026-05-21 14:20:53.000000','授信审批部',_binary '','邹  蔚','$2b$12$T697acVAEHLZwMt9E3mROeI2jnHJUdI.moWv344VlgsHuBwA/jqC2','15972917368','资深经理','15972917368'),(1072,'2026-05-21 14:20:53.000000','2026-05-21 14:20:53.000000','授信审批部',_binary '','刘  钊','$2b$12$wn3prSFBBEp9ut1qP0cLs.3xok50q.948KnbdLdVgtoMrmzhKoSn2','15001002699','资深经理','15001002699'),(1073,'2026-05-21 14:20:53.000000','2026-05-21 14:20:53.000000','授信审批部',_binary '','路  畅','$2b$12$qtKXsRiLsZicv92EmjHuHuCONAN2h0olDtJYedpgXc12FhfZO9uQC','13917507237','高级经理','13917507237'),(1074,'2026-05-21 14:20:53.000000','2026-05-21 14:20:53.000000','授信审批部',_binary '','赵  琳','$2b$12$OW9roiPvhFN1MJ/VOAIVQ.LcHxvmmdFA33aKs6j/34sIAPuVR8NtS','18322515752','高级经理','18322515752'),(1075,'2026-05-21 14:20:54.000000','2026-05-21 14:20:54.000000','授信审批部',_binary '','郭  曼','$2b$12$151hsw1Zrk70Y/XGMPMP4uDw.0FZCX7KA9F8fxkd8AA0NFIQ05.cS','15811125313','高级经理','15811125313'),(1076,'2026-05-21 14:20:54.000000','2026-05-21 14:20:54.000000','授信审批部',_binary '','钟稆诚','$2b$12$5X9wKMHaCN5qxo/HkS4sX.UxeXocFXBEuZjLfUm7lURV9srCLeIHS','17801262645','经理','17801262645'),(1077,'2026-05-21 14:20:54.000000','2026-05-21 14:20:54.000000','授信审批部',_binary '','张闻名','$2b$12$F8zCWksyfzdWfJ0KJYSLBuUs.3zvSf8bIqDlm81Hd4FrhgAEs06qS','18618452812','经理','18618452812'),(1078,'2026-05-21 14:20:54.000000','2026-05-21 14:20:54.000000','授信审批部',_binary '','王云菲','$2b$12$lBwGuKlvz18NTdI8ULdkOugsKbcmB0WtMUGRXQyB1E6faD9S/UbMq','18222900637','经理','18222900637'),(1079,'2026-05-21 14:20:54.000000','2026-05-21 14:20:54.000000','授信审批部',_binary '','齐国虎','$2b$12$VesM7.gXmhLcGlpORlI9wOOZBXX38cC.hD.dRO54tTRF8q3j9yTzG','18500483396','经理','18500483396'),(1080,'2026-05-21 14:20:55.000000','2026-05-21 14:20:55.000000','授信审批部',_binary '','鄯瑾佳','$2b$12$mS2vQWok0RryqLyiteoA7O906QOg1.m/SqgXZE2qUwPMmBaEwlf2W','18500087330','经理','18500087330'),(1081,'2026-05-21 14:20:55.000000','2026-05-21 14:20:55.000000','授信审批部',_binary '','徐志远','$2b$12$B4U2wi8.rpFhgAdE.aPz/Oyl6zh3EqaRc.l7Y.B7p3CB1p/xvW16S','18210220098','助理','18210220098'),(1082,'2026-05-21 14:20:55.000000','2026-05-21 14:20:55.000000','授信审批部',_binary '','郑  莹','$2b$12$zb3GbacC7fgT9RwCqBLk0uyzPNQyeS2MhnypVXOUdln0yAnaXxCw.','18301663870','助理','18301663870'),(1083,'2026-05-21 14:20:55.000000','2026-05-21 14:20:55.000000','授信审批部',_binary '','李  佳','$2b$12$nzPllIuyQF.lGAesjV4cpOZ42gVKKOlbJFUcvyECedBkSgPddv8by','13801229742','助理','13801229742'),(1084,'2026-05-21 14:20:56.000000','2026-05-21 14:20:56.000000','授信审批部',_binary '','夏心言','$2b$12$wSKS6sip6RkrxVWHs3mgse1WNas/EdJPcTkEb01.iBtnabkglwrHC','18600650520','助理','18600650520'),(1085,'2026-05-21 14:20:56.000000','2026-05-21 14:20:56.000000','授信审批部',_binary '','任靖怡','$2b$12$3GRmxiLWl.h.1.v5Yl4rse1XY43lpAn45T/TnPh.Coi4O6yHgaehK','18188008746','助理','18188008746'),(1086,'2026-05-21 14:20:56.000000','2026-05-21 14:20:56.000000','授信审批部',_binary '','李颖慧','$2b$12$mNGDDyz9gDsnIJMlylGDdOn2EnRrVPwqigOLjc1mJOb00mIj18SmW','18839108126','助理','18839108126'),(1087,'2026-05-21 14:20:56.000000','2026-05-21 14:20:56.000000','资产管理部',_binary '','庞卢璐','$2b$12$4N4uGtHRUcnpMyDkjQ9C5OUIkyK2g0gu6u78PQcEPQH8BVX69KmpC','18331436749','部门负责人','18331436749'),(1088,'2026-05-21 14:20:57.000000','2026-05-21 14:20:57.000000','资产管理部',_binary '','曾宪发','$2b$12$rK0EOOgFanFhxwopMvvINelCRZl2e9LVjlM9BTarT3PdeVFIazgrq','18911707035','总经理助理','18911707035'),(1089,'2026-05-21 14:20:57.000000','2026-05-21 14:20:57.000000','资产管理部',_binary '','段果山','$2b$12$.2XqsYCLloa.bOEf5RJVreKCosGeuvc1ud8zfyCpRHvITUssj9ktO','15110299885','总经理助理','15110299885'),(1090,'2026-05-21 14:20:57.000000','2026-05-21 14:20:57.000000','资产管理部',_binary '','沈文浩','$2b$12$Uxkhop9S5U9hCIZyamvWnury9bDl8JtxdEptXWVFKuoLDb2g4/Lj2','13311160702','资深经理','13311160702'),(1091,'2026-05-21 14:20:57.000000','2026-05-21 14:20:57.000000','资产管理部',_binary '','武  强','$2b$12$QPAVticjSyhgetyWqCCHle4FqE0kTSUwpPIQSF02OqQBuRxVZSKyW','18611375012','资深经理','18611375012'),(1092,'2026-05-21 14:20:58.000000','2026-05-21 14:20:58.000000','资产管理部',_binary '','康帅文','$2b$12$JFmMh.gTQOKDeFvYTcwCm.U9psKAkfq/a54ivuWxNs5Ht4QvaLhhS','15810292918','高级经理','15810292918'),(1093,'2026-05-21 14:20:58.000000','2026-05-21 14:20:58.000000','资产管理部',_binary '','曲圣悦','$2b$12$rhkinMeOiVLwRvUMfe5//ug7ZwYunJHhVWEMr6q9lQ4DhbLrl/MNO','13401163352','高级经理','13401163352'),(1094,'2026-05-21 14:20:58.000000','2026-05-21 14:20:58.000000','资产管理部',_binary '','张亦驰','$2b$12$EUYBPHQMdGyRJEvUUu09wONPTlRxqwDDFAg1cEAScyKwhWK533etG','17710480213','高级经理','17710480213'),(1095,'2026-05-21 14:20:58.000000','2026-05-21 14:20:58.000000','资产管理部',_binary '','吕明辰','$2b$12$mV8TEyhklL8V9gGomz3.I.7VcqtfUMxmjL0k52zo83MFe90KDugUq','13521077533','经理','13521077533'),(1096,'2026-05-21 14:20:58.000000','2026-05-21 14:20:58.000000','资产管理部',_binary '','许  航','$2b$12$kaOWqHbRrts7uKcutmFz4O2/fDTCslQPnKiT/hP2kROxZzkBG9HBa','13671355280','经理','13671355280'),(1097,'2026-05-21 14:20:59.000000','2026-05-21 14:20:59.000000','资产管理部',_binary '','薛洪峰','$2b$12$z5zE1jmHEXttCwxxi.7dqucPxjWS0LpIhRyWhsPVbPR/qoDcN17QK','18920280672','经理','18920280672'),(1098,'2026-05-21 14:20:59.000000','2026-05-21 14:20:59.000000','资产管理部',_binary '','耿  欣','$2b$12$YeKuUr.R14CEb2VQ429w8eLcXjosdBTy9jnqw2V7js7hnU.P8mpbq','18810519208','经理','18810519208'),(1099,'2026-05-21 14:20:59.000000','2026-05-21 14:20:59.000000','资产管理部',_binary '','刘泽润','$2b$12$zdVzBgkOIaq4w6w5UAwc4ODo3aC3c7rVnscBEzQ.CSXy4tVOPMXRG','13811552686','经理','13811552686'),(1100,'2026-05-21 14:20:59.000000','2026-05-21 14:20:59.000000','资产管理部',_binary '','刘艺诗','$2b$12$/jB13ijPe.uzwOHwiJH9/eWVswi2WdyfPeqv39bE92lqehOmAGd1O','13810782570','经理','13810782570'),(1101,'2026-05-21 14:21:00.000000','2026-05-21 14:21:00.000000','资产管理部',_binary '','何丽丽','$2b$12$AJ8mi793zYp8ckvy5vDMheIKPkdaS.AypPuAa4a2CReSKE87sj0Pm','17710042333','经理','17710042333'),(1102,'2026-05-21 14:21:00.000000','2026-05-21 14:21:00.000000','资产管理部',_binary '','张金涛','$2b$12$zFjdXrksc0v00YrU.vy2y.j2tD9N79GIxpXimqRrfmw4x.wTPdCEe','18700864710','经理','18700864710'),(1103,'2026-05-21 14:21:00.000000','2026-05-21 14:21:00.000000','资产保全部',_binary '','夏彦明','$2b$12$7fBYoKeOZl3.e4OYeIwt7uos5x5Bs3q.GAg9AK.kYdMZd2I1zv/9C','18901069697','部门负责人','18901069697'),(1104,'2026-05-21 14:21:00.000000','2026-05-21 14:21:00.000000','资产保全部',_binary '','周  朋','$2b$12$SlAD4zXW9OoRYdGOtW8Bdey3PEgJFXR9xVRSjv98l5PMT6.b8FEjS','13520869275','总经理助理','13520869275'),(1105,'2026-05-21 14:21:01.000000','2026-05-21 14:21:01.000000','资产保全部',_binary '','王  娟','$2b$12$BVcFBIcJrejK6KHx.9GpEeIw5xgHLLoohzVBRAN5DgTZNHkH/m1qC','13910259794','资深经理','13910259794'),(1106,'2026-05-21 14:21:01.000000','2026-05-21 14:21:01.000000','资产保全部',_binary '','闫  昊','$2b$12$QOMXbo4.vw.SwnIra8/9TeEfcrkaipCO1aLo3irElZ4PoSgVUoDrC','13651286130','资深经理','13651286130'),(1107,'2026-05-21 14:21:01.000000','2026-05-21 14:21:01.000000','资产保全部',_binary '','郭  亮','$2b$12$Sw12pF2ZWqwmdx.xmRJZMOP/Uyz25l9rnXAUn5RdghZ/i9oH0UJCi','18501281195','资深经理','18501281195'),(1108,'2026-05-21 14:21:01.000000','2026-05-21 14:21:01.000000','资产保全部',_binary '','周  云','$2b$12$UdTDM5c06/DQcUSnMV/Bcugl3usfNbgved1Sy6X030BqxGS50Lzbi','18908716168','资深经理','18908716168'),(1109,'2026-05-21 14:21:02.000000','2026-05-21 14:21:02.000000','资产保全部',_binary '','杨雨琛','$2b$12$0AvyG2oc1gZD12zL6oDbGOclaGPXvKvWMM.JyOlcH6zMjLbxSxLh.','13522165056','经理','13522165056'),(1110,'2026-05-21 14:21:02.000000','2026-05-21 14:21:02.000000','资产保全部',_binary '','王海波','$2b$12$Pg9FCf7liQCvNtXqGLtWK.EPrkclK3NYrV7j5dU7qCVTSOFqMZqEu','18522091066','经理','18522091066'),(1111,'2026-05-21 14:21:02.000000','2026-05-21 14:21:02.000000','资产保全部',_binary '','金  凯','$2b$12$o0odVwazRtyHDX1wL3FGyOhPWhjrKrcUUqfDnoRQVjtmuWGlL6/.u','18911207385','经理','18911207385'),(1112,'2026-05-21 14:21:02.000000','2026-05-21 14:21:02.000000','法律合规部',_binary '','黄  巍','$2b$12$6fguGxJy9ac2v0oogXsb.eO8PbUcfBeifzaABcOqasQePIZzlidnG','13699136066','部门负责人','13699136066'),(1113,'2026-05-21 14:21:02.000000','2026-05-21 14:21:02.000000','法律合规部',_binary '','邓籽归','$2b$12$8XrJXaG/UpHGmr0xmd896eQ/k.pI9Oo18i37qGer8yxNfcC9sdDtW','18010036577','总经理助理','18010036577'),(1114,'2026-05-21 14:21:03.000000','2026-05-21 14:21:03.000000','法律合规部',_binary '','高  飞','$2b$12$q1GaI7MLpeMYCoMK1FKgVuisqt6Nl7gYYrup.ZZ.cRzsi7kr6vsjG','15600095577','资深经理','15600095577'),(1115,'2026-05-21 14:21:03.000000','2026-05-21 14:21:03.000000','法律合规部',_binary '','陈 萍','$2b$12$kcuBlDIbEczd3eCnyc3UwOe9YTi2Iun/EQAZTg3w47Qt1btSl9CmK','15853195206','高级经理','15853195206'),(1116,'2026-05-21 14:21:03.000000','2026-05-21 14:21:03.000000','法律合规部',_binary '','李信芳','$2b$12$w3CnSpeEbBKXTmQ3.heloe/hHTH654oYAfEOo3aNT5TgewjSLtOWW','18601169869','高级经理','18601169869'),(1117,'2026-05-21 14:21:03.000000','2026-05-21 14:21:03.000000','审计部',_binary '','杨现菊','$2b$12$SJxqteUEZSpWjdAq0eU.3.V8xdgzVPbfhv4/mD0VFc5uQaYfWI2..','13910518919','部门负责人','13910518919'),(1118,'2026-05-21 14:21:04.000000','2026-05-21 14:21:04.000000','审计部',_binary '','丁闽军','$2b$12$bVTCWTND6jC2XjBCDp7tXOgTPVVGf5lyDRpVQmWnat2YXBLK.IzBC','13910926591','资深经理','13910926591'),(1119,'2026-05-21 14:21:04.000000','2026-05-21 14:21:04.000000','审计部',_binary '','翟  娜','$2b$12$eHm9XoDl7/7W8TQdLDfwqeGYLU.xpmF.Kub4qpQj5AvfrEs28j3p2','18600183380','高级经理','18600183380'),(1120,'2026-05-21 14:21:04.000000','2026-05-21 14:21:04.000000','审计部',_binary '','刘效祥','$2b$12$9cecvUGYQhDtbO.icGLWfuZmIW4wlZCavjwwWMeFFlAzVVgAajEDm','18610315132','高级经理','18610315132'),(1121,'2026-05-21 14:21:04.000000','2026-05-21 14:21:04.000000','审计部',_binary '','李洁雯','$2b$12$IL56EIsTapVTyLIxZjSBXeI5mF6YMDiXrRhXHhd9Fi7H3yjUH6gW2','18810298547','经理','18810298547'),(1122,'2026-05-21 14:21:05.000000','2026-05-21 14:21:05.000000','信息科技部',_binary '','李  田','$2b$12$KHGFW4T0qLkVdHcqcR50t.G50VDpMQV1t3FVp/pkY132fNNjydjzu','13810123560','部门负责人','13810123560'),(1123,'2026-05-21 14:21:05.000000','2026-05-21 14:21:05.000000','信息科技部',_binary '','丘杰平','$2b$12$cfj9ttKkwVN8y1AgRfqayeZ/6rDtsw4hx2g/IIXAvX6qlg3IVuA62','13911200516','副总经理','13911200516'),(1124,'2026-05-21 14:21:05.000000','2026-05-21 14:21:05.000000','信息科技部',_binary '','付  蓉','$2b$12$faUEhqp8FfokdMcjP.oDeeJsFviqxP38i2MrizbBbVqrmLnxS3JCS','13466746485','副总经理','13466746485'),(1126,'2026-05-21 14:21:05.000000','2026-05-21 14:21:05.000000','信息科技部',_binary '','袁启勋','$2b$12$OlWYguGICyuUcZ4VX3q2OuBeALYu5ln3GLdNQzA74Fgz0b8fHwP9K','18800005138','技术总监','18800005138'),(1127,'2026-05-21 14:21:05.000000','2026-05-21 14:21:05.000000','信息科技部',_binary '','山  伟','$2b$12$I6csgtxJ5DRiChfsVs7K1uaa3lcpTflVcKCjn0rkrR2Tm8VgHfFdS','13466370251','技术总监','13466370251'),(1128,'2026-05-21 14:21:06.000000','2026-05-21 14:21:06.000000','信息科技部',_binary '','沈  亮','$2b$12$WhokzLnQWdPB4jMWjEHnK.IgO8PxeocmMAG6.LUApwr3/EhE9GxWO','18319011606','技术总监','18319011606'),(1129,'2026-05-21 14:21:06.000000','2026-05-21 14:21:06.000000','信息科技部',_binary '','邓  鹏','$2b$12$baUbCkpButN1JbMNWJ40de0xQ7iXPCf3PPE2VB6CKdZyLCySRhJj2','18610773712','高级信息技术工程师','18610773712'),(1130,'2026-05-21 14:21:06.000000','2026-05-21 14:21:06.000000','信息科技部',_binary '','宋超林','$2b$12$Fa6NARTtiVYT8KEgnrl9zOiD1nuZBdaM.Y4s5tmVcY9ee5JvhnSQq','18514616383','高级信息技术工程师','18514616383'),(1131,'2026-05-21 14:21:06.000000','2026-05-21 14:21:06.000000','信息科技部',_binary '','汤银辉','$2b$12$F1ezTFv4FyrkVIuCzrhImuW/82tX599cBDvVhjTTVGjmW9NPqj9G6','18988827685','高级信息技术工程师','18988827685'),(1132,'2026-05-21 14:21:07.000000','2026-05-21 14:21:07.000000','信息科技部',_binary '','曲  宁','$2b$12$93jxLHCZJn2mMBShOpnBTuPZAF5KQ7NWci/NEhljh3Us9OKRF6iSq','13522686856','高级信息技术工程师','13522686856'),(1133,'2026-05-21 14:21:07.000000','2026-05-21 14:21:07.000000','信息科技部',_binary '','曹国威','$2b$12$GGW76eDB.jzd0UoH4ywJSO4dHIh0ej8Ha6uut6AaZtfzM3G7XetSC','17600687112','高级信息技术工程师','17600687112'),(1134,'2026-05-21 14:21:07.000000','2026-05-21 14:21:07.000000','信息科技部',_binary '','李  超','$2b$12$iFrCLqUNDdL9ltSTcJiAb.2nS60vG791aileuFu7OqK0H6Rcbs/RO','15110016868','高级信息技术工程师','15110016868'),(1135,'2026-05-21 14:21:07.000000','2026-05-21 14:21:07.000000','信息科技部',_binary '','陈天龙','$2b$12$AdEra8MQotoJSEobWNtTwO.gmdsYzMH2keUBT5XhPUFMDz/EHYGlK','15652581010','高级信息技术工程师','15652581010'),(1136,'2026-05-21 14:21:08.000000','2026-05-21 14:21:08.000000','信息科技部',_binary '','毕淑卉','$2b$12$uHc5alu2rijo5Dheg1qea./4wSzkDsBas9WIGAXCWRh8hgUW7us/S','15011161196','信息技术工程师','15011161196'),(1137,'2026-05-21 14:21:08.000000','2026-05-21 14:21:08.000000','信息科技部',_binary '','周明润','$2b$12$45dLNO1DkGyFr0u0ugP7zOhYY0bLA5zTDyNmw915PBwg4ves7Rbn2','15010045790','信息技术工程师','15010045790'),(1138,'2026-05-21 14:21:08.000000','2026-05-21 14:21:08.000000','信息科技部',_binary '','琚  寰','$2b$12$s6WKbidhmm.bj4NvbavI2Ov0g9GwRP.qtxZxejbU9oKXbZVfN3XUi','13671087125','信息技术工程师','13671087125'),(1139,'2026-05-21 14:21:08.000000','2026-05-21 14:21:08.000000','信息科技部',_binary '','张  真','$2b$12$8mqNi/ZQMpVMpRCyd6ZX/.kOvW/4SSgnSMozQPRlZUFAboezDl72i','18646229522','信息技术工程师','18646229522'),(1140,'2026-05-21 14:21:09.000000','2026-05-21 14:21:09.000000','信息科技部',_binary '','朱宏坤','$2b$12$Hv6MeeUBUMP4Y0hj25Wrj.Mv65mtnZH/Osyrt.KdO3jxLMkKvYaG2','18610052757','信息技术工程师','18610052757'),(1141,'2026-05-21 14:21:09.000000','2026-05-21 14:21:09.000000','信息科技部',_binary '','王峰军','$2b$12$iS.L7ZCQjyLIeuyhzHZ4pOx9uSKPvCVF.8REAQtWeOse15LrdhJYq','18253162641','信息技术工程师','18253162641'),(1142,'2026-05-21 14:21:09.000000','2026-05-21 14:21:09.000000','信息科技部',_binary '','谷  静','$2b$12$hF9nXAPv0Z31453G262.r.tiob6fqq9QAqB9dLkjQ2eu5fDq2oeUW','18519393249','信息技术工程师','18519393249'),(1143,'2026-05-21 14:21:09.000000','2026-05-21 14:21:09.000000','信息科技部',_binary '','樊泽璞','$2b$12$/EKEx2Jvu2Hfb6zPbn6D1.pK2nDWukoHi0LZLKNjSQItvJgZtDRr2','17600119194','信息技术工程师','17600119194'),(1144,'2026-05-21 14:21:09.000000','2026-05-21 14:21:09.000000','信息科技部',_binary '','王重阳','$2b$12$oq8FsfYeiweILM.oZzynO.6lrbm/epUo9T.cDHe4yGjmiD3MIRpya','13591157040','信息技术工程师','13591157040'),(1145,'2026-05-21 14:21:10.000000','2026-05-21 14:21:10.000000','信息科技部',_binary '','胡伯涵','$2b$12$06nbEiqciARhJt4b8zGsuen.x4NSLVkxnEK0a651UdV6EO2nhjq6i','13581620394','信息技术工程师','13581620394'),(1146,'2026-05-21 14:21:10.000000','2026-05-21 14:21:10.000000','信息科技部',_binary '','史小龙','$2b$12$cUVTaFgCYqDwg6iL2bP/x.vcTQ/Fpw4zsAFLPA2gJSuc.BeW6CU6C','18810730177','信息技术工程师','18810730177'),(1147,'2026-05-21 14:21:10.000000','2026-05-21 14:21:10.000000','信息科技部',_binary '','贾  伟','$2b$12$JYMW2esCjc9BUgYNHWfaN.d1rXaR6LwiJcItyYU2Min/b.h6pUVru','15311189838','助理信息技术工程师','15311189838'),(1148,'2026-05-21 14:21:10.000000','2026-05-21 14:21:10.000000','信息科技部',_binary '','刘文龙','$2b$12$4EnH5d7q1uCieg1dtXLX.eaUdaMcf4x6tdDbD5Gc.lRmXjRupYNi6','18001197326','助理信息技术工程师','18001197326'),(1150,'2026-05-21 14:21:11.000000','2026-05-21 14:21:11.000000','信息科技部',_binary '','罗  京','$2b$12$bf2SkRH3BwuNziSYibe2d.ZAYgS1GMk5aT0/s.wD2eWKjTLydYhra','18511581700','助理信息技术工程师','18511581700'),(1151,'2026-05-21 14:21:11.000000','2026-05-21 14:21:11.000000','信息科技部',_binary '','王艺霖','$2b$12$doLrJR6PkLD3mUtoXvyWkeEPZdhPggeu0WuYUSUB3AEnwZ5jUKxfK','18402576395','助理信息技术工程师','18402576395'),(1152,'2026-05-21 14:21:11.000000','2026-05-21 14:21:11.000000','信息科技部',_binary '','郭红军','$2b$12$638Z5kGYGmLYDIgZuwhPKOqCFBcL9svt3kQb.CXkM9Eowm1G24SUC','15612501128','助理信息技术工程师','15612501128'),(1153,'2026-05-21 14:21:11.000000','2026-05-21 14:21:11.000000','营销管理部',_binary '','李吾一','$2b$12$ks9cFVqmC4/qECGNCGkloOg0h7xSd2WJSvdNwo1TnOpjhMH8AKJu.','13651279751','部门负责人','13651279751'),(1154,'2026-05-21 14:21:12.000000','2026-05-21 14:21:12.000000','营销管理部',_binary '','周运桥','$2b$12$VhLA9RzopJ5.1Gic/IQDq.A/T/zlX0AyTTsN2BI83EpRQOv7BpPjC','13520280718','副总经理\n项目公司管理中心总经理（兼）','13520280718'),(1155,'2026-05-21 14:21:12.000000','2026-05-21 14:21:12.000000','营销管理部',_binary '','林竹潇','$2b$12$nHS4bnOZGwhIAgR4PxmNmOfZ47rsox02Uv85x0KFHfcd4KOzHg46W','13693131683','总经理助理','13693131683'),(1156,'2026-05-21 14:21:12.000000','2026-05-21 14:21:12.000000','营销管理部',_binary '','曹  妍','$2b$12$WGNY8/cX2IAYFGppmbUOU.4.LWzI2F/73MKTYVU.25.cyJGUjLNXm','13810312697','资深经理','13810312697'),(1157,'2026-05-21 14:21:12.000000','2026-05-21 14:21:12.000000','营销管理部',_binary '','张  音','$2b$12$SWnIk/438ZH.ZS2jgDEl2eSQzYFj0Km2Jf7U9lR4j.MC/xCfFymBy','18612629855','资深经理','18612629855'),(1158,'2026-05-21 14:21:13.000000','2026-05-21 14:21:13.000000','营销管理部',_binary '','刘洪博','$2b$12$TGtFdSLob33lyjVSsBHnG.jehuLjIcoyy48LqqcEzVyTVTZgX/kwO','18366156162','高级经理','18366156162'),(1159,'2026-05-21 14:21:13.000000','2026-05-21 14:21:13.000000','营销管理部',_binary '','刘雨卿','$2b$12$2A58.leuXBMYdC93.5XKHuyDQSe56wsNzRVl7e/aAsFApsJ9Hla4m','13910530092','经理','13910530092'),(1160,'2026-05-21 14:21:13.000000','2026-05-21 14:21:13.000000','营销管理部',_binary '','刘  磊','$2b$12$HjlmOwN4eHyZm4E6eY4YxuN0HFCgzATB3zVJXh6pGfMGvjOB0iQVW','15810503728','经理','15810503728'),(1161,'2026-05-21 14:21:13.000000','2026-05-21 14:21:13.000000','营销管理部',_binary '','安家宽','$2b$12$XjLOOiJH2jdJSv8ZK2jhpOnurR7vp7ecAtpKB2aaC7b5TR0.DcqSe','15811378676','经理','15811378676'),(1162,'2026-05-21 14:21:13.000000','2026-05-21 14:21:13.000000','营销管理部',_binary '','李铭涓','$2b$12$ygSLL4SFzu4Dh0tAAp5Z8eeMcHrExp/H1c.gvBwgsHje/cdA.khM2','18039776669','经理','18039776669'),(1163,'2026-05-21 14:21:14.000000','2026-05-21 14:21:14.000000','营销管理部',_binary '','魏毓琪','$2b$12$sI4qxnCGfatvFY3n13eICOp0gvYfb2khz7tmFkMbMNnzmsBJgDMYe','15703552971','助理','15703552971'),(1164,'2026-05-21 14:21:14.000000','2026-05-21 14:21:14.000000','营销管理部',_binary '','米  兰','$2b$12$R0bSu/ic5qTs5xbIVw8pQ.M3Nz3RwAjhiMQAeRwCgHfkhKL4sHjdm','13810484040','助理','13810484040'),(1165,'2026-05-21 14:21:14.000000','2026-05-21 14:21:14.000000','生态协同部',_binary '','王  浩','$2b$12$4kpPXjeLdS4CwxXqHS.HKe/JaZrC8iq3fYjSpnZhbEk2.a6WuJ8aq','18610164905','部门负责人','18610164905'),(1166,'2026-05-21 14:21:14.000000','2026-05-21 14:21:14.000000','生态协同部',_binary '','韩瑶瑶','$2b$12$phsgrAyPOyiA4WknOtrPmuHf1k.vZTIc.aYhcrlX6qFvh//JzHzBi','13126767462','副总经理','13126767462'),(1167,'2026-05-21 14:21:15.000000','2026-05-21 14:21:15.000000','生态协同部',_binary '','佘晓曦','$2b$12$gsVG555PM/ftj098IIcyt.Vabz2fCgN9qdZ..e0CUDrErwFkJJScS','13811925918','总经理助理','13811925918'),(1168,'2026-05-21 14:21:15.000000','2026-05-21 14:21:15.000000','生态协同部',_binary '','田  军','$2b$12$VEh2NZdRttdIVG5kox3HFOW9UAl.ZtV71G7ECJ5ZDkBBb42oh/EIW','18612375515','资深经理','18612375515'),(1169,'2026-05-21 14:21:15.000000','2026-05-21 14:21:15.000000','生态协同部',_binary '','张雁飞','$2b$12$fcb27b0cy31G8XU1u///ieWzNEtearZiKIjDpflLGiURo8SWnH1CS','18500856665','资深经理','18500856665'),(1170,'2026-05-21 14:21:15.000000','2026-05-21 14:21:15.000000','生态协同部',_binary '','杜  贺','$2b$12$D1g/BsG9wcQxtrGENaO7U.U1QltRrv.SwI1M.qP1vDSkF2N8uaguS','18511988081','高级经理','18511988081'),(1171,'2026-05-21 14:21:16.000000','2026-05-21 14:21:16.000000','生态协同部',_binary '','李彦林','$2b$12$Pf2LBQP0vVe8ErLICuSSy.F59riiBEO18nWT0HV1IvbC6yrFn8FUC','18917092230','高级经理','18917092230'),(1172,'2026-05-21 14:21:16.000000','2026-05-21 14:21:16.000000','生态协同部',_binary '','江文成','$2b$12$PFpK2LSaMfcCJHOh76g0iu96kb3BtFMwHq/V3NFHm//f4p53Jl0ei','15910836266','高级经理','15910836266'),(1173,'2026-05-21 14:21:16.000000','2026-05-21 14:21:16.000000','生态协同部',_binary '','朱乾威','$2b$12$wXM4CYnP01by5p7CyT4cEebiZZ1bqA/KrcTfX96wgydc6/7hvlvcK','15990728295','高级经理','15990728295'),(1174,'2026-05-21 14:21:16.000000','2026-05-21 14:21:16.000000','生态协同部',_binary '','乔星宇','$2b$12$WcNeCOG2rjipVtR1EMVg2eRRe.F0XEaMfYlDobGDejhATZqrH/2vm','18813057806','经理','18813057806'),(1175,'2026-05-21 14:21:16.000000','2026-05-21 14:21:16.000000','生态协同部',_binary '','刘思宇','$2b$12$OgYWLmKiXNYyRHDI73jflOlWwvb.b6SeVub.6uGwK43JIkBh5ahBy','18001301904','经理','18001301904'),(1176,'2026-05-21 14:21:17.000000','2026-05-21 14:21:17.000000','生态协同部',_binary '','佟泽宇','$2b$12$iGMAyzz.spHNQy6p8MPP5.HuZ3p6gyPvl7RSItcvjh.58RXNSmaOO','15801598250','经理','15801598250'),(1177,'2026-05-21 14:21:17.000000','2026-05-21 14:21:17.000000','生态协同部',_binary '','江  舸','$2b$12$RNKpS6O8/trG.aHJgVC9j.gTL8Rn/7fn0dzmNCEnL8evqzLyInsw.','15601055696','经理','15601055696'),(1178,'2026-05-21 14:21:17.000000','2026-05-21 14:21:17.000000','生态协同部',_binary '','徐  伟','$2b$12$aPR9yoOTwtyv08WcpnyP/u4vM4EU9w58Ep717nP2lthUZD7LF8Gui','18605858118','经理','18605858118'),(1179,'2026-05-21 14:21:17.000000','2026-05-21 14:21:17.000000','生态协同部',_binary '','吴联鹤','$2b$12$DRWy1ZHhehcnxppqeGMjCOdvqdbWCTK7q.VvacU8w12bMxeof0HyG','15168030202','经理','15168030202'),(1180,'2026-05-21 14:21:18.000000','2026-05-21 14:21:18.000000','生态协同部',_binary '','张永杰','$2b$12$qEhIpV/.qBidEoEhJXt6qODANXV1sKzan5CaC9f0G8qid71QrB606','15850712570','经理','15850712570'),(1181,'2026-05-21 14:21:18.000000','2026-05-21 14:21:18.000000','生态协同部',_binary '','袁模羿','$2b$12$ZnvXxn36qhTrbgPAFtuZ/upmgjgBmX2xi5eBTGkYjgGgfzC57zZOi','13691240290','经理','13691240290'),(1182,'2026-05-21 14:21:18.000000','2026-05-21 14:21:18.000000','生态协同部',_binary '','朱韵铮','$2b$12$LQr8VQVhFTr8tT7dzjRL2OcL/4UPys.fuOcP8kfckOo.aT4Gxg/v2','13522389866','经理','13522389866'),(1183,'2026-05-21 14:21:18.000000','2026-05-21 14:21:18.000000','生态协同部',_binary '','成  武','$2b$12$0PBK799LyAkoG/YR038SEu8NTO9apy6ZRP2E/GzTqQD8jtcJ3s.oe','18323832993','经理','18323832993'),(1184,'2026-05-21 14:21:19.000000','2026-05-21 14:21:19.000000','生态协同部',_binary '','刘知立','$2b$12$dZmnJd750mARm4QaVkZx7.tRQLHyOnzMrDw2jNeu6ArCF8WITpJyC','15861181756','经理','15861181756'),(1185,'2026-05-21 14:21:19.000000','2026-05-21 14:21:19.000000','生态协同部',_binary '','魏余牧','$2b$12$WUOZQ.6iB2StYRo0Rq1Mf.gczJ6k0HHyzgj4fPq1K7A8az0FaU00a','18693287652','助理','18693287652'),(1186,'2026-05-21 14:21:19.000000','2026-05-21 14:21:19.000000','生态协同部',_binary '','许嘉萌','$2b$12$gY0GB5jtnpavyA0qyYVfAuPlfEyqwrHWugO5vVXlC6840LtwQ2Qee','13717956080','助理','13717956080'),(1187,'2026-05-21 14:21:19.000000','2026-05-21 14:21:19.000000','生态协同部',_binary '','朱万泰','$2b$12$/8SMR1y7vdBZloooswWlKefIP2RKzlVoXkImfR0Q6gq0rkMIdsMg2','17695507505','助理','17695507505'),(1188,'2026-05-21 14:21:20.000000','2026-05-21 14:21:20.000000','生态协同部',_binary '','黄飞雨','$2b$12$2V4dL1Muz0iSHN6DJPToUOr/aWak0LGmXLwYgEIeA9evglFDaxmZe','18500279812','助理','18500279812'),(1189,'2026-05-21 14:21:20.000000','2026-05-21 14:21:20.000000','生态协同部',_binary '','李统照','$2b$12$PMx6rnuKM3GOEJxG6sEQkurmK1iZ/GjIdQgxy8HYNj8kN3QtR7Ec2','18103529019','助理','18103529019'),(1190,'2026-05-21 14:21:20.000000','2026-05-21 14:21:20.000000','生态协同部',_binary '','鲍  婕','$2b$12$uuXhSLQcZSe0srGoTI49t.MYt5wxbPqE7TQf1ck6FpA.cPsE9vOQi','18210189850','助理','18210189850'),(1191,'2026-05-21 14:21:20.000000','2026-05-21 14:21:20.000000','生态协同部',_binary '','刘秀峰','$2b$12$mhs8rTR2U8jvlKatL5/L8uk4EgWHLLBhwwcttonO7oYIm7FhrD8SK','13801234101','派遣','13801234101'),(1192,'2026-05-21 14:21:20.000000','2026-05-21 14:21:20.000000','生态协同部',_binary '','黄  斌','$2b$12$EKFiiYSF7o6uGN4lGy6oueKtXN7AxVk3xviDQnEjxRScyRrCTcGDO','18662926878','派遣','18662926878'),(1193,'2026-05-21 14:21:21.000000','2026-05-21 14:21:21.000000','生态协同部',_binary '','王跃跃','$2b$12$eHOkQrK9we6ByEFk258FJuQnwDxIh9t4wO3QBAxsudfnQ/q6U0z5K','13917052220','派遣','13917052220'),(1194,'2026-05-21 14:21:21.000000','2026-05-21 14:21:21.000000','产业金融业务部',_binary '','张  盛','$2b$12$aoE.P6f8wbksCWccWFgmRuzN7jJQHG/Ee6pJn8TT6NkSBe8NBFYiS','18611693199','部门负责人','18611693199'),(1195,'2026-05-21 14:21:21.000000','2026-05-21 14:21:21.000000','产业金融业务部',_binary '','张  评','$2b$12$JUMK6UC7OdG5CT1K9HLGwe2mwAxxULSX4ixrZJRlqLTJ9rFC0IJ4u','15810712295','资深经理','15810712295'),(1196,'2026-05-21 14:21:21.000000','2026-05-21 14:21:21.000000','产业金融业务部',_binary '','徐晨钧','$2b$12$O9ravrnJaAKc5v74oeKYi.r3HeKRvdgeo7nY8en8u0Dj2Lity2xAS','18519019170','高级经理','18519019170'),(1197,'2026-05-21 14:21:22.000000','2026-05-21 14:21:22.000000','产业金融业务部',_binary '','南昌庆','$2b$12$9uz8rMemOKxzxNy.swo6vuxHs18Oibs9ZvNj5e0Sw97N0q.NSyccu','18513503203','高级经理','18513503203'),(1198,'2026-05-21 14:21:22.000000','2026-05-21 14:21:22.000000','产业金融业务部',_binary '','王鲜','$2b$12$XN9gpSZrb8gcZyslxOa5IusUNi9hjIEIdnWzkehu1qCbwV/2HSzSq','18811539227','经理','18811539227'),(1199,'2026-05-21 14:21:22.000000','2026-05-21 14:21:22.000000','产业金融业务部',_binary '','赵颖','$2b$12$zi7z7Oiyctdm1Stta6BU1ea5aOarsQsRJOA.cF3iZeY2XU5lnlUHW','18210029026','经理','18210029026'),(1200,'2026-05-21 14:21:22.000000','2026-05-21 14:21:22.000000','产业金融业务部',_binary '','郝玮','$2b$12$60EM6DnqZDI8qX1uAw7FSe0u7bzFgkeVMk3YgXwo3E9/AsBDdlL7a','15620622670','经理','15620622670'),(1201,'2026-05-21 14:21:23.000000','2026-05-21 14:21:23.000000','产业金融业务部',_binary '','刘梦麒','$2b$12$E1BD3AC.3j9pQc.ShidXQuGTTT99bp1/ULf7qHNz67HDho.R80Lhm','13581659862','经理','13581659862'),(1202,'2026-05-21 14:21:23.000000','2026-05-21 14:21:23.000000','产业金融业务部',_binary '','王良实','$2b$12$C./zzFji/Jt2rwgIHtEA1.1/7CDiNZ9HDgQwAqPFQqndKsbKp0Kiu','13079229233','经理','13079229233'),(1203,'2026-05-21 14:21:23.000000','2026-05-21 14:21:23.000000','产业金融业务部',_binary '','杨瑞婷','$2b$12$mIcYYhGNLw7RE.3KH0eTx.vi7uudrTCyTX7HZm9b5GrsBrMGOiJmm','15830183530','助理','15830183530'),(1204,'2026-05-21 14:21:23.000000','2026-05-21 14:21:23.000000','产业金融业务部',_binary '','王雨时','$2b$12$alhOWR7M4Tz2dZS.S.xp1.KOyK3BzL2//UQq.Bahs8FSVN9vK0jkq','18612801146','助理','18612801146'),(1205,'2026-05-21 14:21:24.000000','2026-05-21 14:21:24.000000','航运业务部',_binary '','李  飞','$2b$12$q/MRDvzkBsmbJl6mEdOyHe95qhpyyEoW5XN0uTXhswUvwXV.KN89a','13636377323','部门负责人','13636377323'),(1206,'2026-05-21 14:21:24.000000','2026-05-21 14:21:24.000000','航运业务部',_binary '','黄  贤','$2b$12$YaMX0Lk6GTRDwQX7Gy634eK..KClPvZis7u9mhTYQAsJq0zLEBrKe','15810088716','总经理助理','15810088716'),(1207,'2026-05-21 14:21:24.000000','2026-05-21 14:21:24.000000','航运业务部',_binary '','杨苗帆','$2b$12$3dZYjHxk43Aef6LtH.3GkO3zgmhXddqOTrZsbzgx4Bc8K7xk/kEB2','13472810770','资深经理','13472810770'),(1208,'2026-05-21 14:21:24.000000','2026-05-21 14:21:24.000000','航运业务部',_binary '','童道平','$2b$12$Gg5.0Cv3lK0OdFGbzNlA.uoVsNCb/GEx4dVwWGMr21HwNdtgeWlD6','15806299115','资深经理','15806299115'),(1209,'2026-05-21 14:21:24.000000','2026-05-21 14:21:24.000000','航运业务部',_binary '','潘剑峰','$2b$12$Vh6HUl7vAdwKbCwwxW9t7u6cYkTqwZ4MjvhrNW8.hN0dOEsE25k5S','15000014173','高级经理','15000014173'),(1210,'2026-05-21 14:21:25.000000','2026-05-21 14:21:25.000000','航运业务部',_binary '','刘  文','$2b$12$49/SNqPsgs83KxHx2EujXu8NR4GW9pq4cqWvt4u5A3u0uq9KHjfc.','13821738491','高级经理','13821738491'),(1211,'2026-05-21 14:21:25.000000','2026-05-21 14:21:25.000000','航运业务部',_binary '','赵炳星','$2b$12$15GcngXsQ2BW4JM6BDzngOFCL6r0zu1l27UpeaSaXJKcRPk4nkoQe','15022373862','高级经理','15022373862'),(1212,'2026-05-21 14:21:25.000000','2026-05-21 14:21:25.000000','航运业务部',_binary '','滕  飞','$2b$12$1ZLvz6/trXHmGV6SU9Xce.H4nRVTA4VeXEO1n14NQEds18G34GrH2','15101151309','经理','15101151309'),(1213,'2026-05-21 14:21:25.000000','2026-05-21 14:21:25.000000','航运业务部',_binary '','王潇荻','$2b$12$830fq7AuP3bF6.6gWzY1P.XKYMP4DzQD1bAhio3lvygjk207kgUXq','15007194646','经理','15007194646'),(1214,'2026-05-21 14:21:26.000000','2026-05-21 14:21:26.000000','航运业务部',_binary '','彭  宁','$2b$12$QApmRa2hEwB1vN97VPiGIunLP//33TdncNxjtxpO1.LNRtyk9S2Ty','13667088492','助理','13667088492'),(1215,'2026-05-21 14:21:26.000000','2026-05-21 14:21:26.000000','航运业务部',_binary '','卞振宇','$2b$12$ydM1cTy/GdtxAnRhlnPQ5OpTXJHWGhpfvExvkcVQaiA1VPKR5jCoi','13022100661','助理','13022100661'),(1216,'2026-05-21 14:21:26.000000','2026-05-21 14:21:26.000000','航运业务部',_binary '','王伟卓','$2b$12$lhYS3HjEQngN06qDlL/CEem9jB7FdY8GyzsSEWo0YDDy3U29b5RSi','18704505151','助理','18704505151'),(1217,'2026-05-21 14:21:26.000000','2026-05-21 14:21:26.000000','航运业务部',_binary '','武昊东','$2b$12$QCmsRKnrZibVexWZaHPwvOGq3iSe0pRTaqDkZ53MXAotw4GeTFZMS','13581129770','助理','13581129770'),(1218,'2026-05-21 14:21:27.000000','2026-05-21 14:21:27.000000','航运业务部',_binary '','余妍','$2b$12$CfBHRprceHGdcp5srAVwJurUX4ZF27odiKqBluHUEniuUERxhn9iq','18633739182','派遣','18633739182'),(1219,'2026-05-21 14:21:27.000000','2026-05-21 14:21:27.000000','绿色金融业务部',_binary '','马自润','$2b$12$NLNWA.HBdlDYeEK6huiERupPX3jwvW8F8FR0kJ8vbcZ8lntm5xGzO','13521611556','部门负责人','13521611556'),(1220,'2026-05-21 14:21:27.000000','2026-05-21 14:21:27.000000','绿色金融业务部',_binary '','姜一鸣','$2b$12$gzblC5ZntJr.aY4az5tQaOxfDWvXYX6A8WzaCkING0Fu0sEERTqES','13478208081','资深经理','13478208081'),(1221,'2026-05-21 14:21:27.000000','2026-05-21 14:21:27.000000','绿色金融业务部',_binary '','王  威','$2b$12$DSYINtnkteBUNBfr0c0w3exS8dENlJu.dag8cGISUVXcFb7zZHtMa','18601046939','资深经理','18601046939'),(1222,'2026-05-21 14:21:28.000000','2026-05-21 14:21:28.000000','绿色金融业务部',_binary '','朱修阳','$2b$12$InM6LMUQFz2WNa5hVfs0MeRDy7St2tx2k4mUx6Y6JoyrSrvAPSxyO','18610934938','资深经理','18610934938'),(1223,'2026-05-21 14:21:28.000000','2026-05-21 14:21:28.000000','绿色金融业务部',_binary '','魏子森','$2b$12$s9yOVlehkfBiBGYsS3tD4u40FGUGpoY/SlAuYHiTHT2Ze0luymTY6','13207508723','资深经理','13207508723'),(1224,'2026-05-21 14:21:28.000000','2026-05-21 14:21:28.000000','绿色金融业务部',_binary '','谭晓丹','$2b$12$3ZYCeCGnxqH5ZC8DyFqYXeyf5V.93lvk.ShZrw29nbBzUdWR2txGa','18801072857','高级经理','18801072857'),(1225,'2026-05-21 14:21:28.000000','2026-05-21 14:21:28.000000','绿色金融业务部',_binary '','杜金龙','$2b$12$2ly7PlrBz.ruTKnpb07AXOP25YDTMMg/ZGw4AAgWgxS62hBfdxlM2','13031029088','高级经理','13031029088'),(1226,'2026-05-21 14:21:28.000000','2026-05-21 14:21:28.000000','绿色金融业务部',_binary '','李太杰','$2b$12$18XFtRA/hON7oqyHDia09eEwkg.Ppb71XuQ6ZdKmkf8QqQbSVlsl2','15510031147','经理','15510031147'),(1227,'2026-05-21 14:21:29.000000','2026-05-21 14:21:29.000000','绿色金融业务部',_binary '','祝  帆','$2b$12$IXkWoCIgrt.CfSqrXeQ9BOlwSJnFGkOpFhOfI0C/CmavEMJ/i/OU.','13789809605','经理','13789809605'),(1228,'2026-05-21 14:21:29.000000','2026-05-21 14:21:29.000000','绿色金融业务部',_binary '','曹晓睿','$2b$12$vohO9HOL5HspSupV4UHQDezjzVt1iAby7.sE8LooJHGlyWUY4DRFO','13581593330','经理','13581593330'),(1229,'2026-05-21 14:21:29.000000','2026-05-21 14:21:29.000000','绿色金融业务部',_binary '','王士丰','$2b$12$leeBZORzFVc2xSIpyA9XH.PBe5AMEJvjAOmTv.lesIR7A.Fw4YqN2','19303929286','经理','19303929286'),(1230,'2026-05-21 14:21:29.000000','2026-05-21 14:21:29.000000','绿色金融业务部',_binary '','张子琪','$2b$12$IkkIV1Woh.xBPxsu2SuRX.zqpdUm92Wrfb38cOxpdAjhOYm3GoAMS','18403416130','助理','18403416130'),(1231,'2026-05-21 14:21:30.000000','2026-05-21 14:21:30.000000','绿色金融业务部',_binary '','田依萌','$2b$12$0afsbBjLtviGNX0USSqRL.SPgvCuqm0WzUk8oXce9v4CGX0SntpkS','15368062232','助理','15368062232'),(1232,'2026-05-21 14:21:30.000000','2026-05-21 14:21:30.000000','绿色金融业务部',_binary '','李佳霖','$2b$12$uQiV8FG2j7e3x6v12onbrONuENS5EhuZJlhOYL32qE/RCqGo3UI.W','15642106316','助理','15642106316'),(1234,'2026-05-21 14:21:30.000000','2026-05-21 14:21:30.000000','普惠金融业务部',_binary '','袁  伟','$2b$12$gMAPF0YTPcCshX.jQDbdduh7kjjFpszeIksHvnqorqXjCE4QG3gNG','18611052016','副总经理','18611052016'),(1235,'2026-05-21 14:21:30.000000','2026-05-21 14:21:30.000000','普惠金融业务部',_binary '','孙丹丹','$2b$12$ndVHyDvjJZ6nwHlKzSDXjeSOmUxCe/X0/2obU6UcEaQYmrUjZpRCW','18610273900','资深经理','18610273900'),(1236,'2026-05-21 14:21:31.000000','2026-05-21 14:21:31.000000','普惠金融业务部',_binary '','王淼鑫','$2b$12$uCSzfrFPXM4JGQvzzlLAremKCKV4wIxS8IpWqeg0Vet8Dm/nMKIzG','15500046933','资深经理','15500046933'),(1237,'2026-05-21 14:21:31.000000','2026-05-21 14:21:31.000000','普惠金融业务部',_binary '','陈太阳','$2b$12$NS64V37rj/XhHoGcAZ4wXOgjZ8nO8NTnKZe7212DGgJojiUrXhd3S','18201064030','高级经理','18201064030'),(1238,'2026-05-21 14:21:31.000000','2026-05-21 14:21:31.000000','普惠金融业务部',_binary '','刘  娜','$2b$12$5nz/HfpbwERi5JLEYcWpK.A4cQjSFhzg9dZ9qnk2QTFWtwPyhZ9nm','13811966545','高级经理','13811966545'),(1239,'2026-05-21 14:21:31.000000','2026-05-21 14:21:31.000000','普惠金融业务部',_binary '','孙华超','$2b$12$FmLOJAbMJ4eEQ7Yh2NBb/ugBYH4wvjQ8y89AYpFPm2nmuiUKt4a.2','13810570367','高级经理','13810570367'),(1240,'2026-05-21 14:21:31.000000','2026-05-21 14:21:31.000000','普惠金融业务部',_binary '','沈雨珂','$2b$12$h8enKWfyEYJ.26aKAqoVT.PwqoEWELKi4xlFnrzWKS0KONlE7Zufe','18701092853','高级经理','18701092853'),(1241,'2026-05-21 14:21:32.000000','2026-05-21 14:21:32.000000','普惠金融业务部',_binary '','徐  进','$2b$12$v5/SBOZbN6UqhzKbufBSY.BYftaUS.57V6aKXDa/eUaqIKoYbA7gu','15600604928','高级经理','15600604928'),(1242,'2026-05-21 14:21:32.000000','2026-05-21 14:21:32.000000','普惠金融业务部',_binary '','朱伟根','$2b$12$i3vs5EqqazttZPNqzqBni.k3fNAH9Uc42C.rwvO1QMSiE1kgUf3hO','15160492508','高级经理','15160492508'),(1243,'2026-05-21 14:21:32.000000','2026-05-21 14:21:32.000000','普惠金融业务部',_binary '','龚  彬','$2b$12$LqcDPydKp.qFaWXzhXTno.t/cXW/SAMK07DBmFwuwH9WQRgSqDlVG','18601998754','经理','18601998754'),(1244,'2026-05-21 14:21:32.000000','2026-05-21 14:21:32.000000','普惠金融业务部',_binary '','魏丽晴','$2b$12$TVE4xwqPbtyw4h3VwkTk6OxfTG99owgKXd7h5d7k1pc1jECKteWLy','13811888327','经理','13811888327'),(1245,'2026-05-21 14:21:33.000000','2026-05-21 14:21:33.000000','普惠金融业务部',_binary '','童一丁','$2b$12$dtG6PQF/3yQUniISEajTFei8CDBn7klCi9BBHsgoqWc4e6A9vMOGu','13102095015','经理','13102095015'),(1246,'2026-05-21 14:21:33.000000','2026-05-21 14:21:33.000000','普惠金融业务部',_binary '','陈霂之','$2b$12$W6pXW2GEiCa8BaNgHVfKaOZvO3NNpKtmsvPKVZJebZQo2hF71ZCkC','15911036598','经理','15911036598'),(1247,'2026-05-21 14:21:33.000000','2026-05-21 14:21:33.000000','普惠金融业务部',_binary '','王雨绮','$2b$12$N8z3hjvHJ41dhlsBQg1EhenHieCm5xYLHJHPdTUsygkddF/eY2rku','13120159488','经理','13120159488'),(1248,'2026-05-21 14:21:33.000000','2026-05-21 14:21:33.000000','普惠金融业务部',_binary '','高梦蝶','$2b$12$2tIiJRpm43jphf7BOWOVVOk4KtDcC0iwwHN/5hwIWrNIaaeuxBFoW','18811730546','经理','18811730546'),(1249,'2026-05-21 14:21:34.000000','2026-05-21 14:21:34.000000','普惠金融业务部',_binary '','聂和禹','$2b$12$XoTMnwHwy.SJwqbJ/SI9juvOaQNWfYupFpDSYcOSLF0.jB1mOQZTS','18613818983','助理','18613818983'),(1250,'2026-05-21 14:21:34.000000','2026-05-21 14:21:34.000000','普惠金融业务部',_binary '','曹  颖','$2b$12$8hBAnQOzmSlLpmQC0S.9Ju4clQL6Hfc8izDr/uD013e1/dFVqc9se','13520287656','助理','13520287656'),(1251,'2026-05-21 14:21:34.000000','2026-05-21 14:21:34.000000','普惠金融业务部',_binary '','王俊微','$2b$12$i26TeuJixfEkRcg2k9qQjO1jR5wRuFz96bbMDD/6lwBbs7Wm1Yt3i','15933538065','助理','15933538065'),(1252,'2026-05-21 14:21:34.000000','2026-05-21 14:21:34.000000','普惠金融业务部',_binary '','董  雯','$2b$12$tPPvNSMBUqAH5t6ZLG0g5O7/DaDYGMgv58BmE7092nDdU1QKgONQS','15110096769','派遣','15110096769'),(1253,'2026-05-21 14:21:35.000000','2026-05-21 14:21:35.000000','普惠金融业务部',_binary '','董芯宇','$2b$12$IsJrAp2csA1q0mIYBxKeKeNNOolS2xE2w6W8exFlRBbQVyWXR3882','17630962357','派遣','17630962357'),(1254,'2026-05-21 14:21:35.000000','2026-05-21 14:21:35.000000','京津冀业务中心',_binary '','张  铭','$2b$12$0jglZlhgangHDDEW8vQGD.S7VHoRi4k6H7J3XpObUWHo2T7K4bbwm','13910578656','部门负责人','13910578656'),(1255,'2026-05-21 14:21:35.000000','2026-05-21 14:21:35.000000','京津冀业务中心',_binary '','赵若汀','$2b$12$T7ehu/SBwRAvba5Q1m7SKep4q.ffv5r0LjN0HLIBZUGQl9Yhv0VjK','13511083199','资深经理','13511083199'),(1256,'2026-05-21 14:21:35.000000','2026-05-21 14:21:35.000000','京津冀业务中心',_binary '','邢润敏','$2b$12$PbXofPYfw/owgUessVrp4OLDJHQP.JIokP5u79ne.L7ypfjw6IvYm','13051779978','高级经理','13051779978'),(1257,'2026-05-21 14:21:35.000000','2026-05-21 14:21:35.000000','京津冀业务中心',_binary '','贾  航','$2b$12$veWmQldBbO0VaDfXA2UqNerg.TbYuf63Oh3Vl0dgIwKjMKXdq9YDy','18633280013','经理','18633280013'),(1258,'2026-05-21 14:21:36.000000','2026-05-21 14:21:36.000000','京津冀业务中心',_binary '','陈祎明','$2b$12$zSKtbuOpxEYtz5dpp.KP7e3.IyCkKpzrBoDX9DyiAnRL6Xt9PYUsC','13439855513','经理','13439855513'),(1259,'2026-05-21 14:21:36.000000','2026-05-21 14:21:36.000000','京津冀业务中心',_binary '','马欣晨','$2b$12$EixNVMiE4pJfGEAxT8gjdeTIXHlxu34TA7Me7nu6MKTUfKXqHIALy','13260755192','助理','13260755192'),(1260,'2026-05-21 14:21:36.000000','2026-05-21 14:21:36.000000','京津冀业务中心',_binary '','陆沛铮','$2b$12$sZxWnlmYeUCsY/A5Ezu5BeHGrdY5PrY4b7Okys50yyrGvDe63yxQS','13718876692','助理','13718876692'),(1261,'2026-05-21 14:21:36.000000','2026-05-21 14:21:36.000000','长三角业务中心',_binary '','李忻哲','$2b$12$Pk6SsdrRQB4A5IYJA6MWuu5KqWX8a7JBr2u7LsPfRbLyM56IQRd8m','18669054222','部门负责人','18669054222'),(1262,'2026-05-21 14:21:37.000000','2026-05-21 14:21:37.000000','长三角业务中心',_binary '','朱颖瑾','$2b$12$wyhlRV4aIErnK9P1Rx0MpujkLvjhn1ldsDFMl7ZUceVzxeIF6ZckO','18601192275','副总经理','18601192275'),(1263,'2026-05-21 14:21:37.000000','2026-05-21 14:21:37.000000','长三角业务中心',_binary '','周学晨','$2b$12$XOC2wqwIVvrtlrx4IlIHi.ivxZxmig5ZAZgr3jY.SqoeslavXBToG','13552288075','高级经理','13552288075'),(1264,'2026-05-21 14:21:37.000000','2026-05-21 14:21:37.000000','长三角业务中心',_binary '','王子鉴','$2b$12$fafmD6/dml88h3Q1zR3MaO9lURGgG2g/wGVqtaaY0/P1Jg8GmByM2','18511200388','经理','18511200388'),(1265,'2026-05-21 14:21:37.000000','2026-05-21 14:21:37.000000','长三角业务中心',_binary '','张凯杰','$2b$12$wPIv7p.5cLPmB5vrioJhMu7pwNI38uKYdmnHWsJbkWCigFksQT40K','18310707102','经理','18310707102'),(1266,'2026-05-21 14:21:38.000000','2026-05-21 14:21:38.000000','长三角业务中心',_binary '','邹子臻','$2b$12$aMIMgyy4CkZgNZktEGPtmudoG5os9jvoDZiUkY0EuqG.9tqtAyj1e','13262990171','助理','13262990171'),(1267,'2026-05-21 14:21:38.000000','2026-05-21 14:21:38.000000','长三角业务中心',_binary '','赵明轩','$2b$12$TTf2Wqeb8uM75R8AIogxROAt/1kmxtEDaDDP9.yL.xiYxfB99MC/S','13393220698','助理','13393220698'),(1268,'2026-05-21 14:21:38.000000','2026-05-21 14:21:38.000000','长三角业务中心',_binary '','吴志宝','$2b$12$PWToDn6NRzfHFomGWhNSKebNhejifYAHfvawf5RyzfsLBzdVsguxa','13365698135','助理','13365698135'),(1269,'2026-05-21 14:21:38.000000','2026-05-21 14:21:38.000000','长三角业务中心',_binary '','杨  杰','$2b$12$o8/nKQT48ovWDqMtC3VAqe.69qPjCYPpSeu.thod6/wxwaVmNknCW','19957729285','派遣','19957729285'),(1270,'2026-05-21 14:21:39.000000','2026-05-21 14:21:39.000000','大湾区业务中心',_binary '','杨  帆','$2b$12$gyho5a/cFN/qmus2FWsYM.2aGQyLqocl4inm9HT51T3fhYG9LD0Fi','15081243115','部门负责人','15081243115'),(1271,'2026-05-21 14:21:39.000000','2026-05-21 14:21:39.000000','大湾区业务中心',_binary '','刘建怀','$2b$12$Whlg4jvtYNjs/.WxOL2B1.Ea18Vjd3gTHkrnBnQjw/UNHd7GcaPxC','18510087358','副总经理','18510087358'),(1272,'2026-05-21 14:21:39.000000','2026-05-21 14:21:39.000000','大湾区业务中心',_binary '','彭  程','$2b$12$bDzwvcPXftSrEaJVgM9i6OfaRFjaJfQMihDOPmvGAgPwG7VQY6Co.','15810895577','高级经理','15810895577'),(1273,'2026-05-21 14:21:39.000000','2026-05-21 14:21:39.000000','大湾区业务中心',_binary '','贾越超','$2b$12$FrEorkmjL65LfwCQnXH1A.yeoRiwkcMUDnbaAK22RIU52t3YUjnp6','13803280937','高级经理','13803280937'),(1274,'2026-05-21 14:21:39.000000','2026-05-21 14:21:39.000000','大湾区业务中心',_binary '','邢可成','$2b$12$NYACznLrnfk8Hqap7/mVw.e57mcq1MryfYKWp533DLXJpBY241Joq','18911144247','高级经理','18911144247'),(1275,'2026-05-21 14:21:40.000000','2026-05-21 14:21:40.000000','大湾区业务中心',_binary '','林腾浩','$2b$12$ckTo3l5TLs.CoRdwaFPDx.63yoQ48S/faBnqdBj1zm6ptlaTvEyoi','13501555162','经理','13501555162'),(1276,'2026-05-21 14:21:40.000000','2026-05-21 14:21:40.000000','大湾区业务中心',_binary '','邓翔瑞','$2b$12$S6styESFYwhiTsDRjLu4A.M2siMaiQiYDmuPjCbjrZnYrBE0xhyNS','18927628086','助理','18927628086'),(1277,'2026-05-21 14:21:40.000000','2026-05-21 14:21:40.000000','大湾区业务中心',_binary '','杨梓铭','$2b$12$xxZWdltpJt17mOy27WXxauMtWxrHXouGvaWThHX1VgMuiBYDa5Iga','15262073284','助理','15262073284'),(1278,'2026-05-21 14:21:40.000000','2026-05-21 14:21:40.000000','大湾区业务中心',_binary '','李增辉','$2b$12$mRIHL2kAIjFbrR6p6ZN7fupqMiAVhN6BqjIXKBFHRrIY7oBFwM.82','18813979633','派遣','18813979633'),(1279,'2026-05-21 14:21:41.000000','2026-05-21 14:21:41.000000','西南区域业务中心',_binary '','丁  力','$2b$12$hK5JBa7LfFD4gBI1Qxa/POCQeDAHlJ6hXcbE5Pbf85WnCtc5qrkNG','13708432973','部门负责人','13708432973'),(1280,'2026-05-21 14:21:41.000000','2026-05-21 14:21:41.000000','西南区域业务中心',_binary '','姚  杰','$2b$12$7pZmwkgp8O5qzy4mqRu7N.f8u9cz/7V.vpJkWZCBCvdKKHUlOBnhO','13888586034','资深经理','13888586034'),(1281,'2026-05-21 14:21:41.000000','2026-05-21 14:21:41.000000','西南区域业务中心',_binary '','张  骏','$2b$12$n.PROl9E6sJc1tppBLaWhOLWix5VChWIodxtAqNi9Rd8uYVWn.fo6','18687811119','资深经理','18687811119'),(1282,'2026-05-21 14:21:41.000000','2026-05-21 14:21:41.000000','西南区域业务中心',_binary '','刘  娜','$2b$12$2kZmfJ0XwhJMNzG0uhKzk.lZOuin/tcSVrcedZHQt6IAFrpqcuFIC','13769560830','资深经理','13769560830'),(1283,'2026-05-21 14:21:42.000000','2026-05-21 14:21:42.000000','西南区域业务中心',_binary '','伍中津','$2b$12$3Fdg2n739KwJkJ1dAsEIuuigN0suVKJzKx5fB6N.N9y9EDX5pWaXm','13700602590','高级经理','13700602590'),(1284,'2026-05-21 14:21:42.000000','2026-05-21 14:21:42.000000','西南区域业务中心',_binary '','黄畅翔','$2b$12$ayDstljHC8ct7dA.VylVNOSsQSuSPoA2EqOKG.TAsRzC2ClOUwInq','15708427989','经理','15708427989'),(1285,'2026-05-21 14:21:42.000000','2026-05-21 14:21:42.000000','西南区域业务中心',_binary '','林 琳','$2b$12$.CluFqoeRVvzY3BnsSDxz.E5Z1pOmk2z03W2rMGizx8hwQer9q.q6','13987167126','经理','13987167126'),(1286,'2026-05-21 14:21:42.000000','2026-05-21 14:21:42.000000','西南区域业务中心',_binary '','吴  锰','$2b$12$L23gYbYW28S3cuMWWDTVxOlAdUTUlP5WMAbEm1U6Nz77IrPSVW0f2','15877936018','信息技术工程师','15877936018'),(1287,'2026-05-21 14:21:43.000000','2026-05-21 14:21:43.000000','西南区域业务中心',_binary '','杨文渠','$2b$12$joYtnIvepB4e3dhD/oyBy.Vv.ypzXxipAY08BVAyUDu5pLpDYiazS','17877780161','助理','17877780161'),(1288,'2026-05-21 14:21:43.000000','2026-05-21 14:21:43.000000','西南区域业务中心',_binary '','徐曼芸','$2b$12$ZsLMlmGmUyqBi/lXasFZo.D2EWNKXY3C7yHMZgbE8w0o2ZWLWa83C','18468228948','助理','18468228948'),(1289,'2026-05-21 14:21:43.000000','2026-05-21 14:21:43.000000','西南区域业务中心',_binary '','李思蓉','$2b$12$6KD.9MKEIGCrMhT4FJ5iIOb4npab7WT.YT2ePB2z0G/NttlsgKf8e','15187781521','助理','15187781521'),(1290,'2026-05-21 14:21:43.000000','2026-05-21 14:21:43.000000','西南区域业务中心',_binary '','梁  涛','$2b$12$DqioNjVMQxS9paNFYiRDOOXo11wkqZpVx8paYKgT3403I1uwdHJbu','13708414880\n','助理','13708414880'),(1291,'2026-05-21 14:21:43.000000','2026-05-21 14:21:43.000000','中部区域业务中心',_binary '','龙  誉','$2b$12$WjYdhiDFwhZI.DIxpzY5Tu0ZDawBcAbh3XeoTscF/sRL6pOXdSZAq','13691033965','部门负责人','13691033965'),(1292,'2026-05-21 14:21:44.000000','2026-05-21 14:21:44.000000','中部区域业务中心',_binary '','刘闻潇','$2b$12$3ju5sXrpZiOazrkQserq..ciDJQhauc17lpACsoif8XlxJQiGyA7W','15562698000','高级经理','15562698000'),(1293,'2026-05-21 14:21:44.000000','2026-05-21 14:21:44.000000','中部区域业务中心',_binary '','孟德飞','$2b$12$d03IA3nDKmjmwzJGfN8NCO4knmO.a79ij/T.M14gqyTlsi75TvTC6','15198957379','高级经理','15198957379'),(1294,'2026-05-21 14:21:44.000000','2026-05-21 14:21:44.000000','中部区域业务中心',_binary '','罗冠华','$2b$12$wkIuj/4SCKg15SaxauRf5uOuixaXRqy4smbpXac3tjGcq5XyH5zPm','13313122787','经理','13313122787');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
SET @@SESSION.SQL_LOG_BIN = @MYSQLDUMP_TEMP_LOG_BIN;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-22 19:45:38
