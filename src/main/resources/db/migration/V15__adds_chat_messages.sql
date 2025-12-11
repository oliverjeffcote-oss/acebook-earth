CREATE TABLE chat_messages (
    id bigserial PRIMARY KEY,
    content TEXT,
    sender_id BIGINT NOT NULL,
    recipient_id BIGINT NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sender FOREIGN KEY (sender_id) REFERENCES users(id),
    CONSTRAINT fk_recipient FOREIGN KEY (recipient_id) REFERENCES users(id)
);