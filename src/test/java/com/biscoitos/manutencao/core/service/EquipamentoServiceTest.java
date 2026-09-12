package com.biscoitos.manutencao.core.service;

import com.biscoitos.manutencao.common.exception.DuplicidadeException;
import com.biscoitos.manutencao.common.exception.EntidadeNaoEncontradaException;
import com.biscoitos.manutencao.common.exception.HorometroRetrocessoException;
import com.biscoitos.manutencao.core.domain.Equipamento;
import com.biscoitos.manutencao.core.domain.LinhaProducao;
import com.biscoitos.manutencao.core.domain.StatusEquipamento;
import com.biscoitos.manutencao.core.repository.EquipamentoRepository;
import com.biscoitos.manutencao.core.repository.LinhaProducaoRepository;
import com.biscoitos.manutencao.core.web.dto.EquipamentoRequest;
import com.biscoitos.manutencao.core.web.dto.EquipamentoUpdateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EquipamentoServiceTest {

    @Mock
    private EquipamentoRepository equipamentoRepository;
    @Mock
    private LinhaProducaoRepository linhaProducaoRepository;

    @InjectMocks
    private EquipamentoService equipamentoService;

    @Test
    void deveRecusarCriarComCodigoDuplicado() {
        when(equipamentoRepository.existsByCodigo("MIST-01")).thenReturn(true);

        EquipamentoRequest request = new EquipamentoRequest("MIST-01", "Misturadora", "Mistura", UUID.randomUUID());

        assertThatThrownBy(() -> equipamentoService.criar(request))
                .isInstanceOf(DuplicidadeException.class);
    }

    @Test
    void deveAtualizarNomeSetorELinha() {
        UUID equipamentoId = UUID.randomUUID();
        UUID novaLinhaId = UUID.randomUUID();

        LinhaProducao linhaAntiga = LinhaProducao.builder().id(UUID.randomUUID()).nome("Linha 1").build();
        Equipamento existente = Equipamento.builder()
                .id(equipamentoId)
                .codigo("MIST-01")
                .nome("Nome antigo")
                .setor("Setor antigo")
                .linhaProducao(linhaAntiga)
                .status(StatusEquipamento.ATIVO)
                .build();

        LinhaProducao novaLinha = LinhaProducao.builder().id(novaLinhaId).nome("Linha 2").build();

        when(equipamentoRepository.findByIdComLinhaProducao(equipamentoId)).thenReturn(Optional.of(existente));
        when(linhaProducaoRepository.findById(novaLinhaId)).thenReturn(Optional.of(novaLinha));
        when(equipamentoRepository.save(any(Equipamento.class))).thenAnswer(inv -> inv.getArgument(0));

        EquipamentoUpdateRequest request = new EquipamentoUpdateRequest("Nome novo", "Setor novo", novaLinhaId);

        Equipamento atualizado = equipamentoService.atualizar(equipamentoId, request);

        assertThat(atualizado.getNome()).isEqualTo("Nome novo");
        assertThat(atualizado.getSetor()).isEqualTo("Setor novo");
        assertThat(atualizado.getLinhaProducao().getId()).isEqualTo(novaLinhaId);
        // codigo não muda no update — é a chave de negócio (ver EquipamentoUpdateRequest)
        assertThat(atualizado.getCodigo()).isEqualTo("MIST-01");
    }

    @Test
    void deveFalharAoAtualizarEquipamentoInexistente() {
        UUID equipamentoId = UUID.randomUUID();
        when(equipamentoRepository.findByIdComLinhaProducao(equipamentoId)).thenReturn(Optional.empty());

        EquipamentoUpdateRequest request = new EquipamentoUpdateRequest("Nome", "Setor", UUID.randomUUID());

        assertThatThrownBy(() -> equipamentoService.atualizar(equipamentoId, request))
                .isInstanceOf(EntidadeNaoEncontradaException.class);
    }

    @Test
    void deveInativarEquipamento() {
        UUID equipamentoId = UUID.randomUUID();
        Equipamento existente = Equipamento.builder()
                .id(equipamentoId)
                .status(StatusEquipamento.ATIVO)
                .build();

        when(equipamentoRepository.findByIdComLinhaProducao(equipamentoId)).thenReturn(Optional.of(existente));
        when(equipamentoRepository.save(any(Equipamento.class))).thenAnswer(inv -> inv.getArgument(0));

        Equipamento inativado = equipamentoService.inativar(equipamentoId);

        assertThat(inativado.getStatus()).isEqualTo(StatusEquipamento.INATIVO);
    }

    @Test
    void deveReativarEquipamento() {
        UUID equipamentoId = UUID.randomUUID();
        Equipamento existente = Equipamento.builder()
                .id(equipamentoId)
                .status(StatusEquipamento.INATIVO)
                .build();

        when(equipamentoRepository.findByIdComLinhaProducao(equipamentoId)).thenReturn(Optional.of(existente));
        when(equipamentoRepository.save(any(Equipamento.class))).thenAnswer(inv -> inv.getArgument(0));

        Equipamento ativado = equipamentoService.ativar(equipamentoId);

        assertThat(ativado.getStatus()).isEqualTo(StatusEquipamento.ATIVO);
    }

    @Test
    void deveAtualizarHorasDeOperacao() {
        // US18 (Preventiva)
        UUID equipamentoId = UUID.randomUUID();
        Equipamento existente = Equipamento.builder()
                .id(equipamentoId)
                .horasOperacaoAcumuladas(100)
                .build();

        when(equipamentoRepository.findByIdComLinhaProducao(equipamentoId)).thenReturn(Optional.of(existente));
        when(equipamentoRepository.save(any(Equipamento.class))).thenAnswer(inv -> inv.getArgument(0));

        Equipamento atualizado = equipamentoService.atualizarHorasOperacao(equipamentoId, 150);

        assertThat(atualizado.getHorasOperacaoAcumuladas()).isEqualTo(150);
    }

    @Test
    void deveAceitarInformarOMesmoValorDeHoras() {
        // Igual não é retrocesso — RN09 só bloqueia valor MENOR que o atual.
        UUID equipamentoId = UUID.randomUUID();
        Equipamento existente = Equipamento.builder()
                .id(equipamentoId)
                .horasOperacaoAcumuladas(100)
                .build();

        when(equipamentoRepository.findByIdComLinhaProducao(equipamentoId)).thenReturn(Optional.of(existente));
        when(equipamentoRepository.save(any(Equipamento.class))).thenAnswer(inv -> inv.getArgument(0));

        Equipamento atualizado = equipamentoService.atualizarHorasOperacao(equipamentoId, 100);

        assertThat(atualizado.getHorasOperacaoAcumuladas()).isEqualTo(100);
    }

    @Test
    void deveRecusarRetrocederHorasDeOperacao() {
        // RN09
        UUID equipamentoId = UUID.randomUUID();
        Equipamento existente = Equipamento.builder()
                .id(equipamentoId)
                .horasOperacaoAcumuladas(200)
                .build();

        when(equipamentoRepository.findByIdComLinhaProducao(equipamentoId)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> equipamentoService.atualizarHorasOperacao(equipamentoId, 150))
                .isInstanceOf(HorometroRetrocessoException.class);
    }

    @Test
    void deveAtualizarTempoCicloIdeal() {
        // US28 (Dashboard de OEE)
        UUID equipamentoId = UUID.randomUUID();
        Equipamento existente = Equipamento.builder().id(equipamentoId).build();

        when(equipamentoRepository.findByIdComLinhaProducao(equipamentoId)).thenReturn(Optional.of(existente));
        when(equipamentoRepository.save(any(Equipamento.class))).thenAnswer(inv -> inv.getArgument(0));

        Equipamento atualizado = equipamentoService.atualizarTempoCicloIdeal(equipamentoId, 42);

        assertThat(atualizado.getTempoCicloIdealSegundos()).isEqualTo(42);
    }
}
