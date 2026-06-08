package com.mathew;

import java.util.ArrayList;
import java.util.List;

public class Partido {
    private final int id;
    private final String equipoA;
    private final String equipoB;
    private double pozoTotal;
    private final List<Apuesta> apuestas;

    public Partido(int id, String equipoA, String equipoB) {
        this.id = id;
        this.equipoA = equipoA;
        this.equipoB = equipoB;
        this.pozoTotal = 0.0;
        this.apuestas = new ArrayList<>();
    }

    public int getId() { return id; }
    public String getEquipoA() { return equipoA; }
    public String getEquipoB() { return equipoB; }
    public double getPozoTotal() { return pozoTotal; }
    public List<Apuesta> getApuestas() { return apuestas; }

    public void agregarMonedasAlPozo(double cantidad) {
        this.pozoTotal += cantidad;
    }
}
