# Cenários de Teste de Integração com Testcontainers 🐳

Este documento descreve cenários ideais de testes de integração para serem implementados no projeto base "Catálogo de Produtos" utilizando **Spring Boot** e **Testcontainers**. O Testcontainers permite que você execute contêineres reais (como PostgreSQL, MySQL ou Redis) durante os testes de integração, garantindo que o comportamento seja idêntico ao de produção.

---

## 🚀 Estrutura Básica do Setup com Testcontainers

Antes de iniciar os testes, configure a classe base de testes de integração para inicializar o contêiner de banco de dados (ex: PostgreSQL) e sobrescrever as propriedades do Spring de maneira dinâmica.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        postgres.start();
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

---

## 📋 Cenários Recomendados de Teste

### 1. Persistência de Dados Válidos (Fluxo Feliz)
*   **Objetivo**: Garantir que as entidades corretas sejam mapeadas, validadas e salvas corretamente no banco de dados físico.
*   **Estágios**:
    1.  Efetuar uma requisição `POST /api/products` com um corpo `ProductRequestDTO` contendo todos os dados válidos.
    2.  Verificar o status `201 Created` e os campos no JSON de retorno (como a presença de um `id` do tipo `UUID` e a data de criação `createdAt` populada).
    3.  Ir diretamente no `ProductRepository` e verificar se a linha correspondente existe no banco físico.
    4.  Fazer um `GET /api/products/{id}` usando o ID gerado e assegurar que as informações retornadas coincidem perfeitamente.

### 2. Comportamento sob Dados Inválidos (Validação e Restrições)
*   **Objetivo**: Validar se o mecanismo de interceptação de erros (`GlobalExceptionHandler`) e as anotações do Bean Validation (`@NotBlank`, `@DecimalMin`, `@Min`) estão agindo em união com o banco de dados.
*   **Estágios (Testar individualmente para cada campo)**:
    *   **Nome em Branco**: Tentar persistir um produto com o campo `name` vazio ou nulo. Esperado: Código `422 Unprocessable Entity` com detalhes da validação.
    *   **Preço Inválido**: Enviar um payload com preço do produto negativo ou igual a zero (ex: `BigDecimal.ZERO`). Esperado: Código `422 Unprocessable Entity` detalhando o campo `price`.
    *   **Quantidade de Estoque Negativa**: Enviar um estoque nulo ou de valor menor que zero (ex: `-5`). Esperado: Código `422 Unprocessable Entity` detalhando o campo `stockQuantity`.

### 3. Tratamento de Exceção para Recursos Inexistentes
*   **Objetivo**: Assegurar o tratamento amigável de erros REST quando se tenta interagir com IDs fictícios.
*   **Estágios**:
    1.  Fazer requisições aos endpoints:
        *   `GET /api/products/{id}`
        *   `PUT /api/products/{id}`
        *   `DELETE /api/products/{id}`
    2.  Utilizar um UUID aleatório que certamente não está persistido no banco de dados.
    3.  Assegurar o retorno `404 Not Found` contendo o corpo estruturado da classe `StandardError` com a mensagem exata: `"Produto não encontrado com o ID: <uuid_pesquisado>"`.

### 4. Rollback de Transação (`@Transactional` Behavior)
*   **Objetivo**: Validar a integridade transacional garantida pela anotação `@Transactional` no `ProductService`.
*   **Estágios**:
    1.  Crie um cenário de teste ou um método helper no serviço que force uma exceção em tempo de execução (`RuntimeException`) *após* salvar um produto no repositório mas *antes* do término do escopo da transação.
    2.  Execute o fluxo que gera a falha.
    3.  Verifique se o produto **não foi salvo** no banco de dados do contêiner, provando que o rollback de transação do Spring Data JPA com o driver de banco nativo funcionou corretamente.

### 5. Controle de Concorrência e Isolamento
*   **Objetivo**: Analisar o comportamento concorrente da base de dados física operando com múltiplos threads modificando o estoque de um produto de maneira simultânea.
*   **Estágios**:
    1.  Persista um produto de teste com 10 unidades de estoque (`stockQuantity = 10`).
    2.  Simule uma concorrência real simulando múltiplos threads concorrentes (ex: usando `CountDownLatch` ou `ExecutorService`) onde cada fluxo tenta efetuar a compra/redução de estoque de um produto (o que exige ler o valor atual, verificar e atualizar).
    3.  Este teste ajuda a demonstrar a necessidade e validar a implementação futura de **Pessimistic/Optimistic Locking** (`@Version` ou `@Lock(LockModeType.PESSIMISTIC_WRITE)`) para evitar o problema clássico de *Race Conditions* (estoque corrompido).

---

## 🛠️ Exemplo de Implementação de Fluxo de Erro com REST Asserts

Veja como escrever um teste de integração focado no comportamento de dados inválidos utilizando o `WebTestClient` ou o `MockMvc`:

```java
@Test
@DisplayName("Deve retornar 422 quando criar produto com preço menor ou igual a zero")
void shouldReturn422WhenPriceIsZeroOrNegative() {
    ProductRequestDTO invalidDto = ProductRequestDTO.builder()
            .name("Teclado Mecânico")
            .description("Teclado RGB")
            .price(new BigDecimal("-10.00")) // Preço Inválido
            .stockQuantity(15)
            .build();

    webTestClient.post()
            .uri("/api/products")
            .bodyValue(invalidDto)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
            .expectBody()
            .jsonPath("$.error").isEqualTo("Falha na validação de campos")
            .jsonPath("$.errors[0].fieldName").isEqualTo("price")
            .jsonPath("$.errors[0].message").isEqualTo("O preço do produto deve ser maior que zero.");
}
```

Implementando estes 5 cenários, você garante uma cobertura sólida e de extrema qualidade no seu projeto base com o Testcontainers!
