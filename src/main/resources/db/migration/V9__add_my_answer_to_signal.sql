-- V9__add_my_answer_to_signal.sql
-- Add myAnswer field to member_signal table

ALTER TABLE `member_signal`
ADD COLUMN `my_answer` VARCHAR(255) DEFAULT '' NOT NULL AFTER `message`;
