package com.vedasole.ekartecommercebackend.service.service_impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Unit tests for the SKU generation logic in {@link ProductServiceImpl}.
 * <p>
 * Verifies that SKU generation is safe for arbitrary-length inputs (including very short
 * and null descriptions), deterministic, and produces a human-readable format compatible
 * with the existing SKU pattern.
 */
class ProductServiceImplSkuTest {

    // ===== Bug-reproduction tests (short inputs that previously caused StringIndexOutOfBoundsException) =====

    @Test
    void generateSku_shortCategoryName_shouldNotCrash() {
        // "AB" is only 2 chars, less than the required 3-char category segment
        assertThatNoException().isThrownBy(
                () -> ProductServiceImpl.generateSku("AB", "Pen", "A nice pen"));
    }

    @Test
    void generateSku_shortProductName_shouldNotCrash() {
        // "Pen" is only 3 chars, less than the required 5-char name segment
        assertThatNoException().isThrownBy(
                () -> ProductServiceImpl.generateSku("Electronics", "Pen", "A nice pen"));
    }

    @Test
    void generateSku_shortDesc_shouldNotCrash() {
        // "Hi" is only 2 chars, less than the required 3-char desc segment
        assertThatNoException().isThrownBy(
                () -> ProductServiceImpl.generateSku("Electronics", "Phone", "Hi"));
    }

    @Test
    void generateSku_nullDesc_shouldNotCrash() {
        // desc can be null in the Product entity (no @NotNull)
        assertThatNoException().isThrownBy(
                () -> ProductServiceImpl.generateSku("Electronics", "Phone", null));
    }

    @Test
    void generateSku_allInputsTooShort_shouldNotCrash() {
        assertThatNoException().isThrownBy(
                () -> ProductServiceImpl.generateSku("A", "B", "C"));
    }

    // ===== Exact-output / format tests =====

    @Test
    void generateSku_shortCategoryName_padsWithX() {
        // "AB" -> "ABX" (padded to 3 chars)
        String sku = ProductServiceImpl.generateSku("AB", "Phone", "Good one");
        assertThat(sku).isEqualTo("ABX-Phone-Goo");
    }

    @Test
    void generateSku_singleCharCategory_padsWithXX() {
        // "A" -> "AXX" (padded to 3 chars)
        String sku = ProductServiceImpl.generateSku("A", "Phone", "Good one");
        assertThat(sku).isEqualTo("AXX-Phone-Goo");
    }

    @Test
    void generateSku_shortProductName_padsWithX() {
        // "Pen" -> "PenXX" (padded to 5 chars)
        String sku = ProductServiceImpl.generateSku("Electronics", "Pen", "A nice pen");
        assertThat(sku).isEqualTo("Ele-PenXX-AXn");
    }

    @Test
    void generateSku_singleCharName_padsWithXXXX() {
        // "P" -> "PXXXX" (padded to 5 chars)
        String sku = ProductServiceImpl.generateSku("Electronics", "P", "Description");
        assertThat(sku).isEqualTo("Ele-PXXXX-Des");
    }

    @Test
    void generateSku_shortDesc_padsWithX() {
        // "Hi" -> "HiX" (padded to 3 chars)
        String sku = ProductServiceImpl.generateSku("Electronics", "Phone", "Hi");
        assertThat(sku).isEqualTo("Ele-Phone-HiX");
    }

    @Test
    void generateSku_singleCharDesc_padsWithXX() {
        // "H" -> "HXX" (padded to 3 chars)
        String sku = ProductServiceImpl.generateSku("Electronics", "Phone", "H");
        assertThat(sku).isEqualTo("Ele-Phone-HXX");
    }

    @Test
    void generateSku_nullDesc_treatedAsEmptyAndPadded() {
        // null -> "" -> "XXX" (padded to 3 chars)
        String sku = ProductServiceImpl.generateSku("Electronics", "Phone", null);
        assertThat(sku).isEqualTo("Ele-Phone-XXX");
    }

    @Test
    void generateSku_emptyDesc_paddedWithXXX() {
        String sku = ProductServiceImpl.generateSku("Electronics", "Phone", "");
        assertThat(sku).isEqualTo("Ele-Phone-XXX");
    }

    @Test
    void generateSku_normalInputs_unchangedBehavior() {
        // Standard case: category "Electronics" (>=3), name "Phone" (>=5), desc "Latest model" (>=3)
        String sku = ProductServiceImpl.generateSku("Electronics", "Phone", "Latest model");
        assertThat(sku).isEqualTo("Ele-Phone-Lat");
    }

    @Test
    void generateSku_longInputs_truncatesCorrectly() {
        String sku = ProductServiceImpl.generateSku("Electronics", "Smartphone Pro", "The best device ever made");
        // cat: "Ele", name: "Smart", desc: "The"
        assertThat(sku).isEqualTo("Ele-Smart-The");
    }

    @Test
    void generateSku_exactlyMinimumLengths_noTruncationOrPadding() {
        // cat "Cat" (3), name "12345" (5), desc "ABC" (3)
        String sku = ProductServiceImpl.generateSku("Cat", "12345", "ABC");
        assertThat(sku).isEqualTo("Cat-12345-ABC");
    }

    // ===== Space handling =====

    @Test
    void generateSku_spacesInName_replacedWithX() {
        // "A B C" (5 chars) -> spaces replaced -> "AXBXC"
        String sku = ProductServiceImpl.generateSku("Category", "A B C", "Description");
        assertThat(sku).isEqualTo("Cat-AXBXC-Des");
    }

    @Test
    void generateSku_spacesInDesc_replacedWithX() {
        // "A B" (3 chars) -> spaces replaced -> "AXB"
        String sku = ProductServiceImpl.generateSku("Category", "Phone", "A B");
        assertThat(sku).isEqualTo("Cat-Phone-AXB");
    }

    @Test
    void generateSku_spacesInShortName_paddedAfterReplacement() {
        // "A B" (3 chars) -> pad to 5 -> "A BXX" -> replace spaces -> "AXBXX"
        String sku = ProductServiceImpl.generateSku("Category", "A B", "Description");
        assertThat(sku).isEqualTo("Cat-AXBXX-Des");
    }

    @Test
    void generateSku_leadingSpaceInName_replacedWithX() {
        // " Phone" -> take 5 -> " Phon" -> replace spaces -> "XPhon"
        String sku = ProductServiceImpl.generateSku("Category", " Phone", "Description");
        assertThat(sku).isEqualTo("Cat-XPhon-Des");
    }

    // ===== Determinism =====

    @Test
    void generateSku_sameInputs_producesSameOutput() {
        String sku1 = ProductServiceImpl.generateSku("Electronics", "Phone", "Latest model");
        String sku2 = ProductServiceImpl.generateSku("Electronics", "Phone", "Latest model");
        assertThat(sku1).isEqualTo(sku2);
    }

    @Test
    void generateSku_differentInputs_producesDifferentOutput() {
        String sku1 = ProductServiceImpl.generateSku("Electronics", "Phone", "Latest model");
        String sku2 = ProductServiceImpl.generateSku("Clothing", "Shirt", "Cotton fabric");
        assertThat(sku1).isNotEqualTo(sku2);
    }

    // ===== SKU format structure =====

    @Test
    void generateSku_alwaysHasTwoDashes() {
        String sku = ProductServiceImpl.generateSku("A", "B", "C");
        long dashCount = sku.chars().filter(c -> c == '-').count();
        assertThat(dashCount).isEqualTo(2);
    }

    @ParameterizedTest
    @CsvSource({
            "Electronics, Smartphone, Great device",
            "AB, Pen, Hi",
            "A, B, C",
            "Category, Name With Spaces, A B C"
    })
    void generateSku_variousInputs_baseFormatHasCorrectSegmentLengths(String cat, String name, String desc) {
        String sku = ProductServiceImpl.generateSku(cat, name, desc);
        String[] parts = sku.split("-");
        assertThat(parts).hasSize(3);
        assertThat(parts[0]).hasSize(3);
        assertThat(parts[1]).hasSize(5);
        assertThat(parts[2]).hasSize(3);
    }

    @Test
    void generateSku_noSpacesInOutput() {
        String sku = ProductServiceImpl.generateSku("My Category", "My Product Name", "My Description Here");
        assertThat(sku).doesNotContain(" ");
    }

    // ===== Additional edge cases =====

    @Test
    void generateSku_specialCharactersInInput_truncatedCorrectly() {
        // "Cat-1" take 3 = "Cat", "Phone-2" take 5 = "Phone", "Desc-3" take 3 = "Des"
        String sku = ProductServiceImpl.generateSku("Cat-1", "Phone-2", "Desc-3");
        assertThat(sku).isEqualTo("Cat-Phone-Des");
    }

    @Test
    void generateSku_unicodeInput_handledSafely() {
        // Unicode chars should not crash (even if output is not ideal)
        assertThatNoException().isThrownBy(
                () -> ProductServiceImpl.generateSku("类目", "商品名", "描述文字"));
    }

    @Test
    void generateSku_categoryNameExactlyTwoChars() {
        String sku = ProductServiceImpl.generateSku("AB", "Phone", "Description");
        assertThat(sku).startsWith("ABX-");
    }

    @Test
    void generateSku_productNameExactlyFourChars() {
        String sku = ProductServiceImpl.generateSku("Category", "ABCD", "Description");
        // "ABCD" -> pad to 5 -> "ABCDX"
        assertThat(sku).isEqualTo("Cat-ABCDX-Des");
    }

    // ===== safeSegment direct tests =====

    @Test
    void safeSegment_nullValue_returnsPaddedXs() {
        assertThat(ProductServiceImpl.safeSegment(null, 3)).isEqualTo("XXX");
    }

    @Test
    void safeSegment_emptyValue_returnsPaddedXs() {
        assertThat(ProductServiceImpl.safeSegment("", 5)).isEqualTo("XXXXX");
    }

    @Test
    void safeSegment_valueLongerThanMax_truncates() {
        assertThat(ProductServiceImpl.safeSegment("Hello World", 5)).isEqualTo("Hello");
    }

    @Test
    void safeSegment_valueExactlyMax_returnsAsIs() {
        assertThat(ProductServiceImpl.safeSegment("Hello", 5)).isEqualTo("Hello");
    }

    @Test
    void safeSegment_valueShorterThanMax_padsWithX() {
        assertThat(ProductServiceImpl.safeSegment("Hi", 5)).isEqualTo("HiXXX");
    }
}
