ALTER TABLE users
    ADD COLUMN image_path VARCHAR(255),
    ADD COLUMN description TEXT,
    ADD COLUMN birthday DATE,
    ADD COLUMN location VARCHAR(255),
    ADD COLUMN education VARCHAR(255),
    ADD COLUMN birthplace VARCHAR(255),
    ADD COLUMN employment VARCHAR(255);