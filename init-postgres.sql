CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(19, 2),
    quantity INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert sample data
INSERT INTO products (name, description, price, quantity) VALUES
('Postgres Product 1', 'Description for Postgres Product 1', 19.99, 100),
('Postgres Product 2', 'Description for Postgres Product 2', 29.99, 50),
('Postgres Product 3', 'Description for Postgres Product 3', 39.99, 25);
