package com.vedasole.ekartecommercebackend.service.serviceImpl;

import com.vedasole.ekartecommercebackend.entity.Category;
import com.vedasole.ekartecommercebackend.payload.ProductDto;
import com.vedasole.ekartecommercebackend.repository.CategoryRepo;
import com.vedasole.ekartecommercebackend.repository.ProductRepo;
import com.vedasole.ekartecommercebackend.service.service_impl.ProductServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Integration tests for {@link ProductServiceImpl#createProduct(ProductDto)}.
 * <p>
 * Uses an embedded database ({@code @DataJpaTest}) so the full flow — DTO mapping,
 * SKU generation, uniqueness check via {@code existsBySku}, and JPA persistence — is
 * exercised end-to-end.
 */
@DataJpaTest
class ProductServiceImplIntegrationTest {

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private CategoryRepo categoryRepo;

    private ProductServiceImpl productService;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setAmbiguityIgnored(true);
        productService = new ProductServiceImpl(productRepo, modelMapper, categoryRepo);

        testCategory = categoryRepo.save(
                Category.builder()
                        .name("Electronics")
                        .image("cat.jpg")
                        .desc("Electronic items")
                        .active(true)
                        .build());
    }

    @AfterEach
    void tearDown() {
        // Use JPQL bulk delete to avoid JPA cascade loading issues with category_id NOT NULL
        productRepo.deleteAllInBatch();
        categoryRepo.deleteAllInBatch();
    }

    // ===== Normal inputs =====

    @Test
    void createProduct_normalInputs_generatesExpectedSku() {
        ProductDto dto = ProductDto.builder()
                .name("Smartphone")
                .image("img.jpg")
                .desc("Latest model")
                .price(999.0)
                .discount(0.0)
                .qtyInStock(10)
                .categoryId(testCategory.getCategoryId())
                .build();

        ProductDto created = productService.createProduct(dto);

        assertThat(created).isNotNull();
        // cat "Electronics"->"Ele", name "Smartphone"->"Smart", desc "Latest model"->"Lat"
        assertThat(productRepo.findById(created.getProductId()).orElseThrow().getSku())
                .isEqualTo("Ele-Smart-Lat");
    }

    // ===== Bug-reproduction: short inputs that previously crashed =====

    @Test
    void createProduct_shortCategoryName_doesNotCrash() {
        Category shortCat = categoryRepo.save(
                Category.builder().name("AB").image("c.jpg").desc("d").active(true).build());

        ProductDto dto = ProductDto.builder()
                .name("Phone")
                .image("img.jpg")
                .desc("Good product")
                .price(100.0)
                .discount(0.0)
                .qtyInStock(5)
                .categoryId(shortCat.getCategoryId())
                .build();

        assertThatNoException().isThrownBy(() -> productService.createProduct(dto));
    }

    @Test
    void createProduct_shortProductName_doesNotCrash() {
        ProductDto dto = ProductDto.builder()
                .name("Pen")
                .image("img.jpg")
                .desc("A nice pen")
                .price(1.5)
                .discount(0.0)
                .qtyInStock(100)
                .categoryId(testCategory.getCategoryId())
                .build();

        assertThatNoException().isThrownBy(() -> productService.createProduct(dto));
    }

    @Test
    void createProduct_shortDesc_doesNotCrash() {
        ProductDto dto = ProductDto.builder()
                .name("Phone")
                .image("img.jpg")
                .desc("Hi")
                .price(100.0)
                .discount(0.0)
                .qtyInStock(5)
                .categoryId(testCategory.getCategoryId())
                .build();

        assertThatNoException().isThrownBy(() -> productService.createProduct(dto));
    }

    @Test
    void createProduct_shortInputs_generatesPaddedSku() {
        Category shortCat = categoryRepo.save(
                Category.builder().name("AB").image("c.jpg").desc("d").active(true).build());

        ProductDto dto = ProductDto.builder()
                .name("Pen")
                .image("img.jpg")
                .desc("Hi")
                .price(1.5)
                .discount(0.0)
                .qtyInStock(10)
                .categoryId(shortCat.getCategoryId())
                .build();

        ProductDto created = productService.createProduct(dto);

        String sku = productRepo.findById(created.getProductId()).orElseThrow().getSku();
        // cat "AB"->"ABX", name "Pen"->"PenXX", desc "Hi"->"HiX"
        assertThat(sku).isEqualTo("ABX-PenXX-HiX");
    }

    // ===== SKU uniqueness =====

    @Test
    void createProduct_duplicateBaseSku_appendsUniquenessSuffix() {
        ProductDto dto1 = ProductDto.builder()
                .name("Phone")
                .image("img1.jpg")
                .desc("Good product")
                .price(100.0)
                .discount(0.0)
                .qtyInStock(5)
                .categoryId(testCategory.getCategoryId())
                .build();
        ProductDto dto2 = ProductDto.builder()
                .name("Phone")
                .image("img2.jpg")
                .desc("Good product")
                .price(200.0)
                .discount(0.0)
                .qtyInStock(10)
                .categoryId(testCategory.getCategoryId())
                .build();

        ProductDto created1 = productService.createProduct(dto1);
        ProductDto created2 = productService.createProduct(dto2);

        String sku1 = productRepo.findById(created1.getProductId()).orElseThrow().getSku();
        String sku2 = productRepo.findById(created2.getProductId()).orElseThrow().getSku();

        // Both have same base SKU "Ele-Phone-Goo"; second must have a suffix
        assertThat(sku1).isEqualTo("Ele-Phone-Goo");
        assertThat(sku2).startsWith("Ele-Phone-Goo-");
        assertThat(sku1).isNotEqualTo(sku2);
    }

    @Test
    void createProduct_multipleProductsSameSku_incrementsSuffix() {
        ProductDto baseDto = ProductDto.builder()
                .name("Phone")
                .image("img.jpg")
                .desc("Good product")
                .price(100.0)
                .discount(0.0)
                .qtyInStock(5)
                .categoryId(testCategory.getCategoryId())
                .build();

        // Create 3 products with identical SKU-generating inputs
        productService.createProduct(baseDto);
        productService.createProduct(baseDto);
        ProductDto created3 = productService.createProduct(baseDto);

        String sku3 = productRepo.findById(created3.getProductId()).orElseThrow().getSku();
        // Third product should have suffix "-2"
        assertThat(sku3).isEqualTo("Ele-Phone-Goo-2");
    }

    // ===== SKU format =====

    @Test
    void createProduct_skuMatchesFormatSegments() {
        ProductDto dto = ProductDto.builder()
                .name("Laptop")
                .image("img.jpg")
                .desc("High performance")
                .price(1500.0)
                .discount(5.0)
                .qtyInStock(3)
                .categoryId(testCategory.getCategoryId())
                .build();

        ProductDto created = productService.createProduct(dto);

        String sku = productRepo.findById(created.getProductId()).orElseThrow().getSku();
        String[] parts = sku.split("-");
        assertThat(parts).hasSizeGreaterThanOrEqualTo(3);
        assertThat(parts[0]).hasSize(3);   // category segment
        assertThat(parts[1]).hasSize(5);   // name segment
        assertThat(parts[2]).hasSize(3);   // desc segment
    }

    @Test
    void createProduct_skuContainsNoSpaces() {
        ProductDto dto = ProductDto.builder()
                .name("A B C D E")
                .image("img.jpg")
                .desc("X Y Z")
                .price(10.0)
                .discount(0.0)
                .qtyInStock(1)
                .categoryId(testCategory.getCategoryId())
                .build();

        ProductDto created = productService.createProduct(dto);

        String sku = productRepo.findById(created.getProductId()).orElseThrow().getSku();
        assertThat(sku).doesNotContain(" ");
    }
}
