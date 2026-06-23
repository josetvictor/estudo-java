Para a pratica de testes de integração utilizando testcontainers foi feito os seguintes passos:

1. Adicionado algumas dependências no pom.xml, como o testcontainers e o driver do PostgreSQL.

```xml
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>

    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <scope>test</scope>
    </dependency>
```
2. Foi criado uma classe base abstrata `BaseIntegrationTest` para centralizar a configuração do Testcontainers e do Spring Boot, garantindo que todos os testes de integração compartilhem o mesmo contêiner PostgreSQL e contexto de aplicação.

3. Foi criado um arquivo de propriedades `application-test.properties` para configurar o banco de dados PostgreSQL e o Hibernate, garantindo que os testes de integração utilizem o banco correto e exibam as consultas SQL no console.

4. Os testes de integração criados foram seguindo o plano de teste descrito em TESTCONTAINERS-SCENARIOS.md, garantindo que todas as funcionalidades do sistema sejam validadas de forma isolada e confiável. Como era uma aplicação de estudo, o foco era o uso da ferramenta Testcontainers para simular o ambiente de produção e validar o comportamento do sistema em condições controladas.

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

> Por alguma razão os testes não estão rodando, está dando um erro no restTemplate, mas não consegui identificar o motivo. Acredito que seja algum problema de configuração do Spring Boot ou do Testcontainers que eu preciso entender melhor. Por isso, não consegui rodar os testes de integração, mas o código está pronto para ser executado assim que o problema for resolvido.