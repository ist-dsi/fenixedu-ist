<!DOCTYPE html>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

<h1> SELECTS ALL COURSESSS!!!<small>11one</small></h1>
<spring:url var="formActionUrl" value="${action}"/>
<form class="form-horizontal" role="form">
  <div class="form-group">
    <label for="inputEmail3" class="col-sm-1 control-label">Remetente</label>
    <div class="col-sm-11">
	<label for="inputEmail3" class=" control-label"><strong>${message.selectedSender.title}</strong></label>
    </div>
  </div>
  <div class="form-group">
    <label for="inputPassword3" class="col-sm-1 control-label">Responder a</label>
    <div class="col-sm-11">
      <p><input type="checkbox">  OIOIOOI</p>
    </div>
  </div>
  <div class="form-group">
    <label for="inputPassword3" class="col-sm-1 control-label">Destinatarios (BCC)</label>
    <div class="col-sm-11">
      <p><input type="checkbox" disabled checked="checked">Alunos Pre-selecionados</p>
      <p><input type="checkbox">  OIOIOOI</p>
      <p><input type="checkbox">  OIOIOOI</p>
    </div>
  </div>
  <div class="form-group">
    <label for="inputPassword3" class="col-sm-1 control-label">Outros Destinatarios</label>
    <div class="col-sm-11">
      <input type="text" class="form-control">
    </div>
  </div>
  <div class="form-group">
    <label for="inputPassword3" class="col-sm-1 control-label">Assunto</label>
    <div class="col-sm-11">
      <input type="text" class="form-control">
    </div>
  </div>
  <div class="form-group">
  	<label for="inputPassword3" class="col-sm-1 control-label">Mensagem</label>
  	<div class="col-sm-11">
    	<textarea class="form-control" rows="5"></textarea>
    </div>
  </div>
</form>


<form:form modelAttribute="students" role="form" method="post" action="${formActionUrl}" enctype="multipart/form-data" class="form-horizontal">



<button type="submit" class="btn btn-default">
	<spring:message code="label.submit" />
</button>
</form:form>
