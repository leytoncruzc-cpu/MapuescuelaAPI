package cl.mapuescuela.service;

import cl.mapuescuela.dto.pedido.DetallePedidoRequest;
import cl.mapuescuela.dto.pedido.DetallePedidoResponse;
import cl.mapuescuela.dto.pedido.PedidoRequest;
import cl.mapuescuela.dto.pedido.PedidoResponse;
import cl.mapuescuela.entity.*;
import cl.mapuescuela.exception.BusinessRuleException;
import cl.mapuescuela.exception.ResourceNotFoundException;
import cl.mapuescuela.repository.ProductoRepository;
import cl.mapuescuela.repository.PedidoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PedidoService {
    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;

    public PedidoService(
            PedidoRepository pedidoRepository,
            ProductoRepository productoRepository
    ) {
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
    }

    public PedidoResponse crear(PedidoRequest request) {

        Pedido pedido = new Pedido();

        pedido.setNombreCliente(request.nombreCliente());
        pedido.setEmailCliente(request.emailCliente());
        pedido.setTelefonoCliente(request.telefonoCliente());
        pedido.setDireccionDespacho(request.direccionDespacho());
        pedido.setModalidadEntrega(request.modalidadEntrega());

        for (DetallePedidoRequest detalleRequest : request.detalles()) {

            Producto producto = productoRepository
                    .findById(detalleRequest.productoId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Producto no encontrado: "
                                            + detalleRequest.productoId()
                            )
                    );

            if (producto.getEstado() != EstadoProducto.ACTIVO) {
                throw new BusinessRuleException(
                        "El producto no está activo: "
                                + producto.getId()
                );
            }

            if (producto.getStock() < detalleRequest.cantidad()) {
                throw new BusinessRuleException(
                        "Stock insuficiente para el producto: "
                                + producto.getId()
                );
            }

            DetallePedido detalle = new DetallePedido();

            detalle.setProducto(producto);
            detalle.setCantidad(detalleRequest.cantidad());
            detalle.setPrecioUnitario(producto.getPrecio());

            pedido.agregarDetalle(detalle);
        }

        pedido.recalcularTotal();

        Pedido guardado = pedidoRepository.save(pedido);

        return toResponse(guardado);
    }

    @Transactional(readOnly = true)
    public PedidoResponse buscarPorId(Long id) {

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Pedido no encontrado: " + id
                        )
                );

        return toResponse(pedido);
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listar() {

        return pedidoRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponse buscarPorCodigo(String codigo) {
        Pedido pedido = pedidoRepository
                .findByCodigo(codigo)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Pedido no encontrado: " + codigo
                        )
                );
        return toResponse(pedido);
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarPorEstado(EstadoPedido estado) {
        return pedidoRepository.findByEstado(estado)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public PedidoResponse cancelar(Long id) {
        Pedido pedido = obtenerPedido(id);
        validarPuedeModificar(pedido);
        pedido.setEstado(EstadoPedido.CANCELADO);
        return toResponse(pedido);
    }

    public void eliminar(Long id) {
        Pedido pedido = obtenerPedido(id);
        pedidoRepository.delete(pedido);
    }

    public PedidoResponse actualizar(
            Long id,
            PedidoRequest request
    ) {

        Pedido pedido = obtenerPedido(id);

        validarPuedeModificar(pedido);

        pedido.setNombreCliente(request.nombreCliente());
        pedido.setEmailCliente(request.emailCliente());
        pedido.setTelefonoCliente(request.telefonoCliente());
        pedido.setDireccionDespacho(request.direccionDespacho());
        pedido.setModalidadEntrega(request.modalidadEntrega());

        pedido.getDetalles().clear();

        for (DetallePedidoRequest detalleRequest : request.detalles()) {

            Producto producto = productoRepository
                    .findById(detalleRequest.productoId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Producto no encontrado: "
                                            + detalleRequest.productoId()
                            )
                    );

            if (producto.getStock() < detalleRequest.cantidad()) {
                throw new BusinessRuleException(
                        "Stock insuficiente para el producto: "
                                + producto.getId()
                );
            }

            DetallePedido detalle = new DetallePedido();

            detalle.setProducto(producto);
            detalle.setCantidad(detalleRequest.cantidad());
            detalle.setPrecioUnitario(producto.getPrecio());

            pedido.agregarDetalle(detalle);
        }

        pedido.recalcularTotal();

        return toResponse(pedido);
    }

    private Pedido obtenerPedido(Long id) {

        return pedidoRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Pedido no encontrado: " + id
                        )
                );
    }

    private void validarPuedeModificar(Pedido pedido) {

        if (pedido.getEstado() != EstadoPedido.PENDIENTE_PAGO) {

            throw new BusinessRuleException(
                    "El pedido no puede modificarse porque se encuentra en estado "
                            + pedido.getEstado()
            );
        }
    }


    private PedidoResponse toResponse(Pedido pedido) {

        List<DetallePedidoResponse> detalles =
                pedido.getDetalles()
                        .stream()
                        .map(detalle ->
                                new DetallePedidoResponse(
                                        detalle.getId(),
                                        detalle.getProducto().getId(),
                                        detalle.getProducto().getNombre(),
                                        detalle.getCantidad(),
                                        detalle.getPrecioUnitario(),
                                        detalle.getSubtotal()
                                )
                        )
                        .toList();

        return new PedidoResponse(
                pedido.getId(),
                pedido.getCodigo(),
                pedido.getNombreCliente(),
                pedido.getEmailCliente(),
                pedido.getTelefonoCliente(),
                pedido.getDireccionDespacho(),
                pedido.getModalidadEntrega(),
                pedido.getEstado(),
                pedido.getTotal(),
                pedido.getFechaCreacion(),
                detalles
        );
    }
}
