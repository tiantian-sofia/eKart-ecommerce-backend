package com.vedasole.ekartecommercebackend.service.service_impl;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.vedasole.ekartecommercebackend.entity.*;
import com.vedasole.ekartecommercebackend.exception.APIException;
import com.vedasole.ekartecommercebackend.exception.ResourceNotFoundException;
import com.vedasole.ekartecommercebackend.repository.*;
import com.vedasole.ekartecommercebackend.service.service_interface.FlashSalePurchaseService;
import com.vedasole.ekartecommercebackend.utility.AppConstant;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.vedasole.ekartecommercebackend.utility.AppConstant.RELATIONS.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FlashSalePurchaseServiceImpl implements FlashSalePurchaseService {

    private final FlashSaleRepo flashSaleRepo;
    private final FlashSaleItemRepo flashSaleItemRepo;
    private final FlashSalePurchaseRepo flashSalePurchaseRepo;
    private final CustomerRepo customerRepo;
    private final ProductRepo productRepo;
    private final OrderRepo orderRepo;
    private final AddressRepo addressRepo;
    private final StripeService stripeService;
    private final EntityManager entityManager;

    @Override
    public String purchaseFlashSaleItem(long saleId, long itemId, int quantity, long customerId) {

        // === STEP 1: Load and validate FlashSale ===
        FlashSale flashSale = flashSaleRepo.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException(FLASH_SALE.getValue(), "id", saleId));

        if (!flashSale.isActive()) {
            if (flashSale.isUpcoming()) {
                throw new APIException(
                        "Flash sale has not started yet. Starts at: " + flashSale.getStartTime(),
                        HttpStatus.BAD_REQUEST);
            } else {
                throw new APIException("Flash sale has ended.", HttpStatus.BAD_REQUEST);
            }
        }

        // === STEP 2: Load and validate FlashSaleItem ===
        FlashSaleItem flashSaleItem = flashSaleItemRepo
                .findByFlashSale_FlashSaleIdAndFlashSaleItemId(saleId, itemId)
                .orElseThrow(() -> new ResourceNotFoundException(FLASH_SALE_ITEM.getValue(), "id", itemId));

        // === STEP 3: Check per-customer purchase limit ===
        int alreadyPurchased = flashSalePurchaseRepo
                .sumQuantityByFlashSaleItemAndCustomer(itemId, customerId);
        if (alreadyPurchased + quantity > flashSaleItem.getLimitPerCustomer()) {
            throw new APIException(
                    String.format("Purchase limit exceeded. Already purchased: %d, limit: %d.",
                            alreadyPurchased, flashSaleItem.getLimitPerCustomer()),
                    HttpStatus.BAD_REQUEST);
        }

        // === STEP 4: Atomic stock deduction (concurrency-safe) ===
        int rowsAffected = flashSaleItemRepo.deductStock(itemId, quantity);
        if (rowsAffected == 0) {
            throw new APIException(
                    "Insufficient flash sale stock. Sold out or not enough available.",
                    HttpStatus.BAD_REQUEST);
        }

        // === STEP 5: Load customer and product ===
        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(CUSTOMER.getValue(), "id", customerId));
        Product product = productRepo.findById(flashSaleItem.getProduct().getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT.getValue(), "id",
                        flashSaleItem.getProduct().getProductId()));

        // === STEP 6: Create Order ===
        double flashTotal = flashSaleItem.getFlashPrice() * quantity;

        Address dummyAddress = addressRepo.save(new Address(
                "Flash Sale Address",
                "Flash Sale Address",
                "Flash Sale City",
                "Flash Sale State",
                "Flash Sale Country",
                100001
        ));

        Order order = Order.builder()
                .customer(customer)
                .orderItems(new ArrayList<>())
                .address(dummyAddress)
                .total(0)
                .orderStatus(AppConstant.OrderStatus.ORDER_CREATED)
                .build();
        Order savedOrder = orderRepo.save(order);

        // Create OrderItem
        OrderItem orderItem = new OrderItem(savedOrder, product, quantity);
        savedOrder.setOrderItems(new ArrayList<>(List.of(orderItem)));
        orderRepo.save(savedOrder);
        // At this point, @PrePersist/@PreUpdate has set total to the normal product price total

        // === STEP 7: Override total with flash price via native SQL ===
        entityManager.flush();
        entityManager.createNativeQuery(
                        "UPDATE \"order\" SET \"total\" = :flashTotal WHERE \"order_id\" = :orderId")
                .setParameter("flashTotal", flashTotal)
                .setParameter("orderId", savedOrder.getOrderId())
                .executeUpdate();

        // === STEP 8: Create FlashSalePurchase record ===
        FlashSalePurchase purchase = FlashSalePurchase.builder()
                .flashSaleItem(flashSaleItem)
                .customer(customer)
                .quantity(quantity)
                .orderId(savedOrder.getOrderId())
                .flashPriceTotal(flashTotal)
                .build();
        flashSalePurchaseRepo.save(purchase);

        // === STEP 9: Create Stripe Checkout Session with flash price ===
        try {
            Session session = stripeService.createFlashSaleCheckoutSession(
                    savedOrder, flashSaleItem.getFlashPrice(), quantity, customer, product);
            return session.getUrl();
        } catch (StripeException ex) {
            log.error("Error creating flash sale checkout session: {}", ex.getMessage(), ex);
            throw new APIException("Failed to create checkout session: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional
    public void reapplyFlashSaleTotal(long orderId) {
        flashSalePurchaseRepo.findByOrderId(orderId).ifPresent(purchase -> {
            entityManager.createNativeQuery(
                            "UPDATE \"order\" SET \"total\" = :flashTotal WHERE \"order_id\" = :orderId")
                    .setParameter("flashTotal", purchase.getFlashPriceTotal())
                    .setParameter("orderId", orderId)
                    .executeUpdate();
            log.info("Re-applied flash sale total {} for order {}", purchase.getFlashPriceTotal(), orderId);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFlashSaleOrder(long orderId) {
        return flashSalePurchaseRepo.findByOrderId(orderId).isPresent();
    }
}
