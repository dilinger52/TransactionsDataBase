-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------
-- Schema transactions
-- -----------------------------------------------------
DROP SCHEMA IF EXISTS `transactions` ;

-- -----------------------------------------------------
-- Schema transactions
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `transactions` DEFAULT CHARACTER SET utf8 ;
USE `transactions` ;

-- -----------------------------------------------------
-- Table `transactions`.`client`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `transactions`.`client` ;

CREATE TABLE IF NOT EXISTS `transactions`.`client` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `pib` VARCHAR(45) NOT NULL,
  `phone_number` VARCHAR(45) NULL,
  `telegram` VARCHAR(45) NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `id_UNIQUE` (`id` ASC) VISIBLE)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `transactions`.`currency`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `transactions`.`currency` ;

CREATE TABLE IF NOT EXISTS `transactions`.`currency` (
  `id` INT NOT NULL,
  `name` VARCHAR(45) NULL,
  `average_exchange` DOUBLE NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `transactions`.`account`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `transactions`.`account` ;

CREATE TABLE IF NOT EXISTS `transactions`.`account` (
  `client_id` INT NOT NULL,
  `currency_id` INT NOT NULL,
  `amount` DOUBLE NULL,
  PRIMARY KEY (`client_id`, `currency_id`),
  INDEX `fk_client_has_currency_client1_idx` (`client_id` ASC) VISIBLE,
  INDEX `fk_account_currency1_idx` (`currency_id` ASC) VISIBLE,
  CONSTRAINT `fk_client_has_currency_client1`
    FOREIGN KEY (`client_id`)
    REFERENCES `transactions`.`client` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_account_currency1`
    FOREIGN KEY (`currency_id`)
    REFERENCES `transactions`.`currency` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `transactions`.`user`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `transactions`.`user` ;

CREATE TABLE IF NOT EXISTS `transactions`.`user` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `login` VARCHAR(45) NOT NULL,
  `password` VARCHAR(256) NOT NULL,
  `role_id` INT NOT NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `transactions`.`transaction`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `transactions`.`transaction` ;

CREATE TABLE IF NOT EXISTS `transactions`.`transaction` (
  `id` INT NOT NULL,
  `date` DATETIME NOT NULL,
  `client_id` INT NOT NULL,
  `currency_id` INT NOT NULL,
  `rate` DOUBLE NOT NULL,
  `balance` DOUBLE NOT NULL,
  `commission` DOUBLE NULL,
  `amount` DOUBLE NOT NULL COMMENT 'HRN',
  `transportation` DOUBLE NULL,
  `pib_color` VARCHAR(45) NULL,
  `amount_color` VARCHAR(45) NULL,
  `balance_color` VARCHAR(45) NULL,
  `comment` VARCHAR(200) NULL,
  `user_id` INT NOT NULL,
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
    REFERENCES `transactions`.`user` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;

-- -----------------------------------------------------
-- Data for table `transactions`.`client`
-- -----------------------------------------------------
START TRANSACTION;
USE `transactions`;
INSERT INTO `transactions`.`client` (`id`, `pib`, `phone_number`, `telegram`) VALUES (1, 'Ivan', '+38 (056) 329-17-12', '@ivan');
INSERT INTO `transactions`.`client` (`id`, `pib`, `phone_number`, `telegram`) VALUES (2, 'Vasil', '+38 (068) 111-11-11', '@vasil');
INSERT INTO `transactions`.`client` (`id`, `pib`, `phone_number`, `telegram`) VALUES (3, 'Egor', '+38 (097) 678-23-12', '@egor');
INSERT INTO `transactions`.`client` (`id`, `pib`, `phone_number`, `telegram`) VALUES (4, 'Pavel', '+38 (044) 514-74-18', '@pavel');
INSERT INTO `transactions`.`client` (`id`, `pib`, `phone_number`, `telegram`) VALUES (5, 'Artem', '+38 (096) 552-64-58', '@artem');
INSERT INTO `transactions`.`client` (`id`, `pib`, `phone_number`, `telegram`) VALUES (6, 'Sofia', '+38 (052) 968-68-68', '@sofia');

COMMIT;


-- -----------------------------------------------------
-- Data for table `transactions`.`currency`
-- -----------------------------------------------------
START TRANSACTION;
USE `transactions`;
INSERT INTO `transactions`.`currency` (`id`, `name`, `average_exchange`) VALUES (980, 'UAH', 1);
INSERT INTO `transactions`.`currency` (`id`, `name`, `average_exchange`) VALUES (840, 'USD', 36);
INSERT INTO `transactions`.`currency` (`id`, `name`, `average_exchange`) VALUES (978, 'EUR', 40);
INSERT INTO `transactions`.`currency` (`id`, `name`, `average_exchange`) VALUES (985, 'PLN', 9);

COMMIT;


-- -----------------------------------------------------
-- Data for table `transactions`.`account`
-- -----------------------------------------------------
START TRANSACTION;
USE `transactions`;
INSERT INTO `transactions`.`account` (`client_id`, `currency_id`, `amount`) VALUES (1, 980, 100.01);
INSERT INTO `transactions`.`account` (`client_id`, `currency_id`, `amount`) VALUES (1, 840, 120.45);
INSERT INTO `transactions`.`account` (`client_id`, `currency_id`, `amount`) VALUES (2, 980, 500);
INSERT INTO `transactions`.`account` (`client_id`, `currency_id`, `amount`) VALUES (3, 980, 70.68);
INSERT INTO `transactions`.`account` (`client_id`, `currency_id`, `amount`) VALUES (3, 840, 17.52);
INSERT INTO `transactions`.`account` (`client_id`, `currency_id`, `amount`) VALUES (4, 840, 732);
INSERT INTO `transactions`.`account` (`client_id`, `currency_id`, `amount`) VALUES (5, 980, 1000);
INSERT INTO `transactions`.`account` (`client_id`, `currency_id`, `amount`) VALUES (6, 980, 5000);

COMMIT;


-- -----------------------------------------------------
-- Data for table `transactions`.`user`
-- -----------------------------------------------------
START TRANSACTION;
USE `transactions`;
INSERT INTO `transactions`.`user` (`id`, `login`, `password`, `role_id`) VALUES (1, 'admin', '3329cb839124549c72911a7bbf3a2612432b2fe41d37fef2bc1033c4c88f612f', 1);

COMMIT;

