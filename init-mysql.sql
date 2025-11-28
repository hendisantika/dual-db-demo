CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(19, 2),
    quantity INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Insert sample data
INSERT INTO products (name, description, price, quantity) VALUES
('MySQL Product 1', 'Description for MySQL Product 1', 19.99, 100),
('MySQL Product 2', 'Description for MySQL Product 2', 29.99, 50),
('MySQL Product 3', 'Description for MySQL Product 3', 39.99, 25);
