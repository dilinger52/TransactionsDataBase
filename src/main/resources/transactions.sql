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
  PRIMARY KEY (`id`),
  UNIQUE INDEX `id_UNIQUE` (`id` ASC) VISIBLE)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `transactions`.`currency`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `transactions`.`currency` ;

CREATE TABLE IF NOT EXISTS `transactions`.`currency` (
  `client_id` INT NOT NULL,
  `currency_id` INT NOT NULL,
  `name` VARCHAR(45) NOT NULL,
  `amount` DOUBLE NULL,
  PRIMARY KEY (`client_id`, `currency_id`),
  INDEX `fk_client_has_currency_client1_idx` (`client_id` ASC) VISIBLE,
  CONSTRAINT `fk_client_has_currency_client1`
    FOREIGN KEY (`client_id`)
    REFERENCES `transactions`.`client` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `transactions`.`transaction`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `transactions`.`transaction` ;

CREATE TABLE IF NOT EXISTS `transactions`.`transaction` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `client1_id` INT NOT NULL,
  `currency1_id` INT NOT NULL,
  `rate1` DECIMAL NOT NULL,
  `commission1` DECIMAL NULL,
  `amount1` DOUBLE NOT NULL COMMENT 'HRN',
  `client2_id` INT NULL,
  `currency2_id` INT NULL,
  `rate2` DECIMAL NULL,
  `commission2` DECIMAL NULL,
  `amount2` DOUBLE NULL COMMENT 'HRN',
  `client3_id` INT NULL,
  `currency3_id` INT NULL,
  `rate3` DECIMAL NULL,
  `commission3` DECIMAL NULL,
  `amount3` DOUBLE NULL COMMENT 'HRN',
  `client4_id` INT NULL,
  `currency4_id` INT NULL,
  `rate4` DECIMAL NULL,
  `commission4` DECIMAL NULL,
  `amount4` DOUBLE NULL COMMENT 'HRN',
  `client5_id` INT NULL,
  `currency5_id` INT NULL,
  `rate5` DECIMAL NULL,
  `commission5` DECIMAL NULL,
  `amount5` DOUBLE NULL COMMENT 'HRN',
  `client6_id` INT NULL,
  `currency6_id` INT NULL,
  `rate6` DECIMAL NULL,
  `commission6` DECIMAL NULL,
  `amount6` DOUBLE NULL COMMENT 'HRN',
  `balance1` DECIMAL NOT NULL,
  `balance2` DECIMAL NULL,
  `balance3` DECIMAL NULL,
  `balance4` DECIMAL NULL,
  `balance5` DECIMAL NULL,
  `balance6` DECIMAL NULL,
  PRIMARY KEY (`id`),
  INDEX `fk_client_has_currency_has_client_has_currency_client_has_c_idx` (`client2_id` ASC, `currency2_id` ASC) VISIBLE,
  INDEX `fk_client_has_currency_has_client_has_currency_client_has_c_idx1` (`client1_id` ASC, `currency1_id` ASC) VISIBLE,
  UNIQUE INDEX `id_UNIQUE` (`id` ASC) VISIBLE,
  INDEX `fk_transaction_currency1_idx` (`client3_id` ASC, `currency3_id` ASC) VISIBLE,
  INDEX `fk_transaction_currency2_idx` (`client4_id` ASC, `currency4_id` ASC) VISIBLE,
  INDEX `fk_transaction_currency3_idx` (`client5_id` ASC, `currency5_id` ASC) VISIBLE,
  INDEX `fk_transaction_currency4_idx` (`client6_id` ASC, `currency6_id` ASC) VISIBLE,
  CONSTRAINT `fk_client_has_currency_has_client_has_currency_client_has_cur1`
    FOREIGN KEY (`client1_id` , `currency1_id`)
    REFERENCES `transactions`.`currency` (`client_id` , `currency_id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_client_has_currency_has_client_has_currency_client_has_cur2`
    FOREIGN KEY (`client2_id` , `currency2_id`)
    REFERENCES `transactions`.`currency` (`client_id` , `currency_id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_transaction_currency1`
    FOREIGN KEY (`client3_id` , `currency3_id`)
    REFERENCES `transactions`.`currency` (`client_id` , `currency_id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_transaction_currency2`
    FOREIGN KEY (`client4_id` , `currency4_id`)
    REFERENCES `transactions`.`currency` (`client_id` , `currency_id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_transaction_currency3`
    FOREIGN KEY (`client5_id` , `currency5_id`)
    REFERENCES `transactions`.`currency` (`client_id` , `currency_id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_transaction_currency4`
    FOREIGN KEY (`client6_id` , `currency6_id`)
    REFERENCES `transactions`.`currency` (`client_id` , `currency_id`)
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
INSERT INTO `transactions`.`client` (`id`, `pib`) VALUES (1, 'Ivan');
INSERT INTO `transactions`.`client` (`id`, `pib`) VALUES (2, 'Vasil');
INSERT INTO `transactions`.`client` (`id`, `pib`) VALUES (3, 'Egor');
INSERT INTO `transactions`.`client` (`id`, `pib`) VALUES (4, 'Pavel');
INSERT INTO `transactions`.`client` (`id`, `pib`) VALUES (5, 'Artem');
INSERT INTO `transactions`.`client` (`id`, `pib`) VALUES (6, 'Sofia');

COMMIT;


-- -----------------------------------------------------
-- Data for table `transactions`.`currency`
-- -----------------------------------------------------
START TRANSACTION;
USE `transactions`;
INSERT INTO `transactions`.`currency` (`client_id`, `currency_id`, `name`, `amount`) VALUES (1, 980, 'UAH', 100.01);
INSERT INTO `transactions`.`currency` (`client_id`, `currency_id`, `name`, `amount`) VALUES (1, 840, 'USD', 120.45);
INSERT INTO `transactions`.`currency` (`client_id`, `currency_id`, `name`, `amount`) VALUES (2, 980, 'UAH', 500);
INSERT INTO `transactions`.`currency` (`client_id`, `currency_id`, `name`, `amount`) VALUES (3, 980, 'UAH', 70.68);
INSERT INTO `transactions`.`currency` (`client_id`, `currency_id`, `name`, `amount`) VALUES (3, 840, 'USD', 17.52);
INSERT INTO `transactions`.`currency` (`client_id`, `currency_id`, `name`, `amount`) VALUES (4, 840, 'USD', 732);
INSERT INTO `transactions`.`currency` (`client_id`, `currency_id`, `name`, `amount`) VALUES (5, 980, 'UAH', 1000);
INSERT INTO `transactions`.`currency` (`client_id`, `currency_id`, `name`, `amount`) VALUES (6, 980, 'UAH', 5000);

COMMIT;

