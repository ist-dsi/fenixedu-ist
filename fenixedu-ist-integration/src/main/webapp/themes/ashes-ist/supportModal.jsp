<%--

    Copyright © 2013 Instituto Superior Técnico

    This file is part of FenixEdu IST Integration.

    FenixEdu IST Integration is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FenixEdu IST Integration is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with FenixEdu IST Integration.  If not, see <http://www.gnu.org/licenses/>.

--%>
<%@ taglib uri="http://fenix-ashes.ist.utl.pt/fenix-renderers" prefix="fr" %>

<div class="modal-dialog modal-lg">
    <div class="modal-content">
        <form id="supportForm" class="form-horizontal">
        <input type="hidden" name="userAgent" value="${pageContext.request.getHeader('User-Agent')}" />
        <input type="hidden" name="referer" value="${pageContext.request.getHeader('Referer')}" />
        <div class="modal-header">
            <button type="button" class="close" data-dismiss="modal"><span aria-hidden="true">&times;</span><span class="sr-only">Close</span></button>
            <h4 class="modal-title" id="myModalLabel">${fr:message('resources.FenixeduIstIntegrationResources', 'label.support.form')}</h4>
        </div>
        <div class="modal-body form-body">
            ${fr:message('resources.FenixeduIstIntegrationResources', 'label.support.form.welcome')}
            <hr />
                <div class="form-group">
                    <label class="col-sm-2 control-label">${fr:message('resources.FenixeduIstIntegrationResources', 'label.error.page.support')}:</label>
                    <div class="col-sm-10">
                        <select name="support" id="support" class="form-control">
                        </select>
                    </div>
                </div>
                <div class="form-group">
                    <label for="subject" class="col-sm-2 control-label">${fr:message('resources.FenixeduIstIntegrationResources', 'label.error.page.subject')}:</label>
                    <div class="col-sm-10">
                        <input type="text" name="subject" id="subject" class="form-control" required />
                    </div>
                </div>
                <div class="form-group">
                    <label for="description" class="col-sm-2 control-label">${fr:message('resources.FenixeduIstIntegrationResources', 'label.error.page.description')}:</label>
                    <div class="col-sm-10">
                        <textarea id="description" name="description" rows="5" style="width: 100%" required></textarea>
                        <small>${fr:message('resources.FenixeduIstIntegrationResources', 'label.error.page.help')}</small>
                    </div>
                </div>
                <div class="form-group">
                    <label for="type" class="col-sm-2 control-label">${fr:message('resources.FenixeduIstIntegrationResources', 'label.support.form.type')}:</label>
                    <div class="col-sm-10">
                        <label class="radio-inline">
                            <input type="radio" name="type" id="type-error" value="error" checked>
                            ${fr:message('resources.FenixeduIstIntegrationResources', 'label.support.form.type.error')}
                        </label>
                        <label class="radio-inline">
                            <input type="radio" name="type" id="type-request" value="request" required>
                            ${fr:message('resources.FenixeduIstIntegrationResources', 'label.support.form.type.request')}
                        </label>
                        <label class="radio-inline">
                            <input type="radio" name="type" id="type-question" value="question">
                            ${fr:message('resources.FenixeduIstIntegrationResources', 'label.support.form.type.question')}
                        </label>
                    </div>
                </div>
                <div class="form-group">
                    <label for="attachment" class="col-sm-2 control-label">${fr:message('resources.FenixeduIstIntegrationResources', 'label.support.form.attachment')}:</label>
                    <div class="col-sm-10">
                        <div class="alert alert-danger" id="largeFile" style="display: none">
                            ${fr:message('resources.FenixeduIstIntegrationResources', 'label.support.form.attachment.file.too.large')}
                        </div>
                        <input type="file" name="attachment" id="attachment" />
                        <small>${fr:message('resources.FenixeduIstIntegrationResources', 'label.support.form.attachment.help')}</small>
                    </div>
                </div>
        </div>
        <div class="modal-body success text-center hide">
            <h3>${fr:message('resources.FenixeduIstIntegrationResources', 'message.error.page.submitted')}</h3>
            ${fr:message('resources.FenixeduIstIntegrationResources', 'message.error.page.submitted.body')} <a href="mailto:dsi@tecnico.ulisboa.pt">dsi@tecnico.ulisboa.pt</a>.
        </div>
        <div class="modal-footer">
            <button type="submit" class="btn btn-primary">${fr:message('resources.FenixeduIstIntegrationResources', 'label.error.page.submit')}</button>
        </div>
        </form>
    </div>
</div>

<script>
var data = { 'functionality': window.current$functionality };
$.get('${pageContext.request.contextPath}/api/fenix-ist/support-form/' + window.current$functionality, function(data){
  $('#support').append($('<option>', {
            value: data.default.id,
            text : data.default.title
  }));
  $.each(data.options, function (i, item) {
      $('#support').append($('<option>', {
          value: item.id,
          text : item.title
      }));
  });
});


$.ajaxSetup({ headers: {'Content-Type':'application/json; charset=UTF-8'} });
$('#supportForm input[type=file]').on('change', function (event) {
    var file = event.target.files[0]; $('#largeFile').hide();
    if(file.size > 5 * 1024 * 1024) { $('#largeFile').show(); return; }
    var reader = new FileReader();
    data['fileName'] = file.name; data['mimeType'] = file.type;
    reader.onload = function (e) {
        var content = e.target.result;
        data['attachment'] = content.substr(content.indexOf(",") + 1, content.length);
    };
    reader.readAsDataURL(file);
});
$('#supportForm').on('submit', function(event) {
    event.preventDefault(); var target = $(event.target); $.map(target.serializeArray(), function(n, i){ data[n['name']] = n['value']; });
    target.find('button[type=submit]').html("${fr:message('resources.FenixeduIstIntegrationResources', 'label.error.page.submitting')}...");
    target.find('button[type=submit]').attr('disabled', true);
    $.post('${pageContext.request.contextPath}/api/fenix-ist/support-form', JSON.stringify(data), function () {
        target.find('.success').removeClass('hide'); target.find('.modal-footer').hide(); target.find('.form-body').hide();
    });
});
</script>