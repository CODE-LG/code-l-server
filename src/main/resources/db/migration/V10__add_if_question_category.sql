-- V10__add_if_question_category.sql
-- Add 'IF' category to question table's category enum

ALTER TABLE `question` 
MODIFY COLUMN `category` 
ENUM('BALANCE_ONE','CURRENT_ME','DATE','FAVORITE','IF','MEMORY','VALUES','WANT_TALK') 
NOT NULL;
