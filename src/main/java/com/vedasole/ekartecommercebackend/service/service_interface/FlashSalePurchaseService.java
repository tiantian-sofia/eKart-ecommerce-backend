package com.vedasole.ekartecommercebackend.service.service_interface;

public interface FlashSalePurchaseService {

    /**
     * Purchases a flash sale item: validates rules, deducts stock, creates order,
     * creates Stripe checkout session.
     *
     * @return Stripe checkout session URL
     */
    String purchaseFlashSaleItem(long saleId, long itemId, int quantity, long customerId);

    /**
     * Re-applies the flash sale total to an order after webhook processing.
     * Called from the webhook handler to ensure the order total reflects the flash price.
     */
    void reapplyFlashSaleTotal(long orderId);

    /**
     * Checks if an order is associated with a flash sale purchase.
     */
    boolean isFlashSaleOrder(long orderId);
}
