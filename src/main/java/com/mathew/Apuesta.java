package com.mathew;

public class Apuesta {
    private final String usuarioId;
    private final long cantidad;
    private final String opcion; // "GANADOR_A", "GANADOR_B", "EMPATE" o "DF"

    public Apuesta(String usuarioId, long cantidad, String opcion) {
        this.usuarioId = usuarioId;
        this.cantidad = cantidad;
        this.opcion = opcion;
    }

    public String getUsuarioId() { return usuarioId; }
    public long getCantidad() { return cantidad; }
    public String getOpcion() { return opcion; }
}