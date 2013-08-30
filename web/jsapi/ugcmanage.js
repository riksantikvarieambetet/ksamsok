var UGC_NS = {
		
	/**
	 * Initializes startup
	 * functions for dialogs.
	 */	
	init : function () {
		UGC_NS.initEditLinks();
	},
	
	/**
	 * Initialize jquery on links for
	 * opening dialogs.
	 */
	initEditLinks : function () {
		$('.edit-dialog').on('click', function (e){
	         e.preventDefault();
	         var url = $(this).attr('href'),
	             width = 400,
	             height = 650,
	             title = $(this).data('title');
	         UGC_NS.loadDialog(url, width, height, title);
	      });
	      
	    $('.delete-dialog').on('click', function (e){
	         e.preventDefault();
	         var url = $(this).attr('href'),
                 width = 400,
                 height = 650,
                 title = $(this).data('title');
	         UGC_NS.loadDialog(url, width, height, title);
	      });
	},
	
	/**
	 * Load a dialog, either edit or delete of a
	 * UGChub-object.
	 */
	loadDialog : function (url, width, height, title) {
		var formData,
		    form,
		    formid,
		    postUrl,
		    inputOuter,
		    nameOuter;
		
		$('#manageDialog').dialog({
	            autoOpen: false,
	            modal: true,
	            draggable: false,
	            resizable: false,
	            width: width,
	            height: height,
	            title: title,
	            close: function(ui, event) {
	            	UGC_NS.closeDialog();
	            }
          }).load(url, function(response, status, xhr){
        	  if (xhr.status == '200') {        		  
        		  form = $(response).find('form');
            	  formid = $(form).attr('id');
            	  postUrl = $(form).attr('action');
            	  
            	  $('.close-dialog').click(function (e) {
        			  e.preventDefault();
        			  UGC_NS.closeDialog();
        		  });
            	  
    		       $('#' + formid).on('submit', function(e){
    		    	  e.preventDefault();
    		    	  formData = $(this).serialize();
    		    	  
    		    	  $.ajax({
    		    		  url: postUrl,
    		    		 type: 'POST',
    		    		 dataType: 'json',
    		    		 data: formData,
    		    		 success: function(data) {    		    			 
    		    			 if ($.isEmptyObject(data)) {
    		    				 UGC_NS.closeDialog();
    		    			 } else {
			    				 $('#' + formid).find(':input').each(function(){
	    		    				 inputOuter = $(this);
	    		    				 nameOuter = $(inputOuter).attr('name');
	    		    				 
	    		    				 $.each(data, function(key, value){
	    		    					if (nameOuter === key) {
	    		    						$('input[name="' + key + '"]').nextAll('span:first').text(value);
	    		    					} else {
	    		    		    			$('input[name="' + nameOuter + '"]').nextAll('span:first').text('');
	    		    					}
	    		    				 });
	    		    			 });
    		    			 }
    		    		 },
    		    		 error: function(XMLHttpRequest, textStatus, errorThrown) {
    		    			 alert('Status: ' + textStatus + '\nError: ' + errorThrown);
    		    		 }
    		    	  });
    		       });  
        	  }   	
		    }).dialog('open');
	},
	
	/**
	 * General dialog-closer for the 
	 * dialogs added to the stated id-tag.
	 */
	closeDialog : function () {
		$('#manageDialog').dialog('close');
		window.location.href = "../admin/ugchub";
	}
  };

  $(function () {
	  UGC_NS.init();
  });