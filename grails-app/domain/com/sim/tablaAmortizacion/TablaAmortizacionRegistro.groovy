package com.sim.tablaAmortizacion

import com.sim.credito.Prestamo

class TablaAmortizacionRegistro {
	
	//LOS NOMBRE QUE APARECEN EN COMENTARIO HACEN REFERENCIA AL SERVICIO DE TABLA AMORTIZACION ORIGINADO POR MIKE
	Integer     numeroPago //numeroDePago
	Date		fecha
	BigDecimal	impSaldoInicial
	BigDecimal  tasaInteres
	BigDecimal  impInteres //interes
	BigDecimal  impIvaInteres
	BigDecimal  impCapital //amortizacionCapital
	BigDecimal  impPago //pagoTotal
	BigDecimal  impSaldoFinal //saldoInsoluto
	Boolean     pagoPuntual = false
	BigDecimal  impInteresPagado
	BigDecimal  impIvaInteresPagado
	BigDecimal  impCapitalPagado
	BigDecimal  impPagoPagado
	Boolean     pagado = false
	Date		fechaPagoUltimo
	Date		fechaValorCalculado

	static belongsTo = [prestamo:Prestamo]
	
	static hasMany =   [tablaAmortizacionAccesorio : TablaAmortizacionAccesorio]
	
	static constraints = {
		numeroPago()
		fecha()
		impSaldoInicial 			nullable:false
		tasaInteres					nullable:false
		impInteres					nullable:false
		impIvaInteres				nullable:false
		impCapital					nullable:false
		impPago						nullable:false
		impSaldoFinal				nullable:false
		pagoPuntual()
		impInteresPagado			nullable:true
		impIvaInteresPagado			nullable:true
		impCapitalPagado 			nullable:true
		impPagoPagado				nullable:true
		pagado()
		fechaPagoUltimo             nullable:true
		fechaValorCalculado()
	}

	String toString() {
		"${prestamo} :Pago ${numeroPago}"
	}
}