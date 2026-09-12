package com.biscoitos.manutencao.producao.service;

import com.biscoitos.manutencao.common.exception.DadosInvalidosException;
import com.biscoitos.manutencao.common.exception.EntidadeNaoEncontradaException;
import com.biscoitos.manutencao.core.domain.Equipamento;
import com.biscoitos.manutencao.core.domain.Usuario;
import com.biscoitos.manutencao.core.repository.EquipamentoRepository;
import com.biscoitos.manutencao.core.repository.UsuarioRepository;
import com.biscoitos.manutencao.core.service.AuditoriaService;
import com.biscoitos.manutencao.producao.domain.ApontamentoProducao;
import com.biscoitos.manutencao.producao.repository.ApontamentoProducaoRepository;
import com.biscoitos.manutencao.producao.web.dto.RegistrarApontamentoRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApontamentoProducaoServiceTest {

    @Mock
    private ApontamentoProducaoRepository apontamentoProducaoRepository;
    @Mock
    private EquipamentoRepository equipamentoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private ApontamentoProducaoService apontamentoProducaoService;

    @Test
    void deveRecusarQuantidadeBoaMaiorQueProduzida() {
        // RN12
        Instant inicio = Instant.now();
        RegistrarApontamentoRequest request = new RegistrarApontamentoRequest(
                UUID.randomUUID(), inicio, inicio.plus(1, ChronoUnit.HOURS), 100, 150);

        assertThatThrownBy(() -> apontamentoProducaoService.registrar(request, UUID.randomUUID()))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    void deveRecusarDataHoraFimAnteriorOuIgualAoInicio() {
        // RN13
        Instant inicio = Instant.now();
        RegistrarApontamentoRequest request = new RegistrarApontamentoRequest(
                UUID.randomUUID(), inicio, inicio, 100, 90);

        assertThatThrownBy(() -> apontamentoProducaoService.registrar(request, UUID.randomUUID()))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    void deveRegistrarApontamentoValido() {
        UUID equipamentoId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        Instant inicio = Instant.now();

        when(equipamentoRepository.findById(equipamentoId))
                .thenReturn(Optional.of(Equipamento.builder().id(equipamentoId).nome("Forno").build()));
        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.of(Usuario.builder().id(usuarioId).nome("Ana").build()));
        when(apontamentoProducaoRepository.save(any(ApontamentoProducao.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistrarApontamentoRequest request = new RegistrarApontamentoRequest(
                equipamentoId, inicio, inicio.plus(8, ChronoUnit.HOURS), 500, 480);

        ApontamentoProducao apontamento = apontamentoProducaoService.registrar(request, usuarioId);

        assertThat(apontamento.getQuantidadeProduzida()).isEqualTo(500);
        assertThat(apontamento.getQuantidadeBoa()).isEqualTo(480);
        assertThat(apontamento.getDataRegistro()).isNotNull();
    }

    @Test
    void devePermitirQuantidadeBoaIgualAProduzida() {
        // 100% de aproveitamento não é erro — RN12 só bloqueia MAIOR que produzida.
        UUID equipamentoId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        Instant inicio = Instant.now();

        when(equipamentoRepository.findById(equipamentoId))
                .thenReturn(Optional.of(Equipamento.builder().id(equipamentoId).build()));
        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.of(Usuario.builder().id(usuarioId).build()));
        when(apontamentoProducaoRepository.save(any(ApontamentoProducao.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistrarApontamentoRequest request = new RegistrarApontamentoRequest(
                equipamentoId, inicio, inicio.plus(1, ChronoUnit.HOURS), 200, 200);

        ApontamentoProducao apontamento = apontamentoProducaoService.registrar(request, usuarioId);

        assertThat(apontamento.getQuantidadeBoa()).isEqualTo(200);
    }

    @Test
    void deveFalharAoRegistrarComEquipamentoInexistente() {
        UUID equipamentoId = UUID.randomUUID();
        Instant inicio = Instant.now();

        when(equipamentoRepository.findById(equipamentoId)).thenReturn(Optional.empty());

        RegistrarApontamentoRequest request = new RegistrarApontamentoRequest(
                equipamentoId, inicio, inicio.plus(1, ChronoUnit.HOURS), 100, 90);

        assertThatThrownBy(() -> apontamentoProducaoService.registrar(request, UUID.randomUUID()))
                .isInstanceOf(EntidadeNaoEncontradaException.class);
    }
}
