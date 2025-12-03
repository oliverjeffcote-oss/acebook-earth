DROP TABLE IF EXISTS relationships;

CREATE TABLE relationships (
  id bigserial PRIMARY KEY,
  requester_id BIGINT NOT NULL,
  receiver_id BIGINT NOT NULL,
  CONSTRAINT fk_requester_id
    FOREIGN KEY (requester_id)
    REFERENCES users(id)
    ON DELETE CASCADE,
  CONSTRAINT fk_receiver_id
    FOREIGN KEY (receiver_id)
    REFERENCES users(id)
    ON DELETE CASCADE,
  status varchar(50) DEFAULT 'pending'
);