package com.unialfa.biblioteca;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifica se o contexto Spring sobe com a configuracao completa.
 * O segredo JWT usado aqui existe SOMENTE para testes: em execucao real ele
 * vem da variavel de ambiente JWT_SECRET (ver application.properties).
 */
@SpringBootTest(properties = {
        "api.security.token.secret=chave-somente-para-testes-automatizados-nao-usar",
        "spring.datasource.url=jdbc:h2:mem:teste-contexto"
})
class BibliotecaApplicationTests {

	@Test
	void contextLoads() {
	}

}
