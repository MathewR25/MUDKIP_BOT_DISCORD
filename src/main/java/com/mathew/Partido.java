package com.mathew;

import java.util.ArrayList;
import java.util.List;

public class Partido {
    private final int id;
    private final String equipoA;
    private final String equipoB;
    private double pozoTotal;
    private final List<Apuesta> apuestas;
    private boolean abierto; // <- Agregado de forma segura

    public Partido(int id, String equipoA, String equipoB) {
        this.id = id;
        this.equipoA = equipoA;
        this.equipoB = equipoB;
        this.pozoTotal = 0.0;
        this.apuestas = new ArrayList<>();
        this.abierto = true; // <- Inicia abierto por defecto
    }

    public int getId() { return id; }
    public String getEquipoA() { return equipoA; }
    public String getEquipoB() { return equipoB; }
    public double getPozoTotal() { return pozoTotal; }
    public List<Apuesta> getApuestas() { return apuestas; }

    // --- Métodos de control para el comando /pausar-apuestas ---
    public boolean isAbierto() { return abierto; }
    public void setAbierto(boolean abierto) { this.abierto = abierto; }

    public void agregarMonedasAlPozo(double cantidad) {
        this.pozoTotal += cantidad;
    }
}
