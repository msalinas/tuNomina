package com.sim.credito

import mx.com.creditoreal.ws.client.Client;
import mx.com.creditoreal.ws.dto.Adicional;
import mx.com.creditoreal.ws.dto.Solicitud;
import mx.com.creditoreal.ws.exception.ClientException;

//Clases importadas para el metodo: altaPrestamos
import com.sim.catalogo.*
import com.sim.entidad.*
import com.sim.empresa.EmpEmpleado
import com.sim.producto.ProPromocion
import com.sim.cliente.RsCliente
import com.sim.pfin.PfinCatConcepto

class PrestamoService {

	//Servicio que genera las Tablas de Amortizacion
	def tablaAmortizacionRegistroService 

    String envioSolicitudCreditoReal(Prestamo prestamo) {

    	def documentos = prestamo.documentos
    	File doctoSolicitud
    	File doctoPagare
    	File doctoIdentificacion
    	File doctoAdicionalA
    	File doctoAdicionalB
    	File doctoAdicionalC
    	File doctoAdicional

		File path = new File("${System.getProperty('user.home')}/Documents/tuNomina/documentosCredito/${prestamo.cliente.id}/${prestamo.folioSolicitud}")

		List<PrestamoDocumento> documentosListaCr = []
		
		documentos.each{documentoPrestamo ->

			switch ( documentoPrestamo.documento ) {
			    case SimCatDocumento.findByClaveDocumento('SOLICITUD_PRESTAMO'):
			    	doctoSolicitud = new File(path, documentoPrestamo.nombreArchivo)
			    	log.info "Solicitud: "+doctoSolicitud
			    break
			    case SimCatDocumento.findByClaveDocumento('PAGARE_PRESTAMO'):
			    	doctoPagare = new File(path, documentoPrestamo.nombreArchivo)
			    	log.info "Pagare: "+doctoPagare
			    break
			    case SimCatDocumento.findByClaveDocumento('IDENTIFICACION_PRESTAMO'):
			    	doctoIdentificacion = new File(path, documentoPrestamo.nombreArchivo)
			    	log.info "Identificacion: "+doctoIdentificacion
			    break
			    default:
			    	if (!doctoAdicionalA){
			    		doctoAdicionalA = new File(path, documentoPrestamo.nombreArchivo)
			    		log.info "doctoAdicionalA: "+doctoAdicionalA
			    	}else if(!doctoAdicionalB){
			    		doctoAdicionalB = new File(path, documentoPrestamo.nombreArchivo)
			    		log.info "doctoAdicionalB: "+doctoAdicionalB
			    	}else if(!doctoAdicionalC){
			    		doctoAdicionalC = new File(path, documentoPrestamo.nombreArchivo)
			    		log.info "doctoAdicionalC: "+doctoAdicionalC
			    	}else{
			    		//SE ASIGNA EL DOCUMENTO A UNA LISTA PARA ENVIAR
			    		//A CREDITO REAL
			    		documentosListaCr.add(documentoPrestamo)
			    		log.info "No se asigno el documento: "+documentoPrestamo.nombreArchivo
			    	}
			}
    	}
    	
		Client client = null;
		
		try {
			//TRUE SIGNIFICA QUE ENVIA A UN WEBSERVICE DE CREDITO REAL EN UN AMBIENTE DE PRUEBAS
			client = new Client(true);
		} catch (ClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Solicitud solicitud = new Solicitud();
		solicitud.setReferencia("${prestamo.folioSolicitud}"); //Numero de Referencia o Folio propia del distribuidor o dejar en blanco
        log.info "Referencia: " + solicitud.getReferencia()
		solicitud.setDistribuidor("9999"); //9999  Se asignara por CR
		solicitud.setSucursal("1111"); //1111 Se asignara por CR
		//EL VENDEDOR SE REFIEREN A MTN O EL VENDEDOR DE MTN
		solicitud.setVendedor(""); //Clave de Vendedor asignada. Dejar en blanco para pruebas
        log.info "¿Que enviar en Vendedor?"
		solicitud.setDap("0"); //Enviar un 0
		solicitud.setPercepciones(prestamo.percepcionesMensuales); //Percepciones mensuales del cliente
        log.info "Percepciones Mensuales: "+ solicitud.getPercepciones()
		solicitud.setDeducciones(prestamo.deduccionesMensuales); //Deducciones mensuales del cliente
        log.info "Deducciones Mensuales: "+ solicitud.getDeducciones()
		solicitud.setSolicitud(doctoSolicitud?.getBytes()); //Imagen binarizada de la Solictud de credito
		solicitud.setPagare(doctoPagare?.getBytes()); //Imagen binarizada del pagare
		solicitud.setIdentificacion(doctoIdentificacion?.getBytes()); //Imagen binarizada de la identificacion
		solicitud.setDocadA(doctoAdicionalA?.getBytes()); //Imagen binarizada de un documento adicional
		solicitud.setDocadB(doctoAdicionalB?.getBytes()); //Imagen binarizada de un documento adicional
		solicitud.setDocadC(doctoAdicionalC?.getBytes()); //Imagen binarizada de un documento adicional
		
		String consecutivo = "NO SE GENERO";
			
		try {
			consecutivo = client.solicitudZell(solicitud);
		} catch (ClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Respuesta Credito Real Consecutivo: "+ consecutivo);
		
		prestamo.consecutivoCr = consecutivo

		if(documentosListaCr){
			log.info ("Existen mas de 6 documentos para enviar a CR")

			documentosListaCr.each{ documentoPrestamo ->
				
			    doctoAdicional = new File(path, documentoPrestamo.nombreArchivo)				

				Adicional adicional = new Adicional()
				adicional.setConsecutivo(consecutivo)
				adicional.setDocAd(doctoAdicional.getBytes())
				
				String respuesta = "No se genero respuesta"
				try {
					respuesta = client.documentoAdicional(adicional);
				} catch (ClientException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				log.info("Respuesta para el documento ${doctoAdicional}: ${respuesta}")
			}
		}
        return consecutivo
    }

    String envioDocumentoCreditoReal(String idDocumento) {

		PrestamoDocumento documento = 
			PrestamoDocumento.get(idDocumento)	
		log.info "Documento: "+documento

		String respuesta = "No se genero respuesta"

		File path = new File("${System.getProperty('user.home')}/Documents/tuNomina/documentosCredito/${documento.prestamo.cliente.id}/${documento.prestamo.folioSolicitud}")
		log.info ("Path: "+path)

		String consecutivo = documento.prestamo.consecutivoCr
		log.info "Prestamo: "+documento.prestamo
		log.info "Consecutivo CR: "+consecutivo

		if(consecutivo){
			File doctoAdicional = new File(path, documento.nombreArchivo)
			log.info "Documento Adicional: "+doctoAdicional

			Adicional adicional = new Adicional()
			adicional.setConsecutivo(consecutivo)
			adicional.setDocAd(doctoAdicional.getBytes())		
			
			try {
				Client client = new Client(true);
				respuesta = client.documentoAdicional(adicional);
			} catch (ClientException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			log.info("Respuesta para el documento ${doctoAdicional}: ${respuesta}")
		}else{
			respuesta = """
			No existe consecutivo, debe enviarse primero la solicitud a Credito Real
			"""
		}

		return "${documento.nombreArchivo} : ${respuesta}"
    }

    Boolean generarAccesoriosPrestamo(Prestamo prestamo){
    	ProPromocion promocion = prestamo.promocion
    	ArrayList accesoriosPromocion = promocion.proPromocionAccesorio
    	accesoriosPromocion.each{
    		//VALIDA QUE EL ACCESORIO NO SEA INTERES E IVA DE INTERES
    		if (it.accesorio !=
    			SimCatAccesorio.findByConcepto(PfinCatConcepto.findByClaveConcepto('INTERES')) 
    			&& it.accesorio !=
    			SimCatAccesorio.findByConcepto(PfinCatConcepto.findByClaveConcepto('IVAINT'))){
				def prestamoAccesorio = new PrestamoAccesorio(
					accesorio		:	it.accesorio,
					valor			:	0,
					prestamo		: 	prestamo,
				).save()
			}
    	}
    }

    Boolean altaPrestamos () {
		Prestamo prestamoUno = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('JCHSDFYUYUI'),
				correoSolicitante:     "javierhernandez@gmail.com",
				folioSolicitud : 	   1,
				dependencia : 		   EntDependencia.findByClaveDependencia('IMSS'),
				promocion: 			   ProPromocion.findByClavePromocion('MOR78987'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   10000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('01/16/2013'),
				).save()

/*
		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoUno)		

		Prestamo prestamoDos = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('CSALSDFYUYUI'),
				correoSolicitante:     "carsalcido@gmail.com",
				folioSolicitud : 	   2,
				dependencia : 		   EntDependencia.findByClaveDependencia('CFE'),
				promocion: 			   ProPromocion.findByClavePromocion('PROMOCHIDO'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   10000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('01/17/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoDos)

		Prestamo prestamoTres = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('MAZASDFYUYUI'),
				correoSolicitante:     "franciscorodriguez@gmail.com",
				folioSolicitud : 	   3,
				dependencia : 		   EntDependencia.findByClaveDependencia('IMSS'),
				promocion: 			   ProPromocion.findByClavePromocion('MOR78987'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   10000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('01/18/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoTres)

		Prestamo prestamoCuatro = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('PACOSDFYUYUI'),
				correoSolicitante:     "guillermoochoa@gmail.com",
				folioSolicitud : 	   4,
				dependencia : 		   EntDependencia.findByClaveDependencia('CFE'),
				promocion: 			   ProPromocion.findByClavePromocion('PROMOCHIDO'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   10000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('01/19/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoCuatro)

		Prestamo prestamoCinco = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('CHUYSDFYUYUI'),
				correoSolicitante:     "jesuscorona@gmail.com",
				folioSolicitud : 	   5,
				dependencia : 		   EntDependencia.findByClaveDependencia('IMSS'),
				promocion: 			   ProPromocion.findByClavePromocion('MOR78987'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   10000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('01/20/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoCinco)

		Prestamo prestamoSeis = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('TORRSDFYUYUI'),
				correoSolicitante:     "gerardotorrado@gmail.com",
				folioSolicitud : 	   6,
				dependencia : 		   EntDependencia.findByClaveDependencia('CFE'),
				promocion: 			   ProPromocion.findByClavePromocion('PROMOCHIDO'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   10000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('01/21/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoSeis)

		Prestamo prestamoSiete = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('ORIBSDFYUYUI'),
				correoSolicitante:     "oribeperalta@gmail.com",
				folioSolicitud : 	   7,
				dependencia : 		   EntDependencia.findByClaveDependencia('IMSS'),
				promocion: 			   ProPromocion.findByClavePromocion('MOR78987'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   10000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('01/22/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoSiete)



		Prestamo prestamoOcho = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('BENJSDFYUYUI'),
				correoSolicitante:     "benjamingalindo@gmail.com",
				folioSolicitud : 	   8,
				dependencia : 		   EntDependencia.findByClaveDependencia('CFE'),
				promocion: 			   ProPromocion.findByClavePromocion('PROMOCHIDO'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   10000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('01/23/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoOcho)

		Prestamo prestamoNueve = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('TENASDFYUYUI'),
				correoSolicitante:     "alfredotena@gmail.com",
				folioSolicitud : 	   9,
				dependencia : 		   EntDependencia.findByClaveDependencia('IMSS'),
				promocion: 			   ProPromocion.findByClavePromocion('MOR78987'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   10000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('01/24/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoNueve)

		Prestamo prestamoDiez = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('LUISSDFYUYUI'),
				correoSolicitante:     "luisgarcia@gmail.com",
				folioSolicitud : 	   10,
				dependencia : 		   EntDependencia.findByClaveDependencia('CFE'),
				promocion: 			   ProPromocion.findByClavePromocion('PROMOCHIDO'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   10000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('01/25/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoDiez)

		Prestamo prestamoOnce = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('JOAQSDFYUYUI'),
				correoSolicitante:     "joaquinlopez@gmail.com",
				folioSolicitud : 	   11,
				dependencia : 		   EntDependencia.findByClaveDependencia('IMSS'),
				promocion: 			   ProPromocion.findByClavePromocion('MOR78987'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   15000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('01/26/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoOnce)

		Prestamo prestamoDoce = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('JAVISDFYUYUI'),
				correoSolicitante:     "javieralatorre@hotmail.com",
				folioSolicitud : 	   12,
				dependencia : 		   EntDependencia.findByClaveDependencia('CFE'),
				promocion: 			   ProPromocion.findByClavePromocion('PROMOCHIDO'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   15000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('01/27/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoDoce)

		Prestamo prestamoTrece = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('ALEXSDFYUYUI'),
				correoSolicitante:     "alejandrovillalvazo@hotmail.com",
				folioSolicitud : 	   13,
				dependencia : 		   EntDependencia.findByClaveDependencia('IMSS'),
				promocion: 			   ProPromocion.findByClavePromocion('MOR78987'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   15000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('01/28/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoTrece)

		Prestamo prestamoCatorce = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('ADELSDFYUYUI'),
				correoSolicitante:     "adelamicha@hotmail.com",
				folioSolicitud : 	   14,
				dependencia : 		   EntDependencia.findByClaveDependencia('CFE'),
				promocion: 			   ProPromocion.findByClavePromocion('PROMOCHIDO'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   15000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('01/29/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoCatorce)

		Prestamo prestamoQuince = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('EDUSALBUS'),
				correoSolicitante:     "eduardosalazar@hotmail.com",
				folioSolicitud : 	   15,
				dependencia : 		   EntDependencia.findByClaveDependencia('IMSS'),
				promocion: 			   ProPromocion.findByClavePromocion('MOR78987'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   15000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('01/30/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoQuince)

		Prestamo prestamoDieciseis = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('CARMSDFYUYUI'),
				correoSolicitante:     "carmenaristegui@gmail.com",
				folioSolicitud : 	   16,
				dependencia : 		   EntDependencia.findByClaveDependencia('CFE'),
				promocion: 			   ProPromocion.findByClavePromocion('PROMOCHIDO'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   15000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('01/31/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoDieciseis)

		Prestamo prestamoDiecisiete = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('ARAPSDFYUYUI'),
				correoSolicitante:     "arelypaz@hotmail.com",
				folioSolicitud : 	   17,
				dependencia : 		   EntDependencia.findByClaveDependencia('IMSS'),
				promocion: 			   ProPromocion.findByClavePromocion('MOR78987'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   17000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('02/01/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoDiecisiete)



		Prestamo prestamoDieciocho = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('LIDYSDFYUYUI'),
				correoSolicitante:     "lidya09@hotmail.com",
				folioSolicitud : 	   18,
				dependencia : 		   EntDependencia.findByClaveDependencia('CFE'),
				promocion: 			   ProPromocion.findByClavePromocion('PROMOCHIDO'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   17000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('02/02/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoDieciocho)

		Prestamo prestamoDiecinueve = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('HANNSDFYUYUI'),
				correoSolicitante:     "hannia@hotmail.com",
				folioSolicitud : 	   19,
				dependencia : 		   EntDependencia.findByClaveDependencia('IMSS'),
				promocion: 			   ProPromocion.findByClavePromocion('MOR78987'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   17000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('02/03/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoDiecinueve)

		Prestamo prestamoVeinte = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('ANAMSDFYUYUI'),
				correoSolicitante:     "anama13@gmail.com",
				folioSolicitud : 	   20,
				dependencia : 		   EntDependencia.findByClaveDependencia('CFE'),
				promocion: 			   ProPromocion.findByClavePromocion('PROMOCHIDO'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   17000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('02/04/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoVeinte)

		Prestamo prestamoVeintiUno = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('ANAGSDFYUYUI'),
				correoSolicitante:     "guevara@gmail.com",
				folioSolicitud : 	   21,
				dependencia : 		   EntDependencia.findByClaveDependencia('IMSS'),
				promocion: 			   ProPromocion.findByClavePromocion('MOR78987'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   17000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('02/05/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoVeintiUno)

		Prestamo prestamoVeintiDos = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('ESTBSDFYUYUI'),
				correoSolicitante:     "esteban03@gmail.com",
				folioSolicitud : 	   22,
				dependencia : 		   EntDependencia.findByClaveDependencia('CFE'),
				promocion: 			   ProPromocion.findByClavePromocion('PROMOCHIDO'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   17000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('02/06/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoVeintiDos)

		Prestamo prestamoVeintiTres = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('MARISDFYUYUI'),
				correoSolicitante:     "mariano09@gmail.com",
				folioSolicitud : 	   23,
				dependencia : 		   EntDependencia.findByClaveDependencia('IMSS'),
				promocion: 			   ProPromocion.findByClavePromocion('MOR78987'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   14000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('02/07/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoVeintiTres)

		Prestamo prestamoVeintiCuatro = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('jorgSDFYUYUI'),
				correoSolicitante:     "zaeza09@gmail.com",
				folioSolicitud : 	   24,
				dependencia : 		   EntDependencia.findByClaveDependencia('CFE'),
				promocion: 			   ProPromocion.findByClavePromocion('PROMOCHIDO'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   14000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('02/08/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoVeintiCuatro)

		Prestamo prestamoVeintiCinco = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('EUGENIOHUYTG'),
				correoSolicitante:     "eugenioder09@gmail.com",
				folioSolicitud : 	   25,
				dependencia : 		   EntDependencia.findByClaveDependencia('IMSS'),
				promocion: 			   ProPromocion.findByClavePromocion('MOR78987'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   14000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('02/09/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoVeintiCinco)

		Prestamo prestamoVeintiSeis = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('OMARDFYUYUI'),
				correoSolicitante:     "omarchaparro@gmail.com",
				folioSolicitud : 	   26,
				dependencia : 		   EntDependencia.findByClaveDependencia('CFE'),
				promocion: 			   ProPromocion.findByClavePromocion('PROMOCHIDO'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   14000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('02/10/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoVeintiSeis)

		Prestamo prestamoVeintiSiete = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('CHABE5FYUYUI'),
				correoSolicitante:     "javierlopez@gmail.com",
				folioSolicitud : 	   27,
				dependencia : 		   EntDependencia.findByClaveDependencia('IMSS'),
				promocion: 			   ProPromocion.findByClavePromocion('MOR78987'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   14000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('02/11/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoVeintiSiete)

		Prestamo prestamoVeintiOcho = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('ADISDFYUYUI'),
				correoSolicitante:     "adrianarodrigu@gmail.com",
				folioSolicitud : 	   28,
				dependencia : 		   EntDependencia.findByClaveDependencia('CFE'),
				promocion: 			   ProPromocion.findByClavePromocion('PROMOCHIDO'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   14000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('02/12/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoVeintiOcho)

		Prestamo prestamoVeintiNueve = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('LEE41235'),
				correoSolicitante:     "carlosperez@gmail.com",
				folioSolicitud : 	   29,
				dependencia : 		   EntDependencia.findByClaveDependencia('IMSS'),
				promocion: 			   ProPromocion.findByClavePromocion('MOR78987'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   14000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('02/13/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoVeintiNueve)

		Prestamo prestamoTreinta = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('PAU678990'),
				correoSolicitante:     "paulinarubio@gmail.com",
				folioSolicitud : 	   30,
				dependencia : 		   EntDependencia.findByClaveDependencia('CFE'),
				promocion: 			   ProPromocion.findByClavePromocion('PROMOCHIDO'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   14000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('02/14/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoTreinta)

		Prestamo prestamoTreintaYUno = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('ROMEO342'),
				correoSolicitante:     "romeosantos@gmail.com",
				folioSolicitud : 	   31,
				dependencia : 		   EntDependencia.findByClaveDependencia('IMSS'),
				promocion: 			   ProPromocion.findByClavePromocion('MOR78987'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   17000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('02/15/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoTreintaYUno)

		Prestamo prestamoTreintaYDos = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('HNERI462'),
				correoSolicitante:     "henrisanchez@gmail.com",
				folioSolicitud : 	   32,
				dependencia : 		   EntDependencia.findByClaveDependencia('CFE'),
				promocion: 			   ProPromocion.findByClavePromocion('PROMOCHIDO'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   17000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('02/16/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoTreintaYDos)

		Prestamo prestamoTreintaYTres = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('LUI456'),
				correoSolicitante:     "luissolis@gmail.com",
				folioSolicitud : 	   33,
				dependencia : 		   EntDependencia.findByClaveDependencia('IMSS'),
				promocion: 			   ProPromocion.findByClavePromocion('MOR78987'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   14000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('02/17/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoTreintaYTres)

		Prestamo prestamoTreintaYCuatro = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('ELSA543'),
				correoSolicitante:     "elsaruizs@gmail.com",
				folioSolicitud : 	   34,
				dependencia : 		   EntDependencia.findByClaveDependencia('CFE'),
				promocion: 			   ProPromocion.findByClavePromocion('PROMOCHIDO'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   14000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('02/18/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoTreintaYCuatro)

		Prestamo prestamoTreintaYCinco = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('PACO456'),
				correoSolicitante:     "memo@gmail.com",
				folioSolicitud : 	   35,
				dependencia : 		   EntDependencia.findByClaveDependencia('IMSS'),
				promocion: 			   ProPromocion.findByClavePromocion('MOR78987'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   14000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('02/19/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoTreintaYCinco)

		Prestamo prestamoTreintaYSeis = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('RAFA543'),
				correoSolicitante:     "rmarquez@gmail.com",
				folioSolicitud : 	   36,
				dependencia : 		   EntDependencia.findByClaveDependencia('CFE'),
				promocion: 			   ProPromocion.findByClavePromocion('PROMOCHIDO'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   14000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('02/20/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoTreintaYSeis)

		Prestamo prestamoTreintaYSiete = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('GERA456'),
				correoSolicitante:     "gerardo4@gmail.com",
				folioSolicitud : 	   37,
				dependencia : 		   EntDependencia.findByClaveDependencia('IMSS'),
				promocion: 			   ProPromocion.findByClavePromocion('MOR78987'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   14000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('02/21/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoTreintaYSiete)

		Prestamo prestamoTreintaYOcho = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('JUAN456'),
				correoSolicitante:     "juand@gmail.com",
				folioSolicitud : 	   38,
				dependencia : 		   EntDependencia.findByClaveDependencia('CFE'),
				promocion: 			   ProPromocion.findByClavePromocion('PROMOCHIDO'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   14000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('02/22/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoTreintaYOcho)

		Prestamo prestamoTreintaYNueve = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('CARPNIOHUYTG'),
				correoSolicitante:     "carpe@gmail.com",
				folioSolicitud : 	   39,
				dependencia : 		   EntDependencia.findByClaveDependencia('IMSS'),
				promocion: 			   ProPromocion.findByClavePromocion('MOR78987'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   14000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('02/23/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoTreintaYNueve)

		Prestamo prestamoCuarenta = new Prestamo(
				cliente : 			   RsCliente.findByNumeroDeNomina('CRIS456'),
				correoSolicitante:     "cristianbenitez@gmail.com",
				folioSolicitud : 	   40,
				dependencia : 		   EntDependencia.findByClaveDependencia('CFE'),
				promocion: 			   ProPromocion.findByClavePromocion('PROMOCHIDO'),
				sucursal: 			   EntSucursal.findByClaveSucursal('EDOMEX'),
				delegacion: 		   EntDelegacion.findByClaveDelegacion('ZACATECAS'),
				vendedor : 			   EmpEmpleado.findByNumeroNomina('001'),
				fechaSolicitud:		   new Date('01/01/2013'),
				montoSolicitado: 	   14000,
				percepcionesMensuales: 20000,
				deduccionesMensuales:  25000,
				estatusSolicitud:      SimCatEtapaPrestamo.findByClaveEtapaPrestamo('ACTIVO'),
				formaDeDispercion:     SimCatFormaEntrega.findByClaveFormaEntrega('VENBANCO'),
				documentosCorrectos:   false,
				aprobado:              false,
				reenviarSolicitud:     false,
				incluirEnListasCobro:  true,
				fechaCobro:            new Date('02/24/2013'),
				).save(failOnError: true)

		tablaAmortizacionRegistroService.generaTablaAmortizacion(prestamoCuarenta)
*/
		return true

	}
}