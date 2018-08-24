<%--

    Copyright � 2018 Instituto Superior T�cnico

    This file is part of FenixEdu IST GIAF Invoices.

    FenixEdu IST GIAF Invoices is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FenixEdu IST GIAF Invoices is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with FenixEdu IST GIAF Invoices.  If not, see <http://www.gnu.org/licenses/>.

--%>
<%@page import="org.fenixedu.bennu.core.security.Authenticate"%>
<%@page import="org.fenixedu.bennu.core.groups.Group"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<% final String contextPath = request.getContextPath(); %>

<style>
<!--
.json-viewer {
    color: #000;
    padding-left: 20px;
}

.json-viewer ul {
    list-style-type: none;
    margin: 0;
    margin: 0 0 0 1px;
    border-left: 1px dotted #ccc;
    padding-left: 2em;
}

.json-viewer .hide {
    display: none;
}

.json-viewer ul li .type-string,
.json-viewer ul li .type-date {
    color: #0B7500;
}

.json-viewer ul li .type-boolean {
    color: #1A01CC;
    font-weight: bold;
}

.json-viewer ul li .type-number {
    color: #1A01CC;
}

.json-viewer ul li .type-null {
    color: red;
}

.json-viewer a.list-link {
    color: #000;
    text-decoration: none;
    position: relative;
}

.json-viewer a.list-link:before {
    color: #aaa;
    content: "\25BC";
    position: absolute;
    display: inline-block;
    width: 1em;
    left: -1em;
}

.json-viewetent: "\25B6";
}

.json-viewer a.list-link.empty:before {
    contr a.list-link.collapsed:before {
    conent: "";
}

.json-viewer .items-ph {
    color: #aaa;
    padding: 0 1em;
}

.json-viewer .items-ph:hover {
    text-decoration: underline;
}
-->
</style>

<div class="page-header">
	<h1>
		<spring:message code="title.sap.invoice.viewer" text="SAP Documents"/>
	</h1>
</div>

<h3 id="NoResults" style="display: none;"><spring:message code="label.events.none" text="No events found." /></h3>

<table id="eventsTable" class="table tdmiddle" style="display: none;">
	<thead>
		<tr>
			<th rowspan="2"><spring:message code="label.event.id" text="Event ID"/></th>
			<th rowspan="2"><spring:message code="label.event.description" text="Event"/></th>
            <th colspan="3" style="text-align: center;"><spring:message code="label.event.debt" text="Debt"/></th>
            <th colspan="3" style="text-align: center;"><spring:message code="label.event.fine" text="Fine"/></th>
            <th colspan="3" style="text-align: center;"><spring:message code="label.event.interest" text="Interest"/></th>
            <th rowspan="2"></th>
            <th rowspan="2"></th>
            <th rowspan="2"></th>
		</tr>
		<tr>
            <th><spring:message code="label.event.total" text="Total"/></th>
            <th><spring:message code="label.event.exempt" text="Exempt"/></th>
            <th><spring:message code="label.event.payed" text="Payed"/></th>

            <th><spring:message code="label.event.total" text="Total"/></th>
            <th><spring:message code="label.event.exempt" text="Exempt"/></th>
            <th><spring:message code="label.event.payed" text="Payed"/></th>

            <th><spring:message code="label.event.total" text="Total"/></th>
            <th><spring:message code="label.event.exempt" text="Exempt"/></th>
            <th><spring:message code="label.event.payed" text="Payed"/></th>
		</tr>
	</thead>
	<tbody id="eventList">
	</tbody>
</table>

<div class="container">
  <!-- Modal -->
  <div class="modal fade" id="requestDetails" role="dialog">
    <div class="modal-dialog">
      <div class="modal-content">
        <div class="modal-header">
          <button type="button" class="close" data-dismiss="modal">&times;</button>
          <h4 class="modal-title"><spring:message code="label.sapRequest.requestAndResponseDetails" text="Integration Details"/></h4>
        </div>
        <div class="modal-body">
            <h5><spring:message code="label.sapRequest.response" text="Response"/></h5>
            <div id="sapResponseDetails"></div>
            <h5><spring:message code="label.sapRequest.request" text="Request"/></h5>
            <div id="sapRequestDetails"></div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
        </div>
      </div>
    </div>
  </div>
</div>

<script type="text/javascript">
	var events = ${events};
	var contextPath = '<%= contextPath %>';
    var jsonViewerRequest;
    var jsonViewerResponse;

	function integrationPart(sapRequest) {
		if (sapRequest.integrated) {
			if (sapRequest.integrationMessage) {
			    return '<span title="' + sapRequest.integrationMessage + '">' + '<spring:message code="label.yes" text="Yes"/> '
			         + '<span class="glyphicon glyphicon-info-sign">&nbsp;</span>' + '</span>';				
			} else {
				return '<spring:message code="label.yes" text="Yes"/>';
			}
		} else {
			return '<spring:message code="label.no" text="No"/>';
		}
	}

    function calculateRequests(eventId) {
    	<% if (Group.dynamic("managers").isMember(Authenticate.getUser())) { %>
        return '<form method="post" action="' + contextPath + '/sap-invoice-viewer/' + eventId + '/sync">'
        	   + '${csrf.field()}'
        	   + '<button type="submit" class="btn btn-warning"><spring:message code="label.calculate.request" text="Calculate Requests"/></button>'
        	   + '</form>'
        	   ;
        <% } else { %>
            return '';
        <% } %>
    }

    function syncEvent(eventId) {
    	<% if (Group.dynamic("managers").isMember(Authenticate.getUser())) { %>
    	return '<form method="post" action="' + contextPath + '/sap-invoice-viewer/' + eventId + '/sync">'
               + '${csrf.field()}'
               + '<button type="submit" class="btn btn-warning"><spring:message code="label.repeat.request" text="Sync"/></button>'
               + '</form>'
               ;
    	<% } else { %>
    	   return '';
        <% } %>
    }

    function deleteRequest(sapRequest) {
    	<% if (Group.dynamic("managers").isMember(Authenticate.getUser())) { %>
        if (!sapRequest.integrated) {
            return '<form method="post" action="' + contextPath + '/sap-invoice-viewer/' + sapRequest.id + '/delete">'
               + '${csrf.field()}'
               + '<button type="submit" class="btn btn-warning"><spring:message code="label.delete" text="Repeat Request"/></button>'
               + '</form>'
               ;
        }
        <% } %>
        return '';
    }

    function sentPart(sapRequest) {
        if (sapRequest.sent) {
            return '<span title="' + sapRequest.whenSent + '">' + '<spring:message code="label.yes" text="Yes"/> '
                + '<span class="glyphicon glyphicon-time">&nbsp;</span>' + '</span>';
        } else {
            return '<spring:message code="label.no" text="No"/>';
        }
    }

    function sapDocumentNumberPart(sapRequest) {
        if (sapRequest.sapDocumentNumber == null) {
        	return '';
        } else {
            var docName = sapRequest.sapDocumentNumber.replace("\/", "_");
            var link = contextPath + '/invoice-downloader/sap/' + sapRequest.id + '/' + docName + '.pdf';
            return '<a href="' + link + '">' + sapRequest.sapDocumentNumber + '</a>';
        }
    }

    function displayResponse(sapRequestId) {
    	var item;
    	$(events).each(function(i, event) {
    		$(event.sapRequests).each(function(j, sapRequest) {
    			if (sapRequest.id == sapRequestId) {
    				item = sapRequest;
    			}
    		});
    	});
    	jsonViewerRequest.showJSON(JSON.parse(item.request), -1, -1);
    	jsonViewerResponse.showJSON(JSON.parse(item.integrationMessage), -1, -1);
    }
    
    function requestPart(sapRequest) {
    	return '<span class="glyphicon glyphicon-list-alt" data-toggle="modal" data-target="#requestDetails" onclick="displayResponse(' + sapRequest.id + '); return false;">&nbsp;</span>';
    }

    function expandButton(i, hasSapRequests) {
    	if (hasSapRequests) {
    	    return '<a href="#" onclick="document.getElementById(\'eventDetailRow' + i +'\').style.display = \'table-row\'; return false;" class="btn btn-default"><spring:message code="label.view" text="View"/></a>'
    	} else {
    		return '';
    	}
    }

	$(document).ready(function() {
		jsonViewerRequest = new JSONViewer();
		jsonViewerResponse = new JSONViewer();
		document.querySelector("#sapRequestDetails").appendChild(jsonViewerRequest.getContainer());
        document.querySelector("#sapResponseDetails").appendChild(jsonViewerResponse.getContainer());

        if (events.length == 0) {
			document.getElementById("NoResults").style.display = 'block';
		} else {
			document.getElementById("eventsTable").style.display = 'block';
		}
        $(events).each(function(i, event) {
        	rowStyle = event.isCanceled ? "text-decoration: line-through;" : "";
        	var hasSapRequests = $(event.sapRequests).length > 0;
			row = $('<tr style="' + rowStyle + '"/>').appendTo($('#eventList'))
                .append($('<td/>').text(event.eventId))
                .append($('<td/>').text(event.eventDescription))

                .append($('<td/>').text(event.debtAmount))
                .append($('<td/>').text(event.debtExemptionAmount))
                .append($('<td/>').text(event.paidDebtAmount))

                .append($('<td/>').text(event.fineAmount))
                .append($('<td/>').text(event.fineExemptionAmount))
                .append($('<td/>').text(event.paidFineAmount))

                .append($('<td/>').text(event.interestAmount))
                .append($('<td/>').text(event.interestExemptionAmount))
                .append($('<td/>').text(event.paidInterestAmount))
                .append($('<td/>').html(expandButton(i, hasSapRequests)))
                .append($('<td/>').html(calculateRequests(event.eventId)))
                .append($('<td/>').html(syncEvent(event.eventId)))
                ;

			if (hasSapRequests) {
			    sapTable = $('<table class="table" style="background-color:#F0F0F0;"/>');
			    $('<tr id="eventDetails' + i + '"/>').appendTo(sapTable)
                    .append($('<th/>').text('<spring:message code="label.sapRequest.created" text="Created"/>'))
                    .append($('<th/>').text('<spring:message code="label.sapRequest.requestType" text="Request Type"/>'))
                    .append($('<th/>').text('<spring:message code="label.sapRequest.documentNumber" text="Document Number"/>'))
                    .append($('<th/>').text('<spring:message code="label.sapRequest.value" text="Value"/>'))
                    .append($('<th/>').text('<spring:message code="label.sapRequest.advancement" text="Advancement"/>'))
                    .append($('<th/>').text('<spring:message code="label.sapRequest.sent" text="Sent"/>'))
                    .append($('<th/>').text('<spring:message code="label.sapRequest.integrated" text="Integrated"/>'))
                    .append($('<th/>').text('<spring:message code="label.sapRequest.sapDocumentNumber" text="Sap Document Number"/>'))
                    .append($('<th/>').text('<spring:message code="label.sapRequest.request" text="Request / Response"/>'))
                    .append($('<th/>').text('<spring:message code="label.sapRequest.clientId" text="clientId"/>'))
                    .append($('<th/>').text(''))
                    ;
			    $('<tr id="eventDetailRow' + i + '" style="display: none;"/>').appendTo($('#eventList'))
                    .append($('<td colspan="12"/>').html(sapTable));
			    $(event.sapRequests).each(function(j, sapRequest) {
			        $('<tr/>').appendTo(sapTable)
                        .append($('<td/>').text(sapRequest.whenCreated))
                        .append($('<td/>').text(sapRequest.requestType))
                        .append($('<td/>').text(sapRequest.documentNumber))
                        .append($('<td/>').text(sapRequest.value))
                        .append($('<td/>').text(sapRequest.advancement))
                        .append($('<td/>').html(sentPart(sapRequest)))
                        .append($('<td/>').html(integrationPart(sapRequest)))
                        .append($('<td/>').html(sapDocumentNumberPart(sapRequest)))
                        .append($('<td/>').html(requestPart(sapRequest)))

//                        .append($('<td/>').text(sapRequest.whenSent))
//                        .append($('<td/>').text(sapRequest.integrationMessage))
                        .append($('<td/>').text(sapRequest.clientId))
//                        .append($('<td/>').text(sapRequest.integrationMessage))
                        .append($('<td/>').html(deleteRequest(sapRequest)))
                        ;
			    });
			}
        });
	});


	JSONViewer = (function() {
	    var JSONViewer = function() {
	        this._dom = {};
	        this._dom.container = document.createElement("pre");
	        this._dom.container.classList.add("json-viewer");
	    };

	    /**
	     * Visualise JSON object.
	     * 
	     * @param {Object|Array} json Input value
	     * @param {Number} [maxLvl] Process only to max level, where 0..n, -1 unlimited
	     * @param {Number} [colAt] Collapse at level, where 0..n, -1 unlimited
	     */
	    JSONViewer.prototype.showJSON = function(json, maxLvl, colAt) {
	        maxLvl = typeof maxLvl === "number" ? maxLvl : -1; // max level
	        colAt = typeof colAt === "number" ? colAt : -1; // collapse at

	        var jsonData = this._processInput(json);
	        var walkEl = this._walk(jsonData, maxLvl, colAt, 0);

	        this._dom.container.innerHTML = "";
	        this._dom.container.appendChild(walkEl);
	    };

	    /**
	     * Get container with pre object - this container is used for visualise JSON data.
	     * 
	     * @return {Element}
	     */
	    JSONViewer.prototype.getContainer = function() {
	        return this._dom.container;
	    };

	    /**
	     * Process input JSON - throws exception for unrecognized input.
	     * 
	     * @param {Object|Array} json Input value
	     * @return {Object|Array}
	     */
	    JSONViewer.prototype._processInput = function(json) {
	        if (json && typeof json === "object") {
	            return json;
	        }
	        else {
	            throw "Input value is not object or array!";
	        }
	    };

	    /**
	     * Recursive walk for input value.
	     * 
	     * @param {Object|Array} value Input value
	     * @param {Number} maxLvl Process only to max level, where 0..n, -1 unlimited
	     * @param {Number} colAt Collapse at level, where 0..n, -1 unlimited
	     * @param {Number} lvl Current level
	     */
	    JSONViewer.prototype._walk = function(value, maxLvl, colAt, lvl) {
	        var frag = document.createDocumentFragment();
	        var isMaxLvl = maxLvl >= 0 && lvl >= maxLvl;
	        var isCollapse = colAt >= 0 && lvl >= colAt;

	        switch (typeof value) {
	            case "object":
	                if (value) {
	                    var isArray = Array.isArray(value);
	                    var items = isArray ? value : Object.keys(value);

	                    if (lvl === 0) {
	                        // root level
	                        var rootCount = this._createItemsCount(items.length);
	                        // hide/show
	                        var rootLink = this._createLink(isArray ? "[" : "{");

	                        if (items.length) {
	                            rootLink.addEventListener("click", function() {
	                                if (isMaxLvl) return;

	                                rootLink.classList.toggle("collapsed");
	                                rootCount.classList.toggle("hide");

	                                // main list
	                                this._dom.container.querySelector("ul").classList.toggle("hide");
	                            }.bind(this));

	                            if (isCollapse) {
	                                rootLink.classList.add("collapsed");
	                                rootCount.classList.remove("hide");
	                            }
	                        }
	                        else {
	                            rootLink.classList.add("empty");
	                        }

	                        rootLink.appendChild(rootCount);
	                        frag.appendChild(rootLink);
	                    }

	                    if (items.length && !isMaxLvl) {
	                        var len = items.length - 1;
	                        var ulList = document.createElement("ul");
	                        ulList.setAttribute("data-level", lvl);
	                        ulList.classList.add("type-" + (isArray ? "array" : "object"));

	                        items.forEach(function(key, ind) {
	                            var item = isArray ? key : value[key];
	                            var li = document.createElement("li");

	                            if (typeof item === "object") {
	                                var isEmpty = false;

	                                // null && date
	                                if (!item || item instanceof Date) {
	                                    li.appendChild(document.createTextNode(isArray ? "" : key + ": "));
	                                    li.appendChild(this._createSimple(item ? item : null));
	                                }
	                                // array & object
	                                else {
	                                    var itemIsArray = Array.isArray(item);
	                                    var itemLen = itemIsArray ? item.length : Object.keys(item).length;

	                                    // empty
	                                    if (!itemLen) {
	                                        li.appendChild(document.createTextNode(key + ": " + (itemIsArray ? "[]" : "{}")));
	                                    }
	                                    else {
	                                        // 1+ items
	                                        var itemTitle = (typeof key === "string" ? key + ": " : "") + (itemIsArray ? "[" : "{");
	                                        var itemLink = this._createLink(itemTitle);
	                                        var itemsCount = this._createItemsCount(itemLen);

	                                        // maxLvl - only text, no link
	                                        if (maxLvl >= 0 && lvl + 1 >= maxLvl) {
	                                            li.appendChild(document.createTextNode(itemTitle));
	                                        }
	                                        else {
	                                            itemLink.appendChild(itemsCount);
	                                            li.appendChild(itemLink);
	                                        }

	                                        li.appendChild(this._walk(item, maxLvl, colAt, lvl + 1));
	                                        li.appendChild(document.createTextNode(itemIsArray ? "]" : "}"));
	                                        
	                                        var list = li.querySelector("ul");
	                                        var itemLinkCb = function() {
	                                            itemLink.classList.toggle("collapsed");
	                                            itemsCount.classList.toggle("hide");
	                                            list.classList.toggle("hide");
	                                        };

	                                        // hide/show
	                                        itemLink.addEventListener("click", itemLinkCb);

	                                        // collapse lower level
	                                        if (colAt >= 0 && lvl + 1 >= colAt) {
	                                            itemLinkCb();
	                                        }
	                                    }
	                                }
	                            }
	                            // simple values
	                            else {
	                                // object keys with key:
	                                if (!isArray) {
	                                    li.appendChild(document.createTextNode(key + ": "));
	                                }

	                                // recursive
	                                li.appendChild(this._walk(item, maxLvl, colAt, lvl + 1));
	                            }

	                            // add comma to the end
	                            if (ind < len) {
	                                li.appendChild(document.createTextNode(","));
	                            }

	                            ulList.appendChild(li);
	                        }, this);

	                        frag.appendChild(ulList);
	                    }
	                    else if (items.length && isMaxLvl) {
	                        var itemsCount = this._createItemsCount(items.length);
	                        itemsCount.classList.remove("hide");

	                        frag.appendChild(itemsCount);
	                    }

	                    if (lvl === 0) {
	                        // empty root
	                        if (!items.length) {
	                            var itemsCount = this._createItemsCount(0);
	                            itemsCount.classList.remove("hide");

	                            frag.appendChild(itemsCount);
	                        }

	                        // root cover
	                        frag.appendChild(document.createTextNode(isArray ? "]" : "}"));

	                        // collapse
	                        if (isCollapse) {
	                            frag.querySelector("ul").classList.add("hide");
	                        }
	                    }
	                    break;
	                }

	            default:
	                // simple values
	                frag.appendChild(this._createSimple(value));
	                break;
	        }

	        return frag;
	    };

	    /**
	     * Create simple value (no object|array).
	     * 
	     * @param  {Number|String|null|undefined|Date} value Input value
	     * @return {Element}
	     */
	    JSONViewer.prototype._createSimple = function(value) {
	        var spanEl = document.createElement("span");
	        var type = typeof value;
	        var txt = value;

	        if (type === "string") {
	            txt = '"' + value + '"';
	        }
	        else if (value === null) {
	            type = "null";
	            txt = "null";
	        }
	        else if (value === undefined) {
	            txt = "undefined";
	        }
	        else if (value instanceof Date) {
	            type = "date";
	            txt = value.toString();
	        }

	        spanEl.classList.add("type-" + type);
	        spanEl.innerHTML = txt;

	        return spanEl;
	    };

	    /**
	     * Create items count element.
	     * 
	     * @param  {Number} count Items count
	     * @return {Element}
	     */
	    JSONViewer.prototype._createItemsCount = function(count) {
	        var itemsCount = document.createElement("span");
	        itemsCount.classList.add("items-ph");
	        itemsCount.classList.add("hide");
	        itemsCount.innerHTML = this._getItemsTitle(count);

	        return itemsCount;
	    };

	    /**
	     * Create clickable link.
	     * 
	     * @param  {String} title Link title
	     * @return {Element}
	     */
	    JSONViewer.prototype._createLink = function(title) {
	        var linkEl = document.createElement("a");
	        linkEl.classList.add("list-link");
	        linkEl.href = "javascript:void(0)";
	        linkEl.innerHTML = title || "";

	        return linkEl;
	    };

	    /**
	     * Get correct item|s title for count.
	     * 
	     * @param  {Number} count Items count
	     * @return {String}
	     */
	    JSONViewer.prototype._getItemsTitle = function(count) {
	        var itemsTxt = count > 1 || count === 0 ? "items" : "item";

	        return (count + " " + itemsTxt);
	    };

	    return JSONViewer;
	})();
</script>
