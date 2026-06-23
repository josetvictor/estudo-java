# Documentação Técnica — Testes de Integração com Testcontainers

## Sumário

1. [Visão Geral](#visão-geral)
2. [Estrutura de Arquivos Criados/Modificados](#estrutura-de-arquivos)
3. [Configuração das Dependências (pom.xml)](#configuração-das-dependências)
4. [Perfil de Teste (application-test.properties)](#perfil-de-teste)
5. [Classe Base — BaseIntegrationTest](#classe-base--baseintegrationtest)
6. [Modificações no Código de Produção](#modificações-no-código-de-produção)
7. [Cenários de Teste Implementados](#cenários-de-teste-implementados)
8. [Boas Práticas Adotadas e Justificativas](#boas-práticas-adotadas-e-justificativas)
9. [Como Executar os Testes](#como-executar-os-testes)

---

## Visão Geral

Os testes de integração foram implementados utilizando **Testcontainers** para subir um
contêiner real de **PostgreSQL** durante a execução da suite de testes. Isso garante que
o comportamento validado é idêntico ao ambiente de produção — diferentemente de bancos em
memória como H2, que possuem dialetos, tipos e comportamentos de locking distintos.

---

## Estrutura de Arquivos

```
src/
├── main/java/com/catalogo/
│   ├── repository/
│   │   └── ProductRepository.java          ← MODIFICADO: adicionado findByIdWithLock
│   └── service/
│       └── ProductService.java             ← MODIFICADO: adicionado decrementStock
│
└── test/
    ├── resources/
    │   └── application-test.properties     ← CRIADO: configurações do perfil "test"
    └── java/com/catalogo/
        ├── BaseIntegrationTest.java         ← CRIADO: classe base com Testcontainers
        └── controller/
            ├── ProductPersistenceIntegrationTest.java   ← CRIADO: Cenário 1
            ├── ProductValidationIntegrationTest.java    ← CRIADO: Cenário 2
            ├── ProductNotFoundIntegrationTest.java      ← CRIADO: Cenário 3
            ├── ProductTransactionRollbackTest.java      ← CRIADO: Cenário 4
            └── ProductConcurrencyIntegrationTest.java   ← CRIADO: Cenário 5
```

---

## Classe Base — `BaseIntegrationTest`

### Decisões de design

#### 1. Padrão Singleton de Container

```java
static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

static {
    POSTGRES.start();
}
```

**Por quê?**  
Subir um contêiner Docker é uma operação cara (2–5 segundos). Com o campo `static final`
e o bloco `static {}`, o contêiner é criado e iniciado **uma única vez por JVM**, sendo
compartilhado por todas as classes de teste. Alternativas como `@Container` com a extensão
`@Testcontainers` reiniciam o contêiner a cada classe de teste, multiplicando o tempo
de execução desnecessariamente.

#### 2. `@DynamicPropertySource`

```java
@DynamicPropertySource
static void overrideDataSourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
}
```

**Por quê?**  
O Testcontainers atribui uma porta aleatória ao contêiner na inicialização. O
`@DynamicPropertySource` é a forma oficial do Spring Boot para registrar propriedades
no `Environment` **antes** da construção do `ApplicationContext`, permitindo que o
datasource seja configurado com a URL correta do contêiner em tempo de execução.

#### 3. `@SpringBootTest(webEnvironment = RANDOM_PORT)`

**Por quê?**  
Inicia o servidor Tomcat embutido em uma porta aleatória disponível, habilitando testes
de integração que exercitam a stack HTTP completa: serialização JSON, resolução de
content-type, filtros, interceptors e o `GlobalExceptionHandler`. Isso é fundamental
para validar a camada de apresentação junto ao banco real.

#### 4. `@ActiveProfiles("test")`

**Por quê?**  
Ativa o arquivo `application-test.properties`, que sobrescreve configurações específicas
para o ambiente de teste (ex: `ddl-auto=create-drop`) sem alterar o
`application.properties` principal.

#### 5. Reutilização do `ApplicationContext` (Context Caching)

O Spring Boot reutiliza o mesmo `ApplicationContext` para todas as classes de teste que
compartilham as mesmas configurações (`@SpringBootTest`, `@ActiveProfiles`,
`@DynamicPropertySource`). Como todas as subclasses herdam exatamente as mesmas
anotações e o contêiner singleton produz sempre as mesmas credenciais (fixadas em
`withUsername("test")` / `withPassword("test")`), o contexto é criado uma única vez
para toda a suite. Isso reduz drasticamente o tempo total de execução.

#### 6. `@BeforeEach cleanDatabase()`

```java
@BeforeEach
void cleanDatabase() {
    productRepository.deleteAll();
}
```

**Por quê?**  
Garante **isolamento entre testes**. Sem essa limpeza, dados criados por um teste
contaminariam o estado do banco para os testes seguintes, produzindo falhas
não-determinísticas (flaky tests). Cada teste parte de um estado limpo e previsível.

---

## Modificações no Código de Produção

### `ProductRepository` — `findByIdWithLock`

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Product p WHERE p.id = :id")
Optional<Product> findByIdWithLock(@Param("id") UUID id);
```

**Por quê?**  
O Cenário 5 exige que a operação de decremento de estoque seja segura para ambientes
concorrentes. `PESSIMISTIC_WRITE` emite um `SELECT ... FOR UPDATE` no PostgreSQL, fazendo
com que a primeira transação a executar o comando obtenha um lock exclusivo na linha.
Outras transações que tentam o mesmo `SELECT FOR UPDATE` ficam bloqueadas até que a
primeira libere o lock (commit ou rollback). Isso serializa os decrementos e elimina
o risco de *Lost Update*.

### `ProductService` — `decrementStock`

```java
@Transactional
public void decrementStock(UUID id, int quantity) { ... }
```

**Por quê?**  
Encapsula a lógica de redução de estoque em um método de serviço transacional, garantindo
que a leitura com lock (`findByIdWithLock`), a validação de estoque suficiente e a
escrita (`save`) ocorram dentro de um único escopo transacional atômico.

---

## Cenários de Teste Implementados

### Cenário 1 — Persistência de Dados Válidos (`ProductPersistenceIntegrationTest`)

**O que testa:**
- `POST /api/products` com payload válido → `201 Created`
- Presença de `id` (UUID) e `createdAt` no corpo da resposta
- Verificação direta no `ProductRepository` (banco físico) de que a linha foi criada
- `GET /api/products/{id}` com o ID gerado → `200 OK` com os mesmos dados

**Por que é importante:**  
Valida o fluxo completo de persistência: mapeamento DTO → entidade → banco → entidade
→ DTO de resposta. Confirma que as conversões e a geração automática de campos estão corretas.

---

### Cenário 2 — Dados Inválidos (`ProductValidationIntegrationTest`)

**O que testa:**  
Cada campo obrigatório/com restrição é testado individualmente em requisições `POST`:
- `name` nulo ou em branco → `422 Unprocessable Entity`
- `price` nulo, zero ou negativo → `422 Unprocessable Entity`
- `stockQuantity` nulo ou negativo → `422 Unprocessable Entity`

**Estrutura do corpo esperado:**
```json
{
  "status": 422,
  "error": "Falha na validação de campos",
  "errors": [
    { "fieldName": "price", "message": "O preço do produto deve ser maior que zero." }
  ]
}
```

**Por que é importante:**  
Garante que o `GlobalExceptionHandler` + Bean Validation estão integrados e respondendo
com a estrutura e mensagens corretas. Confirma também que nenhum dado inválido chegou a
ser persistido no banco.

---

### Cenário 3 — Recursos Inexistentes (`ProductNotFoundIntegrationTest`)

**O que testa:**
- `GET /api/products/{id}` com UUID aleatório → `404 Not Found`
- `PUT /api/products/{id}` com UUID aleatório → `404 Not Found`
- `DELETE /api/products/{id}` com UUID aleatório → `404 Not Found`

**Estrutura do corpo esperado:**
```json
{
  "status": 404,
  "error": "Recurso não encontrado",
  "message": "Produto não encontrado com o ID: <uuid>",
  "path": "/api/products/<uuid>",
  "timestamp": "..."
}
```

**Por que é importante:**  
Valida que o tratamento de erro REST é consistente para todos os verbos HTTP que operam
por ID, e que o corpo da resposta segue o contrato definido por `StandardError`.

---

### Cenário 4 — Rollback de Transação (`ProductTransactionRollbackTest`)

**O que testa:**  
Que uma `RuntimeException` lançada dentro de um escopo `@Transactional` causa o rollback
automático de todos os `save()` executados até aquele ponto na mesma transação.

**Técnica utilizada — `TransactionTemplate`:**
```java
TransactionTemplate template = new TransactionTemplate(transactionManager);
assertThatThrownBy(() -> template.execute(status -> {
    productRepository.save(product);  // salvo dentro da transação
    throw new RuntimeException("Forçando rollback");  // causa rollback
}));

assertThat(productRepository.count()).isZero();  // confirmação do rollback
```

**Por que `TransactionTemplate` e não um endpoint HTTP?**  
Com `RANDOM_PORT`, as requisições HTTP são processadas em threads separadas gerenciadas
pelo Tomcat. O JUnit não consegue interceptar ou controlar a transação nessas threads.
O `TransactionTemplate` permite controlar explicitamente o ciclo de vida da transação
**na mesma thread do teste**, tornando o cenário determinístico e reproduzível.

**Por que é importante:**  
Prova que o Spring Data JPA + PostgreSQL (driver nativo) está configurado corretamente
para o gerenciamento transacional. Esse comportamento não pode ser adequadamente validado
com H2, pois o comportamento de rollback com drivers nativos pode diferir.

---

### Cenário 5 — Controle de Concorrência (`ProductConcurrencyIntegrationTest`)

**O que testa:**  
Que 10 threads concorrentes conseguem decrementar o estoque de um produto de 10 para 0
sem race conditions, graças ao `PESSIMISTIC_WRITE` lock.

**Técnica utilizada — `CountDownLatch` + `ExecutorService`:**
```java
CountDownLatch startLatch = new CountDownLatch(1);  // sinal de largada
CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);  // aguarda conclusão

// Todas as threads aguardam o sinal simultâneo
startLatch.await();
productService.decrementStock(productId, 1);

// Dispara todas ao mesmo tempo
startLatch.countDown();
```

**Por que `CountDownLatch`?**  
O latch de início faz com que todas as threads fiquem prontas e aguardando antes de
serem liberadas simultaneamente com um único `countDown()`, maximizando a concorrência
real e aumentando a probabilidade de expor race conditions caso o lock não estivesse
implementado.

**Por que é importante e só funciona com PostgreSQL real?**  
H2 em memória não implementa `SELECT FOR UPDATE` com a mesma semântica de bloqueio do
PostgreSQL. Apenas com um banco real é possível validar que o `PESSIMISTIC_WRITE` está
de fato serializando os acessos concorrentes e prevenindo o problema de *Lost Update*.

---

## Boas Práticas Adotadas e Justificativas

| Prática | Onde aplicada | Justificativa |
|---|---|---|
| **Container Singleton** | `BaseIntegrationTest` | Inicia o contêiner 1x por JVM, evitando overhead de Docker a cada classe de teste |
| **Context Caching** | Configurações compartilhadas em `BaseIntegrationTest` | O Spring reutiliza o `ApplicationContext`, reduzindo tempo de boot do Spring |
| **`@BeforeEach cleanDatabase()`** | `BaseIntegrationTest` | Isolamento: cada teste parte de um estado limpo, eliminando flaky tests por dados residuais |
| **`@DynamicPropertySource`** | `BaseIntegrationTest` | Injeta a URL dinâmica do contêiner no ambiente Spring antes da construção do contexto |
| **`@ActiveProfiles("test")`** | `BaseIntegrationTest` | Separa configurações de teste do `application.properties` principal |
| **`ddl-auto=create-drop`** | `application-test.properties` | Garante schema sempre sincronizado com entidades; descarta tudo ao final |
| **Driver PostgreSQL em `scope=test`** | `pom.xml` | Não polui o artefato de produção, que usa H2 |
| **Versões gerenciadas pelo BOM** | `pom.xml` | Evita conflitos de versão; Spring Boot garante compatibilidade entre suas dependências |
| **`TestRestTemplate`** | Testes de cenários 1–3 | Realiza chamadas HTTP reais ao servidor em `RANDOM_PORT`, testando a stack completa |
| **Testes individuais por campo** | Cenário 2 | Cada campo inválido em um teste separado: falhas são precisas e diagnóstico é imediato |
| **`TransactionTemplate`** | Cenário 4 | Único jeito de controlar transações na mesma thread do teste com `RANDOM_PORT` |
| **`PESSIMISTIC_WRITE`** | `ProductRepository`, Cenário 5 | Previne race conditions em estoque; validado apenas com banco real (PostgreSQL) |
| **`CountDownLatch`** | Cenário 5 | Maximiza concorrência real liberando todas as threads simultaneamente |
| **Teste de controle** | Cenários 4 e 5 | Cada cenário inclui um caso positivo (sem falha) para confirmar que o fluxo normal funciona |

---

## Como Executar os Testes

**Pré-requisito:** Docker instalado e em execução na máquina.

```bash
# Executar toda a suite de testes de integração
./mvnw verify

# Executar apenas os testes de integração (excluindo unit tests, se houver)
./mvnw test -Dtest="*IntegrationTest,*RollbackTest,*ConcurrencyTest"

# Executar um cenário específico
./mvnw test -Dtest="ProductPersistenceIntegrationTest"
./mvnw test -Dtest="ProductValidationIntegrationTest"
./mvnw test -Dtest="ProductNotFoundIntegrationTest"
./mvnw test -Dtest="ProductTransactionRollbackTest"
./mvnw test -Dtest="ProductConcurrencyIntegrationTest"
```

> O Testcontainers gerencia automaticamente o ciclo de vida do contêiner Docker:
> ele é baixado (se necessário), iniciado antes dos testes e encerrado ao final —
> tudo sem nenhuma configuração manual.
