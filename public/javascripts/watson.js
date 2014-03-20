var appRoute = null;
var jsonContentType = "application/json; charset=UTF-8";

$(document).ready(function($) {
	appRoute = jsRoutes.controllers.Application;
	$("#login-box").submit(function(e) {
		e.preventDefault();
		loginSubmit();
	});
	
	$("#register-box #input-email, #pwreset-box #input-email").blur(function() {
		checkEmail("#"+this.id);
	});
	$("#register-box #input-password").blur(function() {
		checkPassword("#"+this.id);
		checkPasswordsMatch("#input-password", "#input-pw-confirm");
	});
	$("#register-box #input-pw-confirm").blur(function() {
		checkPasswordsMatch("#input-password", "#input-pw-confirm");
	});
	$("#register-box").submit(function(e) {
		e.preventDefault();
		checkEmail("#input-email");
		checkPassword("#input-password");
		checkPasswordsMatch("#input-password", "#input-pw-confirm");
		registerSubmit();
	});
	
	$("#pwreset-box").submit(function(e) {
		e.preventDefault();
		resetPwSubmit();
	});
	
	setActiveNav();
	$(".table-sorted").tablesorter({
		cssAsc: "header-sort-up",
		cssDesc: "header-sort-down"
	});
	getDatesFromUnix(".unixtime", false);
	getDatesFromUnix(".unixtime-full", true);
	prettifyEnums(".enum");
	ip4sToDots(".ipv4");
	tagBgs();
	
	if ($("#associated-uris").length) {
		addAssociatedUriInput("#associated-uris");
	}
	
	$(".date-picker").daterangepicker({
		ranges: {
			'Today': [new Date(), new Date()],
			'Yesterday': [moment().subtract('days', 1), moment().subtract('days', 1)],
			'Last 7 Days': [moment().subtract('days', 6), new Date()],
			'Last 30 Days': [moment().subtract('days', 29), new Date()],
			'This Month': [moment().startOf('month'), moment().endOf('month')],
			'Last Month': [moment().subtract('month', 1).startOf('month'), moment().subtract('month', 1).endOf('month')]
		},
		format: "DD MMM YYYY",
		opens: "left"
	});
	
	$(".has-char-counter").keyup(function() {
		$("#"+this.id+"-used-char-ctr").text($(this).val().length);
	});
	
	$("th.sorter-false").click(function() {
		var wasChecked = $("#"+this.id+" input").is(":checked");
		$("td.selectable").each(function() {
			if (wasChecked) {
				$("#"+this.id+" input").prop("checked", false);
				$("#"+this.id+" .glyphicon-unchecked").show();
				$("#"+this.id+" .glyphicon-check").hide();
			} else {
				$("#"+this.id+" input").prop("checked", true);
				$("#"+this.id+" .glyphicon-unchecked").hide();
				$("#"+this.id+" .glyphicon-check").show();
			}
		});
	});
	
	$("td.selectable, th.selectable").click(function() {
		var checkbox = $("#"+this.id+" input");
		if (checkbox.is(":checked")) {
			checkbox.prop("checked", false);
		} else {
			checkbox.prop("checked", true);
		}
		$("#"+this.id+" .glyphicon-unchecked").toggle();
		$("#"+this.id+" .glyphicon-check").toggle();
	});
	
	var tableFilters = [{id:"#reviews-table",fields:["status", "blacklisted", "created"]},
	                    {id:"#requests-table",fields:["status", "email", "requested"]}];
	tableFilters.map(function(table) {
		if ($(table.id).length) {
			if ($(table.id+" tbody tr").length == 0) {
				$(".no-results-msg").show();
				$(".hide-on-no-results").hide();
			}
			setFilterInputs(table.fields);
		}
	});
	
	var tableSorts = [{id:"#reviews-blacklist-table",sortOn:1},
	              {id:"#reviews-requests-table",sortOn:4},
	              {id:"#reviews-others-table",sortOn:2},
	              {id:"#reviews-rescans-table",sortOn:3},
	              {id:"#reviews-table",sortOn:4},
	              {id:"#requests-table",sortOn:4}];
	tableSorts.map(function(table) {
		initSortTable(table.id, table.sortOn);
	});
	
});

function setActiveNav() {
	var path = (window.location.pathname=="/") ? "dashboard" : window.location.pathname.substr(1);
	var nav = path.split("/")[0];
	if (nav) {
		$("#li-nav-"+nav).addClass("active");
	}
}

function tagBgs() {
	$(".tag").each(function() {
		$(this).css("background-color", "#"+$(this).data("tagbg"));
	});
}

function addAssociatedUriInput(id) {
	var selector = id + " li .uri";
	var index = ($(id+" li").last().data("index")==null) ? 0 : $(id+" li").last().data("index") + 1;
	if (emptyCount(selector) == 0) {
		$(id).append(associatedUriInput(index));
	}
	
	$(selector).focusout(function() {
		if (emptyCount(selector) > 1) {
			$(selector).each(function() {
				if (!$(this).val()) {
					$(this.parentElement).addClass("remove");
				}
			});
		}
		$(".remove").remove();
		addAssociatedUriInput(id);
	});
}

function associatedUriInput(index) {
	return '<li id="associated-'+index+'" data-index="'+index+'">'+
	'<input type="url" name="associated-uri-'+index+'" class="input-large form-control uri inline" placeholder="Associated URI">'+
	'<select name="associated-resolved-'+index+'" class="inline form-control auto-width">'+
	'<option value="resolved">Resolved</option>'+
	'<option value="dnr">Did Not Resolve</option>'+
	'<option value="unknown">Unknown</option>'+
	'</select>'+
	'<select name="associated-type-'+index+'" class="inline form-control auto-width">'+
	'<option value="payload">Payload</option>'+
	'<option value="intermediary">Intermediary</option>'+
	'<option value="unknown">Unknown</option>'+
	'</select>'+
	'<select name="associated-intent-'+index+'" class="inline form-control auto-width">'+
	'<option value="hacked">Hacked</option>'+
	'<option value="malicious">Malicious</option>'+
	'<option value="unknown">Unknown</option>'+
	'</select>';
}

function emptyCount(selector) {
	var empty = 0;
	$(selector).each(function() {
		empty = ($(this).val()) ? empty : empty+=1;
	});
	return empty;
}

function setFilterInputs(fields) {
	var ck = $.cookie();
	fields.map(function(field) {
		var value = ck[field];
		if (value && value.length > 0) {
			$("#"+field.toLowerCase()).val(value);
		}
	});
}

function loginSubmit() {
	$("#btn-login").focus().blur();
	var email = $("#input-email").val();
	if ($(".form-info").is(":hidden")) {
		if (isSbwEmail(email)) {
			$("#login-alert").hide();
			$("#login-info").show();
			var obj = {
				"email" : email,
				"pw" : $("#input-password").val()
			};
			appRoute.login().ajax({
				contentType: jsonContentType,
				data: JSON.stringify(obj)
			}).done(function(res) {
				$("#login-well").hide("blind", 495);
				setTimeout(function() {$("#login-success").show("blind", 100)}, 500);
				var returnTo = (res.returnTo) ? res.returnTo : "/";
				window.location.replace(returnTo);
			}).fail(function() {
				$("#login-alert").show();
			}).always(function() {
				$("#login-info").hide();
			});
		} else {
			$("#login-alert").show();
		}
	}
}

function registerSubmit() {
	$("#register-btn").focus().blur();
	if ($(".form-info").is(":hidden") && regFormIsValidated()) {
		$("#register-alert").hide();
		$("#register-info").show();
		var obj = {
			"email" : $("#input-email").val(),
			"pw" : $("#input-password").val()
		};
		appRoute.createAccount().ajax({
			contentType: jsonContentType,
			data: JSON.stringify(obj)
		}).done(function(res) {
			if (res.created) {
				$("#register-well").hide("blind", 495);
				setTimeout(function() {$("#register-success").show("blind", 100)}, 500);
			} else {
				$("#register-alert").show();
			}
		}).fail(function() {
			$("#register-alert").show();
		}).always(function() {
			$("#register-info").hide();
		});
	}
}

function resetPwSubmit() {
	$("#btn-pwreset").focus().blur();
	var email = $("#input-email").val();
	if ($(".form-info").is(":hidden")) {
		if (isSbwEmail(email)) {
			$("#pwreset-alert").hide();
			$("#pwreset-info").show();
			var obj = {
				"email" : email,
			};
			appRoute.sendPwResetEmail().ajax({
				contentType: jsonContentType,
				data: JSON.stringify(obj)
			}).done(function(res) {
				if (res.sent) {
					$("#pwreset-well").hide("blind", 495);
					setTimeout(function() {$("#pwreset-success").show("blind", 100)}, 500);
				} else {
					$("#pwreset-alert").show();
				}
			}).fail(function() {
				$("#pwreset-alert").show();
			}).always(function() {
				$("#pwreset-info").hide();
			});
		} else {
			$("#pwreset-alert").show();
		}
	}
}

function checkEmail(id) {
	var valid = false;
	var email = $(id).val();
	if (isSbwEmail(email)) {
		valid = true;
	}
	toggleValid(id, valid);
}

function isSbwEmail(email) {
	return email && email.length > 0 && (/.+@stopbadware.org$/).test(email);
}

function checkPassword(id) {
	var valid = false;
	var pw = $(id).val();
	if (pw && pw.length >= 10) {
		var validRegexes = 0;
		var regexes = [/[a-z]+/, /[A-Z]+/, /[0-9]+/, /[!@#$%^&*()\-_=+\"':;|<>?\[\]~]+/];
		regexes.map(function(regex) {
			if (regex.test(pw)) {
				validRegexes++;
			}
		});
		valid = validRegexes == regexes.length;
	}
	toggleValid(id, valid);
}

function checkPasswordsMatch(pwID, cpwID) {
	var pw = $(pwID).val();
	var cpw = $(cpwID).val();
	if (pw && cpw && cpw.length > 0) {
		(pw===cpw) ? toggleValid(cpwID, true) : toggleValid(cpwID, false);
	}
}

function regFormIsValidated() {
	var ids = ["#input-email-form-group", "#input-password-form-group", "#input-pw-confirm-form-group"];
	var validIds = 0;
	ids.map(function(id) {
		if ($(id).hasClass("has-success")) {
			validIds++;
		}
	});
	return validIds == ids.length;
}

function toggleValid(baseId, valid) {
	var group = $(baseId+"-form-group");
	var icon = $(baseId+"-feedback");
	if (valid) {
		group.removeClass("has-error").addClass("has-success");
		icon.removeClass("glyphicon-remove").addClass("glyphicon-ok");
	} else {
		group.removeClass("has-success").addClass("has-error");
		icon.removeClass("glyphicon-ok").addClass("glyphicon-remove");
	}
	
}

function prettifyEnums(selector) {
	$(selector).each(function() {
		$(this).text($(this).text().replace(/_/g, " "));
	});
}

function ip4sToDots(selector) {
	$(selector).each(function() {
		$(this).text(longToDots($(this).text()));
	});
}

function longToDots(ip) {
	var d = ip%256;
	for (var i = 3; i > 0; i--) { 
		ip = Math.floor(ip/256);
		d = ip%256 + '.' + d;
	}
	return d;
}

function getDatesFromUnix(selector, withTime) {
	$(selector).each(function() {
		$(this).text(formatDate($(this).text(), withTime));
	});
}

function formatDate(unix, withTime) {
	if (isNaN(unix)) {
		return unix;
	} else {
		var date = new Date(unix * 1000)
		return (withTime) ? date.toString() : date.toDateString();
	}
}

function initSortTable(tableId, sortCol) {
	if ($(tableId).length && $(tableId+" tbody tr").length) {
		$(tableId).trigger("sorton",[[[sortCol,1]]]);
	}
}