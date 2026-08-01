package com.biscoitos.manutencao.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Log de auditoria genérico (RNF03) — qualquer módulo pode registrar aqui, não é
 * específico de Paradas. Por isso "entidade" é uma string livre (nome da tabela/entidade
 * auditada) em vez de uma FK tipada: essa tabela precisa sobreviver a entidades que ainda
 * nem existem (OS, Estoque, Preventiva — Fases 2/3).
 *
 * usuarioId é UUID puro, não @ManyToOne para Usuario: quem grava o log já tem o UUID em
 * mãos (vem do usuário autenticado) e não precisa carregar o Usuario inteiro só pra logar.
 */
@Entity
@Table(name = "log_auditoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogAuditoria {

    @Id
    @GeneratedValue
    private UUID id;

    private String entidade;

    @Column(name = "entidade_id")
    private UUID entidadeId;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    private String acao;

    @Column(name = "data_hora")
    @Builder.Default
    private Instant dataHora = Instant.now();
}
