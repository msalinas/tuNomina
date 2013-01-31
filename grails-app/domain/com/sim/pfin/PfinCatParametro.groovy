package com.sim.pfin

class PfinCatParametro {
	String     claveMedio
	Date       fechaMedio
	Boolean    operaSabado = false
	Boolean    operaDomingo = false
	Boolean    operaDiaFestivo = false
	Integer    numeroDigitosDespliega
	Integer	   consecutivoPublicacion

	static constraints = {
		claveMedio(size:3..15, unique: true, nullable: false, blank: false)
		fechaMedio()
		operaSabado()
		operaDomingo()
		operaDiaFestivo()
		numeroDigitosDespliega(range:1..8)
		consecutivoPublicacion()
	}

	String toString() {
		"${claveMedio} - ${fechaMedio}"
	}
}
