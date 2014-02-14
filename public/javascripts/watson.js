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
		registerSubmit();
	});
	
	$("#pwreset-box").submit(function(e) {
		e.preventDefault();
		resetPwSubmit();
	});
	
	setActiveNav();
	
});

function setActiveNav() {
	var path = (window.location.pathname=="/") ? "dashboard" : window.location.pathname.substr(1);
	var nav = path.split("/")[0];
	if (nav) {
		$("#li-nav-"+nav).addClass("active");
	}
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