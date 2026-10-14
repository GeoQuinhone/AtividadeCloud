package com.unialfa.biblioteca.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioTest {

    @Test
    @DisplayName("Privilegio ADMIN gera a authority ROLE_ADMIN")
    void adminRecebeRoleAdmin() {
        var usuario = new Usuario(1L, "admin", "x", "Admin", "ADMIN");

        assertThat(usuario.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Qualquer outro privilegio gera ROLE_USER")
    void demaisRecebemRoleUser() {
        var usuario = new Usuario(2L, "user", "x", "User", "USER");

        assertThat(usuario.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("O username usado pelo Spring Security e o login")
    void usernameEhOLogin() {
        var usuario = new Usuario(3L, "maria", "x", "Maria", "USER");

        assertThat(usuario.getUsername()).isEqualTo("maria");
    }
}
