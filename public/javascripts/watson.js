var appRoute = null;
var jsonContentType = "application/json; charset=UTF-8";

$(document).ready(function($) {
	appRoute = jsRoutes.controllers.Application;
	$("#register-box #input-email").blur(function() {
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
		registerSubmit();
	});
});

function registerSubmit() {
	$("#btn-register").focus().blur();
	if ($("#register-info").is(":hidden") && regFormIsValidated()) {
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
				setTimeout(function() {$("#register-success").show("blind", 400)}, 500);
			} else {
				$("#register-alert").show();
				$("#register-alert-msg").text(res.msg);
			}
		}).fail(function() {
			$("#register-alert").show();
		}).always(function() {
			$("#register-info").hide();
		});
	}
}

function checkEmail(id) {
	var valid = false;
	var email = $(id).val();
	if (email && email.length > 0 && (/.+@stopbadware.org$/).test(email)) {
		valid = true;
	}
	toggleValid(id, valid);
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