DROP TABLE IF EXISTS todos;
CREATE TABLE todos (
  id          INTEGER      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  description VARCHAR(255) NOT NULL,
  done        BOOLEAN      NOT NULL
);