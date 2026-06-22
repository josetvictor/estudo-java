package com.catalogo;

import com.catalogo.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Classe base abstrata para todos os testes de integração com Testcontainers.
 *
 * <p><b>Padrão Singleton de Container</b>: o PostgreSQLContainer é declarado como
 * campo {@code static final} e iniciado no bloco estático. Isso garante que apenas
 * UM contêiner seja criado por JVM (e não um por classe de teste), maximizando
 * a reutilização e reduzindo drasticamente o tempo total de execução da suite.
 *
 * <p><b>@DynamicPropertySource</b>: injeta dinamicamente as propriedades
 * {@code spring.datasource.*} no {@code Environment} do Spring ANTES de o
 * ApplicationContext ser construído, conectando a aplicação ao contêiner real.
 *
 * <p><b>Reutilização de ApplicationContext</b>: como todas as subclasses compartilham
 * as mesmas anotações e propriedades, o Spring Boot reutiliza o mesmo contexto de
 * aplicação (context caching), evitando a recriação do contexto a cada teste.
 *
 * <p><b>@BeforeEach cleanDatabase()</b>: garante isolamento entre os testes limpando
 * o banco antes de cada método de teste, prevenindo que dados residuais causem falhas.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    // -----------------------------------------------------------------------
    // Singleton Container — iniciado uma única vez por JVM
    // -----------------------------------------------------------------------
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    static {
        POSTGRES.start();
    }

    // -----------------------------------------------------------------------
    // Injeção dinâmica das propriedades de datasource no contexto do Spring
    // -----------------------------------------------------------------------
    @DynamicPropertySource
    static void overrideDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    // -----------------------------------------------------------------------
    // Dependências compartilhadas disponíveis para todas as subclasses
    // -----------------------------------------------------------------------
    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected ProductRepository productRepository;

    // -----------------------------------------------------------------------
    // Isolamento: limpa o banco antes de cada teste para evitar poluição de dados
    // -----------------------------------------------------------------------
    @BeforeEach
    void cleanDatabase() {
        productRepository.deleteAll();
    }
}
