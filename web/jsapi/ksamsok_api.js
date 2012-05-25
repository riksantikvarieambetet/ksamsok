var KSamsok = window.KSamsok || {};
KSamsok.search = function(text, callback, fields) {
	var scrSrc;
	if (!KSamsok.__scriptBase) {
		var scripts = document.getElementsByTagName("script");
		for (i = 0; i < scripts.length; ++i) {
			var scrSrc = scripts[i].src;
			var re = /ksamsok_api.js$/;
			if (scrSrc && scrSrc.match(re)) {
				KSamsok.__scriptBase = scrSrc.replace(re, "");
				break;
			}
		}
		if (!KSamsok.__scriptBase) {
			alert("Kunde inte ta fram script base, ej korrekt installerat?");
			return;
		}
	}
      scrSrc = KSamsok.__scriptBase + "jssearch.jsp?query=" + escape(text) + "&callback=" + callback + "&fields=" + fields;
	var heads = document.getElementsByTagName("head");
	if (!heads) {
		alert("hittade inte head i html");
		return;
	}

	var scr = document.getElementById("ksamsok_search");
	if (scr) {
		heads[0].removeChild(scr);
	}
	scr = document.createElement("script");
	scr.id = "ksamsok_search";
	scr.type= "text/javascript";
	scr.src = scrSrc;
	heads[0].appendChild(scr);
}
