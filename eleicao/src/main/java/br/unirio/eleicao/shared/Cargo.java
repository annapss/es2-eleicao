package br.unirio.eleicao.shared;

public enum Cargo {
    DEPUTADO_ESTADUAL(1, "Deputado Estadual"),
    DEPUTADO_FEDERAL(2, "Deputado Federal"),
    SENADOR(3, "Senador"),
    GOVERNADOR(4, "Governador"),
    PRESIDENTE(5, "Presidente da República");

    private final long id;
    private final String nome;

    Cargo(long id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    public long getId() { return id; }

    public String getNome() { return nome; }

    public static Cargo deId(long id) {
        for (Cargo c : values()) {
            if (c.id == id) return c;
        }
        throw new IllegalArgumentException("Cargo inexistente: " + id);
    }
}