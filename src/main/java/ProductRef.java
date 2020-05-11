public enum ProductRef {
    Basic(100),
    Deluxe(200);

    private final int productId;

    ProductRef(int id) {
        this.productId = id;
    }

    public int getProductId() {
        return this.productId;
    }
}
