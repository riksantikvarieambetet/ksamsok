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
		var cronValue;
		var curr;
		if (length == '5') {
			cronValue = currentCronValueArray.toString();
		} else if (length == '6') {
			curr = currentCronValueArray.slice(1);
			cronValue = curr.toString();
			//ta bort sekunden i början
		} else if (length == '7') {
			//ta bort sekunden i början och året på slutet, kommer att bli obsolete
			curr = currentCronValueArray.slice(1, 6);
			cronValue = curr.toString();
		}
		
		var valueMod = cronValue.replace(/,/g, " ");
				
		
		$('#selector').cron({
		    initial: valueMod,
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