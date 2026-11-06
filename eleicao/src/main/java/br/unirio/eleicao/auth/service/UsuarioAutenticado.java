package br.unirio.eleicao.auth.service;

import br.unirio.eleicao.shared.Perfil;

public record UsuarioAutenticado(Long id, Perfil perfil) {}
