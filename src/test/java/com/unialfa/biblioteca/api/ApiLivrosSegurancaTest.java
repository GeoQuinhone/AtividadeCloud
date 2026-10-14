package com.unialfa.biblioteca.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de integracao: sobe a aplicacao inteira em uma porta aleatoria e
 * conversa com ela por HTTP, como um cliente real da API faria.
 * Cobre: bloqueio sem token, login JWT, uso do token e redirecionamento da parte web.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "api.security.token.secret=chave-somente-para-testes-automatizados-nao-usar",
                "spring.datasource.url=jdbc:h2:mem:teste-api"
        })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiLivrosSegurancaTest {

    private static final Pattern TOKEN = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"");

    @Value("${local.server.port}")
    private int porta;

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private URI uri(String caminho) {
        return URI.create("http://localhost:" + porta + caminho);
    }

    private HttpResponse<String> enviar(HttpRequest request) throws Exception {
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String obterToken() throws Exception {
        var login = HttpRequest.newBuilder(uri("/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"username\":\"admin\",\"password\":\"admin\"}"))
                .build();
        var resposta = enviar(login);
        assertThat(resposta.statusCode()).as("status do login").isEqualTo(200);

        var m = TOKEN.matcher(resposta.body());
        assertThat(m.find()).as("resposta do login deve conter o campo token").isTrue();
        return m.group(1);
    }

    @Test
    @Order(1)
    @DisplayName("API sem token responde 401")
    void apiSemTokenEhBloqueada() throws Exception {
        var resposta = enviar(HttpRequest.newBuilder(uri("/api/livros")).GET().build());

        assertThat(resposta.statusCode()).isEqualTo(401);
    }

    @Test
    @Order(2)
    @DisplayName("Token invalido responde 401")
    void tokenInvalidoEhRecusado() throws Exception {
        var resposta = enviar(HttpRequest.newBuilder(uri("/api/livros"))
                .header("Authorization", "Bearer token.invalido.123")
                .GET().build());

        assertThat(resposta.statusCode()).isEqualTo(401);
    }

    @Test
    @Order(3)
    @DisplayName("Login gera JWT; com ele e possivel cadastrar e listar livros")
    void fluxoCompletoComToken() throws Exception {
        var token = obterToken();
        assertThat(token.split("\\.")).as("JWT tem 3 partes").hasSize(3);

        // Banco vazio: a API devolve 404 na listagem (comportamento atual do LivroApi)
        var vazia = enviar(HttpRequest.newBuilder(uri("/api/livros"))
                .header("Authorization", "Bearer " + token).GET().build());
        assertThat(vazia.statusCode()).isEqualTo(404);

        var cadastro = enviar(HttpRequest.newBuilder(uri("/api/livros"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"titulo\":\"Entrega Continua\",\"isbn\":\"978-8582601037\",\"anoPublicacao\":2014}"))
                .build());
        assertThat(cadastro.statusCode()).isEqualTo(200);
        assertThat(cadastro.body()).contains("\"id\"").contains("Entrega Continua");

        var lista = enviar(HttpRequest.newBuilder(uri("/api/livros"))
                .header("Authorization", "Bearer " + token).GET().build());
        assertThat(lista.statusCode()).isEqualTo(200);
        assertThat(lista.body()).contains("Entrega Continua");
    }

    @Test
    @Order(4)
    @DisplayName("Pagina web sem login redireciona para /login")
    void paginaWebRedirecionaParaLogin() throws Exception {
        var resposta = enviar(HttpRequest.newBuilder(uri("/")).GET().build());

        assertThat(resposta.statusCode()).isEqualTo(302);
        assertThat(resposta.headers().firstValue("Location")).hasValueSatisfying(
                location -> assertThat(location).endsWith("/login"));
    }

    @Test
    @Order(5)
    @DisplayName("Pagina de login e publica")
    void paginaDeLoginEhPublica() throws Exception {
        var resposta = enviar(HttpRequest.newBuilder(uri("/login")).GET().build());

        assertThat(resposta.statusCode()).isEqualTo(200);
    }
}
