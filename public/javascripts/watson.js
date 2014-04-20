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
	
	$(".review-request-box").submit(function(e) {
		e.preventDefault();
		var uris = $("#"+this.id+" .rr-uri").val();
		var email = $("#"+this.id+" .rr-email").val();
		var notes = $("#"+this.id+" .rr-notes").val();
		$("#"+this.id+" button").focus().blur();
		requestReview(uris, email, notes);
	});
	
	$(".update-request").click(function() {
		$(this).focus().blur();
		var requestId = $("#request-id").data("id");
		var reason = $(this).data("reason");
		updateReviewRequest(requestId, reason);
	});
	
	$("#bulk-close-reviews").submit(function(e) {
		e.preventDefault();
		$("#btn-close-as").focus().blur();
		var requestIds = new Array();
		$(".selectable input[type=checkbox]:checked").each(function() {
			var id = $(this).val();
			if (!isNaN(id)) {
				requestIds.push(parseInt(id));
			}
		});
		var reason = $("#close-as").val();
		updateReviewRequests(requestIds, reason);
	});
	
	$(".update-review").click(function() {
		$(this).focus().blur();
		updateReviewStatus($("#review-id").data("id"), $(this).data("status"));
	});
	
	$(".add-note-form").submit(function(e) {
		e.preventDefault();
		$(".add-note-form button").blur();
		addNote($(this).data("model"), $(this).data("id"), $("#note").val());
	});
	
	$("#review-test-data-save").click(function() {
		$(this).focus().blur();
		saveReviewTestData(false, false);
	});
	
	$("#review-test-data-save-bad").click(function() {
		$(this).focus().blur();
		saveReviewTestData(true, false);
	});
	
	$("#review-test-data-save-bad-next").click(function() {
		$(this).focus().blur();
		saveReviewTestData(true, true);
	});
	
	$(".new-cr-box").submit(function(e) {
		e.preventDefault();
		var uris = $(".cr-uris").val();
		var description = $(".cr-description").val();
		var badCode = $(".cr-bad-code").val();
		var crType = $("#type").val();
		var crSource = $("#source").val();
		$("#"+this.id+" button").focus().blur();
		addCommunityReports(uris, description, badCode, crType, crSource)
	});
	
	$("#add-request-response").submit(function(e) {
		e.preventDefault();
		$("#"+this.id+" button").focus().blur();
		var question = $("#question").val();
		var answers = new Array();
		$("#add-request-response .answer").each(function() {
			var answer = $(this).val();
			if (answer) {
				answers.push(answer);
			}
		});
		addRequestResponse(question, answers);
	});
	
	$(".toggle-response").click(function() {
		$(this).focus().blur();
		toggleResponse(this.id);
	});
	
	$(".refresh").click(function() {
		location.reload(true);
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
	
	ZeroClipboard.config({
		forceHandCursor: true,
		moviePath: "/assets/ZeroClipboard.swf" 
	});
	var zc = new ZeroClipboard($(".clipboard"));
	
	if ($("#associated-uris").length) {
		renderAssociatedUris(".associated-uris-data", "#associated-uris");
		addAssociatedUriInput("#associated-uris");
	}
	
	$(".date-picker").daterangepicker({
		ranges: {
			"Today": [new Date(), new Date()],
			"Yesterday": [moment().subtract("days", 1), moment().subtract("days", 1)],
			"Last 7 Days": [moment().subtract("days", 6), new Date()],
			"Last 30 Days": [moment().subtract("days", 29), new Date()],
			"This Month": [moment().startOf("month"), moment().endOf("month")],
			"Last Month": [moment().subtract("month", 1).startOf("month"), moment().subtract("month", 1).endOf("month")]
		},
		format: "DD MMM YYYY",
		opens: "left"
	});
	
	$(".has-char-counter").keyup(function() {
		$("#"+this.id+"-used-char-ctr").text($(this).val().length);
	});
	
	$("#badware-category").change(function() {
		if ($("#badware-category").val() == "EXECUTABLE") {
			$("#executable-hash").prop("disabled", false);
		} else {
			$("#executable-hash").val("");
			$("#executable-hash-alert").hide();
			$("#executable-hash").prop("disabled", true);
		}
	});
	
	$("#executable-hash").focusout(function() {
		if ($(this).val().length != 64) {
			$("#executable-hash-alert").show();
		} else {
			$("#executable-hash-alert").hide();
		}
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
	                    {id:"#requests-table",fields:["status", "email", "requested"]},
	                    {id:"#crs-table",fields:["type", "source", "reported"]}];
	tableFilters.map(function(table) {
		if ($(table.id).length) {
			if ($(table.id+" tbody tr").length == 0) {
				$(".no-results-msg").show();
				$(".hide-on-no-results").hide();
			}
			setFilterInputs(table.fields);
		}
	});
	
	var tableSorts = [{id:"#blacklist-events-table",sortOn:1},
	              {id:"#reviews-requests-table",sortOn:4},
	              {id:"#reviews-others-table",sortOn:2},
	              {id:"#reviews-rescans-table",sortOn:3},
	              {id:"#reviews-table",sortOn:4},
	              {id:"#requests-table",sortOn:4},
	              {id:"#crs-table",sortOn:5}];
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
		window.setTimeout(function() {
			addAssociatedUriInput(id);
		}, 200);
	});
}

function associatedUriInput(index) {
	return '<li id="associated-'+index+'" data-index="'+index+'">'+
	'<input type="url" id="associated-uri-'+index+'" class="input-large form-control uri inline" placeholder="Associated URI">'+
	'<select id="associated-resolved-'+index+'" class="inline form-control auto-width">'+
	'<option value="RESOLVED">Resolved</option>'+
	'<option value="DNR">Did Not Resolve</option>'+
	'<option value="">Unchecked</option>'+
	'</select>'+
	'<select id="associated-type-'+index+'" class="inline form-control auto-width">'+
	'<option value="PAYLOAD">Payload</option>'+
	'<option value="INTERMEDIARY">Intermediary</option>'+
	'<option value="LANDING">Landing</option>'+
	'<option value="">Unknown</option>'+
	'</select>'+
	'<select id="associated-intent-'+index+'" class="inline form-control auto-width">'+
	'<option value="HACKED">Hacked</option>'+
	'<option value="MALICIOUS">Malicious</option>'+
	'<option value="FREE_HOST">Free Host</option>'+
	'<option value="">Unknown</option>'+
	'</select>';
}

function renderAssociatedUris(id, listId) {
	$(id).each(function(index) {
		$(listId).append(associatedUriInput(index));
		$("#associated-uri-"+index).val($(this).data("uri"));
		if ($(this).data("resolved")) {
			$("#associated-resolved-"+index).val("RESOLVED");
		} else if ($(this).data("resolved") === "") {
			$("#associated-resolved-"+index).val("");
		} else {
			$("#associated-resolved-"+index).val("DNR");
		}
		$("#associated-type-"+index).val($(this).data("type"));
		$("#associated-intent-"+index).val($(this).data("intent"));
	});
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

function requestReview(uris, email, notes) {
	var validEmail = isValidEmail(email);
	var hasUris = uris && uris.length > 0;
	if ($(".form-info").is(":hidden")) {
		scrollToBottom();
		if (validEmail && hasUris) {
			$(".form-alert, .form-success").hide();
			$(".form-info").show();
			var obj = {
				"uris": uris,
				"email": email,
				"notes": notes
			};
			appRoute.requestReview().ajax({
				contentType: jsonContentType,
				data: JSON.stringify(obj)
			}).done(function(res) {
				if (res.id) {
					var msg = "Review Request <a href=\"/requests/"+res.id+"\">"+res.id+"</a> Created";
					$(".success-msg").html(msg);
				}
				$(".form-success").show();
			}).fail(function(res) {
				var msg = res.responseJSON.msg;
				if (msg && msg.length > 0) {
					$(".alert-msg").text(msg);
				}
				$(".form-alert").show();
			}).always(function() {
				$(".form-info").hide();
			});
		} else {
			if (!validEmail) {
				$(".alert-msg").text("Valid email required!");
			} else if (!hasUris) {
				$(".alert-msg").text("URI required!");
			}
			$(".form-alert").show();
		}
	}
}

function updateReviewStatus(reviewId, status) {
	var ajaxStatus = ".review-update-status ";
	if ($(ajaxStatus+".form-info").is(":hidden")) {
		$(".alert").hide();
		$(ajaxStatus+".form-info").show();
		var obj = {
			"id": reviewId,
			"status": status
		};
		appRoute.updateReviewStatus().ajax({
			contentType: jsonContentType,
			data: JSON.stringify(obj)
		}).done(function(res) {
			if (res.status && res.updated_at) {
				renderReviewStatus(res.status, res.updated_at, res.is_open)
			}
			$(ajaxStatus+".form-success").show();
		}).fail(function() {
			$(ajaxStatus+".form-alert").show();
		}).always(function() {
			$(ajaxStatus+".form-info").hide();
		});
	}	
}

function toggleReviewButtons(status, isOpen) {
	if (isOpen) {
		$(".is-open").prop("disabled", false);
		$(".is-closed").prop("disabled", true);
		if (status == "PENDING_BAD") {
			$(".is-pending-bad").prop("disabled", false);
		} else {
			$(".is-pending-bad").prop("disabled", true);
		}
	} else {
		$(".is-open, .is-pending-bad").prop("disabled", true);
		$(".is-closed").prop("disabled", false);
	}
}

function saveReviewTestData(markBad, advance) {
	var reviewId = $("#review-id").data("id");
	var category = $("#badware-category").val();
	var sha256 = $("#executable-hash").val();
	var badCode = $("#badcode").val();
	var associatedUris = new Array();
	$("#associated-uris li").each(function(i) {
		var uri = $("#associated-uri-"+i).val();
		if (uri.length > 0) {
			var au = {
				"uri": uri,
				"resolved": $("#associated-resolved-"+i).val(),
				"type": $("#associated-type-"+i).val(),
				"intent": $("#associated-intent-"+i).val()
			};
			associatedUris[i] = au;
		}
	});
	
	var ajaxStatus = ".review-test-data-status ";
	if ($(ajaxStatus+".form-info").is(":hidden") && $("#executable-hash-alert").is(":hidden")) {
		$(".alert").hide();
		$(ajaxStatus+".form-info").show();
		var obj = {
			"id": reviewId,
			"category": category,
			"sha256": sha256,
			"bad_code": badCode,
			"associated_uris": associatedUris,
			"mark_bad": markBad
		};
		appRoute.updateReviewTestData().ajax({
			contentType: jsonContentType,
			data: JSON.stringify(obj)
		}).done(function(res) {
			if (res) {
				if (markBad && res.status && res.updated_at) {
					renderReviewStatus(res.status, res.updated_at, res.is_open)
				}
			}
			$(ajaxStatus+".form-success").show();
			if (advance) {
				window.location = $("#next-review").attr("href");
			}
		}).fail(function() {
			$(ajaxStatus+".form-alert").show();
		}).always(function() {
			$(ajaxStatus+".form-info").hide();
		});
	}	
}

function renderReviewStatus(status, updatedAt, isOpen) {
	$("#status").text(status);
	prettifyEnums("#status");
	$("#status-updated").text(updatedAt);
	$("#status-updated").show();
	getDatesFromUnix("#status-updated", true);
	toggleReviewButtons(status, isOpen);
}

function updateReviewRequest(requestId, reason) {
	if ($(".form-info").is(":hidden")) {
		$(".form-alert, .form-success").hide();
		$(".form-info").show();
		var obj = {
			"id": requestId,
			"reason": reason
		};
		appRoute.closeReviewRequest().ajax({
			contentType: jsonContentType,
			data: JSON.stringify(obj)
		}).done(function(res) {
			if (res.closed_reason && res.closed_at) {
				$("#closed-reason").text(res.closed_reason);
				prettifyEnums("#closed-reason");
				$("#closed-at").text(res.closed_at);
				$("#closed-at").removeClass("non-vis");
				getDatesFromUnix("#closed-at", true);
			}
			$(".update-request").prop("disabled", true);
			$(".form-success").show();
		}).fail(function(res) {
			$(".form-alert").show();
		}).always(function() {
			$(".form-info").hide();
		});
	}	
}

function updateReviewRequests(requestIds, reason) {
	if ($(".form-info").is(":hidden")) {
		$(".form-alert, .form-success").hide();
		$(".form-info").show();
		var obj = {
			"ids": requestIds,
			"reason": reason
		};
		appRoute.closeReviewRequests().ajax({
			contentType: jsonContentType,
			data: JSON.stringify(obj)
		}).done(function(res) {
			if (res.msg) {
				$(".success-msg").text(res.msg);
			}
			$(".form-success").show();
		}).fail(function(res) {
			$(".form-alert").show();
		}).always(function() {
			$(".form-info").hide();
		});
	}	
}

function addNote(model, id, note) {
	var ajaxStatus = ".note-status ";
	if ($(ajaxStatus+".form-info").is(":hidden")) {
		$(".alert").hide();
		$(ajaxStatus+".form-info").show();
		var obj = {
			"model": model,
			"id": id,
			"note": note
		};
		appRoute.addNote().ajax({
			contentType: jsonContentType,
			data: JSON.stringify(obj)
		}).done(function(res) {
			if (res.notes) {
				res.notes.map(function(n) {
					var noteId = "#note-"+n.id;
					if ($(noteId).length == 0) {
						renderNote(n);
						getDatesFromUnix(noteId+" .unixtime", false);
						$(noteId+" span").animate({"background-color": "#DFF0D8"}, 25);
						$(noteId+" span").animate({"background-color": "rgba(0, 0, 0, 0)"}, 7500);
					}
				});
			}
			$("#note").val("");
			$(ajaxStatus+".form-success").show();
		}).fail(function() {
			$(ajaxStatus+".form-alert").show();
		}).always(function() {
			$(ajaxStatus+".form-info").hide();
		});
	}	
}

function renderNote(note) {
	var li = "<li id=\"note-"+note.id+"\"><label>"+note.author+"<span class=\"unixtime\">"+note.created_at+"</span>"+
		"</label><span class=\"note\"></span></li>";
	$(".notes").append(li);
	$("#note-"+note.id+" .note").text(note.note).html();
}

function addCommunityReports(uris, description, badCode, crType, crSource) {
	var hasUris = uris && uris.length > 0;
	if ($(".form-info").is(":hidden")) {
		scrollToBottom();
		if (hasUris) {
			$(".form-alert, .form-success").hide();
			$(".form-info").show();
			var obj = {
				"uris": uris,
				"description": description,
				"bad_code": badCode,
				"type": crType,
				"source": crSource
			};
			appRoute.submitCommunityReports().ajax({
				contentType: jsonContentType,
				data: JSON.stringify(obj)
			}).done(function(res) {
				if (res.msg) {
					$(".success-msg").html(res.msg);
				}
				$(".form-success").show();
			}).fail(function(res) {
				var msg = res.responseJSON.msg;
				if (msg && msg.length > 0) {
					$(".alert-msg").text(msg);
				}
				$(".form-alert").show();
			}).always(function() {
				$(".form-info").hide();
			});
		} else {
			$(".alert-msg").text("URI required!");
			$(".form-alert").show();
		}
	}
}

function addRequestResponse(question, answers) {
	var valid = question && answers && answers.length >= 2;
	if ($(".form-info").is(":hidden")) {
		scrollToBottom();
		if (valid) {
			$(".form-alert, .form-success").hide();
			$(".form-info").show();
			var obj = {
				"question": question,
				"answers": answers
			};
			appRoute.addResponse().ajax({
				contentType: jsonContentType,
				data: JSON.stringify(obj)
			}).done(function() {
				$(".form-success").show();
			}).fail(function(res) {
				$(".form-alert").show();
			}).always(function() {
				$(".form-info").hide();
			});
		} else {
			var msg = (!question) ? "Question required!" : "At least TWO answers are required";
			$(".alert-msg").text(msg);
			$(".form-alert").show();
		}
	}
}

function toggleResponse(id) {
	var button = $("#"+id);
	var isEnabled = button.hasClass("btn-success");
	var obj = {
		"id": id,
		"disable": isEnabled
	};
	appRoute.toggleResponse().ajax({
		contentType: jsonContentType,
		data: JSON.stringify(obj)
	}).done(function() {
		if (isEnabled) {
			button.addClass("btn-danger");
			button.removeClass("btn-success");
			button.text("Disabled");
		} else {
			button.addClass("btn-success");
			button.removeClass("btn-danger");
			button.text("Enabled");
		}
	}).fail(function(res) {
		alert("Toggle failed");
	});
}

function loginSubmit() {
	$("#btn-login").focus().blur();
	var email = $("#input-email").val();
	if ($(".form-info").is(":hidden")) {
		if (isSbwEmail(email)) {
			$(".form-alert").hide();
			$(".form-info").show();
			var obj = {
				"email": email,
				"pw": $("#input-password").val()
			};
			appRoute.login().ajax({
				contentType: jsonContentType,
				data: JSON.stringify(obj)
			}).done(function(res) {
				$("#login-well").hide("blind", 495);
				setTimeout(function() {$(".form-success").show("blind", 100)}, 500);
				var returnTo = (res.returnTo) ? res.returnTo : "/";
				window.location.replace(returnTo);
			}).fail(function() {
				$(".form-alert").show();
			}).always(function() {
				$(".form-info").hide();
			});
		} else {
			$(".form-alert").show();
		}
	}
}

function registerSubmit() {
	$("#register-btn").focus().blur();
	if ($(".form-info").is(":hidden") && regFormIsValidated()) {
		$(".form-alert").hide();
		$(".form-info").show();
		var obj = {
			"email": $("#input-email").val(),
			"pw": $("#input-password").val()
		};
		appRoute.createAccount().ajax({
			contentType: jsonContentType,
			data: JSON.stringify(obj)
		}).done(function(res) {
			if (res.created) {
				$("#register-well").hide("blind", 495);
				setTimeout(function() {$(".form-success").show("blind", 100)}, 500);
			} else {
				$(".form-alert").show();
			}
		}).fail(function() {
			$(".form-alert").show();
		}).always(function() {
			$(".form-info").hide();
		});
	}
}

function resetPwSubmit() {
	$("#btn-pwreset").focus().blur();
	var email = $("#input-email").val();
	if ($(".form-info").is(":hidden")) {
		if (isSbwEmail(email)) {
			$(".form-alert").hide();
			$(".form-info").show();
			var obj = {
				"email": email,
			};
			appRoute.sendPwResetEmail().ajax({
				contentType: jsonContentType,
				data: JSON.stringify(obj)
			}).done(function(res) {
				if (res.sent) {
					$("#pwreset-well").hide("blind", 495);
					setTimeout(function() {$(".form-success").show("blind", 100)}, 500);
				} else {
					$(".form-alert").show();
				}
			}).fail(function() {
				$(".form-alert").show();
			}).always(function() {
				$(".form-info").hide();
			});
		} else {
			$(".form-alert").show();
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

function isValidEmail(email) {
	return email && email.length > 0 && (/.+@.+\..+$/).test(email);
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

function scrollToBottom() {
	$("html, body").animate({ scrollTop: $(document).height()}, "slow");
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

function getDatesFromUnix(selector, full) {
	$(selector).each(function() {
		$(this).text(formatDate($(this).text(), full));
	});
}

function formatDate(unix, full) {
	if (isNaN(unix) || unix <= 0) {
		return "";
	} else {
		var date = new Date(unix * 1000);
		return (full) ? date.toString() : dateShortFormat(date);
	}
}

function dateShortFormat(date) {
	var regex = /^\w{3}\s(.*):\d{2}\s.*$/;
	return regex.exec(date)[1];
}

function initSortTable(tableId, sortCol) {
	if ($(tableId).length && $(tableId+" tbody tr").length) {
		$(tableId).trigger("sorton",[[[sortCol,1]]]);
	}
}