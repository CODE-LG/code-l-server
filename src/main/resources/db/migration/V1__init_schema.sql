-- block_member_relation
DROP TABLE IF EXISTS `block_member_relation`;
CREATE TABLE `block_member_relation` (
  `blocked_member_id` bigint DEFAULT NULL,
  `blocker_member_id` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `status` enum('BLOCKED','UNBLOCKED') DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKdsr0m0wy7ihip22ij1ckivgpn` (`blocked_member_id`),
  KEY `FKry2oe0ajf7pyv7k8495u6tcs6` (`blocker_member_id`),
  CONSTRAINT `FKdsr0m0wy7ihip22ij1ckivgpn` FOREIGN KEY (`blocked_member_id`) REFERENCES `member` (`id`),
  CONSTRAINT `FKry2oe0ajf7pyv7k8495u6tcs6` FOREIGN KEY (`blocker_member_id`) REFERENCES `member` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- chat_room_member
DROP TABLE IF EXISTS `chat_room_member`;
CREATE TABLE `chat_room_member` (
  `chat_id` bigint DEFAULT NULL,
  `chat_room_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `left_at` datetime(6) DEFAULT NULL,
  `member_id` bigint NOT NULL,
  `member_status` enum('ACTIVE','LEFT') DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKsseano88a8cv0ne1qxsiv1g4v` (`chat_room_id`,`member_id`),
  UNIQUE KEY `UKc3bwd8ohk6yni9mjeryembv4g` (`chat_id`),
  KEY `FKq64atn9y4cyjpp4qcrllxi3o5` (`member_id`),
  CONSTRAINT `FKb9o8lisg7q5wiv978eing6088` FOREIGN KEY (`chat_id`) REFERENCES `chat` (`id`),
  CONSTRAINT `FKo6a9v51aal2574fjb1ldlw4di` FOREIGN KEY (`chat_room_id`) REFERENCES `chat_room` (`id`),
  CONSTRAINT `FKq64atn9y4cyjpp4qcrllxi3o5` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- chat_room_question
DROP TABLE IF EXISTS `chat_room_question`;
CREATE TABLE `chat_room_question` (
  `is_used` bit(1) NOT NULL,
  `chat_room_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `question_id` bigint NOT NULL,
  `requested_by_member_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `used_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK1mdkxhjqch6uccxt1iegs4ej5` (`chat_room_id`,`question_id`),
  KEY `FK6uvsi17h1sv35piwgc72nq4sx` (`question_id`),
  KEY `FKrxxhqhqldakqf0kljq79f0mc3` (`requested_by_member_id`),
  CONSTRAINT `FK6uvsi17h1sv35piwgc72nq4sx` FOREIGN KEY (`question_id`) REFERENCES `question` (`id`),
  CONSTRAINT `FKermg781g6u4fk2q3p6bhiq86b` FOREIGN KEY (`chat_room_id`) REFERENCES `chat_room` (`id`),
  CONSTRAINT `FKrxxhqhqldakqf0kljq79f0mc3` FOREIGN KEY (`requested_by_member_id`) REFERENCES `member` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- chat_room
DROP TABLE IF EXISTS `chat_room`;
CREATE TABLE `chat_room` (
  `is_unlocked` bit(1) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `recent_chat_id` bigint DEFAULT NULL,
  `unlocked_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `status` enum('DISABLED','LOCKED','UNLOCKED') DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK1b9vcrrg9sp0nkfygcgebv44e` (`recent_chat_id`),
  CONSTRAINT `FK44nqbivue0gtsjdpgt7y0imcj` FOREIGN KEY (`recent_chat_id`) REFERENCES `chat` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- chat
DROP TABLE IF EXISTS `chat`;
CREATE TABLE `chat` (
  `chat_room_id` bigint NOT NULL,
  `from_chat_room_member_id` bigint DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sent_at` datetime(6) DEFAULT NULL,
  `message` varchar(255) DEFAULT NULL,
  `chat_content_type` enum('CLOSE_CONVERSATION','MATCHED','ONBOARDING','QUESTION','TEXT','TIME','UNLOCKED','UNLOCKED_APPROVED','UNLOCKED_REJECTED','UNLOCKED_REQUEST') DEFAULT NULL,
  `sender_type` enum('MY','PARTNER','SYSTEM','USER') DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK44b6elhh512d2722l09i6qdku` (`chat_room_id`),
  KEY `FK9mryo76qkolxuojwwbtj9c1fx` (`from_chat_room_member_id`),
  CONSTRAINT `FK44b6elhh512d2722l09i6qdku` FOREIGN KEY (`chat_room_id`) REFERENCES `chat_room` (`id`),
  CONSTRAINT `FK9mryo76qkolxuojwwbtj9c1fx` FOREIGN KEY (`from_chat_room_member_id`) REFERENCES `chat_room_member` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- code_unlock_request
DROP TABLE IF EXISTS `code_unlock_request`;
CREATE TABLE `code_unlock_request` (
  `chat_room_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `processed_at` datetime(6) DEFAULT NULL,
  `processed_by_id` bigint DEFAULT NULL,
  `requested_at` datetime(6) DEFAULT NULL,
  `requester_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `status` enum('APPROVED','PENDING','REJECTED') DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKmidqbqlkm89oeydsids7g8xvx` (`chat_room_id`),
  KEY `FKasx5j9682qplkvfynoiol0uwm` (`processed_by_id`),
  KEY `FKew1slajceqoc7uljtbd7spir9` (`requester_id`),
  CONSTRAINT `FKasx5j9682qplkvfynoiol0uwm` FOREIGN KEY (`processed_by_id`) REFERENCES `member` (`id`),
  CONSTRAINT `FKew1slajceqoc7uljtbd7spir9` FOREIGN KEY (`requester_id`) REFERENCES `member` (`id`),
  CONSTRAINT `FKmidqbqlkm89oeydsids7g8xvx` FOREIGN KEY (`chat_room_id`) REFERENCES `chat_room` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- member_signal
DROP TABLE IF EXISTS `member_signal`;
CREATE TABLE `member_signal` (
  `created_at` datetime(6) DEFAULT NULL,
  `from_member_id` bigint DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `to_member_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `message` varchar(255) DEFAULT NULL,
  `receiver_status` enum('APPROVED','NONE','PENDING','PENDING_HIDDEN','REJECTED') DEFAULT NULL,
  `sender_status` enum('APPROVED','NONE','PENDING','PENDING_HIDDEN','REJECTED') DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKlobswiesdohss9tutwg3pqb7v` (`from_member_id`),
  KEY `FKif6qksw91ei4qedxui44mm9yd` (`to_member_id`),
  CONSTRAINT `FKif6qksw91ei4qedxui44mm9yd` FOREIGN KEY (`to_member_id`) REFERENCES `member` (`id`),
  CONSTRAINT `FKlobswiesdohss9tutwg3pqb7v` FOREIGN KEY (`from_member_id`) REFERENCES `member` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- member
DROP TABLE IF EXISTS `member`;
CREATE TABLE `member` (
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `fcm_token` varchar(255) DEFAULT NULL,
  `oauth_id` varchar(255) DEFAULT NULL,
  `reject_reason` varchar(255) DEFAULT NULL,
  `member_status` enum('DONE','ESSENTIAL_COMPLETED','HIDDEN_COMPLETED','PENDING','PERSONALITY_COMPLETED','PHONE_VERIFIED','REJECT','SIGNUP','WITHDRAWN') DEFAULT NULL,
  `oauth_type` enum('ADMIN','APPLE','GOOGLE','KAKAO') DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKpogkt256oewuximsknodfn6da` (`oauth_type`,`oauth_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- profiles
DROP TABLE IF EXISTS `profiles`;
CREATE TABLE `profiles` (
  `birth_date` date DEFAULT NULL,
  `essential_completed` bit(1) NOT NULL,
  `height` int DEFAULT NULL,
  `hidden_completed` bit(1) NOT NULL,
  `personality_completed` bit(1) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `essential_completed_at` datetime(6) DEFAULT NULL,
  `hidden_completed_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint DEFAULT NULL,
  `personality_completed_at` datetime(6) DEFAULT NULL,
  `representative_question_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `code_image` varchar(1000) DEFAULT NULL,
  `face_image` varchar(1000) DEFAULT NULL,
  `affection_style` varchar(255) DEFAULT NULL,
  `alcohol` varchar(255) DEFAULT NULL,
  `big_city` varchar(255) DEFAULT NULL,
  `body_type` varchar(255) DEFAULT NULL,
  `code_name` varchar(255) DEFAULT NULL,
  `conflict_resolution_style` varchar(255) DEFAULT NULL,
  `contact_style` varchar(255) DEFAULT NULL,
  `date_style` varchar(255) DEFAULT NULL,
  `hair_length` varchar(255) DEFAULT NULL,
  `interests` varchar(255) DEFAULT NULL,
  `introduce` varchar(255) DEFAULT NULL,
  `job` varchar(255) DEFAULT NULL,
  `love_language` varchar(255) DEFAULT NULL,
  `mbti` varchar(255) DEFAULT NULL,
  `personalities` varchar(255) DEFAULT NULL,
  `relationship_values` varchar(255) DEFAULT NULL,
  `representative_answer` varchar(255) DEFAULT NULL,
  `small_city` varchar(255) DEFAULT NULL,
  `smoke` varchar(255) DEFAULT NULL,
  `style` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKa26u3c3eoglisoov0p1k1841f` (`member_id`),
  UNIQUE KEY `UK9amiri54mo9sfa9jpq7inru5m` (`representative_question_id`),
  CONSTRAINT `FK3je4xlea0lern2dsaq2ofgyfd` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`),
  CONSTRAINT `FK5o19tomscbkmbixg93uj3ieih` FOREIGN KEY (`representative_question_id`) REFERENCES `question` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- question
DROP TABLE IF EXISTS `question`;
CREATE TABLE `question` (
  `is_active` bit(1) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `content` varchar(500) NOT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `category` enum('BALANCE_ONE','CURRENT_ME','DATE','FAVORITE','MEMORY','VALUES','WANT_TALK') NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;