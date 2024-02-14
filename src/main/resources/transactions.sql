-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------
-- Schema mydb
-- -----------------------------------------------------
-- -----------------------------------------------------
-- Schema transactions
-- -----------------------------------------------------

-- -----------------------------------------------------
-- Schema transactions
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `transactions` DEFAULT CHARACTER SET utf8mb3 ;
USE `transactions` ;

-- -----------------------------------------------------
-- Table `transactions`.`currency`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `transactions`.`currency` (
  `id` INT NOT NULL,
  `name` VARCHAR(45) NULL DEFAULT NULL,
  `average_exchange` DOUBLE NULL DEFAULT NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;


-- -----------------------------------------------------
-- Table `transactions`.`client`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `transactions`.`client` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `pib` VARCHAR(45) NOT NULL,
  `phone_number` VARCHAR(45) NULL DEFAULT NULL,
  `telegram` VARCHAR(45) NULL DEFAULT NULL,
  `color` VARCHAR(100) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `id_UNIQUE` (`id` ASC) VISIBLE)
ENGINE = InnoDB
AUTO_INCREMENT = 448
DEFAULT CHARACTER SET = utf8mb3;


-- -----------------------------------------------------
-- Table `transactions`.`account`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `transactions`.`account` (
  `client_id` INT NOT NULL,
  `currency_id` INT NOT NULL,
  `amount` DOUBLE NULL DEFAULT NULL,
  `amount_color` VARCHAR(100) NULL DEFAULT NULL,
  PRIMARY KEY (`client_id`, `currency_id`),
  INDEX `fk_client_has_currency_client1_idx` (`client_id` ASC) VISIBLE,
  INDEX `fk_account_currency1_idx` (`currency_id` ASC) VISIBLE,
  CONSTRAINT `fk_account_currency1`
    FOREIGN KEY (`currency_id`)
    REFERENCES `transactions`.`currency` (`id`),
  CONSTRAINT `fk_client_has_currency_client1`
    FOREIGN KEY (`client_id`)
    REFERENCES `transactions`.`client` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;


-- -----------------------------------------------------
-- Table `transactions`.`user`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `transactions`.`user` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `login` VARCHAR(45) NOT NULL,
  `password` VARCHAR(256) NOT NULL,
  `role_id` INT NOT NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB
AUTO_INCREMENT = 10
DEFAULT CHARACTER SET = utf8mb3;


-- -----------------------------------------------------
-- Table `transactions`.`transaction`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `transactions`.`transaction` (
  `id` INT NOT NULL,
  `date` DATETIME NOT NULL,
  `client_id` INT NOT NULL,
  `currency_id` INT NOT NULL,
  `rate` DOUBLE NOT NULL,
  `balance` DOUBLE NOT NULL,
  `commission` DOUBLE NULL DEFAULT NULL,
  `amount` DOUBLE NOT NULL COMMENT 'HRN',
  `transportation` DOUBLE NULL DEFAULT NULL,
  `comment_color` VARCHAR(100) NULL DEFAULT NULL,
  `input_color` VARCHAR(100) NULL DEFAULT NULL,
  `output_color` VARCHAR(100) NULL DEFAULT NULL,
  `comment` VARCHAR(200) NULL DEFAULT NULL,
  `user_id` INT NOT NULL,
  `tarif_color` VARCHAR(100) NULL DEFAULT NULL,
  `commission_color` VARCHAR(100) NULL DEFAULT NULL,
  `rate_color` VARCHAR(100) NULL DEFAULT NULL,
  `transportation_color` VARCHAR(100) NULL DEFAULT NULL,
  `amount_color` VARCHAR(100) NULL DEFAULT NULL,
  `balance_color` VARCHAR(100) NULL DEFAULT NULL,
  PRIMARY KEY (`id`, `client_id`, `currency_id`, `user_id`),
  INDEX `fk_client_has_currency_has_client_has_currency_client_has_c_idx1` (`client_id` ASC, `currency_id` ASC) VISIBLE,
  INDEX `fk_transaction_user1_idx` (`user_id` ASC) VISIBLE,
  CONSTRAINT `fk_client_has_currency_has_client_has_currency_client_has_cur1`
    FOREIGN KEY (`client_id`)
    REFERENCES `transactions`.`account` (`client_id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_transaction_user1`
    FOREIGN KEY (`user_id`)
    REFERENCES `transactions`.`user` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
