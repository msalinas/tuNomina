package com.sim.entidad

class EntDelegacion {

    String  claveDelegacion
    String  nombreDelegacion
    String  descripcionDelegacion

    static belongsTo = [sucursal: EntSucursal]

    static constraints = {
        claveDelegacion(size:3..15, unique: true, nullable: false, blank: false)
        nombreDelegacion(size:3..50, unique: true, nullable: false, blank: false)
        descripcionDelegacion(size:5..150, nullable: true)
        sucursal nullable: true
    }

    String toString() {
        "${nombreDelegacion}"
    }}
