<%@page import="org.joda.time.DateTime"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="joda" uri="http://www.joda.org/joda/time/tags" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<html:xhtml />
<fmt:setBundle basename="resources.GiafInvoicesResources" />

<% final String contextPath = request.getContextPath(); %>

<div class="page-header">
    <h1>
        <span style="color: gray;"><spring:message code="title.sap.invoice.viewer" text="SAP Documents"/></span>
    </h1>
</div>

<div class="page-body">
    <c:set var="person" scope="request" value="${event.person}"/>
    <jsp:include page="../fenixedu-academic/accounting/heading-person.jsp"/>
    <jsp:include page="../fenixedu-academic/accounting/heading-event.jsp"/>

        <c:if test="${isSapIntegrator}">
            <form method="post" action="<%= contextPath %>/sap-invoice-viewer/${event.externalId}/calculateRequests" style="display: inline;">
                ${csrf.field()}
                <button type="submit" class="btn btn-default"><spring:message code="label.calculate.request" text="Calculate Request"/></button>
            </form>
            <form method="post" action="<%= contextPath %>/sap-invoice-viewer/${event.externalId}/sync" style="display: inline;">
                ${csrf.field()}
                <button type="submit" class="btn btn-info"><spring:message code="label.repeat.request" text="Sync"/></button>
            </form>
        </c:if>

    <div>
        <table class="table tdmiddle">
            <thead>
                <tr>
                    <th><spring:message code="label.sapRequest.created" text="Created"/></th>
                    <th><spring:message code="label.sapRequest.requestType" text="Request Type"/></th>
                    <th><spring:message code="label.sapRequest.documentNumber" text="Document Number"/></th>
                    <th><spring:message code="label.sapRequest.value" text="Value"/></th>
                    <th><spring:message code="label.sapRequest.advancement" text="Advancement"/></th>
                    <th><spring:message code="label.sapRequest.sent" text="Sent"/></th>
                    <th><spring:message code="label.sapRequest.integrated" text="Integrated"/></th>
                    <th><spring:message code="label.sapRequest.sapDocumentNumber" text="Sap Document Number"/></th>
                    <th><spring:message code="label.sapRequest.clientId" text="clientId"/></th>
                    <th></th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="sapRequest" items="${sapRequests}">
                    <tr
                        <c:if test="${sapRequest.ignore}">class="strikeLine"</c:if>
                        <c:if test="${not empty sapRequest.anulledRequest}">class="anulledRequest"</c:if>
                       >
                        <td>
                            <joda:format value="${sapRequest.whenCreated}" pattern="yyyy-MM-dd HH:mm:ss"/>
                        </td>
                        <td>
                            <spring:message code="label.document.type.${sapRequest.requestType}"/>
                        </td>
                        <td>
                            <c:if test="${sapRequest.requestType == 'INVOICE'}">
                                <a href="<%= contextPath %>/invoice-downloader/sap/${sapRequest.externalId}/details">
                                    ${sapRequest.documentNumber}
                                </a>
                            </c:if>
                            <c:if test="${sapRequest.requestType != 'INVOICE'}">
                                ${sapRequest.documentNumber}
                            </c:if>
                        </td>
                        <td>
                            ${sapRequest.value}
                        </td>
                        <td>
                            ${sapRequest.advancement}
                        </td>
                        <td>
                            <c:if test="${sapRequest.sent}">
                                <joda:format var="whenSent" value="${sapRequest.whenSent}" pattern="yyyy-MM-dd HH:mm:ss"/>
                                <span title="${whenSent}">
                                    <spring:message code="label.yes" text="Yes"/>
                                </span> 
                            </c:if>
                            <c:if test="${not sapRequest.sent}">
                                <spring:message code="label.no" text="No"/>
                            </c:if>
                        </td>
                        <td>
                            <c:if test="${sapRequest.integrated}">
                                <span class="glyphicon glyphicon glyphicon-ok" aria-hidden="true" style="color: green;"></span>
                            </c:if>
                            <c:if test="${sapRequest.sent}">
                                <c:if test="${not sapRequest.integrated}">
                                    <span class="glyphicon glyphicon glyphicon-remove" aria-hidden="true" style="color: red;"></span>
                                </c:if>
                            </c:if>
                        </td>
                        <td>
                            <c:if test="${not empty sapRequest.sapDocumentNumber}">
                                <a href="<%= contextPath %>/invoice-downloader/sap/${sapRequest.externalId}/${sapRequest.documentNumber}.pdf">
                                    ${sapRequest.sapDocumentNumber}
                                </a>
                            </c:if>
                        </td>
                        <td>
                            ${sapRequest.clientId}
                        </td>
                        <td>
                            <a href="#" class="btn btn-default" onclick="displayDetails('srd${sapRequest.externalId}'); return false;">
                                <spring:message code="label.details" text="Details"/>
                            </a>
                        </td>
                    </tr>
                    <tr id="srd${sapRequest.externalId}" style="background-color:#F0F0F0; display: none;">
                        <td colspan="10">
                            <div style="background-color:#F0F0F0; height: 400px; overflow: scroll;">
                                <h3>
                                    <spring:message code="label.request.id" text="Request ID"/>: ${sapRequest.externalId}
                                <c:if test="${isPaymentManager}">
                                    <c:if test="${sapRequest.isAvailableForTransfer and not sapRequest.integrated and sapRequest.requestType == 'INVOICE'}">
                                        <a href="<%= contextPath %>/sap-invoice-viewer/${sapRequest.externalId}/transfer" class="btn btn-info">
                                            <spring:message code="label.transfer" text="Transfer"/>
                                        </a>
                                    </c:if>
                                </c:if>
                                <c:if test="${isSapIntegrator}">
                                    <c:if test="${not sapRequest.integrated}">
                                        <form method="post" action="<%= contextPath %>/sap-invoice-viewer/${sapRequest.externalId}/delete" style="display: inline;"
                                            onsubmit="return confirm('<spring:message code="label.delete.confirm" text="Are you sure?"/>');">
                                            ${csrf.field()}
                                            <button type="submit" class="btn btn-warning"><spring:message code="label.delete" text="Delete Request"/></button>
                                        </form>
                                    </c:if>
                                    <c:if test="${sapRequest.canBeCanceled}">
                                        <form method="post" action="<%= contextPath %>/sap-invoice-viewer/${sapRequest.externalId}/cancel" style="display: inline;"
                                            onsubmit="return confirm('<spring:message code="label.cancel.confirm" text="Are you sure?"/>');">
                                            ${csrf.field()}
                                            <button type="submit" class="btn btn-warning"><spring:message code="label.cancel" text="Cancel Request"/></button>
                                        </form>
                                    </c:if>
                                    <c:if test="${sapRequest.canBeClosed}">
                                        <c:if test="${sapRequest.requestType == 'DEBT'}">
                                            <form method="post" action="<%= contextPath %>/sap-invoice-viewer/${sapRequest.externalId}/close" style="display: inline;"
                                                onsubmit="return confirm('<spring:message code="label.debt.close.confirm" text="Are you sure?"/>');">
                                                ${csrf.field()}
                                                <button type="submit" class="btn btn-warning"><spring:message code="label.debt.close" text="Close Debt"/></button>
                                            </form>
                                        </c:if>
                                        <c:if test="${sapRequest.requestType == 'INVOICE'}">
                                            <form method="post" action="<%= contextPath %>/sap-invoice-viewer/${sapRequest.externalId}/close" style="display: inline;"
                                                onsubmit="return confirm('<spring:message code="label.invoice.close.confirm" text="Are you sure?"/>');">
                                                ${csrf.field()}
                                                <button type="submit" class="btn btn-warning"><spring:message code="label.invoice.close" text="Close Invoice"/></button>
                                            </form>
                                        </c:if>
                                    </c:if>
                                </c:if>
                                </h3>
                                <h4>
                                    <spring:message code="label.request.integrationMessage" text="Error Messages"/>
                                </h4>
                                <div id="srdm${sapRequest.externalId}"></div>
                                <h4>
                                    <spring:message code="label.request" text="Request"/>
                                </h4>
                                <div id="srdr${sapRequest.externalId}"></div>
                            </div>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </div>
</div>

<c:forEach var="sapRequest" items="${sapRequests}">
    <script type="text/javascript">
        $(document).ready(function() {
        	var integrationMessageAsJson = ${sapRequest.integrationMessageAsJson};
        	var requestAsJson = ${sapRequest.requestAsJson};
            integrationPart('srdm${sapRequest.externalId}', integrationMessageAsJson);
            integrationPart('srdr${sapRequest.externalId}', requestAsJson);
        });
    </script>
</c:forEach>

<style>
<!--
.strikeLine {
    text-decoration: line-through;
}

.anulledRequest {
    background-color: #ffcaba;
}

.json {
    width: 1000px;
    overflow: scroll;
}

</style>

<script type="text/javascript">

    function displayDetails(id) {
    	if (getComputedStyle(document.getElementById(id), null).display === 'table-row') {
    		document.getElementById(id).style.display = 'none';
    	} else {
    	    document.getElementById(id).style.display = 'table-row';
    	}
    }

    function integrationPart(id, json) {
        var jv = new JSONViewer();
        try {
            jv.showJSON(json, -1, -1);
            document.getElementById(id).appendChild(jv.getContainer());
        } catch(e) {
        	document.getElementById(id).innerHTML = e + '<br/>' + '<pre class="json">' + json + '</pre>';
        }
    }

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
