var appRoute = null;
var jsonContentType = "application/json; charset=UTF-8";

$(document).ready(function($) {
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
});

function checkEmail(id) {
	var valid = false;
	var email = $(id).val();
	if (email && email.length > 0 && (/.+@stopbadware.org$/).test(email)) {
		valid = true;
	}
	toggleValid(id, valid);
}

function checkPassword(id) {
	var valid = true;
	var pw = $(id).val();
	if (pw && pw.length >= 10) {
		var regexes = [/[a-z]+/, /[A-Z]+/, /[0-9]+/, /[!@#$%^&*()\-_=+\"':;|<>?\[\]~]+/];
		regexes.map(function(regex) {
			if (!regex.test(pw)) {
				valid = false;
			}
		});
	} else {
		valid = false;
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