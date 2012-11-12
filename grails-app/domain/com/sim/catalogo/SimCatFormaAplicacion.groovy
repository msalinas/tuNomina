package com.sim.catalogo

class SimCatFormaAplicacion {
	String  claveFormaAplicacion
	String  nombreFormaAplicacion

    static constraints = {
		claveFormaAplicacion (size:3..10, unique: true, nullable: false, blank: false)
		nombreFormaAplicacion (size:3..60, unique: true, nullable: false, blank: false)
    }
	String toString() {
		"${nombreFormaAplicacion}"
	}
}
