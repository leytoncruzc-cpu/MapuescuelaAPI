package cl.mapuescuela.controller;

import cl.mapuescuela.dto.pedido.PedidoRequest;
import cl.mapuescuela.dto.pedido.PedidoResponse;
import cl.mapuescuela.service.PedidoService;
import cl.mapuescuela.entity.EstadoPedido;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {
    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse crear(
            @Valid @RequestBody PedidoRequest request
    ) {
        return pedidoService.crear(request);
    }

    @GetMapping
    public List<PedidoResponse> listar() {
        return pedidoService.listar();
    }

    @GetMapping("/{id}")
    public PedidoResponse buscarPorId(
            @PathVariable Long id
    ) {
        return pedidoService.buscarPorId(id);
    }

    @GetMapping("/codigo/{codigo}")
    public PedidoResponse buscarPorCodigo(
            @PathVariable String codigo
    ) {
        return pedidoService.buscarPorCodigo(codigo);
    }

    @GetMapping("/estado/{estado}")
    public List<PedidoResponse> listarPorEstado(
            @PathVariable EstadoPedido estado
    ) {
        return pedidoService.listarPorEstado(estado);
    }

    @PutMapping("/{id}")
    public PedidoResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody PedidoRequest request
    ) {
        return pedidoService.actualizar(id, request);
    }

    @PostMapping("/{id}/cancelar")
    public PedidoResponse cancelar(
            @PathVariable Long id
    ) {
        return pedidoService.cancelar(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(
            @PathVariable Long id
    ) {
        pedidoService.eliminar(id);
    }
}
