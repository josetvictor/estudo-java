package com.catalogo.service;

import com.catalogo.dto.ProductRequestDTO;
import com.catalogo.dto.ProductResponseDTO;
import com.catalogo.exception.ProductNotFoundException;
import com.catalogo.model.Product;
import com.catalogo.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Camada de serviço responsável por conter a lógica de negócio para manipulação de Produtos.
 * Intermedeia a comunicação entre a camada de apresentação (Controller) e de persistência (Repository).
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Cria um novo produto no catálogo com base nas especificações do DTO de entrada.
     * Atribui automaticamente o carimbo de data/hora atual (createdAt).
     *
     * @param dto Objeto de transferência com as informações de criação do produto.
     * @return O DTO do produto criado contendo seu ID gerado e createdAt preenchido.
     */
    @Transactional
    public ProductResponseDTO createProduct(ProductRequestDTO dto) {
        Product product = Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .stockQuantity(dto.getStockQuantity())
                .createdAt(Instant.now())
                .build();

        Product savedProduct = productRepository.save(product);
        return convertToResponseDTO(savedProduct);
    }

    /**
     * Busca um produto no banco de dados através de seu UUID.
     *
     * @param id Identificador único (UUID) do produto.
     * @return O DTO do produto correspondente encontrado.
     * @throws ProductNotFoundException se nenhum produto for localizado com o ID informado.
     */
    @Transactional(readOnly = true)
    public ProductResponseDTO findProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Produto não encontrado com o ID: " + id));
        return convertToResponseDTO(product);
    }

    /**
     * Recupera todos os produtos cadastrados no sistema.
     *
     * @return Uma lista contendo os DTOs de todos os produtos cadastrados.
     */
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> findAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Atualiza os dados de um produto pré-existente a partir do ID fornecido.
     * Mantém intactas propriedades imutáveis como ID e data de criação (createdAt).
     *
     * @param id Identificador único do produto que sofrerá alteração.
     * @param dto Os novos valores estruturados em um DTO.
     * @return O DTO contendo os dados do produto atualizado.
     * @throws ProductNotFoundException se o produto não for localizado.
     */
    @Transactional
    public ProductResponseDTO updateProduct(UUID id, ProductRequestDTO dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Produto não encontrado com o ID: " + id));

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStockQuantity(dto.getStockQuantity());

        Product updatedProduct = productRepository.save(product);
        return convertToResponseDTO(updatedProduct);
    }

    /**
     * Exclui em definitivo um produto do banco de dados a partir do ID informado.
     *
     * @param id O UUID do produto a ser deletado.
     * @throws ProductNotFoundException se o produto não estiver cadastrado.
     */
    @Transactional
    public void deleteProduct(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException("Produto não cadastrado para exclusão com o ID: " + id);
        }
        productRepository.deleteById(id);
    }

    /**
     * Decrementa o estoque de um produto de forma segura para ambientes concorrentes,
     * utilizando bloqueio pessimista (PESSIMISTIC_WRITE) para evitar race conditions.
     * Apenas uma transação por vez conseguirá ler e modificar o registro do produto.
     *
     * @param id       UUID do produto a ter o estoque reduzido.
     * @param quantity Quantidade a ser decrementada. Deve ser positiva.
     * @throws ProductNotFoundException  se o produto não for encontrado.
     * @throws IllegalStateException     se o estoque for insuficiente para a operação.
     */
    @Transactional
    public void decrementStock(UUID id, int quantity) {
        Product product = productRepository.findByIdWithLock(id)
                .orElseThrow(() -> new ProductNotFoundException("Produto não encontrado com o ID: " + id));

        if (product.getStockQuantity() < quantity) {
            throw new IllegalStateException(
                    "Estoque insuficiente para o produto com ID: " + id +
                    ". Disponível: " + product.getStockQuantity() + ", solicitado: " + quantity);
        }

        product.setStockQuantity(product.getStockQuantity() - quantity);
        productRepository.save(product);
    }

    /**
     * Converte internamente uma entidade Product em seu DTO correspondente para retorno da API.
     *
     * @param product A entidade original a ser mapeada.
     * @return O DTO correspondente instanciado.
     */
    private ProductResponseDTO convertToResponseDTO(Product product) {
        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .createdAt(product.getCreatedAt())
                .build();
    }
}
