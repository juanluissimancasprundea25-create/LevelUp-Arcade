package com.leveluparcade.service;

import com.leveluparcade.dto.request.ClienteCreateRequest;
import com.leveluparcade.dto.request.ClienteUpdateRequest;
import com.leveluparcade.dto.response.ClienteCreadoResponse;
import com.leveluparcade.dto.response.ClienteResponse;

import java.util.List;

/**
 * Operaciones de gestion de clientes desde el panel admin.
 *
 * <p>Encapsula la logica de coordinacion entre las entidades
 * {@code Cliente} y {@code Usuario}, ya que el alta de cliente
 * implica crear ambas a la vez.
 */
public interface ClienteService {

    /** Lista todos los clientes. */
    List<ClienteResponse> listarTodos();

    /** Busca por texto libre en nombre/apellidos/email/NIF. */
    List<ClienteResponse> buscarPorTexto(String texto);

    /** Obtiene un cliente por id o lanza ResourceNotFoundException. */
    ClienteResponse obtenerPorId(Long id);

    /**
     * Da de alta un nuevo cliente: crea Usuario (rol CLIENTE, password
     * temporal aleatoria) + Cliente asociado. Devuelve la password
     * temporal en plano para que el admin la comunique al cliente.
     *
     * @throws IllegalArgumentException si el email ya existe
     * @throws IllegalArgumentException si el NIF ya existe
     */
    ClienteCreadoResponse crear(ClienteCreateRequest request);

    /**
     * Actualiza los datos de un cliente existente.
     * No permite cambiar email ni password (flujos separados).
     */
    ClienteResponse actualizar(Long id, ClienteUpdateRequest request);

    /** Elimina un cliente. Por cascada, borra tambien su Usuario. */
    void eliminar(Long id);
}