CREATE SEQUENCE categories_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE products_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE categories
(
    id          INTEGER NOT NULL,
    name        VARCHAR(255),
    description VARCHAR(255),
    CONSTRAINT pk_categories PRIMARY KEY (id)
);

CREATE TABLE products
(
    id                 INTEGER          NOT NULL,
    name               VARCHAR(255),
    description        VARCHAR(255),
    available_quantity DOUBLE PRECISION NOT NULL,
    price              NUMERIC(38, 2),
    category_id        INTEGER,
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id)
        REFERENCES categories (id)
);

CREATE INDEX idx_products_category_id ON products (category_id);
