Atue como um Engenheiro de Software sênior especialista em Java e Spring Boot. 

Preciso que você crie a estrutura completa de um CRUD simples de "Catálogo de Produtos" (Product Catalog) para servir de projeto base de estudos. O objetivo principal deste projeto é que a regra de negócio seja simples e extremamente bem documentada (com Javadoc e comentários claros), pois usarei este código no futuro para aplicar testes de integração com Testcontainers.

Por favor, gere o código seguindo as melhores práticas (Clean Code, camadas bem definidas) para os seguintes componentes:

1. ENTIDADE (Domain):
- Classe `Product` com os campos: `UUID id`, `String name`, `String description`, `BigDecimal price`, `Integer stockQuantity`, `Instant createdAt`.
- Validações necessárias: Nome não pode ser vazio, preço deve ser maior que zero, estoque não pode ser negativo.

2. REPOSITÓRIO:
- Interface `ProductRepository` estendendo `JpaRepository`.

3. EXCEÇÕES CUSTOMIZADAS:
- Classe `ProductNotFoundException` e um `GlobalExceptionHandler` usando `@ControllerAdvice` para capturar as exceções e retornar respostas REST amigáveis (ex: 404 Not Found).

4. CAMADA DE SERVIÇO (Service):
- Classe `ProductService` com os métodos:
  - `createProduct(ProductDTO dto)` (Deve validar regras e definir o createdAt)
  - `findProductById(UUID id)` (Deve lançar ProductNotFoundException se não existir)
  - `findAllProducts()`
  - `updateProduct(UUID id, ProductDTO dto)`
  - `deleteProduct(UUID id)`
- Inclua documentação Javadoc em cada método explicando o que ele faz e quais exceções pode lançar.

5. CAMADA DE CONTROLADOR (Controller/REST):
- Classe `ProductController` expondo os endpoints padrão (`GET`, `POST`, `PUT`, `DELETE`) sob o path `/api/products`.
- Use DTOs (`ProductRequestDTO` e `ProductResponseDTO`) para a entrada e saída de dados.

6. DOCUMENTAÇÃO ADICIONAL:
- Crie um arquivo breve em formato Markdown com sugestões de "Cenários de Teste de Integração" ideais que eu deveria cobrir quando for aplicar o Testcontainers neste CRUD (ex: testar comportamento de concorrência, testar rollback de transação, testar persistência com dados válidos/inválidos).

Forneça o código completo, limpo e pronto para ser inserido em uma estrutura padrão do Spring Boot (Maven/Gradle).