/**
 * 
 */
$(function(){
	var pausCheck = $('.pauseToggle'),
		paused = $('#paused'),
		formData;
	
		/*cron begin*/
		
		
		var cronStringValue = $('#cronstring').val();
		var currentCronValueArray = cronStringValue.split(" ");
		var length = currentCronValueArray.length;
		var valueString;
		var curr;
		if (length == '5') {
			valueString = currentCronValueArray.toString();
		} else if (length == '6') {
			curr = currentCronValueArray.slice(1);
			valueString = curr.toString();
			//ta bort sekunden i början
		} else if (length == '7') {
			//ta bort sekunden i början och året på slutet
			curr = currentCronValueArray.slice(1, 5);
			valueString = curr.toString();
		}
		
		var valueMod = valueString.replace(/,/g, " ");
		console.log("Pre: " + cronStringValue + "\nAfter: " + valueMod);
		//database value
		var dbValue = "1 " + valueMod;
		
		
		var cron_field = $('#selector').cron();
		
		$('#selector').cron({
		    initial: "15 4 * * *",
		    onChange: function() {
		    	$('#cronstring').val($(this).cron("value"));
		    }
		});
			
		/*cron end*/
		
	
	pausCheck.on('click', function(){
		$('#runService').prop('disabled', pausCheck.is(':checked') ? true : false);
		$(paused).val(pausCheck.is(':checked') ? "true" : "false");
//		formData = $('form').serialize();
//		formData += '&action=update';		
//		$.post('serviceaction.jsp', formData)
//		.done(function(data){
//			updateForm(data);
//		});
	});
	
	function updateForm (data) {
		$.each($.parseJSON(data), function(key, value) {
			var $elem = $('form').find('[name=' + key + ']');
			if ($elem.is('select')) {
				$('option', $elem).each(function() {
	                if (this.value == value)
	                    this.selected = true;
	            });
			} else if ($elem.is('textarea')) {
				$elem.val(value);
	        } else {
	            switch($elem.attr("type")) {
	                case "text":
	                case "hidden":
	                	$elem.val(value);  
	                    break;
	                case "checkbox":
	                    if (value == '1')
	                    	$elem.prop('checked', true);
	                    else
	                    	$elem.prop('checked', false);
	                    break;
	            }
	        }
		});
	} 
});