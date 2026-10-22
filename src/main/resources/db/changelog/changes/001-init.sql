--liquibase formatted sql

--changeset relay:001-create-users
CREATE TABLE users (
    id         UUID PRIMARY KEY,
    username   VARCHAR(32) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL
);
--rollback DROP TABLE users;

--changeset relay:001-create-chats
CREATE TABLE chats (
    id         UUID PRIMARY KEY,
    type       VARCHAR(16) NOT NULL CHECK (type IN ('DIRECT', 'GROUP')),
    title      VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL
);
--rollback DROP TABLE chats;

--changeset relay:001-create-chat-members
CREATE TABLE chat_members (
    chat_id   UUID        NOT NULL REFERENCES chats (id) ON DELETE CASCADE,
    user_id   UUID        NOT NULL REFERENCES users (id),
    role      VARCHAR(16) NOT NULL CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER')),
    joined_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (chat_id, user_id)
);
CREATE INDEX chat_members_user_id_idx ON chat_members (user_id);
--rollback DROP TABLE chat_members;
