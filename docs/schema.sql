-- Reference schema (Hibernate creates this automatically with ddl-auto=update).
-- Hibernate maps enums to MySQL ENUM columns; VARCHAR is shown here for readability.
CREATE DATABASE IF NOT EXISTS messmate;
USE messmate;

CREATE TABLE residents (
  id      BIGINT AUTO_INCREMENT PRIMARY KEY,
  name    VARCHAR(80)  NOT NULL,
  room_no VARCHAR(10)  NOT NULL,
  email   VARCHAR(120) NOT NULL UNIQUE
);

CREATE TABLE meals (
  id        BIGINT AUTO_INCREMENT PRIMARY KEY,
  meal_date DATE        NOT NULL,
  meal_type VARCHAR(12) NOT NULL,              -- BREAKFAST | LUNCH | DINNER
  CONSTRAINT uk_meal_date_type UNIQUE (meal_date, meal_type)
);

CREATE TABLE menu_items (
  id       BIGINT AUTO_INCREMENT PRIMARY KEY,
  name     VARCHAR(80) NOT NULL,
  category VARCHAR(12) NOT NULL,               -- MAIN | SIDE | DESSERT | BEVERAGE
  veg      BIT         NOT NULL,
  meal_id  BIGINT      NOT NULL,
  FOREIGN KEY (meal_id) REFERENCES meals(id)
);

CREATE TABLE feedback (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  resident_id  BIGINT   NOT NULL,
  meal_id      BIGINT   NOT NULL,
  rating       INT      NOT NULL,              -- 1..5, enforced by @Min/@Max
  comment_text VARCHAR(500),
  submitted_at DATETIME NOT NULL,
  CONSTRAINT uk_feedback_resident_meal UNIQUE (resident_id, meal_id),
  FOREIGN KEY (resident_id) REFERENCES residents(id),
  FOREIGN KEY (meal_id)     REFERENCES meals(id)
);

CREATE TABLE meal_opt_outs (                   -- many-to-many: resident skips meal
  resident_id BIGINT NOT NULL,
  meal_id     BIGINT NOT NULL,
  PRIMARY KEY (resident_id, meal_id),
  FOREIGN KEY (resident_id) REFERENCES residents(id),
  FOREIGN KEY (meal_id)     REFERENCES meals(id)
);
