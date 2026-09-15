package br.com.marcosbassetto.concursos.domain.usuario.dto;

import br.com.marcosbassetto.concursos.domain.usuario.entity.UsuarioEntity;
import org.springframework.data.domain.Page;

import java.util.List;


public record UsuarioListResponse(
        List<UsuarioDTO> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {


    public static UsuarioListResponse fromPage(Page<UsuarioDTO> page) {
        return new UsuarioListResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }


    public static UsuarioListResponse fromEntityPage(Page<UsuarioEntity> page) {
        return new UsuarioListResponse(
                UsuarioDTO.fromEntityList(page.getContent()),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}