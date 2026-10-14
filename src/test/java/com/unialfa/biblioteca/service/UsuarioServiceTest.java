package com.unialfa.biblioteca.service;

import com.unialfa.biblioteca.model.Usuario;
import com.unialfa.biblioteca.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regras de senha do UsuarioService: a senha nunca pode ser gravada em texto puro
 * e um hash ja existente nao pode ser codificado de novo.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final String HASH_BCRYPT = "$2a$10$abcdefghijklmnopqrstuuJ8h0Qy0xg8yU5Qn8yqv7mJk3G2bW9a";

    @Mock
    private UsuarioRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UsuarioService service;

    @BeforeEach
    void setUp() {
        service = new UsuarioService(repository, passwordEncoder);
        when(repository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Novo usuario: senha em texto puro e codificada antes de salvar")
    void codificaSenhaEmTextoPuro() {
        when(passwordEncoder.encode("senha123")).thenReturn(HASH_BCRYPT);
        var usuario = new Usuario(null, "maria", "senha123", "Maria", "USER");

        var salvo = service.salvar(usuario);

        assertThat(salvo.getPassword()).isEqualTo(HASH_BCRYPT);
        assertThat(salvo.getPassword()).isNotEqualTo("senha123");
        verify(passwordEncoder).encode("senha123");
    }

    @Test
    @DisplayName("Senha que ja e hash BCrypt nao e codificada novamente")
    void naoRecodificaHashExistente() {
        var usuario = new Usuario(null, "joao", HASH_BCRYPT, "Joao", "USER");

        var salvo = service.salvar(usuario);

        assertThat(salvo.getPassword()).isEqualTo(HASH_BCRYPT);
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Edicao com senha em branco mantem a senha ja gravada no banco")
    void edicaoSemSenhaMantemSenhaDoBanco() {
        var noBanco = new Usuario(7L, "ana", HASH_BCRYPT, "Ana", "ADMIN");
        when(repository.findById(7L)).thenReturn(Optional.of(noBanco));
        var edicao = new Usuario(7L, "ana", "", "Ana Paula", "ADMIN");

        var salvo = service.salvar(edicao);

        assertThat(salvo.getPassword()).isEqualTo(HASH_BCRYPT);
        assertThat(salvo.getNome()).isEqualTo("Ana Paula");
        verify(passwordEncoder, never()).encode(anyString());
    }
}
