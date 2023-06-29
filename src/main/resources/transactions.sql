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
  PRIMARY KEY (`id`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `transactions`.`account`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `transactions`.`account` ;

CREATE TABLE IF NOT EXISTS `transactions`.`account` (
  `client_id` INT NOT NULL,
  `amount` DOUBLE NULL,
  `currency_id` INT NOT NULL,
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
  PRIMARY KEY (`id`, `client_id`, `currency_id`),
  INDEX `fk_client_has_currency_has_client_has_currency_client_has_c_idx1` (`client_id` ASC, `currency_id` ASC) VISIBLE,
  CONSTRAINT `fk_client_has_currency_has_client_has_currency_client_has_cur1`
    FOREIGN KEY (`client_id`)
    REFERENCES `transactions`.`account` (`client_id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
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
INSERT INTO `transactions`.`currency` (`id`, `name`) VALUES (980, 'UAH');
INSERT INTO `transactions`.`currency` (`id`, `name`) VALUES (840, 'USD');
INSERT INTO `transactions`.`currency` (`id`, `name`) VALUES (978, 'EUR');
INSERT INTO `transactions`.`currency` (`id`, `name`) VALUES (985, 'PLN');

COMMIT;


-- -----------------------------------------------------
-- Data for table `transactions`.`account`
-- -----------------------------------------------------
START TRANSACTION;
USE `transactions`;
INSERT INTO `transactions`.`account` (`client_id`, `amount`, `currency_id`) VALUES (1, 100.01, 980);
INSERT INTO `transactions`.`account` (`client_id`, `amount`, `currency_id`) VALUES (1, 120.45, 840);
INSERT INTO `transactions`.`account` (`client_id`, `amount`, `currency_id`) VALUES (2, 500, 980);
INSERT INTO `transactions`.`account` (`client_id`, `amount`, `currency_id`) VALUES (3, 70.68, 980);
INSERT INTO `transactions`.`account` (`client_id`, `amount`, `currency_id`) VALUES (3, 17.52, 840);
INSERT INTO `transactions`.`account` (`client_id`, `amount`, `currency_id`) VALUES (4, 732, 840);
INSERT INTO `transactions`.`account` (`client_id`, `amount`, `currency_id`) VALUES (5, 1000, 980);
INSERT INTO `transactions`.`account` (`client_id`, `amount`, `currency_id`) VALUES (6, 5000, 980);

COMMIT;

