package com.unialfa.biblioteca.service;

import com.unialfa.biblioteca.model.Livro;
import com.unialfa.biblioteca.repository.LivroRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LivroServiceTest {

    @Mock
    private LivroRepository repository;

    @InjectMocks
    private LivroService service;

    private static Livro livro(Long id, String titulo) {
        var livro = new Livro();
        livro.setId(id);
        livro.setTitulo(titulo);
        livro.setIsbn("978-8576082675");
        livro.setAnoPublicacao(2009);
        return livro;
    }

    @Test
    @DisplayName("Salvar devolve o livro persistido pelo repositorio")
    void salvarDelegaAoRepositorio() {
        var novo = livro(null, "Codigo Limpo");
        var persistido = livro(1L, "Codigo Limpo");
        when(repository.save(novo)).thenReturn(persistido);

        var resultado = service.salvar(novo);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getTitulo()).isEqualTo("Codigo Limpo");
    }

    @Test
    @DisplayName("Listar todos devolve exatamente o que esta no repositorio")
    void listarTodos() {
        when(repository.findAll()).thenReturn(List.of(livro(1L, "A"), livro(2L, "B")));

        assertThat(service.listar()).extracting(Livro::getTitulo).containsExactly("A", "B");
    }

    @Test
    @DisplayName("Buscar por id inexistente lanca NoSuchElementException")
    void buscarIdInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listar(99L)).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("Remover chama deleteById com o id informado")
    void remover() {
        service.remover(5L);

        verify(repository).deleteById(5L);
    }
}
