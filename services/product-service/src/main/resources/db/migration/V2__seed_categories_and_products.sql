WITH seeded_categories AS (
    INSERT INTO categories (id, name, description)
        VALUES (nextval('categories_seq'), 'Electronics', 'Computers and electronic accessories'),
               (nextval('categories_seq'), 'Books', 'Books for learning and leisure'),
               (nextval('categories_seq'), 'Home and Office', 'Everyday home and office essentials')
        RETURNING id, name),
     seed_products (name, description, available_quantity, price, category_name) AS (VALUES ('Laptop',
                                                                                             'Laptop with 16 GB RAM and 512 GB SSD',
                                                                                             25, 899.99, 'Electronics'),
                                                                                            ('Wireless Mouse',
                                                                                             'Ergonomic wireless mouse',
                                                                                             100, 24.99, 'Electronics'),
                                                                                            ('Mechanical Keyboard',
                                                                                             'Mechanical keyboard with backlit keys',
                                                                                             60, 79.99, 'Electronics'),
                                                                                            ('Java Programming Book',
                                                                                             'A practical introduction to Java programming',
                                                                                             80, 39.99, 'Books'),
                                                                                            ('Desk Lamp',
                                                                                             'Adjustable LED desk lamp',
                                                                                             45, 29.99,
                                                                                             'Home and Office'),
                                                                                            ('Office Chair',
                                                                                             'Adjustable ergonomic office chair',
                                                                                             30, 149.99,
                                                                                             'Home and Office'))
INSERT
INTO products (id, name, description, available_quantity, price, category_id)
SELECT nextval('products_seq'),
       product.name,
       product.description,
       product.available_quantity,
       product.price,
       category.id
FROM seed_products AS product
         JOIN seeded_categories AS category ON category.name = product.category_name;
