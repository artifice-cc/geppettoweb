-- MySQL dump 10.13  Distrib 5.5.31, for debian-linux-gnu (x86_64)
--
-- Host: localhost    Database: sisyphus_retrospect
-- ------------------------------------------------------
-- Server version	5.5.31-0ubuntu0.13.04.1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `analyses`
--

DROP TABLE IF EXISTS `analyses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `analyses` (
  `analysisid` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `problems` varchar(1024) DEFAULT NULL,
  `resultstype` enum('non-comparative','comparative') NOT NULL,
  `code` text NOT NULL,
  `caption` text,
  PRIMARY KEY (`analysisid`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `graphs`
--

DROP TABLE IF EXISTS `graphs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `graphs` (
  `graphid` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `problems` varchar(1024) NOT NULL,
  `resultstype` enum('comparative','non-comparative') NOT NULL,
  `code` text NOT NULL,
  `caption` text,
  `width` float DEFAULT NULL,
  `height` float DEFAULT NULL,
  PRIMARY KEY (`graphid`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `run_analyses`
--

DROP TABLE IF EXISTS `run_analyses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `run_analyses` (
  `runanalysisid` int(11) NOT NULL AUTO_INCREMENT,
  `runid` int(11) NOT NULL,
  `analysisid` int(11) NOT NULL,
  PRIMARY KEY (`runanalysisid`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `run_graphs`
--

DROP TABLE IF EXISTS `run_graphs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `run_graphs` (
  `rungraphid` int(11) NOT NULL AUTO_INCREMENT,
  `runid` int(11) NOT NULL,
  `graphid` int(11) NOT NULL,
  PRIMARY KEY (`rungraphid`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `table_fields`
--

DROP TABLE IF EXISTS `table_fields`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `table_fields` (
  `tfid` int(11) NOT NULL AUTO_INCREMENT,
  `runid` int(11) NOT NULL,
  `field` varchar(255) NOT NULL,
  `tabletype` enum('comparative','non-comparative','paired') NOT NULL,
  PRIMARY KEY (`tfid`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `template_analyses`
--

DROP TABLE IF EXISTS `template_analyses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `template_analyses` (
  `templateid` int(11) NOT NULL AUTO_INCREMENT,
  `runid` int(11) NOT NULL,
  `name` varchar(255) NOT NULL,
  `caption` text,
  `template` varchar(255) NOT NULL,
  `xfield` varchar(255) DEFAULT NULL,
  `yfield` varchar(255) DEFAULT NULL,
  `code` text,
  PRIMARY KEY (`templateid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `template_graphs`
--

DROP TABLE IF EXISTS `template_graphs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `template_graphs` (
  `templateid` int(11) NOT NULL AUTO_INCREMENT,
  `runid` int(11) NOT NULL,
  `name` varchar(255) NOT NULL,
  `caption` text,
  `width` float NOT NULL DEFAULT '7',
  `height` float NOT NULL DEFAULT '4',
  `template` varchar(255) NOT NULL,
  `xfield` varchar(255) DEFAULT NULL,
  `xfactor` varchar(255) DEFAULT NULL,
  `xlabel` varchar(1024) DEFAULT NULL,
  `yfield` varchar(255) DEFAULT NULL,
  `ylabel` varchar(1024) DEFAULT NULL,
  `fill` varchar(255) DEFAULT NULL,
  `color` varchar(255) DEFAULT NULL,
  `linetype` varchar(255) DEFAULT NULL,
  `shape` varchar(255) DEFAULT NULL,
  `facethoriz` varchar(255) DEFAULT NULL,
  `facetvert` varchar(255) DEFAULT NULL,
  `code` text,
  PRIMARY KEY (`templateid`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2013-06-01 13:41:34
