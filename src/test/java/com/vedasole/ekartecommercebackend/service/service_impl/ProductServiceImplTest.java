package com.vedasole.ekartecommercebackend.service.service_impl;

import com.vedasole.ekartecommercebackend.entity.Category;
import com.vedasole.ekartecommercebackend.entity.Product;
import com.vedasole.ekartecommercebackend.payload.ProductDto;
import com.vedasole.ekartecommercebackend.repository.CategoryRepo;
import com.vedasole.ekartecommercebackend.repository.ProductRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepo productRepo;
    @Mock
    private ModelMapper modelMapper;
    @Mock
    private CategoryRepo categoryRepo;

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(productRepo, modelMapper, categoryRepo);
    }

    // ---- Integration tests through createProduct ----

    /**
     * Reproduces the original crash: category name "AB" (2 chars) and product name "Pen" (3 chars)
     * triggered StringIndexOutOfBoundsException in the old substring(0,3) / substring(0,5) calls.
     */
    @Test
    @DisplayName("createProduct should not crash when category='AB' and name='Pen'")
    void createProduct_shouldNotCrash_whenInputsShorterThanSubstringLengths() {
        // Given
        Category category = Category.builder()
                .categoryId(1L).name("AB").image("cat.jpg").desc("Short category").active(true).build();
        Product product = Product.builder()
                .name("Pen").image("pen.jpg").desc("OK").price(5.0).discount(0).qtyInStock(10)
                .category(Category.builder().categoryId(1L).build()).build();
        ProductDto inputDto = ProductDto.builder()
                .name("Pen").image("pen.jpg").desc("OK").price(5.0).discount(0).qtyInStock(10).categoryId(1L).build();
        ProductDto outputDto = ProductDto.builder()
                .productId(1L).name("Pen").image("pen.jpg").desc("OK").price(5.0).discount(0).qtyInStock(10).categoryId(1L).build();

        given(modelMapper.map(inputDto, Product.class)).willReturn(product);
        given(categoryRepo.findById(1L)).willReturn(Optional.of(category));
        given(productRepo.save(any(Product.class))).willAnswer(inv -> inv.getArgument(0));
        given(modelMapper.map(any(Product.class), eq(ProductDto.class))).willReturn(outputDto);

        // When & Then
        assertThatNoException().isThrownBy(() -> productService.createProduct(inputDto));

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepo).save(captor.capture());
        assertThat(captor.getValue().getSku()).isEqualTo("AB-Pen-OK");
    }

    @Test
    @DisplayName("createProduct generates backward-compatible SKU for normal-length inputs")
    void createProduct_shouldGenerateCompatibleSku_whenInputsLongEnough() {
        // Given
        Category category = Category.builder()
                .categoryId(1L).name("Electronics").image("cat.jpg").desc("Gadgets").active(true).build();
        Product product = Product.builder()
                .name("Laptop Stand").image("stand.jpg").desc("Adjustable laptop stand").price(29.99).discount(5).qtyInStock(100)
                .category(Category.builder().categoryId(1L).build()).build();
        ProductDto inputDto = ProductDto.builder()
                .name("Laptop Stand").image("stand.jpg").desc("Adjustable laptop stand").price(29.99).discount(5).qtyInStock(100).categoryId(1L).build();
        ProductDto outputDto = ProductDto.builder()
                .productId(1L).name("Laptop Stand").image("stand.jpg").desc("Adjustable laptop stand")
                .price(29.99).discount(5).qtyInStock(100).categoryId(1L).build();

        given(modelMapper.map(inputDto, Product.class)).willReturn(product);
        given(categoryRepo.findById(1L)).willReturn(Optional.of(category));
        given(productRepo.save(any(Product.class))).willAnswer(inv -> inv.getArgument(0));
        given(modelMapper.map(any(Product.class), eq(ProductDto.class))).willReturn(outputDto);

        // When
        productService.createProduct(inputDto);

        // Then — must match the old format: first 3 of category, first 5 of name (spaces→X), first 3 of desc
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepo).save(captor.capture());
        assertThat(captor.getValue().getSku()).isEqualTo("Ele-Lapto-Adj");
    }

    // ---- Unit tests for generateSku (package-private) ----

    @Nested
    @DisplayName("generateSku boundary cases")
    class GenerateSkuTests {

        @Test
        @DisplayName("normal-length inputs produce original format: 3-5-3")
        void normalLengthInputs() {
            assertThat(productService.generateSku("Electronics", "Laptop Stand", "Adjustable"))
                    .isEqualTo("Ele-Lapto-Adj");
        }

        @Test
        @DisplayName("exact-length inputs (cat=3, name=5, desc=3)")
        void exactLengthInputs() {
            assertThat(productService.generateSku("Cat", "Phone", "Desc"))
                    .isEqualTo("Cat-Phone-Des");
        }

        @Test
        @DisplayName("category shorter than 3 chars")
        void shortCategory() {
            assertThat(productService.generateSku("AB", "Keyboard", "Mechanical")).isEqualTo("AB-Keybo-Mec");
        }

        @Test
        @DisplayName("product name shorter than 5 chars")
        void shortProductName() {
            assertThat(productService.generateSku("Tools", "Pen", "Writing instrument")).isEqualTo("Too-Pen-Wri");
        }

        @Test
        @DisplayName("description shorter than 3 chars")
        void shortDescription() {
            assertThat(productService.generateSku("Books", "Novel", "OK")).isEqualTo("Boo-Novel-OK");
        }

        @Test
        @DisplayName("all inputs shorter than required lengths")
        void allShortInputs() {
            assertThat(productService.generateSku("AB", "Pen", "OK")).isEqualTo("AB-Pen-OK");
        }

        @Test
        @DisplayName("single-character inputs")
        void singleCharInputs() {
            assertThat(productService.generateSku("A", "B", "C")).isEqualTo("A-B-C");
        }

        @Test
        @DisplayName("spaces in product name get replaced with X")
        void spacesInProductName() {
            assertThat(productService.generateSku("Home", "TV Set", "Entertainment"))
                    .isEqualTo("Hom-TVXSe-Ent");
        }

        @Test
        @DisplayName("spaces in description get replaced with X")
        void spacesInDescription() {
            assertThat(productService.generateSku("Home", "Chair", "A B")).isEqualTo("Hom-Chair-AXB");
        }

        @Test
        @DisplayName("spaces in short product name get replaced with X")
        void spacesInShortProductName() {
            assertThat(productService.generateSku("Cat", "A B", "Desc")).isEqualTo("Cat-AXB-Des");
        }

        @Test
        @DisplayName("very long inputs are truncated correctly")
        void veryLongInputs() {
            assertThat(productService.generateSku(
                    "Automobiles and Vehicles",
                    "Super Deluxe Racing Wheel Pro",
                    "Premium quality racing wheel for professionals"
            )).isEqualTo("Aut-Super-Pre");
        }

        @Test
        @DisplayName("SKU is deterministic — same inputs always produce same output")
        void deterministic() {
            String sku1 = productService.generateSku("Cat", "Prod Name", "Desc here");
            String sku2 = productService.generateSku("Cat", "Prod Name", "Desc here");
            assertThat(sku1).isEqualTo(sku2);
        }
    }
}
