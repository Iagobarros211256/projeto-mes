package com.biscoitos.manutencao.core.security;

import com.biscoitos.manutencao.common.exception.EntidadeNaoEncontradaException;
import com.biscoitos.manutencao.core.domain.Usuario;
import com.biscoitos.manutencao.core.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UsuarioLogadoResolver {

    private final UsuarioRepository usuarioRepository;

    /** O "username" do Spring Security aqui é o e-mail (ver UsuarioDetailsService). */
    public Usuario resolver(Authentication authentication) {
        String email = authentication.getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Usuário", email));
    }
}
