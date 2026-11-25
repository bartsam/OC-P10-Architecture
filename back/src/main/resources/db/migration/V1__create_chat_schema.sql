CREATE TABLE user_account (
    id          uuid         NOT NULL,
    first_name  varchar(100) NOT NULL,
    last_name   varchar(100) NOT NULL,
    role        varchar(20)  NOT NULL,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_user_account PRIMARY KEY (id),
    CONSTRAINT ck_user_account_role CHECK (role IN ('CUSTOMER', 'AGENT'))
);

CREATE TABLE conversation (
    id          uuid        NOT NULL,
    user_id     uuid        NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_conversation PRIMARY KEY (id),
    CONSTRAINT fk_conversation_user FOREIGN KEY (user_id) REFERENCES user_account (id)
);

CREATE INDEX idx_conversation_created_at ON conversation (created_at DESC);

CREATE TABLE message (
    id               uuid        NOT NULL,
    conversation_id  uuid        NOT NULL,
    sender_id        uuid        NOT NULL,
    content          text        NOT NULL,
    sent_at          timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_message PRIMARY KEY (id),
    CONSTRAINT fk_message_conversation FOREIGN KEY (conversation_id) REFERENCES conversation (id),
    CONSTRAINT fk_message_sender FOREIGN KEY (sender_id) REFERENCES user_account (id),
    CONSTRAINT ck_message_content CHECK (char_length(btrim(content)) > 0 AND char_length(content) <= 2000)
);

CREATE INDEX idx_message_conversation_sent_at ON message (conversation_id, sent_at);