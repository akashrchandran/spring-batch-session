CREATE TABLE IF NOT EXISTS sales_data (
    index BIGINT,
    invoice_no VARCHAR(50),
    stock_code VARCHAR(50),
    description TEXT,
    quantity INTEGER,
    invoice_date TIMESTAMP,
    unit_price DECIMAL(10, 2),
    customer_id VARCHAR(50),
    country VARCHAR(100),
    PRIMARY KEY (index)
);
