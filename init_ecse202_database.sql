SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL,ALLOW_INVALID_DATES';

-- -----------------------------------------------------
-- Schema ecse202
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `ecse202` DEFAULT CHARACTER SET utf8 ;
USE `ecse202` ;

-- -----------------------------------------------------
-- Table `ecse202`.`classlist`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `ecse202`.`classlist` ;

CREATE TABLE IF NOT EXISTS `ecse202`.`classlist` (
  `username` VARCHAR(40) NOT NULL,
  `fullname` VARCHAR(40) NOT NULL,
  `password` VARCHAR(40) NOT NULL,
  PRIMARY KEY (`username`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;

-- -----------------------------------------------------
-- Table `ecse202`.`assignments`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `ecse202`.`assignments` ;

CREATE TABLE IF NOT EXISTS `ecse202`.`assignments` (
  `username` VARCHAR(40) NOT NULL,
  `assignment` INT NOT NULL,
  `score` VARCHAR(10) NOT NULL,
  `failures` TINYTEXT NULL DEFAULT NULL,
  `stamp_created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`username`, `assignment`),
	CONSTRAINT `fk_assignments_username1`
	    FOREIGN KEY (`username`)
	    REFERENCES `ecse202`.`classlist` (`username`)
	    ON DELETE CASCADE
	    ON UPDATE NO ACTION)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;
