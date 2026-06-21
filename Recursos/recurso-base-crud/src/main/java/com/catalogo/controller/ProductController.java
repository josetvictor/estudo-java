package com.catalogo.controller;

import com.catalogo.dto.ProductRequestDTO;
import com.catalogo.dto.ProductResponseDTO;
import com.catalogo.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST que expõe os endpoints para gerenciar os produtos do catálogo.
 * Mapeado sob o caminho base "/api/products".
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Endpoint para criação de um novo produto.
     *
     * @param requestDTO O DTO contendo os dados do produto.
     * @return O DTO do produto criado e o status HTTP 201 Created.
     */
    @PostMapping
    public ResponseEntity<ProductResponseDTO> createProduct(@Valid @RequestBody ProductRequestDTO requestDTO) {
        ProductResponseDTO responseDTO = productService.createProduct(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    /**
     * Endpoint para listagem de todos os produtos do catálogo.
     *
     * @return Uma lista com todos os produtos e status HTTP 200 OK.
     */
    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>> findAllProducts() {
        List<ProductResponseDTO> list = productService.findAllProducts();
        return ResponseEntity.ok(list);
    }

    /**
     * Endpoint para busca de um único produto pelo ID informado.
     *
     * @param id O identificador único (UUID) do produto.
     * @return O produto localizado e o status HTTP 200 OK.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> findProductById(@PathVariable UUID id) {
        ProductResponseDTO responseDTO = productService.findProductById(id);
        return ResponseEntity.ok(responseDTO);
    }

    /**
     * Endpoint para atualização cadastral de um produto específico.
     *
     * @param id O identificador único (UUID) do produto a ser atualizado.
     * @param requestDTO Os novos dados cadastrais para o produto.
     * @return O produto já com as modificações aplicadas e o status HTTP 200 OK.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> updateProduct(
            @PathVariable UUID id, 
            @Valid @RequestBody ProductRequestDTO requestDTO) {
        ProductResponseDTO responseDTO = productService.updateProduct(id, requestDTO);
        return ResponseEntity.ok(responseDTO);
    }

    /**
     * Endpoint para exclusão física de um produto.
     *
     * @param id O identificador único (UUID) do produto a ser excluído.
     * @return Status HTTP 204 No Content.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
