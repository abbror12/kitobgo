package com.example.kitobgo.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;


public interface ProductRepository
        extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    boolean existsByIsbn(String isbn);

    boolean existsByIsbnAndIdNot(String isbn, Long id);

    /** Kategoriyaga biror mahsulot biriktirilganmi — kategoriyani o'chirishdan oldingi tekshiruv. */
    boolean existsByCategories_Id(Long categoryId);

    Optional<Product> findByIdAndStatus(Long id, ProductStatus status);

    Page<Product> findAllByStatus(ProductStatus status, Pageable pageable);
}
