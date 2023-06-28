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
  `id` INT NOT NULL,
  `date` DATETIME NOT NULL,
  `client_id` INT NOT NULL,
  `currency_id` INT NOT NULL,
  `rate` DOUBLE NOT NULL,
  `balance` DOUBLE NOT NULL,
  `commission` DOUBLE NULL,
  `amount` DOUBLE NOT NULL COMMENT 'HRN',
  `transportation` DOUBLE NULL,
  PRIMARY KEY (`id`, `client_id`),
  INDEX `fk_client_has_currency_has_client_has_currency_client_has_c_idx1` (`client_id` ASC, `currency_id` ASC) VISIBLE,
  CONSTRAINT `fk_client_has_currency_has_client_has_currency_client_has_cur1`
    FOREIGN KEY (`client_id` , `currency_id`)
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
INSERT INTO `transactions`.`client` (`id`, `pib`, `phone_number`, `telegram`) VALUES (1, 'Ivan', '+38 (087) 675-09-36', '@ivan');
INSERT INTO `transactions`.`client` (`id`, `pib`, `phone_number`, `telegram`) VALUES (2, 'Vasil', '+38 (076) 130-64-19', '@vasil');
INSERT INTO `transactions`.`client` (`id`, `pib`, `phone_number`, `telegram`) VALUES (3, 'Egor', '+38 (092) 277-99-81', '@egor');
INSERT INTO `transactions`.`client` (`id`, `pib`, `phone_number`, `telegram`) VALUES (4, 'Pavel', '+38 (077) 493-73-42', '@pavel');
INSERT INTO `transactions`.`client` (`id`, `pib`, `phone_number`, `telegram`) VALUES (5, 'Artem', '+38 (058) 409-94-17', '@artem');
INSERT INTO `transactions`.`client` (`id`, `pib`, `phone_number`, `telegram`) VALUES (6, 'Sofia', '+38 (038) 155-39-83', '@sofia');

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

