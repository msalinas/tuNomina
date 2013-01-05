
<%@ page import="com.sim.listacobro.ListaCobroProceso" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <meta name="layout" content="main" />
        <g:set var="entityName" value="${message(code: 'listaCobroProceso.label', default: 'ListaCobroProceso')}" />
        <title><g:message code="default.list.label" args="[entityName]" /></title>
    </head>
    <body>
			<div class="nav" role="navigation">
			 <ul> 
            <li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
	          <li><g:link class="list" controller="task" action="myTaskList"><g:message code="myTasks.label" default="My Tasks ({0})" args="[myTasksCount]" /></g:link></li>
            <li><g:link class="create" action="start"><g:message code="default.new.label" args="[entityName]" /></g:link></li>
			  </ul>
			</div>
        <div class="body">
            <h1><g:message code="default.list.label" args="[entityName]" /></h1>
            <g:if test="${flash.message}">
            <div class="message">${flash.message}</div>
            </g:if>
            <div class="list">
                <table>
                    <thead>
                        <tr>
                        
                            <g:sortableColumn property="id" title="${message(code: 'listaCobroProceso.id.label', default: 'Id')}" />
                        
                            <th><g:message code="listaCobroProceso.listaCobro.label" default="Lista Cobro" /></th>
                        
                            <g:sortableColumn property="fechaAplicacion" title="${message(code: 'listaCobroProceso.fechaAplicacion.label', default: 'Fecha Aplicacion')}" />
                        
                            <g:sortableColumn property="comentarios" title="${message(code: 'listaCobroProceso.comentarios.label', default: 'Comentarios')}" />
                        
                            <th><g:message code="listaCobroProceso.estatusListaCobro.label" default="Estatus Lista Cobro" /></th>
                        
                            <th><g:message code="listaCobroProceso.usuario.label" default="Usuario" /></th>
                        
                        </tr>
                    </thead>
                    <tbody>
                    <g:each in="${listaCobroProcesoInstanceList}" status="i" var="listaCobroProcesoInstance">
                        <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
                        
                            <td><g:link action="show" id="${listaCobroProcesoInstance.id}">${fieldValue(bean: listaCobroProcesoInstance, field: "id")}</g:link></td>
                        
                            <td>${fieldValue(bean: listaCobroProcesoInstance, field: "listaCobro")}</td>
                        
                            <td><g:formatDate date="${listaCobroProcesoInstance.fechaAplicacion}" /></td>
                        
                            <td>${fieldValue(bean: listaCobroProcesoInstance, field: "comentarios")}</td>
                        
                            <td>${fieldValue(bean: listaCobroProcesoInstance, field: "estatusListaCobro")}</td>
                        
                            <td>${fieldValue(bean: listaCobroProcesoInstance, field: "usuario")}</td>
                        
                        </tr>
                    </g:each>
                    </tbody>
                </table>
            </div>
            <div class="paginateButtons">
                <g:paginate total="${listaCobroProcesoInstanceTotal}" />
            </div>
        </div>
    </body>
</html>
